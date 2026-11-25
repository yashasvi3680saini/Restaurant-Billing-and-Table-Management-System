class Table {
    private final int id, capacity;
    private boolean available=true;
    Table(int id,int capacity){this.id=id;this.capacity=capacity;}
    int getId(){return id;} int getCapacity(){return capacity;}
    boolean isAvailable(){return available;} void setAvailable(boolean v){available=v;}
    String json(){return "{\"id\":"+id+",\"capacity\":"+capacity+",\"available\":"+available+"}";}
}