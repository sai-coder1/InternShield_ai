/**
 * InternShield AI - Authentication & Form Validation Controller
 * Powered by Supabase Authentication (v2 SDK).
 * Handles sign up, sign in, sign out, password toggles, and dynamic auth state.
 */

document.addEventListener("DOMContentLoaded", () => {
  initPasswordToggles();
  initLoginForm();
  initRegisterForm();
  initForgotPassword();
  initSampleCredentials();
  initSignOutButtons();
  initAuthStateListener();
});

// Standard email format validation regex
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
 * Helper to render notice banner messages (error / success / info)
 */
function showBanner(bannerEl, type, messageHtml) {
  if (!bannerEl) return;
  bannerEl.className = `auth-notice-banner ${type} visible`;
  bannerEl.innerHTML = messageHtml;
  bannerEl.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

/**
 * Login Form Controller - Powered by Supabase signInWithPassword()
 */
function initLoginForm() {
  const form = document.getElementById("loginForm");
  if (!form) return;

  const emailInput = document.getElementById("loginEmail");
  const emailError = document.getElementById("loginEmailError");
  const passwordInput = document.getElementById("loginPassword");
  const passwordError = document.getElementById("loginPasswordError");
  const submitBtn = document.getElementById("loginSubmitBtn");
  const noticeBanner = document.getElementById("loginNoticeBanner");

  if (emailInput) {
    emailInput.addEventListener("input", () => clearError(emailInput, emailError));
  }
  if (passwordInput) {
    passwordInput.addEventListener("input", () => clearError(passwordInput, passwordError));
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    let isValid = true;

    if (noticeBanner) {
      noticeBanner.classList.remove("visible", "success", "error", "info");
      noticeBanner.innerHTML = "";
    }

    const emailVal = emailInput ? emailInput.value.trim() : "";
    if (!emailVal) {
      showError(emailInput, emailError, "Email address is required.");
      isValid = false;
    } else if (!EMAIL_REGEX.test(emailVal)) {
      showError(emailInput, emailError, "Please enter a valid email address.");
      isValid = false;
    } else {
      clearError(emailInput, emailError);
    }

    const passwordVal = passwordInput ? passwordInput.value : "";
    if (!passwordVal) {
      showError(passwordInput, passwordError, "Password is required.");
      isValid = false;
    } else {
      clearError(passwordInput, passwordError);
    }

    if (!isValid) return;

    // Check Supabase initialization
    if (!window.supabaseClient) {
      showBanner(noticeBanner, "error", "Authentication service is temporarily unavailable. Please try again later.");
      return;
    }

    // Set loading state
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = "Signing In...";
    }

    try {
      const { data, error } = await window.supabaseClient.auth.signInWithPassword({
        email: emailVal,
        password: passwordVal
      });

      if (error) {
        showBanner(noticeBanner, "error", `<strong>Sign In Failed:</strong> ${error.message}`);
        if (submitBtn) {
          submitBtn.disabled = false;
          submitBtn.textContent = "Sign In";
        }
      } else {
        const userName = data.user?.user_metadata?.full_name || data.user?.email || "Student";
        showBanner(noticeBanner, "success", `<strong>Welcome back, ${userName}!</strong> Signing in...`);
        setTimeout(() => {
          window.location.href = "index.html";
        }, 800);
      }
    } catch (err) {
      showBanner(noticeBanner, "error", `<strong>Error:</strong> ${err.message || "An unexpected error occurred."}`);
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = "Sign In";
      }
    }
  });
}

/**
 * Registration Form Controller - Powered by Supabase signUp()
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
  const submitBtn = document.getElementById("registerSubmitBtn");
  const noticeBanner = document.getElementById("registerNoticeBanner");

  if (nameInput) nameInput.addEventListener("input", () => clearError(nameInput, nameError));
  if (emailInput) emailInput.addEventListener("input", () => clearError(emailInput, emailError));
  if (passwordInput) passwordInput.addEventListener("input", () => clearError(passwordInput, passwordError));
  if (confirmPasswordInput) confirmPasswordInput.addEventListener("input", () => clearError(confirmPasswordInput, confirmPasswordError));
  if (termsCheckbox) termsCheckbox.addEventListener("change", () => clearError(null, termsError));

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    let isValid = true;

    if (noticeBanner) {
      noticeBanner.classList.remove("visible", "success", "error", "info");
      noticeBanner.innerHTML = "";
    }

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

    const emailVal = emailInput ? emailInput.value.trim() : "";
    if (!emailVal) {
      showError(emailInput, emailError, "Email address is required.");
      isValid = false;
    } else if (!EMAIL_REGEX.test(emailVal)) {
      showError(emailInput, emailError, "Please enter a valid email address.");
      isValid = false;
    } else {
      clearError(emailInput, emailError);
    }

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

    if (termsCheckbox && !termsCheckbox.checked) {
      showError(null, termsError, "You must agree to the Terms of Service and Privacy Policy.");
      isValid = false;
    } else {
      clearError(null, termsError);
    }

    if (!isValid) return;

    if (!window.supabaseClient) {
      showBanner(noticeBanner, "error", "Authentication service is temporarily unavailable. Please try again later.");
      return;
    }

    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = "Creating Account...";
    }

    try {
      // Connect to Supabase signUp with user's full name in options.data
      const { data, error } = await window.supabaseClient.auth.signUp({
        email: emailVal,
        password: passwordVal,
        options: {
          data: {
            full_name: nameVal
          }
        }
      });

      if (error) {
        showBanner(noticeBanner, "error", `<strong>Registration Failed:</strong> ${error.message}`);
        if (submitBtn) {
          submitBtn.disabled = false;
          submitBtn.textContent = "Create Account";
        }
      } else {
        const user = data.user;
        // Case: Duplicate account where email confirmation is on
        if (user && user.identities && user.identities.length === 0) {
          showBanner(noticeBanner, "error", "An account with this email address already exists. <a href='login.html' style='color:var(--blue);font-weight:700;'>Sign In instead</a>.");
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = "Create Account";
          }
        } else if (data.session) {
          // Auto-confirmed / instant session
          showBanner(noticeBanner, "success", `<strong>Account Created!</strong> Welcome, ${nameVal}! Redirecting to homepage...`);
          setTimeout(() => {
            window.location.href = "index.html";
          }, 1000);
        } else {
          // Email confirmation link sent
          showBanner(noticeBanner, "success", `
            <strong>Account Registered!</strong>
            <p style="margin-top: 6px; margin-bottom: 0;">
              A confirmation email has been sent to <strong>${emailVal}</strong>. Please check your inbox to activate your account.
            </p>
          `);
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = "Create Account";
          }
        }
      }
    } catch (err) {
      showBanner(noticeBanner, "error", `<strong>Error:</strong> ${err.message || "An unexpected error occurred."}`);
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = "Create Account";
      }
    }
  });
}

/**
 * Forgot Password Notice / Handler
 */
