package com.internshield.internship;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Validates URLs against Server-Side Request Forgery (SSRF) and prohibits connections
 * to private IP ranges, localhost, cloud metadata endpoints, and internal infrastructure.
 */
@Component
public class SsrfProtectionValidator {

    private static final Logger log = LoggerFactory.getLogger(SsrfProtectionValidator.class);

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "::1",
            "metadata.google.internal",
            "instance-data",
            "169.254.169.254"
    );

    /**
     * Validates that the provided URL is safe to fetch via HTTP/HTTPS.
     *
     * @param url the URL to test
     * @return the normalized valid URI
     * @throws IllegalArgumentException if the URL violates security rules
     */
    public URI validateSafeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }

        String trimmed = url.trim();
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL syntax: " + url);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Protocol not permitted. Only HTTP and HTTPS are allowed: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL host is missing or invalid: " + url);
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);
        if (BLOCKED_HOSTS.contains(lowerHost)) {
            throw new IllegalArgumentException("Access to internal/metadata hosts is blocked: " + host);
        }

        if (lowerHost.endsWith(".local") || lowerHost.endsWith(".internal") || lowerHost.endsWith(".corp")) {
            throw new IllegalArgumentException("Access to internal domain zone is blocked: " + host);
        }

        // Validate DNS resolution to detect internal IP destinations
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isLoopbackAddress()) {
                    throw new IllegalArgumentException("Access to loopback address is blocked: " + address.getHostAddress());
                }
                if (address.isSiteLocalAddress()) {
                    throw new IllegalArgumentException("Access to private site-local network is blocked: " + address.getHostAddress());
                }
                if (address.isLinkLocalAddress()) {
                    throw new IllegalArgumentException("Access to link-local network is blocked: " + address.getHostAddress());
                }
                if (address.isAnyLocalAddress()) {
                    throw new IllegalArgumentException("Access to wildcard local address is blocked: " + address.getHostAddress());
                }
                if (address.isMulticastAddress()) {
                    throw new IllegalArgumentException("Access to multicast network is blocked: " + address.getHostAddress());
                }

                // Explicit check for AWS/Azure/GCP metadata address (169.254.169.254)
                byte[] bytes = address.getAddress();
                if (bytes.length == 4) {
                    int b0 = bytes[0] & 0xFF;
                    int b1 = bytes[1] & 0xFF;
                    if (b0 == 127) { // 127.0.0.0/8
                        throw new IllegalArgumentException("Loopback address blocked");
                    }
                    if (b0 == 10) { // 10.0.0.0/8
                        throw new IllegalArgumentException("Private IP blocked (10.x.x.x)");
                    }
                    if (b0 == 172 && (b1 >= 16 && b1 <= 31)) { // 172.16.0.0/12
                        throw new IllegalArgumentException("Private IP blocked (172.16-31.x.x)");
                    }
                    if (b0 == 192 && b1 == 168) { // 192.168.0.0/16
                        throw new IllegalArgumentException("Private IP blocked (192.168.x.x)");
                    }
                    if (b0 == 169 && b1 == 254) { // 169.254.0.0/16 link local
                        throw new IllegalArgumentException("Link-local/metadata address blocked (169.254.x.x)");
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("DNS resolution failed or timed out for host {}: {}", host, e.getMessage());
            // If DNS cannot be resolved, allow listing extractor to handle connection failure gracefully
        }

        return uri;
    }
}
