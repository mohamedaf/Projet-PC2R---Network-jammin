package serveur;

/**
 * 
 * @author Hassan
 * 
 *         La classe contenant une methode modifiant une chaine de caractere et
 *         la retournant sans les caracteres non correctes s'ils existent
 */
public class Utils {

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
