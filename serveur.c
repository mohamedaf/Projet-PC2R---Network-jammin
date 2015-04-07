#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>

#include "serveur.h"
//#include "client.h"

int MAX_CLIENTS = 4;

/**********************************************************************************/
/******* Fonctions de base pour la connexion et l'echange client serveur **********/
/**********************************************************************************/

int init_connection(int port)
{
   int sock;
   struct sockaddr_in sin = { 0 };

   /* On cree la socket */
   
   if((sock=socket(AF_INET, SOCK_STREAM, 0)) == -1)
   {
      perror("Fonction init_connection : socket()\n");
      exit(errno);
   }

   sin.sin_addr.s_addr = htonl(INADDR_ANY);
   sin.sin_port = htons(port);
   sin.sin_family = AF_INET;

   if(bind(sock,(struct sockaddr *) &sin, sizeof sin) == -1)
   {
      perror("Fonction init_connection : bind()\n");
      exit(errno);
   }

   /* On autorise un nombre MAX_CLIENTS de clients 
      au maximum a se connecter au serveur */
   
   if(listen(sock, MAX_CLIENTS) == -1)
   {
      perror("Fonction init_connection : listen()\n");
      exit(errno);
   }

   return sock;
}


void write_client(int sock, const char *buffer)
{
   if(write(sock, buffer, strlen(buffer)) == -1)
   {
      perror("fonction write_client : write()\n");
      exit(errno);
   }
}


int read_client(int sock, char *buffer)
{
   int n = 0;

   if((n = read(sock, buffer, BUF_SIZE - 1)) == -1)
   {
      perror("read_client : read()\n");
      /* Si erreur on deconnectent le socket */
      n = 0;
   }

   buffer[n] = 0;
   return n;
}


void clear_clients(Client *clients, int actual)
{
   int i;
   
   for(i = 0; i < actual; i++)
   {
      close(clients[i].sock);
   }
}


void remove_client(Client *clients, int to_remove, int *actual)
{
   memmove(clients + to_remove, clients + to_remove + 1,
	   (*actual - to_remove - 1) * sizeof(Client));
   /* nombre clients - 1 */
   (*actual)--;
}


void send_message_to_all_clients(Client *clients, Client sender,
				 int actual, const char *buffer, char from_server)
{
   int i;
   char message[BUF_SIZE];
   //message[0] = 0;
   
   for(i = 0; i < actual; i++)
   {
      /* On n'envoie pas le message au client expediteur du message */
      if(sender.sock != clients[i].sock)
      {
         if(from_server == 0)
         {
            strncpy(message, sender.name, BUF_SIZE - 1);
            strncat(message, " : ", sizeof message - strlen(message) - 1);
         }
         strncat(message, buffer, sizeof message - strlen(message) - 1);
         write_client(clients[i].sock, message);
      }
   }
}


void end_connection(int sock)
{
   close(sock);
}


/**********************************************************************************/
/**************************** Messages du serveur *********************************/
/**********************************************************************************/

/*************************** Connexion/Deconnexion ********************************/

/* Signifie au musicien qui a demandee la connexion que celle-ci 
   est accepte sous le nom "user" */
void welcome(int sock, char *user)
{
  char *buffer = (char*) malloc(strlen("WELCOME/")+strlen(user)+3);

  strcat(buffer, "WELCOME/");
  strcat(buffer, user);
  strcat(buffer, "/\n");
  
  write_client(sock, buffer);
}

/* Signifie au musicien que le serveur attend une connexion sur le port audio */
void audio_port(int sock)
{
  write_client(sock, "AUDIO_PORT/2014/\n");
}

/* Signifie que le canal audio est établi */
void audio_ok(int sock, char port)
{
  char *buffer = (char*) malloc(strlen("AUDIO_OK/")+4);

  strcat(buffer, "AUDIO_OK/");
  strcat(buffer, &port);
  strcat(buffer, "/\n");
  
  write_client(sock, buffer);
}

/* Signifie a tous les clients la connexion de "user" */
void connected(Client *clients, Client client,
	       int actual, char from_server, char *user)
{
  char *buffer = (char*) malloc(strlen("CONNECTED/")+strlen(user)+3);

  strcat(buffer, "CONNECTED/");
  strcat(buffer, user);
  strcat(buffer, "/\n");
  
  send_message_to_all_clients(clients, client, actual, buffer, from_server);
}

/* Signifie a tous les clients le depart de "user" */
void exited(Client *clients, Client client,
	       int actual, char from_server, char *user)
{
  char *buffer = (char*) malloc(strlen("EXITED/")+strlen(user)+3);

  strcat(buffer, "EXITED/");
  strcat(buffer, user);
  strcat(buffer, "/\n");
  
  send_message_to_all_clients(clients, client, actual, buffer, from_server);
}

/********************* Gestion des paramètres de Jam ******************************/

/* Signale au client que la session est vide */
void empty_session(int sock)
{
  write_client(sock, "EMPTY_SESSION\n");
}

/* Signale au client les parameetres de la jam */
void current_session(int sock, char *style, char* tempo, char nbMus)
{
  char *buffer = (char*) malloc(strlen("CURRENT_SESSION/")+strlen(style)+strlen(tempo)+6);

  strcat(buffer, "CURRENT_SESSION/");
  strcat(buffer, style);
  strcat(buffer, "/");
  strcat(buffer, tempo);
  strcat(buffer, "/");
  strcat(buffer, &nbMus);
  strcat(buffer, "/\n");
  
  write_client(sock, buffer);
}

