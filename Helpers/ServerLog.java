package Helpers;

// import java.io.BufferedReader;
// import java.io.FileNotFoundException;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.IOException;
// import com.google.gson.Gson;
// import com.google.gson.GsonBuilder;

public class ServerLog {
  private int clientInCafe, clientWaitingOrder;
  private String ordersWaiting, ordersBrewing, ordersInTray;
  private String serverMsg;

  public ServerLog() {
    this.clientInCafe = 0;
    this.clientWaitingOrder = 0;
    this.ordersWaiting = null;
    this.ordersBrewing = null;
    this.ordersInTray = null;
    this.serverMsg = null;
  }

  public void setClientInCafe(int clientInCafe) {
    this.clientInCafe = clientInCafe;
  }

  public void setClientWaitingOrder(int clientWaitingOrder) {
    this.clientWaitingOrder = clientWaitingOrder;
  }

  public void setOrdersWaiting(String ordersWaiting) {
    this.ordersWaiting = ordersWaiting;
  }

  public void setOrdersBrewing(String ordersBrewing) {
    this.ordersBrewing = ordersBrewing;
  }

  public void setOrdersInTray(String ordersInTray) {
    this.ordersInTray = ordersInTray;
  }

  public void setServerMsg(String serverMsg) {
    this.serverMsg = serverMsg;
  }
}