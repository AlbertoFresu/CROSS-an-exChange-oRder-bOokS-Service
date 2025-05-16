#!/bin/bash
cd "$(dirname "$0")"

# Creazione della lista dei sorgenti
find src -name "*.java" > sources.txt

# Compilazione
javac -cp "lib/gson-2.8.9.jar" -d out @sources.txt

# Creazione del JAR
jar --create --file server.jar --main-class=server.ServerMain -C out .

# Aggiunta delle configurazioni
jar --update --file server.jar -C config server.cfg -C resources .

# Esecuzione del server
java -cp "server.jar:lib/gson-2.8.9.jar" server.ServerMain
