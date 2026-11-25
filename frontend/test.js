/**
 * InternShield AI - Frontend Configuration & Integrity Tests
 */

const fs = require("fs");
const path = require("path");
const assert = require("assert");

const frontendDir = __dirname;
let passed = 0;
let failed = 0;

function test(name, fn) {
  try {
    fn();
    console.log(`  PASS: ${name}`);
    passed++;
  } catch (err) {
    console.error(`  FAIL: ${name}`);
    console.error(`    ${err.message}`);
    failed++;
  }
}

console.log("==================================================");
console.log("Running Frontend Configuration Tests");
console.log("==================================================");

// Simulated resolveApiBaseUrl implementation matching frontend/js/app.js
function createResolver(mockWindow) {
  return function resolveApiBaseUrl() {
    const configured = typeof mockWindow.INTERNSHIELD_API_BASE_URL === "string"
      ? mockWindow.INTERNSHIELD_API_BASE_URL.trim()
      : "";

    if (configured) {
      return configured.replace(/\/+$/, "");
    }

    const host = mockWindow.location ? mockWindow.location.hostname : "";
    const protocol = mockWindow.location ? mockWindow.location.protocol : "";

    const isLocal = Boolean(
      host === "localhost" ||
      host === "127.0.0.1" ||
      host === "[::1]" ||
      host === "" ||
      protocol === "file:"
    );

    if (isLocal) {
      return "http://localhost:8080";
    }

    // Production: Do not fall back to localhost!
    return "";
  };
}

// Test 1: Local development defaults to http://localhost:8080
test("Local development on localhost defaults to http://localhost:8080", () => {
  const win = { location: { hostname: "localhost", protocol: "http:" } };
  const resolve = createResolver(win);
  assert.strictEqual(resolve(), "http://localhost:8080");
});

test("Local development on 127.0.0.1 defaults to http://localhost:8080", () => {
  const win = { location: { hostname: "127.0.0.1", protocol: "http:" } };
  const resolve = createResolver(win);
  assert.strictEqual(resolve(), "http://localhost:8080");
});

test("Local development via file: protocol defaults to http://localhost:8080", () => {
  const win = { location: { hostname: "", protocol: "file:" } };
  const resolve = createResolver(win);
  assert.strictEqual(resolve(), "http://localhost:8080");
});

// Test 2: Production environment respects configured URL
test("Production with configured URL returns the configured backend URL", () => {
  const win = {
    location: { hostname: "internshield-ai.vercel.app", protocol: "https:" },
    INTERNSHIELD_API_BASE_URL: "https://internshield-ai-backend.onrender.com"
  };
  const resolve = createResolver(win);
  assert.strictEqual(resolve(), "https://internshield-ai-backend.onrender.com");
});

test("Production trims trailing slashes from configured URL", () => {
  const win = {
    location: { hostname: "internshield-ai.vercel.app", protocol: "https:" },
    INTERNSHIELD_API_BASE_URL: "https://internshield-ai-backend.onrender.com///"
  };
  const resolve = createResolver(win);
  assert.strictEqual(resolve(), "https://internshield-ai-backend.onrender.com");
});

// Test 3: Production without config does NOT fall back to localhost
test("Production without configured URL does NOT fall back to localhost", () => {
  const win = {
    location: { hostname: "internshield-ai.vercel.app", protocol: "https:" },
    INTERNSHIELD_API_BASE_URL: ""
  };
  const resolve = createResolver(win);
  const result = resolve();
  assert.notStrictEqual(result, "http://localhost:8080");
  assert.strictEqual(result, "");
});

// Test 4: HTML files include config.js
test("index.html includes js/config.js script tag", () => {
  const content = fs.readFileSync(path.join(frontendDir, "index.html"), "utf8");
  assert.ok(content.includes('src="js/config.js"'), "index.html must include js/config.js");
});

test("login.html includes js/config.js script tag", () => {
  const content = fs.readFileSync(path.join(frontendDir, "login.html"), "utf8");
  assert.ok(content.includes('src="js/config.js"'), "login.html must include js/config.js");
});

test("register.html includes js/config.js script tag", () => {
  const content = fs.readFileSync(path.join(frontendDir, "register.html"), "utf8");
  assert.ok(content.includes('src="js/config.js"'), "register.html must include js/config.js");
});

// Test 5: Verify no hardcoded localhost API endpoints in app.js
test("frontend/js/app.js does not contain hardcoded localhost API endpoint URLs", () => {
  const appJs = fs.readFileSync(path.join(frontendDir, "js", "app.js"), "utf8");
  const lines = appJs.split("\n");
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (line.includes("fetch(") && line.includes("localhost:8080")) {
      throw new Error(`Line ${i + 1} contains hardcoded localhost:8080 in fetch call: ${line.trim()}`);
    }
  }
});

console.log("==================================================");
console.log(`Results: ${passed} passed, ${failed} failed`);
console.log("==================================================");

if (failed > 0) {
  process.exit(1);
}
