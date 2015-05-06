#include <sndfile.h>
#include <alsa/asoundlib.h>
#include <pthread.h>
#include <pthread.h>
#include "alsa_play.h"
#include "client.h"
#include "record.h"
#include "audio_client.h"

extern int run_capture;
extern long audio_buffer_size;
int run_capture;
extern pthread_mutex_t running_capture_mutex;
extern long tick;

static char *device = "default"; /* playback device */
snd_output_t *output = NULL;
unsigned char buffer[16 * 1024];

int alsa_play(void) {
	int err;
	unsigned int i;
	snd_pcm_t *handle;
	snd_pcm_sframes_t frames;
	for (i = 0; i < sizeof(buffer); i++)
		buffer[i] = random() & 0xff;
	if ((err = snd_pcm_open(&handle, device, SND_PCM_STREAM_PLAYBACK, 0)) < 0) {
		printf("Playback open error: %s\n", snd_strerror(err));
		exit(EXIT_FAILURE);
	}
	if ((err = snd_pcm_set_params(handle, SND_PCM_FORMAT_U8,
			SND_PCM_ACCESS_RW_INTERLEAVED, 1, 48000, 1, 500000)) < 0) { /* 0.5sec */
		printf("Playback open error: %s\n", snd_strerror(err));
		exit(EXIT_FAILURE);
	}
	for (i = 0; i < 16; i++) {
		frames = snd_pcm_writei(handle, buffer, sizeof(buffer));
		if (frames < 0)
			frames = snd_pcm_recover(handle, frames, 0);
		if (frames < 0) {
			printf("snd_pcm_writei failed: %s\n", snd_strerror(err));
			break;
		}
		if (frames > 0 && frames < (long) sizeof(buffer))
			printf("Short write (expected %li, wrote %li)\n",
					(long) sizeof(buffer), frames);
	}
	snd_pcm_close(handle);
	return 0;
}

int alsa_play_from_file(char * file) {
	int err;
	unsigned int i;
	snd_pcm_t *handle;
	snd_pcm_sframes_t frames;

	SF_INFO sfi;
	SNDFILE *sf;

	sf_count_t nread;

	float *buf;

	/* Open the input file */
	memset(&sfi, 0, sizeof(sfi));
	if ((sf = sf_open(file, SFM_READ, &sfi)) == NULL)
		DIE("Could not open \"%s\".\n", file);

	/* Infos */
	printf("Channels : %d\n", sfi.channels);
	printf("Format : %d\n", sfi.format);
	printf("Samplerate : %d\n", sfi.samplerate);
	printf("Sections : %d\n", sfi.sections);
	printf("Seekable : %d\n", sfi.seekable);
	printf("Frames : %d\n", sfi.frames);

	const size_t nframes = sfi.samplerate;

	/* Allocate buffer */
	if ((buf = malloc(nframes * sfi.channels * sizeof(float))) == NULL)
		DIE("Could not malloc.");

	for (i = 0; i < sizeof(buffer); i++)
		buffer[i] = random() & 0xff;
	if ((err = snd_pcm_open(&handle, device, SND_PCM_STREAM_PLAYBACK, 0)) < 0) {
		printf("Playback open error: %s\n", snd_strerror(err));
		exit(EXIT_FAILURE);
	}
	if ((err = snd_pcm_set_params(handle, SND_PCM_FORMAT_FLOAT_LE,
			SND_PCM_ACCESS_RW_INTERLEAVED, sfi.channels, sfi.samplerate, 1,
			500000)) < 0) { /* 0.5sec */
		printf("Playback open error: %s\n", snd_strerror(err));
		exit(EXIT_FAILURE);
	}

	snd_pcm_prepare(handle);

	while ((nread = sf_readf_float(sf, buf, nframes)) > 0) {
		frames = snd_pcm_writei(handle, buf, nframes);
		if (frames < 0)
			frames = snd_pcm_recover(handle, frames, 0);
		if (frames < 0) {
			printf("snd_pcm_writei failed: %s\n", snd_strerror(err));
			break;
		}
		if (frames > 0 && frames < nframes)
			printf("Short write (expected %i, wrote %li)\n", nframes, frames);
	}
	sf_close(sf);
	free(buf);

	snd_pcm_close(handle);

	return 0;
}

