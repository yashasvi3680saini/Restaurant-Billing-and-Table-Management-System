import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static String esc(String value) {
    if (value == null) {
        return "";
    }

    return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
}

    static final RestaurantService service = new RestaurantService();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0
        );

        server.createContext("/", Main::handleRequest);
        server.setExecutor(null);
        server.start();

        System.out.println("Restaurant system running at http://localhost:8080");
    }

    static void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.startsWith("/api/")) {
                handleApi(exchange, path, method);
            } else {
                serveFile(exchange, path);
            }
        } catch (Exception e) {
            sendJson(exchange, 400,
                    "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    static void handleApi(
            HttpExchange exchange,
            String path,
            String method
    ) throws IOException {

        // GET APIs
        if (method.equals("GET")) {
            if (path.equals("/api/tables")) {
                sendJson(exchange, 200, service.getTablesJson());
                return;
            }

            if (path.equals("/api/menu")) {
                sendJson(exchange, 200, service.getMenuJson());
                return;
            }

            if (path.equals("/api/orders")) {
                sendJson(exchange, 200, service.getOrdersJson());
                return;
            }

            if (path.equals("/api/bills")) {
                sendJson(exchange, 200, service.getBillsJson());
                return;
            }
        }

        // Read form data from frontend
        Map<String, String> data = readForm(exchange);

        // Book a table / create order
        if (method.equals("POST") && path.equals("/api/orders")) {
            int tableId = number(data, "tableId");
            sendJson(exchange, 200,
                    service.createOrder(tableId));
            return;
        }

        // Add menu item to order
        if (method.equals("POST")
                && path.matches("/api/orders/\\d+/items")) {

            int orderId = pathNumber(path, 3);
            int menuItemId = number(data, "menuItemId");
            int quantity = number(data, "quantity");

            sendJson(exchange, 200,
                    service.addItem(orderId, menuItemId, quantity));
            return;
        }

        // Remove item from order
        if (method.equals("DELETE")
                && path.matches("/api/orders/\\d+/items/\\d+")) {

            String[] parts = path.split("/");
            int orderId = Integer.parseInt(parts[3]);
            int menuItemId = Integer.parseInt(parts[5]);

            sendJson(exchange, 200,
                    service.removeItem(orderId, menuItemId));
            return;
        }

        // Generate bill / checkout
        if (method.equals("POST")
                && path.matches("/api/orders/\\d+/checkout")) {

            int orderId = pathNumber(path, 3);
            sendJson(exchange, 200,
                    service.checkout(orderId));
            return;
        }

        // Mark bill as paid
        if (method.equals("POST")
                && path.matches("/api/bills/\\d+/pay")) {

            int billId = pathNumber(path, 3);
            String paymentMethod = data.getOrDefault("method", "Cash");

            sendJson(exchange, 200,
                    service.payBill(billId, paymentMethod));
            return;
        }

        // Add menu item
        if (method.equals("POST") && path.equals("/api/menu")) {
            String name = data.getOrDefault("name", "").trim();
            String category = data.getOrDefault("category", "Other").trim();
            double price = decimal(data, "price");

            sendJson(exchange, 200,
                    service.addMenuItem(name, category, price));
            return;
        }

        // Delete menu item
        if (method.equals("DELETE")
                && path.matches("/api/menu/\\d+")) {

            int menuItemId = pathNumber(path, 3);
            sendJson(exchange, 200,
                    service.deleteMenuItem(menuItemId));
            return;
        }

        sendJson(exchange, 404, "{\"error\":\"API endpoint not found\"}");
    }

    static void serveFile(HttpExchange exchange, String path)
            throws IOException {

        if (path.equals("/")) {
            path = "/index.html";
        }

        Path file = Paths.get("public" + path).normalize();

        if (!file.startsWith(Paths.get("public").normalize())
                || !Files.exists(file)
                || Files.isDirectory(file)) {
            sendText(exchange, 404, "File not found");
            return;
        }

        String contentType = "text/plain";

        if (path.endsWith(".html")) {
            contentType = "text/html";
        } else if (path.endsWith(".css")) {
            contentType = "text/css";
        } else if (path.endsWith(".js")) {
            contentType = "application/javascript";
        }

        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type",
                contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    static Map<String, String> readForm(HttpExchange exchange)
            throws IOException {

        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Map<String, String> result = new HashMap<>();

        if (body.isEmpty()) {
            return result;
        }

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8
            );

            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";

            result.put(key, value);
        }

        return result;
    }

    static int number(Map<String, String> data, String key) {
        return Integer.parseInt(data.getOrDefault(key, "0"));
    }

    static double decimal(Map<String, String> data, String key) {
        return Double.parseDouble(data.getOrDefault(key, "0"));
    }

    static int pathNumber(String path, int index) {
        return Integer.parseInt(path.split("/")[index]);
    }

    static void sendJson(
            HttpExchange exchange,
            int status,
            String json
    ) throws IOException {

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    static void sendText(
            HttpExchange exchange,
            int status,
            String text
    ) throws IOException {

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8"
        );
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    static String escape(String text) {
        if (text == null) return "";

        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}

class RestaurantService {

    private final List<TableData> tables = new ArrayList<>();
    private final List<MenuData> menu = new ArrayList<>();
    private final List<OrderData> orders = new ArrayList<>();
    private final List<BillData> bills = new ArrayList<>();

    private int nextOrderId = 1;
    private int nextBillId = 1;
    private int nextMenuId = 1;

    private final double TAX_RATE = 5.0;

    RestaurantService() {
        // Restaurant tables
        for (int i = 1; i <= 8; i++) {
            tables.add(new TableData(i, 4));
        }

        // Initial menu
        addMenuItemInternal("Butter Roti", "Indian", 20);
        addMenuItemInternal("Paneer Butter Masala", "Indian", 180);
        addMenuItemInternal("Veg Burger", "Fast Food", 120);
        addMenuItemInternal("Margherita Pizza", "Pizza", 250);
        addMenuItemInternal("Cold Coffee", "Beverages", 100);
        addMenuItemInternal("French Fries", "Fast Food", 90);
    }

    // TABLES
    String getTablesJson() {
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < tables.size(); i++) {
            TableData t = tables.get(i);

            if (i > 0) json.append(",");

            json.append("{")
                    .append("\"id\":").append(t.id).append(",")
                    .append("\"capacity\":").append(t.capacity).append(",")
                    .append("\"available\":").append(t.available)
                    .append("}");
        }

        return json.append("]").toString();
    }

    // MENU
    String getMenuJson() {
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < menu.size(); i++) {
            MenuData m = menu.get(i);

            if (i > 0) json.append(",");

            json.append("{")
                    .append("\"id\":").append(m.id).append(",")
                    .append("\"name\":\"").append(Main.escape(m.name)).append("\",")
                    .append("\"category\":\"").append(Main.escape(m.category)).append("\",")
                    .append("\"price\":").append(m.price)
                    .append("}");
        }

        return json.append("]").toString();
    }

    String addMenuItem(String name, String category, double price) {
        if (name.isEmpty()) {
            throw new RuntimeException("Item name is required");
        }

        if (price <= 0) {
            throw new RuntimeException("Price must be greater than zero");
        }

        MenuData item = addMenuItemInternal(name, category, price);
        return menuItemJson(item);
    }

    private MenuData addMenuItemInternal(
            String name,
            String category,
            double price
    ) {
        MenuData item = new MenuData(
                nextMenuId++, name, category, price
        );

        menu.add(item);
        return item;
    }

    String deleteMenuItem(int id) {
        MenuData item = findMenuItem(id);

        if (item == null) {
            throw new RuntimeException("Menu item not found");
        }

        menu.remove(item);
        return "{\"success\":true}";
    }

    // ORDERS
    String getOrdersJson() {
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < orders.size(); i++) {
            if (i > 0) json.append(",");
            json.append(orderJson(orders.get(i)));
        }

        return json.append("]").toString();
    }

    String createOrder(int tableId) {
        TableData table = findTable(tableId);

        if (table == null) {
            throw new RuntimeException("Table not found");
        }

        if (!table.available) {
            throw new RuntimeException("Table is already booked");
        }

        table.available = false;

        OrderData order = new OrderData(nextOrderId++, tableId);
        orders.add(order);

        return orderJson(order);
    }

    String addItem(int orderId, int menuItemId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        OrderData order = findOrder(orderId);
        MenuData item = findMenuItem(menuItemId);

        if (order == null || order.closed) {
            throw new RuntimeException("Active order not found");
        }

        if (item == null) {
            throw new RuntimeException("Menu item not found");
        }

        OrderItemData existing = null;

        for (OrderItemData oi : order.items) {
            if (oi.menuItemId == menuItemId) {
                existing = oi;
                break;
            }
        }

        if (existing == null) {
            order.items.add(new OrderItemData(
                    item.id, item.name, item.price, quantity
            ));
        } else {
            existing.quantity += quantity;
        }

        return orderJson(order);
    }

    String removeItem(int orderId, int menuItemId) {
        OrderData order = findOrder(orderId);

        if (order == null || order.closed) {
            throw new RuntimeException("Active order not found");
        }

        order.items.removeIf(
                item -> item.menuItemId == menuItemId
        );

        return orderJson(order);
    }

    // BILLING
    String checkout(int orderId) {
        OrderData order = findOrder(orderId);

        if (order == null || order.closed) {
            throw new RuntimeException("Order not found or already closed");
        }

        if (order.items.isEmpty()) {
            throw new RuntimeException("Add at least one item before billing");
        }

        double subtotal = orderSubtotal(order);
        double tax = subtotal * TAX_RATE / 100.0;
        double discount = 0;

        // 10% discount for bills of ₹1000 or more
        if (subtotal >= 1000) {
            discount = subtotal * 10.0 / 100.0;
        }

        double total = subtotal + tax - discount;

        BillData bill = new BillData(
                nextBillId++,
                order.id,
                order.tableId,
                subtotal,
                tax,
                discount,
                total
        );

        bills.add(bill);
        order.closed = true;
        order.billId = bill.id;

        return billJson(bill);
    }

    String getBillsJson() {
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < bills.size(); i++) {
            if (i > 0) json.append(",");
            json.append(billJson(bills.get(i)));
        }

        return json.append("]").toString();
    }

    String payBill(int billId, String method) {
        BillData bill = findBill(billId);

        if (bill == null) {
            throw new RuntimeException("Bill not found");
        }

        if (bill.paid) {
            throw new RuntimeException("Bill is already paid");
        }

        bill.paid = true;
        bill.paymentMethod = method;

        TableData table = findTable(bill.tableId);

        if (table != null) {
            table.available = true;
        }

        return billJson(bill);
    }

    // JSON HELPERS
    private String orderJson(OrderData order) {
        StringBuilder json = new StringBuilder();

        json.append("{")
                .append("\"id\":").append(order.id).append(",")
                .append("\"tableId\":").append(order.tableId).append(",")
                .append("\"status\":\"")
                .append(order.closed ? "Billed" : "Active").append("\",")
                .append("\"subtotal\":").append(orderSubtotal(order)).append(",")
                .append("\"items\":[");

        for (int i = 0; i < order.items.size(); i++) {
            if (i > 0) json.append(",");

            OrderItemData item = order.items.get(i);

            json.append("{")
                    .append("\"menuItemId\":").append(item.menuItemId).append(",")
                    .append("\"name\":\"").append(Main.escape(item.name)).append("\",")
                    .append("\"quantity\":").append(item.quantity).append(",")
                    .append("\"price\":").append(item.price).append(",")
                    .append("\"total\":").append(item.price * item.quantity)
                    .append("}");
        }

        return json.append("]}").toString();
    }

    private String billJson(BillData bill) {
        return "{"
                + "\"id\":" + bill.id + ","
                + "\"orderId\":" + bill.orderId + ","
                + "\"tableId\":" + bill.tableId + ","
                + "\"subtotal\":" + bill.subtotal + ","
                + "\"tax\":" + bill.tax + ","
                + "\"discount\":" + bill.discount + ","
                + "\"total\":" + bill.total + ","
                + "\"paid\":" + bill.paid + ","
                + "\"paymentMethod\":\""
                + Main.escape(bill.paymentMethod) + "\""
                + "}";
    }

    private String menuItemJson(MenuData item) {
        return "{"
                + "\"id\":" + item.id + ","
                + "\"name\":\"" + Main.escape(item.name) + "\","
                + "\"category\":\"" + Main.escape(item.category) + "\","
                + "\"price\":" + item.price
                + "}";
    }

    private double orderSubtotal(OrderData order) {
        double total = 0;

        for (OrderItemData item : order.items) {
            total += item.price * item.quantity;
        }

        return total;
    }

    private TableData findTable(int id) {
        for (TableData t : tables) {
            if (t.id == id) return t;
        }
        return null;
    }

    private MenuData findMenuItem(int id) {
        for (MenuData m : menu) {
            if (m.id == id) return m;
        }
        return null;
    }

    private OrderData findOrder(int id) {
        for (OrderData o : orders) {
            if (o.id == id) return o;
        }
        return null;
    }

    private BillData findBill(int id) {
        for (BillData b : bills) {
            if (b.id == id) return b;
        }
        return null;
    }
}

