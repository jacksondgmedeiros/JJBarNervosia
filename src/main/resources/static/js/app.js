const loginForm = document.getElementById('loginForm');
const loginFeedback = document.getElementById('loginFeedback');
const loginCard = document.getElementById('loginCard');
const appCard = document.getElementById('appCard');
const sessionInfo = document.getElementById('sessionInfo');

const form = document.getElementById('orderForm');
const stockForm = document.getElementById('stockForm');
const userForm = document.getElementById('userForm');
const waiterFeedback = document.getElementById('waiterFeedback');
const stockFeedback = document.getElementById('stockFeedback');
const adminFeedback = document.getElementById('adminFeedback');
const kitchenList = document.getElementById('kitchenList');
const cashierList = document.getElementById('cashierList');
const stockList = document.getElementById('stockList');
const usersList = document.getElementById('usersList');
const productSelect = document.getElementById('productSelect');
const unitPricePreview = document.getElementById('unitPricePreview');
const waiterOpenSessions = document.getElementById('waiterOpenSessions');
const waiterHistory = document.getElementById('waiterHistory');

let productsCache = [];
let cashierSessionsCache = [];
let waiterOpenCache = [];
let waiterHistoryCache = [];
let selectedSessionId = null;
let authUser = null;
let usersCache = [];

const cashierDetailModal = new bootstrap.Modal(document.getElementById('cashierDetailModal'));
const confirmCloseBtn = document.getElementById('confirmCloseBtn');

loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const payload = {
        role: document.getElementById('loginRole').value,
        username: document.getElementById('loginUsername').value,
        password: document.getElementById('loginPassword').value
    };

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.error || 'Falha de login');

        authUser = data;
        loginFeedback.innerHTML = '';
        loginCard.classList.add('d-none');
        appCard.classList.remove('d-none');
        sessionInfo.innerHTML = `<div><strong>${authUser.fullName}</strong></div><div class="small text-muted">${friendlyRole(authUser.role)}</div>`;
        configureTabsForRole();
        await refreshAllViews();
    } catch (error) {
        loginFeedback.innerHTML = `<div class="alert alert-danger">${error.message}</div>`;
    }
});

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const payload = {
        tableNumber: Number(document.getElementById('tableNumber').value),
        customerName: document.getElementById('customerName').value,
        productId: Number(productSelect.value),
        quantity: Number(document.getElementById('quantity').value),
        notes: document.getElementById('notes').value
    };

    try {
        const response = await apiFetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!response.ok) throw await toError(response, 'Erro ao lançar pedido.');

        waiterFeedback.innerHTML = '<div class="alert alert-success">Item adicionado na comanda. Envie a comanda quando terminar.</div>';
        document.getElementById('quantity').value = '1';
        document.getElementById('notes').value = '';
        await refreshAllViews();
    } catch (err) {
        waiterFeedback.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
    }
});

stockForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const productId = document.getElementById('editingProductId').value;
    const payload = {
        name: document.getElementById('stockName').value,
        category: document.getElementById('stockCategory').value,
        unitPrice: Number(document.getElementById('stockPrice').value)
    };

    const url = productId ? `/api/products/${productId}` : '/api/products';
    const method = productId ? 'PUT' : 'POST';

    try {
        const response = await apiFetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!response.ok) throw await toError(response, 'Erro ao salvar produto.');

        stockFeedback.innerHTML = '<div class="alert alert-success">Produto salvo com sucesso.</div>';
        clearStockForm();
        await loadProducts();
    } catch (err) {
        stockFeedback.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
    }
});

document.getElementById('cancelStockEdit').addEventListener('click', clearStockForm);

document.getElementById('waiterOpenSearch').addEventListener('input', renderWaiterOpenSessions);
document.getElementById('waiterClosedSearch').addEventListener('input', renderWaiterHistory);

userForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const userId = document.getElementById('editingUserId').value;
    const payload = {
        fullName: document.getElementById('userFullName').value,
        username: document.getElementById('userUsername').value,
        password: document.getElementById('userPassword').value,
        role: document.getElementById('userRole').value
    };

    const url = userId ? `/api/admin/users/${userId}` : '/api/admin/users';
    const method = userId ? 'PUT' : 'POST';

    const response = await apiFetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    if (!response.ok) {
        const error = await toError(response, 'Erro ao salvar usuário.');
        adminFeedback.innerHTML = `<div class="alert alert-danger">${error.message}</div>`;
        return;
    }

    adminFeedback.innerHTML = '<div class="alert alert-success">Usuário salvo.</div>';
    clearUserForm();
    await loadUsers();
});

