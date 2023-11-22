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
  private ArrayList<String> clientCount = new ArrayList<>();
  private Map<String, ArrayList<Integer>> startedThread = new HashMap<>();

  // All Orders in Cafe Stored Here
  private Map<String, Orders> orders = new TreeMap<>();

  // At Most 2 Tea and Coffee Threads Run Concurrently
  private final Semaphore teaSemaphore = new Semaphore(2);
  private final Semaphore coffeeSemaphore = new Semaphore(2);

  // Constrcutor to store clients list
  public CoffeeBar(ArrayList<String> clientCount) {
    this.clientCount = clientCount;
  }

  // Add New Orders to Coffee Bar (WAITING)
  public String placeOrder(String clientSocket, int numOfTea, int numOfCoffee) {
    String isAddOn = "false";
    // Check if customer has previous order, add new order instance if no, else
    // append order to previous order
    synchronized (orders) {
      if (orders.containsKey(clientSocket)) {
        isAddOn = "true";
        orders.get(clientSocket).addOnOrders(numOfTea, numOfCoffee);
      } else {
        Orders order = new Orders(numOfTea, numOfCoffee);
        orders.put(clientSocket, order);
      }
      startBrewing(clientSocket); // Add order into Queue and Start Brewing
    }
    displayCafeState(); // Display Cafe Status in Server Terminal

    return isAddOn;
  }

  // Start Brewing Orders (WAITING > BREWING)
  public void startBrewing(String clientSocket) {
    // Get drink ID of teas and coffees in WAITING AREA
    final ArrayList<Integer> teaWaiting = orders.get(clientSocket).getDrinkState(TEA, WAIT);
    final ArrayList<Integer> coffeeWaiting = orders.get(clientSocket).getDrinkState(COFFEE, WAIT);

    // Append Client Socket Key to startedThread it is not already in the Map
    startedThread.putIfAbsent(clientSocket, new ArrayList<Integer>());

    // Brew Tea
    if (teaWaiting.size() > 0) {
      // Loop through "teaWaiting" (TEA WAITING AREA)
      for (Integer drinkID : teaWaiting) {
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
              finishBrewing(clientSocket, drinkID, TEA); // Handle current tea order fulfilled
            } catch (InterruptedException e) {
              e.printStackTrace();
            } finally {
              teaSemaphore.release(); // Release semaphore to allow remaining threads to run
            }
          });
          startedThread.get(clientSocket).add(drinkID); // Append Drink ID to Started Thread List
          brewTea.start(); // Start brew tea thread
        }
      }
    }

    // Brew Coffee
    if (coffeeWaiting.size() > 0) {
      // Loop through "coffeeWaiting" (COFFEE WAITING AREA)
      for (Integer drinkID : coffeeWaiting) {
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
            finishBrewing(clientSocket, drinkID, COFFEE); // Handle coffee fulfilled
          } catch (InterruptedException e) {
            e.printStackTrace();
          } finally {
            coffeeSemaphore.release(); // Release semaphore to allow remaining threads to run
          }
        });
        brewCoffee.start(); // Start brew coffee thread
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
      orders.get(clientSocket).removeAllInTray();
      displayCafeState(); // Display Cafe Status in Server Terminal
    }
  }

  // Retrieve Order Status
  public Map<String, Integer> getOrderStatus(String clientSocket) {
    Map<String, Integer> orderStatus = new HashMap<>();

    synchronized (orders) {
      if (!orders.isEmpty() && orders.containsKey(clientSocket)) {
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

  // All Customers Waiting Order to be Delivered
  public int customerWaiting() {
    int customersWaiting = 0;

    synchronized (orders) {
      for (String key : orders.keySet()) {
        if (orders.get(key).isWaitingOrder())
          customersWaiting += 1;
      }
    }

    return customersWaiting;
  }

  // All Customers Orders in Each Area (Waiting, Brewing, Tray)
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
}