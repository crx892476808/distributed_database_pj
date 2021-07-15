package transaction;

import java.io.Serializable;

public class Car implements ResourceItem, Serializable {
    public static final String INDEX_LOCATION = "location";
    protected String location;
    protected int price;
    protected int numCars;
    protected int numAvail;

    protected boolean isdeleted = false;

    public Car(String location, int price, int numRooms, int numAvail) {
        this.location = location;
        this.price = price;
        this.numCars = numRooms;
        this.numAvail = numAvail;
    }

    public String[] getColumnNames() {
        return new String[]{"location", "price", "numCars", "numAvail"};
    }

    public String[] getColumnValues() {
        return new String[]{location, "" + price, "" + numCars, "" + numAvail};
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setNumCars(int numRooms) {
        this.numCars = numRooms;
    }

    public void setNumAvail(int numAvail) {
        this.numAvail = numAvail;
    }

    public Object getIndex(String indexName) throws InvalidIndexException {
        if (indexName.equals(INDEX_LOCATION))
            return location;
        else
            throw new InvalidIndexException(indexName);
    }

    public Object getKey() {
        return location;
    }

    public Object clone(){
        Car o = new Car(getLocation(), getPrice(), getNumCars(), getNumAvail());
        o.isdeleted = isdeleted;
        return o;
    }

    public boolean isDeleted() {
        return isdeleted;
    }

    public void delete() {
        isdeleted = true;
    }

    public String getLocation() {
        return location;
    }

    public int getPrice() {
        return price;
    }

    public int getNumCars() {
        return numCars;
    }

    public int getNumAvail() {
        return numAvail;
    }
}
