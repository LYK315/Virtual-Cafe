import java.util.Scanner;

import Helpers.CustomerModel;

public class Customer {
  private static String choice = "";

  public static void main(String[] args) { 
    
    System.out.println("Hello, how should I address you: ");

    // Try to get user input
    try {
      Scanner input = new Scanner(System.in);
      String customerName = input.nextLine();

      // Try to set up connection with Server
      try (CustomerModel newCustomer = new CustomerModel(customerName)) {
        System.out.println("\nWelcome to the Cafe, " + customerName + ".");

        // Receive input from user as Order / Request to Barista
        while (!choice.equals("exit")) {
          // Prompt user on how to interact with the server
          System.out.println("\nFollow Command Formats below to Interact with Barista:\n" +
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
          System.out.println("Enter a Command:");

          // Try to get user Input
          choice = input.nextLine().toLowerCase();
          String[] substring = choice.split(" ");

          // Handle user input
          switch (substring[0]) {
            case "order":
              // Handle order status
              if (substring[1].equals("status")) {
                // Store retrieve all order status
                String[] orderStatus = newCustomer.getOrderStatus();

                // Print all order status
                System.out.println("\nOrder Status for: " + customerName);
                for (String order : orderStatus) {
                  System.out.println("- " + order);
                }
              }
              // Handle order drinks
              else {
                String order = newCustomer.orderDrinks(choice);
                System.out.println("Order received from " + customerName + " (" + order + ")");
              }
              break;

            // User exits the cafe
            case "exit":
              newCustomer.exitCafe();
              System.out.println("\nBye Bye, " + customerName + ".");
              break;

            // User typed wrong command
            default:
              System.out.println("Unknown Command: " + choice);
              break;
          }
        }
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
