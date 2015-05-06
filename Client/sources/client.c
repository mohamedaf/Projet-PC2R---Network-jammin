#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <pthread.h>

#include "client.h"
#include "parser.h"
#include "alsa_play.h"
#include "audio_client.h"

void init(void) {
#ifdef WIN32
	WSADATA wsa;
	int err = WSAStartup(MAKEWORD(2, 2), &wsa);
	if(err < 0)
	{
		puts("WSAStartup failed !");
		exit(EXIT_FAILURE);
	}
#endif
}

void end(void) {
#ifdef WIN32
	WSACleanup();
#endif
}

void app(const char *address, char *name, int port, char* fileName) {
	SOCKET sock = init_connection(address, port);
	char* buffer = malloc(sizeof(char) * BUF_SIZE);
	int audio_port, tempo = -1, client_originel = 0;
	long first_tick = 0;
	audio_socket_infos* a_s_i = NULL;
	pthread_t audio_client_thread;

	fd_set rdfs;

	char* t = malloc(sizeof(char) * 512);
	sprintf(t, "CONNECT/%s/", name);
	write_server(sock, t);
	free(t);

	while (1) {
		FD_ZERO(&rdfs);

		/* add STDIN_FILENO */
		FD_SET(STDIN_FILENO, &rdfs);

		/* add the socket */
		FD_SET(sock, &rdfs);

		if (select(sock + 1, &rdfs, NULL, NULL, NULL) == -1) {
			perror("select()");
			exit(errno);
		}

		/* something from standard input : i.e keyboard */
		if (FD_ISSET(STDIN_FILENO, &rdfs)) {
			fgets(buffer, BUF_SIZE - 1, stdin);
			{
				char *p = NULL;
				p = strstr(buffer, "\n");
				if (p != NULL) {
					*p = 0;
				} else {
					buffer[BUF_SIZE - 1] = 0;
				}
			}
			write_server(sock, buffer);
			if ((tempo = parse_output(buffer)) > 0) {
				client_originel = 1;
			}
		} else if (FD_ISSET(sock, &rdfs)) {
			buffer = read_server(sock);

			if ((audio_port = parse_line_by_line(buffer, &tempo, &first_tick))
					> 0) {
				/* Demarrer le client audio */
				printf("Port : %d\n", audio_port);
				a_s_i = new_audio_socket_infos(address, audio_port, tempo,
						client_originel, first_tick, fileName);

				pthread_create(&audio_client_thread, NULL, thread_audio_client,
						(void*) a_s_i);
			}
		}

	}

	end_connection(sock);
	pthread_join(audio_client_thread, NULL);
	free(a_s_i);
}

int init_connection(const char *address, int port) {
	SOCKET sock = socket(AF_INET, SOCK_STREAM, 0);
	SOCKADDR_IN sin = { 0 };
	struct hostent *hostinfo;

	if (sock == INVALID_SOCKET) {
		perror("socket()");
		exit(errno);
	}

	hostinfo = gethostbyname(address);
	if (hostinfo == NULL) {
		fprintf(stderr, "Unknown host %s.\n", address);
		exit(EXIT_FAILURE);
	}

	sin.sin_addr = *(IN_ADDR *) hostinfo->h_addr;
	sin.sin_port = htons(port);
	sin.sin_family = AF_INET;

	if (connect(sock, (SOCKADDR *) &sin, sizeof(SOCKADDR)) == SOCKET_ERROR) {
		perror("connect()");
		exit(errno);
	}

	return sock;
}

void end_connection(int sock) {
	closesocket(sock);
}

char* read_server(SOCKET sock) {
	int n = 0;
	char* buffer = malloc(sizeof(char) * BUF_SIZE);
	if ((n = recv(sock, buffer, BUF_SIZE - 1, 0)) <= 0) {
		perror("recv()");
		perror("Disconnected from server");
		exit(errno);
	}
	buffer[n] = '\0';
	printf("Received : %s", buffer);

	/*int i;
	 for (i = 0; i < strlen(buffer); i++) {
	 printf("%d-", buffer[i]);
	 }*/
	return buffer;
}

void write_server(SOCKET sock, char *buffer) {
	strcat(buffer, "\n");
	if (send(sock, buffer, strlen(buffer) + 1, 0) < 0) {
		perror("send()");
		exit(errno);
	}
}

int main(int argc, char **argv) {
	if (argc < 1) {
		printf("Usage : %s [address] [-port] [-user]\n", argv[0]);
		return EXIT_FAILURE;
	}

	int port = 2015;
	char* name = "pc2r";
	int i;
	char* file = NULL;
	for (i = 2; i < argc; i++) {
		if (strcmp(argv[i], "-port")==0) {
			i++;
			port = atoi(argv[i]);
		} else if (strcmp(argv[i], "-user")==0) {
			i++;
			name = argv[i];
		}else if (strcmp(argv[i], "-f")==0) {
			i++;
			file = argv[i];
		}
	}

	init();

	app(argv[1], name, port, file);

	end();

	return EXIT_SUCCESS;
}
