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
		server = s;
		this.file = new ArrayList<byte[]>();
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
					Integer tick = -1;

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
										this, buffer, tick)) {
									ok = false;
								}
							}
						}
					} else if (!ok || tick == -1) {
						if (tick == -1)
							System.out.println("tick == -1 Erreur !!");

						/** On deconnecte le client pour cause d'absence */
						Commandes.audio_ko(outchan);
						break;
					}

					/**
					 * Traitement donnees audio reçus
					 */

					// Commandes.audio_mix(outchan, new String(buffer));

					if (!server.getHashBuffers().containsKey(tick + 4)) {
						server.getHashBuffers().put(tick + 4,
								new ArrayList<byte[]>());
					}

					/** j'ajoute le buffer a la hashMap */
					server.getHashBuffers().get(tick + 4).add(buffer);

					if (server.getHashBuffers().get(tick + 4).size() == server
							.getNbConnectedClients()) {
						/*
						 * Demarrer le thread qui verifie la file d'attente et a
						 * chaque notification reçu vide le premier element
						 */
						
						SendMixThread smx = new SendMixThread(buffer, outchan, this);
						smx.start();
						
						/* Je melange */

						MixThread mx = new MixThread(server.getHashBuffers()
								.get(tick + 4), this);
						
						mx.start();
					}

				}
				synchronized (server) {
					server.clientJamLeft(outchan);
					/**
					 * On deconnecte aussi le client sur l'autre socket
					 */
					server.clientLeft(client.getOutchan(), client.getUserName());
				}
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
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
}
