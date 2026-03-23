/**
 * API Service Module
 * Handles all HTTP requests to the backend API with authentication
 * Note: Uses Auth.API_BASE_URL from auth.js
 */

/**
 * Make an authenticated fetch request
 * @param {string} url - API endpoint URL
 * @param {object} options - Fetch options
 * @returns {Promise<Response>} - Fetch response
 */
async function fetchWithAuth(url, options = {}) {
    const token = Auth.getToken();

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(url, {
        ...options,
        headers
    });

    // Handle 401 Unauthorized or 403 Forbidden - session expired
    if (response.status === 401 || response.status === 403) {
        Auth.handleSessionExpiry('Your session has expired. Please login again to continue.');
        throw new Error('Session expired');
    }

    return response;
}

/**
 * Parse API error response
 * @param {Response} response - Fetch response
 * @returns {Promise<string>} - Error message
 */
async function parseError(response) {
    try {
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const data = await response.json();
            return data.message || data.errorMsg || data.error || 'An error occurred';
        }
        return await response.text() || 'An error occurred';
    } catch (e) {
        return 'An error occurred';
    }
}

/**
 * Get user expenses with optional month filter
 * @param {string} yearMonth - Optional filter in format 'YYYY-MM'
 * @returns {Promise<object>} - { userExpenses: [], totalMonthlyExpense: number }
 */
async function getExpenses(yearMonth = null) {
    let url = `${Auth.API_BASE_URL}/api/expenses`;
    if (yearMonth) {
        url += `?yearMonth=${yearMonth}`;
    }

    const response = await fetchWithAuth(url, {
        method: 'GET'
    });

    if (!response.ok) {
        throw new Error(await parseError(response));
    }

    return await response.json();
}

/**
 * Add new expenses
 * @param {Array<object>} expenses - Array of expense objects
 * @returns {Promise<Array>} - Array of created expense objects
 */
async function addExpenses(expenses) {
    const response = await fetchWithAuth(`${Auth.API_BASE_URL}/api/expenses`, {
        method: 'POST',
        body: JSON.stringify(expenses)
    });

    if (!response.ok) {
        throw new Error(await parseError(response));
    }

    return await response.json();
}

/**
 * Update an existing expense
 * @param {number} id - Expense ID
 * @param {object} expense - Updated expense data
 * @returns {Promise<object>} - Updated expense object
 */
async function updateExpense(id, expense) {
    const response = await fetchWithAuth(`${Auth.API_BASE_URL}/api/expenses/${id}`, {
        method: 'PUT',
        body: JSON.stringify(expense)
    });

    if (!response.ok) {
        throw new Error(await parseError(response));
    }

    return await response.json();
}

/**
 * Delete an expense
 * @param {number} id - Expense ID
 * @returns {Promise<void>}
 */
async function deleteExpense(id) {
    const response = await fetchWithAuth(`${Auth.API_BASE_URL}/api/expenses/${id}`, {
        method: 'DELETE'
    });

    if (!response.ok) {
        throw new Error(await parseError(response));
    }
}

/**
 * Change user password
 * @param {string} newPassword - New password
 * @returns {Promise<string>} - Success message
 */
async function changePassword(newPassword) {
    const response = await fetchWithAuth(`${Auth.API_BASE_URL}/user/changePass`, {
        method: 'POST',
        headers: {
            'Content-Type': 'text/plain'
        },
        body: newPassword
    });

    if (!response.ok) {
        throw new Error(await parseError(response));
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        return await response.json();
    }
    return await response.text();
}

// Export for use in other modules
window.API = {
    getExpenses,
    addExpenses,
    updateExpense,
    deleteExpense,
    changePassword
};
