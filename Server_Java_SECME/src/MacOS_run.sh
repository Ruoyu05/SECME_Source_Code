
# src file's path
# e.g.
#
# cd /Users/username/Desktop/Server_Java_SECME/src
#
cd ~your path~/src

javac -cp .:../lib/Java-WebSocket-1.5.3.jar:../lib/slf4j-api-2.0.3.jar:../lib/slf4j-jdk14-2.0.3.jar:../lib/gson-2.9.1.jar:../lib/mysql-connector-java-8.0.29.jar:../lib/bcprov-jdk18on-171.jar Server.java

java -cp .:../lib/Java-WebSocket-1.5.3.jar:../lib/slf4j-api-2.0.3.jar:../lib/slf4j-jdk14-2.0.3.jar:../lib/gson-2.9.1.jar:../lib/mysql-connector-java-8.0.29.jar:../lib/bcprov-jdk18on-171.jar Server
