package Serveur;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

/**
 * Idee essayer d'ameliorer en liberant la place d'un client apres deconnexion
 */

public class EchoServer {
    private Vector<EchoClient> clients;
    private Vector<Socket> sockets;
    private Vector<DataOutputStream> streams;
    private ServerSocket serv, serv2;
    private Socket client;
    private int capacity, nbConnectedClients, nbWaitingSocks, port;
    private String style, tempo;

    public EchoServer(int port, int capacity) {
	this.capacity = capacity;
	this.port = port;
	this.clients = new Vector<EchoClient>(capacity);
	this.sockets = new Vector<Socket>();
	this.streams = new Vector<DataOutputStream>();
	this.style = null;
	this.tempo = null;

	for (int i = 0; i < capacity; i++) {
	    EchoClient tmpEcho = new EchoClient(this);
	    clients.add(tmpEcho);
	    tmpEcho.start();
	}

	nbConnectedClients = 0;
	nbWaitingSocks = 0;
    }

    public Socket removeFirstSocket() {
	Socket ret = sockets.get(0);
	sockets.removeElementAt(0);
	return ret;
    }

    public void newConnect(DataOutputStream out) {
	nbConnectedClients++;
	nbWaitingSocks--;
	System.out.println(" Thread handled connection.");
	System.out.println("   * " + nbConnectedClients + " connected.");
	System.out.println("   * " + nbWaitingSocks + " waiting.");
	streams.add(out);
	try {
	    out.writeChars(" Please give you name :\n");
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    public void clientLeft(DataOutputStream out, String userName) {
	nbConnectedClients--;
	System.out.println(" Client left.");
	System.out.println("   * " + nbConnectedClients + " connected.");
	System.out.println("   * " + nbWaitingSocks + " waiting.");
	Commandes.exited(this, userName, out);
	streams.remove(out);
    }

    public void clientJamLeft(DataOutputStream out) {
	System.out.println(" Client Jam left.");
	streams.remove(out);
    }

    public void writeAllButMe(String s, DataOutputStream out) {
	try {
	    for (int i = 0; i < nbConnectedClients; i++) {
		if (streams.elementAt(i) != out)
		    streams.elementAt(i).writeChars(s + "\n");
	    }
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    public void writeAllButMe(String s, DataOutputStream out, String userName) {
	try {
	    for (int i = 0; i < nbConnectedClients; i++) {
		if (streams.elementAt(i) != out) {
		    streams.elementAt(i).writeChars("\n" + userName + " :\n");
		    streams.elementAt(i).writeChars(s + "\n");
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * Traiter les reponses aux differentes demandes du client definies dans le
     * protocole
     */
    public boolean AnswerClient(String s, BufferedReader in,
	    DataOutputStream out, String userName) {
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
			out.writeChars("\nVeuillez indiquer le style "
				+ "et le tempo voulu\n");
			answer = in.readLine();
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
		 * ça n'est pas le premier client donc le style et le tempo sont
		 * deja connues
		 */

		Commandes.current_session(out, this.getStyle(),
			this.getTempo(), this.getNbConnectedClients());
	    }

	    Commandes.audio_port(out);

	    /**
	     * Etablir le canal audio
	     */

	    try {
		serv2 = new ServerSocket();
		serv2.setReuseAddress(true);
		serv2.bind(new InetSocketAddress(port));
		client = serv2.accept();
		System.out.println("New connexion at Jammin server.");
		synchronized (this) {
		    sockets.add(client);
		    nbWaitingSocks++;
		    this.notify();
		}
	    } catch (Throwable t) {
		t.printStackTrace(System.err);
	    }

	    Commandes.audio_ok(out);

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

    public Vector<DataOutputStream> getStreams() {
	return streams;
    }

    public void setStreams(Vector<DataOutputStream> streams) {
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

}
