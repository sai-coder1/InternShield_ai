/**
 * InternShield AI - Supabase Client Configuration
 * Initializes the Supabase v2 JavaScript SDK using the public publishable anon key.
 * Provides resilient session retrieval with proactive token refresh.
 */

const SUPABASE_URL = "https://voljvzvcoptqadeeymjj.supabase.co";
const SUPABASE_ANON_KEY = "sb_publishable_720rHpnaBsofC0xpDNdAxw_DzOG3qCz";

let supabaseClient = null;

if (window.supabase && typeof window.supabase.createClient === "function") {
  supabaseClient = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: true
    }
  });
} else {
  console.warn("Supabase library not loaded. Ensure the CDN script is included before supabaseClient.js.");
}

/**
 * Retrieves a valid, non-expired Supabase access token.
 * Proactively refreshes the session if expired or close to expiry (within 60s).
 * Returns null if no authenticated user session exists.
 *
 * @returns {Promise<string|null>} The valid JWT access token or null
 */
async function getValidAccessToken() {
  if (!supabaseClient || !supabaseClient.auth) {
    return null;
  }

  try {
    const { data, error } = await supabaseClient.auth.getSession();
    if (error || !data || !data.session) {
      return null;
    }

    let session = data.session;
    const nowSec = Math.floor(Date.now() / 1000);

    // If access token is expired or within 60s of expiring, proactively refresh
    if (session.expires_at && session.expires_at <= nowSec + 60) {
      console.log("Supabase session token expiring soon; refreshing...");
      const { data: refreshData, error: refreshError } = await supabaseClient.auth.refreshSession();
      if (refreshError || !refreshData?.session) {
        console.warn("Failed to refresh Supabase session:", refreshError?.message);
        // Clear stale session
        try { await supabaseClient.auth.signOut(); } catch (_) {}
        return null;
      }
      session = refreshData.session;
    }

    return session.access_token || null;
  } catch (err) {
    console.warn("Error checking Supabase session:", err);
    return null;
  }
}

/**
 * Force refresh the Supabase session, returning the fresh access token.
 *
 * @returns {Promise<string|null>}
 */
async function refreshAuthSession() {
  if (!supabaseClient || !supabaseClient.auth) {
    return null;
  }

  try {
    const { data, error } = await supabaseClient.auth.refreshSession();
    if (error || !data?.session) {
      return null;
    }
    return data.session.access_token || null;
  } catch (err) {
    console.warn("Failed to force refresh session:", err);
    return null;
  }
}

// Expose globally
window.supabaseClient = supabaseClient;
window.getValidAccessToken = getValidAccessToken;
window.refreshAuthSession = refreshAuthSession;
