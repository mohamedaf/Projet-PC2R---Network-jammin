package serveur.serveurs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 
 * @author Mohamed-Amin AFFES - Hassan KOBROSLI
 * 
 *         Cette classe lit sur l'entree standard j'usqua ce que le mot "exit"
 *         soit tapee, dans ce cas il ferme la socket du serveur et arrete le
 *         programme
 */
public class DeconnexionServer extends Thread {

    private EchoServer server;

    /**
     * Constructeur
     * 
     * @param server
     *            : instance du serveur
     */
    public DeconnexionServer(EchoServer server) {
	this.server = server;
    }

    /**
     * 
     * @return serveur
     */
    public EchoServer getServer() {
	return server;
    }

    /**
     * changer le serveur
     * 
     * @param server
     *            : nouveau serveur
     */
    public void setServer(EchoServer server) {
	this.server = server;
    }

    /**
     * Thread attendant que "exit" soit tapé pour fermer la socket proprement et
     * donc deconnecter les serveurs (protocole et Jam)
     */
    @Override
    public void run() {
	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	String input;

	try {
	    while ((input = br.readLine()) != null) {
		if (input.equals("exit")) {
		    System.out.println("deconnecte");

		    /* Deconnecter les serveurs */
		    this.getServer().getServ().close();
		    if (this.getServer().getServ2() != null)
			this.getServer().getServ2().close();
		    System.exit(0);
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

}
