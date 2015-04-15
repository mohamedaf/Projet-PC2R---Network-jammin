package Serveur;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Mohamed AMIN
 * 
 *         La classe contenant les differents messages correspondants au
 *         protocole que le serveur peux envoyer a un client
 */

public class Commandes {

    /**
     * Connexion/Deconnexion
     */

    /**
     * Signifie au musicien qui a demandee la connexion que celle-ci est accepte
     * sous le nom "userName"
     */
    public static void welcome(DataOutputStream out, String userName) {
	try {
	    out.writeBytes("WELCOME/" + userName + "/\n");
	    out.flush();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * Signifie au musicien que le serveur attend une connexion sur le port
     * audio
     */
    public static void audio_port(DataOutputStream out) {
	try {
	    out.writeBytes("AUDIO_PORT/2014/\n");
	    out.flush();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * Signifie que le canal audio est établi
     */
    public static void audio_ok(DataOutputStream out) {
	try {
	    out.writeBytes("AUDIO_OK/2014/\n");
	    out.flush();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * Signifie a tous les clients la connexion de "userName"
     */
    public static void connected(EchoServer s, String userName,
	    DataOutputStream out) {
	s.writeAllButMe("CONNECTED/" + userName + "/\n", out);
    }

    /**
     * Signifie a tous les clients le depart de "userName"
     */
    public static void exited(EchoServer s, String userName,
	    DataOutputStream out) {
	s.writeAllButMe("EXITED/" + userName + "/\n", out);
    }

    /**
     * Gestion des paramètres de Jam
     */

    /**
     * Signale au client que la session est vide
     */
    public static void empty_session(DataOutputStream out) {
	try {
	    out.writeBytes("EMPTY_SESSION\n");
	    out.flush();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * Signale au client les parameetres de la jam
     */
    public static void current_session(DataOutputStream out, String style,
	    String tempo, int nbMus) {
	try {
	    out.writeBytes("CURRENT_SESSION/" + style + "/" + tempo + "/\n"
		    + nbMus + "/");
	    out.flush();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * Signale la bonne réception des parametres
     */
    public static void ack_opts(DataOutputStream out) {
	try {
	    out.writeBytes("ACK_OPTS\n");
	    out.flush();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * Signale au client que la session est plein
     */
    public static void full_session(DataOutputStream out) {
	try {
	    out.writeBytes("FULL_SESSION\n");
	    out.flush();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * Gestion des flux audios
     */

    /**
     * Bonne réception du buffer
     */
    public static void audio_okk(DataOutputStream out) {
	try {
	    out.writeBytes("AUDIO_OK\n");
	    out.flush();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * Problème de réception
     */
    public static void audio_ko(DataOutputStream out) {
	try {
	    out.writeBytes("AUDIO_KO\n");
	    out.flush();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * Buffer contenant le mélange global des autres musiciens
     */
    public static void audio_mix(DataOutputStream out, String buffer) {
	try {
	    out.writeBytes(buffer);
	    out.flush();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

}
