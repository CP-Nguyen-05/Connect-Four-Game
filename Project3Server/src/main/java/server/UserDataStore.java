package server;

import shared.User;

import java.io.*;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.List;

public class UserDataStore {
    private static final String FILE_PATH = "PlayerData/players.txt";

//     Reads all users out of users.txt (or returns empty list if file missing).
    public List<User> loadUsers() {
        File f = new File(FILE_PATH);
        List<User> users = new ArrayList<>();
        if (!f.exists()) return users;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length != 8) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }
                users.add(new User(
                        parts[0],  // displayName
                        parts[1],  // username
                        parts[2],  // password
                        Integer.parseInt(parts[3]), // score
                        Integer.parseInt(parts[4]), // gamesPlayed
                        Integer.parseInt(parts[5]), // winCount
                        Integer.parseInt(parts[6]), // lossCount
                        Integer.parseInt(parts[7])  // drawCount
                ));
            }
        } catch (IOException | NumberFormatException ex) {
            ex.printStackTrace();
        }
        return users;
    }

//    Overwrites users.txt so each line is exactly 8 comma‑separated fields.
    public void saveUsers(List<User> users) {
        File f = new File(FILE_PATH);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (User u : users) {
                // Alice,alice,password123,0,0,0,0,0
                String line = String.join(",",
                        u.getDisplayName(),
                        u.getUsername(),
                        u.getPassword(),
                        String.valueOf(u.getScore()),
                        String.valueOf(u.getGamesPlayed()),
                        String.valueOf(u.getWinCount()),
                        String.valueOf(u.getLossCount()),
                        String.valueOf(u.getDrawCount())
                );
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    public List<User> getLeaderboard() {
        return loadUsers().stream()
                .filter(u -> u.getScore() > 0)
                .sorted(Comparator.comparingInt(User::getScore).reversed())
                .collect(Collectors.toList());
    }
}