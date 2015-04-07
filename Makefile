GCC_FLAGS = -Wall
all: serveur

serveur.o	: serveur.c
	gcc $(GCC_FLAGS) -c serveur.c serveur.h

serveur	: serveur.o 
	gcc $(GCC_FLAGS) -o serveur serveur.o

clean	:
	rm -f *.o *.h.gch serveur
