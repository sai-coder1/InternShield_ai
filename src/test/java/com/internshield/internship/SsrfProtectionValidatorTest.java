package com.internshield.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsrfProtectionValidatorTest {

    private SsrfProtectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SsrfProtectionValidator();
    }

    @Test
    @DisplayName("Should accept valid public HTTPS URLs")
    void testValidateSafeUrl_validPublicHttps() {
        assertDoesNotThrow(() -> {
            URI uri = validator.validateSafeUrl("https://example.com/careers/internship");
            assertNotNull(uri);
            assertEquals("example.com", uri.getHost());
        });
    }

    @ParameterizedTest(name = "Block localhost/loopback: {0}")
    @ValueSource(strings = {
            "http://localhost/jobs",
            "http://localhost:8080/api",
            "https://127.0.0.1:8000/internship",
            "http://127.0.0.1/test",
            "http://0.0.0.0/",
            "http://[::1]:8080/jobs"
    })
    void testValidateSafeUrl_blocksLocalhostAndLoopback(String blockedUrl) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSafeUrl(blockedUrl));
        assertTrue(ex.getMessage().toLowerCase().contains("blocked") || ex.getMessage().toLowerCase().contains("loopback"));
    }

    @ParameterizedTest(name = "Block cloud metadata endpoints: {0}")
    @ValueSource(strings = {
            "http://169.254.169.254/latest/meta-data/",
            "https://169.254.169.254/computeMetadata/v1/",
            "http://metadata.google.internal/computeMetadata/v1/",
            "http://instance-data/latest/meta-data/"
    })
    void testValidateSafeUrl_blocksCloudMetadata(String metadataUrl) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSafeUrl(metadataUrl));
        assertTrue(ex.getMessage().toLowerCase().contains("blocked") || ex.getMessage().toLowerCase().contains("metadata"));
    }

    @ParameterizedTest(name = "Block internal domain zones: {0}")
    @ValueSource(strings = {
            "http://service.corp/internships",
            "http://intranet.internal/jobs",
            "http://backend.local:9000/careers"
    })
    void testValidateSafeUrl_blocksInternalDomains(String internalUrl) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSafeUrl(internalUrl));
        assertTrue(ex.getMessage().toLowerCase().contains("internal") || ex.getMessage().toLowerCase().contains("blocked"));
    }

    @ParameterizedTest(name = "Block dangerous protocols: {0}")
    @ValueSource(strings = {
            "ftp://example.com/internship.pdf",
            "file:///etc/passwd",
            "file:///C:/Windows/win.ini",
            "gopher://127.0.0.1:70/",
            "ldap://internal.corp:389/o=company"
    })
    void testValidateSafeUrl_blocksUnsupportedProtocols(String badProtocolUrl) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSafeUrl(badProtocolUrl));
        assertTrue(ex.getMessage().toLowerCase().contains("protocol") || ex.getMessage().toLowerCase().contains("not permitted"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on null, blank or malformed URLs")
    void testValidateSafeUrl_invalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateSafeUrl(null));
        assertThrows(IllegalArgumentException.class, () -> validator.validateSafeUrl(""));
        assertThrows(IllegalArgumentException.class, () -> validator.validateSafeUrl("   "));
        assertThrows(IllegalArgumentException.class, () -> validator.validateSafeUrl("not-a-valid-url"));
    }

    @Test
    @DisplayName("Should block domain whose DNS resolves to loopback IP (e.g. localtest.me -> 127.0.0.1)")
    void testValidateSafeUrl_blocksDnsResolvingToLoopback() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateSafeUrl("http://localtest.me/jobs"));
    }
}
