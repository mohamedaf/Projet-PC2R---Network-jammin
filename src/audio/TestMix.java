package audio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class TestMix {

    public static void main(String[] args) {
	CopieTest();
    }

    /**
     * Dans cette methode on lit deux fichiers puis creons une copie d'eux
     */
    @SuppressWarnings({ "rawtypes", "unchecked", "resource" })
    public static void CopieTest() {
	try {

	    /** Recuperation des deux fichiers audios */
	    AudioInputStream clip1 = AudioSystem.getAudioInputStream(new File(
		    "audio/a2.aif"));
	    AudioInputStream clip2 = AudioSystem.getAudioInputStream(new File(
		    "audio/a3.aif"));

	    /******************* FICHIER 1 ************************************/

	    /** On les ajoutent a la collection */
	    Collection list = new ArrayList();
	    list.add(clip1);

	    /** On regle les parametres */

	    /**
	     * Format contenant les parametres essentiels pour la classe de
	     * melange
	     */

	    AudioFormat format = new AudioFormat(22050f, 16, 1, true, false);

	    /** Instancier la classe s'occupant du melange */
	    MixingFloatAudioInputStream mx = new MixingFloatAudioInputStream(
		    format, list);

	    /** creer tableau de taille suffisante puis recuperation du melange */
	    byte[] buffer;
	    buffer = new byte[3 * ((int) mx.getFrameLength())];

	    File f = new File("copie1.aif");
	    ByteArrayInputStream dataStream = new ByteArrayInputStream(buffer);
	    AudioInputStream mixed = new AudioInputStream(dataStream, format,
		    buffer.length / format.getFrameSize());

	    mx.read(buffer, 0, 3 * ((int) mx.getFrameLength()));
	    AudioSystem.write(mixed, AudioFileFormat.Type.AIFF, f);

	    /****************** FICHIER 2 *************************************/

	    /** On les ajoutent a la collection */
	    list = new ArrayList();
	    list.add(clip2);
	    format = new AudioFormat(22050f, 16, 1, true, false);

	    mx = new MixingFloatAudioInputStream(format, list);

	    /** creer tableau de taille suffisante puis recuperation du melange */
	    buffer = new byte[3 * ((int) mx.getFrameLength())];

	    f = new File("copie2.aif");
	    dataStream = new ByteArrayInputStream(buffer);
	    mixed = new AudioInputStream(dataStream, format, buffer.length
		    / format.getFrameSize());

	    mx.read(buffer, 0, 3 * ((int) mx.getFrameLength()));
	    AudioSystem.write(mixed, AudioFileFormat.Type.AIFF, f);

	    /** recuperation depuis copie1 et copie2 puis melange */
	    TestMelange();

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * On melange ici les deux fichiers crees par la methode precedente
     */
    @SuppressWarnings({ "rawtypes", "unchecked", "resource" })
    public static void TestMelange() {
	try {

	    /****************** Melange des deux copies ***********************/

	    /** Recuperation des deux fichiers audios */
	    AudioInputStream clip1 = AudioSystem.getAudioInputStream(new File(
		    "copie1.aif"));
	    AudioInputStream clip2 = AudioSystem.getAudioInputStream(new File(
		    "copie2.aif"));

	    /** On les ajoutent a la collection */
	    Collection list = new ArrayList();
	    list.add(clip1);
	    list.add(clip2);

	    /** On regle les parametres */
	    /**
	     * float frameRate = 44100f; // 44100 samples/s int channels = 2;
	     * double duration = 1.0; int sampleBytes = Short.SIZE / 8; int
	     * frameBytes = sampleBytes * channels;
	     */

	    /**
	     * Format contenant les parametres essentiels pour la classe de
	     * melange
	     */

	    AudioFormat format = new AudioFormat(22050f, 16, 1, true, false);

	    /**
	     * new AudioFormat(Encoding.PCM_SIGNED, frameRate, Short.SIZE,
	     * channels, frameBytes, frameRate, true);
	     */

	    /*** Instancier la classe s'occupant du melange */
	    MixingFloatAudioInputStream mx = new MixingFloatAudioInputStream(
		    format, list);

	    /** creer tableau de taille suffisante puis recuperation du melange */
	    byte[] buffer;
	    buffer = new byte[3 * ((int) mx.getFrameLength())];

	    File f = new File("resultat.aif");
	    ByteArrayInputStream dataStream = new ByteArrayInputStream(buffer);
	    AudioInputStream mixed = new AudioInputStream(dataStream, format,
		    buffer.length / format.getFrameSize());

	    mx.read(buffer, 0, 3 * ((int) mx.getFrameLength()));
	    AudioSystem.write(mixed, AudioFileFormat.Type.AIFF, f);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}
