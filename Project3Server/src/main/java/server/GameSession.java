package server;

import shared.Message;
import shared.MessageType;
import shared.User;
import java.util.UUID;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class GameSession implements Runnable {
    private final ConnectionHandler p1, p2;
    private ConnectionHandler current, other;
    private int[][] board = new int[6][7];
    private final BlockingQueue<Message> inbox = new LinkedBlockingQueue<>();
    private final UserDataStore ds = new UserDataStore();
    private final Server server;

    private boolean rematch1 = false;
    private boolean rematch2 = false;

    public GameSession(ConnectionHandler p1, ConnectionHandler p2, Server server) {
        this.p1 = p1; this.p2 = p2; this.current = p1; this.other = p2;
        this.server = server;
    }

    public void handleMessage(Message m) {
        inbox.offer(m);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message msg = inbox.take();   // BLOCK until a message arrives

                MessageType type = msg.getType();
                if (type == MessageType.MOVE) {
                    int col = Integer.parseInt(msg.getContent());
                    int pid = (current == p1 ? 1 : 2);
                    int row = dropDisc(col, pid);
                    if (row < 0) {
                        current.sendMessage(new Message(
                                UUID.randomUUID().toString(),
                                MessageType.ERROR,
                                "Column is full!",
                                "SERVER",
                                null,
                                System.currentTimeMillis()
                        ));
                        continue;
                    }
                    // broadcast the move "col,row"
                    String payload = col + "," + row;
                    relay(new Message(
                            UUID.randomUUID().toString(),
                            MessageType.MOVE,
                            payload,
                            current.getUsername(),
                            null,
                            System.currentTimeMillis()
                    ));
                    if (checkWin(row, col, pid)) {
                        endGame(current, other);
//                        server.getRoomManager().removeRoom(current.getCurrentRoomId());
//                        server.removeGameSession(current.getCurrentRoomId());
                        return;
                    }
                    if (isDraw()) {
                        endDraw();
//                        server.getRoomManager().removeRoom(current.getCurrentRoomId());
//                        server.removeGameSession(current.getCurrentRoomId());
                        return;
                    }
                    swapPlayers();

                }
                else if (type == MessageType.CHAT) {
                    // in-game chat: echo to both
                    relay(msg);

                }
                else if (type == MessageType.SURRENDER) {
                    if (msg.getSender().equals(current.getUsername())){
                        endGame(other, current);
                    }
                    else{
                        endGame(current, other);
                    }
//                    server.getRoomManager().removeRoom(current.getCurrentRoomId());
//                    server.removeGameSession(current.getCurrentRoomId());
                    return;
                }
                else if (type == MessageType.REMATCH_REQUEST){
                    System.out.println("rematch request received from GameSession");
                    if (msg.getSender().equals(p1.getUsername())) rematch1 = true;
                    if (msg.getSender().equals(p2.getUsername())) rematch2 = true;

                    if (rematch1 && rematch2) {
                        resetBoard();
                        rematch1 = rematch2 = false;
                        current = p1;
                        other   = p2;
                        // reuse GAME_START so clients call showGameScene()
                        for (ConnectionHandler ch : new ConnectionHandler[]{p1, p2}) {
                            ch.sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.GAME_START,
                                    current.getCurrentRoomId(),
                                    "SERVER",
                                    ch.getUsername(),
                                    System.currentTimeMillis()
                            ));
                        }
                    }
                }
                else if (type == MessageType.REMATCH_REJECT){
                    for (ConnectionHandler ch : new ConnectionHandler[]{p1, p2}) {
                        if (!ch.getUsername().equals(msg.getSender())){
                            ch.sendMessage(new Message(
                                    UUID.randomUUID().toString(),
                                    MessageType.ROOM_CANCELLED,
                                    current.getCurrentRoomId(),
                                    "SERVER",
                                    ch.getUsername(),
                                    System.currentTimeMillis()
                            ));
                            System.out.println("send rematch rejected back to "+ch.getUsername());
                        }
                    }
                    server.getRoomManager().removeRoom(current.getCurrentRoomId());
                    server.removeGameSession(current.getCurrentRoomId());
                }

            }
        }
        catch (InterruptedException e) {
            // session was cancelled
        }
        // Optionally notify ConnectionHandler to clean up room
    }


    private int dropDisc(int col, int playerId) {
        for (int row = 5; row >= 0; row--) {
            if (board[row][col] == 0) {
                board[row][col] = playerId;
                return row;
            }
        }
        return -1;
    }

    private boolean isDraw() {
        for (int c = 0; c < 7; c++) {
            if (board[0][c] == 0) return false;
        }
        return true;
    }

    private boolean checkWin(int r, int c, int pid) {
        // directions: horizontal, vertical, diag1, diag2
        int[][] dirs = new int[][] { {0,1}, {1,0}, {1,1}, {1,-1} };
        for (int i = 0; i < dirs.length; i++) {
            int dr = dirs[i][0], dc = dirs[i][1];
            int cnt = 1;
            cnt += countDir(r, c, dr,  dc,  pid);
            cnt += countDir(r, c, -dr, -dc, pid);
            if (cnt >= 4) return true;
        }
        return false;
    }

    private int countDir(int r, int c, int dr, int dc, int pid) {
        int cnt = 0;
        for (int step = 1; step < 4; step++) {
            int nr = r + dr*step, nc = c + dc*step;
            if (nr < 0 || nr > 5 || nc < 0 || nc > 6 || board[nr][nc] != pid) {
                break;
            }
            cnt++;
        }
        return cnt;
    }

    private void swapPlayers() {
        ConnectionHandler tmp = current;
        current = other;
        other = tmp;
    }

    private void endGame(ConnectionHandler winner, ConnectionHandler loser) {
        // notify clients
        winner.sendMessage(new Message(
                UUID.randomUUID().toString(),
                MessageType.GAME_END,
                "YOU_WIN",
                "SERVER",
                winner.getUsername(),
                System.currentTimeMillis()
        ));
        loser.sendMessage(new Message(
                UUID.randomUUID().toString(),
                MessageType.GAME_END,
                "YOU_LOSE",
                "SERVER",
                loser.getUsername(),
                System.currentTimeMillis()
        ));
        // persist stats
        List<User> users = ds.loadUsers();
        for (User u : users) {
            if (u.getUsername().equals(winner.getUsername())) {
                u.setScore(u.getScore()+10);
                u.setWinCount(u.getWinCount() + 1);
                u.setGamesPlayed(u.getGamesPlayed() + 1);
            } else if (u.getUsername().equals(loser.getUsername())) {
                if (u.getScore()>0){
                    u.setScore(u.getScore()-10);
                }
                u.setLossCount(u.getLossCount() + 1);
                u.setGamesPlayed(u.getGamesPlayed() + 1);
            }
        }
        ds.saveUsers(users);
        System.out.println(current.getCurrentRoomId()+ " ended game");
    }

    private void endDraw() {
        // both get DRAW
        relay(new Message(
                UUID.randomUUID().toString(),
                MessageType.GAME_END,
                "DRAW",
                "SERVER",
                null,
                System.currentTimeMillis()
        ));
        // persist draw for both
        List<User> users = ds.loadUsers();
        for (User u : users) {
            if (u.getUsername().equals(p1.getUsername()) ||
                    u.getUsername().equals(p2.getUsername())) {
                u.setDrawCount(u.getDrawCount() + 1);
                u.setGamesPlayed(u.getGamesPlayed() + 1);
            }
        }
        ds.saveUsers(users);
        System.out.println(current.getCurrentRoomId()+ " ended game");

    }

    private void relay(Message m) {
        p1.sendMessage(m);
        p2.sendMessage(m);
    }
    private void resetBoard() {
        board = new int[6][7];
    }

//    private Message error(String text) {
//        return new Message(
//                UUID.randomUUID().toString(),
//                MessageType.ERROR,
//                text,
//                "SERVER",
//                current.getUsername(),
//                System.currentTimeMillis()
//        );
//    }
}