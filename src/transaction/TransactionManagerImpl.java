package transaction;

import java.rmi.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Transaction Manager for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the TM
 */

public class TransactionManagerImpl
        extends java.rmi.server.UnicastRemoteObject
        implements TransactionManager {
    protected HashMap<Integer, ArrayList<ResourceManager>> transIdToRM = new HashMap<>(); //mapping: xid -> RM

    public static void main(String args[]) {
        System.setSecurityManager(new RMISecurityManager());

        String rmiPort = System.getProperty("rmiPort");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        try {
            TransactionManagerImpl obj = new TransactionManagerImpl();
            Naming.rebind(rmiPort + TransactionManager.RMIName, obj);
            System.out.println("TM bound");
        } catch (Exception e) {
            System.err.println("TM not bound:" + e);
            System.exit(1);
        }
    }


    public boolean commit(int xid) throws InvalidTransactionException, RemoteException {
        boolean returnFlag = true;
        for (ResourceManager rm : transIdToRM.get(xid)) {
            if(!rm.prepare(xid)) {
                returnFlag = false;
                break;
            }
            rm.commit(xid);
        }
        return true;
	}

    public void ping() throws RemoteException {
    }

    public void enlist(int xid, ResourceManager rm) throws RemoteException {
        System.out.println("debug TM 55");
        if (!transIdToRM.containsKey(xid)) {
            transIdToRM.put(xid, new ArrayList<ResourceManager>());
        }
        //transIdToRM.computeIfAbsent(xid, k -> new ArrayList<ResourceManager>()); //if null new an ArrayList
        System.out.println("debug TM 57");
        System.out.println("debug "+(transIdToRM.get(xid)==null));
        transIdToRM.get(xid).add(rm);
        System.out.println("debug TM 55");
    }

    public TransactionManagerImpl() throws RemoteException {
    }

    public boolean dieNow()
            throws RemoteException {
        System.exit(1);
        return true; // We won't ever get here since we exited above;
        // but we still need it to please the compiler.
    }

    public void start(int xid) throws RemoteException {
    }

}
