/* =========================================================
   Shared JavaScript used by every page:
   talking to the backend, login check, sidebar,
   messages, formatting and validation.
   ========================================================= */

// ---------- Talking to the backend ----------

// Calls /api/... and returns the JSON reply.
// Throws an Error with the server's message if something goes wrong.
async function api(path, options = {}) {
    const settings = { ...options };
    if (!(settings.body instanceof FormData)) {
        settings.headers = { 'Content-Type': 'application/json', ...(settings.headers || {}) };
    }
    const res = await fetch('/api' + path, settings);

    if (res.status === 401 && path !== '/auth/login') {
        sessionStorage.removeItem('staff');
        window.location = 'index.html';
        throw new Error('Please log in again');
    }

    const data = await res.json().catch(() => null);
    if (!res.ok) {
        const err = new Error(data && data.error ? data.error : 'Something went wrong. Please try again.');
        err.status = res.status;
        err.data = data;
        throw err;
    }
    return data;
}

// Downloads a file (Excel or PDF) from the backend.
async function downloadFile(path, fileName) {
    const res = await fetch('/api' + path);
    if (res.status === 401) {
        sessionStorage.removeItem('staff');
        window.location = 'index.html';
        return;
    }
    if (!res.ok) {
        const data = await res.json().catch(() => null);
        throw new Error(data && data.error ? data.error : 'The download failed. Please try again.');
    }
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
}

// ---------- Login and sidebar ----------

function requireLogin() {
    if (!sessionStorage.getItem('staff')) {
        window.location = 'index.html';
    }
}

async function logout() {
    try {
        await api('/auth/logout', { method: 'POST' });
    } catch (err) {
        // even if the server call fails, leave the app
    }
    sessionStorage.removeItem('staff');
    window.location = 'index.html';
}

// Draws the same sidebar on every page and highlights the current page
function renderSidebar(active) {
    const pages = [
        ['dashboard', 'Dashboard'],
        ['proposal', 'Budget Proposal'],
        ['expenses', 'Expenses'],
        ['reports', 'Reports']
    ];
    let html = '<div class="brand"><img class="brand-mark" src="image.png" alt="PSGiTech logo"><span>PSGiTech Budget System</span></div><nav>';
    for (const [file, label] of pages) {
        const cls = file === active ? 'active' : '';
        html += `<a href="${file}.html" class="${cls}">${label}</a>`;
    }
    html += '</nav>';
    html += '<div class="sidebar-footer">Signed in as <strong>' + escapeHtml(sessionStorage.getItem('staff') || '') +
            '</strong><br><button class="link-btn" onclick="logout()">Log out</button></div>';
    document.getElementById('sidebar').innerHTML = html;
}

// ---------- Messages ----------

// Shows a red message under a field: the element id must be fieldId + '-error'
function showError(fieldId, message) {
    const el = document.getElementById(fieldId + '-error');
    if (el) el.textContent = message;
}

function clearErrors() {
    document.querySelectorAll('.error').forEach(el => el.textContent = '');
}

// type: 'success', 'error' or 'info'
function showMessage(elementId, text, type) {
    document.getElementById(elementId).innerHTML =
        `<div class="alert alert-${type}">${escapeHtml(text)}</div>`;
}

function clearMessage(elementId) {
    document.getElementById(elementId).innerHTML = '';
}

// Stops typed text from being treated as HTML (protects against script injection)
function escapeHtml(text) {
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// ---------- Formatting ----------

// 120000 -> ₹1,20,000.00
function formatRupees(n) {
    return Number(n).toLocaleString('en-IN', { style: 'currency', currency: 'INR' });
}

// "2026-05-14" -> "14 May 2026"
function formatDate(iso) {
    if (!iso) return '';
    const d = new Date(iso.substring(0, 10) + 'T00:00:00');
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

// "2026-05" -> "May 2026"
function formatMonth(yearMonth) {
    const [y, m] = yearMonth.split('-');
    return new Date(Number(y), Number(m) - 1, 1).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
}

// "HALF_YEARLY" -> "Half-yearly"
function formatFrequency(f) {
    const names = { MONTHLY: 'Monthly', QUARTERLY: 'Quarterly', HALF_YEARLY: 'Half-yearly', YEARLY: 'Yearly' };
    return names[f] || '';
}

// Today's date as "YYYY-MM-DD"
function todayIso() {
    const d = new Date();
    const pad = n => String(n).padStart(2, '0');
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

// The academic year (April to March) that today falls in, e.g. { start: "2026-04-01", label: "2026-27" }
function currentAcademicYear() {
    const d = new Date();
    const startYear = d.getMonth() >= 3 ? d.getFullYear() : d.getFullYear() - 1;
    return {
        start: startYear + '-04-01',
        label: startYear + '-' + String((startYear + 1) % 100).padStart(2, '0')
    };
}

// ---------- Validation (same rules as the Java code) ----------

// Digits only, up to 2 decimals: rejects letters, symbols and minus signs
function isAmount(v)         { return /^\d+(\.\d{1,2})?$/.test(v); }
function isPositiveAmount(v) { return isAmount(v) && Number(v) > 0; }
function isValidReceiptNo(v) { return /^[A-Za-z0-9-]+$/.test(v); }
function isMeaningfulText(v) { return v.trim().length > 0 && /[A-Za-z]/.test(v); }
function isFutureDate(d)     { return d > todayIso(); }
function isAcademicYear(v)   {
    if (!/^\d{4}-\d{2}$/.test(v)) return false;
    return (Number(v.substring(0, 4)) + 1) % 100 === Number(v.substring(5));
}
