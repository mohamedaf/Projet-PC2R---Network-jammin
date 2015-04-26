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

    /**
     * Constructeur
     * 
     * @param server
     *            : instance du serveur
     */
    public EchoClient(EchoServer s) {
	server = s;
    }

    @Override
    public void run() {
	Socket s = null;
	boolean me;

	while (true) {
	    me = false;

	    synchronized (server) {
		/**
		 * Attend qu'une socket soit ajouté, donc qu'un client se
		 * connecte
		 */
		if (server.stillWaiting() == 0) {
		    try {
			server.wait();
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		}

		if (!server.getIsJamConnexion()) {
		    /**
		     * Je suis concerne
		     */
		    me = true;
		} else {
		    /**
		     * Si je ne suis pas concerne je notifie l'autre client qui
		     * peut l'etre
		     */
		    server.notify();
		}

		if (me) {
		    System.out.println("EchoClient choisi");
		    System.out.flush();
		    /**
		     * Je recupere la socket
		     */
		    s = server.removeFirstSocket();
		}
	    }
	    if (me) {
		try {
		    inchan = new BufferedReader(new InputStreamReader(
			    s.getInputStream()));
		    outchan = new PrintWriter(s.getOutputStream());
		    socket = s;

		    synchronized (server) {
			/* On verifie si la session est pleine */
			if (server.getCapacity() == server
				.getNbConnectedClients()) {
			    /**
			     * Si c'est le cas on previent le client, fermons la
			     * socket proprement puis passons a l'iteration
			     * suivant et donc a l'attente d'une nouvelle
			     * connexion
			     */
			    Commandes.full_session(outchan);
			    server.setNbWaitingSocks(server.stillWaiting() - 1);
			    socket.close();
			    continue;
			}

			/**
			 * On previens le serveur de la nouvelle connexion
			 */
			server.newConnect(outchan);
			connecte = true;
		    }

		    while (true && connecte) {
			String command = inchan.readLine();
			if (command != null) {
			    command = Utils.filter(command);
			}

			/**
			 * Le client s'est deconnecte ou souhaite se deconnecter
			 */
			if (command == null || command.equals("")
				|| command.equals("EXIT/" + userName + "/")) {
			    System.out.println(" Fin de connexion.");
			    break;
			}

			/**
			 * On traite la commande envoye par le client
			 */
			synchronized (server) {
			    if (!server.AnswerClient(command, inchan, outchan,
				    this))
				server.writeAllButMe(command + "\n", outchan,
					userName);
			}
			System.out.println(userName + " said : " + command);
		    }
		    synchronized (server) {
			/**
			 * Deconnexion du client
			 */
			server.clientLeft(outchan, userName);
		    }
		    socket.close();
		} catch (IOException e) {
		    e.printStackTrace();
		    System.exit(1);
		}
	    }
	}
    }

    /**
     * Deconnecter le client actuel
     */
    public void closeSocket() {
	connecte = false;
    }

    /**
     * retourne le nom du client
     * 
     * @return userName
     */
    public String getUserName() {
	return userName;
    }

    /**
     * donner ou modifier le nom du client
     * 
     * @param userName
     */
    public void setUserName(String userName) {
	this.userName = userName;
    }

    /**
     * retourne le canal de sortie vers le client
     * 
     * @return outchan
     */
    public PrintWriter getOutchan() {
	return outchan;
    }

    /**
     * change le canal de sortie vers le client
     * 
     * @param outchan
     *            : nouveau canal de sortie
     */
    public void setOutchan(PrintWriter outchan) {
	this.outchan = outchan;
    }

}
