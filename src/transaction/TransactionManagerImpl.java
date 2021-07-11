package transaction;

import java.io.*;
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
    protected HashMap<Integer, HashMap<String, RMWithStatus>> transIdtoRMName = new HashMap<>();
    //protected HashMap<Integer, ArrayList<RMWithStatus>> transIdToRMWithStatus = new HashMap<>(); //mapping: xid -> RM
    protected HashMap<Integer, String> transIdToStatus = new HashMap<>(); //mapping: xid -> Status
    public static final String statusInitiated = "Initiated";
    public static final String statusPreparing = "Preparing";
    public static final String statusCommitted = "Committed";
    public static final String statusAborted = "Aborted";
    public static final String statusCompleted = "Completed";
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


    public boolean commit(int xid) throws InvalidTransactionException, IOException, ClassNotFoundException {
        //change status to PREPARING before sending preparing message
        System.out.println("TM committing..."); // For debug
        transIdToStatus.replace(xid, statusPreparing);
        boolean allRMPrepared = true;
        ArrayList<String> notPreparedRMName = new ArrayList<>();
        for(String rmName: transIdtoRMName.get(xid).keySet()) {
            System.out.println(rmName + " preparing...");
            try {
                transIdtoRMName.get(xid).get(rmName).rm.prepare(xid);
            }
            catch(Exception e){
                allRMPrepared = false;
                notPreparedRMName.add(rmName);
            }
            transIdtoRMName.get(xid).get(rmName).rmStatus = rmStatusPrepared;//mark cohort as PREPARED
        }
        if(!allRMPrepared){
            //not all prepared, abort all
            System.out.println("TM aborting...");
            transIdToStatus.replace(xid, statusAborted);
            for(String rmName: transIdtoRMName.get(xid).keySet()) {
                if(!notPreparedRMName.contains(rmName)) {
                    try {
                        transIdtoRMName.get(xid).get(rmName).rm.abort(xid);
                        transIdtoRMName.get(xid).get(rmName).rmStatus = rmStatusAborted;
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
            new File("data/" + xid).delete();
            return false;
        }
        writeTransLog(xid, statusCommitted); //must force a commit log record to disk before sending commit message

        //change status to COMMITTED and send COMMIT message
        boolean allRMAcked = true;
        transIdToStatus.replace(xid, statusCommitted);
        ArrayList<String> notAckedRMName = new ArrayList<>();
        for(String rmName: transIdtoRMName.get(xid).keySet()) {
            System.out.println(rmName + " committing");
            try {
                transIdtoRMName.get(xid).get(rmName).rm.commit(xid);
                transIdtoRMName.get(xid).get(rmName).rmStatus = rmStatusCommitted;
            }
            catch(Exception e){
                allRMAcked = false;
                notAckedRMName.add(rmName);
                //if down, rm status stay in prepared
            }

        }
        if(!allRMAcked){
            ; //not all Acked, some rm is broken before committing
            System.out.println("TM not committing all...");
            return false;
            //keem tm still in "committed" status
            //keep all down rm in "prepared" status
        }

        //Must write a completed log record to disk before deletion from protocol database
        writeTransLog(xid, statusCompleted);

        //when all cohorts have acked, delete entry of transaction from protocol database
        transIdToStatus.remove(xid);
        transIdtoRMName.remove(xid);
        new File("data/" + xid).delete();
        System.out.println("TM committing finish ..."); // For debug

        return allRMAcked;
	}

    public void ping() throws RemoteException {
    }

    public void enlist(int xid, ResourceManager rm) throws RemoteException {
        // check if we need to abort when rm enlist during recovering
        if(transIdtoRMName.containsKey(xid) && transIdtoRMName.get(xid).containsKey(rm.getID()) &&
                !transIdtoRMName.get(xid).get(rm.getID()).rmStatus.equals(rmStatusAborted) &&
                transIdToStatus.containsKey(xid) && transIdToStatus.get(xid).equals(statusAborted)) {
            try{
                System.out.println("Aborting " + xid + "when enlisting " + rm.getID() + " due to rm recover");
                rm.abort(xid);
                transIdtoRMName.get(xid).get(rm.getID()).rmStatus = rmStatusAborted;
                //check if all aborted
                boolean allAborted = true;
                for(String RMName : transIdtoRMName.get(xid).keySet()){
                    if(!transIdtoRMName.get(xid).get(RMName).rmStatus.equals(rmStatusAborted)) {
                        allAborted = false;
                        break;
                    }
                }
                if(allAborted){
                    transIdtoRMName.remove(xid);
                    transIdToStatus.remove(xid);
                    new File("data/" + xid).delete();
                    return;
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        // check if we need to commit when rm enlist during recovering
        if(transIdtoRMName.containsKey(xid) && transIdtoRMName.get(xid).containsKey(rm.getID()) &&
                transIdtoRMName.get(xid).get(rm.getID()).rmStatus.equals(rmStatusPrepared) &&
                transIdToStatus.containsKey(xid) && transIdToStatus.get(xid).equals(statusCommitted)){
            try{
                System.out.println("recommitting " + xid + " when enlisting " + rm.getID() + " due to" +rm.getID()+ " recover");
                rm.commit(xid);
                transIdtoRMName.get(xid).get(rm.getID()).rmStatus = rmStatusCommitted;
                //check if all rm are committed
                boolean allCommitted = true;
                for(String RMName : transIdtoRMName.get(xid).keySet()){
                    if(!transIdtoRMName.get(xid).get(RMName).rmStatus.equals(rmStatusCommitted)) {
                        allCommitted = false;
                        break;
                    }
                }
                if(allCommitted){
                    transIdtoRMName.remove(xid);
                    transIdToStatus.remove(xid);
                    new File("data/" + xid).delete();
                    return;
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

        }

        //normal enlist
        if(!transIdtoRMName.containsKey(xid)){
            transIdtoRMName.put(xid, new HashMap<String, RMWithStatus>());
        }
        transIdtoRMName.get(xid).put(rm.getID(),new RMWithStatus(rm, rmStatusInitiated));
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
        //Mark transaction's status to Initiated
        transIdToStatus.put(xid, statusInitiated);
    }

    public void writeTransLog(int xid, String logContent) throws IOException {
        File folder = new File("transLog");
        if (!folder.exists())
            folder.mkdirs();
        File transTMLog = new File("transLog/"+xid);
        if(!transTMLog.exists())
            transTMLog.createNewFile();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(transTMLog));
        oos.writeObject(logContent);
    }

    public String readTransLog(int xid) throws IOException, ClassNotFoundException {
        File transTMLog = new File("transLog/"+xid);
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(transTMLog));
        String fileContent = (String)ois.readObject();
        return fileContent;
    }

}
