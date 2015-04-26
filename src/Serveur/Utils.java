package serveur;

public class Utils {

    public static String filter(String in) {
	String out = "";

	for (char c : in.toCharArray()) {
	    if (c > 31)
		out += c;
	}
	return out;
    }
}
