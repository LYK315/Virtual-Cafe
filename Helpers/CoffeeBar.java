package Helpers;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

public class CoffeeBar {
  private final String TEA = "tea";
  private final String COFFEE = "coffee";
  private final String WAIT = "waiting";
  private final String BREW = "brewing";
  private final String TRAY = "tray";
  private final String IDLE = "idle";
  private final Semaphore teaSemaphore = new Semaphore(2); // At Most 2 Tea Threads Run Concurrently
  private final Semaphore coffeeSemaphore = new Semaphore(2); // At Most 2 Coffee Threads Run Concurrently
  private TreeMap<String, String> clientCount = new TreeMap<>(); // All Customer in the Cafe <clientSocket, State>
  private TreeMap<String, Orders> orders = new TreeMap<>(); // All Orders in Cafe <clientSocket, Orders>
  private TreeMap<String, ServerLog> currentLog = new TreeMap<>(); // Store Server Log <timestamp, ServerLog>
  private HashMap<String, ArrayList<Integer>> drinkInQueue = new HashMap<>(); // <clientSocket,drinkIDList>
  private boolean shouldPause = false; // Pause Main Brewing Thread if True
  private Integer pauseDuration = 0; // Duration to Pause Main Brewing Threads

  // Constrcutor to store clients list
  public CoffeeBar(TreeMap<String, String> clientCount, TreeMap<String, ServerLog> currentLog) {
    this.clientCount = clientCount;
    this.currentLog = currentLog;
  }

  // Add New Orders to Coffee Bar (WAITING)
  public String placeOrder(String clientSocket, String customerName, int numOfTea, int numOfCoffee) {
    String isAddOn = "false";

    // Check if Customer Order is (add-on), Else Create New Order Instance
    synchronized (orders) {
      if (orders.containsKey(clientSocket)) {
        isAddOn = "true"; // Set "add-on" state to True
        orders.get(clientSocket).addOnOrders(numOfTea, numOfCoffee); // Append Customer "add-on" Orders
      } else {
        Orders order = new Orders(numOfTea, numOfCoffee, customerName); // Create New Order Instance for Customer
        orders.put(clientSocket, order); // Append customer order to the "Order Queue" in Cafe
        clientCount.put(clientSocket, WAIT); // Update Customer Status to WAITING ORDER
      }
      displayCafeState("Order Placed", true); // Print Cafe Status & Log
      startBrewing(clientSocket); // Start Processing Customer Order
    }

    return isAddOn; // Let client know customer is "add-on"
  }

