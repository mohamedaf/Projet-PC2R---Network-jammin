package serveur.Main;

import serveur.serveurs.EchoServer;

/**
 * 
 * @author Mohamed AMIN
 * 
 *         Le main du programme
 */
public class EchoserverPoolChat {
    public static void main(String args[]) {
	int capacity = 3;
	EchoServer server = new EchoServer(2013, capacity);
	server.run();
    }
}