document.getElementById('cancelUserEdit').addEventListener('click', clearUserForm);

productSelect.addEventListener('change', () => {
    const selected = productsCache.find((p) => p.id === Number(productSelect.value));
    unitPricePreview.value = selected ? `R$ ${Number(selected.unitPrice).toFixed(2)}` : '';
});

confirmCloseBtn.addEventListener('click', async () => {
    if (!selectedSessionId) return;
    await closeSession(selectedSessionId);
    cashierDetailModal.hide();
});

async function loadProducts() {
    const response = await apiFetch('/api/products');
    productsCache = await response.json();
    const previouslySelectedProductId = productSelect.value;

    if (productsCache.length === 0) {
        productSelect.innerHTML = '<option value="">Sem produtos em estoque</option>';
        unitPricePreview.value = '';
        stockList.innerHTML = '<div class="alert alert-warning">Cadastre produtos para liberar pedidos do garçom.</div>';
        return;
    }

    productSelect.innerHTML = productsCache.map((product) =>
        `<option value="${product.id}">${friendlyCategory(product.category)} • ${product.name}</option>`
    ).join('');

    const selectedStillExists = productsCache.some((product) => String(product.id) === previouslySelectedProductId);
    if (selectedStillExists) {
        productSelect.value = previouslySelectedProductId;
    }
    productSelect.dispatchEvent(new Event('change'));

    const grouped = groupBy(productsCache, 'category');
    stockList.innerHTML = Object.keys(grouped).map((category) => `
        <div class="mb-3">
            <h6 class="mb-2">${friendlyCategory(category)}</h6>
            <ul class="list-group">
                ${grouped[category].map((p) => `<li class="list-group-item d-flex justify-content-between align-items-center"><span>${p.name}</span><span><strong>R$ ${Number(p.unitPrice).toFixed(2)}</strong> ${canEditStock() ? `<button class="btn btn-sm btn-outline-primary ms-2" onclick="editProduct(${p.id})">Editar</button><button class="btn btn-sm btn-outline-danger ms-1" onclick="removeProduct(${p.id})">Excluir</button>` : ''}</span></li>`).join('')}
            </ul>
        </div>
    `).join('');
}

async function loadKitchen() {
    const response = await apiFetch('/api/kitchen/orders');
    const orders = await response.json();

    if (orders.length === 0) {
        kitchenList.innerHTML = '<div class="col-12"><div class="alert alert-info">Sem pedidos pendentes/preparo.</div></div>';
        return;
    }

    kitchenList.innerHTML = orders.map((order) => `
        <div class="col-md-6 col-lg-4">
            <div class="card h-100 border-warning">
                <div class="card-body">
                    <h5 class="card-title">Mesa ${order.tableNumber}</h5>
                    <p class="text-muted mb-2">Cliente: ${order.customerName}</p>
                    <ul class="list-group list-group-flush">
                        ${order.items.map((item) => `
                            <li class="list-group-item px-0">
                                <p class="mb-1"><strong>${item.itemName}</strong></p>
                                <p class="mb-1">Qtd: ${item.quantity}</p>
                                <p class="mb-1">Obs: ${item.notes || '-'}</p>
                                <p class="mb-2">Status: <span class="badge text-bg-${item.kitchenStatus === 'PREPARING' ? 'primary' : 'secondary'}">${friendlyKitchenStatus(item.kitchenStatus)}</span></p>
                                <div class="d-flex gap-2">
                                    <button class="btn btn-sm btn-outline-primary" onclick="updateKitchenStatus(${item.id}, 'PREPARING')">Preparando</button>
                                    <button class="btn btn-sm btn-success" onclick="updateKitchenStatus(${item.id}, 'DONE')">Concluído</button>
                                </div>
                            </li>
                        `).join('')}
                    </ul>
                </div>
            </div>
        </div>
    `).join('');
}

