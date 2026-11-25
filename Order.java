import java.util.*;
class Order {
    private final int id,tableId; private String status="OPEN";
    private final List<OrderLine> lines=new ArrayList<>();
    private final Deque<String> editHistory=new ArrayDeque<>(); // stack records order edits
    Order(int id,int tableId){this.id=id;this.tableId=tableId;}
    int getId(){return id;} int getTableId(){return tableId;} String getStatus(){return status;} void setStatus(String s){status=s;}
    List<OrderLine> getLines(){return lines;}
    void add(MenuItem item,int qty){editHistory.push("Added "+item.getName());for(OrderLine l:lines)if(l.getMenuItem().getId()==item.getId()){l.setQuantity(l.getQuantity()+qty);return;}lines.add(new OrderLine(item,qty));}
    void remove(int menuId){editHistory.push("Removed item "+menuId);lines.removeIf(l->l.getMenuItem().getId()==menuId);}
    double subtotal(){double s=0;for(OrderLine l:lines)s+=l.total();return s;}
    String json(){StringBuilder b=new StringBuilder("{\"id\":"+id+",\"tableId\":"+tableId+",\"status\":\""+status+"\",\"subtotal\":"+subtotal()+",\"items\":[");for(int i=0;i<lines.size();i++){if(i>0)b.append(",");b.append(lines.get(i).json());}return b.append("]}").toString();}
}
class OrderLine {
    private final MenuItem item;private int quantity;
    OrderLine(MenuItem item,int quantity){this.item=item;this.quantity=quantity;}
    MenuItem getMenuItem(){return item;}int getQuantity(){return quantity;}void setQuantity(int q){quantity=q;}
    double total(){return item.getPrice()*quantity;}
    String json(){return "{\"menuItemId\":"+item.getId()+",\"name\":\""+Main.esc(item.getName())+"\",\"price\":"+item.getPrice()+",\"quantity\":"+quantity+",\"total\":"+total()+"}";}
}