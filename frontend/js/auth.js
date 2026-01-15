/**
 * Authentication Module
 * Handles JWT token management, login, registration, and session handling
 */

const AUTH_TOKEN_KEY = 'expense_tracker_token';
const API_BASE_URL = 'http://localhost:9090';

/**
 * Store JWT token in localStorage
 * @param {string} token - JWT token to store
 */
function saveToken(token) {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
}

/**
 * Retrieve JWT token from localStorage
 * @returns {string|null} - Stored JWT token or null
 */
function getToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY);
}

/**
 * Remove JWT token from localStorage
 */
function removeToken() {
    localStorage.removeItem(AUTH_TOKEN_KEY);
}

/**
 * Check if user is authenticated (has valid token)
 * Supports both JWS (3-part signed) and JWE (5-part encrypted) tokens
 * @returns {boolean} - True if token exists and appears valid
 */
function isAuthenticated() {
    const token = getToken();
    if (!token) return false;

    const parts = token.split('.');

    // JWE tokens have 5 parts (encrypted) - we can't decode them client-side
    // Just check the token exists and let the server validate on API calls
    if (parts.length === 5) {
        return true; // Trust the token, server will validate on API calls
    }

    // JWS tokens have 3 parts (signed) - we can decode and check expiry
    if (parts.length === 3) {
        try {
            // JWT uses URL-safe base64: replace - with + and _ with /
            let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
            // Add padding if needed
            while (base64.length % 4) {
                base64 += '=';
            }

            const payload = JSON.parse(atob(base64));
            const expiry = payload.exp * 1000; // Convert to milliseconds

            if (Date.now() >= expiry) {
                removeToken();
                return false;
            }
            return true;
        } catch (e) {
            console.error('Error parsing JWT token:', e);
            removeToken();
            return false;
        }
    }

    // Unknown token format
    removeToken();
    return false;
}

/**
 * Get user info from JWT token
 * For JWE (encrypted) tokens, we can't decode the payload, so return limited info
 * @returns {object|null} - User info from token payload
 */
function getUserFromToken() {
    const token = getToken();
    if (!token) return null;

    const parts = token.split('.');

    // JWE tokens (5 parts) - encrypted, can't decode payload
    if (parts.length === 5) {
        // Return a placeholder - the actual user info would need to come from an API
        return {
            email: 'User',
            userId: null
        };
    }

    // JWS tokens (3 parts) - signed, can decode payload
    if (parts.length === 3) {
        try {
            // JWT uses URL-safe base64: replace - with + and _ with /
            let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
            // Add padding if needed
            while (base64.length % 4) {
                base64 += '=';
            }

            const payload = JSON.parse(atob(base64));
            return {
                email: payload.sub,
                userId: payload.userId
            };
        } catch (e) {
            console.error('Error parsing JWT for user info:', e);
            return null;
        }
    }

    return null;
}

/**
 * Login user with email and password
 * @param {string} email - User email
 * @param {string} password - User password
 * @returns {Promise<object>} - Login response with status and token
 */
async function login(email, password) {
    const response = await fetch(`${API_BASE_URL}/public/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, password })
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || data.errorMsg || 'Login failed');
    }

    if (data.authToken) {
        saveToken(data.authToken);
    }

    return data;
}

/**
 * Register a new user
 * @param {object} userData - User registration data
 * @param {string} userData.name - User name
 * @param {string} userData.email - User email
 * @param {string} userData.password - User password
 * @param {string} userData.mobileNumber - User mobile number
 * @returns {Promise<string>} - Success message
 */
async function register(userData) {
    const response = await fetch(`${API_BASE_URL}/public/register`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(userData)
    });

    // Registration returns a text message, not JSON
    const contentType = response.headers.get('content-type');
    let data;

    if (contentType && contentType.includes('application/json')) {
        data = await response.json();
    } else {
        data = await response.text();
    }

    if (!response.ok) {
        if (typeof data === 'object') {
            throw new Error(data.message || data.errorMsg || 'Registration failed');
        }
        throw new Error(data || 'Registration failed');
    }

    return data;
}

/**
 * Logout user - clear token and redirect to login
 */
function logout() {
    removeToken();
    window.location.href = 'index.html';
}

/**
 * Protect route - redirect to login if not authenticated
 * Call this on protected pages
 */
function protectRoute() {
    if (!isAuthenticated()) {
        window.location.href = 'index.html';
        return false;
    }
    return true;
}

/**
 * Redirect to dashboard if already authenticated
 * Call this on login page
 */
function redirectIfAuthenticated() {
    if (isAuthenticated()) {
        window.location.href = 'dashboard.html';
        return true;
    }
    return false;
}

// Export for use in other modules
window.Auth = {
    saveToken,
    getToken,
    removeToken,
    isAuthenticated,
    getUserFromToken,
    login,
    register,
    logout,
    protectRoute,
    redirectIfAuthenticated,
    API_BASE_URL
};
