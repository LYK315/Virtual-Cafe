package Helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Orders {
  private final String TEA = "tea";
  private final String COFFEE = "coffee";
  private final String WAIT = "waiting";
  private final String BREW = "brewing";
  private final String TRAY = "tray";

  private final String customerName;
  private int numOfTea = 0, numOfCoffee = 0, totalOrders = 0;

  private Map<Integer, String> drinkWaiting = new HashMap<>();
  private Map<Integer, String> drinkBrewing = new HashMap<>();
  private Map<Integer, String> drinkInTray = new HashMap<>();

  public Orders(String customerName, int numOfTea, int numOfCoffee) {
    this.customerName = customerName;
    this.numOfTea = numOfTea;
    this.numOfCoffee = numOfCoffee;
    addTeaOrders();
    addCoffeeOrders();
  }

  public void addTeaOrders() {
    if (numOfTea > 0) {
      for (int drinkID = totalOrders; drinkID < (numOfTea + totalOrders); drinkID++) {
        setWaiting(drinkID, TEA);
      }
      totalOrders += numOfTea;
    }
  }

  public void addCoffeeOrders() {
    if (numOfCoffee > 0) {
      for (int drinkID = totalOrders; drinkID < (numOfCoffee + totalOrders); drinkID++) {
        setWaiting(drinkID, COFFEE);
      }
      totalOrders += numOfCoffee;
    }
  }

  public String getCustomerName() {
    return customerName;
  }

  // Methods to handle Waiting, Brewing, Tray Area
  public void setWaiting(Integer drinkID, String drinkType) {
    drinkWaiting.put(drinkID, drinkType);
  }

  public void setBrewing(Integer drinkID, String drinkType) {
    drinkBrewing.put(drinkID, drinkType);
  }

  public void setInTray(Integer drinkID, String drinkType) {
    drinkInTray.put(drinkID, drinkType);
  }

  public void removeWaiting(Integer drinkID) {
    drinkWaiting.remove(drinkID);
  }

  public void removeBrewing(Integer drinkID) {
    drinkBrewing.remove(drinkID);
  }

  public void removeAllInTray() {
    drinkInTray.clear();
  }

  // Retrieve all drinks (tea & coffee) based on Drink State
  public Map<Integer, String> getDrinkState (String drinkState) {
    Map<Integer, String> drinkStateType = new HashMap<>();

    if (drinkState.equals(WAIT))
      drinkStateType = drinkWaiting;
    else if (drinkState.equals(BREW))
      drinkStateType = drinkBrewing;
    else if (drinkState.equals(TRAY))
      drinkStateType = drinkInTray;

    return drinkStateType;
  }

  // Retrieve specific drink type (tea / coffee) and Drink State
  public ArrayList<Integer> getDrinkState(String drinkType, String drinkState) {
    Map<Integer, String> drinkStateType = new HashMap<>();
    ArrayList<Integer> drinkdrinkState = new ArrayList<>();

    if (drinkState.equals(WAIT))
      drinkStateType = drinkWaiting;
    else if (drinkState.equals(BREW))
      drinkStateType = drinkBrewing;
    else if (drinkState.equals(TRAY))
      drinkStateType = drinkInTray;

    if (drinkStateType.size() > 0) {
      for (Integer key : drinkStateType.keySet()) {
        if (drinkStateType.get(key).equals(drinkType)) {
          drinkdrinkState.add(key);
        }
      }
    }
    return drinkdrinkState;
  }

  // Check if customer is Waiting Orders
  public boolean isWaitingOrder () {
    boolean isWaiting = false;

    int ordersFulfilling = drinkWaiting.size() + drinkBrewing.size() + drinkInTray.size();

    if (ordersFulfilling > 0) isWaiting = true;

    return isWaiting;
  }
}