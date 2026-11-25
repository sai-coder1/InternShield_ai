/**
 * InternShield AI - Supabase Client Configuration
 * Initializes the Supabase v2 JavaScript SDK using the public publishable anon key.
 * Provides resilient initialization with lazy fallback and safe session token retrieval.
 */

const SUPABASE_URL = "https://voljvzvcoptqadeeymjj.supabase.co";
const SUPABASE_ANON_KEY = "sb_publishable_720rHpnaBsofC0xpDNdAxw_DzOG3qCz";

let _supabaseClient = null;

/**
 * Initializes and returns the Supabase client instance.
 * Safe to call multiple times.
 */
function initSupabaseClient() {
  if (_supabaseClient) {
    return _supabaseClient;
  }

  const sdk = window.supabase;
  if (sdk && typeof sdk.createClient === "function") {
    try {
      _supabaseClient = sdk.createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
        auth: {
          persistSession: true,
          autoRefreshToken: true,
          detectSessionInUrl: true
        }
      });
      return _supabaseClient;
    } catch (err) {
      console.error("Error creating Supabase client:", err);
    }
  } else {
    console.warn("Supabase SDK is not yet available on window.supabase.");
  }
  return null;
}

// Initial eager initialization attempt
initSupabaseClient();

// Define dynamic accessor so window.supabaseClient always resolves whenever accessed
try {
  Object.defineProperty(window, "supabaseClient", {
    get: function() {
      return _supabaseClient || initSupabaseClient();
    },
    set: function(val) {
      _supabaseClient = val;
    },
    configurable: true,
    enumerable: true
  });
} catch (_) {
  window.supabaseClient = _supabaseClient;
}

/**
 * Retrieves a valid, non-expired Supabase access token.
 * Only attempts refresh if an active session already exists and is close to expiring.
 * Returns null if no authenticated user session exists.
 *
 * @returns {Promise<string|null>} The valid JWT access token or null
 */
async function getValidAccessToken() {
  const client = window.supabaseClient;
  if (!client || !client.auth) {
    return null;
  }

  try {
    const { data, error } = await client.auth.getSession();
    if (error || !data || !data.session) {
      return null;
    }

    let session = data.session;
    const nowSec = Math.floor(Date.now() / 1000);

    // If an existing session token is expired or within 60s of expiring, proactively refresh
    if (session.expires_at && session.expires_at <= nowSec + 60) {
      console.log("Supabase session token expiring soon; refreshing...");
      const { data: refreshData, error: refreshError } = await client.auth.refreshSession();
      if (refreshError || !refreshData?.session) {
        console.warn("Failed to refresh Supabase session:", refreshError?.message);
        try { await client.auth.signOut(); } catch (_) {}
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
 * Proactively refresh the Supabase session, returning the fresh access token.
 * Only attempts refresh if an active session exists.
 *
 * @returns {Promise<string|null>}
 */
async function refreshAuthSession() {
  const client = window.supabaseClient;
  if (!client || !client.auth) {
    return null;
  }

  try {
    const { data: sessionData } = await client.auth.getSession();
    if (!sessionData?.session) {
      // No active session exists, do not attempt refresh
      return null;
    }

    const { data, error } = await client.auth.refreshSession();
    if (error || !data?.session) {
      return null;
    }
    return data.session.access_token || null;
  } catch (err) {
    console.warn("Failed to refresh session:", err);
    return null;
  }
}

// Expose helpers globally
window.initSupabaseClient = initSupabaseClient;
window.getValidAccessToken = getValidAccessToken;
window.refreshAuthSession = refreshAuthSession;
