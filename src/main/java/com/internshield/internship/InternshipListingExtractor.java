package com.internshield.internship;

import com.internshield.internship.dto.ExtractedInternshipDetails;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InternshipListingExtractor {

    private static final Logger log = LoggerFactory.getLogger(InternshipListingExtractor.class);

    private static final int TIMEOUT_MILLIS = 8000;
    private static final int MAX_REDIRECTS = 3;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 InternShield-Verifier/1.0";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}|(?:\\+91[-.\\s]?)?[6-9]\\d{9}");

    private static final Pattern PAYMENT_FEE_PATTERN = Pattern.compile(
            "(?i)\\b(registration fee|registration charges|training fee|training charges|processing fee|onboarding fee|security deposit|security fee|certificate fee|certification fee|certification charges|certification program|paid certification|equipment fee|laptop charge|admission fee|upfront payment|upfront fee|pay upfront|pay to start|advance fee|advance payment|pay\\s+(?:rs|inr|usd|\\$|amount|fee)|deposit of rs|pay to confirm|payment[:\\s]+(?:via\\s+|through\\s+)?upi|payment[:\\s]*upi|upi id)\\b");

    private static final Pattern STIPEND_PATTERN = Pattern.compile(
            "(?i)\\b(?:stipend|salary|remuneration|pay|compensation)[:\\s]*([₹$€£]?\\s*\\d{1,3}(?:,\\d{3})*(?:\\s*-\\s*[₹$€£]?\\s*\\d{1,3}(?:,\\d{3})*)?(?:\\s*(?:k|thousand|lakh|per month|/month|p\\.m\\.))?)\\b");

    private static final Pattern WHATSAPP_TELEGRAM_PATTERN = Pattern.compile(
            "(?i)\\b(t\\.me\\/|telegram|wa\\.me\\/|whatsapp|whatsapp group|telegram channel)\\b");

    private final SsrfProtectionValidator ssrfValidator;

    public InternshipListingExtractor(SsrfProtectionValidator ssrfValidator) {
        this.ssrfValidator = ssrfValidator;
    }

    /**
     * Extracts content from an internship URL safely using Jsoup with manual redirect validation.
     * Prevents SSRF bypass through HTTP 301/302/307 redirects to internal destinations.
     *
     * @param url the internship listing URL
     * @return structured extracted details
     */
    public ExtractedInternshipDetails extractFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return ExtractedInternshipDetails.empty();
        }

        try {
            URI currentUri = ssrfValidator.validateSafeUrl(url);
            int redirectCount = 0;
            Connection.Response response = null;

            while (true) {
                response = Jsoup.connect(currentUri.toString())
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MILLIS)
                        .followRedirects(false) // Manual redirect validation to prevent SSRF
                        .maxBodySize(2 * 1024 * 1024) // 2 MB max
                        .ignoreHttpErrors(false)
                        .execute();

                int statusCode = response.statusCode();
                if (statusCode >= 300 && statusCode < 400) {
                    redirectCount++;
                    if (redirectCount > MAX_REDIRECTS) {
                        throw new IllegalArgumentException("Too many redirects: exceeded maximum limit of " + MAX_REDIRECTS);
                    }

                    String location = response.header("Location");
                    currentUri = resolveAndValidateRedirect(currentUri, location);
                } else {
                    break;
                }
            }

            if (response == null) {
                return ExtractedInternshipDetails.unreachable("No response received from remote server");
            }

            Document doc = response.parse();
            String pageTitle = doc.title();
            String metaDesc = "";
            Element metaTag = doc.selectFirst("meta[name=description], meta[property=og:description]");
            if (metaTag != null) {
                metaDesc = metaTag.attr("content");
            }

            // Extract main textual content
            String bodyText = doc.body() != null ? doc.body().text() : "";
            String fullContent = pageTitle + "\n" + metaDesc + "\n" + bodyText;

            // Extract list items (often responsibilities)
            List<String> responsibilities = new ArrayList<>();
            Elements listElements = doc.select("li");
            for (Element li : listElements) {
                String liText = li.text().trim();
                if (liText.length() > 15 && liText.length() < 250) {
                    responsibilities.add(liText);
                    if (responsibilities.size() >= 8) break;
                }
            }

            return analyzeTextContent(fullContent, pageTitle, responsibilities, null);

        } catch (IllegalArgumentException e) {
            log.warn("SSRF or invalid URL blocked for {}: {}", url, e.getMessage());
            return ExtractedInternshipDetails.unreachable("Security validation blocked URL: " + e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to retrieve URL {}: {}", url, e.getMessage());
            return ExtractedInternshipDetails.unreachable("Could not fetch remote page: " + e.getMessage());
        }
    }

    /**
     * Resolves a redirect location relative to the current URI and strictly validates the target against SSRF rules.
     *
     * @param baseUri current verified URI
     * @param redirectLocation raw Location header value from HTTP 3xx response
     * @return validated target URI
     * @throws IllegalArgumentException if the redirect is malformed or targets an internal destination
     */
    public URI resolveAndValidateRedirect(URI baseUri, String redirectLocation) {
        if (redirectLocation == null || redirectLocation.isBlank()) {
            throw new IllegalArgumentException("Malformed redirect: missing or blank Location header");
        }

        URI targetUri;
        try {
            targetUri = baseUri.resolve(redirectLocation.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed redirect target URL: " + redirectLocation);
        }

        // Revalidate the resolved target URL against SSRF rules before allowing connection
        return ssrfValidator.validateSafeUrl(targetUri.toString());
    }

    /**
     * Extracts and analyzes internship information from pasted text.
     *
     * @param text raw description or job posting text
     * @param fallbackCompany default company name if not found in text
     * @return structured extracted details
     */
    public ExtractedInternshipDetails extractFromPastedText(String text, String fallbackCompany) {
        if (text == null || text.isBlank()) {
            return ExtractedInternshipDetails.empty();
        }

        return analyzeTextContent(text, fallbackCompany, Collections.emptyList(), fallbackCompany);
    }

    /**
     * Unified extraction combining URL listing (if accessible) and/or pasted description.
     */
    public ExtractedInternshipDetails extract(String url, String pastedText, String companyName) {
        ExtractedInternshipDetails fromUrl = ExtractedInternshipDetails.empty();
        if (url != null && !url.isBlank()) {
            fromUrl = extractFromUrl(url);
        }

        ExtractedInternshipDetails fromText = ExtractedInternshipDetails.empty();
        if (pastedText != null && !pastedText.isBlank()) {
            fromText = extractFromPastedText(pastedText, companyName);
        }

        // If URL succeeded, blend in any additional pasted text clues
        if (!fromUrl.unreachable() && !fromUrl.rawContent().isBlank()) {
            if (!fromText.rawContent().isBlank()) {
                // Merge details
                List<String> mergedKeywords = new ArrayList<>(fromUrl.paymentKeywordsFound());
                for (String kw : fromText.paymentKeywordsFound()) {
                    if (!mergedKeywords.contains(kw)) mergedKeywords.add(kw);
                }
                boolean feeDemanded = fromUrl.feeDemanded() || fromText.feeDemanded();
                String email = !fromUrl.recruiterEmail().isBlank() ? fromUrl.recruiterEmail() : fromText.recruiterEmail();
                String phone = !fromUrl.recruiterPhone().isBlank() ? fromUrl.recruiterPhone() : fromText.recruiterPhone();
                String stipend = !fromUrl.stipend().isBlank() ? fromUrl.stipend() : fromText.stipend();
                String channel = !"UNKNOWN".equals(fromUrl.communicationChannel()) ? fromUrl.communicationChannel() : fromText.communicationChannel();

                return new ExtractedInternshipDetails(
                        fromUrl.title(),
                        companyName != null ? companyName : fromUrl.companyName(),
                        fromUrl.rawContent() + "\n--- Pasted Description ---\n" + fromText.rawContent(),
                        fromUrl.role().isBlank() ? fromText.role() : fromUrl.role(),
                        stipend,
                        fromUrl.responsibilities().isEmpty() ? fromText.responsibilities() : fromUrl.responsibilities(),
                        email,
                        phone,
                        channel,
                        mergedKeywords,
                        feeDemanded,
                        false,
                        null
                );
            }
            return fromUrl;
        }

        // If URL was unreachable or missing, return text extraction
        if (!fromText.rawContent().isBlank()) {
            return fromText;
        }

        return fromUrl;
    }

    private ExtractedInternshipDetails analyzeTextContent(
            String text, String title, List<String> responsibilities, String fallbackCompany) {
        if (text == null) text = "";

        // Detect Email
        String recruiterEmail = "";
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        if (emailMatcher.find()) {
            recruiterEmail = emailMatcher.group().toLowerCase(Locale.ROOT);
        }

        // Detect Phone
        String recruiterPhone = "";
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        if (phoneMatcher.find()) {
            recruiterPhone = phoneMatcher.group().trim();
        }

        // Detect Payment keywords
        Set<String> keywordsFound = new HashSet<>();
        Matcher feeMatcher = PAYMENT_FEE_PATTERN.matcher(text);
        while (feeMatcher.find()) {
            keywordsFound.add(feeMatcher.group().toLowerCase(Locale.ROOT));
        }

        // Detect Stipend
        String stipend = "";
        Matcher stipendMatcher = STIPEND_PATTERN.matcher(text);
        if (stipendMatcher.find()) {
            stipend = stipendMatcher.group(1).trim();
        }

        // Detect Communication Channel
        String channel = "STANDARD_WEB";
        Matcher channelMatcher = WHATSAPP_TELEGRAM_PATTERN.matcher(text);
        if (channelMatcher.find()) {
            String hit = channelMatcher.group().toLowerCase(Locale.ROOT);
            if (hit.contains("telegram") || hit.contains("t.me")) {
                channel = "TELEGRAM";
            } else if (hit.contains("whatsapp") || hit.contains("wa.me")) {
                channel = "WHATSAPP";
            }
        }

        return new ExtractedInternshipDetails(
                title != null ? title : "",
                fallbackCompany != null ? fallbackCompany : "",
                text,
                title != null ? title : "Internship Role",
                stipend,
                responsibilities,
                recruiterEmail,
                recruiterPhone,
                channel,
                new ArrayList<>(keywordsFound),
                !keywordsFound.isEmpty(),
                false,
                null
        );
    }
}
