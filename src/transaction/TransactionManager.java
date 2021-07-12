package transaction;

import java.io.IOException;
import java.rmi.*;

/** 
 * Interface for the Transaction Manager of the Distributed Travel
 * Reservation System.
 * <p>
 * Unlike WorkflowController.java, you are supposed to make changes
 * to this file.
 */

public interface TransactionManager extends Remote {

    public boolean dieNow()
	throws RemoteException;

    public void ping() throws RemoteException;
    
	public void enlist(int xid, ResourceManager rm) throws RemoteException;

    public boolean commit(int xid) throws InvalidTransactionException, IOException, ClassNotFoundException;

    public void start(int xid) throws RemoteException;

    public void setDieTime(String time) throws RemoteException;

	
    /** The RMI name a TransactionManager binds to. */
    public static final String RMIName = "TM";
}
