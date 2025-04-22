package server;

import shared.Message;
import shared.MessageType;
import java.io.IOException;
import java.util.UUID;

public class GameSession implements Runnable {
    private final ConnectionHandler p1, p2;
    private final UserService userService = new UserService();

    public GameSession(ConnectionHandler p1, ConnectionHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // read from whichever player speaks first
                Message msg = p1.readMessage();

                switch (msg.getType()) {
                    case MOVE:
                    case CHAT:
                        // just relay
                        relay(msg);
                        break;

                    case SURRENDER:
                        // if p1 surrenders, p2 wins
                        endGame(p1, p2, /*isDraw=*/false);
                        return;

                    // you can also detect natural four‐in‐a‐row on the server side,
                    // then call endGame(..., isDraw).
                }

                // now also check p2
                msg = p2.readMessage();
                switch (msg.getType()) {
                    case MOVE:
                    case CHAT:
                        relay(msg);
                        break;
                    case SURRENDER:
                        endGame(p2, p1, /*isDraw=*/false);
                        return;
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private void relay(Message m) {
        p1.sendMessage(m);
        p2.sendMessage(m);
    }

    private void endGame(
            ConnectionHandler surrendered,
            ConnectionHandler winner,
            boolean isDraw
    ) throws IOException {
        // notify loser
        surrendered.sendMessage(new Message(
                UUID.randomUUID().toString(),
                MessageType.GAME_END,
                isDraw ? "DRAW" : "YOU_LOSE",
                "SERVER",
                surrendered.getUsername(),
                System.currentTimeMillis()
        ));
        // notify winner
        winner.sendMessage(new Message(
                UUID.randomUUID().toString(),
                MessageType.GAME_END,
                isDraw ? "DRAW" : "YOU_WIN",
                "SERVER",
                winner.getUsername(),
                System.currentTimeMillis()
        ));

        // persist stats: winner vs loser (draw flag)
        userService.updateAndSaveStats(
                winner.getUsername(),
                surrendered.getUsername(),
                isDraw
        );
    }
}