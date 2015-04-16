package Serveur;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * 
 * @author Mohamed AMIN
 * 
 *         La classe gerant l'interaction du serveur avec un client pour la
 *         partie musicale
 */
public class EchoClientJam extends Thread {
    private DataInputStream inchan;
    private DataOutputStream outchan;
    private EchoServer server;
    private Socket socket;

    public EchoClientJam(EchoServer s) {
	server = s;
    }

    @Override
    public void run() {
	Socket s;

	while (true) {
	    synchronized (server) {
		if (server.stillWaiting() == 0)
		    try {
			server.wait();
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		s = server.removeFirstSocket();
	    }
	    try {
		inchan = new DataInputStream(s.getInputStream());
		outchan = new DataOutputStream(s.getOutputStream());
		socket = s;
		synchronized (server) {
		    server.newConnect(outchan);
		    /*
		     * car la capacite represente le client qui est deja
		     * connecte par la socket de controle
		     */
		    server.setNbConnectedClients(server.getNbConnectedClients() - 1);
		}
		while (true) {
		    String command = inchan.readLine();
		    if (command == null || command.equals("")) {
			System.out.println(" Fin de connexion.");
			break;
		    }
		    synchronized (server) {
			/* TO DO : traitement donnees audio reçus */
		    }
		}
		synchronized (server) {
		    server.clientJamLeft(outchan);
		}
		socket.close();
	    } catch (IOException e) {
		e.printStackTrace();
		System.exit(1);
	    }
	}
    }
}
