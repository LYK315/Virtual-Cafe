import java.util.Scanner;
import Helpers.CustomerModel;

public class Customer {
  private static String choice = "";
  private static boolean properExit = false;

  public static void main(String[] args) {

    System.out.print("Hello, how should I address you: ");

    try {
      // Get User Input
      Scanner input = new Scanner(System.in);
      String customerName = input.nextLine();

      // Intercept and Handle SIGINT (CTRL + C) Signal
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (!properExit) {
          System.out.println("\nReceived termination signal (SIGTERM).\n");
          System.out.println("Leave from the front door next time, " + customerName + ".\n");
        }
      }));

      // Set Up Connection with Server
      try (CustomerModel customer = new CustomerModel(customerName)) {
        System.out.println("\nWelcome to the Cafe, " + customerName + ".");

        // Receive input from user as Order, send Request to Server
        while (!choice.equals("exit")) {
          // Prompt user on how to interact with the server
          System.out.println("\nCommand Formats to Interact with Barista:\n" +
              "****************************************" +
              "\n[ To Order ]\n" +
              "- order 1 tea / order 1 coffee\n" +
              "- order 2 tea and 2 coffee\n" +
              "- order 2 teas and 3 coffees\n" +
              "- order 2 tea and 3 coffees\n" +
              "\n[ View Order Status ]\n" +
              "- order status\n" +
              "\n[ Exit Cafe ]\n" +
              "- exit\n" +
              "****************************************");
          System.out.print("Enter a Command: ");

          // Get User Input
          choice = input.nextLine().toLowerCase();
          String[] substring = choice.split(" ");

          // Handle User Input
          switch (substring[0]) {
            case "order":
              // Handle Order Status
              if (substring[1].equals("status")) {
                // Retrieve all order status
                String[] orderStatus = customer.getOrderStatus();

                // Print All Order Status
                System.out.println("\n[ RESULT SHOWN BELOW ]");
                if (orderStatus.length == 0) {
                  System.out.println("Oops, no new order found for " + customerName);
                } else {
                  System.out.println("Order Status for " + customerName + ": ");
                  for (String order : orderStatus) {
                    System.out.println(order);
                  }
                }
                System.out.println("\nPress enter to continue..\n\n");
                input.nextLine();
              }
              // Handle Order Drinks
              else {
                String order = customer.orderDrinks(choice, customerName);
                String orderSplit[] = order.split(",");
                System.out.println("\n[ RESULT SHOWN BELOW ]");
                if (orderSplit[1].equals("true")) {
                  System.out.println("Add-On Order received from " + customerName + " (" + orderSplit[0] + ")");
                } else {
                  System.out.println("Order received from " + customerName + " (" + orderSplit[0] + ")");
                }
                System.out.println("\nPress enter to continue..\n\n");
                input.nextLine();
              }
              break;

            // User Exits The Cafe
            case "exit":
              customer.stopPolling(); // Interrupt and stop polling immediately after customer exit
              properExit = true;
              customer.exitCafe();
              input.close();
              System.out.println("\nBye Bye, " + customerName + ".\n");
              break;

            // User Typed a Wrong Command
            default:
              System.out.println("\n[ RESULT SHOWN BELOW ]");
              System.out.println("Unknown Command: " + choice);
              System.out.println("\nPress enter to continue..\n\n");
              input.nextLine();
              break;
          }
        }
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
