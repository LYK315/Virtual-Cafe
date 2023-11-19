package Helpers;

import java.util.Map;
import java.util.TreeMap;

public class Orders {
  private final String customerName;
  private int numOfTea = 0;
  private int numOfCoffee = 0;
  private int totalOrders = 0;
  private Map<Integer, String> teaOrders = new TreeMap<>();
  private Map<Integer, String> coffeeOrders = new TreeMap<>();

  public Orders(String customerName, int numOfTea, int numOfCoffee) {
    this.customerName = customerName;
    this.numOfTea = numOfTea;
    this.numOfCoffee = numOfCoffee;
    addTeaOrders();
    addCoffeeOrders();
  }

  public void addTeaOrders() {
    if (numOfTea > 0) {
      for (int i = totalOrders; i < (numOfTea + totalOrders); i++) {
        teaOrders.put(i, "waiting");
      }
    }
  }

  public void addCoffeeOrders() {
    if (numOfCoffee > 0) {
      for (int i = totalOrders; i < (numOfCoffee + totalOrders); i++) {
        coffeeOrders.put(i, "waiting");
      }
    }
  }

  public void setTeaState(int teaID, String state) {
    teaOrders.put(teaID, state);
  }

  public void setCoffeeState(int coffeeID, String state) {
    coffeeOrders.put(coffeeID, state);
  }

  public Map<Integer, String> getTeaState() {
    return teaOrders;
  }

  public Map<Integer, String> getCoffeeState() {
    return coffeeOrders;
  }

  public String getCustomerName() {
    return customerName;
  }
}