// DATA CLASSES
class TableData {
    int id;
    int capacity;
    boolean available = true;

    TableData(int id, int capacity) {
        this.id = id;
        this.capacity = capacity;
    }
}

class MenuData {
    int id;
    String name;
    String category;
    double price;

    MenuData(int id, String name, String category, double price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
    }
}

class OrderData {
    int id;
    int tableId;
    int billId;
    boolean closed = false;
    List<OrderItemData> items = new ArrayList<>();

    OrderData(int id, int tableId) {
        this.id = id;
        this.tableId = tableId;
    }
}

class OrderItemData {
    int menuItemId;
    String name;
    double price;
    int quantity;

    OrderItemData(
            int menuItemId,
            String name,
            double price,
            int quantity
    ) {
        this.menuItemId = menuItemId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
}

class BillData {
    int id;
    int orderId;
    int tableId;
    double subtotal;
    double tax;
    double discount;
    double total;
    boolean paid = false;
    String paymentMethod = "Not Paid";

    BillData(
            int id,
            int orderId,
            int tableId,
            double subtotal,
            double tax,
            double discount,
            double total
    ) {
        this.id = id;
        this.orderId = orderId;
        this.tableId = tableId;
        this.subtotal = subtotal;
        this.tax = tax;
        this.discount = discount;
        this.total = total;
    }
}