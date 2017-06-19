package de.hawHH.RB.TCPTest;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by RB on 11.06.17.
 */
public class Client extends Thread{
    private Socket socket;
    private String connectionTupel;
    private DataOutputStream outputStream;
    private BufferedReader inputStream;

    private Race race;

    private Map<String, String> cars = new HashMap<>();

    private State state;


    public Client(Socket socket, Race race) {
        this.socket = socket;
        this.race = race;
        this.state = State.INIT;

        try {
            connectionTupel = socket.getLocalSocketAddress().toString() + socket.getRemoteSocketAddress().toString();
            inputStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            outputStream = new DataOutputStream(this.socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.sendMessage("type in the name of your car and press enter.\nrepeat to add more cars.\nsend empty line to register all cars at the race.\n");

        this.start();
    }

    public void run() {
        while(true){
            if(state == State.INIT) {
                String input = "";

                try {
                    input = inputStream.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!input.equals("")) {
                    this.addCar(input);
                } else {
                    this.registerCars();
                }
            }

            if(state == State.REGISTERED) {
                String input = "";
                try {
                    input = inputStream.readLine();
                } catch (SocketException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(input.equals("\\INFO")) {
                    this.countdown();
                }
            }

            if(state == State.RACERUNNING) {
                String input = "";
                try {
                    input = inputStream.readLine();
                } catch (SocketException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(input.equals("\\INFO")) {
                    this.sendMessage(this.race.getInfo(connectionTupel));
                }
            }

            if(state == State.RACEEND) {
                this.sendMessage("Race ended. Press return to start a new race with the same cars.\n");
                this.sendMessage("Or type in a new car name to start with new set of cars.\n");

                String input = "";
                try {
                    input = inputStream.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!input.equals("")) {
                    this.state = State.INIT;
                    this.cars = new HashMap<>();
                    this.addCar(input);
                } else {
                    this.registerCars();
                }
            }

            if(state == State.SIGTERM) {
                return;
            }
        }
    }

    /**
     * register all cars of this client in the race
     */
    private void registerCars() {
        for (String longName : cars.keySet()) {
            this.race.registerCar(this, longName, cars.get(longName));
        }
        this.state = State.REGISTERED;
        this.sendMessage("Race starts in:\n");
        this.countdown();
        this.state = State.RACERUNNING;
    }


    /**
     * change state of client to end of race
     */
    public void reset() {
        this.state = State.RACEEND;
    }

    /**
     * close the connection/socket
     */
    public void closeConnection() {
        this.state = State.SIGTERM;
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error closing socket(" + socket.toString() + ")!");
        }
    }

    /**
     * send message on socket
     *
     * @param message
     */
    public void sendMessage(String message) {
        try {
            this.outputStream.writeBytes(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * add car into map of cars
     *
     * @param car
     */
    private void addCar(String car){
        this.cars.put(connectionTupel + "/" + car, car);
    }

    private void countdown() {
        for(int i = race.getCountdown(); i >= 0; --i) {
            this.sendMessage(i + "\n");
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private enum State {
        INIT, REGISTERED, RACERUNNING, RACEEND, SIGTERM
    }

}
