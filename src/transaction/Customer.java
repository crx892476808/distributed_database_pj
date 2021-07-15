package transaction;

import java.io.Serializable;

public class Customer implements ResourceItem, Serializable {
    public static final String INDEX_CUSTNAME = "custName";
    protected String custName;

    protected boolean isdeleted = false;



    public Customer(String custName) {
        this.custName = custName;

    }
    public String[] getColumnNames() {
        return new String[]{"custName"};
    }

    public String[] getColumnValues() {
        return new String[]{custName};
    }

    public Object getIndex(String indexName) throws InvalidIndexException {
        if (indexName.equals(INDEX_CUSTNAME))
            return custName;
        else
            throw new InvalidIndexException(indexName);
    }

    public Object getKey() {
        return custName;
    }

    public Object clone(){
        Customer o = new Customer(getCustName());
        o.isdeleted = isdeleted;
        return o;
    }

    public boolean isDeleted() {
        return isdeleted;
    }

    public void delete() {
        isdeleted = true;
    }

    public String getCustName() {
        return custName;
    }

    public void setCustName(String custName) {
        this.custName = custName;
    }
}
