package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 
 * @author Mohamed AMIN
 * 
 *         Classe de test simulant un client envoyant un buffer
 */
public class ClientTest extends Thread {

    @Override
    public void run() {

	try {
	    Socket s = new Socket("localhost", 2014);
	    BufferedReader in = new BufferedReader(new InputStreamReader(
		    s.getInputStream()));
	    PrintWriter out = new PrintWriter(s.getOutputStream());

	    Integer[] buffer = new Integer[44100];

	    String s2 = "AUDIO_CHUNK/1/";

	    for (int i = 0; i < 44100; i++) {
		buffer[i] = (int) (Math.random() * 100);
		s2 += buffer[i] + "";

		if (i < 44099)
		    s2 += " ";
	    }

	    s2 += "/";

	    out.write(s2);

	    String answer = in.readLine();

	    String[] tab = answer.split("/");
	    String[] tab2 = tab[1].split(" ");

	    Integer[] buffer2 = new Integer[44100];

	    for (int i = 0; i < tab2.length; i++) {
		buffer2[i] = Integer.parseInt(tab2[i]);
	    }

	    if (buffer.equals(buffer2))
		System.out.println("ça marche");
	    else
		System.out.println("Erreur");

	    in.close();
	    out.close();
	} catch (IOException e) {
	    System.out.println("tototo");
	    e.printStackTrace(System.err);
	}
    }
}
