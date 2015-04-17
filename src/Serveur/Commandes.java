package serveur;

import java.io.PrintWriter;

/**
 * 
 * @author Mohamed AMIN
 * 
 *         La classe contenant les differents messages correspondants au
 *         protocole que le serveur peux envoyer a un client
 */

public class Commandes {

    /***************************************************************************
     * Connexion/Deconnexion
     */

    /**
     * Signifie au musicien qui a demandee la connexion que celle-ci est accepte
     * sous le nom "userName"
     */
    public static void welcome(PrintWriter out, String userName) {
	out.println("WELCOME/" + userName + "/");
	out.flush();
    }

    /**
     * Signifie au musicien que le serveur attend une connexion sur le port
     * audio
     */
    public static void audio_port(PrintWriter out) {
	out.println("AUDIO_PORT/2014/");
	out.flush();
    }

    /**
     * Signifie que le canal audio est établi
     */
    public static void audio_ok(PrintWriter out) {
	out.println("AUDIO_OK/2014/");
	out.flush();
    }

    /**
     * Signifie a tous les clients la connexion de "userName"
     */
    public static void connected(EchoServer s, String userName, PrintWriter out) {
	s.writeAllButMe("CONNECTED/" + userName + "/", out);
    }

    /**
     * Signifie a tous les clients le depart de "userName"
     */
    public static void exited(EchoServer s, String userName, PrintWriter out) {
	s.writeAllButMe("EXITED/" + userName + "/", out);
    }

    /***************************************************************************
     * Gestion des paramètres de Jam
     */

    /**
     * Signale au client que la session est vide
     */
    public static void empty_session(PrintWriter out) {
	out.println("EMPTY_SESSION");
	out.flush();
    }

    /**
     * Signale au client les parameetres de la jam
     */
    public static void current_session(PrintWriter out, String style,
	    String tempo, int nbMus) {
	out.println("CURRENT_SESSION/" + style + "/" + tempo + "/" + nbMus
		+ "/");
	out.flush();
    }

    /**
     * Signale la bonne réception des parametres
     */
    public static void ack_opts(PrintWriter out) {
	out.println("ACK_OPTS");
	out.flush();
    }

    /**
     * Signale au client que la session est plein
     */
    public static void full_session(PrintWriter out) {
	out.println("FULL_SESSION");
	out.flush();
    }

    /***************************************************************************
     * Gestion des flux audios
     */

    /**
     * Bonne réception du buffer
     */
    public static void audio_okk(PrintWriter out) {
	out.println("AUDIO_OK");
	out.flush();
    }

    /**
     * Problème de réception
     */
    public static void audio_ko(PrintWriter out) {
	out.println("AUDIO_KO");
	out.flush();
    }

    /**
     * Message au nouveau client indiquant la dernière valeur de tick envoyee
     */
    public static void audio_sync(PrintWriter out, int tick) {
	out.println("AUDIO_SYNC/" + tick + "/");
	out.flush();
    }

    /**
     * Buffer contenant le mélange global des autres musiciens
     */
    public static void audio_mix(PrintWriter out, String buffer) {
	out.println("AUDIO_MIX/" + buffer + "/");
	out.flush();
    }

}