void* thread_read_audio_file(void* args) {
	char* file = args;
	int ret = 0;
	alsa_play_from_file(file);
	pthread_exit(&ret);
}

void* thread_capture_audio(void* args) {
	SOCKET* sock = args;

	char* dev = "default";
	int i, buffer_division = 100;
	int err, ret = 0;
	int buffer_frames = audio_buffer_size;
	size_t mini_buffer_size = buffer_frames / buffer_division;
	short *buffer = NULL;
	short* minibuffer = NULL;
	printf("Buffer frames : %d.\n", buffer_frames);
	unsigned int rate = 48000;
	snd_pcm_t *capture_handle;
	snd_pcm_t* playback_handle = init_stream_playback();
	snd_pcm_hw_params_t *hw_params;
	snd_pcm_format_t format = SND_PCM_FORMAT_S16_LE;

	if ((err = snd_pcm_open(&capture_handle, dev, SND_PCM_STREAM_CAPTURE, 0))
			< 0) {
		fprintf(stderr, "cannot open audio device %s (%s)\n", dev,
				snd_strerror(err));
		exit(1);
	}

	fprintf(stdout, "audio interface opened\n");

	if ((err = snd_pcm_hw_params_malloc(&hw_params)) < 0) {
		fprintf(stderr, "cannot allocate hardware parameter structure (%s)\n",
				snd_strerror(err));
		exit(1);
	}

	fprintf(stdout, "hw_params allocated\n");

	if ((err = snd_pcm_hw_params_any(capture_handle, hw_params)) < 0) {
		fprintf(stderr, "cannot initialize hardware parameter structure (%s)\n",
				snd_strerror(err));
		exit(1);
	}

	fprintf(stdout, "hw_params initialized\n");

	if ((err = snd_pcm_hw_params_set_access(capture_handle, hw_params,
			SND_PCM_ACCESS_RW_INTERLEAVED)) < 0) {
		fprintf(stderr, "cannot set access type (%s)\n", snd_strerror(err));
		exit(1);
	}

	fprintf(stdout, "hw_params access setted\n");

	if ((err = snd_pcm_hw_params_set_format(capture_handle, hw_params, format))
			< 0) {
		fprintf(stderr, "cannot set sample format (%s)\n", snd_strerror(err));
		exit(1);
	}

	fprintf(stdout, "hw_params format setted\n");

	if ((err = snd_pcm_hw_params_set_rate_near(capture_handle, hw_params, &rate,
			0)) < 0) {
		fprintf(stderr, "cannot set sample rate (%s)\n", snd_strerror(err));
		exit(1);
	}

	fprintf(stdout, "hw_params rate setted\n");

	if ((err = snd_pcm_hw_params_set_channels(capture_handle, hw_params, 2))
			< 0) {
		fprintf(stderr, "cannot set channel count (%s)\n", snd_strerror(err));
		exit(1);
	}

	fprintf(stdout, "hw_params channels setted\n");

	if ((err = snd_pcm_hw_params(capture_handle, hw_params)) < 0) {
		fprintf(stderr, "cannot set parameters (%s)\n", snd_strerror(err));
		exit(1);
	}

	fprintf(stdout, "hw_params setted\n");

	snd_pcm_hw_params_free(hw_params);

	fprintf(stdout, "hw_params freed\n");

	if ((err = snd_pcm_prepare(capture_handle)) < 0) {
		fprintf(stderr, "cannot prepare audio interface for use (%s)\n",
				snd_strerror(err));
		exit(1);
	}

	fprintf(stdout, "audio interface prepared\n");

	buffer = malloc(buffer_frames * snd_pcm_format_width(format) / 8 * 2);
	minibuffer = malloc(
			mini_buffer_size * snd_pcm_format_width(format) / 8 * 2);

	fprintf(stdout, "buffer allocated\n");

	while (1) {
		pthread_mutex_lock(&running_capture_mutex);
		if (run_capture == 0)
			break;
		pthread_mutex_unlock(&running_capture_mutex);

		//printf("avant\n");
		for (i = 0; i < buffer_division; i++) {
			//printf("pendant %d\n", i);
			if ((err = snd_pcm_readi(capture_handle, minibuffer,
					mini_buffer_size)) != mini_buffer_size) {
				fprintf(stderr, "read from audio interface failed (%s)\n",
						snd_strerror(err));
				exit(1);
			}
			/* On envoie vers la sortie de la carte son */
			write_to_stream_playback(playback_handle, minibuffer,
					mini_buffer_size);

			//printf("avant memcpy %d\n", i);
			memcpy(buffer + (i * mini_buffer_size), minibuffer,
					mini_buffer_size);

		}
		//printf("apres\n");

		/* On envoie le buffer au serveur */
		audio_send_infos* a = new_send_infos(sock,buffer,buffer_frames,tick++);
		pthread_create(NULL, NULL, thread_send_audio_buffer, (void*) a);
		//send_audio_buffer(sock, buffer, buffer_frames, tick++);
		printf("envoye %li\n", tick);
	}

	free(buffer);
	free(minibuffer);

	fprintf(stdout, "buffer freed\n");

	snd_pcm_close(capture_handle);
	snd_pcm_close(playback_handle);
	fprintf(stdout, "audio interface closed\n");

	pthread_exit(&ret);
}

