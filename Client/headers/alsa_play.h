/*
 * alsa_play.h
 *
 *  Created on: 17 avr. 2015
 *      Author: eleve
 */

#ifndef ALSA_PLAY_H_
#define ALSA_PLAY_H_

#include <alsa/asoundlib.h>
#include "client.h"

typedef struct {
	char* fileName;
	SOCKET* sock;
} alsa_file_infos;

/* Genere et lit des valeurs aleatoires sur
 *  la sortie de la carte son */
int alsa_play(void);

/* Lit un fichier audio */
int alsa_play_from_file(char * file);

/* Thread capturant, lisant et envoyant le sons depuis
 *  l'entree par defaut de la carte son */
void* thread_capture_audio(void* args);

/* Thread qui lit un fichier audio */
void* thread_read_audio_file(void* args);

/* Fonction qui initialise le playback audio */
snd_pcm_t* init_stream_playback();

/* Fonction permettant d'envoyer un buffer a la sortie de la carte son */
void write_to_stream_playback(snd_pcm_t *handle, short* buf, size_t nframes);

/* Thread qui lit un fichier audio, et envoie les buffers lus au serveur.
 * Peut etre utilise en remplacement de thread_capture_audio*/
void* thread_play_file_and_send(void* args);

/* Initialise la structure necessaire au bon fonctionnement du thread_play_file_and_send */
alsa_file_infos* new_alsa_infos(SOCKET* send_sock, char* fileName);

#endif /* HEADERS_ALSA_PLAY_H_ */
