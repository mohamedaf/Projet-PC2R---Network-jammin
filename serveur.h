#ifndef SERVER_H
#define SERVER_H

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <netdb.h>

#define CRLF		"\r\n"
#define BUF_SIZE	1024

typedef struct
{
  int sock;
  char name[BUF_SIZE];
}Client;

/**********************************************************************************/
/******* Fonctions de base pour la connexion et l'echange client serveur **********/
/**********************************************************************************/

int init_connection(int port);
void end_connection(int sock);
int read_client(int sock, char *buffer);
void write_client(int sock, const char *buffer);
void send_message_to_all_clients(Client *clients, Client client,
				 int actual, const char *buffer, char from_server);
void remove_client(Client *clients, int to_remove, int *actual);
void clear_clients(Client *clients, int actual);


/**********************************************************************************/
/**************************** Messages du serveur *********************************/
/**********************************************************************************/

/*************************** Connexion/Deconnexion ********************************/

/* Signifie au musicien qui a demandee la connexion que celle-ci 
   est accepte sous le nom "user" */
void welcome(int sock, char *user);
/* Signifie au musicien que le serveur attend une connexion sur le port audio */
void audio_port(int sock);
/* Signifie que le canal audio est établi */
void audio_ok(int sock, char port);
/* Signifie a tous les clients la connexion de "user" */
void connected(Client *clients, Client client,
	       int actual, char from_server, char *user);
/* Signifie a tous les clients le depart de "user" */
void exited(Client *clients, Client client,
	       int actual, char from_server, char *user);

/********************* Gestion des paramètres de Jam ******************************/

/* Signale au client que la session est vide */
void empty_session(int sock);
/* Signale au client les parameetres de la jam */
void current_session(int sock, char *style, char* tempo, char nbMus);
/* Signale la bonne réception des parametres */
void ack_opts(int sock);
/* Signale au client que la session est plein */
void full_session(int sock);

/**************************** Gestion des flux audios *****************************/

/* Bonne réception du buffer */
void audio_okk(int sock);
/* Problème de réception */
void audio_ko(int sock);
/* Buffer contenant le mélange global des autres musiciens */
void audio_mix(int sock, char* buff);


/**********************************************************************************/
/*********************** Application principale ***********************************/
/**********************************************************************************/

void app(int sock);

void answer_Client(Client client, char* buffer, Client *clients, int *actual);

#endif
