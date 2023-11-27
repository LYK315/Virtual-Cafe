package Helpers;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

// Use Autocloseable for Resource Clean Up
public class CustomerModel implements AutoCloseable {
  private final int port = 8888;
  private final Scanner scanner;
  private final PrintWriter writer;
  private final Socket socket;
  private Thread pollingThread;

  // Constructor handles New Customer Join
  public CustomerModel(String customerName) throws Exception {
    // Connects to the server and create an set up communicaton stream
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
    String orderReply, isAddOn;
    orderReply = scanner.nextLine();
    isAddOn = scanner.nextLine();

    // Independent thread to poll server, untill all orders are delivered
    Thread monitorOrder = new Thread(() -> {
      boolean orderFulfilled = false;

      while (!orderFulfilled) {
        writer.println("IS_ORDER_FULFILLED"); // Send Command to Server

        // Notify customer and stop polling when all orders are fulfilled
        try {
          // Read response from "IS_ORDER_FULFILLED"
          String response = scanner.nextLine();

          if (response.equals("complete")) {
            String orderSplit[] = scanner.nextLine().split(" ");
            int teaCount = Integer.parseInt(orderSplit[0]); // Number of tea
            int coffeeCount = Integer.parseInt(orderSplit[1]); // Numer of coffee

            // Display msg accordingly
            String customerOrder = "", teaPlural = "", coffeePlural = "";
            if (teaCount > 0) {
              teaPlural = teaCount > 1 ? "teas" : "tea"; // Set plural of tea
              if (coffeeCount > 0) {
                coffeePlural = coffeeCount > 1 ? "coffees" : "coffee"; // Set plural of coffee
                customerOrder = teaCount + " " + teaPlural + " and " + coffeeCount + " " + coffeePlural;
              } else {
                customerOrder = teaCount + " " + teaPlural;
              }
            } else {
              coffeePlural = coffeeCount > 1 ? "coffees" : "coffee"; // Set plural of coffee
              customerOrder = coffeeCount + " " + coffeePlural;
            }
            
            // Print notification on Client Terminal
            System.out.println("\n\n*** NOTIFICATION ***\nDear " +
                customerName + ", your order is delivered (" + customerOrder + ")");

            pollingThread = null; // Set polling thread back to null
            orderFulfilled = true; // Stop thread when order is fulfilled
          }
        } catch (Exception e) {
          break;
        }
        try {
          Thread.sleep(15000); // Poll server every 15 sec
        } catch (InterruptedException e) {
        }
      }
    });

    // Dont start thread if customer made Add-on order
    if (isAddOn.equals("false")) {
      // Start thread after customer places an order
      monitorOrder.start();
      pollingThread = monitorOrder; // Assign thread to pollingThread, use to interrupt thread when client left
    }

    return (orderReply + "," + isAddOn); // Let client know if order is add on
  }

  // Method to Interrupt Polling Thread (stop immediately from Thread Sleep)
  public void stopPolling () {
    if (pollingThread != null){
      pollingThread.interrupt();
    }
  }

  // Handle Order Status
  public String[] getOrderStatus() {
    // Send command to Server
    writer.println("ORDER_STATUS");

    // Read and parse response from Server
    String response = scanner.nextLine();
    int numOfArea = Integer.parseInt(response);

    // Read order status retrieved from Server
    String[] orderStatus = new String[numOfArea];
    if (numOfArea != 0) {
      for (int i = 0; i < numOfArea; i++) {
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