const form = document.getElementById('orderForm');
const stockForm = document.getElementById('stockForm');
const waiterFeedback = document.getElementById('waiterFeedback');
const stockFeedback = document.getElementById('stockFeedback');
const kitchenList = document.getElementById('kitchenList');
const cashierList = document.getElementById('cashierList');
const stockList = document.getElementById('stockList');
const productSelect = document.getElementById('productSelect');
const unitPricePreview = document.getElementById('unitPricePreview');
const waiterOpenSessions = document.getElementById('waiterOpenSessions');
const waiterHistory = document.getElementById('waiterHistory');

let productsCache = [];
let cashierSessionsCache = [];
let selectedSessionId = null;

const cashierDetailModal = new bootstrap.Modal(document.getElementById('cashierDetailModal'));
const confirmCloseBtn = document.getElementById('confirmCloseBtn');

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const payload = {
        tableNumber: Number(document.getElementById('tableNumber').value),
        productId: Number(productSelect.value),
        quantity: Number(document.getElementById('quantity').value),
        notes: document.getElementById('notes').value
    };

    try {
        const response = await fetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Erro ao lançar pedido.');
        }

        waiterFeedback.innerHTML = '<div class="alert alert-success">Pedido lançado com sucesso!</div>';
        document.getElementById('quantity').value = '1';
        document.getElementById('notes').value = '';
        await refreshAllViews();
    } catch (err) {
        waiterFeedback.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
    }
});

stockForm.addEventListener('submit', async (event) => {
    event.preventDefault();

    const payload = {
        name: document.getElementById('stockName').value,
        category: document.getElementById('stockCategory').value,
        unitPrice: Number(document.getElementById('stockPrice').value)
    };

    try {
        const response = await fetch('/api/products', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Erro ao cadastrar produto.');
        }

        stockFeedback.innerHTML = '<div class="alert alert-success">Produto cadastrado no estoque.</div>';
        stockForm.reset();
        await loadProducts();
    } catch (err) {
        stockFeedback.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
    }
});

productSelect.addEventListener('change', () => {
    const selected = productsCache.find(p => p.id === Number(productSelect.value));
    unitPricePreview.value = selected ? `R$ ${Number(selected.unitPrice).toFixed(2)}` : '';
});

confirmCloseBtn.addEventListener('click', async () => {
    if (!selectedSessionId) return;
    await closeSession(selectedSessionId);
    cashierDetailModal.hide();
});

async function loadProducts() {
    const response = await fetch('/api/products');
    productsCache = await response.json();

    if (productsCache.length === 0) {
        productSelect.innerHTML = '<option value="">Sem produtos em estoque</option>';
        unitPricePreview.value = '';
        stockList.innerHTML = '<div class="alert alert-warning">Cadastre produtos para liberar pedidos do garçom.</div>';
        return;
    }

    productSelect.innerHTML = productsCache.map(product =>
        `<option value="${product.id}">${friendlyCategory(product.category)} • ${product.name}</option>`
    ).join('');
    productSelect.dispatchEvent(new Event('change'));

    const grouped = groupBy(productsCache, 'category');
    stockList.innerHTML = Object.keys(grouped).map(category => `
        <div class="mb-3">
            <h6 class="mb-2">${friendlyCategory(category)}</h6>
            <ul class="list-group">
                ${grouped[category].map(p => `<li class="list-group-item d-flex justify-content-between"><span>${p.name}</span><strong>R$ ${Number(p.unitPrice).toFixed(2)}</strong></li>`).join('')}
            </ul>
        </div>
    `).join('');
}

