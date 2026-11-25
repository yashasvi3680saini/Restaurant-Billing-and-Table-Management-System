
const $ = selector => document.querySelector(selector);

let tables = [];
let menu = [];
let orders = [];
let bills = [];
let activeView = "overview";
let currentOrder = null;

const money = amount =>
    "₹" + Number(amount || 0).toFixed(2);

// API connection
async function api(path, method = "GET", data = null) {
    const options = {
        method,
        headers: {}
    };

    if (data) {
        options.headers["Content-Type"] =
            "application/x-www-form-urlencoded;charset=UTF-8";

        options.body = new URLSearchParams(data);
    }

    const response = await fetch(path, options);
    const result = await response.json();

    if (!response.ok || result.error) {
        throw new Error(result.error || "Request failed");
    }

    return result;
}

// Toast message
function toast(message) {
    const element = $("#toast");

    element.textContent = message;
    element.classList.add("show");

    setTimeout(() => {
        element.classList.remove("show");
    }, 2500);
}

// Load data from Java
async function refresh() {
    try {
        const results = await Promise.all([
            api("/api/tables"),
            api("/api/menu"),
            api("/api/orders"),
            api("/api/bills")
        ]);

        tables = results[0];
        menu = results[1];
        orders = results[2];
        bills = results[3];

        renderOverview();

        if (activeView !== "overview") {
            renderView(activeView);
        }

    } catch (error) {
        toast(error.message);
    }
}

// Dashboard
function renderOverview() {
    $("#availableCount").textContent =
        tables.filter(table => table.available).length;

    $("#occupiedCount").textContent =
        tables.filter(table => !table.available).length;

    $("#openCount").textContent =
        orders.filter(order => order.status === "OPEN").length;

    $("#paidCount").textContent =
        bills.filter(bill => bill.paid).length;

    $("#tableGrid").innerHTML =
        tables.slice(0, 6).map(tableCard).join("");

    $("#recentOrders").innerHTML =
        orders.slice().reverse().slice(0, 4).map(order => `
            <div class="recent-row">
                <div>
                    <strong>Order #${order.id} · Table ${order.tableId}</strong>
                    <small>${order.items.length} item type(s) · ${money(order.subtotal)}</small>
                </div>

                <span class="status-tag ${order.status === "PAID" ? "paid" : ""}">
                    ${order.status}
                </span>
            </div>
        `).join("") ||
        '<div class="empty">No orders yet.</div>';
}

// Table card
function tableCard(table) {
    return `
        <button class="table-card ${table.available ? "" : "occupied"}"
                onclick="tableAction(${table.id})">

            <div class="table-number">
                Table ${String(table.id).padStart(2, "0")}
                <span>${table.capacity} seats</span>
            </div>

            <small>
                ${table.available ? "Ready for guests" : "Currently serving"}
            </small>

            <span class="table-status">
                ${table.available ? "Available" : "Occupied"}
            </span>
        </button>
    `;
}

// Navigation
function go(view) {
    activeView = view;

    document.querySelectorAll(".nav").forEach(button => {
        button.classList.toggle(
            "active",
            button.dataset.view === view
        );
    });

    const titles = {
        overview: "Overview",
        tables: "Table Management",
        orders: "Orders & Billing",
        menu: "Menu Management"
    };

    $("#pageTitle").textContent = titles[view];

    renderView(view);
}

// Render different sections
function renderView(view) {
    const container = $("#viewContent");

    if (view === "overview") {
        container.innerHTML = "";
        return;
    }

    if (view === "tables") {
        container.innerHTML = `
            <section class="panel view-panel">
                <div class="section-heading">
                    <div>
                        <h3>All Restaurant Tables</h3>
                        <p>Select an available table to create an order.</p>
                    </div>
                </div>

                <div class="table-grid">
                    ${tables.map(tableCard).join("")}
                </div>
            </section>
        `;
    }

    if (view === "menu") {
        container.innerHTML = `
            <section class="panel view-panel">
                <div class="section-heading">
                    <div>
                        <h3>Menu Management</h3>
                        <p>Add or remove menu items.</p>
                    </div>

                    <button class="btn green-btn" onclick="showAddMenu()">
                        + Add Item
                    </button>
                </div>

                <div class="menu-grid">
                    ${menu.map(item => `
                        <div class="menu-item">
                            <strong>${safe(item.name)}</strong>
                            <small>${safe(item.category)}</small>

                            <div style="display:flex;justify-content:space-between;align-items:center">
                                <span class="price">${money(item.price)}</span>

                                <button class="text-btn"
                                        onclick="deleteMenu(${item.id})">
                                    Remove
                                </button>
                            </div>
                        </div>
                    `).join("")}
                </div>
            </section>
        `;
    }

    if (view === "orders") {
        container.innerHTML = `
            <section class="panel view-panel">
                <div class="section-heading">
                    <div>
                        <h3>Orders & Billing</h3>
                        <p>Manage open orders and payments.</p>
                    </div>
                </div>

                ${orders.slice().reverse().map(orderCard).join("") ||
                '<div class="empty">No orders yet.</div>'}

                <h3 style="margin:25px 0 15px">Bills</h3>

                ${bills.slice().reverse().map(billCard).join("") ||
                '<div class="empty">No bills generated yet.</div>'}
            </section>
        `;
    }
}

