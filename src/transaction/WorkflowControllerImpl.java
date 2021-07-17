package transaction;

import lockmgr.DeadlockException;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

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
    protected ResourceManager rmReservations = null;
    protected TransactionManager tm = null;

    public static final String transStatusOK = "OK";
    public static final String transStatusAborted = "Aborted";
    protected HashMap<Integer, String> transToStatus = new HashMap<>();

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
            transToStatus.put(xidCounter, transStatusOK);
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
        if(transToStatus.get(xid).equals(transStatusOK))
            tm.commit(xid);
        else
            tm.abort(xid);
        System.out.println("WC Committing");
        return true;
    }

    public void abort(int xid)
            throws RemoteException,
            InvalidTransactionException {
        tm.abort(xid);
        return;
    }


    // ADMINISTRATIVE INTERFACE
    public boolean addFlight(int xid, String flightNum, int numSeats, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException, DeadlockException {
        try {
            if(transToStatus.get(xid).equals(transStatusAborted))
                return false;
            rmFlights.insert(xid, ResourceManager.TableNameFlights, new Flight(flightNum, price, numSeats, numSeats));
        }
        catch (Exception e){ //rm die after enlist
            e.printStackTrace();
            transToStatus.replace(xid, transStatusAborted);
            return false;
        }
        return true;
    }

    public boolean deleteFlight(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException, DeadlockException {
        try {
            if(transToStatus.get(xid).equals(transStatusAborted))
                return false;
            boolean canDelete = true;
            ArrayList<Reservation> reservations = (ArrayList<Reservation>) rmReservations.query(xid, ResourceManager.TableNameReservations, Reservation.INDEX_RESVKEY, flightNum);
            for(Reservation reservation : reservations){
                if(reservation.getResvType() == Reservation.RESERVATION_TYPE_FLIGHT) {
                    canDelete = false;
                    break;
                }
            }
            if(canDelete) {
                rmFlights.delete(xid, ResourceManager.TableNameFlights, flightNum);
                return true;
            }
            else{
                transToStatus.replace(xid, transStatusAborted);
                return false;
            }
        }
        catch(Exception e){
            e.printStackTrace();
            transToStatus.replace(xid, transStatusAborted);
            return false;
        }
    }

    public boolean addRooms(int xid, String location, int numRooms, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException, DeadlockException {
        try {
            if(transToStatus.get(xid).equals(transStatusAborted))
                return false;
            rmRooms.insert(xid, ResourceManager.TableNameRooms, new Room(location, price, numRooms, numRooms));
        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return false;
        }
        roomscounter += numRooms;
        roomsprice = price;
        return true;
    }

    public boolean deleteRooms(int xid, String location, int numRooms)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try {
            if (transToStatus.get(xid).equals(transStatusAborted))
                return false;
            Room r = (Room) rmRooms.query(xid, ResourceManager.TableNameRooms, location);
            int previousRoomNum = r.getNumRooms();
            int previousRoomAvail = r.getNumAvail();
            int newRoomNum = previousRoomNum - numRooms;
            int newRoomAvail = previousRoomAvail - numRooms;
            r.setNumRooms(newRoomNum);
            r.setNumAvail(newRoomAvail);
            if(newRoomNum < 0 || newRoomAvail < 0)
                throw new Exception();
            rmRooms.update(xid,ResourceManager.TableNameRooms, location, r);
        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return false;
        }
        roomscounter = 0;
        roomsprice = 0;
        return true;
    }

    public boolean addCars(int xid, String location, int numCars, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try {
            if(transToStatus.get(xid).equals(transStatusAborted))
                return false;
            rmCars.insert(xid, ResourceManager.TableNameCars, new Car(location, price, numCars, numCars));
        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return false;
        }
        carscounter += numCars;
        carsprice = price;
        return true;
    }

    public boolean deleteCars(int xid, String location, int numCars)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try {
            if (transToStatus.get(xid).equals(transStatusAborted))
                return false;
            Car c = (Car) rmCars.query(xid, ResourceManager.TableNameCars, location);
            int previousCarNum = c.getNumCars();
            int previousCarAvail = c.getNumAvail();
            int newCarNum = previousCarNum - numCars;
            int newCarAvail = previousCarAvail - numCars;
            c.setNumCars(newCarNum);
            c.setNumAvail(newCarAvail);
            if(newCarNum < 0 || newCarAvail < 0)
                throw new Exception();
            rmCars.update(xid,ResourceManager.TableNameCars, location, c);
        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return false;
        }
        carscounter = 0;
        carsprice = 0;
        return true;
    }

    public boolean newCustomer(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try {
            if(transToStatus.get(xid).equals(transStatusAborted))
                return false;
            rmCustomers.insert(xid, ResourceManager.TableNameCustomers, new Customer(custName));
        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return false;
        }
        return true;
    }

    public boolean deleteCustomer(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try{
            if(transToStatus.get(xid).equals(transStatusAborted))
                return false;
            rmCustomers.delete(xid, ResourceManager.TableNameCustomers, custName);
        }
        catch(Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return false;
        }
        return true;
    }


    // QUERY INTERFACE
    public int queryFlight(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException, DeadlockException {
        //System.out.println(flightcounter==null);
        System.out.println("WC querying...");
        try {
            if (transToStatus.get(xid).equals(transStatusAborted))
                return -1;
            Flight result = (Flight) rmFlights.query(xid, ResourceManager.TableNameFlights, flightNum);
            return result.numAvail;
        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return -1;
        }

        //return flightcounter;
    }

    public int queryFlightPrice(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        System.out.println("WC querying...");
        try {
            if (transToStatus.get(xid).equals(transStatusAborted))
                return -1;
            Flight result = (Flight) rmFlights.query(xid, ResourceManager.TableNameFlights, flightNum);
            return result.price;
        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return -1;
        }
    }

    public int queryRooms(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException, DeadlockException {
        System.out.println("WC querying...");
        try {
            if (transToStatus.get(xid).equals(transStatusAborted))
                return -1;
            Room result = (Room) rmRooms.query(xid, ResourceManager.TableNameRooms, location);
            return result.numAvail;
        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return -1;
        }

        //return roomscounter;
    }

    public int queryRoomsPrice(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        System.out.println("WC querying...");
        try {
            if (transToStatus.get(xid).equals(transStatusAborted))
                return -1;
            Room result = (Room) rmRooms.query(xid, ResourceManager.TableNameRooms, location);
            return result.price;
        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return -1;
        }
    }

    public int queryCars(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        System.out.println("WC querying...");
        try {
            if (transToStatus.get(xid).equals(transStatusAborted))
                return -1;
            Car result = (Car) rmCars.query(xid, ResourceManager.TableNameCars, location);
            return result.numAvail;
        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return -1;
        }
        //return carscounter;
    }

    public int queryCarsPrice(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        System.out.println("WC querying...");
        try {
            if (transToStatus.get(xid).equals(transStatusAborted))
                return -1;
            Car result = (Car) rmCars.query(xid, ResourceManager.TableNameCars, location);
            return result.getPrice();
        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return -1;
        }
        //return carsprice;
    }

    public int queryCustomerBill(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try{
            if (transToStatus.get(xid).equals(transStatusAborted))
                return -1;
            //0 check if custname exists
            if(rmCustomers.query(xid,ResourceManager.TableNameCustomers, custName) == null) {
                transToStatus.replace(xid, transStatusAborted);
                return -1;
            }
            //1 get all reservations related to custName
            int totalPrice = 0;
            ArrayList<Reservation> reservations = (ArrayList<Reservation>) rmReservations.query(xid, ResourceManager.TableNameReservations, Reservation.INDEX_CUSTNAME, custName);
            // 2 get all the price
            for(Reservation reservation: reservations){
                if(reservation.getResvType() == Reservation.RESERVATION_TYPE_FLIGHT){
                    Flight flight = (Flight) rmFlights.query(xid, ResourceManager.TableNameFlights, reservation.getResvKey());
                    totalPrice += flight.getPrice();
                }
                else if(reservation.getResvType() == Reservation.RESERVATION_TYPE_HOTEL){
                    Room room = (Room) rmRooms.query(xid, ResourceManager.TableNameRooms, reservation.getResvKey());
                    totalPrice += room.getPrice();
                }
                else if(reservation.getResvType() == Reservation.RESERVATION_TYPE_CAR){
                    Car car = (Car) rmCars.query(xid, ResourceManager.TableNameCars, reservation.getResvKey());
                    totalPrice += car.getPrice();
                }
            }
            return totalPrice;

        }
        catch (Exception e){
            transToStatus.replace(xid, transStatusAborted);
            return -1;
        }
        //return 0;
    }


    // RESERVATION INTERFACE
    public boolean reserveFlight(int xid, String custName, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        //1 check if flight is full
        try {
            if (transToStatus.get(xid).equals(transStatusAborted))
                return false;
            Flight f = (Flight) rmFlights.query(xid, ResourceManager.TableNameFlights, flightNum);
            if(f.numAvail <= 0){
                transToStatus.replace(xid, transStatusAborted);
                return false;
            }
            f.numAvail = f.numAvail - 1;
            rmFlights.update(xid, ResourceManager.TableNameFlights, flightNum, f);

            //2 check customer exists ?
            if(rmCustomers.query(xid,ResourceManager.TableNameCustomers, custName) == null) {
                transToStatus.replace(xid, transStatusAborted);
                return false;
            }
            //3 : Reserve flight
            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_FLIGHT, flightNum);
            rmReservations.insert(xid, ResourceManager.TableNameReservations, reservation);
        }
        catch (Exception e){
            e.printStackTrace();
            transToStatus.replace(xid, transStatusAborted);
            return false;
        }
        return true;
    }

    public boolean reserveCar(int xid, String custName, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try{
            if (transToStatus.get(xid).equals(transStatusAborted))
                return false;
            //1 check car seats available ?
            Car car = (Car) rmCars.query(xid, ResourceManager.TableNameCars, location);
            if(car.numAvail <= 0){
                transToStatus.replace(xid, transStatusAborted);
                return false;
            }
            car.numAvail = car.numAvail - 1;
            rmCars.update(xid, ResourceManager.TableNameCars, location, car);

            //2 check customer exists ?
            if(rmCustomers.query(xid,ResourceManager.TableNameCustomers, custName) == null) {
                transToStatus.replace(xid, transStatusAborted);
                return false;
            }

            //3 Reserve Cars
            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_CAR, location);
            rmReservations.insert(xid, ResourceManager.TableNameReservations, reservation);
        }
        catch (Exception e){
            e.printStackTrace();
            transToStatus.replace(xid, transStatusAborted);
            return false;
        }
        carscounter--;
        return true;
    }

    public boolean reserveRoom(int xid, String custName, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try{
            if (transToStatus.get(xid).equals(transStatusAborted))
                return false;
            //1 check rooms available ?
            Room room = (Room) rmRooms.query(xid, ResourceManager.TableNameRooms, location);
            if(room.numAvail <= 0){
                transToStatus.replace(xid, transStatusAborted);
                return false;
            }
            room.numAvail = room.numAvail - 1;
            rmRooms.update(xid, ResourceManager.TableNameRooms, location, room);

            //2 check customer exists ?
            if(rmCustomers.query(xid,ResourceManager.TableNameCustomers, custName) == null) {
                transToStatus.replace(xid, transStatusAborted);
                System.out.println("Customer not exist when reserving rooms");
                return false;
            }

            //3 Reserve Rooms
            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_HOTEL, location);
            rmReservations.insert(xid, ResourceManager.TableNameReservations, reservation);
        }
        catch (Exception e){
            e.printStackTrace();
            transToStatus.replace(xid, transStatusAborted);
            return false;
        }
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
            // added : rmReservations
            rmReservations =
                    (ResourceManager) Naming.lookup(rmiPort +
                            ResourceManager.RMINameReservations);
            System.out.println("WC bound to RMReservations");
            //
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
        if(who.equals( rmCars.getID()))
            rmCars.setDieTime("AfterEnlist");
        else if (who.equals(rmCustomers.getID()))
            rmCars.setDieTime("AfterEnlist");
        else if(who.equals(rmFlights.getID()))
            rmFlights.setDieTime("AfterEnlist");
        else if(who.equals(rmRooms.getID()))
            rmRooms.setDieTime("AfterEnlist");
        else if(who.equals(rmReservations.getID()))
            rmReservations.setDieTime("AfterEnlist");
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
        else if(who.equals(rmReservations.getID()))
            rmReservations.setDieTime("BeforePrepare");
        return true;
    }

    public boolean dieRMAfterPrepare(String who)
            throws RemoteException {
        if(who.equals( rmCars.getID()))
            rmCars.setDieTime("AfterPrepare");
        else if (who.equals(rmCustomers.getID()))
            rmCars.setDieTime("AfterPrepare");
        else if(who.equals(rmFlights.getID()))
            rmFlights.setDieTime("AfterPrepare");
        else if(who.equals(rmRooms.getID()))
            rmRooms.setDieTime("AfterPrepare");
        else if(who.equals(rmReservations.getID()))
            rmReservations.setDieTime("AfterPrepare");
        return true;
    }

    public boolean dieTMBeforePreparing() throws RemoteException{
        tm.setDieTime("BeforePreparing");
        return true;
    }

    public boolean dieTMBeforeCommit()
            throws RemoteException {
        tm.setDieTime("BeforeCommit");
        return true;
    }

    public boolean dieTMAfterCommit()
            throws RemoteException {
        tm.setDieTime("AfterCommit");
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
        else if(who.equals(rmReservations.getID()))
            rmReservations.setDieTime("BeforeCommit");
        return true;
    }

    public boolean dieRMBeforeAbort(String who)
            throws RemoteException {
        if(who.equals( rmCars.getID()))
            rmCars.setDieTime("BeforeAbort");
        else if (who.equals(rmCustomers.getID()))
            rmCars.setDieTime("BeforeAbort");
        else if(who.equals(rmFlights.getID()))
            rmFlights.setDieTime("BeforeAbort");
        else if(who.equals(rmRooms.getID()))
            rmRooms.setDieTime("BeforeAbort");
        else if(who.equals(rmReservations.getID()))
            rmReservations.setDieTime("BeforeAbort");
        return true;
    }
}
