package serveur.Serveurs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import serveur.Outils.Commandes;
import serveur.Outils.MixThread;
import serveur.Outils.SendMixThread;

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

    /**
     * Constructeur
     * 
     * @param s
     *            : le serveur
     * @param client
     *            : le client correspondant
     */
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

	    /**
	     * Attendre la connexion d'un client
	     */
	    synchronized (server) {
		if (server.stillWaiting() == 0) {
		    try {
			server.wait();
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		}

		if (server.getIsJamConnexion()) {
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
		    System.out.println("EchoJamClient choisi");
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
			/**
			 * On previens le serveur de la nouvelle connexion
			 */
			server.newJamConnect(outchan);
		    }

		    while (true) {
			int cpt = 0;
			boolean ok = true;
			byte[] buffer = null;
			Integer tick = -1;

			while (!inchan.ready()) {
			    try {
				Thread.sleep(250);
				cpt++;
			    } catch (InterruptedException e) {
				e.printStackTrace(System.err);
			    }

			    if (cpt == 8) {
				/**
				 * le client n'envoie rien on doit le
				 * deconnecter
				 */
				break;
			    }
			}

			/** c'est ok le client est pret a envoyer */
			if (cpt < 8) {
			    System.out.println("c'est ok, cpt = " + cpt);
			    System.out.flush();
			    String command = inchan.readLine();

			    if (command == null || command.equals("")) {
				ok = false;
			    }

			    if (ok) {
				synchronized (server) {
				    buffer = server.AnswerJamClient(command,
					    outchan, this);

				    /** Si la requete n'est pas correcte */
				    if (buffer == null) {
					ok = false;
				    }

				    tick = server.getTickActuel();

				    System.out.println("After AnswerJamClient");
				    System.out.println("tick = " + tick);

				    String s3 = "";

				    for (byte b : buffer) {
					s3 += b + " ";
				    }

				    System.out.println("buffer = " + s3);
				    System.out.flush();
				}
			    }
			} else {
			    /** On deconnecte le client pour cause d'absence */
			    Commandes.audio_ko(outchan);
			    break;
			}

			/** La requete n'est pas correcte */
			if (!ok) {
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
			    }

			    /** j'ajoute le buffer a la hashMap */
			    server.getHashBuffers().get(tick + 4).add(buffer);

			    while (server.getHashBuffers().get(tick + 4).size() < server
				    .getNbConnectedJamClients()) {
				System.out.println("Thread " + this.getName()
					+ ": Attente des autres clients");
			    }

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

    /**
     * Incrementer de 1 la valeur correspondante a key dans la HashMap de
     * verification
     * 
     * @param key
     *            : key
     */
    public void IncrHashBuffersSend(Integer key) {
	synchronized (server) {
	    server.putInHashBuffersSend(key,
		    server.getInHashBuffersSend(key) + 1);
	}
    }

    /**
     * sypprimer les elements correspondants a key dans les deux HashMap si le
     * melange est envoye a tous les clients
     * 
     * @param key
     *            : key
     */
    public void testRemoveKeyFromHash(Integer key) {
	synchronized (server) {
	    if (server.getInHashBuffersSend(key) >= server
		    .getNbConnectedJamClients()) {
		/** Les melanges ont ete envoyes a tous les clients */
		server.getHashBuffers().remove(key);
		server.getHashBuffersSend().remove(key);
	    }
	}
    }

    /**
     * retourne la file d'attente de buffers
     * 
     * @return : file
     */
    public ArrayList<byte[]> getFile() {
	return file;
    }

    /**
     * Ajouter un buffer a la file
     * 
     * @param buffer
     *            : buffer
     */
    public void addToFile(byte[] buffer) {
	file.add(buffer);
    }

    /**
     * supprime le premier element de la file et le retourne
     * 
     * @return : premier element de la file
     */
    public byte[] popFile() {
	return file.remove(0);
    }

    /**
     * retourne le tick actuel du serveur
     * 
     * @return : tickActuel serveur
     */
    public Integer getActualTick() {
	synchronized (server) {
	    return server.getTickActuel();
	}
    }

    /**
     * retourne la taille d'un buffer
     * 
     * @return : buffer size
     */
    public int getSizeBuff() {
	return server.getSizeBuff();
    }
}
