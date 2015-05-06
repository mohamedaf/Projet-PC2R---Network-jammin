/*
 * record.h
 *
 *  Created on: 16 avr. 2015
 *      Author: eleve
 */

#ifndef RECORD_H_
#define RECORD_H_

#include <stdio.h>
#include <stdlib.h>
#include <sndfile.h>
#include <string.h>

#define SAMPLE_RATE (48000)
#define FRAMES_PER_BUFFER (512)
#define NUM_SECONDS (2)
#define NUM_CHANNELS (2)
#define DITHER_FLAG (0)

#define WRITE_TO_FILE (0)

/* Select sample format. */
#if 1
#define PA_SAMPLE_TYPE paFloat32
typedef float SAMPLE;
#define SAMPLE_SILENCE (0.0f)
#define PRINTF_S_FORMAT "%.8f"
#elif 1
#define PA_SAMPLE_TYPE paInt16
typedef short SAMPLE;
#define SAMPLE_SILENCE (0)
#define PRINTF_S_FORMAT "%d"
#elif 0
#define PA_SAMPLE_TYPE paInt8
typedef char SAMPLE;
#define SAMPLE_SILENCE (0)
#define PRINTF_S_FORMAT "%d"
#else
#define PA_SAMPLE_TYPE paUInt8
typedef unsigned char SAMPLE;
#define SAMPLE_SILENCE (128)
#define PRINTF_S_FORMAT "%d"
#endif

#define WARN(...) do { fprintf(stderr, __VA_ARGS__); } while (0)
#define DIE(...) do { WARN(__VA_ARGS__); exit(EXIT_FAILURE); } while (0)
#define PA_ENSURE(...) do { if ((err = __VA_ARGS__) != paNoError) {WARN("PortAudio error %d: %s\n", err, Pa_GetErrorText(err));goto error;}}while (0)


typedef struct {
	int frameIndex; /* Index into sample array. */
	int maxFrameIndex;
	SAMPLE *recordedSamples;
} paTestData;

/* Charge un fichier audio sous la forme d'un tableau d'entiers */
int file_to_float(char* file);

#endif /* RECORD_H_ */
