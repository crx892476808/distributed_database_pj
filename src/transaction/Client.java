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

            //Test: Add flight with BeforePrepare errors
            wc.reconnect();
            //wc.dieRMBeforeCommit(ResourceManager.RMINameFlights);
            int xid = wc.start();
            System.out.println(xid);
            //wc.dieRMBeforePrepare(ResourceManager.RMINameFlights);
            //wc.addFlight(xid, "349", 232, 1001);
            //wc.deleteFlight(xid, "349");
            int remainSeats = wc.queryFlight(xid, "349");
            System.out.println("Flight 347 has " + remainSeats + "seats");
            wc.commit(xid);
            //Test: end



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
