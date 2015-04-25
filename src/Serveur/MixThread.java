package serveur;

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

import testAudio.MixingFloatAudioInputStream;

/**
 * 
 * @author Mohamed AMIN
 * 
 *         La classe effectuant le melange des differents buffers
 */
public class MixThread extends Thread {

    private ArrayList<byte[]> buffers;
    private EchoJamClient clj;

    public MixThread(ArrayList<byte[]> buffers, EchoJamClient clj) {
	this.buffers = buffers;
	this.clj = clj;
    }

    /**
     * Je melange
     */
    @Override
    public void run() {
	ArrayList<File> file = new ArrayList<File>();

	/* pas sur si je mets buffers.size() ou bien 1 ou 2 */
	AudioFormat format = new AudioFormat(22050f, 16, buffers.size(), true,
		false);

	try {
	    for (int i = 0; i < buffers.size(); i++) {
		file.add(new File("copie" + i + ".aif"));
		ByteArrayInputStream dataStream = new ByteArrayInputStream(
			buffers.get(i));
		AudioInputStream mixed = new AudioInputStream(dataStream,
			format, buffers.get(i).length / format.getFrameSize());

		AudioSystem
			.write(mixed, AudioFileFormat.Type.AIFF, file.get(i));

	    }

	    /* Nous avons maintenant buffers.size() copies a mixer */

	    Collection list = new ArrayList();

	    for (int i = 0; i < buffers.size(); i++) {
		try {
		    list.add(AudioSystem.getAudioInputStream(new File("copie"
			    + i + ".aif")));
		} catch (UnsupportedAudioFileException | IOException e) {
		    e.printStackTrace(System.err);
		}
	    }

	    /*** Instancier la classe s'occupant du melange */
	    MixingFloatAudioInputStream mx = new MixingFloatAudioInputStream(
		    format, list);

	    /** creer tableau de taille suffisante puis recuperation du melange */
	    byte[] buffer = new byte[buffers.get(0).length];

	    /* Recuperation du melange et ajout a la file */
	    mx.read(buffer, 0, buffers.get(0).length);

	    synchronized (clj) {
		clj.addToFile(buffer);
		/* Notifier l'ajout */
		clj.notify();
	    }
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}

    }

}