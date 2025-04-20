package server;
import shared.Message;
import shared.MessageType;
import shared.User;
import shared.Room;
import shared.RoomManager;
public class GameSession implements Runnable {
    private ConnectionHandler player1, player2;

    public GameSession(ConnectionHandler p1, ConnectionHandler p2) {
        this.player1 = p1;
        this.player2 = p2;
    }

    @Override
    public void run() {
        // TODO: read moves from one, relayMessage to both
    }

    public void relayMessage(Message msg) {
        player1.sendMessage(msg);
        player2.sendMessage(msg);
    }
}