  // Start Brewing Orders (WAITING > BREWING)
  public void startBrewing(String clientSocket) {
    // Get customer's drinkID for teas and coffees in WAITING AREA
    final ArrayList<Integer> teaWaiting = orders.get(clientSocket).getDrinkState(TEA, WAIT);
    final ArrayList<Integer> coffeeWaiting = orders.get(clientSocket).getDrinkState(COFFEE, WAIT);

    // Append Client to "Brewing Queue"
    drinkInQueue.putIfAbsent(clientSocket, new ArrayList<Integer>());

    // Brew Tea
    if (teaWaiting.size() > 0) {
      // Loop through "teaWaiting" (TEA WAITING AREA)
      for (Integer drinkID : teaWaiting) {
        // Add Tea Order to "Brewing Queue" and Start Thread
        if (!drinkInQueue.get(clientSocket).contains(drinkID)) {
          // Thread to Brew Tea
          Thread brewTea = new Thread(() -> {
            try {
              teaSemaphore.acquire(); // At Most 2 Tea Brew Concurrently

              // Pause if Transferring Orders, to avoid Race Condition or concurrency error
              if (shouldPause) {
                try {
                  Thread.sleep(pauseDuration);
                } catch (Exception e) {
                }
              }

              // Brew Tea only if Tea is in "Brewing Queue"
              if (drinkInQueue.containsKey(clientSocket)) {
                if (drinkInQueue.get(clientSocket).contains(drinkID)) {
                  synchronized (orders) { // Use synchronize to avoid Race Condition
                    orders.get(clientSocket).removeWaiting(drinkID); // Remove Tea from WAITING AREA
                    orders.get(clientSocket).setBrewing(drinkID, TEA); // Add Tea to BREWING AREA
                    displayCafeState("Update Brewing Area", true); // Print Cafe Status & Log
                  }
                  synchronized (drinkInQueue) {
                    drinkInQueue.get(clientSocket).remove(drinkID); // Remove Tea from "Brewing Queue"
                  }
                  Thread.sleep(30000); // 30 Seconds to Fulfill a Tea Order
                  finishBrewing(clientSocket, drinkID, TEA); // Handle Tea Order fulfilled
                }
              }
            } catch (InterruptedException e) {
              e.printStackTrace();
            } finally {
              teaSemaphore.release(); // Release semaphore to allow remaining threads to run
            }
          });
          synchronized (drinkInQueue) {
            drinkInQueue.get(clientSocket).add(drinkID); // Add Tea order to "Brewing Queue"
          }
          brewTea.start(); // Start brew tea thread
        }
      }
    }

    // Brew Coffee
    if (coffeeWaiting.size() > 0) {
      // Loop through "coffeeWaiting" (COFFEE WAITING AREA)
      for (Integer drinkID : coffeeWaiting) {
        // Add Coffee Order to "Brewing Queue" and Start Thread
        if (!drinkInQueue.get(clientSocket).contains(drinkID)) {
          // Thread to Brew Coffee
          Thread brewCoffee = new Thread(() -> {
            try {
              coffeeSemaphore.acquire(); // At most 2 coffee brew concurrently

              // Pause if Server is Transferring Orders
              if (shouldPause) {
                try {
                  Thread.sleep(pauseDuration);
                } catch (Exception e) {
                }
              }

              // Brew Coffee only if Coffee is in "Brewing Queue"
              if (drinkInQueue.containsKey(clientSocket)) {
                if (drinkInQueue.get(clientSocket).contains(drinkID)) {
                  synchronized (orders) {
                    orders.get(clientSocket).removeWaiting(drinkID); // Remove from Coffee from WAITING AREA
                    orders.get(clientSocket).setBrewing(drinkID, COFFEE); // Add Coffee to BREWING AREA
                    displayCafeState("Update Brewing Area", true); // Print Cafe Status & Log
                  }
                  synchronized (drinkInQueue) {
                    drinkInQueue.get(clientSocket).remove(drinkID); // Remove Coffee from "Brewing Queue"
                  }
                  Thread.sleep(45000); // 45 Seconds to Fulfill a Coffee Order
                  finishBrewing(clientSocket, drinkID, COFFEE); // Handle Coffee Order Fulfilled
                }
              }
            } catch (InterruptedException e) {
              e.printStackTrace();
            } finally {
              coffeeSemaphore.release(); // Release semaphore to allow remaining threads to run
            }
          });
          synchronized (drinkInQueue) {
            drinkInQueue.get(clientSocket).add(drinkID); // Add Coffee order to "Brewing Queue"
          }
          brewCoffee.start(); // Start brew coffee thread
        }
      }
    }

  }

  // Orders Finished Brewing (BREWING > TRAY)
  public void finishBrewing(String clientSocket, Integer drinkID, String drinkType) {
    synchronized (orders) {
      orders.get(clientSocket).removeBrewing(drinkID); // Remove drink from BREWING AREA
      orders.get(clientSocket).setInTray(drinkID, drinkType); // Add drink to TRAY
      displayCafeState("Update Tray Area", true); // Print Cafe Status & Log
    }
  }

  // All Orders Fulfilled (REMOVE ALL DRINKS IN TRAY)
  public void ordersFulfilled(String clientSocket) {
    synchronized (orders) {
      clientCount.put(clientSocket, IDLE); // Update Customer status to IDLE
      orders.get(clientSocket).removeAllInTray(); // Remove all drinks in TRAY AREA
      orders.remove(clientSocket); // Remove client from "Order Queue"
      drinkInQueue.remove(clientSocket); // Remove client from "Brewing Queue"
      displayCafeState("Order Delivered", true); // Print Cafe Status & Log
    }
  }

