package org.example.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ConnectionHandler implements Runnable {
    private Socket socket;

    public ConnectionHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // For demonstration, send a welcome message
            out.println("Welcome to Connect Four Server!");

            // Example: Echo any line the client sends
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received from client: " + line);
                out.println("You said: " + line);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
}