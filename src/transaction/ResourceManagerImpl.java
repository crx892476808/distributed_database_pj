package transaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.*;

import lockmgr.DeadlockException;
import lockmgr.LockManager;

/**
 * Resource Manager for the Distributed Travel Reservation System.
 * 
 * Description: toy implementation of the RM
 */

public class ResourceManagerImpl extends java.rmi.server.UnicastRemoteObject implements ResourceManager
{
    HashMap<Integer, String> transIdToStatus  = new HashMap<>();
    public static final String statusInitiated = "Initiated";
    public static final String statusPrepared = "Prepared";
    public static final String statusCommitted = "Committed";
    public static final String statusAborted = "Aborted";

    protected static String RMlogDirPath;

    public static void main(String args[]) {
        Properties prop = new Properties();
        try
        {
            prop.load(new FileInputStream("../../conf/ddb.conf"));
            RMlogDirPath = prop.getProperty("RMLogDirPath");
            System.out.println("RM Log Dir Path = " + RMlogDirPath);
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }

        System.setSecurityManager(new RMISecurityManager());


        String rmiName = System.getProperty("rmiName");
        if (rmiName == null || rmiName.equals("")) {
            System.err.println("No RMI name given");
            System.exit(1);
        }

        String rmiPort = System.getProperty("rmiPort");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        try {
            ResourceManagerImpl obj = new ResourceManagerImpl(rmiName);
            Naming.rebind(rmiPort + rmiName, obj);
            System.out.println(rmiName + " bound");
        } catch (Exception e) {
            System.err.println(rmiName + " not bound:" + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
    protected final static String TRANSACTION_LOG_FILENAME = "transactions.log";

    public Set getTransactions()
    {
        return xids;
    }

    public Collection getUpdatedRows(int xid, String tablename)
    {
        RMTable table = getTable(xid, tablename);
        return new ArrayList(table.table.values());
    }

    public Collection getUpdatedRows(String tablename)
    {
        RMTable table = getTable(tablename);
        return new ArrayList(table.table.values());
    }

    protected String myRMIName = null; // Used to distinguish this RM from other

    protected String dieTime;

    protected HashMap<String, String> RMINameToTableName = new HashMap<>();

    protected String myTableName = null;

    public void setDieTime(String time) throws RemoteException
    {
        dieTime = time;
        System.out.println("Die time set to : " + time);
    }

    public String getID() throws RemoteException
    {
        return myRMIName;
    }

    // RMs
    protected HashSet xids = new HashSet();

    public ResourceManagerImpl(String rmiName) throws RemoteException
    {
        myRMIName = rmiName;
        dieTime = "NoDie";
        //added: Let RM knows it should manipulate which table
        RMINameToTableName.put(ResourceManager.RMINameFlights, ResourceManager.TableNameFlights);
        RMINameToTableName.put(ResourceManager.RMINameCars, ResourceManager.TableNameCars);
        RMINameToTableName.put(ResourceManager.RMINameRooms, ResourceManager.TableNameRooms);
        RMINameToTableName.put(ResourceManager.RMINameCustomers, ResourceManager.TableNameCustomers);
        RMINameToTableName.put(ResourceManager.RMINameReservations, ResourceManager.TableNameReservations);
        myTableName = RMINameToTableName.get(myRMIName);
//        //added : conf tell when to die
//        Properties prop = new Properties();
//        try {
//            prop.load(new FileInputStream("../../conf/ddb.conf"));
//        }
//        catch (Exception e1) {
//            e1.printStackTrace();
//
//        }

        recover();

        while (!reconnect())
        {
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
            }
        }

        new Thread()
        {
            public void run()
            {
                while (true)
                {
                    try
                    {
                        if (tm != null)
                            tm.ping();
                    }
                    catch (Exception e)
                    {
                        tm = null;
                    }

                    if (tm == null)
                    {
                        reconnect();
                        System.out.println("reconnect tm!");

                    }
                    try
                    {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException e)
                    {
                    }

                }
            }
        }.start();
    }

    public void ping()
    {
    }

    public void recover()
    {
        HashSet t_xids = loadTransactionLogs();
        if (t_xids != null)
            xids = t_xids;

        File dataDir = new File("data");
        if (!dataDir.exists())
        {
            dataDir.mkdirs();
        }
        File[] datas = dataDir.listFiles();
        //main table
        for (int i = 0; i < datas.length; i++)
        {
            if (datas[i].isDirectory())
            {
                continue;
            }
            if (datas[i].getName().equals(TRANSACTION_LOG_FILENAME))
            {
                continue;
            }
            if(datas[i].getName().equals(myTableName)) // only handled table is considered
                getTable(datas[i].getName());
        }

        //xtable ( temporary table)
        for (int i = 0; i < datas.length; i++)
        {
            if (!datas[i].isDirectory())
                continue;
            File[] xdatas = datas[i].listFiles();
            int xid = Integer.parseInt(datas[i].getName());
            if (!xids.contains(new Integer(xid)))
            {
                //this should never happen;
                throw new RuntimeException("ERROR: UNEXPECTED XID");
            }
            for (int j = 0; j < xdatas.length; j++)
            {
                if(xdatas[j].getName().equals(myTableName)) {// only handled table is considered
                    RMTable xtable = getTable(xid, xdatas[j].getName());
                    try {
                        xtable.relockAll();
                    } catch (DeadlockException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public boolean reconnect()
    {
        Properties prop = new Properties();
        try
        {
            prop.load(new FileInputStream("../../conf/ddb.conf"));
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
            return false;
        }
        String rmiPort = prop.getProperty("tm.port");
        if (rmiPort == null)
        {
            rmiPort = "";
        }
        else if (!rmiPort.equals(""))
        {
            rmiPort = "//:" + rmiPort + "/";
        }

        try
        {
            tm = (TransactionManager) Naming.lookup(rmiPort + TransactionManager.RMIName);
            System.out.println(myRMIName + "'s xids is Empty ? " + xids.isEmpty());
            for (Iterator iter = xids.iterator(); iter.hasNext();)
            {
                int xid = ((Integer) iter.next()).intValue();
                System.out.println(myRMIName + " Re-enlist to TM with xid" + xid);
                tm.enlist(xid, this);
                if (dieTime.equals("AfterEnlist"))
                    dieNow();
                //                iter.remove();
            }
            System.out.println(myRMIName + " bound to TM");
        }
        catch (Exception e)
        {
            System.err.println(myRMIName + " enlist error:" + e);
            return false;
        }

        return true;
    }

    public boolean dieNow() throws RemoteException
    {
        try
        {
            Thread.sleep(500); // Thread.sleep(long millis); here 500ms
        }
        catch (InterruptedException e)
        {
            //e.printStackTrace();
        }
        System.exit(1);
        return true; // We won't ever get here since we exited above;
        // but we still need it to please the compiler.
    }

    protected TransactionManager tm = null;

    public TransactionManager getTransactionManager() throws TransactionManagerUnaccessibleException
    {
        if (tm != null)
        {
            try
            {
                tm.ping();
            }
            catch (RemoteException e)
            {
                tm = null;
            }
        }
        if (tm == null)
        {
            if (!reconnect())
                tm = null;
        }
        if (tm == null)
            throw new TransactionManagerUnaccessibleException();
        else
            return tm;
    }

    protected LockManager lm = new LockManager();

    protected LockManager getLockManager()
    {
        return lm;
    }

    protected Hashtable tables = new Hashtable(); // xid -> xidtables

    protected RMTable loadTable(File file)
    {
        ObjectInputStream oin = null;
        try
        {
            oin = new ObjectInputStream(new FileInputStream(file));
            return (RMTable) oin.readObject();
        }
        catch (Exception e)
        {
            return null;
        }
        finally
        {
            try
            {
                if (oin != null)
                    oin.close();
            }
            catch (IOException e1)
            {
            }
        }
    }

    protected boolean storeTable(RMTable table, File file)
    {
        file.getParentFile().mkdirs();
        ObjectOutputStream oout = null;
        try
        {
            oout = new ObjectOutputStream(new FileOutputStream(file));
            oout.writeObject(table);
            oout.flush();
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
        finally
        {
            try
            {
                if (oout != null)
                    oout.close();
            }
            catch (IOException e1)
            {
            }
        }
    }

    protected RMTable getTable(int xid, String tablename)
    {
        Hashtable xidtables = null;
        synchronized (tables)
        {
            xidtables = (Hashtable) tables.get(new Integer(xid));
            if (xidtables == null)
            {
                xidtables = new Hashtable();
                tables.put(new Integer(xid), xidtables);
            }
        }
        synchronized (xidtables)
        {
            RMTable table = (RMTable) xidtables.get(tablename);
            if (table != null)
                return table;
            table = loadTable(new File("data/" + (xid == -1 ? "" : "" + xid + "/") + tablename));
            if (table == null)
            {
                if (xid == -1)
                    table = new RMTable(tablename, null, -1, lm);
                else
                {
                    table = new RMTable(tablename, getTable(tablename), xid, lm);
                }
            }
            else
            {
                if (xid != -1)
                {
                    table.setLockManager(lm);
                    table.setParent(getTable(tablename));
                }
            }
            xidtables.put(tablename, table);
            return table;
        }
    }

    protected RMTable getTable(String tablename)
    {
        return getTable(-1, tablename);
    }

    protected HashSet loadTransactionLogs()
    {
        File folder = new File(RMlogDirPath);
        if (!folder.exists())
            folder.mkdirs();
        File xidLog = new File(RMlogDirPath + myRMIName +"_transactions.log");
        ObjectInputStream oin = null;
        try
        {
            oin = new ObjectInputStream(new FileInputStream(xidLog));
            return (HashSet) oin.readObject();
        }
        catch (Exception e)
        {
            return null;
        }
        finally
        {
            try
            {
                if (oin != null)
                    oin.close();
            }
            catch (IOException e1)
            {
            }
        }
    }

    protected boolean storeTransactionLogs(HashSet xids)
    {
        File folder = new File(RMlogDirPath);
        if (!folder.exists())
            folder.mkdirs();
        File xidLog = new File(RMlogDirPath + myRMIName +"_transactions.log");
        xidLog.getParentFile().mkdirs();
        xidLog.getParentFile().mkdirs();
        ObjectOutputStream oout = null;
        try
        {
            oout = new ObjectOutputStream(new FileOutputStream(xidLog));
            oout.writeObject(xids);
            oout.flush();
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
        finally
        {
            try
            {
                if (oout != null)
                    oout.close();
            }
            catch (IOException e1)
            {
            }
        }
    }

    public Collection query(int xid, String tablename) throws DeadlockException, InvalidTransactionException,
            RemoteException
    {
        if (xid < 0)
        {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        try
        {
            synchronized (xids)
            {
                xids.add(new Integer(xid));
                storeTransactionLogs(xids);
            }

            getTransactionManager().enlist(xid, this);
        }
        catch (TransactionManagerUnaccessibleException | IOException e)
        {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        Collection result = new ArrayList();
        RMTable table = getTable(xid, tablename);
        synchronized (table)
        {
            for (Iterator iter = table.keySet().iterator(); iter.hasNext();)
            {
                Object key = iter.next();
                ResourceItem item = table.get(key);
                if (item != null && !item.isDeleted())
                {
                    table.lock(key, LockManager.READ);
                    result.add(item);
                }
            }
            if (!result.isEmpty())
            {
                if (!storeTable(table, new File("data/" + xid + "/" + tablename)))
                {
                    throw new RemoteException("System Error: Can't write table to disk!");
                }
            }
        }
        return result;
    }

    public ResourceItem query(int xid, String tablename, Object key) throws DeadlockException,
            InvalidTransactionException, RemoteException
    {
        if (xid < 0)
        {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        try
        {
            synchronized (xids)
            {
                xids.add(new Integer(xid));
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        }
        catch (TransactionManagerUnaccessibleException | IOException e)
        {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        RMTable table = getTable(xid, tablename);
        ResourceItem item = table.get(key);
        if (item != null && !item.isDeleted())
        {
            table.lock(key, LockManager.READ); //why lock here ?
            if (!storeTable(table, new File("data/" + xid + "/" + tablename)))
            {
                throw new RemoteException("System Error: Can't write table to disk!");
            }
            return item;
        }
        return null;
    }

    public Collection query(int xid, String tablename, String indexName, Object indexVal) throws DeadlockException,
            InvalidTransactionException, InvalidIndexException, RemoteException
    {
        if (xid < 0)
        {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        try
        {
            synchronized (xids)
            {
                xids.add(new Integer(xid));
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        }
        catch (TransactionManagerUnaccessibleException | IOException e)
        {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        Collection result = new ArrayList();
        RMTable table = getTable(xid, tablename);
        synchronized (table)
        {
            for (Iterator iter = table.keySet().iterator(); iter.hasNext();)
            {
                Object key = iter.next();
                ResourceItem item = table.get(key);
                if (item != null && !item.isDeleted() && item.getIndex(indexName).equals(indexVal))
                {
                    table.lock(key, LockManager.READ);
                    result.add(item);
                }
            }
            if (!result.isEmpty())
            {
                if (!storeTable(table, new File("data/" + xid + "/" + tablename)))
                {
                    throw new RemoteException("System Error: Can't write table to disk!");
                }
            }
        }
        return result;
    }

    public boolean update(int xid, String tablename, Object key, ResourceItem newItem) throws DeadlockException,
            InvalidTransactionException, RemoteException
    {
        if (xid < 0)
        {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        if (!key.equals(newItem.getKey()))
            throw new IllegalArgumentException();

        try
        {
            synchronized (xids)
            {
                xids.add(new Integer(xid));
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        }
        catch (TransactionManagerUnaccessibleException | IOException e)
        {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        RMTable table = getTable(xid, tablename);
        ResourceItem item = table.get(key);
        if (item != null && !item.isDeleted())
        {
            table.lock(key, LockManager.WRITE);
            table.put(newItem);
            if (!storeTable(table, new File("data/" + xid + "/" + tablename)))
            {
                throw new RemoteException("System Error: Can't write table to disk!");
            }
            return true;
        }
        return false;
    }

    public boolean insert(int xid, String tablename, ResourceItem newItem) throws DeadlockException,
            InvalidTransactionException, RemoteException
    {
        if (xid < 0)
        {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        try
        {
            synchronized (xids)
            {
                xids.add(new Integer(xid));
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        }
        catch (TransactionManagerUnaccessibleException | IOException e)
        {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        RMTable table = getTable(xid, tablename);

        ResourceItem item = (ResourceItem) table.get(newItem.getKey());
        if (item != null && !item.isDeleted())
        {
            return false;
        }
        table.lock(newItem.getKey(), LockManager.WRITE);
        table.put(newItem);
        if (!storeTable(table, new File("data/" + xid + "/" + tablename)))
        {
            throw new RemoteException("System Error: Can't write table to disk!");
        }
        return true;
    }

    public boolean delete(int xid, String tablename, Object key) throws DeadlockException, InvalidTransactionException,
            RemoteException
    {
        if (xid < 0)
        {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }

        try
        {
            synchronized (xids)
            {
                xids.add(new Integer(xid));
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        }
        catch (TransactionManagerUnaccessibleException | IOException e)
        {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        RMTable table = getTable(xid, tablename);
        ResourceItem item = table.get(key);
        if (item != null && !item.isDeleted())
        {
            table.lock(key, LockManager.WRITE);
            item = (ResourceItem) item.clone();
            item.delete();
            table.put(item);
            if (!storeTable(table, new File("data/" + xid + "/" + tablename)))
            {
                throw new RemoteException("System Error: Can't write table to disk!");
            }
            return true;
        }
        return false;
    }

    public int delete(int xid, String tablename, String indexName, Object indexVal) throws DeadlockException,
            InvalidTransactionException, InvalidIndexException, RemoteException
    {
        if (xid < 0)
        {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        try
        {
            synchronized (xids)
            {
                xids.add(new Integer(xid));
                storeTransactionLogs(xids);
            }
            getTransactionManager().enlist(xid, this);
        }
        catch (TransactionManagerUnaccessibleException | IOException e)
        {
            throw new RemoteException(e.getLocalizedMessage(), e);
        }

        if (dieTime.equals("AfterEnlist"))
            dieNow();

        int n = 0;

        RMTable table = getTable(xid, tablename);
        synchronized (table)
        {
            for (Iterator iter = table.keySet().iterator(); iter.hasNext();)
            {
                Object key = iter.next();
                ResourceItem item = table.get(key);
                if (item != null && !item.isDeleted() && item.getIndex(indexName).equals(indexVal))
                {
                    table.lock(item.getKey(), LockManager.WRITE);
                    item = (ResourceItem) item.clone();
                    item.delete();
                    table.put(item);
                    n++;
                }
            }
            if (n > 0)
            {
                if (!storeTable(table, new File("data/" + xid + "/" + tablename)))
                {
                    throw new RemoteException("System Error: Can't write table to disk!");
                }
            }
        }
        return n;
    }

    public boolean prepare(int xid) throws InvalidTransactionException, RemoteException
    {
        if(!transIdToStatus.containsKey(xid))
            transIdToStatus.put(xid, statusInitiated);
        if (dieTime.equals("BeforePrepare"))
            dieNow();
        if (xid < 0)
        {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        if(!transIdToStatus.containsKey(xid))
            transIdToStatus.replace(xid, statusPrepared);
        if (dieTime.equals("AfterPrepare"))
            dieNow();
        return true;
    }

    public boolean commit(int xid) throws InvalidTransactionException, RemoteException
    {
        if (dieTime.equals("BeforeCommit"))
            dieNow();
        if (xid < 0)
        {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        Hashtable xidtables = (Hashtable) tables.get(new Integer(xid));
        if (xidtables != null)
        {
            synchronized (xidtables)
            {
                for (Iterator iter = xidtables.entrySet().iterator(); iter.hasNext();) // find all tables, iter points to a table
                {
                    //commit all ? why ?
                    Map.Entry entry = (Map.Entry) iter.next(); // entry: a table
                    RMTable xtable = (RMTable) entry.getValue();
                    if(!entry.getKey().equals(myTableName))  //only handle owned table
                        continue;
                    RMTable table = getTable(xtable.getTablename());
                    for (Iterator iter2 = xtable.keySet().iterator(); iter2.hasNext();) // iterate in xtable
                    {
                        Object key = iter2.next(); // iter2 points to an entry
                        ResourceItem item = xtable.get(key);
                        if (item.isDeleted())
                            table.remove(item);
                        else
                            table.put(item);
                    }
                    if (!storeTable(table, new File("data/" + entry.getKey()))) // store the table
                        throw new RemoteException("Can't write table to disk");
                    new File("data/" + xid + "/" + entry.getKey()).delete();
                }
                tables.remove(new Integer(xid));
            }
        }

        if (!lm.unlockAll(xid))
            throw new RuntimeException();

        synchronized (xids)
        {
            xids.remove(new Integer(xid));
            storeTransactionLogs(xids);
        }
        transIdToStatus.replace(xid,statusCommitted);
        return true;
    }

    public void abort(int xid) throws InvalidTransactionException, RemoteException
    {
        if (dieTime.equals("BeforeAbort"))
            dieNow();
        if (xid < 0)
        {
            throw new InvalidTransactionException(xid, "Xid must be positive.");
        }
        Hashtable xidtables = (Hashtable) tables.get(new Integer(xid));
        if (xidtables != null)
        {
            synchronized (xidtables)
            {
                for (Iterator iter = xidtables.entrySet().iterator(); iter.hasNext();)
                {
                    Map.Entry entry = (Map.Entry) iter.next();
                    if(!entry.getKey().equals(myTableName))
                        continue;
                    System.out.println(entry.getKey() + " deleting...");
                    new File("data/" + xid + "/" + entry.getKey()).delete();
                }

                tables.remove(new Integer(xid));
            }

        }

        if (!lm.unlockAll(xid))
            throw new RuntimeException();

        synchronized (xids)
        {
            xids.remove(new Integer(xid));
            storeTransactionLogs(xids);
        }
        transIdToStatus.replace(xid,statusAborted);
    }

    //  test usage
    public ResourceManagerImpl() throws RemoteException
    {
    }

    public void setTransactionManager(TransactionManager tm)
    {
        this.tm = tm;
    }
}