snd_pcm_t* init_stream_playback(unsigned int frame_rate) {
	int err, dir = 0;
	snd_pcm_t *playback_handle;
	snd_pcm_hw_params_t *hw_params;

	if ((err = snd_pcm_open(&playback_handle, "default",
			SND_PCM_STREAM_PLAYBACK, 0)) < 0) {
		fprintf(stderr, "cannot open audio device %s (%s)\n", "default",
				snd_strerror(err));
		return NULL;
	}

	if ((err = snd_pcm_hw_params_malloc(&hw_params)) < 0) {
		fprintf(stderr, "cannot allocate hardware parameter structure (%s)\n",
				snd_strerror(err));
		return NULL;
	}

	if ((err = snd_pcm_hw_params_any(playback_handle, hw_params)) < 0) {
		fprintf(stderr, "cannot initialize hardware parameter structure (%s)\n",
				snd_strerror(err));
		return NULL;
	}

	if ((err = snd_pcm_hw_params_set_access(playback_handle, hw_params,
			SND_PCM_ACCESS_RW_INTERLEAVED)) < 0) {
		fprintf(stderr, "cannot set access type (%s)\n", snd_strerror(err));
		return NULL;
	}

	if ((err = snd_pcm_hw_params_set_format(playback_handle, hw_params,
			SND_PCM_FORMAT_S16_LE)) < 0) {
		fprintf(stderr, "cannot set sample format (%s)\n", snd_strerror(err));
		return NULL;
	}

	if ((err = snd_pcm_hw_params_set_rate_near(playback_handle, hw_params,
			&frame_rate, &dir)) < 0) {
		fprintf(stderr, "cannot set sample rate (%s)\n", snd_strerror(err));
		return NULL;
	}

	if ((err = snd_pcm_hw_params_set_channels(playback_handle, hw_params, 2))
			< 0) {
		fprintf(stderr, "cannot set channel count (%s)\n", snd_strerror(err));
		return NULL;
	}

	if ((err = snd_pcm_hw_params(playback_handle, hw_params)) < 0) {
		fprintf(stderr, "cannot set parameters (%s)\n", snd_strerror(err));
		return NULL;
	}

	snd_pcm_prepare(playback_handle);

	snd_pcm_hw_params_free(hw_params);

	if ((err = snd_pcm_prepare(playback_handle)) < 0) {
		fprintf(stderr, "cannot prepare audio interface for use (%s)\n",
				snd_strerror(err));
		return NULL;
	}
	return playback_handle;
}

