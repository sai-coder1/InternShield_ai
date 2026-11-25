/**
 * InternShield AI - Frontend Build Script
 *
 * Runs during Vercel deployment (or local frontend build).
 * Injects INTERNSHIELD_API_BASE_URL environment variable from Vercel into frontend/js/config.js
 * while keeping local development default intact.
 */

const fs = require("fs");
const path = require("path");

const rootDir = path.resolve(__dirname, "..");
const configFilePath = path.join(rootDir, "frontend", "js", "config.js");

// Read configurable backend URL from environment variables supported across platforms
const rawApiUrl = (
  process.env.INTERNSHIELD_API_BASE_URL ||
  process.env.API_BASE_URL ||
  process.env.VITE_API_BASE_URL ||
  process.env.NEXT_PUBLIC_API_URL ||
  ""
).trim();

// Strip any trailing slashes
const normalizedApiUrl = rawApiUrl.replace(/\/+$/, "");

console.log("==================================================");
console.log("InternShield AI - Frontend Build");
console.log("==================================================");

if (normalizedApiUrl) {
  console.log(`[BUILD] Production backend URL configured: ${normalizedApiUrl}`);
} else {
  console.log("[BUILD] No INTERNSHIELD_API_BASE_URL specified in environment.");
  console.log("[BUILD] Runtime will use safe fallback (localhost for local dev; prompts config in prod).");
}

const configContent = `/**
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
  global.INTERNSHIELD_API_BASE_URL = ${JSON.stringify(normalizedApiUrl)} || global.INTERNSHIELD_API_BASE_URL || "";
})(typeof window !== "undefined" ? window : globalThis);
`;

fs.writeFileSync(configFilePath, configContent, "utf8");
console.log(`[BUILD] Updated ${path.relative(rootDir, configFilePath)} successfully.`);

// Validate essential files exist
const requiredFiles = [
  path.join("frontend", "index.html"),
  path.join("frontend", "login.html"),
  path.join("frontend", "register.html"),
  path.join("frontend", "js", "config.js"),
  path.join("frontend", "js", "app.js"),
  path.join("frontend", "js", "auth.js")
];

let allFilesPresent = true;
for (const relPath of requiredFiles) {
  const fullPath = path.join(rootDir, relPath);
  if (!fs.existsSync(fullPath)) {
    console.error(`[ERROR] Missing required file: ${relPath}`);
    allFilesPresent = false;
  }
}

if (!allFilesPresent) {
  process.exit(1);
}

console.log("[BUILD] Frontend build completed successfully.");
console.log("==================================================");
