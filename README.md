# Virtual Cafe
This project implements Client-Server Architecture in Java that simulates Cafe operation and is designed to handle multiple client concurrently .

# Java Version
JDK 21 is used in this project, so make sure your Java version is up to date if to prevent any error while compiling .

To Check your Java and Java Compiler Version :
```
javac --version

java --version 
```
Download latest JDK on [Oracle Official Website](https://www.oracle.com/java/technologies/downloads/) .

# Gson
Note that `Gson 2.10.1` is used in this project, so make sure you see a file named *`"gson-2.10.1.jar"`* in the root folder befor you run the program .

Download latest Gson on [Maven Repository](https://mvnrepository.com/artifact/com.google.code.gson/gson) .

# Customer Role
Below shows what a customer is able to do from ***Client Terminal*** in this program .
```
Order Drinks
>> order 1 tea
>> order 1 coffee
>> order 1 tea and 2 coffees

Check Order Status
>> order status

Exit Cafe
>> exit
```

# How to Run
Compile *`Barista.java`* and *`Customer.java`* on terminal .

```
Windows:
javac -cp .; gson-2.10.1.jar Barista.java
javac Customer.java

Linux: 
javac -cp .: gson-2.10.1.jar Barista.java
javac Customer.java
```
Everything will be explained based on **Windows** environment, remember to change **' ; '** to **' : '** in terminal if you are using **Linux**.


1 . Run the *`Barista.java (server)`* and *`Customer.java (client)`*.
```
java -cp .; gson-2.10.1.jar Barista
java Customer
```
\
2. You will see *`server`* is waiting for connection, and a list of *Customer Roles* displayed in *`client`*. Now type your name and request for **Order Status** in ***Client Terminal***.
```
>> order status

You should see a message "Oops, no new order found for Your Name"
```

\
3. Now repeat the first step and start *`Customer.java`* four (4) times in **Seperated Terminals**. Then, simply enter any name you like in each ***Client Terminal***.
```
e.g.
>> Liaw Yi Kai
```

\
4. Now lets **Order Drinks** in each ***Client Terminal***, you can type "*order status*" anytime in any ***Client Terminal*** to check their Order Status. Also, observe the ***Server Terminal*** and see the Cafe Status, you should be able to see every single change in the Cafe.
```
1st Client
>> order 10 tea and 10 coffee

2nd Client
>> order 2 teas and 2 coffees

3rd Client
>> order 3 tea

4th Client
>> order 4 coffee
```

\
5. Now, **before** 1st Client's **order is delivered**, let him **exit the Cafe**. Either type "*exit*" or press "*CTRL+C*". Feel free to play around with it and see the difference later.
```
>> exit

or 

CTRL + C
```

\
6. Now, observe the ***Server Terminal*** and you should be able to see message as below. The server checks if 1st Client's **orders** can be **transferred** to other 3 customers in the Cafe.
```
Checking if "1st Client" orders can be transferred..

Removed "1st Client" orders in waiting area Tea(*) Coffee(*)

Finishing "1st Client" orders in brewing area..
```

\
7. Then, you should also be able to see message (as below) in ***Server Terminal*** regarding **order transferred**.
```
(*)Tea (*)Coffee transferred from 1st Client to 2nd Client.
```

\
8. You should also see message (as below) in ***Server Terminal*** if 1st Client's order in Tray is not fully transferred to other customer. `[TRY IT LATER]`
```
(*)Tea removed from 1st Client Tray.

or 

(*)Coffee removed from 1st Client Tray.
```

\
9. Don't forget about the ***Client Terminal*** too. Other client's orders might be delivered in any seconds! And you should be able to see similar **notification** (as below) on their terminal screen.
```
*** NOTIFICATION ***
Dear "2nd Client" your order is delivered (2 teas and 2 coffees)
```

\
10. Now, let every client **exit** and **shut down** the ***Server Terminal*** by "*CTRL+C*". And you should be able to see a file *`"serverLog.json"`* appeared in your root folder. You should see info similar to below in the file.
```
{
  "2023-11-28 23:44:52:236290800": {
    "clientInCafe": 1,
    "clientWaitingOrder": 0,
    "ordersWaiting": "Tea(0) and Coffee(0)",
    "ordersBrewing": "Tea(0) and Coffee(0)",
    "ordersInTray": "Tea(0) and Coffee(0)",
    "serverMsg": "1st Client entered the Cafe."
  },
  "2023-11-28 23:44:57:845313100": {
    "clientInCafe": 2,
    "clientWaitingOrder": 0,
    "ordersWaiting": "Tea(0) and Coffee(0)",
    "ordersBrewing": "Tea(0) and Coffee(0)",
    "ordersInTray": "Tea(0) and Coffee(0)",
    "serverMsg": "2nd Client entered the Cafe."
  },
  .
  .
```

~ END ~ 

# Key Highlights (recap)
- Establishes Connection between **Client** and **Server**.
- Server handles **Multiple Clients Concurrently**.
- Server **Displays every single Change** in the Cafe.
- Client can **Place Orders**.
- Client can **Check Order Status** anytime.
- Client can **Exit Cafe** anytime.
- Client is **Automatically Notified** when Order is Delivered.
- When client left before order is delivered, server will check and try to **Transfer his Orders to Other Clients** in the Cafe.
- Server generates **Server Log File** that records every single change in the Cafe before shutting down.
- Intercepts and **handles SIGTERM** Signals.

# Conclusion
Feel free to play around with the program, obsereve the messages displayed on ***Client Terminal*** and ***Server Terminal*** on different scenarios. But do note that **error checking is not fully implemented** in this program, so please at least try to follow the Customer Role, especially when placing orders.

# Author
Liaw Yi Kai\
University Of Essex\
yl23705@essex.ac.uk

Updated November, 2023.