  // Retrieve Order Status
  public Map<String, Integer> getOrderStatus(String clientSocket) {
    Map<String, Integer> orderStatus = new HashMap<>();

    synchronized (orders) {
      // Check if Customer has Order in "Order Queue"
      if (orders.containsKey(clientSocket) && !orders.isEmpty()) {
        // Retrieve Tea orders
        orderStatus.put("tea_waiting", orders.get(clientSocket).getDrinkState(TEA, WAIT).size());
        orderStatus.put("tea_brewing", orders.get(clientSocket).getDrinkState(TEA, BREW).size());
        orderStatus.put("tea_tray", orders.get(clientSocket).getDrinkState(TEA, TRAY).size());

        // Retrieve Coffee orders
        orderStatus.put("coffee_waiting", orders.get(clientSocket).getDrinkState(COFFEE, WAIT).size());
        orderStatus.put("coffee_brewing", orders.get(clientSocket).getDrinkState(COFFEE, BREW).size());
        orderStatus.put("coffee_tray", orders.get(clientSocket).getDrinkState(COFFEE, TRAY).size());
      }
    }

    // Response to client
    return orderStatus;
  }

  // All Customers in WAITING STATE (Waiting orders to be delivered)
  public int customerWaiting() {
    int customersWaiting = 0;

    synchronized (clientCount) {
      for (String key : clientCount.keySet()) {
        if (clientCount.get(key).equals(WAIT)) {
          customersWaiting += 1;
        }
      }
    }

    return customersWaiting;
  }

  // All Orders in Each Area (WAITING, BREWING, TRAY)
  public int getAllStatus(String drinkType, String drinkState) {
    int drinkCount = 0;

    synchronized (orders) {
      for (String key : orders.keySet()) {
        drinkCount += orders.get(key).getDrinkState(drinkType, drinkState).size();
      }
    }

    return drinkCount;
  }

