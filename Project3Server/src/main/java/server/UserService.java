package server;
import shared.Message;
import shared.MessageType;
import shared.User;
import shared.Room;
import shared.RoomManager;
import java.util.List;

public class UserService {
    private UserDataStore store = new UserDataStore();

    public User validateCredentials(String username, String password) {
        return store.loadUsers().stream()
                .filter(u -> u.getUsername().equals(username) &&
                        u.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }

    public boolean usernameExists(String username) {
        return store.loadUsers().stream()
                .anyMatch(u -> u.getUsername().equals(username));
    }

    public boolean registerUser(User u) {
        List<User> all = store.loadUsers();
        if (all.stream().anyMatch(x -> x.getUsername().equals(u.getUsername())))
            return false;
        all.add(u);
        store.saveUsers(all);
        return true;
    }

    public boolean deleteUser(String username, String password) {
        List<User> all = store.loadUsers();
        boolean removed = all.removeIf(u ->
                u.getUsername().equals(username) &&
                        u.getPassword().equals(password));
        if (removed) store.saveUsers(all);
        return removed;
    }
}