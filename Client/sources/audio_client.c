#include <stdio.h>
#include <pthread.h>
#include "client.h"
#include "audio_client.h"
#include "alsa_play.h"

int run;
int run_capture, begin;
pthread_mutex_t running_mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t running_capture_mutex = PTHREAD_MUTEX_INITIALIZER;
long audio_buffer_size;
long tick;

void* thread_audio_client(void* args) {
	audio_socket_infos* infos = args;
	printf("Connecting on audio port : %d.\n", infos->port);
	SOCKET sock = init_connection(infos->address, infos->port);
	pthread_t thread_reception;
	pthread_t thread_capture;
	int ret = 0;
	tick = infos->first_tick;
	char* file = infos->fileName;
	begin = 1;
	/* Taille des buffers qu'on envoie et recoit, correspond a la duree d'un tick */
	audio_buffer_size = (60 / infos->tempo) * FRAMERATE
	;
	alsa_file_infos* alsa_infos = NULL;

	pthread_mutex_lock(&running_mutex);
	run = 1;
	pthread_mutex_unlock(&running_mutex);
	pthread_mutex_lock(&running_capture_mutex);
	run_capture = 1;

	/* Si il s'agit du premier client, on demarre l'enregistrement tout de suite.
	 * Sinon, on attend de recevoir le mix du serveur, pour pouvoir jouer en meme temps */
	if (infos->client_originel) {
		begin = 0;
		pthread_mutex_unlock(&running_capture_mutex);
	}

	if (file == NULL) {
		/* Demarrer le thread de capture et envoi audio si mode non spectateur */
		pthread_create(&thread_capture, NULL, thread_capture_audio,
				(void*) &sock);
	} else {
		alsa_infos = new_alsa_infos(&sock, file);
		pthread_create(&thread_capture, NULL, thread_play_file_and_send,
				(void*) alsa_infos);
	}
	/* Demarrer la reception du AUDIO_MIX */
	//pthread_create(&thread_reception, NULL, thread_read_audio, (void*) &sock);
	pthread_join(thread_capture, NULL);
	//pthread_join(thread_reception, NULL);

	end_connection(sock);
	printf("Audio connection ended.\n");
	pthread_exit(&ret);
}

void* thread_read_audio(void *args) {
	SOCKET* sock = args;
	int n = 0;
	int ret = 0;
	size_t sss = 7 * audio_buffer_size + 128;
	sss = 1000000;
	char buffer[sss];
	int cpt;

	short* received_audio_buffer = NULL;

	snd_pcm_t* player = init_stream_playback();

	while (1) {
		pthread_mutex_lock(&running_mutex);
		if (run == 0)
			break;
		pthread_mutex_unlock(&running_mutex);

		if ((n = recv(*sock, buffer, sss - 1, 0)) <= 0) {
			perror("recv()");
			perror("Disconnected from audio server");
			ret = 1;
			break;
		}
		buffer[n] = 0;
		received_audio_buffer = analyse_received_audio(buffer, *sock, &cpt);

		if (received_audio_buffer == NULL)
			continue;

		/* On demarre la capture audio */
		if (begin) {
			pthread_mutex_unlock(&running_capture_mutex);
			begin = 0;
		}

		/* On envoie ce qu'on a recu dans la sortie de la carte son */
		write_to_stream_playback(player, received_audio_buffer, cpt);
	}
	pthread_exit(&ret);
}

short* analyse_received_audio(char* buf, SOCKET sock, int* cpt) {

	char* token = strtok(buf, "/");
	char* nb;
	short* audio_buffer = malloc(48000);
	*cpt = 0;
	if (strcmp(token, "AUDIO_MIX") == 0) {
		/* Decoupe et analyse du buffer recu */
		while ((nb = strtok(NULL, " \t")) != NULL && *cpt < 1024) {
			audio_buffer[*cpt] = atoi(nb);
			(*cpt)++;
		}
	}

	return audio_buffer;
}

audio_socket_infos* new_audio_socket_infos(const char* address, int port,
		int tempo, int client_originel, long first_tick, char* fileName) {
	audio_socket_infos* ret = malloc(sizeof(audio_socket_infos));
	ret->address = address;
	ret->port = port;
	ret->tempo = tempo;
	ret->client_originel = client_originel;
	ret->fileName = fileName;
	return ret;
}

void stop_audio_client() {
	pthread_mutex_lock(&running_mutex);
	run = 0;
	pthread_mutex_unlock(&running_mutex);
	pthread_mutex_lock(&running_capture_mutex);
	run_capture = 0;
	pthread_mutex_unlock(&running_capture_mutex);
}

void* thread_send_audio_buffer(void* args) {
	int ret = 0;
	audio_send_infos* i = args;
	SOCKET * sock = i->sock;
	short* audio_buffer = i->audio_buffer;
	size_t size = i->size;
	long tick = i->tick;

	char* to_send = int_array_to_char_array(audio_buffer, size);
	size_t sss = 7 * size + 128;
	char* str = malloc(sizeof(char) * sss);
	memset(str, '\0', sss);
	sprintf(str, "AUDIO_CHUNK/%li/%s/", tick, to_send);
	write_server(*sock, str);
	free(to_send);
	free(str);
	pthread_exit(&ret);
}

char* int_array_to_char_array(short* int_array, size_t size) {
	int i;
	char temp[10];
	/* On multiplie par 7 car la valeur minimale d'un short est -32767,
	 *  soit 6 caracteres, plus un espace entre chaque nombre */

	size_t sss = 7 * size + 128;

	char* str = malloc(sizeof(char) * sss);

	memset(str, '\0', sss);

	for (i = 0; i < size; ++i) {
		sprintf(temp, "%d ", int_array[i]);
		strcat(str, temp);
	}

	return str;
}

audio_send_infos* new_send_infos(SOCKET* sock, short* audio_buffer, size_t size,
		long tick) {
	audio_send_infos* ret = malloc(sizeof(audio_send_infos));
	ret->sock = sock;
	ret->audio_buffer = audio_buffer;
	ret->size = size;
	ret->tick = tick;
	return ret;
}

