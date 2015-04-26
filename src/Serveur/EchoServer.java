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
    private int capacity, nbConnectedClients, nbConnectedJamClients,
	    nbWaitingSocks, port, tickActuel, sizeBuff = 0;
    private String style, tempo;
    /**
     * la HashMap contenant les buffers recus a chaque tick, la clef de la
     * hashMap est le tick recu de la part du client + 4
     */
    private HashMap<Integer, ArrayList<byte[]>> hashBuffers;

    /**
     * Une HashMap permettant de verifier pour chaque tick si les melanges on
     * ete envoyes a tous les clients utile pour la suppression des elements
     * inutiles dans hashBuffers et donc economie de la memoire
     */
    private HashMap<Integer, Integer> hashBuffersSend;

    /**
     * Afin de pouvoir verifier dans quel type de connexion on est avant de
     * recuperer la socket
     */
    private Boolean isJamConnexion = false;

    /**
     * Constructeur
     * 
     * @param port
     *            : port pour la connexion au serveur
     * @param capacity
     *            : nombre de client maximal pouvant se connecter
     */
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
	this.hashBuffersSend = new HashMap<Integer, Integer>();

	for (int i = 0; i < capacity; i++) {
	    EchoClient tmpEcho = new EchoClient(this);
	    clients.add(tmpEcho);
	    tmpEcho.start();
	}

	this.nbConnectedClients = 0;
	this.nbConnectedJamClients = 0;
	this.nbWaitingSocks = 0;

	/* Socket audio */
	try {
	    this.serv2 = new ServerSocket();
	    this.serv2.setReuseAddress(true);
	    this.serv2.bind(new InetSocketAddress(1234));
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * Supprime et retourne la socket en tete de liste
     * 
     * @return : premier element de la liste de sockets
     */
    public Socket removeFirstSocket() {
	System.out.println("size = " + sockets.size());
	Socket ret = sockets.get(0);
	sockets.removeElementAt(0);
	return ret;
    }

    /**
     * nouvelle connexion d'un client au serveur
     * 
     * @param out
     *            : buffer de sortie vers le client
     */
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

    /**
     * nouvelle connexion d'un Jam client au serveur
     * 
     * @param out
     *            : buffer de sortie vers le client
     */
    public void newJamConnect(PrintWriter out) {
	nbConnectedJamClients++;
	nbWaitingSocks--;
	System.out.println(" New connexion at Jammin server");
	streams.add(out);
    }

    /**
     * Un client a quitte la session
     * 
     * @param out
     *            : buffer de sortie vers le client
     * @param userName
     *            : nom du client
     */
    public void clientLeft(PrintWriter out, String userName) {
	nbConnectedClients--;
	System.out.println(" Client left.");
	System.out.println("   * " + nbConnectedClients + " connected.");
	System.out.println("   * " + nbWaitingSocks + " waiting.");
	Commandes.exited(this, userName, out);
	streams.remove(out);
    }

    /**
     * Un Jam client a quitte la session
     * 
     * @param out
     *            : buffer de sortie vers le client
     */
    public void clientJamLeft(PrintWriter out) {
	nbConnectedJamClients--;
	System.out.println(" Client Jam left.");
	streams.remove(out);
    }

    /**
     * Envoie un message a tous les clients sauf le client ayant le buffer de
     * sortie out
     * 
     * @param s
     *            : chaine de caractere a envoyer
     * @param out
     *            : buffer de sortie vers le client
     */
    public void writeAllButMe(String s, PrintWriter out) {
	for (int i = 0; i < nbConnectedClients; i++) {
	    if (streams.elementAt(i) != out) {
		streams.elementAt(i).println(s);
		streams.elementAt(i).flush();
	    }
	}
    }

    /**
     * Envoie un message a tous les clients sauf le client ayant le buffer de
     * sortie out
     * 
     * @param s
     *            : chaine de caractere a envoyer
     * @param out
     *            : buffer de sortie vers le client
     * @param userName
     *            : nom du client expediteur du message
     */
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
     * 
     * @param s
     *            : message recu de la part du cleint
     * @param in
     *            : buffer d'entree du client
     * @param out
     *            : buffer de sortie vers le client
     * @param cl
     *            : le client
     * @return true si protocole reconnu false sinon (si false message chat
     *         room)
     */
    public boolean AnswerClient(String s, BufferedReader in, PrintWriter out,
	    EchoClient cl) {
	String[] tabC;
	String userName;

	/**
	 * Traitement du message CONNECT
	 */
	if (s.contains("CONNECT/")) {
	    tabC = s.split("/");
	    cl.setUserName(tabC[1]);
	    userName = cl.getUserName();
	    Commandes.welcome(out, userName);
	    Commandes.connected(this, userName, out);

	    if (this.style == null || this.tempo == null) {
		/**
		 * Premier client connecte
		 */

		try {
		    /**
		     * On demande au premier client de choisir le style et le
		     * tempo
		     */

		    String answer, tab[];
		    boolean repeter = true;

		    while (repeter) {
			Commandes.empty_session(out);
			answer = in.readLine();
			if (answer != null)
			    answer = Utils.filter(answer);

			tab = answer.split("/");

			if (tab[0].equals("SET_OPTIONS") && (tab.length == 3)) {
			    this.setStyle(tab[1]);
			    this.setTempo(tab[2]);
			    /** taille tableau = 60/tempo * 44100 */
			    this.sizeBuff = (60 / Integer.parseInt(this
				    .getTempo())) * 44100;
			    repeter = false;
			}
		    }

		    /**
		     * Signaler la bonne reception des parametres
		     */
		    Commandes.ack_opts(out);
		} catch (IOException e) {
		    e.printStackTrace(System.err);
		}
	    } else {
		/**
		 * ca n'est pas le premier client donc le style et le tempo sont
		 * deja connues
		 */

		Commandes.current_session(out, this.getStyle(),
			this.getTempo(), this.getNbConnectedClients());
		Commandes.audio_sync(out, this.getTickActuel());
	    }
	    Commandes.audio_port(out);

	    /**
	     * Etablir le canal audio
	     */

	    try {
		EchoJamClient tmpEcho = new EchoJamClient(this, cl);
		tmpEcho.start();
		/******************* Client de test ********************/
		// ClientTest ct = new ClientTest();
		// ct.start();
		/******************************************************/
		client = serv2.accept();
		synchronized (this) {
		    sockets.add(client);
		    nbWaitingSocks++;
		    /** Je previens que c'est une connexion a la jam session */
		    this.setIsJamConnexion(true);
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

    /**
     * Traiter un message venant d'un Jam client
     * 
     * @param s
     *            : le message venant du client
     * @param out
     *            : buffer de sortie vers le client
     * @param cl
     *            : le client
     * @return : le buffer envoye par le client
     */
    public byte[] AnswerJamClient(String s, PrintWriter out, EchoJamClient cl) {
	byte[] buffer = null;
	Integer tick;

	/** Traitement reception buffer audio */

	String tab[] = s.split("/");
	String buffertmp[];

	System.out.println("commande reçu : " + s);
	System.out.flush();

	/**
	 * Si la requete du client est de type AUDIO_CHUNK et que la requete est
	 * correcte
	 */
	if (tab[0].equals("AUDIO_CHUNK") && tab.length == 3
		&& (!tab[1].equals("")) && (!tab[2].equals(""))) {

	    /** recuperer la tick envoye par le client */
	    tick = Integer.parseInt(tab[1]);

	    /** On met a jour la variable representant le tick actuel */
	    if (tick > tickActuel)
		tickActuel = tick;

	    /** recuperer les donnees dans le buffer */
	    buffertmp = tab[2].split(" ");
	    buffer = new byte[sizeBuff];

	    /** Convetir les donnees recues en int puis en byte */
	    for (int i = 0; i < sizeBuff; i++) {
		buffer[i] = Byte.parseByte(buffertmp[i]);
	    }

	    /** Si bonne reception */
	    Commandes.audio_okk(out);
	}

	return buffer;
    }

    /**
     * retourne Nombre de clients en attente de connexion
     * 
     * @return nbWaitingSocks
     */
    public int stillWaiting() {
	return nbWaitingSocks;
    }

    /**
     * Le serveur principal
     */
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
		    this.setIsJamConnexion(false);
		    /** Je previens que c'est une connexion d'un nouveau client */
		    this.notify();
		}
	    }
	} catch (Throwable t) {
	    t.printStackTrace(System.err);
	}
    }

    /**
     * retourne la socket du serveur principale
     * 
     * @return : server socket
     */
    public ServerSocket getServ() {
	return serv;
    }

    /**
     * retourne la socket du serveur Jam
     * 
     * @return : server Jam socket
     */
    public ServerSocket getServ2() {
	return serv2;
    }

    /**
     * retourne le nombre maximal de clients sur une session
     * 
     * @return : capacity
     */
    public int getCapacity() {
	return capacity;
    }

    /**
     * retourne le nombre de clients connectes
     * 
     * @return : nbConnectedClients
     */
    public int getNbConnectedClients() {
	return nbConnectedClients;
    }

    /**
     * retourne le nombre de Jam clients connectes
     * 
     * @return : nbConnectedJamClients
     */
    public int getNbConnectedJamClients() {
	return nbConnectedJamClients;
    }

    /**
     * modifier le nombre de clients en attente de connexion
     * 
     * @param nbWaitingSocks
     *            : nouveau nombre de clients en attente de connexion
     */
    public void setNbWaitingSocks(int nbWaitingSocks) {
	this.nbWaitingSocks = nbWaitingSocks;
    }

    /**
     * le Style de la Jam session
     * 
     * @return : style
     */
    public String getStyle() {
	return style;
    }

    /**
     * modifier le style de la Jam session
     * 
     * @param style
     *            : nouveau style
     */
    public void setStyle(String style) {
	this.style = style;
    }

    /**
     * le tempo de la Jam session
     * 
     * @return : tempo
     */
    public String getTempo() {
	return tempo;
    }

    /**
     * modifier le tempo de la Jam session
     * 
     * @param tempo
     *            : nouveau tempo
     */
    public void setTempo(String tempo) {
	this.tempo = tempo;
    }

    /**
     * retourne le tick actuel
     * 
     * @return : tickActuel
     */
    public int getTickActuel() {
	return tickActuel;
    }

    /**
     * modifie le tick actuel
     * 
     * @param tickActuel
     *            : nouveau tick actuel
     */
    public void setTickActuel(int tickActuel) {
	this.tickActuel = tickActuel;
    }

    /**
     * retourne la hashMap contenant les buffers audios
     * 
     * @return : hashBuffers
     */
    public HashMap<Integer, ArrayList<byte[]>> getHashBuffers() {
	return hashBuffers;
    }

    /**
     * retourne la hashMap de verification du nombre des melanges pret a
     * l'envois
     * 
     * @return : hashBuffersSend
     */
    public HashMap<Integer, Integer> getHashBuffersSend() {
	return hashBuffersSend;
    }

    /**
     * retourne l'element pointe par k dans la HashMap HashBuffersSend
     * 
     * @param k
     *            : key
     * @return : valeur de key dans la hashmap
     */
    public Integer getInHashBuffersSend(Integer k) {
	return hashBuffersSend.get(k);
    }

    /**
     * ajouter une valeur pointe par k dans la HashMap HashBuffersSend
     * 
     * @param k
     *            : key
     * @param v
     *            : valeur
     */
    public void putInHashBuffersSend(Integer k, Integer v) {
	hashBuffersSend.put(k, v);
    }

    /**
     * informe le type de la derniere connexion d'un client au serveur
     * 
     * @return : true si c'est une Jam connexion, false sinon
     */
    public Boolean getIsJamConnexion() {
	return isJamConnexion;
    }

    /**
     * modifier le type de la derniere connexion d'un client au serveur
     * 
     * @param isJamConnexion
     *            : true si c'est le cas, false sinon
     */
    public void setIsJamConnexion(Boolean isJamConnexion) {
	this.isJamConnexion = isJamConnexion;
    }

    /**
     * retourne la taille d'un buffer audio dans cette session
     * 
     * @return : sizeBuff
     */
    public int getSizeBuff() {
	return sizeBuff;
    }

}
