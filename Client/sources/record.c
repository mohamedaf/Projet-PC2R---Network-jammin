#include <stdio.h>
#include <stdlib.h>
#include <sndfile.h>
#include <string.h>
#include "record.h"


int file_to_float(char* file) {
	SF_INFO sfi;
	SNDFILE *sf;

	sf_count_t nread;
	const size_t nframes = 512;
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

	/* Allocate buffer */
	if ((buf = malloc(nframes * sfi.channels * sizeof(float))) == NULL)
		DIE("Could not malloc.");

	/* Write file data to stream */
	while ((nread = sf_readf_float(sf, buf, nframes)) > 0) {

	}

	sf_close(sf);
	free(buf);

	return 1;
}

