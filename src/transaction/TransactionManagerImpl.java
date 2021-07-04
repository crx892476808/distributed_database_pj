package transaction;

import java.io.File;
import java.rmi.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Transaction Manager for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the TM
 */
class RMWithStatus{
    public ResourceManager rm;
    public String rmStatus;
    RMWithStatus(ResourceManager rm, String rmStatus){
        this.rm = rm;
        this.rmStatus = rmStatus;
    }
}
public class TransactionManagerImpl
        extends java.rmi.server.UnicastRemoteObject
        implements TransactionManager {
    protected HashMap<Integer, ArrayList<RMWithStatus>> transIdToRMWithStatus = new HashMap<>(); //mapping: xid -> RM
    protected HashMap<Integer, String> transIdToStatus = new HashMap<>(); //mapping: xid -> Status
    public static final String statusInitiated = "Initiated";
    public static final String statusPreparing = "Preparing";
    public static final String statusCommitted = "Committed";
    public static final String statusAborted = "Aborted";
    public static final String rmStatusInitiated = "Initiated";
    public static final String rmStatusPrepared = "Prepared";
    public static final String rmStatusCommitted = "Committed";
    public static final String rmStatusAborted = "Aborted";

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
        System.out.println("TM committing..."); // For debug
        transIdToStatus.replace(xid, statusPreparing); //change status to preparing before sending preparing message
        boolean allRMPrepared = true;
        for(int i = 0;i != transIdToRMWithStatus.get(xid).size();i++) {
            if(!transIdToRMWithStatus.get(xid).get(i).rm.prepare(xid)){
                allRMPrepared = false;
                break;
            }
            transIdToRMWithStatus.get(xid).get(i).rmStatus = rmStatusPrepared; //mark cohort as PREPARED
        }
        if(!allRMPrepared){
            ;//not all prepared, expection handler
        }

        //change status to COMMITTED and send COMMIT message
        boolean allRMAcked = true;
        transIdToStatus.replace(xid, statusCommitted);
        for(int i = 0;i != transIdToRMWithStatus.get(xid).size();i++){
            if(!transIdToRMWithStatus.get(xid).get(i).rm.commit(xid)){
                allRMAcked = false; // committed failed ,expection handler ,may in fact considering timeout
                break;
            }
            transIdToRMWithStatus.get(xid).get(i).rmStatus = rmStatusCommitted;
        }
        if(!allRMAcked){
            ; //not all Acked, expection handler
        }
        System.out.println("size="+transIdToRMWithStatus.get(xid).size());
        for (RMWithStatus rmWithStatus: transIdToRMWithStatus.get(xid)){
            System.out.println(rmWithStatus.rmStatus);
        }

        //when all cohorts have acked, delete entry of transaction from protocol database
        transIdToStatus.remove(xid);
        transIdToRMWithStatus.remove(xid);
        new File("data/" + xid).delete();

        return allRMAcked;
	}

    public void ping() throws RemoteException {
    }

    public void enlist(int xid, ResourceManager rm) throws RemoteException {

        if (!transIdToRMWithStatus.containsKey(xid)) {
            transIdToRMWithStatus.put(xid, new ArrayList<RMWithStatus>());
        }
        RMWithStatus rmWithStatus = new RMWithStatus(rm, rmStatusInitiated);
        transIdToRMWithStatus.get(xid).add(rmWithStatus);
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
        //Status: Initiated
        transIdToStatus.put(xid, statusInitiated);
    }

}
