/**
 * InternShield AI - Frontend Runtime Configuration
 * Generated / verified during frontend build.
 *
 * In Production (e.g. deployed on Vercel):
 * - INTERNSHIELD_API_BASE_URL is injected during the Vercel build step
 *   from your Vercel Project Settings (Environment Variables).
 * - Or set window.INTERNSHIELD_API_BASE_URL directly here.
 *
 * In Local Development:
 * - Automatically defaults to "http://localhost:8080" when running on localhost / 127.0.0.1.
 */
(function (global) {
  global.INTERNSHIELD_API_BASE_URL = "" || global.INTERNSHIELD_API_BASE_URL || "";
})(typeof window !== "undefined" ? window : globalThis);