// Order card
function orderCard(order) {
    return `
        <div class="order-card">
            <div style="display:flex;justify-content:space-between">
                <h4>Order #${order.id} · Table ${order.tableId}</h4>
                <span class="status-tag">${order.status}</span>
            </div>

            <p>
                ${order.items.length} item type(s) ·
                Subtotal ${money(order.subtotal)}
            </p>

            ${order.status === "OPEN" ? `
                <button class="btn light-btn"
                        onclick="openOrder(${order.id})">
                    Edit Order
                </button>

                <button class="btn green-btn"
                        onclick="checkout(${order.id})">
                    Generate Bill
                </button>
            ` : ""}
        </div>
    `;
}

// Bill card
function billCard(bill) {
    return `
        <div class="order-card">
            <div style="display:flex;justify-content:space-between">
                <h4>Bill #${bill.id} · Table ${bill.tableId}</h4>

                <span class="status-tag ${bill.paid ? "paid" : ""}">
                    ${bill.paid ? "PAID" : "UNPAID"}
                </span>
            </div>

            <p>
                Subtotal ${money(bill.subtotal)} ·
                Tax ${money(bill.tax)} ·
                Discount ${money(bill.discount)}
            </p>

            <strong>Total ${money(bill.total)}</strong>

            ${!bill.paid ? `
                <div style="margin-top:12px">
                    <select class="field" id="pay-${bill.id}">
                        <option>Cash</option>
                        <option>Card</option>
                        <option>UPI</option>
                    </select>

                    <button class="btn green-btn"
                            onclick="payBill(${bill.id})">
                        Mark Paid
                    </button>
                </div>
            ` : ""}
        </div>
    `;
}

// Create order for table
async function tableAction(tableId) {
    const table = tables.find(t => t.id === tableId);

    if (!table) return;

    if (!table.available) {
        const order = orders.find(
            o => o.tableId === tableId && o.status === "OPEN"
        );

        if (order) {
            openOrder(order.id);
        } else {
            toast("This table has an unpaid bill.");
        }

        return;
    }

    try {
        const order = await api(
            "/api/orders",
            "POST",
            { tableId }
        );

        currentOrder = order.id;

        showOrderModal(order);

        await refresh();

    } catch (error) {
        toast(error.message);
    }
}

// Open existing order
function openOrder(orderId) {
    const order = orders.find(o => o.id === orderId);

    if (order) {
        currentOrder = orderId;
        showOrderModal(order);
    }
}

// Order modal
function showOrderModal(order) {
    $("#modalBody").innerHTML = `
        <h3>Order #${order.id} · Table ${order.tableId}</h3>

        <div class="form-row">
            <label>Choose Menu Item</label>

            <select id="pickItem" class="field">
                ${menu.map(item => `
                    <option value="${item.id}">
                        ${safe(item.name)} — ${money(item.price)}
                    </option>
                `).join("")}
            </select>
        </div>

        <div class="form-row">
            <label>Quantity</label>
            <input id="pickQty" type="number" min="1" max="99" value="1">
        </div>

        <button class="btn green-btn" onclick="addOrderItem()">
            + Add to Order
        </button>

        <div id="orderLines" style="margin-top:18px"></div>

        <div class="bill-total">
            <span>Subtotal</span>
            <strong id="modalSubtotal">${money(order.subtotal)}</strong>
        </div>

        <p style="font-size:10px;color:#92988f;margin-top:10px">
            Tax and discount are calculated during checkout.
        </p>
    `;

    renderOrderLines(order);

    $("#modal").classList.remove("hidden");
}