async function loadKitchen() {
    const response = await fetch('/api/kitchen/orders');
    const orders = await response.json();

    if (orders.length === 0) {
        kitchenList.innerHTML = '<div class="col-12"><div class="alert alert-info">Sem pedidos pendentes/preparo.</div></div>';
        return;
    }

    kitchenList.innerHTML = orders.map(order => `
        <div class="col-md-6 col-lg-4">
            <div class="card h-100 border-warning">
                <div class="card-body">
                    <h5 class="card-title">Mesa ${order.tableNumber}</h5>
                    <p class="mb-1"><strong>${order.itemName}</strong></p>
                    <p class="mb-1">Qtd: ${order.quantity}</p>
                    <p class="mb-1">Obs: ${order.notes || '-'}</p>
                    <p class="mb-2">Status: <span class="badge text-bg-${order.kitchenStatus === 'PREPARING' ? 'primary' : 'secondary'}">${friendlyKitchenStatus(order.kitchenStatus)}</span></p>
                    <div class="d-flex gap-2">
                        <button class="btn btn-sm btn-outline-primary" onclick="updateKitchenStatus(${order.id}, 'PREPARING')">Preparando</button>
                        <button class="btn btn-sm btn-success" onclick="updateKitchenStatus(${order.id}, 'DONE')">Concluído</button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

async function updateKitchenStatus(id, status) {
    await fetch(`/api/kitchen/orders/${id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status })
    });
    await refreshAllViews();
}

async function loadWaiterOpenSessions() {
    const response = await fetch('/api/waiter/sessions/open');
    const sessions = await response.json();

    if (sessions.length === 0) {
        waiterOpenSessions.innerHTML = '<div class="col-12"><div class="alert alert-info">Nenhuma comanda em aberto.</div></div>';
        return;
    }

    waiterOpenSessions.innerHTML = sessions.map(session => `
        <div class="col-lg-6">
            <div class="card h-100">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <h6 class="mb-1">Mesa ${session.tableNumber}</h6>
                            <small class="text-muted">Total: R$ ${Number(session.total).toFixed(2)}</small>
                        </div>
                        <span class="badge text-bg-${session.waiterFinalized ? 'success' : 'warning'}">${session.waiterFinalized ? 'Finalizada p/ caixa' : 'Em atendimento'}</span>
                    </div>
                    <ul class="list-group list-group-flush my-2">
                        ${session.items.map(item => `<li class="list-group-item px-0">${item.quantity}x ${item.itemName} <span class="badge text-bg-${kitchenBadge(item.kitchenStatus)} ms-1">${friendlyKitchenStatus(item.kitchenStatus)}</span></li>`).join('')}
                    </ul>
                    <button class="btn btn-sm btn-dark" ${session.waiterFinalized ? 'disabled' : ''} onclick="finalizeSession(${session.sessionId})">Finalizar e enviar ao caixa</button>
                </div>
            </div>
        </div>
    `).join('');
}

async function loadWaiterHistory() {
    const response = await fetch('/api/waiter/sessions/history/today');
    const sessions = await response.json();

    if (sessions.length === 0) {
        waiterHistory.innerHTML = '<div class="col-12"><div class="alert alert-info">Sem histórico no dia.</div></div>';
        return;
    }

    waiterHistory.innerHTML = sessions.map(session => `
        <div class="col-md-6 col-xl-4">
            <div class="border rounded p-2 bg-light-subtle h-100">
                <strong>Mesa ${session.tableNumber}</strong>
                <div class="small">Status: ${session.status}</div>
                <div class="small">Finalizada: ${session.waiterFinalized ? 'Sim' : 'Não'}</div>
                <div class="small">Total: R$ ${Number(session.total).toFixed(2)}</div>
            </div>
        </div>
    `).join('');
}

async function finalizeSession(sessionId) {
    const response = await fetch(`/api/waiter/sessions/${sessionId}/finalize`, { method: 'PATCH' });
    if (!response.ok) {
        const error = await response.json();
        waiterFeedback.innerHTML = `<div class="alert alert-danger">${error.error || 'Erro ao finalizar comanda.'}</div>`;
        return;
    }
    waiterFeedback.innerHTML = '<div class="alert alert-success">Comanda enviada para o caixa.</div>';
    await refreshAllViews();
}

