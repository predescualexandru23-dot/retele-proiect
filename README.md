# FileShare - Aplicație distribuită de partajare fișiere

Aplicație client-server pentru partajarea fișierelor între mai mulți clienți conectați simultan.
Serverul intermediază comunicarea și transferul fișierelor fără a le stoca permanent.


## Tehnologii folosite
- Java 21
- Socket-uri TCP
- Java Swing (GUI)
- Docker

## Arhitectură
- **Server** - rulează în Docker, acceptă conexiuni multiple simultan (un thread per client)
- **Client** - interfață grafică cu drag and drop, monitorizare automată a directorului local

## Protocol
Comunicarea se face printr-un protocol binar custom (DataInputStream/DataOutputStream).
Mesajele text sunt prefixate cu lungimea (4 bytes), urmate de conținut UTF-8.
Fișierele binare folosesc același format: prefix de lungime urmat de bytes bruti.

| Mesaj | Direcție | Descriere |
|-------|----------|-----------|
| `AUTH\|user\|parola` | Client → Server | Autentificare |
| `AUTH_OK` / `AUTH_NEW_OK` | Server → Client | Autentificare reușită |
| `AUTH_FAIL\|motiv` | Server → Client | Autentificare eșuată |
| `PUBLISH\|fisier1,fisier2` | Client → Server | Publică lista de fișiere |
| `FILE_LIST\|user:f1,f2;user2:f3` | Server → Client | Lista agregată |
| `NEW_CLIENT\|user\|fisiere` | Server → Clienți | Notificare client nou |
| `CLIENT_LEFT\|user` | Server → Clienți | Notificare deconectare |
| `FILE_ADD\|user\|fisier` | Server → Clienți | Fișier nou adăugat |
| `FILE_DEL\|user\|fisier` | Server → Clienți | Fișier șters |
| `DOWNLOAD\|user\|fisier` | Client → Server | Cerere descărcare |
| `SEND_FILE\|fisier` | Server → Client | Cerere trimitere fișier |
| `FILE_START\|fisier` | Server → Client | Începe recepția |
| `DISCONNECT` | Client → Server | Deconectare |

## Structură proiect

FileShare/
├── server/
│   ├── src/
│   │   ├── Server.java
│   │   ├── ClientHandler.java
│   │   ├── FileRegistry.java
│   │   └── UserManager.java
│   └── Dockerfile
├── client/
│   └── src/
│       ├── AppWindow.java
│       ├── LoginPanel.java
│       ├── MainPanel.java
│       ├── Client.java
│       └── DirectoryWatcher.java
├── docker-compose.yml
└── README.md

## Rulare

### Cerințe
- Docker Desktop instalat și pornit
- Java 21+ (pentru client)

### Pornire server
```bash
docker compose up --build
```

Serverul pornește pe portul **5000**.

### Pornire client
Deschide proiectul în IntelliJ IDEA și rulează `AppWindow.java` din modulul `client`.

Se pot rula mai mulți clienți simultan — în IntelliJ mergi la:
**Edit Configurations → AppWindow → Allow multiple instances** ✓

### Oprire server
```bash
docker compose down
```

## Funcționalități
- Autentificare cu parolă hash-uită (SHA-256)
- Publicarea automată a fișierelor la conectare
- Notificări în timp real la conectare/deconectare
- Monitorizare automată a directorului local (adăugare/ștergere)
- Descărcare fișiere prin server (suport binar — imagini, PDF, arhive etc.)
- Interfață grafică cu drag and drop
- Server concurent, suporta mai mulți clienți simultan

## Tratarea erorilor
- Deconectare bruscă a clientului - serverul curăță resursele automat
- Timeout la transfer (15 secunde)
- Parola greșită - mesaj clar în interfață
- Fișier inexistent la download - mesaj de eroare

## Autori
- Predescu Alexandru
- Popescu Andrei Mihail
- Poenariu Viorel Gabriel