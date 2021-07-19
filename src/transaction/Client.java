package transaction;

import java.io.FileInputStream;
import java.rmi.Naming;
import java.util.Properties;

/**
 * A toy client of the Distributed Travel Reservation System.
 */

public class Client {

    public static void main(String args[]) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("../../conf/ddb.conf"));
        } catch (Exception e1) {
            e1.printStackTrace();
            return;
        }
        String rmiPort = prop.getProperty("wc.port");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        WorkflowController wc = null;
        try {
            wc = (WorkflowController) Naming.lookup(rmiPort + WorkflowController.RMIName);
            System.out.println("Bound to WC");
        } catch (Exception e) {
            System.err.println("Cannot bind to WC:" + e);
            System.exit(1);
        }

        try {
            //here starts client controller tests
            //Test: add filght and rooms, then query (ordinary transaction handling)
//            int xid = wc.start();
//
//            wc.addFlight(xid, "347", 230, 999);
//            wc.addRooms(xid, "Guangdong", 300,50);
//            int remainSeats = wc.queryFlight(xid, "347");
//            int remainRooms = wc.queryRooms(xid, "Guangdong");
//            wc.commit(xid);
            //Test: end

//            //Test: query Flight with error
//            int xid = wc.start();
//            //wc.dieRMBeforePrepare(ResourceManager.RMINameFlights);
//            int remainSeats = wc.queryFlight(xid, "347");
//
//            //wc.addRooms(xid, "Guangdong", 300,50);
//            int remainRooms = wc.queryRooms(xid, "Guangdong");
//            wc.commit(xid);
//
//            System.out.println("Flight 347 has " + remainSeats + "seats");
//            System.out.println("Hotel in Guangdong has " + remainRooms + " rooms");
//            //Test: end

//            //Test: Add flight with BeforeCommit errors
//            //wc.reconnect();
//            wc.dieRMBeforeCommit(ResourceManager.RMINameFlights);
//            wc.dieRMBeforeCommit(ResourceManager.RMINameRooms);
//            int xid = wc.start();
//            System.out.println(xid);
//            //wc.dieRMBeforePrepare(ResourceManager.RMINameFlights);
//            wc.addRooms(xid,"b",2,2);
//            wc.addFlight(xid, "2", 2, 2);
//            //wc.deleteFlight(xid, "349");
//            //int remainSeats = wc.queryFlight(xid, "349");
//            //System.out.println("Flight 347 has " + remainSeats + "seats");
//            wc.commit(xid);
//            //Test: end

            //TEST: TM down before commit
//            wc.dieTMBeforeCommit();
//            int xid = wc.start();
//            wc.addFlight(xid, "10", 10, 10);
//            wc.commit(xid);
            //TEST: TM down before commit end

            //TEST: TM failure after commit
//            wc.dieTMAfterCommit();
//            int xid = wc.start();
//            wc.addFlight(xid,"11",11,11);
//            wc.commit(xid);
            //TEST: TM failure after commit end

            //TEST: TM failure before preparing
//            wc.dieTMBeforePreparing();
//            int xid = wc.start();
//            wc.addCars(xid, "102",102,102);
//            wc.commit(xid);
            //TEST: TM failure before preparing end

            //TEST : RM failure after prepare
//            wc.dieRMAfterPrepare(ResourceManager.RMINameFlights);
//            int xid = wc.start();
//            wc.addFlight(xid, "13", 13, 13);
//            wc.commit(xid);
            // TEST : RM failure after prepare end


            //TEST : RMFlight failure before prepare, RMRooms failure before abort
//            wc.dieRMBeforePrepare(ResourceManager.RMINameFlights);
//            wc.dieRMBeforeAbort(ResourceManager.RMINameRooms);
//            int xid = wc.start();
//            wc.addRooms(xid, "z",26,26);
//            wc.addFlight(xid, "14",14,14);
//            wc.commit(xid);
            //TEST: RMFlight failure before prepare, RMRooms failure before abort end

            //TEST: RM failure after enlist
//            wc.dieRMAfterEnlist(ResourceManager.RMINameFlights);
//            int xid = wc.start();
//            wc.addFlight(xid, "16", 17, 17);
//            wc.commit(xid);
            //TEST: RM failure after enlist end

            //TEST : addCars
//            int xid = wc.start();
//            wc.addCars(xid,"Tokyo",1000,500);
//            wc.commit(xid);
            //TEST: addCars end

            //TEST: addRooms
//            int xid = wc.start();
//            wc.addRooms(xid, "Tokyo",1000,350);
//            wc.commit(xid);
            //TEST: addRooms end

            //TEST: deleteRooms
//            int xid = wc.start();
//            wc.deleteRooms(xid,"1000",1);
//            wc.commit(xid);
            //TEST: deleteRooms end

            //TEST: delete Cars
//            int xid = wc.start();
//            wc.deleteCars(xid, "1001", 1);
//            wc.commit(xid);
            //TEST: deleteCars end

            //TEST: add Customer
//            int xid = wc.start();
//            wc.newCustomer(xid, "Eren");
//            wc.commit(xid);
            //TEST : add Customer end

            //TEST: add Flight
//            int xid = wc.start();
//            wc.addFlight(xid, "test4", 400, 12000);
//            wc.commit(xid);
            //TEST: add Flight end

            //TEST: delete Customer
//            int xid = wc.start();
//            if(wc.deleteCustomer(xid, "1"))
//                System.out.println("add delete customer finish");
//            wc.commit(xid);
            //TEST: delete CUstomer end

            //TEST: query room price
//            int xid = wc.start();
//            int rPrice = wc.queryRoomsPrice(xid, "1000");
//            System.out.println(rPrice);
//            wc.commit(xid);
            //TEST: query room price end

            //TEST: reserve Flight
//            String custName = "Eren";
//            int xid = wc.start();
//            wc.reserveFlight(xid, custName, "CA929");
//            wc.commit(xid);
            //TEST: reserve Flight end

            //TEST: Reserve Rooms
//            String custName = "Eren";
//            int xid = wc.start();
//            wc.reserveRoom(xid, custName, "Tokyo");
//            wc.commit(xid);
            //TEST: Reserve Rooms finish

            //TEST : reserve Car
//            String custName = "Eren";
//            int xid = wc.start();
//            wc.reserveCar(xid, custName, "Tokyo");
//            wc.commit(xid);
            //TEST : reserve Car end

            //TEST : query total cost
//            String custName = "Eren";
//            int xid = wc.start();
//            int totalPrice = wc.queryCustomerBill(xid, custName);
//            wc.commit(xid);
//            System.out.println(custName + "'s total price = " + totalPrice);
            //TEST: end

            //TEST: die RMReservation before Prepare
//            wc.reconnect();
//            wc.dieRMAfterPrepare(ResourceManager.RMINameReservations);
//            String custName = "Mikasa";
//            int xid = wc.start();
//            wc.newCustomer(xid, custName);
//            wc.reserveRoom(xid, custName, "Tokyo");
//            wc.commit(xid);
            //TEST: end

            //TEST: compound transactions: new customer -> reservation for new customer
//            wc.reconnect();
//            String custName = "Mikasa";
//            int xid = wc.start();
//            wc.newCustomer(xid, custName);
//            wc.reserveRoom(xid, custName, "Tokyo");
//            wc.commit(xid);
            //TEST: end

            //TEST: WC abort(xid)
//            int xid = wc.start();
//            wc.addRooms(xid,"Beijing", 1000,1000);
//            wc.abort(xid);
            //TEST: end

            //TEST: fail delete Flight
//            int xid = wc.start();
//            if(wc.deleteFlight(xid, "test"))
//                System.out.println("delete success");
//            wc.commit(xid);
            //TEST: end

            //TEST: FREE add
//            wc.reconnect();
//            int xid = wc.start();
//            wc.addRooms(xid, "81200",1000,1000);
//            int res = wc.queryRooms(xid,"81200");
//            wc.commit(xid);
//            System.out.println(res);
            //TEST: end

            //TEST: FREE QUERY
//            try {
//                wc.reconnect();
//                int xid = wc.start();
//                int remainCars = wc.queryCars(xid, "1001");
//                System.out.println("Room 1001 has " + remainCars + " rooms");
//                //int remainRooms = wc.queryRooms(xid, "b");
//                //System.out.println("Hotel in b has " + remainRooms + " rooms");
//                wc.commit(xid);
//            }
//            catch (Exception e){
//                e.printStackTrace();
//            }
            //TEST: Free query end

            //Formal Test: Normal Actions
//            int xid = wc.start();
//            wc.addFlight(xid, "MU5183",200,610);
//            wc.addCars(xid, "Shanghai",200, 500);
//            wc.addRooms(xid, "Shanghai", 300, 300);
//            wc.newCustomer(xid,"Eren");
//            wc.reserveFlight(xid,"Eren","MU5183");
//            int totalCost = wc.queryCustomerBill(xid, "Eren");
//            if(wc.commit(xid))
//                System.out.println("From Client: commit successfully");
//            System.out.println("totalCost = " + totalCost);
            //Formal Test: end

            //Formal Test: dieRMAfterEnlist
//            int xid = wc.start();
//            wc.dieRMAfterEnlist(ResourceManager.RMINameFlights);
//            wc.addFlight(xid,"CA1817",200,500);
//            if(wc.commit(xid))
//                System.out.println("From Client: commit successfully");
//            else
//                System.out.println("From Client: commit fail");
            //Formal Test: dieRMAfterEnlist

            //Formal Test: dieRMBeforePrepare
            int xid = wc.start();
            wc.dieRMBeforePrepare(ResourceManager.RMINameCars);
            wc.addCars(xid, "Nanjing", 300,320);
            if(wc.commit(xid))
                System.out.println("From Client: commit successfully");
            else
                System.out.println("From Client: commit fail");
            //Formal Test: end

//            example code for clinets
//            if (!wc.addFlight(xid, "347", 230, 999)) {
//                System.err.println("Add flight failed");
//            }
//            if (!wc.addRooms(xid, "SFO", 500, 150)) {
//                System.err.println("Add room failed");
//            }
//
//            System.out.println("Flight 347 has " +
//                    wc.queryFlight(xid, "347") +
//                    " seats.");
//            if (!wc.reserveFlight(xid, "John", "347")) {
//                System.err.println("Reserve flight failed");
//            }
//            System.out.println("Flight 347 now has " +
//                    wc.queryFlight(xid, "347") +
//                    " seats.");
//
//            if (!wc.commit(xid)) {
//                System.err.println("Commit failed");
//            }

        } catch (Exception e) {
            System.err.println("Received exception:" + e);
            System.exit(1);
        }

    }
}
