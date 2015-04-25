package serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

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
    private EchoClient client;
    /**
     * File contenant le resultat du mix
     */
    private ArrayList<byte[]> file;

    public EchoJamClient(EchoServer s, EchoClient client) {
	this.server = s;
	this.file = new ArrayList<byte[]>();
    }

    @Override
    public void run() {
	Socket s = null;
	boolean me;

	while (true) {
	    me = false;

	    synchronized (server) {
		if (server.stillWaiting() == 0) {
		    try {
			server.wait();
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		}

		if (server.getIsJamConnexion()) {
		    me = true;
		} else {
		    /**
		     * Si je ne suis pas concerne je notifie l'autre client qui
		     * peut l'etre
		     */
		    server.notify();
		}

		if (me) {
		    System.out.println("EchoJamClient choisi");
		    System.out.flush();
		    s = server.removeFirstSocket();
		} else {
		    // System.out.println("EchoJamClient non choisi");
		    // System.out.flush();
		}
	    }
	    if (me) {
		try {
		    inchan = new BufferedReader(new InputStreamReader(
			    s.getInputStream()));
		    outchan = new PrintWriter(s.getOutputStream());
		    socket = s;

		    synchronized (server) {
			server.newJamConnect(outchan);
		    }

		    while (true) {
			int cpt = 0;
			boolean ok = true;
			byte[] buffer = null;
			Integer tick = -1;

			while (!inchan.ready()) {
			    try {
				Thread.sleep(10000);
				cpt++;
			    } catch (InterruptedException e) {
				e.printStackTrace(System.err);
			    }

			    if (cpt == 8)
				break;
			}

			/* c'est ok le client est pret a envoyer */
			if (cpt < 8) {
			    System.out.println("c'est ok, cpt = " + cpt);
			    System.out.flush();
			    String command = inchan.readLine();

			    if (command == null || command.equals("")) {
				ok = false;
			    }

			    if (ok) {
				synchronized (server) {
				    /** Si la requete n'est pas correcte */
				    if (!server.AnswerJamClient(command,
					    outchan, this, buffer, tick)) {
					ok = false;
				    }
				}
			    }
			} else {
			    System.out.println("c'est ko1");
			    System.out.flush();
			    /** On deconnecte le client pour cause d'absence */
			    Commandes.audio_ko(outchan);
			    break;
			}

			/** La requete n'est pas correcte */
			if (!ok) {
			    System.out.println("c'est ko2");
			    System.out.flush();
			    /** On deconnecte le client */
			    Commandes.audio_ko(outchan);
			    break;
			}

			/**
			 * Traitement donnees audio reçus
			 */

			synchronized (server) {
			    if (!server.getHashBuffers().containsKey(tick + 4)) {
				server.getHashBuffers().put(tick + 4,
					new ArrayList<byte[]>());
				/**
				 * Pour l'instant aucun client n'a recu de
				 * melange
				 */
				server.putInHashBuffersSend(tick + 4, 0);

				/** On met a jour le tick actuel du serveur */
				server.setTickActuel(tick);
			    }

			    /** j'ajoute le buffer a la hashMap */
			    server.getHashBuffers().get(tick + 4).add(buffer);

			    if (server.getHashBuffers().get(tick + 4).size() == server
				    .getNbConnectedJamClients()) {
				/**
				 * Demarrer le thread qui verifie la file
				 * d'attente et a chaque notification reçu vide
				 * le premier element
				 */

				SendMixThread smx = new SendMixThread(this,
					outchan, tick + 4);
				smx.start();

				/** Je retire le buffer actuel puis je melange */

				ArrayList<byte[]> buffersToMix = new ArrayList<byte[]>();

				for (byte[] b : server.getHashBuffers().get(
					tick + 4)) {
				    if (!b.equals(buffer))
					buffersToMix.add(b);
				}

				MixThread mx = new MixThread(buffersToMix, this);

				mx.start();
			    }
			}

		    }
		    synchronized (server) {
			server.clientJamLeft(outchan);
			/**
			 * On deconnecte aussi le client sur l'autre socket
			 */
			server.clientLeft(client.getOutchan(),
				client.getUserName());
		    }
		    socket.close();
		} catch (IOException e) {
		    e.printStackTrace();
		    System.exit(1);
		}
	    }
	}
    }

    public void testRemoveKeyFromHash(Integer key) {
	if (server.getInHashBuffersSend(key) >= server
		.getNbConnectedJamClients()) {
	    /** Les melanges ont ete envoyes a tous les clients */
	    server.getHashBuffers().remove(key);
	    server.getHashBuffersSend().remove(key);
	}
    }

    public ArrayList<byte[]> getFile() {
	return file;
    }

    public void setFile(ArrayList<byte[]> file) {
	this.file = file;
    }

    public void addToFile(byte[] buffer) {
	file.add(buffer);
    }

    public byte[] popFile() {
	return file.remove(0);
    }

    public Integer getActualTick() {
	synchronized (server) {
	    return server.getTickActuel();
	}
    }
}
