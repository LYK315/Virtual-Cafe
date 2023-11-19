package Helpers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class CoffeeBar {
  // maps for waitingArea, brewingArea, trayArea
  private final Map<String, Orders> orders = new TreeMap<>();

  private Map<String, String> teaWaiting = new TreeMap<>();
  private Map<String, String> coffeeWaiting = new TreeMap<>();

  private Map<String, String> teaBrewing = new TreeMap<>();
  private Map<String, String> coffeeBrewing = new TreeMap<>();

  private Map<String, String> teaTray = new TreeMap<>();
  private Map<String, String> coffeeTray = new TreeMap<>();

  // Add new orders to Coffee Bar
  public void placeOrder(String customerName, int numOfTea, int numOfCoffee) {
    // Check if customer has previous order, add new order instance if no, else
    // append order to previous order
    Orders order = new Orders(customerName, numOfTea, numOfCoffee);
    // ADD ANOTHER CONSTRUCTOR FOR USER 2ND ORDER, IMPLEMENT ORDER ID
    orders.put(customerName, order);

    // Add new tea orders to WAITING AREA
    for (Integer drinkID : orders.get(customerName).getTeaState().keySet()) {
      if (orders.get(customerName).getTeaState().get(drinkID).equals("waiting")) {
        teaWaiting.put(customerName + " " + drinkID, "tea");
      }
    }

    // Add new coffee orders to WAITING AREA
    for (Integer drinkID : orders.get(customerName).getCoffeeState().keySet()) {
      if (orders.get(customerName).getCoffeeState().get(drinkID).equals("waiting")) {
        coffeeWaiting.put(customerName + " " + drinkID, "coffee");
      }
    }
 
  }

  // Start brewing drinks
  public void startBrewing(String customerName) {
    // Remove order from WAITING AREA, append to BREWING AREA

    // To store currently brewing drinks (started threads)
    Map<String, Thread> brewTeaThreads = new TreeMap<>();
    Map<String, Thread> brewCoffeeThreads = new TreeMap<>();

    // Brew Tea
    if (teaWaiting.size() > 0) {

      // Loop untill all tea order in TEA WAITING AREA is fulfilled
      while (!teaWaiting.isEmpty()) {

        // Loop through the "teaWaiting" (TEA WAITING AREA) to start fulfilling tea
        // orders
        Iterator<String> iterator = teaWaiting.keySet().iterator();
        while (iterator.hasNext()) {
          String key = iterator.next();

          // Start new threads to brew tea, only if currently brewing tea < 2
          if (teaBrewing.size() < 2) {
            String[] teaID = key.split(" "); // Format key stored in "teaWaiting" is "customerName drinkID"

            // Thread to Brew Tea
            Thread brewTea = new Thread(() -> {
              try {
                System.out.println("Brewing Tea"); //TEST
                Thread.sleep(10000); // 30 seconds to fulfill a tea order
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            });

            orders.get(customerName).setTeaState(Integer.parseInt(teaID[1]), "brewing"); // Update tea order states
            iterator.remove(); // Remove current order from "teaWaiting" map (TEA WAITING AREA)
            teaBrewing.put(key, "tea"); // Append current order to "teaBrewing" map (TEA BREWING AREA)
            brewTeaThreads.put(key, brewTea); // Add the running thread to a map (to close it when it is done)
            brewTea.start(); // Start brew tea thread
          } else if (teaBrewing.size() == 2) { // If 2 tea brewing limit reached, get out from the loop first
            break;
          }
        }

        // Use ieterator to loop through the "brewTeaThreads" (order currently
        // fulfilling)
        Iterator<String> brewTeaThread = brewTeaThreads.keySet().iterator();
        while (brewTeaThread.hasNext()) {
          String key = brewTeaThread.next();
          try {
            brewTeaThreads.get(key).join(); // Wait untill the thread is done, close it
            System.out.println("Tea is Done"); //TEST
            brewTeaThread.remove(); // Remove the thread from "brewTeaThreads" map
            finishBrewing(customerName, key, "tea"); // Handle order fulfilled
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }

    // Brew Coffee
    if (coffeeWaiting.size() > 0) {
      // Loop untill all coffee order in COFFEE WAITING AREA is fulfilled
      while (!coffeeWaiting.isEmpty()) {
        // Loop through the "coffeeWaiting" (COFFEE WAITING AREA) to start fulfilling
        // coffee orders
        Iterator<String> iterator = coffeeWaiting.keySet().iterator();
        while (iterator.hasNext()) {
          String key = iterator.next();

          // Start new threads to brew coffee, only if currently brewing coffee < 2
          if (coffeeBrewing.size() < 2) {
            String[] coffeeID = key.split(" "); // Format key stored in "coffeeWaiting" is "customerName drinkID"

            // Thread to Brew Coffee
            Thread brewCoffee = new Thread(() -> {
              try {
                Thread.sleep(10000); // 45 seconds to fulfill a coffee order
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            });

            orders.get(customerName).setCoffeeState(Integer.parseInt(coffeeID[1]), "brewing"); // Update order state
            iterator.remove(); // Remove current order from "coffeeWaiting" map (COFFEE WAITING AREA)
            coffeeBrewing.put(key, "coffee"); // Append current order to "coffeeBrewing" map (COFFEE BREWING AREA)
            brewCoffeeThreads.put(key, brewCoffee); // Add the started thread to a map (to close it when it is done)
            brewCoffee.start(); // Start brew coffee thread
          } else if (coffeeBrewing.size() == 2) { // If 2 coffee brewing limit reached, get out from the loop first
            break;
          }
        }

        // Use ieterator to loop through the "brewCoffeeThreads" (order currently
        // fulfilling)
        Iterator<String> brewCoffeeThread = brewCoffeeThreads.keySet().iterator();
        while (brewCoffeeThread.hasNext()) {
          String key = brewCoffeeThread.next();
          try {
            brewCoffeeThreads.get(key).join(); // Wait untill the thread is done, close it
            brewCoffeeThread.remove(); // Remove the thread from "brewCoffeeThreads" map
            finishBrewing(customerName, key, "coffee"); // Handle order fulfilled
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }
  
  }

  // Finish brewing drinks
  public void finishBrewing(String customerName, String key, String drinkType) {
    // Remove order from BREWING AREA, append to TRAY AREA

    // Tea Finish Brewing
    if (drinkType.equals("tea")) {
      String[] teaID = key.split(" "); // Format key stored in "teaWaiting" is "customerName drinkID"
      orders.get(customerName).setTeaState(Integer.parseInt(teaID[1]), "tray"); // Update tea order states
      teaBrewing.remove(key); // Remove order from "teaBrewing" map (TEA BREWING AREA)
      teaTray.put(key, "tea"); // Append order to (TEA TRAY AREA)
    }

    // Coffee Finish Brewing
    if (drinkType.equals("coffee")) {
      String[] coffeeID = key.split(" "); // Format key stored in "coffeeWaiting" is "customerName drinkID"
      orders.get(customerName).setCoffeeState(Integer.parseInt(coffeeID[1]), "tray"); // Update coffee order states
      coffeeBrewing.remove(key); // Remove order from "coffeeBrewing" map (COFFEE BREWING AREA)
      coffeeTray.put(key, "coffee"); // Append order to (COFFEE TRAY AREA)
    }
  }

  // Retrieve customer order status
  public Map<String, Integer> getOrderStatus(String customerName) {
    System.out.println("Retrieving Order Status"); //TEST

    // return list of orders current state, use customerName to retrieve data
    Map<String, Integer> orderStatus = new HashMap<>();
    int teaInWaiting = 0, teaInBrewing = 0, teaInTray = 0;
    int coffeeInWaiting = 0, coffeeInBrewing = 0, coffeeInTray = 0;

    // Retrieve tea orders
    for (Integer drinkID : orders.get(customerName).getTeaState().keySet()) {
      String teaState = orders.get(customerName).getTeaState().get(drinkID);
      if (teaState.equals("waiting")) {
        teaInWaiting++;
      } else if (teaState.equals("brewing")) {
        teaInBrewing++;
      } else if (teaState.equals("tray")) {
        teaInTray++;
      }
    }

    // Retrieve coffee orders
    for (Integer drinkID : orders.get(customerName).getCoffeeState().keySet()) {
      String coffeeState = orders.get(customerName).getCoffeeState().get(drinkID);
      if (coffeeState.equals("waiting")) {
        coffeeInWaiting++;
      } else if (coffeeState.equals("brewing")) {
        coffeeInBrewing++;
      } else if (coffeeState.equals("tray")) {
        coffeeInTray++;
      }
    }

    // Append to order status
    orderStatus.put("tea_waiting", teaInWaiting);
    orderStatus.put("coffee_waiting", coffeeInWaiting);
    orderStatus.put("tea_brewing", teaInBrewing);
    orderStatus.put("coffee_brewing", coffeeInBrewing);
    orderStatus.put("tea_tray", teaInTray);
    orderStatus.put("coffee_tray", coffeeInTray);

    // Response to client
    return orderStatus;
  }

}