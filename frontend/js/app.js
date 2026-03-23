/**
 * Dashboard Application Module
 * Handles expense management, rendering, and user interactions
 */

// State
let expenses = [];
let totalMonthlyExpense = 0;
let currentEditId = null;
let currentYearMonth = null;

// DOM Elements (initialized on DOMContentLoaded)
let elements = {};

/**
 * Initialize the dashboard application
 */
function initDashboard() {
    // Protect route - redirect if not authenticated
    if (!Auth.protectRoute()) return;

    // Cache DOM elements
    cacheElements();

    // Set up user info
    setupUserInfo();

    // Set up event listeners
    setupEventListeners();

    // Initialize month filter
    initMonthFilter();

    // Load expenses
    loadExpenses();
}

/**
 * Cache frequently used DOM elements
 */
function cacheElements() {
    elements = {
        // Stats
        totalExpenseValue: document.getElementById('totalExpenseValue'),
        expenseCount: document.getElementById('expenseCount'),

        // Table
        expenseTableBody: document.getElementById('expenseTableBody'),
        emptyState: document.getElementById('emptyState'),

        // Filter
        monthFilter: document.getElementById('monthFilter'),

        // User
        userInitial: document.getElementById('userInitial'),

        // Modals
        addModal: document.getElementById('addExpenseModal'),
        editModal: document.getElementById('editExpenseModal'),
        passwordModal: document.getElementById('passwordModal'),
        deleteModal: document.getElementById('deleteModal'),

        // Forms
        addForm: document.getElementById('addExpenseForm'),
        editForm: document.getElementById('editExpenseForm'),
        passwordForm: document.getElementById('passwordForm'),

        // Alerts
        addAlert: document.getElementById('addExpenseAlert'),
        editAlert: document.getElementById('editExpenseAlert'),
        passwordAlert: document.getElementById('passwordAlert'),

        // Buttons
        logoutBtn: document.getElementById('logoutBtn'),
        changePasswordBtn: document.getElementById('changePasswordBtn'),
        addExpenseBtn: document.getElementById('addExpenseBtn'),
        confirmDeleteBtn: document.getElementById('confirmDeleteBtn')
    };
}

/**
 * Set up user information display
 */
function setupUserInfo() {
    const user = Auth.getUserFromToken();
    if (user && user.email) {
        elements.userInitial.textContent = user.email.charAt(0).toUpperCase();
    }
}

/**
 * Set up event listeners
 */
function setupEventListeners() {
    // Logout
    elements.logoutBtn.addEventListener('click', () => Auth.logout());

    // Change password button
    elements.changePasswordBtn.addEventListener('click', () => openModal('passwordModal'));

    // Add expense button
    elements.addExpenseBtn.addEventListener('click', () => openModal('addExpenseModal'));

    // Month filter change
    elements.monthFilter.addEventListener('change', (e) => {
        currentYearMonth = e.target.value || null;
        loadExpenses();
    });

    // Add expense form
    elements.addForm.addEventListener('submit', handleAddExpense);

    // Edit expense form
    elements.editForm.addEventListener('submit', handleEditExpense);

    // Password form
    elements.passwordForm.addEventListener('submit', handleChangePassword);

    // Delete confirmation
    elements.confirmDeleteBtn.addEventListener('click', handleDeleteExpense);

    // Modal close buttons
    document.querySelectorAll('.modal-close, [data-close-modal]').forEach(btn => {
        btn.addEventListener('click', () => {
            closeAllModals();
        });
    });

    // Close modal on overlay click
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) {
                closeAllModals();
            }
        });
    });

    // Close modal on Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeAllModals();
        }
    });
}

/**
 * Initialize month filter with options
 */
function initMonthFilter() {
    const select = elements.monthFilter;
    const now = new Date();

    // Add current month and previous 11 months
    for (let i = 0; i < 12; i++) {
        const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
        const value = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
        const label = date.toLocaleDateString('en-US', { year: 'numeric', month: 'long' });

        const option = document.createElement('option');
        option.value = value;
        option.textContent = label;
        select.appendChild(option);
    }

    // Set current month as default
    const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    select.value = currentMonth;
    currentYearMonth = currentMonth;
}

/**
 * Load expenses from API
 */
async function loadExpenses() {
    try {
        showTableLoading();

        const data = await API.getExpenses(currentYearMonth);
        expenses = data.userExpenses || [];
        totalMonthlyExpense = data.totalMonthlyExpense || 0;

        renderExpenses();
        updateStats();
    } catch (error) {
        console.error('Failed to load expenses:', error);
        showTableError(error.message);
    }
}

/**
 * Render expenses table
 */