async function updateKitchenStatus(id, status) {
    await apiFetch(`/api/kitchen/orders/${id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status })
    });
    await refreshAllViews();
}

async function loadWaiterOpenSessions() {
    const response = await apiFetch('/api/waiter/sessions/open');
    waiterOpenCache = await response.json();
    renderWaiterOpenSessions();
}

function renderWaiterOpenSessions() {
    const term = document.getElementById('waiterOpenSearch').value.toLowerCase().trim();
    const sessions = waiterOpenCache.filter((session) => session.customerName.toLowerCase().includes(term)
        || String(session.tableNumber).includes(term));

    if (sessions.length === 0) {
        waiterOpenSessions.innerHTML = '<div class="col-12"><div class="alert alert-info">Nenhuma comanda em aberto para esse filtro.</div></div>';
        return;
    }

    waiterOpenSessions.innerHTML = sessions.map((session) => `
        <div class="col-lg-6">
            <div class="card h-100">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <h6 class="mb-1">Mesa ${session.tableNumber} • ${session.customerName}</h6>
                            <small class="text-muted">Garçom: #${session.waiterId} - ${session.waiterName}</small><br>
                            <small class="text-muted">Total: R$ ${Number(session.total).toFixed(2)}</small>
                        </div>
                        <span class="badge text-bg-${session.waiterFinalized ? 'success' : 'warning'}">${session.waiterFinalized ? 'Finalizada p/ caixa' : 'Em atendimento'}</span>
                    </div>
                    <ul class="list-group list-group-flush my-2">
                        ${session.items.map((item) => `<li class="list-group-item px-0">${item.quantity}x ${item.itemName} <span class="badge text-bg-${kitchenBadge(item.kitchenStatus)} ms-1">${friendlyKitchenStatus(item.kitchenStatus)}</span></li>`).join('')}
                    </ul>
                    <div class="d-flex flex-wrap gap-2">
                        <button class="btn btn-sm btn-primary" ${session.waiterFinalized ? 'disabled' : ''} onclick="sendSessionToKitchen(${session.sessionId})">Enviar novos itens para cozinha</button>
                        <button class="btn btn-sm btn-dark" ${session.waiterFinalized ? 'disabled' : ''} onclick="finalizeSession(${session.sessionId})">Finalizar e enviar ao caixa</button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

async function loadWaiterHistory() {
    const response = await apiFetch('/api/waiter/sessions/history/today');
    waiterHistoryCache = (await response.json()).filter((session) => session.waiterFinalized);
    renderWaiterHistory();
}

function renderWaiterHistory() {
    const term = document.getElementById('waiterClosedSearch').value.toLowerCase().trim();
    const sessions = waiterHistoryCache.filter((session) => session.customerName.toLowerCase().includes(term)
        || String(session.tableNumber).includes(term));

    if (sessions.length === 0) {
        waiterHistory.innerHTML = '<div class="col-12"><div class="alert alert-info">Sem comandas finalizadas para esse filtro.</div></div>';
        return;
    }

    waiterHistory.innerHTML = sessions.map((session) => `
        <div class="col-md-6 col-xl-4">
            <div class="border rounded p-2 bg-light-subtle h-100">
                <strong>Mesa ${session.tableNumber} • ${session.customerName}</strong>
                <div class="small">Garçom: #${session.waiterId} - ${session.waiterName}</div>
                <div class="small">Status: ${session.status}</div>
                <div class="small">Total: R$ ${Number(session.total).toFixed(2)}</div>
            </div>
        </div>
    `).join('');
}

async function sendSessionToKitchen(sessionId) {
    const response = await apiFetch(`/api/waiter/sessions/${sessionId}/send-to-kitchen`, { method: 'PATCH' });
    if (!response.ok) {
        const error = await response.json();
        waiterFeedback.innerHTML = `<div class="alert alert-danger">${error.error || 'Erro ao enviar comanda para cozinha.'}</div>`;
        return;
    }
    waiterFeedback.innerHTML = '<div class="alert alert-success">Itens enviados para cozinha em lote.</div>';
    await refreshAllViews();
}

async function finalizeSession(sessionId) {
    const response = await apiFetch(`/api/waiter/sessions/${sessionId}/finalize`, { method: 'PATCH' });
    if (!response.ok) {
        const error = await response.json();
        waiterFeedback.innerHTML = `<div class="alert alert-danger">${error.error || 'Erro ao finalizar comanda.'}</div>`;
        return;
    }
    waiterFeedback.innerHTML = '<div class="alert alert-success">Comanda enviada para o caixa.</div>';
    await refreshAllViews();
}

async function loadCashier() {
    const response = await apiFetch('/api/cashier/sessions');
    cashierSessionsCache = await response.json();

    if (cashierSessionsCache.length === 0) {
        cashierList.innerHTML = '<div class="col-12"><div class="alert alert-info">Nenhuma comanda aberta.</div></div>';
        return;
    }

    cashierList.innerHTML = cashierSessionsCache.map((session) => `
        <div class="col-lg-6">
            <div class="card h-100 ${session.waiterFinalized ? 'border-success' : 'border-secondary'}">
                <div class="card-body">
                    <div class="d-flex justify-content-between">
                        <h5>Mesa ${session.tableNumber} • ${session.customerName}</h5>
                        <strong>Total: R$ ${Number(session.total).toFixed(2)}</strong>
                    </div>
                    <p class="mb-2 small text-muted">Garçom: #${session.waiterId} - ${session.waiterName}</p>
                    <p class="mb-2"><span class="badge text-bg-${session.waiterFinalized ? 'success' : 'warning'}">${session.waiterFinalized ? 'Pronta para fechamento' : 'Aguardando finalização do garçom'}</span></p>
                    <button class="btn btn-sm btn-danger" ${session.waiterFinalized ? '' : 'disabled'} onclick="openCashierDetail(${session.sessionId})">Fechar comanda</button>
                </div>
            </div>
        </div>
    `).join('');
}

function openCashierDetail(sessionId) {
    const session = cashierSessionsCache.find((s) => s.sessionId === sessionId);
    if (!session || !session.waiterFinalized) return;

    selectedSessionId = sessionId;
    document.getElementById('cashierModalTitle').textContent = `Fechamento da mesa ${session.tableNumber} - ${session.customerName}`;
    document.getElementById('cashierModalBody').innerHTML = `
        <div class="table-responsive">
            <table class="table table-striped">
                <thead><tr><th>Quantidade</th><th>Descrição</th><th>Valor unitário</th><th>Total</th></tr></thead>
                <tbody>
                    ${session.items.map((item) => `<tr><td>${item.quantity}</td><td>${item.itemName}</td><td>R$ ${Number(item.unitPrice).toFixed(2)}</td><td>R$ ${Number(item.total).toFixed(2)}</td></tr>`).join('')}
                </tbody>
            </table>
        </div>
        <div class="text-end fs-5"><strong>Total final: R$ ${Number(session.total).toFixed(2)}</strong></div>
    `;

    cashierDetailModal.show();
}

async function closeSession(sessionId) {
    const response = await apiFetch(`/api/cashier/sessions/${sessionId}/close`, { method: 'PATCH' });
    if (!response.ok) return;
    await refreshAllViews();
}

async function loadUsers() {
    if (!isAdmin()) return;
    const response = await apiFetch('/api/admin/users');
    usersCache = await response.json();

    usersList.innerHTML = `
        <table class="table table-sm table-striped">
            <thead><tr><th>ID</th><th>Nome</th><th>Login</th><th>Perfil</th><th></th></tr></thead>
            <tbody>
                ${usersCache.map((user) => `<tr><td>${user.id}</td><td>${user.fullName}</td><td>${user.username}</td><td>${friendlyRole(user.role)}</td><td><button class="btn btn-sm btn-outline-primary" onclick="editUser(${user.id})">Editar</button><button class="btn btn-sm btn-outline-danger ms-1" onclick="removeUser(${user.id})">Excluir</button></td></tr>`).join('')}
            </tbody>
        </table>
    `;
}

function editUser(id) {
    const user = usersCache.find((item) => item.id === id);
    if (!user) return;
    document.getElementById('editingUserId').value = id;
    document.getElementById('userFullName').value = user.fullName;
    document.getElementById('userUsername').value = user.username;
    document.getElementById('userRole').value = user.role;
    document.getElementById('cancelUserEdit').classList.remove('d-none');
}

async function removeUser(id) {
    const response = await apiFetch(`/api/admin/users/${id}`, { method: 'DELETE' });
    if (!response.ok) return;
    await loadUsers();
}

function editProduct(id) {
    const product = productsCache.find((p) => p.id === id);
    if (!product) return;
    document.getElementById('editingProductId').value = product.id;
    document.getElementById('stockName').value = product.name;
    document.getElementById('stockCategory').value = product.category;
    document.getElementById('stockPrice').value = product.unitPrice;
    document.getElementById('cancelStockEdit').classList.remove('d-none');
}

async function removeProduct(id) {
    const response = await apiFetch(`/api/products/${id}`, { method: 'DELETE' });
    if (!response.ok) return;
    await loadProducts();
}

function clearStockForm() {
    stockForm.reset();
    document.getElementById('editingProductId').value = '';
    document.getElementById('cancelStockEdit').classList.add('d-none');
}

function clearUserForm() {
    userForm.reset();
    document.getElementById('editingUserId').value = '';
    document.getElementById('cancelUserEdit').classList.add('d-none');
}

function configureTabsForRole() {
    const roleToTab = { WAITER: 'waiter', KITCHEN: 'kitchen', CASHIER: 'cashier', STOCK: 'stock', ADMIN: 'waiter' };
    ['waiter', 'kitchen', 'cashier', 'stock', 'admin'].forEach((tab) => {
        const button = document.getElementById(`tabBtn-${tab}`);
        const pane = document.getElementById(tab);
        const allowed = isAdmin() || roleToTab[authUser.role] === tab || (isAdmin() && tab === 'admin') || (authUser.role === 'ADMIN' && tab === 'admin');
        if (allowed) {
            button.parentElement.classList.remove('d-none');
            pane.classList.remove('d-none');
        } else {
            button.parentElement.classList.add('d-none');
            pane.classList.add('d-none');
        }
        button.classList.remove('active');
        pane.classList.remove('show', 'active');
    });

    const initialTab = isAdmin() ? 'waiter' : roleToTab[authUser.role];
    document.getElementById(`tabBtn-${initialTab}`).classList.add('active');
    document.getElementById(initialTab).classList.add('show', 'active');
}

async function refreshAllViews() {
    if (!authUser) return;
    const tasks = [loadProducts()];

    if (authUser.role === 'WAITER' || isAdmin()) tasks.push(loadWaiterOpenSessions(), loadWaiterHistory());
    if (authUser.role === 'KITCHEN' || isAdmin()) tasks.push(loadKitchen());
    if (authUser.role === 'CASHIER' || isAdmin()) tasks.push(loadCashier());
    if (isAdmin()) tasks.push(loadUsers());

    await Promise.all(tasks);
}

function isAdmin() {
    return authUser?.role === 'ADMIN';
}

function canEditStock() {
    return authUser?.role === 'STOCK' || isAdmin();
}

async function apiFetch(url, options = {}) {
    const headers = options.headers || {};
    if (authUser?.userId) headers['X-User-Id'] = authUser.userId;
    return fetch(url, { ...options, headers });
}

async function toError(response, fallbackMessage) {
    try {
        const body = await response.json();
        return new Error(body.error || fallbackMessage);
    } catch (e) {
        return new Error(fallbackMessage);
    }
}

function friendlyRole(role) {
    return { WAITER: 'Garçom', KITCHEN: 'Cozinha', CASHIER: 'Caixa', STOCK: 'Estoque', ADMIN: 'Administrador' }[role] || role;
}

function friendlyCategory(category) {
    return { BEBIDAS: 'Bebidas', REFEICOES: 'Refeições', LANCHES: 'Lanches', SOBREMESAS: 'Sobremesas' }[category] || category;
}

function friendlyKitchenStatus(status) {
    return { DRAFT: 'Aguardando envio', PENDING: 'Em fila', PREPARING: 'Preparando', DONE: 'Concluído' }[status] || status;
}

function kitchenBadge(status) {
    return { DRAFT: 'warning', PENDING: 'secondary', PREPARING: 'primary', DONE: 'success' }[status] || 'secondary';
}

function groupBy(items, key) {
    return items.reduce((acc, item) => {
        const groupKey = item[key];
        if (!acc[groupKey]) acc[groupKey] = [];
        acc[groupKey].push(item);
        return acc;
    }, {});
}

setInterval(() => {
    if (authUser) refreshAllViews();
}, 8000);
