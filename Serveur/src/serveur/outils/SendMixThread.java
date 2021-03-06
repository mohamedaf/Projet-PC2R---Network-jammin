package serveur.outils;

import java.io.PrintWriter;

import serveur.serveurs.EchoJamClient;

/**
 * 
 * @author Mohamed-Amin AFFES - Hassan KOBROSLI
 * 
 *         La classe effectuant l'envois du melange au client
 */
public class SendMixThread extends Thread {

	private EchoJamClient clj;
	private PrintWriter out;
	private Integer tick;

	/**
	 * Constructeur
	 * 
	 * @param clj
	 *            : Jam client
	 * @param out
	 *            : Buffer de sortie vers le client
	 * @param tick
	 *            : tick correspondant au moment de l'envois
	 */
	public SendMixThread(EchoJamClient clj, PrintWriter out, Integer tick) {
		this.clj = clj;
		this.out = out;
		this.tick = tick;
	}

	@Override
	public void run() {
		try {
			/**
			 * attente d'un ajout d'un buffer a la file d'attente du Jam client
			 */
			synchronized (clj) {
				clj.wait();
			}

			/**
			 * J'opte pour l'instant pour la solution avec attente active c'est
			 * certe tres moche mais deja pour voir si le serveur fonctionne et
			 * pouvoir debuguer dans le cas contraire
			 */

			while (tick > clj.getActualTick()) {
				/*
				 * System.out.println("Thread " + this.getName() +
				 * ": J'attend le tick " + tick + ", tick actuel " +
				 * clj.getActualTick());
				 */
			}

			/** tick Actuel du serveur = tick on peux donc envoyer le melange */

			String buffer = "";

			int cpt = 0;
			for (float f : clj.popFile()) {
				buffer += String.valueOf((int) f * 32767);
				buffer += " ";
				cpt++;
				if (cpt % 1024 == 0) {
					Commandes.audio_mix(out, buffer);
					buffer = "";
				}
			}

			/**
			 * Verifier si on doit supprimer les element de la clef dans la hash
			 * ou pas encore donc si tous les clients ont recu le melange du
			 * tick actuel
			 */
			synchronized (clj) {
				/**
				 * J'incremente la valeur correspondante dans la hashMap de
				 * verification
				 */
				clj.IncrHashBuffersSend(tick);
				/**
				 * Je verifie si on a effectue nbJamClientsConnectes envois on
				 * on peux supprimer la cle correspondant au tick dans les deux
				 * HashMap
				 */
				clj.testRemoveKeyFromHash(tick);
			}
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
		}
	}

}
