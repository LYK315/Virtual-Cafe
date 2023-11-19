import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import Helpers.CoffeeBar;
import Helpers.HandleCustomer;

public class Barista {
  private final static int port = 8888;
  private final static CoffeeBar coffeeBar = new CoffeeBar();

  // Main method to run the program, aka 'server', aka 'barista'
  public static void main(String[] args) {
    OpenShop();
  }

  // Method to run the 'server'
  private static void OpenShop() {
    // Initialize server socket to listen for customer connections
    ServerSocket serverSocket = null;

    try {
      // Instantiate server socket that will constantly listen to connections
      serverSocket = new ServerSocket(port);
      System.out.println("Waiting for Customers...");

      // Server & Client are both seperated processes.
      // If multiple client connected to server, means 'Server Process' will have multiple threads to handle different clients. 
      while (true){
        // Socket stays in block state untill a customer is connected
        Socket socket = serverSocket.accept();

        // Start thread for current customer
        new Thread(new HandleCustomer(socket, coffeeBar)).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}