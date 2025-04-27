package server;

import shared.Message;
import shared.MessageType;
import shared.User;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GameSession implements Runnable {
    private ConnectionHandler p1, p2;
    private ConnectionHandler current, other;
    private int[][] board = new int[6][7];
    private final BlockingQueue<Message> inbox = new LinkedBlockingQueue<>();
    private final UserDataStore ds = new UserDataStore();
    private final Server server;
    private final String roomId;

    // per-player rematch votes
    private volatile boolean rematchP1 = false;
    private volatile boolean rematchP2 = false;

    public GameSession(ConnectionHandler p1, ConnectionHandler p2, Server server, String roomId) {
        this.p1      = p1;
        this.p2      = p2;
        this.current = p1;
        this.other   = p2;
        this.server  = server;
        this.roomId  = roomId;
    }

    // Called by ConnectionHandler whenever a MESSAGE arrives.
    public void handleMessage(Message m) {
        MessageType t = m.getType();
        switch (t) {
            case MOVE:
            case CHAT:
            case SURRENDER:
                inbox.offer(m);
                break;
            case REMATCH_REQUEST:
                if (m.getSender().equals(p1.getUsername())) rematchP1 = true;
                else if (m.getSender().equals(p2.getUsername())) rematchP2 = true;
                break;
            case REMATCH_REJECT:
                // immediately end session and tell both sides
                Message cancel = new Message(
                        MessageType.ROOM_CANCELLED,
                        roomId,
                        "SERVER",
                        null
                );
                p1.sendMessage(cancel);
                p2.sendMessage(cancel);
                // interrupt the session loop
                Thread.currentThread().interrupt();
                server.getRoomManager().removeRoom(roomId);
                server.removeGameSession(roomId);
                break;
            default:
                // ignore
        }
    }

    @Override
    public void run() {
        try {
            boolean keepPlaying = true;
            while (keepPlaying && !Thread.currentThread().isInterrupted()) {
                playSingleGame();
                // after game end, offer rematch
                rematchP1 = rematchP2 = false;

                // wait until one rejects or both accept
                while (!Thread.currentThread().isInterrupted()) {
                    if (rematchP1 && rematchP2) {
                        ConnectionHandler temp = p1;
                        p1 = p2;
                        p2 = temp;
                        keepPlaying = true;
                        resetBoard();
                        Message restart = new Message(
                                MessageType.GAME_START,
                                roomId,
                                "SERVER",
                                null
                        );
                        p1.sendMessage(restart);
                        p2.sendMessage(restart);
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            // session was cancelled (either a reject or connection closed)
        } finally {
            // cleanup room & session
            server.getRoomManager().removeRoom(roomId);
            server.removeGameSession(roomId);
        }
    }

    private void playSingleGame() throws InterruptedException {

        while (!Thread.currentThread().isInterrupted()) {
            Message msg = inbox.take();
            switch (msg.getType()) {
                case MOVE:
                    int col = Integer.parseInt(msg.getContent());
                    int pid = current == p1 ? 1 : 2;
                    int row = dropDisc(col, pid);
                    if (row < 0) {
                        current.sendMessage(error("That column is full."));
                        break;
                    }
                    // relay to both
                    Message m2 = new Message(
                            MessageType.MOVE,
                            col + "," + row,
                            current.getUsername(),
                            null
                    );
                    p1.sendMessage(m2);
                    p2.sendMessage(m2);

                    if (checkWin(row, col, pid)) {
                        endGame(current, other);
                        return;
                    }
                    if (isDraw()) {
                        endDraw();
                        return;
                    }
                    swapPlayers();
                    break;

                case SURRENDER:
                    if (msg.getSender().equals(current.getUsername())){
                        endGame(other, current);
                    }
                    else{
                        endGame(current, other);
                    }
                    return;

                case CHAT:
                    // echo chat to both
                    p1.sendMessage(msg);
                    p2.sendMessage(msg);
                    break;
            }
        }
    }

    private int dropDisc(int col, int playerId) {
        for (int r = 5; r >= 0; r--) {
            if (board[r][col] == 0) {
                board[r][col] = playerId;
                return r;
            }
        }
        return -1;
    }

    private boolean isDraw() {
        for (int c = 0; c < 7; c++) if (board[0][c] == 0) return false;
        return true;
    }

    private boolean checkWin(int r, int c, int pid) {
        int[][] dirs = {{0,1},{1,0},{1,1},{1,-1}};
        for (int[] d : dirs) {
            int cnt = 1 + count(r,c,d[0],d[1],pid) + count(r,c,-d[0],-d[1],pid);
            if (cnt >= 4) return true;
        }
        return false;
    }

    private int count(int r, int c, int dr, int dc, int pid) {
        int cnt = 0;
        for (int i = 1; i < 4; i++) {
            int nr = r + dr*i, nc = c + dc*i;
            if (nr<0||nr>5||nc<0||nc>6||board[nr][nc]!=pid) break;
            cnt++;
        }
        return cnt;
    }

    private void swapPlayers() {
        ConnectionHandler tmp = current;
        current = other;
        other   = tmp;
    }

    private void endGame(ConnectionHandler win, ConnectionHandler lose) {
        System.out.println(win.getUsername()+ " won game in "+ roomId);
        System.out.println("Game over in "+ roomId);
        win.sendMessage(new Message(
                MessageType.GAME_END,
                "YOU_WIN",
                "SERVER",
                win.getUsername()
        ));
        lose.sendMessage(new Message(
                MessageType.GAME_END,
                "YOU_LOSE",
                "SERVER",
                lose.getUsername()
        ));
        List<User> all = ds.loadUsers();
        for (User u : all) {
            if (u.getUsername().equals(win.getUsername())) {
                u.setScore(u.getScore()+10);
                u.setWinCount(u.getWinCount()+1);
                u.setGamesPlayed(u.getGamesPlayed()+1);
            } else if (u.getUsername().equals(lose.getUsername())) {
                u.setScore(u.getScore()-10);
                if (u.getScore()<0){
                    u.setScore(u.getScore()*0);
                }
                u.setLossCount(u.getLossCount()+1);
                u.setGamesPlayed(u.getGamesPlayed()+1);
            }
        }
        ds.saveUsers(all);
    }

    private void endDraw() {
        // notify
        Message draw = new Message(
                MessageType.GAME_END,
                "DRAW",
                "SERVER",
                null
        );
        p1.sendMessage(draw);
        p2.sendMessage(draw);
        // persist draw counts...
        List<User> all = ds.loadUsers();
        for (User u: all) {
            if (u.getUsername().equals(p1.getUsername()) ||
                    u.getUsername().equals(p2.getUsername())) {
                u.setScore(u.getScore()+5);
                u.setDrawCount(u.getDrawCount()+1);
                u.setGamesPlayed(u.getGamesPlayed()+1);
            }
        }
        ds.saveUsers(all);
    }

    private void resetBoard() {
        board = new int[6][7];
        current = p1;
        other   = p2;
    }

    private Message gameStartMsg() {
        return new Message(
                MessageType.GAME_START,
                roomId,
                "SERVER",
                null
        );
    }

    private Message error(String text) {
        return new Message(
                MessageType.ERROR,
                text,
                "SERVER",
                current.getUsername()
        );
    }
}