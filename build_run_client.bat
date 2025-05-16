@echo off
cd /d %~dp0

:: Creazione della lista dei sorgenti
dir /b /s src\*.java > sources.txt

:: Compilazione
javac -cp "lib\gson-2.8.9.jar" -d out @sources.txt

:: Creazione del JAR
jar --create --file client.jar --main-class=client.ClientMain -C out .

:: Aggiunta delle configurazioni
jar --update --file client.jar -C config client.cfg -C resources .

:: Esecuzione del client
java -cp "client.jar;lib\gson-2.8.9.jar" client.ClientMain
