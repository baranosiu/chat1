# Zadanie chatv1

Klasa serwera `local.pbaranowski.chat.server.Application`

Parametry uruchomienia: `port` (domyślnie `9000`)

Klasa klienta `local.pbaranowski.chat.client.Application`

Parametry uruchomienia: `host` `port` (domyślnie `127.0.0.1 9000`)


Ewentualnie po zbudowaniu paczki jar za pomocą `mvn package` można uruchomić przez:

Server: `java -jar target/czat-{version}-jar-with-dependencies.jar server [args]`

Klient: `java -jar target/czat-{version}-jar-with-dependencies.jar [args]`
