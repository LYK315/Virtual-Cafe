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

  public Integer getTotalOrders() {
    return totalOrders;
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

  // Retrieve all drinks (tea & coffee) based on state
  public Map<Integer, String> getDrinkState(String state) {
    Map<Integer, String> stateType = new HashMap<>();

    if (state.equals(WAIT))
      stateType = drinkWaiting;
    else if (state.equals(BREW))
      stateType = drinkBrewing;
    else if (state.equals(TRAY))
      stateType = drinkInTray;

    return stateType;
  }

  // Retrieve specific drink type (tea / coffee) and state
  public ArrayList<Integer> getDrinkState(String drinkType, String state) {
    Map<Integer, String> stateType = new HashMap<>();
    ArrayList<Integer> drinkState = new ArrayList<>();

    if (state.equals(WAIT))
      stateType = drinkWaiting;
    else if (state.equals(BREW))
      stateType = drinkBrewing;
    else if (state.equals(TRAY))
      stateType = drinkInTray;

    if (stateType.size() > 0) {
      for (Integer key : stateType.keySet()) {
        if (stateType.get(key).equals(drinkType)) {
          drinkState.add(key);
        }
      }
    }
    return drinkState;
  }

}
