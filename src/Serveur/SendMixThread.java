package serveur;

import java.io.PrintWriter;

/**
 * 
 * @author Mohamed AMIN
 * 
 *         La classe effectuant l'envois du melange au client
 */
public class SendMixThread extends Thread {

    private EchoJamClient clj;
    private PrintWriter out;
    private Integer tick;

    public SendMixThread(EchoJamClient clj, PrintWriter out, Integer tick) {
	this.clj = clj;
	this.out = out;
	this.tick = tick;
    }

    @Override
    public void run() {
	try {
	    clj.wait();

	    /**
	     * J'opte pour l'instant pour la solution avec attente active c'est
	     * certe tres moche mais deja pour voir si le serveur fonctionne et
	     * pouvoir debuguer dans le cas contraire
	     */

	    while (tick > clj.getActualTick()) {
		System.out.println("J'attend le tick " + tick
			+ ", tick actuel " + clj.getActualTick());
	    }

	    /** tick Actuel du serveur = tick on peux donc envoyer le melange */

	    Commandes.audio_mix(out, new String(clj.popFile()));
	    /**
	     * Verifier si on doit supprimer les element de la clef dans la hash
	     * ou pas encore donc si tous les clients ont recu le melange du
	     * tick actuel
	     */
	    clj.testRemoveKeyFromHash(tick);
	} catch (InterruptedException e) {
	    e.printStackTrace(System.err);
	}
    }

}
