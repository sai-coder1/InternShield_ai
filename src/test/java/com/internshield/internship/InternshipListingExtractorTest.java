package com.internshield.internship;

import com.internshield.internship.dto.ExtractedInternshipDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternshipListingExtractorTest {

    private InternshipListingExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new InternshipListingExtractor(new SsrfProtectionValidator());
    }

    @Test
    @DisplayName("Should extract recruiter email, phone, and fee keywords from text")
    void testExtractFromPastedText_detectsContactAndFees() {
        String sampleJob = """
                Software Engineering Intern at QuickStart Corp.
                Contact recruiter directly at hr.recruiting@quickstart.com or call +91-9876543210.
                Responsibilities: Develop Spring Boot microservices, write unit tests.
                Important Notice: Selected candidates must pay a registration fee of Rs 2500 to confirm enrollment.
                Stipend: ₹25,000 per month.
                """;

        ExtractedInternshipDetails details = extractor.extractFromPastedText(sampleJob, "QuickStart Corp");

        assertNotNull(details);
        assertEquals("QuickStart Corp", details.companyName());
        assertEquals("hr.recruiting@quickstart.com", details.recruiterEmail());
        assertEquals("+91-9876543210", details.recruiterPhone());
        assertTrue(details.feeDemanded());
        assertTrue(details.paymentKeywordsFound().contains("registration fee"));
        assertTrue(details.stipend().contains("25,000"));
        assertFalse(details.unreachable());
    }

    @Test
    @DisplayName("Should detect WhatsApp and Telegram recruitment channels")
    void testExtractFromPastedText_detectsMessagingChannels() {
        String telegramJob = "Apply now! Direct selection. Join our Telegram channel t.me/fast_interns to claim offer.";
        ExtractedInternshipDetails tgDetails = extractor.extractFromPastedText(telegramJob, "FastInterns");
        assertEquals("TELEGRAM", tgDetails.communicationChannel());

        String whatsappJob = "Urgent opening. Contact manager on WhatsApp wa.me/919999999999 for registration charges.";
        ExtractedInternshipDetails waDetails = extractor.extractFromPastedText(whatsappJob, "WhatsAppTeam");
        assertEquals("WHATSAPP", waDetails.communicationChannel());
        assertTrue(waDetails.feeDemanded());
    }

    @Test
    @DisplayName("Should handle SSRF-blocked or unreachable URLs safely without throwing exceptions")
    void testExtractFromUrl_blockedUrlHandledGracefully() {
        ExtractedInternshipDetails details = extractor.extractFromUrl("http://127.0.0.1:8080/internship");

        assertNotNull(details);
        assertTrue(details.unreachable());
        assertNotNull(details.extractionError());
        assertTrue(details.extractionError().contains("Security validation blocked") || details.extractionError().contains("blocked"));
    }

    @Test
    @DisplayName("Should return empty details on null or empty input")
    void testExtract_nullAndEmptyInputs() {
        ExtractedInternshipDetails emptyDetails = extractor.extract(null, null, "Acme");
        assertNotNull(emptyDetails);
        assertTrue(emptyDetails.rawContent().isEmpty());

        ExtractedInternshipDetails blankText = extractor.extractFromPastedText("   ", "Acme");
        assertTrue(blankText.rawContent().isEmpty());
    }

    @Test
    @DisplayName("Should merge URL extraction and pasted text accurately")
    void testExtract_mergedDetails() {
        String pastedNote = "Note: Training charges apply. WhatsApp group: wa.me/1234567890";
        ExtractedInternshipDetails details = extractor.extract("", pastedNote, "Apex Systems");

        assertNotNull(details);
        assertEquals("Apex Systems", details.companyName());
        assertTrue(details.feeDemanded());
        assertEquals("WHATSAPP", details.communicationChannel());
    }

    @Test
    @DisplayName("SSRF Redirect Protection: Blocks redirect to localhost and loopback")
    void testResolveAndValidateRedirect_blocksLocalhostAndLoopback() {
        URI baseUri = URI.create("https://legit-job-board.com/listing/123");

        assertThrows(IllegalArgumentException.class, () ->
                extractor.resolveAndValidateRedirect(baseUri, "http://127.0.0.1:8080/actuator"));

        assertThrows(IllegalArgumentException.class, () ->
                extractor.resolveAndValidateRedirect(baseUri, "http://localhost:3000/internal"));
    }

    @Test
    @DisplayName("SSRF Redirect Protection: Blocks redirect to private IP networks (10.x, 192.168.x)")
    void testResolveAndValidateRedirect_blocksPrivateIps() {
        URI baseUri = URI.create("https://legit-job-board.com/listing/123");

        assertThrows(IllegalArgumentException.class, () ->
                extractor.resolveAndValidateRedirect(baseUri, "http://10.0.0.5/api/credentials"));

        assertThrows(IllegalArgumentException.class, () ->
                extractor.resolveAndValidateRedirect(baseUri, "http://192.168.1.100/admin"));
    }

    @Test
    @DisplayName("SSRF Redirect Protection: Blocks redirect to cloud metadata endpoints")
    void testResolveAndValidateRedirect_blocksCloudMetadata() {
        URI baseUri = URI.create("https://legit-job-board.com/listing/123");

        assertThrows(IllegalArgumentException.class, () ->
                extractor.resolveAndValidateRedirect(baseUri, "http://169.254.169.254/latest/meta-data"));

        assertThrows(IllegalArgumentException.class, () ->
                extractor.resolveAndValidateRedirect(baseUri, "http://metadata.google.internal/computeMetadata/v1"));
    }

    @Test
    @DisplayName("SSRF Redirect Protection: Correctly resolves relative redirect and validates public destination")
    void testResolveAndValidateRedirect_relativeRedirectResolvedSafely() {
        URI baseUri = URI.create("https://company.com/careers/index.html");

        URI resolved = extractor.resolveAndValidateRedirect(baseUri, "/jobs/software-intern");

        assertNotNull(resolved);
        assertEquals("https://company.com/jobs/software-intern", resolved.toString());
    }

    @Test
    @DisplayName("SSRF Redirect Protection: Rejects empty, blank or malformed Location headers")
    void testResolveAndValidateRedirect_rejectsMalformedRedirects() {
        URI baseUri = URI.create("https://company.com/careers");

        assertThrows(IllegalArgumentException.class, () ->
                extractor.resolveAndValidateRedirect(baseUri, ""));

        assertThrows(IllegalArgumentException.class, () ->
                extractor.resolveAndValidateRedirect(baseUri, "   "));

        assertThrows(IllegalArgumentException.class, () ->
                extractor.resolveAndValidateRedirect(baseUri, null));
    }
}
