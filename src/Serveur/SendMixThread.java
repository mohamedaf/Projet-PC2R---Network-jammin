package serveur;

import java.io.PrintWriter;

public class SendMixThread extends Thread {

	private byte[] buffer;
	private PrintWriter out;
	private EchoJamClient clj;
	
	public SendMixThread(byte[] buffer, PrintWriter out, EchoJamClient clj) {
		this.buffer = buffer;
		this.out = out;
		this.clj = clj;
	}
	
	@Override
	public void run() {
		try {
			clj.wait();
			Commandes.audio_mix(out, new String(buffer));
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
		}
	}

}
