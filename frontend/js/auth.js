/**
 * InternShield AI - Authentication & Form Validation Controller
 * Handles client-side validation, password visibility toggles,
 * inline error display, and simulation banners for Login and Registration.
 *
 * NOTE: Real backend authentication is not yet connected.
 */

document.addEventListener("DOMContentLoaded", () => {
  initPasswordToggles();
  initLoginForm();
  initRegisterForm();
  initForgotPassword();
  initSampleCredentials();
});

// Standard email format validation regex (accepts any valid domain: Gmail, Outlook, Yahoo, college, etc.)
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*\.[a-zA-Z]{2,}$/;

/**
 * Show / Hide password toggle controller
 */
function initPasswordToggles() {
  const toggleButtons = document.querySelectorAll(".toggle-password-btn");

  toggleButtons.forEach(btn => {
    btn.addEventListener("click", () => {
      const targetId = btn.getAttribute("data-target");
      const passwordInput = document.getElementById(targetId);

      if (!passwordInput) return;

      const isPassword = passwordInput.getAttribute("type") === "password";
      passwordInput.setAttribute("type", isPassword ? "text" : "password");
      btn.setAttribute("aria-label", isPassword ? "Hide password" : "Show password");

      // Toggle Eye vs Eye-Off SVG
      btn.innerHTML = isPassword
        ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
            <line x1="1" y1="1" x2="23" y2="23"></line>
          </svg>`
        : `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
            <circle cx="12" cy="12" r="3"></circle>
          </svg>`;
    });
  });
}

/**
 * Helper to display inline validation error
 */
function showError(inputEl, errorEl, message) {
  if (inputEl) {
    inputEl.classList.add("error");
    inputEl.setAttribute("aria-invalid", "true");
  }
  if (errorEl) {
    errorEl.textContent = message;
    errorEl.classList.add("visible");
  }
}

/**
 * Helper to clear inline validation error
 */
function clearError(inputEl, errorEl) {
  if (inputEl) {
    inputEl.classList.remove("error");
    inputEl.removeAttribute("aria-invalid");
  }
  if (errorEl) {
    errorEl.textContent = "";
    errorEl.classList.remove("visible");
  }
}

/**
 * Login Form Validation & Simulation
 */
function initLoginForm() {
  const form = document.getElementById("loginForm");
  if (!form) return;

  const emailInput = document.getElementById("loginEmail");
  const emailError = document.getElementById("loginEmailError");
  const passwordInput = document.getElementById("loginPassword");
  const passwordError = document.getElementById("loginPasswordError");
  const noticeBanner = document.getElementById("loginNoticeBanner");

  // Real-time error clearing when user types
  if (emailInput) {
    emailInput.addEventListener("input", () => clearError(emailInput, emailError));
  }
  if (passwordInput) {
    passwordInput.addEventListener("input", () => clearError(passwordInput, passwordError));
  }

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    let isValid = true;

    // Reset old banner
    if (noticeBanner) {
      noticeBanner.classList.remove("visible", "success", "info");
      noticeBanner.innerHTML = "";
    }

    // 1. Email validation
    const emailVal = emailInput ? emailInput.value.trim() : "";
    if (!emailVal) {
      showError(emailInput, emailError, "Email address is required.");
      isValid = false;
    } else if (!EMAIL_REGEX.test(emailVal)) {
      showError(emailInput, emailError, "Please enter a valid email address (e.g., name@gmail.com or college email).");
      isValid = false;
    } else {
      clearError(emailInput, emailError);
    }

    // 2. Password validation
    const passwordVal = passwordInput ? passwordInput.value : "";
    if (!passwordVal) {
      showError(passwordInput, passwordError, "Password is required.");
      isValid = false;
    } else {
      clearError(passwordInput, passwordError);
    }

    if (!isValid) return;

    // Display simulated confirmation banner (does not claim real registration/login)
    if (noticeBanner) {
      noticeBanner.className = "auth-notice-banner success visible";
      noticeBanner.innerHTML = `
        <strong>✓ Demo Mode – Format Verified</strong>
        <p style="margin-top: 6px; margin-bottom: 12px;">
          Valid format verified. <strong>No real account authentication is performed in Demo Mode.</strong> You can now explore the full interactive demonstration.
        </p>
        <div style="display: flex; gap: 10px; flex-wrap: wrap;">
          <a href="index.html" class="btn btn-primary btn-sm">Enter Main Website Demo →</a>
        </div>
      `;
      noticeBanner.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }
  });
}

/**
 * Registration Form Validation & Simulation
 */
function initRegisterForm() {
  const form = document.getElementById("registerForm");
  if (!form) return;

  const nameInput = document.getElementById("regFullName");
  const nameError = document.getElementById("regFullNameError");
  const emailInput = document.getElementById("regEmail");
  const emailError = document.getElementById("regEmailError");
  const passwordInput = document.getElementById("regPassword");
  const passwordError = document.getElementById("regPasswordError");
  const confirmPasswordInput = document.getElementById("regConfirmPassword");
  const confirmPasswordError = document.getElementById("regConfirmPasswordError");
  const termsCheckbox = document.getElementById("regTerms");
  const termsError = document.getElementById("regTermsError");
  const noticeBanner = document.getElementById("registerNoticeBanner");

  // Real-time clearing
  if (nameInput) nameInput.addEventListener("input", () => clearError(nameInput, nameError));
  if (emailInput) emailInput.addEventListener("input", () => clearError(emailInput, emailError));
  if (passwordInput) passwordInput.addEventListener("input", () => clearError(passwordInput, passwordError));
  if (confirmPasswordInput) confirmPasswordInput.addEventListener("input", () => clearError(confirmPasswordInput, confirmPasswordError));
  if (termsCheckbox) termsCheckbox.addEventListener("change", () => clearError(null, termsError));

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    let isValid = true;

    // Reset old banner
    if (noticeBanner) {
      noticeBanner.classList.remove("visible", "success", "info");
      noticeBanner.innerHTML = "";
    }

    // 1. Full name validation
    const nameVal = nameInput ? nameInput.value.trim() : "";
    if (!nameVal) {
      showError(nameInput, nameError, "Full name is required.");
      isValid = false;
    } else if (nameVal.length < 2) {
      showError(nameInput, nameError, "Full name must be at least 2 characters.");
      isValid = false;
    } else {
      clearError(nameInput, nameError);
    }

    // 2. Email validation
    const emailVal = emailInput ? emailInput.value.trim() : "";
    if (!emailVal) {
      showError(emailInput, emailError, "Email address is required.");
      isValid = false;
    } else if (!EMAIL_REGEX.test(emailVal)) {
      showError(emailInput, emailError, "Please enter a valid email address (e.g., name@gmail.com or college email).");
      isValid = false;
    } else {
      clearError(emailInput, emailError);
    }

    // 3. Password length validation
    const passwordVal = passwordInput ? passwordInput.value : "";
    if (!passwordVal) {
      showError(passwordInput, passwordError, "Password is required.");
      isValid = false;
    } else if (passwordVal.length < 8) {
      showError(passwordInput, passwordError, "Password must be at least 8 characters long.");
      isValid = false;
    } else {
      clearError(passwordInput, passwordError);
    }

    // 4. Confirm password validation
    const confirmVal = confirmPasswordInput ? confirmPasswordInput.value : "";
    if (!confirmVal) {
      showError(confirmPasswordInput, confirmPasswordError, "Please confirm your password.");
      isValid = false;
    } else if (confirmVal !== passwordVal) {
      showError(confirmPasswordInput, confirmPasswordError, "Passwords do not match.");
      isValid = false;
    } else {
      clearError(confirmPasswordInput, confirmPasswordError);
    }

    // 5. Terms and Conditions validation
    if (termsCheckbox && !termsCheckbox.checked) {
      showError(null, termsError, "You must agree to the Terms of Service and Privacy Policy.");
      isValid = false;
    } else {
      clearError(null, termsError);
    }

    if (!isValid) return;

    // Display simulated confirmation banner
    if (noticeBanner) {
      noticeBanner.className = "auth-notice-banner success visible";
      noticeBanner.innerHTML = `
        <strong>✓ Demo Mode – Client Validation Successful</strong>
        <p style="margin-top: 6px; margin-bottom: 12px;">
          All registration fields passed validation checks. <strong>Registration is simulated</strong> — no real account has been created and no credentials have been stored.
        </p>
        <div style="display: flex; gap: 10px; flex-wrap: wrap;">
          <a href="index.html" class="btn btn-primary btn-sm">Explore Main Website Demo →</a>
          <a href="login.html" class="btn btn-secondary btn-sm">Go to Login Demo</a>
        </div>
      `;
      noticeBanner.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }
  });
}

/**
 * Forgot Password Notice Handler
 */
function initForgotPassword() {
  const forgotLink = document.getElementById("forgotPasswordLink");
  const noticeBanner = document.getElementById("loginNoticeBanner");

  if (forgotLink && noticeBanner) {
    forgotLink.addEventListener("click", (e) => {
      e.preventDefault();
      noticeBanner.className = "auth-notice-banner info visible";
      noticeBanner.innerHTML = `
        <strong>ℹ️ Password Recovery Notice</strong>
        <p style="margin-top: 6px; margin-bottom: 0;">
          The automated password recovery service is not yet implemented. Account recovery will be enabled once backend mail routing services are connected.
        </p>
      `;
      noticeBanner.scrollIntoView({ behavior: "smooth", block: "nearest" });
    });
  }
}

/**
 * Sample Credentials Autofill for Demonstration
 */
function initSampleCredentials() {
  const autofillBtn = document.getElementById("fillSampleCredsBtn");
  const emailInput = document.getElementById("loginEmail");
  const passwordInput = document.getElementById("loginPassword");
  const emailError = document.getElementById("loginEmailError");
  const passwordError = document.getElementById("loginPasswordError");

  if (autofillBtn && emailInput && passwordInput) {
    autofillBtn.addEventListener("click", () => {
      emailInput.value = "demo.student@internshield.ai";
      passwordInput.value = "DemoPass2026!";
      clearError(emailInput, emailError);
      clearError(passwordInput, passwordError);
    });
  }
}
