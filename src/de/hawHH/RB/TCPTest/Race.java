package de.hawHH.RB.TCPTest;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Thread.sleep;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Created by RB on 11.06.17.
 */
public class Race implements Runnable {

    private Set<Client> clients;
    private Map<String, Client> carMap;
    private Map<String, Integer> carTimes;
    private Map<String, Integer> carRanks;
    private State state;

    private LocalTime registrationStart;
    private int minRoundTime;
    private String minRoundTimeId;
    private int ranking;
    private String leaderboard;


    public Race() {
        this.resetRace();
    }


    /**
     * initialize/reset state of this thread
     */
    private void resetRace() {
        this.carMap = new HashMap<>();
        this.carTimes = new HashMap<>();
        this.carRanks = new HashMap<>();
        this.clients = new HashSet<>();
        this.state = State.INIT;
        this.minRoundTime = Integer.MAX_VALUE;
        this.minRoundTimeId = "";
        this.ranking = 1;
        this.leaderboard = "Leaderboard:\n";
    }

    @Override
    public void run() {
        while(true){
            state = State.ACCEPT;
            registrationStart = LocalTime.now();
            System.out.println("Die Registrierung für das Rennen ist gestartet (" + registrationStart + ")");
            try {
                sleep(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            state = State.RACESTART;
            if(this.carMap.size() > 1) {
                this.startRace();
            }else if(this.carMap.size() == 1) {
                System.out.println("Es wurden nicht genug Menschen für das Rennen angemeldet. Neuer Versuch");
                for(Client c : this.clients){
                    c.sendMessage("Du hast gewonnen. Du bist der Einzige gewesen");
                    c.closeConnection();
                }
                cancelRace();
            }else {
                System.out.println("Es wurden nicht genug Menschen für das Rennen angemeldet. Neuer Versuch");
                cancelRace();
            }
        }
    }

    /**
     * starting procedure of the race
     *
     */
    private void startRace() {
        state = state.RACERUNNING;
        this.broadcast("Das Rennen startet jetzt.\n");
        System.out.println("Das Rennen ist gestartet. Anmeldungen:");
        for (String s: carMap.keySet()) {
            System.out.println(s);
            this.carRanks.put(s,0);
        }
        System.out.println("------------------------------------");


        while(carTimes.size() > 0){
            calculateSmallestRoundTime();

            try {
                sleep(minRoundTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            carMap.get(minRoundTimeId).sendMessage("Dein Auto " + minRoundTimeId + " kam als Nummer " + ranking + " ins Ziel!\n");
            System.out.println("Rang " + ranking + " | " + minRoundTimeId);
            this.carRanks.put(minRoundTimeId,ranking);
            this.leaderboard += "Rang " + ranking + " | " + minRoundTimeId + "\n";
            this.ranking++;
        }

        endRace();
    }

    /**
     * ending procedure of the race
     *
     */
    private void endRace() {
        state = state.RACEEND;
        System.out.println("---------------------------");

        this.leaderboard += "\nDruecke Enter um fortzufahren\n";

        for(Client c : clients) {
            c.sendMessage(leaderboard);
            c.reset();
        }

        resetRace();
    }

    /**
     * cancelling of the race. (when not enough cars are in the race, only one car)
     */
    private void cancelRace() {
        state = state.INIT;
        for(Client c : clients) {
            c.closeConnection();
        }
        resetRace();
    }


    /**
     * add a client to the List of Clients
     *
     */
    public synchronized boolean registerCar(Client client, String id, String carname) {
        if(state.equals(State.INIT) || state.equals(State.ACCEPT)){
            this.carMap.put(id, client);
            this.clients.add(client);
            client.sendMessage("Sie haben sich erfolgreich registriert: " + id + "\n");

            int randomtime = (int) (Math.random() * (30000-10000) + 10000);
            carTimes.put(id, randomtime);

            LocalTime now = LocalTime.now();
            long diff = registrationStart.until(now, SECONDS);
            long remainingTime = 30 - diff;
            client.sendMessage(remainingTime + " Sekunden bis zum Start\n\n");
            return true;
        }else{
            return false;
        }
    }

    /**
     * get the smallest roundtime of all cars
     * also decrement the other cars roundTimes by that time
     *
     */
    private void calculateSmallestRoundTime() {
        int min = Integer.MAX_VALUE;
        String minID = "";
        for(Map.Entry<String, Integer> entry : carTimes.entrySet()){
            String key = entry.getKey();
            int value = entry.getValue();

            if(value < min){
                min = value;
                minID = key;
            }
        }
        minRoundTime = min;
        minRoundTimeId = minID;

        carTimes.remove(minRoundTimeId);
        for(Map.Entry<String, Integer> entry : carTimes.entrySet()){
            String key = entry.getKey();
            int value = entry.getValue();

            value -= minRoundTime;
        }
    }

    /**
     * broadcast message to all clients, that are registered in the race
     *
     * @param message
     */
    private void broadcast(String message) {
        if(clients.size() > 0){
            for(Client c : clients) {
                c.sendMessage(message);
            }
        }
    }

    /**
     * get info from all cars for a specific client
     *
     * @param clientID
     * @return String info
     */
    public String getInfo(String clientID) {
        String info = "info:\n";
        for(String s: this.carRanks.keySet()){
            if(s.toLowerCase().contains(clientID.toLowerCase())) {
                int rank = this.carRanks.get(s);
                if(rank > 0){
                    info += s + "ist auf Rang " + rank + "\n";
                } else {
                    info += "\"" + s + "\" ist noch nicht im Ziel angekommen.\n";
                }
            }
        }
        return info;
    }

    /**
     * get the current time until the race starts
     * @return
     */
    public int getCountdown() {
        if(state == State.ACCEPT || state == State.INIT) {
            LocalTime now = LocalTime.now();
            long diff = registrationStart.until(now, SECONDS);
            long remainingTime = 30 - diff;
            return (int) remainingTime;
        }
        return 0;
    }

    private enum State {
        INIT, ACCEPT, RACESTART, RACERUNNING, RACEEND
    }
}
