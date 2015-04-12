package Serveur;

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
    private ServerSocket serv;
    private Socket client;
    private int capacity, nbConnectedClients, nbWaitingSocks, port;

    public EchoServer(int port, int capacity) {
	this.capacity = capacity;
	this.port = port;
	this.clients = new Vector<EchoClient>(capacity);
	this.sockets = new Vector<Socket>();
	this.streams = new Vector<DataOutputStream>();

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
	writeAllButMe("*** " + userName + " left discussion ***\n", out);
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
    public void AnswerClient(String s, DataOutputStream out, String userName) {
	if (s.equals("CONNECT/" + userName + "/")) {
	    Commandes.welcome(out, userName);
	    Commandes.audio_port(out);
	    Commandes.audio_ok(out, port);
	    Commandes.connected(this, userName, out);
	}
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
}
