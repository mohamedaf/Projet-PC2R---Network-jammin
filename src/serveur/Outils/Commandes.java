package serveur.Outils;

import java.io.PrintWriter;

import serveur.Serveurs.EchoServer;

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
     * 
     * @param out
     *            : Buffer d'envois au client
     * @param userName
     *            : Nom du client connecte
     */
    public synchronized static void welcome(PrintWriter out, String userName) {
	out.println("WELCOME/" + userName + "/");
	out.flush();
    }

    /**
     * Signifie au musicien que le serveur attend une connexion sur le port
     * audio
     * 
     * @param out
     *            : Buffer d'envois au client
     */
    public static void audio_port(PrintWriter out) {
	out.println("AUDIO_PORT/1234/");
	out.flush();
    }

    /**
     * Signifie que le canal audio est etabli
     * 
     * @param out
     *            : Buffer d'envois au client
     */
    public static void audio_ok(PrintWriter out) {
	out.println("AUDIO_OK/1234/");
	out.flush();
    }

    /**
     * Signifie a tous les clients la connexion de "userName"
     * 
     * @param s
     *            : Serveur
     * @param userName
     *            : Nom du client connecte
     * @param out
     *            : Buffer d'envois au client
     */
    public static void connected(EchoServer s, String userName, PrintWriter out) {
	s.writeAllButMe("CONNECTED/" + userName + "/", out);
    }

    /**
     * Signifie a tous les clients le depart de "userName"
     * 
     * @param s
     *            : Serveur
     * @param userName
     *            : Nom du client connecte
     * @param out
     *            : Buffer d'envois au client
     */
    public static void exited(EchoServer s, String userName, PrintWriter out) {
	s.writeAllButMe("EXITED/" + userName + "/", out);
    }

    /***************************************************************************
     * Gestion des paramètres de Jam
     */

    /**
     * Signale au client que la session est vide
     * 
     * @param out
     *            : Buffer d'envois au client
     */
    public static void empty_session(PrintWriter out) {
	out.println("EMPTY_SESSION/");
	out.flush();
    }

    /**
     * Signale au client les parameetres de la jam
     * 
     * @param out
     *            : Buffer d'envois au client
     * @param style
     *            : style de la Jam session
     * @param tempo
     *            : tempo de la Jam session
     * @param nbMus
     *            : Nombre de musiciens connectes a la Jam session
     */
    public static void current_session(PrintWriter out, String style,
	    String tempo, int nbMus) {
	out.println("CURRENT_SESSION/" + style + "/" + tempo + "/" + nbMus
		+ "/");
	out.flush();
    }

    /**
     * Signale la bonne reception des parametres
     * 
     * @param out
     *            : Buffer d'envois au client
     */
    public static void ack_opts(PrintWriter out) {
	out.println("ACK_OPTS/");
	out.flush();
    }

    /**
     * Signale au client que la session est plein
     * 
     * @param out
     *            : Buffer d'envois au client
     */
    public static void full_session(PrintWriter out) {
	out.println("FULL_SESSION/");
	out.flush();
    }

    /***************************************************************************
     * Gestion des flux audios
     */

    /**
     * Bonne reception du buffer
     * 
     * @param out
     *            : Buffer d'envois au client
     */
    public static void audio_okk(PrintWriter out) {
	out.println("AUDIO_OK/");
	out.flush();
    }

    /**
     * Problème de reception
     * 
     * @param out
     *            : Buffer d'envois au client
     */
    public static void audio_ko(PrintWriter out) {
	out.println("AUDIO_KO/");
	out.flush();
    }

    /**
     * Message au nouveau client indiquant la dernière valeur de tick envoyee
     * 
     * @param out
     *            : Buffer d'envois au client
     * @param tick
     *            : tick actuel du serveur
     */
    public static void audio_sync(PrintWriter out, int tick) {
	out.println("AUDIO_SYNC/" + tick + "/");
	out.flush();
    }

    /**
     * Buffer contenant le melange global des autres musiciens
     * 
     * @param out
     *            : Buffer d'envois au client
     * @param buffer
     *            : buffer audio du melange
     */
    public static void audio_mix(PrintWriter out, String buffer) {
	out.println("AUDIO_MIX/" + buffer + "/");
	out.flush();
    }

    /***************************************************************************
     * Discussion instantanee
     */

    public static void Listen(EchoServer s, PrintWriter out, String userName,
	    String message) {
	s.writeAllButMe("LISTEN/" + userName + "/" + message + "/", out);
    }

    /**
     * retourne la chaine sans les caracteres non correctes
     * 
     * @param in
     *            : chaine de caractere
     * @return : chaine correcte
     */
    public static String filter(String in) {
	String out = "";

	for (char c : in.toCharArray()) {
	    if (c > 31)
		out += c;
	}
	return out;
    }

}
