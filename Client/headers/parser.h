/*
 * parse.h
 *
 *  Created on: 16 avr. 2015
 *      Author: eleve
 */

#ifndef PARSER_H_
#define PARSER_H_

/* Fonction d'affichage pour le log, a modifier si on veut changer la sortie */
void my_log(char* msg);
/* Analayse les messages recus du serveur */
int parse_line_by_line(char* str, int* tempo, long* first_tick);
/* Analayse un message recu du serveur */
int parse(char* tab, int* tempo, long* first_tick);
/* Analyse les commandes entrees dans le terminal a destination du serveur */
int parse_output(char* msg);
/* Concatene des chaines de caractere */
char* concat_strings(char* str1, char* str2);

#endif /* PARSER_H_ */
