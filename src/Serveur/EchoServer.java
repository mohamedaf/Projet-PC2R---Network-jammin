package serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import client.ClientTest;

/**
 * Idee essayer d'ameliorer en liberant la place d'un client apres deconnexion
 */

/**
 * 
 * @author Mohamed AMIN
 * 
 *         La classe representant le serveur
 */
public class EchoServer {
	private Vector<EchoClient> clients;
	private Vector<Socket> sockets;
	private Vector<PrintWriter> streams;
	private ServerSocket serv, serv2;
	private Socket client;
	private int capacity, nbConnectedClients, nbWaitingSocks, port, tickActuel;
	private String style, tempo;
	/**
	 * la HashMap contenant les buffers recus a chaque tick, la clef de la
	 * hashMap est le tick re?u de la part du client + 4
	 */
	private HashMap<Integer, ArrayList<byte[]>> hashBuffers;

	public EchoServer(int port, int capacity) {
		this.capacity = capacity;
		this.port = port;
		this.clients = new Vector<EchoClient>(capacity);
		this.sockets = new Vector<Socket>();
		this.streams = new Vector<PrintWriter>();
		this.style = null;
		this.tempo = null;
		this.serv = null;
		this.serv2 = null;
		this.hashBuffers = new HashMap<Integer, ArrayList<byte[]>>();

		for (int i = 0; i < capacity; i++) {
			EchoClient tmpEcho = new EchoClient(this);
			clients.add(tmpEcho);
			tmpEcho.start();
		}

		nbConnectedClients = 0;
		nbWaitingSocks = 0;

		/* Socket audio */
		try {
			this.serv2 = new ServerSocket();
			this.serv2.setReuseAddress(true);
			this.serv2.bind(new InetSocketAddress(2014));
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}

	public Socket removeFirstSocket() {
		System.out.println("size = " + sockets.size());
		Socket ret = sockets.get(0);
		sockets.removeElementAt(0);
		return ret;
	}

	public void newConnect(PrintWriter out) {
		nbConnectedClients++;
		nbWaitingSocks--;
		System.out.println(" Thread handled connection.");
		System.out.println("   * " + nbConnectedClients + " connected.");
		System.out.println("   * " + nbWaitingSocks + " waiting.");
		streams.add(out);
		out.println(" Please give you name :");
		out.flush();
	}

	public void clientLeft(PrintWriter out, String userName) {
		nbConnectedClients--;
		System.out.println(" Client left.");
		System.out.println("   * " + nbConnectedClients + " connected.");
		System.out.println("   * " + nbWaitingSocks + " waiting.");
		Commandes.exited(this, userName, out);
		streams.remove(out);
	}

	public void clientJamLeft(PrintWriter out) {
		System.out.println(" Client Jam left.");
		streams.remove(out);
	}

	public void writeAllButMe(String s, PrintWriter out) {
		for (int i = 0; i < nbConnectedClients; i++) {
			if (streams.elementAt(i) != out) {
				streams.elementAt(i).println(s);
				streams.elementAt(i).flush();
			}
		}
	}

	public void writeAllButMe(String s, PrintWriter out, String userName) {
		for (int i = 0; i < nbConnectedClients; i++) {
			if (streams.elementAt(i) != out) {
				streams.elementAt(i).println("\n" + userName + " :");
				streams.elementAt(i).println(s);
				streams.elementAt(i).flush();
			}
		}
	}

	/**
	 * Traiter les reponses aux differentes demandes du client definies dans le
	 * protocole
	 */
	public boolean AnswerClient(String s, BufferedReader in, PrintWriter out,
			String userName, EchoClient cl) {

		/* Traitement du message CONNECT */
		if (s.equals("CONNECT/" + userName + "/")) {
			Commandes.welcome(out, userName);
			Commandes.connected(this, userName, out);

			if (this.style == null || this.tempo == null) {
				/* Premier client connecte */
				Commandes.empty_session(out);
				try {
					/**
					 * On demande au premier client de choisir le style et le
					 * tempo
					 */

					String answer, tab[];
					boolean repeter = true;

					while (repeter) {
						out.println("\nVeuillez indiquer le style "
								+ "et le tempo voulu :");
						out.flush();
						answer = in.readLine();

						if (answer == null
								|| answer.equals("EXIT/" + userName + "/")) {
							cl.closeSocket();
							return true;
						}

						tab = answer.split("/");

						if (tab[0].equals("SET_OPTIONS") && (tab.length == 3)) {
							this.setStyle(tab[1]);
							this.setTempo(tab[2]);
							repeter = false;
						}
					}

					/* Signaler la bonne reception des parametres */
					Commandes.ack_opts(out);
				} catch (IOException e) {
					e.printStackTrace(System.err);
				}
			} else {
				/**
				 * ça n'est pas le premier client donc le style et le tempo
				 * sont deja connues
				 */

				Commandes.current_session(out, this.getStyle(),
						this.getTempo(), this.getNbConnectedClients());
			}

			Commandes.audio_port(out);

			/**
			 * Etablir le canal audio
			 */

			try {
				EchoJamClient tmpEcho = new EchoJamClient(this, cl);
				tmpEcho.start();
				/******************* Client de test ********************/
				ClientTest ct = new ClientTest();
				ct.start();
				/******************************************************/
				client = serv2.accept();
				System.out.println("New connexion at Jammin server.");
				synchronized (this) {
					sockets.add(client);
					System.out.println("after add size = " + sockets.size());
					nbWaitingSocks++;
					this.notify();
				}
			} catch (Throwable t) {
				t.printStackTrace(System.err);
			}

			Commandes.audio_ok(out);
			return true;
		}

		/* Fin du traitement du message CONNECT */

		return false;
	}

	public boolean AnswerJamClient(String s, PrintWriter out, EchoJamClient cl,
			byte[] buffer, Integer tick) {
		/** Traitement reception buffer audio */

		String tab[] = s.split("/");
		String buffertmp[];

		/**
		 * Si la requete du client est de type AUDIO_CHUNK et que la requete est
		 * correcte
		 */
		if (tab[0].equals("AUDIO_CHUNK") && tab.length == 3
				&& (!tab[1].equals("")) && (!tab[2].equals(""))) {

			/** recuperer la tick envoye par le client */
			tick = Integer.parseInt(tab[1]);
			/** On met a jour la variable representant le tick actuel */
			tickActuel = tick;

			/** recuperer les donnees dans le buffer */
			buffertmp = tab[2].split(" ");
			buffer = new byte[buffertmp.length];

			/** Convetir les donnees recues en int puis en byte */
			for (int i = 0; i < buffertmp.length; i++) {
				buffer[i] = Byte.parseByte(buffertmp[i]);
			}

			/** Si bonne reception */
			Commandes.audio_okk(out);

			return true;
		}

		return false;
	}

	public int stillWaiting() {
		return nbWaitingSocks;
	}

	public void run() {
		try {
			serv = new ServerSocket();
			serv.setReuseAddress(true);
			serv.bind(new InetSocketAddress(port));
			DeconnexionServer ds = new DeconnexionServer(this);
			ds.start();
			System.out.println("Please tape \"exit\" to quit proprely");

			while (true) {
				client = serv.accept();
				System.out.println("New connexion at server.");
				synchronized (this) {
					sockets.add(client);
					nbWaitingSocks++;
					this.notify();
				}
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	public Vector<EchoClient> getClients() {
		return clients;
	}

	public void setClients(Vector<EchoClient> clients) {
		this.clients = clients;
	}

	public Vector<Socket> getSockets() {
		return sockets;
	}

	public void setSockets(Vector<Socket> sockets) {
		this.sockets = sockets;
	}

	public Vector<PrintWriter> getStreams() {
		return streams;
	}

	public void setStreams(Vector<PrintWriter> streams) {
		this.streams = streams;
	}

	public ServerSocket getServ() {
		return serv;
	}

	public void setServ(ServerSocket serv) {
		this.serv = serv;
	}

	public ServerSocket getServ2() {
		return serv2;
	}

	public void setServ2(ServerSocket serv2) {
		this.serv2 = serv2;
	}

	public Socket getClient() {
		return client;
	}

	public void setClient(Socket client) {
		this.client = client;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getNbConnectedClients() {
		return nbConnectedClients;
	}

	public void setNbConnectedClients(int nbConnectedClients) {
		this.nbConnectedClients = nbConnectedClients;
	}

	public int getNbWaitingSocks() {
		return nbWaitingSocks;
	}

	public void setNbWaitingSocks(int nbWaitingSocks) {
		this.nbWaitingSocks = nbWaitingSocks;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public String getTempo() {
		return tempo;
	}

	public void setTempo(String tempo) {
		this.tempo = tempo;
	}

	public int getTickActuel() {
		return tickActuel;
	}

	public void setTickActuel(int tickActuel) {
		this.tickActuel = tickActuel;
	}

	public HashMap<Integer, ArrayList<byte[]>> getHashBuffers() {
		return hashBuffers;
	}

	public void setHashBuffers(HashMap<Integer, ArrayList<byte[]>> hashBuffers) {
		this.hashBuffers = hashBuffers;
	}

}
