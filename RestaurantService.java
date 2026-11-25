import java.util.*;

class RestaurantService {
    private final List<Table> tables = new ArrayList<>();
    private final List<MenuItem> menu = new ArrayList<>();
    private final Map<Integer,Order> orders = new LinkedHashMap<>();
    private final Map<Integer,Bill> bills = new LinkedHashMap<>();
    private int nextOrder=1, nextBill=1, nextMenu=10;
    private final DiscountPolicy discountPolicy = new FlatDiscount(0); // polymorphic discount strategy

    RestaurantService() {
        for(int i=1;i<=12;i++) tables.add(new Table(i, i<=4?2:4));
        menu.add(new MenuItem(1,"Paneer Tikka","Starter",220));
        menu.add(new MenuItem(2,"Veg Spring Rolls","Starter",160));
        menu.add(new MenuItem(3,"Butter Paneer","Main Course",280));
        menu.add(new MenuItem(4,"Dal Makhani","Main Course",240));
        menu.add(new MenuItem(5,"Butter Naan","Bread",45));
        menu.add(new MenuItem(6,"Jeera Rice","Rice",150));
        menu.add(new MenuItem(7,"Masala Dosa","South Indian",180));
        menu.add(new MenuItem(8,"Gulab Jamun","Dessert",90));
        menu.add(new MenuItem(9,"Cold Coffee","Beverage",130));
    }
    synchronized String tablesJson() {
        StringBuilder b=new StringBuilder("[");
        for(int i=0;i<tables.size();i++){if(i>0)b.append(",");b.append(tables.get(i).json());}
        return b.append("]").toString();
    }
    synchronized String menuJson() {
        StringBuilder b=new StringBuilder("[");
        for(int i=0;i<menu.size();i++){if(i>0)b.append(",");b.append(menu.get(i).json());}
        return b.append("]").toString();
    }
    synchronized String ordersJson() {
        StringBuilder b=new StringBuilder("["); int i=0;
        for(Order o:orders.values()){if(i++>0)b.append(",");b.append(o.json());}
        return b.append("]").toString();
    }
    synchronized String billsJson() {
        StringBuilder b=new StringBuilder("["); int i=0;
        for(Bill bill:bills.values()){if(i++>0)b.append(",");b.append(bill.json());}
        return b.append("]").toString();
    }
    synchronized String createOrder(int tableId) {
        Table t=findTable(tableId);
        if(t==null) return error("Table not found");
        if(!t.isAvailable()) return error("This table is already occupied");
        Order o=new Order(nextOrder++,tableId);
        orders.put(o.getId(),o); t.setAvailable(false);
        return o.json();
    }
    synchronized String addItem(int orderId,int menuId,int qty) {
        Order o=findOpenOrder(orderId); MenuItem m=findMenu(menuId);
        if(o==null)return error("Open order not found");
        if(m==null)return error("Menu item not found");
        if(qty<1 || qty>99)return error("Quantity must be from 1 to 99");
        o.add(m,qty);
        return o.json();
    }
    synchronized String removeItem(int orderId,int menuId) {
        Order o=findOpenOrder(orderId);
        if(o==null)return error("Open order not found");
        o.remove(menuId);
        return o.json();
    }
    synchronized String checkout(int orderId) {
        Order o=findOpenOrder(orderId);
        if(o==null)return error("Open order not found");
        if(o.getLines().isEmpty())return error("Add at least one item before checkout");
        double subtotal=o.subtotal(), discount=discountPolicy.discount(subtotal);
        double tax=(subtotal-discount)*0.05;
        Bill b=new Bill(nextBill++,o.getId(),o.getTableId(),subtotal,discount,tax,subtotal-discount+tax);
        bills.put(b.getId(),b); o.setStatus("BILLED");
        return b.json();
    }
    synchronized String pay(int billId,String method) {
        Bill b=bills.get(billId);
        if(b==null)return error("Bill not found");
        if(b.isPaid())return error("Bill is already paid");
        b.markPaid(method);
        Table t=findTable(b.getTableId()); if(t!=null)t.setAvailable(true);
        Order o=orders.get(b.getOrderId()); if(o!=null)o.setStatus("PAID");
        return b.json();
    }
    synchronized String addMenu(String name,String category,double price) {
        if(name.trim().isEmpty())return error("Item name is required");
        if(price<=0)return error("Price must be greater than zero");
        MenuItem m=new MenuItem(nextMenu++,name.trim(),category.trim().isEmpty()?"Main Course":category.trim(),price);
        menu.add(m);return m.json();
    }
    synchronized String deleteMenu(int id) {
        boolean removed=menu.removeIf(m->m.getId()==id);
        return removed?"{\"success\":true}":error("Menu item not found");
    }
    private Table findTable(int id){for(Table t:tables)if(t.getId()==id)return t;return null;}
    private MenuItem findMenu(int id){for(MenuItem m:menu)if(m.getId()==id)return m;return null;}
    private Order findOpenOrder(int id){Order o=orders.get(id);return o!=null&&"OPEN".equals(o.getStatus())?o:null;}
    private String error(String s){return "{\"error\":\""+Main.esc(s)+"\"}";}
}