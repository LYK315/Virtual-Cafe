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
  private String customerName = null;
  private int numOfTea = 0, numOfCoffee = 0, totalOrders = 0;
  private Map<Integer, String> drinkWaiting = new HashMap<>(); // Format <drinkID, drinkType>
  private Map<Integer, String> drinkBrewing = new HashMap<>(); // Format <drinkID, drinkType>
  private Map<Integer, String> drinkInTray = new HashMap<>(); // Format <drinkID, drinkType>

  public Orders(int numOfTea, int numOfCoffee, String customerName) {
    this.numOfTea = numOfTea;
    this.numOfCoffee = numOfCoffee;
    this.customerName = customerName;
    addTeaOrders();
    addCoffeeOrders();
  }

  public void addOnOrders(int numOfTea, int numOfCoffee) {
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

  public String getCustomerName () {
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

  public void removeInTray(Integer drinkID) {
    drinkInTray.remove(drinkID);
  }

  public void removeAllInTray() {
    drinkInTray.clear();
  }

  // Retrieve all drinks (tea & coffee) based on Drink State
  public Map<Integer, String> getDrinkState(String drinkState) {
    Map<Integer, String> drinkStateType = new HashMap<>();

    if (drinkState.equals(WAIT))
      drinkStateType = drinkWaiting;
    else if (drinkState.equals(BREW))
      drinkStateType = drinkBrewing;
    else if (drinkState.equals(TRAY))
      drinkStateType = drinkInTray;

    return drinkStateType;
  }

  // Retrieve specific drink type (tea / coffee) based on Drink State
  public ArrayList<Integer> getDrinkState(String drinkType, String drinkState) {
    Map<Integer, String> drinkStateType = new HashMap<>();
    ArrayList<Integer> orderInState = new ArrayList<>();

    if (drinkState.equals(WAIT))
      drinkStateType = drinkWaiting;
    else if (drinkState.equals(BREW))
      drinkStateType = drinkBrewing;
    else if (drinkState.equals(TRAY))
      drinkStateType = drinkInTray;

    if (drinkStateType.size() > 0) {
      for (Integer key : drinkStateType.keySet()) {
        if (drinkStateType.get(key).equals(drinkType)) {
          orderInState.add(key);
        }
      }
    }
    return orderInState;
  }

}