package Serveur;

public class EchoserverPoolChat {
    public static void main(String args[]) {
	int capacity = Integer.parseInt(args[0]);
	EchoServer server = new EchoServer(2013, capacity);
	server.run();
    }
}