function initForgotPassword() {
  const forgotLink = document.getElementById("forgotPasswordLink");
  const emailInput = document.getElementById("loginEmail");
  const noticeBanner = document.getElementById("loginNoticeBanner");

  if (forgotLink && noticeBanner) {
    forgotLink.addEventListener("click", async (e) => {
      e.preventDefault();
      const emailVal = emailInput ? emailInput.value.trim() : "";

      if (!emailVal || !EMAIL_REGEX.test(emailVal)) {
        showBanner(noticeBanner, "info", "Please type your email address into the Email field above, then click 'Forgot password?' to receive a reset link.");
        if (emailInput) emailInput.focus();
        return;
      }

      if (!window.supabaseClient) {
        showBanner(noticeBanner, "error", "Authentication service is unavailable.");
        return;
      }

      try {
        const { error } = await window.supabaseClient.auth.resetPasswordForEmail(emailVal);
        if (error) {
          showBanner(noticeBanner, "error", `Could not send reset email: ${error.message}`);
        } else {
          showBanner(noticeBanner, "success", `Password reset email sent to <strong>${emailVal}</strong>. Please check your inbox.`);
        }
      } catch (err) {
        showBanner(noticeBanner, "error", err.message || "Failed to request password reset.");
      }
    });
  }
}

/**
 * Sample Credentials Autofill for Demonstration / Testing
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

/**
 * Connects all Sign Out buttons to supabase.auth.signOut()
 */
function initSignOutButtons() {
  const signoutButtons = document.querySelectorAll(".auth-signout-btn");
  signoutButtons.forEach(btn => {
    btn.addEventListener("click", async (e) => {
      e.preventDefault();
      if (!window.supabaseClient) return;

      try {
        await window.supabaseClient.auth.signOut();
        // Reload or redirect to refresh auth state
        window.location.reload();
      } catch (err) {
        console.error("Sign out error:", err);
      }
    });
  });
}

/**
 * Supabase Auth State Change Listener
 * Keeps navigation bars synchronized across index.html, login.html, and register.html
 */
function initAuthStateListener() {
  if (!window.supabaseClient) return;

  // 1. Initial session check on page load
  window.supabaseClient.auth.getSession().then(({ data: { session } }) => {
    updateNavAuthUI(session ? session.user : null);
  }).catch(err => {
    console.warn("Could not retrieve initial session:", err);
  });

  // 2. Real-time auth state subscription
  window.supabaseClient.auth.onAuthStateChange((event, session) => {
    updateNavAuthUI(session ? session.user : null);
  });
}

/**
 * Helper to toggle Guest vs Authenticated UI elements in headers and navigation
 */
function updateNavAuthUI(user) {
  const guestElements = document.querySelectorAll(".nav-auth-guest");
  const userElements = document.querySelectorAll(".nav-auth-user");
  const userNameDisplays = document.querySelectorAll(".user-name-display");

  if (user) {
    // Determine friendly display name
    const fullName = user.user_metadata?.full_name 
      || user.user_metadata?.name 
      || (user.email ? user.email.split("@")[0] : "User");

    userNameDisplays.forEach(el => {
      el.textContent = fullName;
    });

    guestElements.forEach(el => {
      el.style.display = "none";
    });

    userElements.forEach(el => {
      if (el.parentElement && el.parentElement.classList.contains("nav-actions")) {
        el.style.display = "inline-flex";
      } else {
        el.style.display = "";
      }
    });
  } else {
    guestElements.forEach(el => {
      el.style.display = "";
    });

    userElements.forEach(el => {
      el.style.display = "none";
    });
  }
}
