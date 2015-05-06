/*
 * audio_client.h
 *
 *  Created on: 17 avr. 2015
 *      Author: eleve
 */

#ifndef AUDIO_CLIENT_H_
#define AUDIO_CLIENT_H_

#include <stdio.h>
#include <pthread.h>
#include "client.h"

#define FRAMERATE 48000;

typedef struct {
	const char* address;
	int port;
	int tempo;
	int client_originel;
	long first_tick;
	char* fileName;
} audio_socket_infos;

typedef struct {
	SOCKET* sock;
	short* audio_buffer;
	size_t size;
	long tick;
} audio_send_infos;

/* Thread gerant la partie audio */
void* thread_audio_client(void* args);

/* Thread recevant les donnees du serveur */
void* thread_read_audio(void *args);

/* Fonction qui analyse et renvoie le buffer recu converti en entiers */
short* analyse_received_audio(char* buf, SOCKET sock, int* cpt);

/* Initialise la structure envoyee au  thread_audio_client */
audio_socket_infos* new_audio_socket_infos(const char* address, int port,
		int tempo, int client_originel, long first_tick, char* fileName);

/* Stoppe le client audio */
void stop_audio_client();

/* Thread permettant d'envoyer un buffer audio au serveur */
void* thread_send_audio_buffer(void* args);

/* Convertit un buffer d'entiers en buffer de string */
char* int_array_to_char_array(short* int_array, size_t size);

/* Initialise la structure envoyee au  thread_send_audio_buffer */
audio_send_infos* new_send_infos(SOCKET* sock, short* audio_buffer, size_t size,
		long tick);

#endif /* HEADERS_AUDIO_CLIENT_H_ */
