package transaction;

import java.io.Serializable;

public class Room implements ResourceItem, Serializable {
    public static final String INDEX_LOCATION = "location";
    protected String location;
    protected int price;
    protected int numRooms;
    protected int numAvail;

    protected boolean isdeleted = false;

    public Room(String location, int price, int numRooms, int numAvail) {
        this.location = location;
        this.price = price;
        this.numRooms = numRooms;
        this.numAvail = numAvail;
    }

    public String[] getColumnNames() {
        return new String[]{"location", "price", "numRooms", "numAvail"};
    }

    public String[] getColumnValues() {
        return new String[]{location, "" + price, "" + numRooms, "" + numAvail};
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
        Room o = new Room(getLocation(), getPrice(), getNumRooms(), getNumAvail());
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

    public int getNumRooms() {
        return numRooms;
    }

    public int getNumAvail() {
        return numAvail;
    }
}
