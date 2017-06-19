package de.hawHH.RB.TCPTest;

import java.io.IOException;
import java.net.*;

public class Server {

    private static Race race;

    public static void main (String[] args) {
        race = new Race();
        Thread raceThread = new Thread(race);
        raceThread.start();

        ServerSocket welcomeSocket = null;
        try {
            welcomeSocket = new ServerSocket(5000);

            // wait for connections
            while (true) {
                Socket connectionSocket = welcomeSocket.accept();
                new Client(connectionSocket,race);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}