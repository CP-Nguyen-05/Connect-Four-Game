package org.example.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientMain {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to Connect Four Server at " + SERVER_ADDRESS + ":" + SERVER_PORT);

            // Read the welcome message from the server
            System.out.println("Server says: " + in.readLine());

            // Simple loop to read lines from console and send to server
            String userInput;
            System.out.println("Type a message (or 'exit' to quit):");
            while ((userInput = consoleInput.readLine()) != null) {
                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
                out.println(userInput); // send to server
                String serverResponse = in.readLine();
                System.out.println("Server: " + serverResponse);
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}