package transaction;

import java.io.Serializable;

public class Flight implements ResourceItem, Serializable {
    public static final String INDEX_FLIGHTNUM = "flightNum";

    protected String flightNum;

    protected int price;

    protected int numSeats;

    protected int numAvail;

    protected boolean isdeleted = false;

    public Flight(String flightNum, int price, int numSeats, int numAvail) {
        this.flightNum = flightNum;
        this.price = price;
        this.numSeats = numSeats;
        this.numAvail = numAvail;
    }

    public String[] getColumnNames() {
        return new String[]{"flightNum", "price", "numSeats", "numAvail"};
    }

    public String[] getColumnValues() {
        return new String[]{flightNum, "" + price, "" + numSeats, "" + numAvail};
    }

    public Object getIndex(String indexName) throws InvalidIndexException {
        if (indexName.equals(INDEX_FLIGHTNUM))
            return flightNum;
        else
            throw new InvalidIndexException(indexName);
    }

    public Object getKey() {
        return flightNum;
    }

    public Object clone(){
        Flight o = new Flight(getFlightNum(), getPrice(), getNumSeats(), getNumAvail());
        o.isdeleted = isdeleted;
        return o;
    }

    public boolean isDeleted() {
        return isdeleted;
    }

    public void delete() {
        isdeleted = true;
    }


    public String getFlightNum() {
        return flightNum;
    }

    public int getPrice() {
        return price;
    }

    public int getNumSeats() {
        return numSeats;
    }

    public int getNumAvail() {
        return numAvail;
    }
}
