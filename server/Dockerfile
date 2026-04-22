# Folosim o imagine Java 21 slim — destul de mica dar completa
FROM eclipse-temurin:21-jdk

# Cream un folder pentru aplicatie in container
WORKDIR /app

# Copiem toate fisierele .java din server/src
COPY src/ ./src/

# Cream folderul pentru output
RUN mkdir -p out

# Compilam toate fisierele Java
RUN javac -d out src/*.java

# Cream folderul pentru useri si fisiere
RUN mkdir -p shares

# Portul pe care asculta serverul
EXPOSE 5000

# Comanda de pornire
CMD ["java", "-cp", "out", "Server"]