async function loadCashier() {
    const response = await fetch('/api/cashier/sessions');
    cashierSessionsCache = await response.json();

    if (cashierSessionsCache.length === 0) {
        cashierList.innerHTML = '<div class="col-12"><div class="alert alert-info">Nenhuma comanda aberta.</div></div>';
        return;
    }

    cashierList.innerHTML = cashierSessionsCache.map(session => `
        <div class="col-lg-6">
            <div class="card h-100 ${session.waiterFinalized ? 'border-success' : 'border-secondary'}">
                <div class="card-body">
                    <div class="d-flex justify-content-between">
                        <h5>Mesa ${session.tableNumber}</h5>
                        <strong>Total: R$ ${Number(session.total).toFixed(2)}</strong>
                    </div>
                    <p class="mb-2"><span class="badge text-bg-${session.waiterFinalized ? 'success' : 'warning'}">${session.waiterFinalized ? 'Pronta para fechamento' : 'Aguardando finalização do garçom'}</span></p>
                    <button class="btn btn-sm btn-danger" ${session.waiterFinalized ? '' : 'disabled'} onclick="openCashierDetail(${session.sessionId})">Fechar comanda</button>
                </div>
            </div>
        </div>
    `).join('');
}

function openCashierDetail(sessionId) {
    const session = cashierSessionsCache.find(s => s.sessionId === sessionId);
    if (!session || !session.waiterFinalized) return;

    selectedSessionId = sessionId;
    document.getElementById('cashierModalTitle').textContent = `Fechamento da mesa ${session.tableNumber}`;
    document.getElementById('cashierModalBody').innerHTML = `
        <div class="table-responsive">
            <table class="table table-striped">
                <thead>
                    <tr>
                        <th>Quantidade</th>
                        <th>Descrição</th>
                        <th>Valor unitário</th>
                        <th>Total</th>
                    </tr>
                </thead>
                <tbody>
                    ${session.items.map(item => `<tr><td>${item.quantity}</td><td>${item.itemName}</td><td>R$ ${Number(item.unitPrice).toFixed(2)}</td><td>R$ ${Number(item.total).toFixed(2)}</td></tr>`).join('')}
                </tbody>
            </table>
        </div>
        <div class="text-end fs-5"><strong>Total final: R$ ${Number(session.total).toFixed(2)}</strong></div>
    `;

    cashierDetailModal.show();
}

async function closeSession(sessionId) {
    const response = await fetch(`/api/cashier/sessions/${sessionId}/close`, { method: 'PATCH' });
    if (!response.ok) {
        return;
    }
    await refreshAllViews();
}

function friendlyCategory(category) {
    return {
        BEBIDAS: 'Bebidas',
        REFEICOES: 'Refeições',
        LANCHES: 'Lanches',
        SOBREMESAS: 'Sobremesas'
    }[category] || category;
}

function friendlyKitchenStatus(status) {
    return {
        PENDING: 'Em fila',
        PREPARING: 'Preparando',
        DONE: 'Concluído'
    }[status] || status;
}

function kitchenBadge(status) {
    return {
        PENDING: 'secondary',
        PREPARING: 'primary',
        DONE: 'success'
    }[status] || 'secondary';
}

function groupBy(items, key) {
    return items.reduce((acc, item) => {
        const groupKey = item[key];
        if (!acc[groupKey]) acc[groupKey] = [];
        acc[groupKey].push(item);
        return acc;
    }, {});
}

async function refreshAllViews() {
    await Promise.all([
        loadProducts(),
        loadKitchen(),
        loadCashier(),
        loadWaiterOpenSessions(),
        loadWaiterHistory()
    ]);
}

refreshAllViews();
setInterval(() => {
    loadKitchen();
    loadCashier();
    loadWaiterOpenSessions();
}, 5000);
