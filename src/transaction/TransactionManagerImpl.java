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

    protected String dieTime = new String();
    protected  static String myRmiPort;

    public static void main(String args[]) {
        System.setSecurityManager(new RMISecurityManager());

        String rmiPort = System.getProperty("rmiPort");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }
        myRmiPort = rmiPort;

        try {
            TransactionManagerImpl obj = new TransactionManagerImpl();
            Naming.rebind(rmiPort + TransactionManager.RMIName, obj);
            System.out.println("TM bound");
        } catch (Exception e) {
            System.err.println("TM not bound:" + e);
            System.exit(1);
        }
    }

    public void recover(){
        File logDir = new File("transLog");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        // get all the log data, resume transIDToStatus and transIdtoRMName
        File[] logs = logDir.listFiles();
        for (File log: logs){
            int xid = Integer.parseInt(log.getName());
            try {
                HashMap<String, String> logContent = readTransLog(xid);
                transIdToStatus.put(xid,logContent.get("TM"));
                logContent.remove("TM");
                transIdtoRMName.put(xid,new HashMap<String, RMWithStatus>());
                for(String rmName: logContent.keySet()){
                    ResourceManager rm = (ResourceManager)Naming.lookup(myRmiPort + rmName);
                    RMWithStatus rmWithStatus = new RMWithStatus(rm, logContent.get(rmName));
                    transIdtoRMName.get(xid).put(rmName, rmWithStatus);
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

        }


        // resume previous transaction
        for(int xid : transIdToStatus.keySet()){
            String TMstatus = transIdToStatus.get(xid);
            if(TMstatus.equals(statusPreparing)){ //die before committing, recommit all
                try {
                    for (String rmName : transIdtoRMName.get(xid).keySet()) { //recommit all rm, do not consider cascade down
                        transIdtoRMName.get(xid).get(rmName).rm.commit(xid);
                        transIdtoRMName.get(xid).get(rmName).rmStatus = rmStatusCommitted;
                    }
                    //Must write a completed log record to disk before deletion from protocol database
                    writeTransLog(xid, statusCompleted, transIdtoRMName.get(xid));

                    //when all cohorts have acked, delete entry of transaction from protocol database
                    transIdToStatus.remove(xid);
                    transIdtoRMName.remove(xid);
                    new File("data/" + xid).delete();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            else if(TMstatus.equals(statusCompleted)){ // TM die after commit
                System.out.println("recover transaction from status completion");
                transIdToStatus.remove(xid);
                transIdtoRMName.remove(xid);
                new File("data/" + xid).delete();
                deleteTransLog(xid);
            }
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
        writeTransLog(xid, statusCommitted,transIdtoRMName.get(xid)); //must force a commit log record to disk before sending commit message
        if (dieTime.equals("BeforeCommit"))
            dieNow();
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
        writeTransLog(xid, statusCompleted, transIdtoRMName.get(xid));

        if(dieTime.equals("AfterCommit")){
            dieNow();
        }

        //when all cohorts have acked, delete entry of transaction from protocol database
        transIdToStatus.remove(xid);
        transIdtoRMName.remove(xid);
        new File("data/" + xid).delete();
        System.out.println("TM committing finish?"); // For debug
        deleteTransLog(xid);
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
                    deleteTransLog(xid);
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
            return;
        }

        // check if we need to commit when rm enlist during recovering
        if(transIdtoRMName.containsKey(xid) && transIdtoRMName.get(xid).containsKey(rm.getID()) &&
                transIdtoRMName.get(xid).get(rm.getID()).rmStatus.equals(rmStatusPrepared) &&
                transIdToStatus.containsKey(xid) && transIdToStatus.get(xid).equals(statusCommitted)){
            try{
                System.out.println("recommitting transaction " + xid + " when enlisting " + rm.getID() + " due to" +rm.getID()+ " recover");
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
                //debug
                for(String RMName : transIdtoRMName.get(xid).keySet()){
                    System.out.println(RMName + " " + transIdtoRMName.get(xid).get(RMName).rmStatus);
                }
                //debug end
                if(allCommitted){
                    transIdtoRMName.remove(xid);
                    transIdToStatus.remove(xid);
                    new File("data/" + xid).delete();
                    deleteTransLog(xid);
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
            return;
        }

        //normal enlist
        if(!transIdtoRMName.containsKey(xid)){
            transIdtoRMName.put(xid, new HashMap<String, RMWithStatus>());
        }
        transIdtoRMName.get(xid).put(rm.getID(),new RMWithStatus(rm, rmStatusInitiated));
    }

    public TransactionManagerImpl() throws RemoteException {
        // let TM recover from previous failure
        recover();
    }

    public void setDieTime(String time) throws RemoteException
    {
        dieTime = time;
        System.out.println("TM Die time set to : " + time);
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

    public void writeTransLog(int xid, String TMstatus, HashMap<String, RMWithStatus> RMIname2RMwithStatus) throws IOException {
        //content : HashMap<String, String> name2status
        // name can be "TM", means TM, or can be any RMI Name
        HashMap<String, String> logContent = new HashMap<>();
        logContent.put("TM", TMstatus);
        for(String keyName: RMIname2RMwithStatus.keySet()){
            String RMStatus = RMIname2RMwithStatus.get(keyName).rmStatus;
            logContent.put(keyName, RMStatus);
        }
        File folder = new File("transLog");
        if (!folder.exists())
            folder.mkdirs();
        File transTMLog = new File("transLog/"+xid);
        if(!transTMLog.exists())
            transTMLog.createNewFile();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(transTMLog));
        oos.writeObject(logContent);
    }

    public HashMap<String, String> readTransLog(int xid) throws IOException, ClassNotFoundException {
        File transTMLog = new File("transLog/"+xid);
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(transTMLog));
        HashMap<String, String> logContent = (HashMap<String, String>)ois.readObject();
        return logContent;
    }

    public void deleteTransLog(int xid) { // delete the transLog of transaction xid
        File transTMLog = new File("transLog/"+xid);
        if(transTMLog.exists())
            transTMLog.delete();
        System.out.println("come here for deleting translog");
    }

}
