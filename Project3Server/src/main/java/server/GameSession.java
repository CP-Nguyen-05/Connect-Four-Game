package server;

import shared.Message;
import shared.MessageType;
import shared.User;
import java.util.UUID;
import java.io.IOException;
import java.util.List;

public class GameSession implements Runnable {
    private final ConnectionHandler p1;
    private final ConnectionHandler p2;
    // 0 = empty, 1 = p1’s disc, 2 = p2’s disc
    private final int[][] board = new int[6][7];
    private ConnectionHandler current;
    private ConnectionHandler other;
    private final UserDataStore ds = new UserDataStore();

    public GameSession(ConnectionHandler p1, ConnectionHandler p2) {
        this.p1      = p1;
        this.p2      = p2;
        this.current = p1;
        this.other   = p2;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message msg = current.readMessage();
                MessageType type = msg.getType();

                if (type == MessageType.MOVE) {
                    System.out.println("MOve go in GameSession");
                    int col = Integer.parseInt(msg.getContent());
                    int playerId = (current == p1 ? 1 : 2);
                    int row = dropDisc(col, playerId);
                    if (row < 0) {
                        // invalid move (column full)
                        current.sendMessage(error("Column is full!"));
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

                    // check for win
                    if (checkWin(row, col, playerId)) {
                        endGame(current, other);
                        return;
                    }

                    // check for draw
                    if (isDraw()) {
                        endDraw();
                        return;
                    }

                    // swap turns
                    swapPlayers();

                } else if (type == MessageType.CHAT) {
                    // in-game chat
                    relay(msg);

                } else if (type == MessageType.SURRENDER) {
                    // someone surrendered
                    endSurrender(current, other);
                    return;
                }
                // ignore any other message types
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        // no cleanup here; your ConnectionHandler finally block can remove the room
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
                u.setWinCount(u.getWinCount() + 1);
                u.setGamesPlayed(u.getGamesPlayed() + 1);
            } else if (u.getUsername().equals(loser.getUsername())) {
                u.setLossCount(u.getLossCount() + 1);
                u.setGamesPlayed(u.getGamesPlayed() + 1);
            }
        }
        ds.saveUsers(users);
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
    }

    private void endSurrender(ConnectionHandler loser, ConnectionHandler winner) {
        // same as a normal win/loss
        endGame(winner, loser);
    }

    private void relay(Message m) {
        p1.sendMessage(m);
        p2.sendMessage(m);
    }

    private Message error(String text) {
        return new Message(
                UUID.randomUUID().toString(),
                MessageType.ERROR,
                text,
                "SERVER",
                current.getUsername(),
                System.currentTimeMillis()
        );
    }
}