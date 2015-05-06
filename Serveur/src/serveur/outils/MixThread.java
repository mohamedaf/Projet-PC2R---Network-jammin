package serveur.outils;

import java.util.ArrayList;

import serveur.serveurs.EchoJamClient;

/**
 * 
 * @author Mohamed-Amin AFFES - Hassan KOBROSLI
 * 
 *         La classe effectuant le melange des differents buffers
 */
public class MixThread extends Thread {

    private ArrayList<float[]> buffers;
    private EchoJamClient clj;

    /**
     * Constructeur
     * 
     * @param buffers
     *            : ArrayList contenant les buffers a melanger
     * @param clj
     *            : Jam client a qui le melange doit etre envoye
     */
    public MixThread(ArrayList<float[]> buffers, EchoJamClient clj) {
	this.buffers = buffers;
	this.clj = clj;
    }

    /**
     * Je melange
     */
    @Override
    public void run() {
	/**
	 * buffers.size() = 0 si client unique dans le cas du client unique un
	 * tableau contenant que des 0 est envoye
	 */
	if (buffers.size() > 0) {

	    /**
	     * creer tableau de taille suffisante puis recuperation du melange
	     */
	    float[] buffer = new float[clj.getSizeBuff()];

	    /**
	     * On additionne les donnees
	     */
	    for (int i = 0; i < clj.getSizeBuff(); i++) {
		buffer[i] = 0;

		for (float[] f : buffers) {
		    buffer[i] += f[i];
		}
	    }

	    boolean depassement = false;

	    for (float f : buffer) {
		if (f > 1 || f < -1) {
		    depassement = true;
		    break;
		}
	    }

	    /**
	     * On recherche la valeur max du buffer
	     */
	    if (depassement) {
		float max = Float.MIN_VALUE;

		for (float f : buffer) {
		    if (Math.abs(f) > max) {
			max = Math.abs(f);
		    }
		}

		/**
		 * On divise le tout par cette valeur
		 */
		for (int i = 0; i < buffer.length; i++) {
		    buffer[i] = buffer[i] / max;
		}
	    }

	    synchronized (clj) {
		clj.addToFile(buffer);
		/** Notifier l'ajout */
		clj.notifyAll();
	    }

	} else {
	    /**
	     * Pour traiter le cas d'un client unique le client ignore donc ce
	     * tableau d'un element contenant un zero
	     */
	    float[] buffer = new float[clj.getSizeBuff()];

	    for (int i = 0; i < clj.getSizeBuff(); i++) {
		buffer[i] = 0;
	    }

	    synchronized (clj) {
		clj.addToFile(buffer);
		/** Notifier l'ajout */
		clj.notifyAll();
	    }
	}

    }
}