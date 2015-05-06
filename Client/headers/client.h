#ifndef CLIENT_H
#define CLIENT_H

#ifdef WIN32

#include <winsock2.h>

#elif defined (linux)

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h> /* close */
#include <netdb.h> /* gethostbyname */
#include "parser.h"

#define INVALID_SOCKET -1
#define SOCKET_ERROR -1
#define closesocket(s) close(s)
typedef int SOCKET;
typedef struct sockaddr_in SOCKADDR_IN;
typedef struct sockaddr SOCKADDR;
typedef struct in_addr IN_ADDR;

#else

#error not defined for this platform

#endif

#define CRLF	 "\r\n"

#define BUF_SIZE 4096

/* Permet de compiler et d'executer correctement sous windows */
void init(void);
/* Permet de compiler et d'executer correctement sous windows */
void end(void);
/* Fonction principale socket */
void app(const char *address, char *name, int port, char* fileName);
/* Initialise le socket */
int init_connection(const char *address, int port);
/* deconnecte le socket */
void end_connection(int sock);
/* Lit un message du serveur */
char* read_server(SOCKET sock);
/* Envoie un message au serveur */
void write_server(SOCKET sock, char *buffer);

#endif /* guard */

