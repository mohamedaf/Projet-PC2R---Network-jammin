package Serveur;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class EchoClient extends Thread {
    private BufferedReader inchan;
    private DataOutputStream outchan;
    private EchoServer server;
    private Socket socket;
    private String userName;

    public EchoClient(EchoServer s) {
	server = s;
    }

    @Override
    public void run() {
	Socket s;

	while (true) {
	    synchronized (server) {
		if (server.stillWaiting() == 0)
		    try {
			server.wait();
		    } catch (InterruptedException e) {
			e.printStackTrace();
		    }
		s = server.removeFirstSocket();
	    }
	    try {
		inchan = new BufferedReader(new InputStreamReader(
			s.getInputStream()));
		outchan = new DataOutputStream(s.getOutputStream());
		socket = s;
		synchronized (server) {
		    server.newConnect(outchan);
		}
		/* Recuperer le nom du client */
		userName = inchan.readLine();
		synchronized (server) {
		    server.writeAllButMe("*** New user on chat ***\n"
			    + "*** User name : " + userName + " ***\n", outchan);
		}
		while (true) {
		    String command = inchan.readLine();
		    if (command == null || command.equals("")
			    || s.equals("EXIT/" + userName + "/")) {
			System.out.println(" Fin de connexion.");
			break;
		    }
		    synchronized (server) {
			server.AnswerClient(command, outchan, userName);
			server.writeAllButMe(command + "\n", outchan, userName);
		    }
		}
		synchronized (server) {
		    server.clientLeft(outchan, userName);
		}
		socket.close();
	    } catch (IOException e) {
		e.printStackTrace();
		System.exit(1);
	    }
	}
    }

    public String getUserName() {
	return userName;
    }

    public void setUserName(String userName) {
	this.userName = userName;
    }

}
