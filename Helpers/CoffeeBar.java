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
  private TreeMap<String, String> clientCount = new TreeMap<>();
  private Map<String, ArrayList<Integer>> startedThread = new HashMap<>();
  private Map<String, Orders> orders = new TreeMap<>(); // All Orders in Cafe Stored Here
  private final Semaphore teaSemaphore = new Semaphore(2); // At Most 2 Tea Threads Run Concurrently
  private final Semaphore coffeeSemaphore = new Semaphore(2); // At Most 2 Coffee Threads Run Concurrently

  // Constrcutor to store clients list
  public CoffeeBar(TreeMap<String, String> clientCount) {
    this.clientCount = clientCount;
  }

  // Add New Orders to Coffee Bar (WAITING)
  public String placeOrder(String clientSocket, int numOfTea, int numOfCoffee) {
    String isAddOn = "false";

    // Check if Customer is Adding New Orders (add-on). If not, Create New Order
    // Instance
    synchronized (orders) {
      if (orders.containsKey(clientSocket)) {
        isAddOn = "true";
        orders.get(clientSocket).addOnOrders(numOfTea, numOfCoffee); // Append Customer "add-on" Orders
      } else {
        Orders order = new Orders(numOfTea, numOfCoffee); // Create New Instance of Order for Customer
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
    // Get customer's drink ID of teas and coffees in WAITING AREA
    final ArrayList<Integer> teaWaiting = orders.get(clientSocket).getDrinkState(TEA, WAIT);
    final ArrayList<Integer> coffeeWaiting = orders.get(clientSocket).getDrinkState(COFFEE, WAIT);

    // Append Client to startedThread (Barista is noted for all orders in this "Queue")
    startedThread.putIfAbsent(clientSocket, new ArrayList<Integer>());

    // Brew Tea
    if (teaWaiting.size() > 0) {
      // Loop through "teaWaiting" (TEA WAITING AREA)
      for (Integer drinkID : teaWaiting) {
        // Start Thread only if "Barista is not noted about the order"
        if (!startedThread.get(clientSocket).contains(drinkID)) {
          // Thread to Brew Tea
          Thread brewTea = new Thread(() -> {
            try {
              teaSemaphore.acquire(); // At most 2 tea brew concurrently
              synchronized (orders) { // Use lock to avoid race condition
                orders.get(clientSocket).removeWaiting(drinkID); // Remove tea from WAITING AREA
                orders.get(clientSocket).setBrewing(drinkID, TEA); // Add tea to BREWING AREA
                displayCafeState(); // Display Cafe Status in Server Terminal
              }
              Thread.sleep(10000); // 30 seconds to fulfill a tea order
              startedThread.get(clientSocket).remove(drinkID); // Remove order from startedThread once its done
              finishBrewing(clientSocket, drinkID, TEA); // Handle current tea order fulfilled
            } catch (InterruptedException e) {
              e.printStackTrace();
            } finally {
              teaSemaphore.release(); // Release semaphore to allow remaining threads to run
            }
          });
          startedThread.get(clientSocket).add(drinkID); // Barista is noted for this order
          brewTea.start(); // Start brew tea thread
        }
      }
    }

    // Brew Coffee
    if (coffeeWaiting.size() > 0) {
      // Loop through "coffeeWaiting" (COFFEE WAITING AREA)
      for (Integer drinkID : coffeeWaiting) {
        // Start Thread only if "Barista is not noted about the order"
        if (!startedThread.get(clientSocket).contains(drinkID)) {
          // Thread to Brew Coffee
          Thread brewCoffee = new Thread(() -> {
            try {
              coffeeSemaphore.acquire(); // At most 2 coffee brew concurrently
              synchronized (orders) { // Use lock to avoid race condition
                orders.get(clientSocket).removeWaiting(drinkID); // Remove from coffee from WAITING AREA
                orders.get(clientSocket).setBrewing(drinkID, COFFEE); // Add coffee to BREWING AREA
                displayCafeState(); // Display Cafe Status in Server Terminal
              }
              Thread.sleep(15000); // 45 seconds to fulfill a coffee order
              startedThread.get(clientSocket).remove(drinkID); // Remove order from startedThread once its done
              finishBrewing(clientSocket, drinkID, COFFEE); // Handle coffee fulfilled
            } catch (InterruptedException e) {
              e.printStackTrace();
            } finally {
              coffeeSemaphore.release(); // Release semaphore to allow remaining threads to run
            }
          });
          startedThread.get(clientSocket).add(drinkID); // Barista is noted for this order
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

  // Method to Display Cafe Status
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

  // Remove Client if Disconnected
  public void removeClient(String clientSocket) {
    clientCount.remove(clientSocket);
  }
}