package testAudio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class Test {

    public static void main(String[] args) {
	saveAudio1();
    }

    /* Ne fonctionne pas encore */
    @SuppressWarnings({ "rawtypes", "unused" })
    public static void saveAudio1() {
	try {

	    /* Recuperation des deux fichiers audios */
	    AudioInputStream clip1 = AudioSystem.getAudioInputStream(new File(
		    "audio/a2.aif"));
	    AudioInputStream clip2 = AudioSystem.getAudioInputStream(new File(
		    "audio/a3.aif"));

	    /* On les ajoutent a la collection */
	    Collection list = new ArrayList();
	    list.add(clip1);
	    list.add(clip2);

	    /* On regle les parametres */
	    /*
	     * float frameRate = 44100f; // 44100 samples/s int channels = 2;
	     * double duration = 1.0; int sampleBytes = Short.SIZE / 8; int
	     * frameBytes = sampleBytes * channels;
	     */

	    /*
	     * Format contenant les parametres essentiels pour la classe de
	     * melange
	     */

	    AudioFormat format = clip1.getFormat();

	    /*
	     * new AudioFormat(Encoding.PCM_SIGNED, frameRate, Short.SIZE,
	     * channels, frameBytes, frameRate, true);
	     */

	    /* Instancier la classe s'occupant du melange */
	    MixingFloatAudioInputStream mx = new MixingFloatAudioInputStream(
		    format, list);

	    /* creer tableau de taille suffisante puis recuperation du melange */
	    byte[] buffer;
	    buffer = new byte[3 * ((int) mx.getFrameLength())];

	    File f = new File("toto.aif");
	    ByteArrayInputStream dataStream = new ByteArrayInputStream(buffer);
	    AudioInputStream mixed = new AudioInputStream(dataStream, format,
		    buffer.length / format.getFrameSize());

	    for (int i = 0; i < 1; i++) {
		mx.read(buffer, 0, 3 * ((int) mx.getFrameLength()));

		for (byte b : buffer) {
		    System.out.println(b);
		}

		AudioSystem.write(mixed, AudioFileFormat.Type.AIFF, f);
	    }

	    /* Ecriture du resultat */
	    // FileOutputStream fos = new FileOutputStream(new
	    // File("toto.aif"));
	    // fos.write(buffer, 0, buffer.length);
	    // fos.close();

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
