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
 *         partie musicale
 */
public class EchoJamClient extends Thread {
    private BufferedReader inchan;
    private PrintWriter outchan;
    private EchoServer server;
    private Socket socket;

    public EchoJamClient(EchoServer s) {
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
		    server.newConnect(outchan);
		    /*
		     * car la capacite represente le client qui est deja
		     * connecte par la socket de controle
		     */
		    server.setNbConnectedClients(server.getNbConnectedClients() - 1);
		}
		while (true) {
		    int cpt = 0;
		    boolean ok = false;
		    byte[] buffer = null;

		    while (!inchan.ready()) {
			try {
			    Thread.sleep(250);
			    cpt++;
			} catch (InterruptedException e) {
			    e.printStackTrace(System.err);
			}

			if (cpt == 8)
			    break;
		    }

		    /* c'est ok le client est pret a envoyer */
		    if (cpt < 8) {
			String command = inchan.readLine();

			if (command == null || command.equals("")) {
			    ok = false;
			}

			if (ok) {
			    synchronized (server) {
				/** Si la requete n'est pas correcte */
				if (!server.AnswerJamClient(command, outchan,
					this, buffer)) {
				    ok = false;
				}
			    }
			}
		    } else if (!ok) {
			/** On deconnecte le client pour cause d'absence */
			Commandes.audio_ko(outchan);
			break;
		    }

		    /**
		     * Traitement donnees audio reçus
		     */

		    /**
		     * A partir d'ici l'audio est recupere dans le buffer pour
		     * l'instant j'essaye juste de reenvoyer la donnee au client
		     * et le client essaye de jouer la donnee
		     */

		    Commandes.audio_mix(outchan, new String(buffer));

		    /**
		     * Ajouter a la hashMap ou liste ou autre ..
		     */

		    /**
		     * Voir si les autres client on aussi envoye la donnee
		     * correpondant au tick actuel et si c'est le cas melanger
		     * puis envoyer le resultat sinon attendre que les autres
		     * envoient donc attendre un notify sur la case en question
		     */

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
