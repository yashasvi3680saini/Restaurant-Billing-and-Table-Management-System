class MenuItem {
    private final int id; private final String name,category; private final double price;
    MenuItem(int id,String name,String category,double price){this.id=id;this.name=name;this.category=category;this.price=price;}
    int getId(){return id;} String getName(){return name;} String getCategory(){return category;} double getPrice(){return price;}
    String json(){return "{\"id\":"+id+",\"name\":\""+Main.esc(name)+"\",\"category\":\""+Main.esc(category)+"\",\"price\":"+price+"}";}
}