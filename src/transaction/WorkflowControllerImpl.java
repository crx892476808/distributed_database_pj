package transaction;

import lockmgr.DeadlockException;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;

/**
 * Workflow Controller for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the WC.  In the real
 * implementation, the WC should forward calls to either RM or TM,
 * instead of doing the things itself.
 */

public class WorkflowControllerImpl
        extends java.rmi.server.UnicastRemoteObject
        implements WorkflowController {

    protected int flightcounter, flightprice, carscounter, carsprice, roomscounter, roomsprice;
    protected int xidCounter;

    protected ResourceManager rmFlights = null;
    protected ResourceManager rmRooms = null;
    protected ResourceManager rmCars = null;
    protected ResourceManager rmCustomers = null;
    protected TransactionManager tm = null;

    public static void main(String args[]) {
        System.setSecurityManager(new RMISecurityManager());

        String rmiPort = System.getProperty("rmiPort");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        try {
            WorkflowControllerImpl obj = new WorkflowControllerImpl();
            Naming.rebind(rmiPort + WorkflowController.RMIName, obj);
            System.out.println("WC bound");
        } catch (Exception e) {
            System.err.println("WC not bound:" + e);
            System.exit(1);
        }
    }


    public WorkflowControllerImpl() throws RemoteException {
        flightcounter = 0;
        flightprice = 0;
        carscounter = 0;
        carsprice = 0;
        roomscounter = 0;
        roomsprice = 0;
        flightprice = 0;

        xidCounter = 1;

        while (!reconnect()) {
            // would be better to sleep a while
        }
    }


    // TRANSACTION INTERFACE
    public int start()
            throws RemoteException {
        String rmiPort = System.getProperty("rmiPort");
        try {
            //rmFlights = (ResourceManager) Naming.lookup(rmiPort + ResourceManager.RMINameFlights);
            //rmRooms = (ResourceManager) Naming.lookup(rmiPort + ResourceManager.RMINameRooms);
            //rmCars = (ResourceManager) Naming.lookup(rmiPort + ResourceManager.RMINameCars);
            //rmCustomers = (ResourceManager) Naming.lookup(rmiPort + ResourceManager.RMINameCustomers);
            tm.start(xidCounter);
        }
        catch (Exception e) {
        }
        return (xidCounter++);
    }

    public boolean commit(int xid)
            throws IOException,
            TransactionAbortedException,
            InvalidTransactionException, ClassNotFoundException {
        tm.commit(xid);
        System.out.println("WC Committing");
        return true;
    }

    public void abort(int xid)
            throws RemoteException,
            InvalidTransactionException {
        return;
    }


    // ADMINISTRATIVE INTERFACE
    public boolean addFlight(int xid, String flightNum, int numSeats, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException, DeadlockException {
        rmFlights.insert(xid, ResourceManager.TableNameFlights, new Flight(flightNum,price, numSeats, numSeats));
        flightcounter += numSeats; //???
        flightprice = price;
        return true;
    }

    public boolean deleteFlight(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException, DeadlockException {
        rmFlights.delete(xid,ResourceManager.TableNameFlights,flightNum);
        flightcounter = 0; // ???
        flightprice = 0;
        return true;
    }

    public boolean addRooms(int xid, String location, int numRooms, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException, DeadlockException {
        rmRooms.insert(xid, ResourceManager.TableNameRooms, new Room(location, price, numRooms, numRooms));
        roomscounter += numRooms;
        roomsprice = price;
        return true;
    }

    public boolean deleteRooms(int xid, String location, int numRooms)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        roomscounter = 0;
        roomsprice = 0;
        return true;
    }

    public boolean addCars(int xid, String location, int numCars, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        carscounter += numCars;
        carsprice = price;
        return true;
    }

    public boolean deleteCars(int xid, String location, int numCars)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        carscounter = 0;
        carsprice = 0;
        return true;
    }

    public boolean newCustomer(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return true;
    }

    public boolean deleteCustomer(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return true;
    }


    // QUERY INTERFACE
    public int queryFlight(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException, DeadlockException {
        //System.out.println(flightcounter==null);
        System.out.println("querying...");
        Flight result = (Flight) rmFlights.query(xid,ResourceManager.TableNameFlights,flightNum);
        return result.numAvail;
        //return flightcounter;
    }

    public int queryFlightPrice(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return flightprice;
    }

    public int queryRooms(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException, DeadlockException {
        System.out.println("WC querying...");
        Room result = (Room) rmRooms.query(xid, ResourceManager.TableNameRooms, location);
        return result.numAvail;
        //return roomscounter;
    }

    public int queryRoomsPrice(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return roomsprice;
    }

    public int queryCars(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return carscounter;
    }

    public int queryCarsPrice(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return carsprice;
    }

    public int queryCustomerBill(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        return 0;
    }


    // RESERVATION INTERFACE
    public boolean reserveFlight(int xid, String custName, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        flightcounter--;
        return true;
    }

    public boolean reserveCar(int xid, String custName, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        carscounter--;
        return true;
    }

    public boolean reserveRoom(int xid, String custName, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        roomscounter--;
        return true;
    }

    // TECHNICAL/TESTING INTERFACE
    public boolean reconnect()
            throws RemoteException {
        String rmiPort = System.getProperty("rmiPort");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        try {
            rmFlights =
                    (ResourceManager) Naming.lookup(rmiPort +
                            ResourceManager.RMINameFlights);
            System.out.println("WC bound to RMFlights");
            rmRooms =
                    (ResourceManager) Naming.lookup(rmiPort +
                            ResourceManager.RMINameRooms);
            System.out.println("WC bound to RMRooms");
            rmCars =
                    (ResourceManager) Naming.lookup(rmiPort +
                            ResourceManager.RMINameCars);
            System.out.println("WC bound to RMCars");
            rmCustomers =
                    (ResourceManager) Naming.lookup(rmiPort +
                            ResourceManager.RMINameCustomers);
            System.out.println("WC bound to RMCustomers");
            tm =
                    (TransactionManager) Naming.lookup(rmiPort +
                            TransactionManager.RMIName);
            System.out.println("WC bound to TM");
        } catch (Exception e) {
            System.err.println("WC cannot bind to some component:" + e);
            return false;
        }

        try {
            if (rmFlights.reconnect() && rmRooms.reconnect() &&
                    rmCars.reconnect() && rmCustomers.reconnect()) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("Some RM cannot reconnect:" + e);
            return false;
        }

        return false;
    }

    public boolean dieNow(String who)
            throws RemoteException {
        if (who.equals(TransactionManager.RMIName) ||
                who.equals("ALL")) {
            try {
                tm.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameFlights) ||
                who.equals("ALL")) {
            try {
                rmFlights.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameRooms) ||
                who.equals("ALL")) {
            try {
                rmRooms.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameCars) ||
                who.equals("ALL")) {
            try {
                rmCars.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameCustomers) ||
                who.equals("ALL")) {
            try {
                rmCustomers.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(WorkflowController.RMIName) ||
                who.equals("ALL")) {
            System.exit(1);
        }
        return true;
    }

    public boolean dieRMAfterEnlist(String who)
            throws RemoteException {
        return true;
    }

    public boolean dieRMBeforePrepare(String who)
            throws RemoteException {
        if(who.equals( rmCars.getID()))
            rmCars.setDieTime("BeforePrepare");
        else if (who.equals(rmCustomers.getID()))
            rmCars.setDieTime("BeforePrepare");
        else if(who.equals(rmFlights.getID()))
            rmFlights.setDieTime("BeforePrepare");
        else if(who.equals(rmRooms.getID()))
            rmRooms.setDieTime("BeforePrepare");
        return true;
    }

    public boolean dieRMAfterPrepare(String who)
            throws RemoteException {
        return true;
    }

    public boolean dieTMBeforeCommit()
            throws RemoteException {
        return true;
    }

    public boolean dieTMAfterCommit()
            throws RemoteException {
        return true;
    }

    public boolean dieRMBeforeCommit(String who)
            throws RemoteException {
        if(who.equals( rmCars.getID()))
            rmCars.setDieTime("BeforeCommit");
        else if (who.equals(rmCustomers.getID()))
            rmCars.setDieTime("BeforeCommit");
        else if(who.equals(rmFlights.getID()))
            rmFlights.setDieTime("BeforeCommit");
        else if(who.equals(rmRooms.getID()))
            rmRooms.setDieTime("BeforeCommit");
        return true;
    }

    public boolean dieRMBeforeAbort(String who)
            throws RemoteException {
        return true;
    }
}
