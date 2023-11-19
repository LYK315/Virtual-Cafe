package Helpers;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;

public class HandleCustomer implements Runnable {
  private final Socket socket;
  private CoffeeBar coffeeBar;
  private boolean lostConnection = true;

  public HandleCustomer(Socket socket, CoffeeBar coffeeBar) {
    this.socket = socket;
    this.coffeeBar = coffeeBar;
  }

  @Override
  public void run() {
    String customerName = null;

    // Try to set up connection with client using socket
    try {
      Scanner scanner = new Scanner(socket.getInputStream());
      PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

      // Try to read customr name (sent from client server)
      try {
        // Read and output customer name
        customerName = scanner.nextLine();
        System.out.println(customerName + " entered the Cafe.");

        // Response with success state to Client
        writer.println("SUCCESS");

        // Handle Orders / Requests from Customer
        while (true) {
          // Read requests from client
          String request = scanner.nextLine().toLowerCase();
          String[] substring = request.split(" ");

          switch (substring[0]) {
            case "order_status":
              // Retrive status of each orders
              Map<String, Integer> orderStatus = coffeeBar.getOrderStatus(customerName);

              int teaWaiting = orderStatus.get("tea_waiting").intValue();
              int coffeeWaiting = orderStatus.get("coffee_waiting").intValue();

              int teaBrewing = orderStatus.get("tea_brewing").intValue();
              int coffeeBrewing = orderStatus.get("coffee_brewing").intValue();

              int teaTray = orderStatus.get("tea_tray").intValue();
              int coffeeTray = orderStatus.get("coffee_tray").intValue();

              // Response with TOTAL DRINKS of customer
              int numOfArea = 0;
              if (teaWaiting + coffeeWaiting > 0) { numOfArea+=1; };
              if (teaBrewing + coffeeBrewing > 0) { numOfArea+=1; };
              if (teaTray + coffeeTray > 0) { numOfArea+=1; };
              writer.println(numOfArea);

              String teaPlural, coffeePlural;

              // Response with drinks in WAITING AREA
              if (teaWaiting > 0) {
                teaPlural = teaWaiting > 1 ? "teas" : "tea";
                if (coffeeWaiting > 0) {
                  coffeePlural = coffeeWaiting > 1 ? "coffees" : "coffee";
                  writer.println(
                      teaWaiting + " " + teaPlural + " and " + coffeeWaiting + " " + coffeePlural + " in waiting area");
                } else {
                  writer.println(teaWaiting + " " + teaPlural + " in waiting area");
                }
              } else if (coffeeWaiting > 0) {
                coffeePlural = coffeeWaiting > 1 ? "coffees" : "coffee";
                writer.println(coffeeWaiting + " " + coffeePlural + " in waiting area");
              }

              // Response with drinks in BREWING AREA
              if (teaBrewing > 0) {
                teaPlural = teaBrewing > 1 ? "teas" : "tea";
                if (coffeeBrewing > 0) {
                  coffeePlural = coffeeBrewing > 1 ? "coffees" : "coffee";
                  writer.println(
                      teaBrewing + " " + teaPlural + " and " + coffeeBrewing + " " + coffeePlural
                          + " is currently being prepared");
                } else {
                  writer.println(teaBrewing + " " + teaPlural + " is currently being prepared");
                }
              } else if (coffeeBrewing > 0) {
                coffeePlural = coffeeBrewing > 1 ? "coffees" : "coffee";
                writer.println(coffeeBrewing + " " + coffeePlural + " is currently being prepared");
              }

              // Response with drinks in TRAY AREA
              if (teaTray > 0) {
                teaPlural = teaTray > 1 ? "teas" : "tea";
                if (coffeeTray > 0) {
                  coffeePlural = coffeeTray > 1 ? "coffees" : "coffee";
                  writer.println(
                      teaTray + " " + teaPlural + " and " + coffeeTray + " " + coffeePlural
                          + " is currently in the tray");
                } else {
                  writer.println(teaTray + " " + teaPlural + " is currently in the tray");
                }
              } else if (coffeeTray > 0) {
                coffeePlural = coffeeTray > 1 ? "coffees" : "coffee";
                writer.println(coffeeTray + " " + coffeePlural + " is currently in the tray");
              }

              break;

            case "order_drinks":
              // order_drinks order 1 tea - 4
              // order_drinks order 1 tea and 1 coffee - 7
              int numOfTea = 0, numOfCoffee = 0;

              // Customer order either tea or coffee
              if (substring.length == 4) {
                if (substring[3].equals("tea") || substring[3].equals("teas")) {
                  numOfTea = Integer.parseInt(substring[2]);
                  writer.println(numOfTea + " " + substring[3]);
                } else {
                  numOfCoffee = Integer.parseInt(substring[2]);
                  writer.println(numOfCoffee + " " + substring[3]);
                }
              }
              // Customer order both tea and coffee
              else {
                if (substring[3].equals("tea") || substring[3].equals("teas")) {
                  numOfTea = Integer.parseInt(substring[2]);
                  numOfCoffee = Integer.parseInt(substring[5]);
                  writer.println(numOfTea + " " + substring[3] + " and " + numOfCoffee + " " + substring[6]);
                } else {
                  numOfCoffee = Integer.parseInt(substring[2]);
                  numOfTea = Integer.parseInt(substring[5]);
                  writer.println(numOfCoffee + " " + substring[3] + " and " + numOfTea + " " + substring[6]);
                }
              }

              // Place order to Coffee Bar
              coffeeBar.placeOrder(customerName, numOfTea, numOfCoffee);
              coffeeBar.startBrewing(customerName);
              break;

            case "exit":
              socket.close();
              lostConnection = false;
              break;

            default:
              throw new Exception("Unknown command: " + substring[0]);
          }
        }

      } catch (Exception e) {
        writer.println("ERROR " + e.getMessage());
        socket.close();
      }
    } catch (Exception e) {
    } finally {
      if (lostConnection) // If client left by CTRL+C or Lost Connection
        System.out.println("[Lost Connection] " + customerName + " disappeared.");
      else // If client left by using exit command
        System.out.println(customerName + " left the Cafe.");
    }
  }
}