function renderExpenses() {
    const tbody = elements.expenseTableBody;

    if (expenses.length === 0) {
        tbody.innerHTML = '';
        elements.emptyState.classList.remove('hidden');
        return;
    }

    elements.emptyState.classList.add('hidden');

    tbody.innerHTML = expenses.map(expense => `
        <tr data-id="${expense.id}">
            <td>${escapeHtml(expense.description)}</td>
            <td>
                <span class="expense-category">
                    ${getCategoryIcon(expense.category)}
                    ${escapeHtml(expense.category)}
                </span>
            </td>
            <td class="expense-amount">₹${formatNumber(expense.amount)}</td>
            <td>${formatDate(expense.date)}</td>
            <td>
                <div class="expense-actions">
                    <button class="btn btn-ghost btn-sm" onclick="openEditModal(${expense.id})" title="Edit">
                        ✏️
                    </button>
                    <button class="btn btn-ghost btn-sm" onclick="openDeleteModal(${expense.id})" title="Delete">
                        🗑️
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

/**
 * Update statistics display
 */
function updateStats() {
    elements.totalExpenseValue.textContent = `₹${formatNumber(totalMonthlyExpense)}`;
    elements.expenseCount.textContent = expenses.length;
}

/**
 * Show loading state in table
 */
function showTableLoading() {
    elements.expenseTableBody.innerHTML = `
        <tr>
            <td colspan="5" class="text-center" style="padding: 2rem;">
                <div class="spinner" style="margin: 0 auto;"></div>
            </td>
        </tr>
    `;
    elements.emptyState.classList.add('hidden');
}

/**
 * Show error state in table
 */
function showTableError(message) {
    elements.expenseTableBody.innerHTML = `
        <tr>
            <td colspan="5" class="text-center text-danger" style="padding: 2rem;">
                Failed to load expenses: ${escapeHtml(message)}
            </td>
        </tr>
    `;
}

/**
 * Handle add expense form submission
 */
async function handleAddExpense(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const expense = {
        description: formData.get('description'),
        categoryName: formData.get('category'),
        amount: parseFloat(formData.get('amount')),
        date: formData.get('date')
    };

    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;

    try {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner"></span> Adding...';
        hideAlert(elements.addAlert);

        await API.addExpenses([expense]);

        closeAllModals();
        e.target.reset();
        await loadExpenses();

    } catch (error) {
        showAlert(elements.addAlert, error.message, 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    }
}

/**
 * Open edit modal with expense data
 */
function openEditModal(id) {
    const expense = expenses.find(e => e.id === id);
    if (!expense) return;

    currentEditId = id;

    document.getElementById('editDescription').value = expense.description;
    document.getElementById('editCategory').value = expense.category;
    document.getElementById('editAmount').value = expense.amount;
    document.getElementById('editDate').value = expense.date;

    openModal('editExpenseModal');
}

/**
 * Handle edit expense form submission
 */
async function handleEditExpense(e) {
    e.preventDefault();

    if (!currentEditId) return;

    const formData = new FormData(e.target);
    const expense = {
        description: formData.get('description'),
        categoryName: formData.get('category'),
        amount: parseFloat(formData.get('amount')),
        date: formData.get('date')
    };

    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;

    try {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner"></span> Saving...';
        hideAlert(elements.editAlert);

        await API.updateExpense(currentEditId, expense);

        closeAllModals();
        currentEditId = null;
        await loadExpenses();

    } catch (error) {
        showAlert(elements.editAlert, error.message, 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    }
}

/**
 * Open delete confirmation modal
 */
function openDeleteModal(id) {
    currentEditId = id;
    openModal('deleteModal');
}

/**
 * Handle delete expense
 */
async function handleDeleteExpense() {
    if (!currentEditId) return;

    const btn = elements.confirmDeleteBtn;
    const originalText = btn.innerHTML;

    try {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span> Deleting...';

        await API.deleteExpense(currentEditId);

        closeAllModals();
        currentEditId = null;
        await loadExpenses();

    } catch (error) {
        alert('Failed to delete expense: ' + error.message);
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
}

/**
 * Handle change password form submission
 */
async function handleChangePassword(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const newPassword = formData.get('newPassword');
    const confirmPassword = formData.get('confirmPassword');

    if (newPassword !== confirmPassword) {
        showAlert(elements.passwordAlert, 'Passwords do not match', 'error');
        return;
    }

    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;

    try {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner"></span> Changing...';
        hideAlert(elements.passwordAlert);

        await API.changePassword(newPassword);

        showAlert(elements.passwordAlert, 'Password changed successfully!', 'success');
        e.target.reset();

        setTimeout(() => {
            closeAllModals();
        }, 1500);

    } catch (error) {
        showAlert(elements.passwordAlert, error.message, 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    }
}

// ============================================
// MODAL FUNCTIONS
// ============================================

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
}

function closeAllModals() {
    document.querySelectorAll('.modal-overlay').forEach(modal => {
        modal.classList.remove('active');
    });
    document.body.style.overflow = '';

    // Clear alerts
    document.querySelectorAll('.alert').forEach(alert => {
        alert.classList.remove('show');
    });
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

function showAlert(element, message, type = 'error') {
    element.textContent = message;
    element.className = `alert alert-${type} show`;
}

function hideAlert(element) {
    element.classList.remove('show');
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatNumber(num) {
    return new Intl.NumberFormat('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(num);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function getCategoryIcon(category) {
    const icons = {
        'Food': '🍔',
        'Transport': '🚗',
        'Shopping': '🛍️',
        'Entertainment': '🎮',
        'Bills': '📄',
        'Health': '💊',
        'Education': '📚',
        'Travel': '✈️',
        'Other': '📦'
    };
    return icons[category] || '📦';
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', initDashboard);

// Expose functions for inline event handlers
window.openEditModal = openEditModal;
window.openDeleteModal = openDeleteModal;
