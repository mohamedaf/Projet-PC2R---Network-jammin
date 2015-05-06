#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include "parser.h"

void my_log(char* msg) {
	printf("%s\n", msg);
}

int parse_line_by_line(char* str, int* tempo, long* first_tick) {
	char buffer[1024];

	char *token = "\r\n";

	int l = strlen(str);
	char *start, *p;
	start = str;
	int port = -1, port1 = -1;

	p = strstr(str, token);
	while (p) {
		strncpy(buffer, str, p - str);
		buffer[p - str] = '\0';

		port1 = parse(buffer, tempo, first_tick);
		if (port1 > 0)
			port = port1;

		str = p + strlen(token);

		if ((str - start) >= l)
			break;

		p = strstr(str, token);
	}
	if (!p) {
		port1 = parse(str, tempo, first_tick);
		if (port1 > 0)
			port = port1;
	}
	return port;
}

int parse(char* tab, int* tempo, long* first_tick) {
	char* tab2 = malloc(sizeof(char) * strlen(tab) + 5);
	strcpy(tab2, tab);
	char* token = strtok(tab2, "/");
	char* token1, *token2;
	char* ret = malloc(128 * sizeof(char));
	int n, audio_connect = -1;

	if (strcmp(token, "WELCOME") == 0) {
		token = strtok(NULL, "/");
		sprintf(ret, "Connexion succesfull. Your name on the server is : %s",
				token);
	} else if (strcmp(token, "AUDIO_PORT") == 0) {
		token = strtok(NULL, "/");
		n = atoi(token);
		sprintf(ret, "Connect on port %d for audio transmission.", n);
		/* On se connecte au port audio */
		audio_connect = n;
	} else if (strcmp(token, "AUDIO_OK") == 0) {

	} else if (strcmp(token, "CONNECTED") == 0) {
		token = strtok(NULL, "/");
		sprintf(ret, "%s is now connected.", token);
	} else if (strcmp(token, "EXITED") == 0) {
		token = strtok(NULL, "/");
		sprintf(ret, "%s is now disconnected.", token);
	} else if (strcmp(token, "EMPTY_SESSION") == 0) {
		ret = "Session is empty.\nUse : SET_OPTIONS/style/tempo";
	} else if (strcmp(token, "CURRENT_SESSION") == 0) {
		token = strtok(NULL, "/");
		token1 = strtok(NULL, "/");
		token2 = strtok(NULL, "/");
		sprintf("Current session : style -> %s, tempo -> %s, musicians -> %s.",
				token, token1, token2);
		*tempo = atoi(token1);

	} else if (strcmp(token, "ACK_OPTS") == 0) {
		ret = "Set options successfull.";
	} else if (strcmp(token, "FULL_SESSION") == 0) {
		ret = "Current session is full.";
	} else if (strcmp(token, "AUDIO_OK") == 0) {

	} else if (strcmp(token, "AUDIO_KO") == 0) {
		ret = "Server has audio reception problems";
	} else if (strcmp(token, "AUDIO_SYNC") == 0) {
		token = strtok(NULL, "/");
		*first_tick = atoi(token) + 1;
	} else if (strcmp(token, "AUDIO_MIX") == 0) {

	} else if (strcmp(token, "LISTEN") == 0) {
		token = strtok(NULL, "/");
		token1 = strtok(NULL, "/");
	} else {
		//perror("Unknown command\n");
	}
	my_log(ret);

	/* On retourne le port auquel on doit se connecter ou -1 */
	return audio_connect;

}

int parse_output(char* msg) {
	char* token = strtok(msg, "/");
	char* token1;
	int tempo = -1;

	if (strcmp(token, "SET_OPTIONS") == 0) {
		token = strtok(NULL, "/");
		token1 = strtok(NULL, "/");
		printf("Style : %s, tempo : %s", token, token1);
		tempo = atoi(token1);
	}
	return tempo;

}

char* concat_strings(char* str1, char* str2) {
	char * new_str;
	if ((new_str = malloc(strlen(str1) + strlen(str2) + 1)) != NULL) {
		new_str[0] = '\0';
		strcat(new_str, str1);
		strcat(new_str, str2);
	} else {
		perror("malloc failed!\n");
	}
	return new_str;
}