void write_to_stream_playback(snd_pcm_t *handle, short* buf, size_t nframes) {
	long int frames;
	frames = snd_pcm_writei(handle, buf, nframes);
	if (frames < 0)
		frames = snd_pcm_recover(handle, frames, 0);
	if (frames < 0) {
		/*printf("snd_pcm_writei failed: %s\n", snd_strerror(frames));
		 return;*/
	}
	if (frames > 0 && frames < nframes)
		printf("Short write (expected %i, wrote %li)\n", nframes, frames);
}

void* thread_play_file_and_send(void* args) {
	alsa_file_infos* inf = args;
	SOCKET* sock = inf->sock;
	char* file = inf->fileName;
	int err;
	unsigned int i, j;
	snd_pcm_t *handle;
	snd_pcm_sframes_t frames;

	SF_INFO sfi;
	SNDFILE *sf;
	sf_count_t nread;
	pthread_t thread_t;

	short *buf;

	/* Open the input file */
	memset(&sfi, 0, sizeof(sfi));
	if ((sf = sf_open(file, SFM_READ, &sfi)) == NULL)
		DIE("Could not open \"%s\".\n", file);

	/* Infos */
	printf("Channels : %d\n", sfi.channels);
	printf("Format : %d\n", sfi.format);
	printf("Samplerate : %d\n", sfi.samplerate);
	printf("Sections : %d\n", sfi.sections);
	printf("Seekable : %d\n", sfi.seekable);
	printf("Frames : %d\n", sfi.frames);

	const size_t nframes = sfi.samplerate;

	/* Allocate buffer */
	if ((buf = malloc(nframes * sfi.channels * sizeof(short))) == NULL)
		DIE("Could not malloc.");

	for (i = 0; i < sizeof(buffer); i++)
		buffer[i] = random() & 0xff;
	if ((err = snd_pcm_open(&handle, device, SND_PCM_STREAM_PLAYBACK, 0)) < 0) {
		printf("Playback open error: %s\n", snd_strerror(err));
		exit(EXIT_FAILURE);
	}
	if ((err = snd_pcm_set_params(handle, SND_PCM_FORMAT_S16_LE,
			SND_PCM_ACCESS_RW_INTERLEAVED, sfi.channels, sfi.samplerate, 1,
			500000)) < 0) { /* 0.5sec */
		printf("Playback open error: %s\n", snd_strerror(err));
		exit(EXIT_FAILURE);
	}

	snd_pcm_prepare(handle);

	while ((nread = sf_readf_short(sf, buf, nframes)) > 0) {
		frames = snd_pcm_writei(handle, buf, nframes);

		audio_send_infos* a = new_send_infos(sock,buf,nframes,tick++);
		pthread_create(&thread_t, NULL, thread_send_audio_buffer, (void*) a);
		printf("Tick %d sent\n",tick);
		//send_audio_buffer(sock, buf, nframes, tick++);

		if (frames < 0)
			frames = snd_pcm_recover(handle, frames, 0);
		if (frames < 0) {
			printf("snd_pcm_writei failed: %s\n", snd_strerror(err));
			break;
		}
		if (frames > 0 && frames < nframes)
			printf("Short write (expected %i, wrote %li)\n", nframes, frames);
	}
	sf_close(sf);

	free(buf);
	snd_pcm_close(handle);
	pthread_exit(&err);
}

alsa_file_infos* new_alsa_infos(SOCKET* send_sock, char* fileName) {
	alsa_file_infos* ret = malloc(sizeof(alsa_file_infos));
	ret->fileName = fileName;
	ret->sock = send_sock;
	return ret;
}
