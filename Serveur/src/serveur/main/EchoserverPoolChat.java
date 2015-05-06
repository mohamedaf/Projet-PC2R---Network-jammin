package serveur.main;

import serveur.serveurs.EchoServer;

/**
 * 
 * @author Mohamed-Amin AFFES - Hassan KOBROSLI
 * 
 *         Le main du programme
 */
public class EchoserverPoolChat {
	public static void main(String args[]) {
		int capacity = 4;
		int port = 2015;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-max")) {
				i++;
				capacity = Integer.parseInt(args[i]);
			}
			if (args[i].equals("-max")) {
				i++;
				port = Integer.parseInt(args[i]);
			}
		}

		EchoServer server = new EchoServer(port, capacity);
		server.run();
	}
}
