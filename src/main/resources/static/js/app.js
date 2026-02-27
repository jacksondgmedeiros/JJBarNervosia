const form = document.getElementById('orderForm');
const waiterFeedback = document.getElementById('waiterFeedback');
const kitchenList = document.getElementById('kitchenList');
const cashierList = document.getElementById('cashierList');

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const payload = {
        tableNumber: Number(document.getElementById('tableNumber').value),
        itemName: document.getElementById('itemName').value,
        quantity: Number(document.getElementById('quantity').value),
        unitPrice: Number(document.getElementById('unitPrice').value),
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
        document.getElementById('itemName').value = '';
        document.getElementById('quantity').value = '1';
        document.getElementById('unitPrice').value = '';
        document.getElementById('notes').value = '';
        await loadKitchen();
        await loadCashier();
    } catch (err) {
        waiterFeedback.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
    }
});

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
                    <p class="mb-2">Status: <span class="badge text-bg-secondary">${order.kitchenStatus}</span></p>
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
    await loadKitchen();
    await loadCashier();
}

async function loadCashier() {
    const response = await fetch('/api/cashier/sessions');
    const sessions = await response.json();

    if (sessions.length === 0) {
        cashierList.innerHTML = '<div class="col-12"><div class="alert alert-info">Nenhuma comanda aberta.</div></div>';
        return;
    }

    cashierList.innerHTML = sessions.map(session => `
        <div class="col-lg-6">
            <div class="card h-100">
                <div class="card-body">
                    <div class="d-flex justify-content-between">
                        <h5>Mesa ${session.tableNumber}</h5>
                        <strong>Total: R$ ${Number(session.total).toFixed(2)}</strong>
                    </div>
                    <ul class="list-group list-group-flush my-2">
                        ${session.items.map(item => `
                            <li class="list-group-item px-0">
                                ${item.quantity}x ${item.itemName} - R$ ${Number(item.total).toFixed(2)}
                                <span class="badge text-bg-${item.kitchenStatus === 'DONE' ? 'success' : 'warning'} ms-1">${item.kitchenStatus}</span>
                            </li>
                        `).join('')}
                    </ul>
                    <button class="btn btn-sm btn-danger" onclick="closeSession(${session.sessionId})">Fechar comanda</button>
                </div>
            </div>
        </div>
    `).join('');
}

async function closeSession(sessionId) {
    await fetch(`/api/cashier/sessions/${sessionId}/close`, { method: 'PATCH' });
    await loadCashier();
}

loadKitchen();
loadCashier();
setInterval(loadKitchen, 5000);
setInterval(loadCashier, 5000);