/* Signale la bonne réception des parametres */
void ack_opts(int sock)
{
  write_client(sock, "ACK_OPTS\n");
}

/* Signale au client que la session est plein */
void full_session(int sock)
{
  write_client(sock, "FULL_SESSION\n");
}

/**************************** Gestion des flux audios *****************************/

/* Bonne réception du buffer */
void audio_okk(int sock)
{
  write_client(sock, "AUDIO_OK\n");
}

/* Problème de réception */
void audio_ko(int sock)
{
  write_client(sock, "AUDIO_KO\n");
}

/* Buffer contenant le mélange global des autres musiciens */
void audio_mix(int sock, char* buff)
{
  write_client(sock, buff);
}


/**********************************************************************************/
/*********************** Application principale ***********************************/
/**********************************************************************************/

void app(int sock)
{
   char buffer[BUF_SIZE];
   int i, actual = 0, max = sock;
   Client clients[MAX_CLIENTS];

   fd_set rdfs;

   while(1)
   {
     /* Vider l'ensemble rdfs */
     FD_ZERO(&rdfs);
     
     /* ajout de STDIN_FILENO a l'ensemble rdfs */
     FD_SET(STDIN_FILENO, &rdfs);
     
     /* ajout du descripteur de la socket a l'ensemble rdfs */
     FD_SET(sock, &rdfs);
     
     /* ajout du descripteur des sockets des clients connectes */
     for(i = 0; i < actual; i++)
     {
       FD_SET(clients[i].sock, &rdfs);
     }
     
     if(select(max + 1, &rdfs, NULL, NULL, NULL) == -1)
     {
       perror("fonction app : select()\n");
       exit(errno);
     }
     
     /* Quelque chose est tapee sur le clavier on quitte donc la boucle */
     if(FD_ISSET(STDIN_FILENO, &rdfs))
       break;  
     else if(FD_ISSET(sock, &rdfs))
     {
       /* Connexion avec un nouveau client */
       
       struct sockaddr_in csin = { 0 };
       size_t sinsize = sizeof(csin);
       int csock = accept(sock, (struct sockaddr *)&csin, &sinsize);

       printf("Client connectee !\n");
       
       if(csock == -1)
       {
	 perror("fonction app : accept()\n");
	 continue;
       }
	 
       /* after connecting the client sends its name si valeur
	  retournee = -1 la client s'est donc deconnecte */
       if(read_client(csock, buffer) == -1)
	 continue;

       printf("nom du client : %s\n", buffer);
       
       /* Mettre a jour la valeur max */
       max = (csock > max) ? csock : max;

       /* ajouter le nouveau descripteur a l'ensemble rdfs */
       FD_SET(csock, &rdfs);
	 
       Client c = { csock };
       strncpy(c.name, buffer, BUF_SIZE - 1);
       clients[actual] = c;
       actual++;
     }
     else
     {
       /* On verifie si le client tente de communiquer avec le serveur */
       
       for(i = 0; i < actual; i++)
       {
	 /* Un client envoie un message */
	 if(FD_ISSET(clients[i].sock, &rdfs))
	 {
	   Client client = clients[i];
	   int c = read_client(clients[i].sock, buffer);
	   
	   /* Le client s'est deconnecte */
	   if(c == 0)
	   {
	     close(clients[i].sock);
	     remove_client(clients, i, &actual);
	     strncpy(buffer, client.name, BUF_SIZE - 1);
	     strncat(buffer, " disconnected !\n", BUF_SIZE - strlen(buffer) - 1);
	     send_message_to_all_clients(clients, client, actual, buffer, 1);
	   }
	   else
	   {
	     answer_Client(client, buffer, clients, &actual);
	     send_message_to_all_clients(clients, client, actual, buffer, 0);
	   }
	   
	   break;
	 }
       }
     }
   }

   clear_clients(clients, actual);
}


void answer_Client(Client client, char *buffer, Client *clients, int *actual)
{
  int i;
  char *s = (char*) malloc(strlen("CONNECT/")+strlen(client.name)+3);

  strcat(s, "CONNECT/");
  strcat(s, client.name);
  strcat(s, "/\n");

  printf("toto\n");
  
  if(!strcmp(buffer, s)){
    printf("co\n");
    welcome(client.sock, client.name);
    audio_port(client.sock);
    audio_ok(client.sock, (char) 2014);
    connected(clients, client, *actual, 0, client.name);
  }

  s = (char*) malloc(strlen("CONNECT/")+strlen(client.name)+3);

  strcat(s, "EXIT/");
  strcat(s, client.name);
  strcat(s, "/\n");

  if(!strcmp(buffer, s)){
    close(client.sock);
    for(i=0; i<MAX_CLIENTS; i++){
      if(clients[i].sock == client.sock)
	remove_client(clients, i, actual);
    }
    exited(clients, client, *actual, 0, client.name);
  }
}


int main(int argc, char **argv)
{
  int sock;

  sock = init_connection(2013);
  app(sock);
  end_connection(sock);
  
  return EXIT_SUCCESS;
}
