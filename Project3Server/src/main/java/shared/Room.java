package shared;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String roomId;
    private List<User> players    = new ArrayList<>();
    private List<User> spectators = new ArrayList<>();
    private int maxPlayerCapacity;

    public Room(String roomId, int maxPlayerCapacity) {
        this.roomId            = roomId;
        this.maxPlayerCapacity = maxPlayerCapacity;
    }

    public boolean isOpenForPlayers() {
        return players.size() < maxPlayerCapacity;
    }

    public void addPlayer(User u) {
        players.add(u);
    }

    public void addSpectator(User u) {
        spectators.add(u);
    }

    public String getRoomId() {
        return roomId;
    }
}