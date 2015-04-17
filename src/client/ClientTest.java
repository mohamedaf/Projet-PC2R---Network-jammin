package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientTest extends Thread {

    @Override
    public void run() {

	try {
	    Socket s = new Socket("localhost", 2014);
	    BufferedReader in = new BufferedReader(new InputStreamReader(
		    s.getInputStream()));
	    PrintWriter out = new PrintWriter(s.getOutputStream());

	    double[] buffer = new double[44100];

	    String s2 = "AUDIO_CHUNK/1/";

	    for (int i = 0; i < 44100; i++) {
		buffer[i] = Math.random() * 100;
		s2 += buffer[i] + "";

		if (i < 44099)
		    s2 += " ";
	    }

	    s2 += "/";

	    out.write(s2);

	    String answer = in.readLine();

	    String[] tab = answer.split("/");
	    String[] tab2 = tab[1].split(" ");

	    double[] buffer2 = new double[44100];

	    for (int i = 0; i < tab2.length; i++) {
		buffer2[i] = Double.parseDouble(tab2[i]);
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
