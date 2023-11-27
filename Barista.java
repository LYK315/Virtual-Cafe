import java.io.*;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import Helpers.CoffeeBar;
import Helpers.HandleCustomer;
import Helpers.ServerLog;

public class Barista {
  private final static int port = 8888;
  private final static String IDLE = "idle";
  private static TreeMap<String, String> clientCount = new TreeMap<>(); // Format <portNumber, State>
  private static TreeMap<String, ServerLog> currentLog = new TreeMap<>(); // Store Server Log

  // Main method to run the program, a.k.a 'server', a.k.a 'barista'
  public static void main(String[] args) {
    // Check if "serverLog.json" file already exist
    if (new File("serverLog.json").exists()) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();

      // Retrieve Previous Log in the file
      String previousLog;
      try {
        previousLog = readPreviousLog();
        Type mapType = currentLog.getClass();
        TreeMap<String, ServerLog> previousData = gson.fromJson(previousLog, mapType);
        currentLog = previousData;
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    OpenShop();
  }

  // Method to run the 'server'
  private static void OpenShop() {
    // Initialize server socket to listen for customer connections
    ServerSocket serverSocket = null;
    final CoffeeBar coffeeBar = new CoffeeBar(clientCount, currentLog);

    // Intercept when Server Shut Down, Wrtie Server Log to JSON file
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("\nWriting Server Log..");
      try {
        jsonWriter();
        System.out.println("\nDone.. Bye Bye");
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }));

    try {
      // Instantiate Server Socket
      serverSocket = new ServerSocket(port);
      System.out.println("Waiting for Customers...");

      while (true) {
        // Socket stays in blocked state untill a customer is connected
        Socket socket = serverSocket.accept();

        // Append new customer in Cafe
        clientCount.put(Integer.toString(socket.getPort()), IDLE);

        // Start independant thread for new joined customer
        new Thread(new HandleCustomer(socket, coffeeBar, clientCount)).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Read Previous Server Log Info
  private static String readPreviousLog() throws FileNotFoundException, IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader("serverLog.json"))) {
      StringBuilder content = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line);
      }
      return content.toString();
    }
  }

  // Write Server Log to JSON File
  public static void jsonWriter() throws FileNotFoundException, IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Write Log to JSON File
    try (FileWriter writer = new FileWriter("serverLog.json")) {
      gson.toJson(currentLog, writer);
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}