# SECME
 An Encrypted Chat Application (Include Server Code)

#Server:
  Language:Java
  #If you want to running it in your server, you should install Mysql and Java_JRE in you server.
  #before Running this code, you must change the database setting in `MySqlConnect.java`.
  e.g.
    private String host = "localhost";
    private int port = 3306;
    private String db_name = "SECME_APP";
    private String db_account = "root";
    private String db_password = "12345678";
  

  Java Command:
  #Notice
  `./~` is the `lib` file location in your server

  #On Windows OS
    $javac -cp .;./~/Java-WebSocket-1.5.3.jar;./~/slf4j-api-2.0.3.jar;./~/slf4j-jdk14-2.0.3.jar;./~/gson-2.9.1.jar;./~/mysql-connector-java-8.0.29.jar;./~/bcprov-jdk18on-171.jar Server.java
    $java -cp .;./~/Java-WebSocket-1.5.3.jar;./~/slf4j-api-2.0.3.jar;./~/slf4j-jdk14-2.0.3.jar;./~/gson-2.9.1.jar;./~/mysql-connector-java-8.0.29.jar;./~/bcprov-jdk18on-171.jar Server

  #On Linux or MacOS
    $javac -cp .:./~/Java-WebSocket-1.5.3.jar:./~/slf4j-api-2.0.3.jar:./~/slf4j-jdk14-2.0.3.jar:./~/gson-2.9.1.jar:./~/mysql-connector-java-8.0.29.jar:./~/bcprov-jdk18on-171.jar Server.java
    $java -cp .:./~/Java-WebSocket-1.5.3.jar:./~/slf4j-api-2.0.3.jar:./~/slf4j-jdk14-2.0.3.jar:./~/gson-2.9.1.jar:./~/mysql-connector-java-8.0.29.jar:./~/bcprov-jdk18on-171.jar Server


#APP
  Notice:
  #I have running a server for test.
  #If you want try the app without build a server,maybe you can use the setting written in down.
    IP: 47.74.1.184  Port: 10086

  IOS Code:
    Language:Swift
    #If you want to running it in your iPhone, you can use Xcode to make it.

  Android Code:
    Language:Java
    #If you want to running  it in your Android Phone, you can use Android Studio to make it.
