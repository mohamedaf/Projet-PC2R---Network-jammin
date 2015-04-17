package serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 
 * @author Mohamed AMIN
 * 
 *         La classe gerant l'interaction du serveur avec un client pour la
 *         partie protocle/chat
 */
public class EchoClient extends Thread {
    private BufferedReader inchan;
    private PrintWriter outchan;
    private EchoServer server;
    private Socket socket;
    private String userName;
    private boolean connecte;

    public EchoClient(EchoServer s) {
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
		inchan = new BufferedReader(new InputStreamReader(
			s.getInputStream()));
		outchan = new PrintWriter(s.getOutputStream());
		socket = s;
		synchronized (server) {
		    /* On verifie si la session est pleine */
		    if (server.getCapacity() == server.getNbConnectedClients()) {
			/**
			 * Si c'est le cas on previent le client, fermons la
			 * socket proprement puis passons a l'iteration suivant
			 * et donc a l'attente d'une nouvelle connexion
			 */
			Commandes.full_session(outchan);
			server.setNbWaitingSocks(server.getNbWaitingSocks() - 1);
			socket.close();
			continue;
		    }

		    server.newConnect(outchan);
		    connecte = true;
		}
		/* Recuperer le nom du client */
		userName = inchan.readLine();
		synchronized (server) {
		    server.writeAllButMe("*** New user on chat ***\n"
			    + "*** User name : " + userName + " ***\n", outchan);
		}
		while (true && connecte) {
		    String command = inchan.readLine();
		    if (command == null || command.equals("")
			    || command.equals("EXIT/" + userName + "/")) {
			System.out.println(" Fin de connexion.");
			break;
		    }
		    synchronized (server) {
			if (!server.AnswerClient(command, inchan, outchan,
				userName, this))
			    server.writeAllButMe(command + "\n", outchan,
				    userName);
		    }
		}
		synchronized (server) {
		    server.clientLeft(outchan, userName);
		}
		socket.close();
	    } catch (IOException e) {
		e.printStackTrace();
		System.exit(1);
	    }
	}
    }

    public void closeSocket() {
	connecte = false;
    }

    public String getUserName() {
	return userName;
    }

    public void setUserName(String userName) {
	this.userName = userName;
    }

}
