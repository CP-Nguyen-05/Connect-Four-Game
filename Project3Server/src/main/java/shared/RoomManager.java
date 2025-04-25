package shared;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomManager {
    private List<Room> activeRooms = new ArrayList<>();

    public Optional<Room> getAvailableRoom() {
        return activeRooms.stream()
                .filter(Room::isOpenForPlayers)
                .findFirst();
    }
    private int nextRoomNumber = 1;
    public Room createRoom() {
        String id = "ROOM " + nextRoomNumber++;
        Room r = new Room(id, 2);
        activeRooms.add(r);
        return r;
    }

    public Optional<Room> findRoomById(String roomId) {
        return activeRooms.stream()
                .filter(r -> r.getRoomId().equals(roomId))
                .findFirst();
    }
    public synchronized void removeRoom(String roomId) {
        activeRooms.removeIf(r -> r.getRoomId().equals(roomId));
    }

    public List<Room> getOpenRooms() {
        return new ArrayList<>(activeRooms);
    }
}