// Render items in order
function renderOrderLines(order) {
    $("#orderLines").innerHTML =
        order.items.map(item => `
            <div class="line-row">
                <span>
                    ${safe(item.name)} × ${item.quantity}
                    <small style="display:block;color:#999;margin-top:3px">
                        ${money(item.total)}
                    </small>
                </span>

                <button onclick="removeOrderItem(${item.menuItemId})">
                    Remove
                </button>
            </div>
        `).join("") ||
        '<div class="empty">No items added yet.</div>';

    $("#modalSubtotal").textContent = money(order.subtotal);
}

// Add item
async function addOrderItem() {
    try {
        const menuItemId = Number($("#pickItem").value);
        const quantity = Number($("#pickQty").value);

        const order = await api(
            `/api/orders/${currentOrder}/items`,
            "POST",
            { menuItemId, quantity }
        );

        renderOrderLines(order);

        await refresh();

    } catch (error) {
        toast(error.message);
    }
}

// Remove item
async function removeOrderItem(menuItemId) {
    try {
        const order = await api(
            `/api/orders/${currentOrder}/items/${menuItemId}`,
            "DELETE"
        );

        renderOrderLines(order);

        await refresh();

    } catch (error) {
        toast(error.message);
    }
}

// Generate bill
async function checkout(orderId) {
    try {
        const bill = await api(
            `/api/orders/${orderId}/checkout`,
            "POST",
            {}
        );

        $("#modalBody").innerHTML = `
            <h3>Bill Generated ✓</h3>
            <p>Bill #${bill.id} · Table ${bill.tableId}</p>

            <div class="bill-total">
                <span>Subtotal</span>
                <strong>${money(bill.subtotal)}</strong>
            </div>

            <div class="bill-total">
                <span>Discount</span>
                <strong>${money(bill.discount)}</strong>
            </div>

            <div class="bill-total">
                <span>Tax (5%)</span>
                <strong>${money(bill.tax)}</strong>
            </div>

            <div class="bill-total final">
                <span>Amount Due</span>
                <strong>${money(bill.total)}</strong>
            </div>

            <button class="btn green-btn"
                    style="margin-top:16px"
                    onclick="closeModal();go('orders')">
                View Bill & Payment
            </button>
        `;

        $("#modal").classList.remove("hidden");

        await refresh();

    } catch (error) {
        toast(error.message);
    }
}

// Payment
async function payBill(billId) {
    try {
        const method = $("#pay-" + billId).value;

        await api(
            `/api/bills/${billId}/pay`,
            "POST",
            { method }
        );

        toast("Payment recorded! Table is available.");

        await refresh();

    } catch (error) {
        toast(error.message);
    }
}

// Add menu item modal
function showAddMenu() {
    $("#modalBody").innerHTML = `
        <h3>Add Menu Item</h3>

        <div class="form-row">
            <label>Item Name</label>
            <input id="newName" placeholder="Shahi Paneer">
        </div>

        <div class="form-row">
            <label>Category</label>
            <input id="newCategory" placeholder="Main Course">
        </div>

        <div class="form-row">
            <label>Price (₹)</label>
            <input id="newPrice" type="number" min="1" placeholder="250">
        </div>

        <button class="btn green-btn" onclick="saveMenu()">
            Save Menu Item
        </button>
    `;

    $("#modal").classList.remove("hidden");
}

// Save menu item
async function saveMenu() {
    try {
        await api(
            "/api/menu",
            "POST",
            {
                name: $("#newName").value,
                category: $("#newCategory").value,
                price: $("#newPrice").value
            }
        );

        closeModal();

        toast("Menu item added!");

        await refresh();

    } catch (error) {
        toast(error.message);
    }
}

// Delete menu item
async function deleteMenu(id) {
    if (!confirm("Remove this menu item?")) return;

    try {
        await api(`/api/menu/${id}`, "DELETE");

        toast("Menu item removed!");

        await refresh();

    } catch (error) {
        toast(error.message);
    }
}

// Close modal
function closeModal() {
    $("#modal").classList.add("hidden");
}

// Escape text before displaying
function safe(text) {
    return String(text).replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

// Event listeners
document.querySelectorAll(".nav").forEach(button => {
    button.addEventListener("click", () => {
        go(button.dataset.view);
    });
});

document.querySelectorAll("[data-goto]").forEach(button => {
    button.addEventListener("click", () => {
        go(button.dataset.goto);
    });
});

$("#refreshBtn").addEventListener("click", refresh);
$("#modalClose").addEventListener("click", closeModal);

$("#modal").addEventListener("click", event => {
    if (event.target.id === "modal") {
        closeModal();
    }
});

// Start application
refresh();
