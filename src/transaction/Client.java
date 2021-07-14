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
//            wc.addFlight(xid, "12",12,12);
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

            //TEST: FREE QUERY
            try {
                wc.reconnect();
                int xid = wc.start();
                int remainSeats = wc.queryFlight(xid, "17");
                System.out.println("Flight 17 has " + remainSeats + " seats");
                //int remainRooms = wc.queryRooms(xid, "b");
                //System.out.println("Hotel in b has " + remainRooms + " rooms");
                wc.commit(xid);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            //TEST: Free query end



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
