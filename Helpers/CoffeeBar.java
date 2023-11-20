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

  // All Orders are Stored Here
  private Map<String, Orders> orders = new TreeMap<>();

  // At Most 2 Tea and Coffee Thread Runs Concurrently
  private final Semaphore teaSemaphore = new Semaphore(2);
  private final Semaphore coffeeSemaphore = new Semaphore(2);

  // Add New Orders to Coffee Bar (WAITING)
  public void placeOrder(String customerName, int numOfTea, int numOfCoffee) {
    // Check if customer has previous order, add new order instance if no, else
    // append order to previous order
    Orders order = new Orders(customerName, numOfTea, numOfCoffee);
    // ADD ANOTHER CONSTRUCTOR FOR USER 2ND ORDER, IMPLEMENT ORDER ID
    orders.put(customerName, order);
  }

  // Start Brewing Drinks (WAITING > BREWING)
  public void startBrewing(String customerName) {
    // Get teas and coffees in WAITING AREA
    final ArrayList<Integer> teaWaiting = orders.get(customerName).getDrinkState(TEA, WAIT);
    final ArrayList<Integer> coffeeWaiting = orders.get(customerName).getDrinkState(COFFEE, WAIT);

    // Brew Tea
    if (teaWaiting.size() > 0) {
      // Loop through "teaWaiting" (TEA WAITING AREA)
      for (Integer drinkID : teaWaiting) {
        // Thread to Brew Tea
        Thread brewTea = new Thread(() -> {
          try {
            teaSemaphore.acquire(); // At most 2 tea brew concurrently
            synchronized (orders) { // Use lock to avoid race condition
              orders.get(customerName).removeWaiting(drinkID); // Remove tea from WAITING AREA
              orders.get(customerName).setBrewing(drinkID, TEA); // Add tea to BREWING AREA
            }
            Thread.sleep(5000); // 30 seconds to fulfill a tea order
            finishBrewing(customerName, drinkID, TEA); // Handle current tea order fulfilled
          } catch (InterruptedException e) {
            e.printStackTrace();
          } finally {
            teaSemaphore.release(); // Release semaphore to allow remaining threads to run
          }
        });
        brewTea.start(); // Start brew tea thread
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
              orders.get(customerName).removeWaiting(drinkID); // Remove from coffee from WAITING AREA
              orders.get(customerName).setBrewing(drinkID, COFFEE); // Add coffee to BREWING AREA
            }
            Thread.sleep(10000); // 45 seconds to fulfill a coffee order
            finishBrewing(customerName, drinkID, COFFEE); // Handle coffee fulfilled
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

  // Finish Brewing Drinks (BREWING > TRAY)
  public void finishBrewing(String customerName, Integer drinkID, String drinkType) {
    synchronized (orders) {
      orders.get(customerName).removeBrewing(drinkID); // Remove drink from BREWING AREA
      orders.get(customerName).setInTray(drinkID, drinkType); // Add drink to TRAY
    }
  }

  // All Orders Fulfiled (REMOVE ALL DRINKS IN TRAY)
  public void ordersFulfilled(String customerName) {
    synchronized (orders) {
      orders.get(customerName).removeAllInTray();
    }
  }

  // Retrieve Order Status
  public Map<String, Integer> getOrderStatus(String customerName) {
    Map<String, Integer> orderStatus = new HashMap<>();

    synchronized (orders) {
      if (!orders.isEmpty() && orders.containsKey(customerName)) {
        // Retrieve Tea orders
        orderStatus.put("tea_waiting", orders.get(customerName).getDrinkState(TEA, WAIT).size());
        orderStatus.put("tea_brewing", orders.get(customerName).getDrinkState(TEA, BREW).size());
        orderStatus.put("tea_tray", orders.get(customerName).getDrinkState(TEA, TRAY).size());

        // Retrieve Coffee orders
        orderStatus.put("coffee_waiting", orders.get(customerName).getDrinkState(COFFEE, WAIT).size());
        orderStatus.put("coffee_brewing", orders.get(customerName).getDrinkState(COFFEE, BREW).size());
        orderStatus.put("coffee_tray", orders.get(customerName).getDrinkState(COFFEE, TRAY).size());
      }
    }

    // Response to client
    return orderStatus;
  }

}