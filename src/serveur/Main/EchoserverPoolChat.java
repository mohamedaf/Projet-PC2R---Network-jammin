package serveur.Main;

import serveur.Serveurs.EchoServer;

/**
 * 
 * @author Mohamed AMIN
 * 
 *         Le main du programme
 */
public class EchoserverPoolChat {
    public static void main(String args[]) {
	int capacity = 4;
	int port = 2015;
	EchoServer server = new EchoServer(port, capacity);
	server.run();
    }
}
