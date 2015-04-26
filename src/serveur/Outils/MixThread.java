package serveur.Outils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import serveur.serveurs.EchoJamClient;
import audio.MixingFloatAudioInputStream;

/**
 * 
 * @author Mohamed AMIN
 * 
 *         La classe effectuant le melange des differents buffers
 */
public class MixThread extends Thread {

    private ArrayList<byte[]> buffers;
    private EchoJamClient clj;

    /**
     * Constructeur
     * 
     * @param buffers
     *            : ArrayList contenant les buffers a melanger
     * @param clj
     *            : Jam client a qui le melange doit etre envoye
     */
    public MixThread(ArrayList<byte[]> buffers, EchoJamClient clj) {
	this.buffers = buffers;
	this.clj = clj;
    }

    /**
     * Je melange
     */
    @SuppressWarnings({ "rawtypes", "unchecked", "resource" })
    @Override
    public void run() {
	/**
	 * buffers.size() = 0 si client unique dans le cas du client unique un
	 * tableau contenant que des 0 est envoye
	 */
	if (buffers.size() > 0) {
	    ArrayList<File> file = new ArrayList<File>();

	    /**
	     * La variable format sert pour la construction des nouveaux
	     * fichiers audio et le melange elle contient les parametres audios
	     * a prendre en compte
	     */
	    // pas sur si je mets buffers.size() ou bien 1 ou 2
	    AudioFormat format = new AudioFormat(44100f, 16, buffers.size(),
		    true, false);

	    try {
		/**
		 * Creer des fichiers audios a partir des buffers
		 */
		for (int i = 0; i < buffers.size(); i++) {
		    file.add(new File("copie" + i + ".aif"));
		    ByteArrayInputStream dataStream = new ByteArrayInputStream(
			    buffers.get(i));
		    AudioInputStream mixed = new AudioInputStream(dataStream,
			    format, buffers.get(i).length
				    / format.getFrameSize());

		    AudioSystem.write(mixed, AudioFileFormat.Type.AIFF,
			    file.get(i));

		}

		/** Nous avons maintenant buffers.size() copies a mixer */

		Collection list = new ArrayList();

		for (int i = 0; i < buffers.size(); i++) {
		    try {
			list.add(AudioSystem.getAudioInputStream(new File(
				"copie" + i + ".aif")));
		    } catch (UnsupportedAudioFileException | IOException e) {
			e.printStackTrace(System.err);
		    }
		}

		/*** Instancier la classe s'occupant du melange */
		MixingFloatAudioInputStream mx = new MixingFloatAudioInputStream(
			format, list);

		/**
		 * creer tableau de taille suffisante puis recuperation du
		 * melange
		 */
		byte[] buffer = new byte[clj.getSizeBuff()];

		/** Recuperation du melange et ajout a la file */
		mx.read(buffer, 0, clj.getSizeBuff());

		synchronized (clj) {
		    clj.addToFile(buffer);
		    /** Notifier l'ajout */
		    clj.notifyAll();
		}
	    } catch (IOException e) {
		e.printStackTrace(System.err);
	    }
	} else {
	    /**
	     * Pour traiter le cas d'un client unique le client ignore donc ce
	     * tableau d'un element contenant un zero
	     */
	    byte[] buffer = new byte[clj.getSizeBuff()];

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