  // Print Cafe Status & Log
  public void displayCafeState(String serverMsg, boolean displayTerminal) {
    Integer clientInCafe, clientWaitingOrder;
    Integer teaWaiting, teaBrewing, teaInTray;
    Integer coffeeWaiting, coffeeBrewing, coffeeInTray;

    // Retrieve current Cafe State
    clientInCafe = clientCount.size();
    clientWaitingOrder = customerWaiting();
    teaWaiting = getAllStatus(TEA, WAIT);
    teaBrewing = getAllStatus(TEA, BREW);
    teaInTray = getAllStatus(TEA, TRAY);
    coffeeWaiting = getAllStatus(COFFEE, WAIT);
    coffeeBrewing = getAllStatus(COFFEE, BREW);
    coffeeInTray = getAllStatus(COFFEE, TRAY);

    // Display in Server Terminal Screen if displayTerminal = true
    if (displayTerminal) {
      System.out.println("\nClients in Cafe: " + clientInCafe);
      System.out.println("Clients Waiting: " + clientWaitingOrder);
      System.out.println("Orders Waiting : " + "Tea(" + teaWaiting + ") & Coffee(" + coffeeWaiting + ")");
      System.out.println("Orders Brewing : " + "Tea(" + teaBrewing + ") & Coffee(" + coffeeBrewing + ")");
      System.out.println("Orders in Tray : " + "Tea(" + teaInTray + ") & Coffee(" + coffeeInTray + ")\n");
    }

    // Update Cafe State in Server Log Class
    ServerLog serverLog = new ServerLog(); // Class to store server log to write to JSON
    serverLog.setClientInCafe(clientInCafe);
    serverLog.setClientWaitingOrder(clientWaitingOrder);
    serverLog.setOrdersWaiting("Tea(" + teaWaiting + ") and Coffee(" + coffeeWaiting + ")");
    serverLog.setOrdersBrewing("Tea(" + teaBrewing + ") and Coffee(" + coffeeBrewing + ")");
    serverLog.setOrdersInTray("Tea(" + teaInTray + ") and Coffee(" + coffeeInTray + ")");
    serverLog.setServerMsg(serverMsg);

    // Get Current System Date & Time
    ZonedDateTime currentTimestampWithZone = ZonedDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String currentDateTime = currentTimestampWithZone.format(formatter);

    // Append Server Log
    currentLog.put(currentDateTime, serverLog);

    try {
      Thread.sleep(700);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  // Transfer Customer Order
  public void transferOrder(String fromClient) {
    // Use an Independent Thread to Transfer Order
    Thread waitAndTransfer = new Thread(() -> {
      String serverMsg;

      // Order detail of (frmClient)
      Orders fromCustomer = orders.get(fromClient);
      String fromCustomerName = orders.get(fromClient).getCustomerName();

      // Server Message
      serverMsg = "Checking if " + fromCustomerName + " orders can be transferred..";
      System.out.println(serverMsg);
      displayCafeState(serverMsg, false);

      // Remove All Orders in (frmClient) WAITING AREA
      Integer frmTeaWaiting, frmCoffeeWaiting;
      frmTeaWaiting = fromCustomer.getDrinkState(TEA, WAIT).size();
      frmCoffeeWaiting = fromCustomer.getDrinkState(COFFEE, WAIT).size();
      if (frmTeaWaiting + frmCoffeeWaiting > 0) {
        if (frmTeaWaiting > 0) { // Remove Tea Order
          for (Integer drinkID : fromCustomer.getDrinkState(TEA, WAIT)) {
            synchronized (orders) {
              fromCustomer.removeWaiting(drinkID); // Remove reamaining tea in WAITING AREA
            }
            drinkInQueue.get(fromClient).remove(drinkID); // Remove remaining tea in 'Queue to Brew'
          }
        }
        if (frmCoffeeWaiting > 0) { // Remove Coffee Order
          for (Integer drinkID : fromCustomer.getDrinkState(COFFEE, WAIT)) {
            synchronized (orders) {
              fromCustomer.removeWaiting(drinkID); // Remove reamaining coffee in WAITING AREA
            }
            drinkInQueue.get(fromClient).remove(drinkID); // Remove remaining coffee in 'Queue to Brew'
          }
        }

        serverMsg = "Removed " + fromCustomerName + " orders in waiting area " + "Tea(" + frmTeaWaiting + ") "
            + "Coffee(" + frmCoffeeWaiting + ")";
        System.out.println(serverMsg);
        displayCafeState(serverMsg, false);
      }

      // Wait for (frmClient) Orders in BREWING AREA to Complete
      if (fromCustomer.getDrinkState(TEA, BREW).size() + fromCustomer.getDrinkState(COFFEE, BREW).size() > 0) {
        serverMsg = "Finishing " + fromCustomerName + " orders in brewing area..";
        System.out.println(serverMsg);
        displayCafeState(serverMsg, false);

        if (fromCustomer.getDrinkState(COFFEE, BREW).size() > 0) { // Got Coffee in BREWING AREA
          pauseDuration = 47000; // Duration to Pause
          shouldPause = true; // Pause the main Coffee Brewing Process
          try {
            Thread.sleep(45000); // Wait for (frmClient) Coffee to complete
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        } else if (fromCustomer.getDrinkState(TEA, BREW).size() > 0) { // Only Got Tea in BREWING AREA
          pauseDuration = 32000; // Duration to Pause
          shouldPause = true; // Pause the main Tea Brewing Process
          try {
            Thread.sleep(30000); // Wait for (frmClient) Tea to complete
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }

      // Retrieve (frmClient) Initial Tea & Coffee Count in TRAY AREA
      ArrayList<Integer> initFromTeaTray = fromCustomer.getDrinkState(TEA, TRAY);
      ArrayList<Integer> initFromCoffeeTray = fromCustomer.getDrinkState(COFFEE, TRAY);

      // Transfer Orders (frmClient) > (toClient)
      Iterator<Map.Entry<String, Orders>> iterator = orders.entrySet().iterator();
      while (iterator.hasNext()) { // Loop Through All Customer in Order Queue
        Map.Entry<String, Orders> entry = iterator.next();
        String toClient = entry.getKey();

        // Retrieve (frmClient) Tea & Coffee in TRAY AREA
        ArrayList<Integer> fromTeaTray = fromCustomer.getDrinkState(TEA, TRAY);
        ArrayList<Integer> fromCoffeeTray = fromCustomer.getDrinkState(COFFEE, TRAY);

        if (!toClient.equals(fromClient)) { // Skip if Loop Through (frmClient) in the Queue
          // Order detail of (toClient)
          Orders toCustomer = orders.get(toClient);
          String toCustomerName = orders.get(toClient).getCustomerName();

          // Retrieve (toClient) Tea & Coffee Count in WAITING AREA
          int toTeaWaiting = toCustomer.getDrinkState(TEA, WAIT).size();
          int toCoffeeWaiting = toCustomer.getDrinkState(COFFEE, WAIT).size();

          // Transfer Tea
          Integer teaTransferred = 0;
          if (fromTeaTray.size() > 0 && toTeaWaiting > 0) {
            // (toClient) Tea is More than / Same to (frmClient) Tea in WAITING AREA
            if (toTeaWaiting > fromTeaTray.size() || toTeaWaiting == fromTeaTray.size()) {
              teaTransferred = fromTeaTray.size();
              // Remove All (frmClient) Tea in TRAY AREA
              for (Integer frmTeaID : fromTeaTray) {
                synchronized (orders) {
                  fromCustomer.removeInTray(frmTeaID);
                }
              }
              // Transfer all (frmClient) Tea in TRAY to (toClient)
              for (int i = 0; i < fromTeaTray.size(); i++) {
                Integer toTeaID = toCustomer.getDrinkState(TEA, WAIT).get(0);
                synchronized (orders) {
                  toCustomer.removeWaiting(toTeaID); // Remove (toClient) Tea from WAITING AREA
                  toCustomer.setInTray(toTeaID, TEA); // Remove (toClient) Tea to TRAY AREA
                  drinkInQueue.get(toClient).remove(toTeaID); // Remove (toClient) Tea in "Queue to Brew"
                }
              }
            }
            // (toClient) Tea Lesser than (frmClient) Tea in WAITING AREA
            else if (toTeaWaiting < fromTeaTray.size()) {
              teaTransferred = toTeaWaiting;
              // Remove (frmClient) Tea in TRAY AREA
              int j = 0;
              for (Integer frmTeaID : fromTeaTray) {
                if (j < toTeaWaiting) {
                  j++;
                  synchronized (orders) {
                    fromCustomer.removeInTray(frmTeaID);
                  }
                } else
                  break;
              }
              // Transfer (frmClient) Tea in TRAY to (toClient)
              for (int i = 0; i < toTeaWaiting; i++) {
                Integer toTeaID = toCustomer.getDrinkState(TEA, WAIT).get(0);
                synchronized (orders) {
                  toCustomer.removeWaiting(toTeaID); // Remove (toClient) Tea from WAITING AREA
                  toCustomer.setInTray(toTeaID, TEA); // Remove (toClient) Tea to TRAY AREA
                  drinkInQueue.get(toClient).remove(toTeaID); // Remove (toClient) Tea in "Queue to Brew"
                }
              }
            }
            serverMsg = "(" + teaTransferred.toString() + ")Tea transferred from " + fromCustomerName + " to "
                + toCustomerName;
            System.out.println(serverMsg);
            displayCafeState(serverMsg, true);
          }

          // Transfer Coffee
          Integer coffeeTransferred = 0;
          if (fromCoffeeTray.size() > 0 && toCoffeeWaiting > 0) {
            // (toClient) Coffee is More than / Same to (frmClient) Coffee in WAITING AREA
            if (toCoffeeWaiting > fromCoffeeTray.size() || toCoffeeWaiting == fromCoffeeTray.size()) {
              coffeeTransferred = fromCoffeeTray.size();
              // Remove (frmClient) Coffee in TRAY AREA
              for (Integer frmCoffeeID : fromCoffeeTray) {
                synchronized (orders) {
                  fromCustomer.removeInTray(frmCoffeeID);
                }
              }
              // Transfer all (frmClient) Coffee in TRAY to (toClient)
              for (int i = 0; i < fromCoffeeTray.size(); i++) {
                Integer toCoffeeID = toCustomer.getDrinkState(COFFEE, WAIT).get(0);
                synchronized (orders) {
                  toCustomer.removeWaiting(toCoffeeID); // Remove (toClient) Coffee from WAITING AREA
                  toCustomer.setInTray(toCoffeeID, COFFEE); // Remove (toClient) Coffee to TRAY AREA
                  drinkInQueue.get(toClient).remove(toCoffeeID); // Remove (toClient) Coffee in "Queue to Brew"
                }
              }
            }
            // (toClient) Coffee Lesser than (frmClient) Coffee in WAITING AREA
            else if (toCoffeeWaiting < fromCoffeeTray.size()) {
              coffeeTransferred = toCoffeeWaiting;
              // Remove (frmClient) Coffee in TRAY AREA
              int j = 0;
              for (Integer frmCoffeeID : fromCoffeeTray) {
                if (j < toCoffeeWaiting) {
                  j++;
                  synchronized (orders) {
                    fromCustomer.removeInTray(frmCoffeeID);
                  }
                } else
                  break;
              }
              // Transfer (frmClient) Coffee in TRAY to (toClient)
              for (int i = 0; i < toCoffeeWaiting; i++) {
                Integer toCoffeeID = toCustomer.getDrinkState(COFFEE, WAIT).get(0);
                synchronized (orders) {
                  toCustomer.removeWaiting(toCoffeeID); // Remove (toClient) Coffee from WAITING AREA
                  toCustomer.setInTray(toCoffeeID, COFFEE); // Remove (toClient) Coffee to TRAY AREA
                  drinkInQueue.get(toClient).remove(toCoffeeID); // Remove (toClient) Coffee in "Queue to Brew"
                }
              }
            }
            serverMsg = "(" + coffeeTransferred.toString() + ")Coffee transferred from " + fromCustomerName + " to "
                + toCustomerName;
            System.out.println(serverMsg);
            displayCafeState(serverMsg, true);
          }
        }
      }

      // Display msg if (frmClient) orders is not transferred to any other customer
      if (initFromTeaTray.size() + initFromCoffeeTray.size() == fromCustomer.getDrinkState(TEA, TRAY).size()
          + fromCustomer.getDrinkState(COFFEE, TRAY).size()) {

        serverMsg = fromCustomerName + " remaining orders is not transferred to other customer";
        System.out.println(serverMsg);
        displayCafeState(serverMsg, false);
      }

      // Remove All (frmClient) Orders in TRAY (if still have orders not transferred)
      // Simply remove (frmClient) from "orders" object
      int remainFrmTeaTray, remainFrmCoffeeTray;
      remainFrmTeaTray = fromCustomer.getDrinkState(TEA, TRAY).size();
      remainFrmCoffeeTray = fromCustomer.getDrinkState(COFFEE, TRAY).size();
      if (remainFrmTeaTray > 0) {
        if (remainFrmCoffeeTray > 0) {
          serverMsg = "(" + remainFrmTeaTray + ")Tea and (" + remainFrmCoffeeTray + ") removed from " + fromCustomerName
              + " Tray";
        } else {
          serverMsg = "(" + remainFrmTeaTray + ")Tea removed from " + fromCustomerName + " Tray";
        }
      } else if (remainFrmCoffeeTray > 0) {
        serverMsg = "(" + remainFrmCoffeeTray + ")Coffee removed from " + fromCustomerName + " Tray";
      }
      
      // Remove (frmClient) from "orders" object
      orders.remove(fromClient);
      System.out.println(serverMsg);
      displayCafeState(serverMsg, true);

      shouldPause = false; // Continue Other Brew Process
    });

    waitAndTransfer.start(); // Start the thread
  }

  // Remove Client from Cafe
  public void removeClient(String clientSocket) {
    clientCount.remove(clientSocket);
  }

}