class Bill {
    private final int id,orderId,tableId; private final double subtotal,discount,tax,total;
    private boolean paid=false; private String paymentMethod="";
    Bill(int id,int orderId,int tableId,double subtotal,double discount,double tax,double total){this.id=id;this.orderId=orderId;this.tableId=tableId;this.subtotal=subtotal;this.discount=discount;this.tax=tax;this.total=total;}
    int getId(){return id;}int getOrderId(){return orderId;}int getTableId(){return tableId;}boolean isPaid(){return paid;}
    void markPaid(String method){paid=true;paymentMethod=method;}
    String json(){return "{\"id\":"+id+",\"orderId\":"+orderId+",\"tableId\":"+tableId+",\"subtotal\":"+subtotal+",\"discount\":"+discount+",\"tax\":"+tax+",\"total\":"+total+",\"paid\":"+paid+",\"paymentMethod\":\""+Main.esc(paymentMethod)+"\"}";}
}