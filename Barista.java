import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import Helpers.CoffeeBar;
import Helpers.HandleCustomer;

public class Barista {
  private final static int port = 8888;
  private final static CoffeeBar coffeeBar = new CoffeeBar();

  // Main method to run the program, a.k.a 'server', a.k.a 'barista'
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
      // If multiple client connects to server, 'Server Process' will have multiple threads to handle different clients. 
      while (true){
        // Socket stays in blocked state untill a customer is connected
        Socket socket = serverSocket.accept();

        // Start independant thread for new joined customer
        new Thread(new HandleCustomer(socket, coffeeBar)).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}