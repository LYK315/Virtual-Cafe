package Helpers;

import java.util.ArrayList;
import java.util.HashMap;
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
  private TreeMap<String, String> clientCount = new TreeMap<>(); // All Customer in the Cafe
  private Map<String, ArrayList<Integer>> drinkInQueue = new HashMap<>(); // "Brewing Queue"
  private Map<String, Orders> orders = new TreeMap<>(); // All Orders in Cafe Stored Here
  private boolean shouldPause = false; // Pause Main Brewing Thread if True
  private Integer pauseDuration = 0;

  // Constrcutor to store clients list
  public CoffeeBar(TreeMap<String, String> clientCount) {
    this.clientCount = clientCount;
  }

  // Add New Orders to Coffee Bar (WAITING)
  public String placeOrder(String clientSocket, String customerName, int numOfTea, int numOfCoffee) {
    String isAddOn = "false";

    // Check if Customer is Adding New Orders (add-on). If not, Create New Order
    // Instance
    synchronized (orders) {
      if (orders.containsKey(clientSocket)) {
        isAddOn = "true";
        orders.get(clientSocket).addOnOrders(numOfTea, numOfCoffee); // Append Customer "add-on" Orders
      } else {
        Orders order = new Orders(numOfTea, numOfCoffee, customerName); // Create New Instance of Order for Customer
        orders.put(clientSocket, order); // Append customer's order to the "Order Queue" in Cafe
        clientCount.put(clientSocket, WAIT); // Update Customer Status to WAITING ORDER
      }
      displayCafeState(); // Display Cafe Status in Server Terminal
      startBrewing(clientSocket); // Start Processing Customer's Order
    }

    return isAddOn; // Let client know customer is adding on new orders (display different msg)
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

              // Pause if Server is Transferring Orders
              if (shouldPause) {
                try {
                  Thread.sleep(pauseDuration);
                } catch (Exception e) {
                }
              }

              // Brew Tea only if Tea is in "Brewing Queue"
              if (drinkInQueue.get(clientSocket).contains(drinkID)) {
                synchronized (orders) { // Use lock to avoid race condition
                  orders.get(clientSocket).removeWaiting(drinkID); // Remove tea from WAITING AREA
                  orders.get(clientSocket).setBrewing(drinkID, TEA); // Add tea to BREWING AREA
                  displayCafeState(); // Display Cafe Status in Server Terminal
                }
                synchronized (drinkInQueue) {
                  drinkInQueue.get(clientSocket).remove(drinkID); // Remove Tea from "Brewing Queue"
                }
                Thread.sleep(10000); // 30 seconds to Fulfill a Tea Order
                finishBrewing(clientSocket, drinkID, TEA); // Handle Tea Order fulfilled
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
              if (drinkInQueue.get(clientSocket).contains(drinkID)) {
                synchronized (orders) { // Use lock to avoid race condition
                  orders.get(clientSocket).removeWaiting(drinkID); // Remove from coffee from WAITING AREA
                  orders.get(clientSocket).setBrewing(drinkID, COFFEE); // Add coffee to BREWING AREA
                  displayCafeState(); // Display Cafe Status in Server Terminal
                }
                synchronized (drinkInQueue) {
                  drinkInQueue.get(clientSocket).remove(drinkID); // Remove Coffee from "Brewing Queue"
                }
                Thread.sleep(15000); // 45 Seconds to Fulfill a Coffee Order
                finishBrewing(clientSocket, drinkID, COFFEE); // Handle Coffee Order Fulfilled
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
      displayCafeState(); // Display Cafe Status in Server Terminal
    }
  }

  // All Orders Fulfilled (REMOVE ALL DRINKS IN TRAY)
  public void ordersFulfilled(String clientSocket) {
    synchronized (orders) {
      clientCount.put(clientSocket, IDLE); // Update Customer status to IDLE
      orders.get(clientSocket).removeAllInTray(); // Remove all drinks in TRAY AREA
      orders.remove(clientSocket); // Remove client from "Order Queue"
      drinkInQueue.remove(clientSocket); // Remove client from "Barista noted the order"
      displayCafeState(); // Display Cafe Status in Server Terminal
    }
  }

  // Retrieve Order Status
  public Map<String, Integer> getOrderStatus(String clientSocket) {
    Map<String, Integer> orderStatus = new HashMap<>();

    synchronized (orders) {
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

  // All Customers Orders in Each Area (WAITING, BREWING, TRAY)
  public int getAllStatus(String drinkType, String drinkState) {
    int drinkCount = 0;

    synchronized (orders) {
      for (String key : orders.keySet()) {
        drinkCount += orders.get(key).getDrinkState(drinkType, drinkState).size();
      }
    }

    return drinkCount;
  }

  // Display Cafe Status
  public void displayCafeState() {
    System.out.println("\nClients in Cafe: " + clientCount.size());
    System.out.println("Clients Waiting: " + customerWaiting());

    System.out.println("Orders Waiting : " + "Tea(" + getAllStatus(TEA, WAIT) + ") & Coffee("
        + getAllStatus(COFFEE, WAIT) + ")");

    System.out.println("Orders Brewing : " + "Tea(" + getAllStatus(TEA, BREW) + ") & Coffee("
        + getAllStatus(COFFEE, BREW) + ")");

    System.out.println("Orders in Tray : " + "Tea(" + getAllStatus(TEA, TRAY) + ") & Coffee("
        + getAllStatus(COFFEE, TRAY) + ")\n");
  }

  // Transfer Customer Order
  public void transferOrder(String fromClient) {

    Thread waitAndTransfer = new Thread(() -> {
      // Order detail of (frmClient)
      Orders fromCustomer = orders.get(fromClient);
      String fromCustomerName = orders.get(fromClient).getCustomerName();

      System.out.println("Checking if " + fromCustomerName + "'s orders can be transferred..");

      // Remove Orders in (frmClient) WAITING AREA
      if (fromCustomer.getDrinkState(TEA, WAIT).size() + fromCustomer.getDrinkState(COFFEE, WAIT).size() > 0) {
        if (fromCustomer.getDrinkState(TEA, WAIT).size() > 0) { // Remove Tea Order
          for (Integer drinkID : fromCustomer.getDrinkState(TEA, WAIT)) {
            fromCustomer.removeWaiting(drinkID); // Remove reamaining tea in WAITING AREA
            drinkInQueue.get(fromClient).remove(drinkID); // Remove remaining tea in 'Queue to Brew'
          }
        }
        if (fromCustomer.getDrinkState(COFFEE, WAIT).size() > 0) { // Remove Coffee Order
          for (Integer drinkID : fromCustomer.getDrinkState(COFFEE, WAIT)) {
            fromCustomer.removeWaiting(drinkID); // Remove reamaining coffee in WAITING AREA
            drinkInQueue.get(fromClient).remove(drinkID); // Remove remaining coffee in 'Queue to Brew'
          }
        }
        System.out.println("Removed " + fromCustomerName + "'s orders in waiting area");
      }

      // Wait for (frmClient) Orders in BREWING AREA to complete
      if (fromCustomer.getDrinkState(TEA, BREW).size() + fromCustomer.getDrinkState(COFFEE, BREW).size() > 0) {
        System.out.println("Finishing " + fromCustomerName + "'s orders in brewing area");
        if (fromCustomer.getDrinkState(COFFEE, BREW).size() > 0) { // Got Coffee in BREWING AREA
          pauseDuration = 16000; // Duration to Pause
          shouldPause = true; // Pause the main Coffee Brewing Process
          try {
            Thread.sleep(15000); // Wait for (frmClient) Coffee to complete
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        } else if (fromCustomer.getDrinkState(TEA, BREW).size() > 0) { // Only Got Tea in BREWING AREA
          pauseDuration = 11000; // Duration to Pause
          shouldPause = true; // Pause the main Tea Brewing Process
          try {
            Thread.sleep(10000); // Wait for (frmClient) Tea to complete
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }

      // Retrieve (frmClient) Tea & Coffee in TRAY AREA
      ArrayList<Integer> fromTeaTray = fromCustomer.getDrinkState(TEA, TRAY);
      ArrayList<Integer> fromCoffeeTray = fromCustomer.getDrinkState(COFFEE, TRAY);

      // Transfer Orders (frmClient) > (toClient)
      for (String toClient : orders.keySet()) { // Loop Through All Customer in Order Queue
        if (!toClient.equals(fromClient)) { // Skip if Loop Through (frmClient) in the Queue
          // Order detail of (toClient)
          Orders toCustomer = orders.get(toClient);
          String toCustomerName = orders.get(toClient).getCustomerName();

          // Retrieve (toClient) Tea & Coffee Count in WAITING AREA
          int toTeaWaiting = toCustomer.getDrinkState(TEA, WAIT).size();
          int toCoffeeWaiting = toCustomer.getDrinkState(COFFEE, WAIT).size();

          // Transfer Tea
          if (toTeaWaiting > 0) {
            // (toClient) Tea is More than / Same to (frmClient) Tea in WAITING AREA
            if (toTeaWaiting > fromTeaTray.size() || toTeaWaiting == fromTeaTray.size()) {
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
            System.out.println("\nTea transferred from " + fromCustomerName + " to " + toCustomerName);
          }

          // Transfer Coffee
          if (toCoffeeWaiting > 0) {
            // (toClient) Coffee is More than / Same to (frmClient) Coffee in WAITING AREA
            if (toCoffeeWaiting > fromCoffeeTray.size() || toCoffeeWaiting == fromCoffeeTray.size()) {
              // Remove (frmClient) Coffee in TRAY AREA
              synchronized (orders) {
                // Can delete all in Tray, bcz All Tea in Tray is deleted, only Coffee Left
                fromCustomer.removeAllInTray();
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
            System.out.println("\nCoffee transferred from " + fromCustomerName + " to " + toCustomerName);
          }
        }
      }
      displayCafeState();
      shouldPause = false; // Continue Other Brew Process
    });

    waitAndTransfer.start();
  }

  // Remove Client if Disconnected
  public void removeClient(String clientSocket) {
    clientCount.remove(clientSocket);
  }

}