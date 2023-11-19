package Helpers;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

// Use Autocloseable for Resource Clean Up
public class CustomerModel implements AutoCloseable {

  final int port = 8888;

  private final Scanner reader;
  private final PrintWriter writer;
  private final Socket socket;

  // Handle new customer join
  public CustomerModel(String customerName) throws Exception {
    // Connects to the server and create an object socket for communication
    socket = new Socket("localhost", port);
    reader = new Scanner(socket.getInputStream());
    writer = new PrintWriter(socket.getOutputStream(), true);

    // Send Customer Name to Server
    writer.println(customerName);

    // Parse the resposne from Server
    String response = reader.nextLine();
    if (response.trim().compareToIgnoreCase("success") != 0) {
      throw new Exception(response);
    }
  }

  // Handle Order Drinks
  public String orderDrinks(String order) throws Exception {

    // Send command to Server
    writer.println("ORDER_DRINKS " + order);

    // Read response from Server
    String response = reader.nextLine();

    return response;
  }

  // Handle Order Status
  public String[] getOrderStatus() {
    // Send command to Server
    writer.println("ORDER_STATUS");

    // Read the response from Server (number of orders)
    String response = reader.nextLine();
    int numOfOrders = Integer.parseInt(response);

    // Read the order status of each drink
    String[] orderStatus = new String[numOfOrders];
    for (int i = 0; i < numOfOrders; i++) {
      orderStatus[i] = reader.nextLine();
    }

    return orderStatus;
  }

  // Handle when customer wants to exit the cafe
  public void exitCafe () {
    writer.println("EXIT");
  }

  @Override
  public void close() throws Exception {
    // Close Scanner & PrintWriter to release the associated resources
    reader.close();
    writer.close();
  }
}