package Helpers;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

// Use Autocloseable for Resource Clean Up
public class CustomerModel implements AutoCloseable {
  final int port = 8888;

  private final Scanner scanner;
  private final PrintWriter writer;
  private final Socket socket;

  // Handle New Customer Joins
  public CustomerModel(String customerName) throws Exception {
    // Connects to the server and create an object socket for communication
    socket = new Socket("localhost", port);
    scanner = new Scanner(socket.getInputStream());
    writer = new PrintWriter(socket.getOutputStream(), true);

    // Send Customer Name to Server
    writer.println(customerName);

    // Read resposne from Server
    String response = scanner.nextLine();
    if (response.trim().compareToIgnoreCase("success") != 0) {
      throw new Exception(response); // Throw exception if connection failed
    }
  }

  // Handle Order Drinks
  public String orderDrinks(String order, String customerName) throws Exception {
    // Send command to Server
    writer.println("ORDER_DRINKS " + order);

    // Read response from Server
    String response = scanner.nextLine();

    // Start independent thread to poll server and check untill all orders are fulfilled
    Thread monitorOrder = new Thread(() -> {
      boolean orderFulfilled = false;

      while (orderFulfilled != true) {
        writer.println("order_fulfilled"); // Invoke "order fulfilled" method in HandleCustomer
        if (scanner.nextLine().equals("complete")) {
          // Notify customer and stop thread when all orders are fulfilled
          String orderSplit[] = order.split(" ");
          StringBuilder orderCombine = new StringBuilder();
          for (int i = 1; i < orderSplit.length; i++) {
            orderCombine.append(orderSplit[i]).append(" ");
          }
          String customerOrder = orderCombine.toString().trim();
          System.out.println("\n\n[ IMPORTANT ]\nOrder delivered to " + customerName + " (" + customerOrder + ")");

          // Stop thread when order is fulfilled
          orderFulfilled = true;
        }
        try {
          Thread.sleep(4000); // Check with server every 30.8 sec
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    monitorOrder.start(); // Start thread after customer places an order

    return response;
  }

  // Handle Order Status
  public String[] getOrderStatus(String customerName) {
    // Send command to Server
    writer.println("ORDER_STATUS");

    // Read response from Server (total number of orders)
    String response = scanner.nextLine();
    int numOfOrders = Integer.parseInt(response);

    // Read order status retrieved from Server
    String[] orderStatus = new String[numOfOrders];
    if (numOfOrders != 0) {
      for (int i = 0; i < numOfOrders; i++) {
        orderStatus[i] = "- " + scanner.nextLine();
      }
    }

    return orderStatus;
  }

  // Handle when customer wants to exit the cafe
  public void exitCafe() {
    writer.println("EXIT");
  }

  @Override
  public void close() throws Exception {
    // Close Scanner & PrintWriter to release the associated resources
    scanner.close();
    writer.close();
  }

}