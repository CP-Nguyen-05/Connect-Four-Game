import shared.User;

import shared.MessageType;
import shared.Message;

import java.util.UUID;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class ConnectFourApp extends Application {
    private Stage primaryStage;
    private User currentUser;
    private Client client;  // or ClientConnection + Client wrapper
    private ClientConnection conn;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Connect Four");
        showLoginScene();
        stage.show();
    }

    private void showLoginScene() {
        // Build controls
        Label userLbl = new Label("Username:");
        TextField userFld = new TextField();
        Label passLbl = new Label("Password:");
        PasswordField passFld = new PasswordField();
        Label msgLbl  = new Label();      // <-- will show register/login feedback

        Button loginBtn = new Button("Login");
        Button regBtn   = new Button("Register");

        // REGISTER handler
        regBtn.setOnAction(e -> {
            String u = userFld.getText().trim();
            String p = passFld.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                msgLbl.setText("Enter both username & password");
                return;
            }
            try {
                // 1) Connect once
                if (conn == null) {
                    conn = new ClientConnection();
                    conn.connect("localhost", 12345);
                }
                // 2) Send REGISTER
                Message reg = new Message(
                        UUID.randomUUID().toString(),
                        MessageType.REGISTER,
                        p,    // password in content
                        u,    // sender=username
                        null,
                        System.currentTimeMillis()
                );
                conn.sendMessage(reg);

                // 3) BLOCKING read of server’s reply
                Message reply = conn.receiveMessage();
                if (reply.getType() == MessageType.CHAT) {
                    msgLbl.setText(reply.getContent());
                } else {
                    msgLbl.setText("Error: " + reply.getContent());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                msgLbl.setText("Server error: " + ex.getMessage());
            }
        });

        // LOGIN handler
        loginBtn.setOnAction(e -> {
            String u = userFld.getText().trim();
            String p = passFld.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                msgLbl.setText("Enter both username & password");
                return;
            }
            try {
                // 1) If we haven’t connected yet, do so now
                if (conn == null) {
                    conn = new ClientConnection();
                    conn.connect("localhost", 12345);
                }

                // 2) Send LOGIN (no need to REGISTER first)
                Message login = new Message(
                        UUID.randomUUID().toString(),
                        MessageType.LOGIN,
                        p,    // password in content
                        u,    // sender=username
                        null,
                        System.currentTimeMillis()
                );
                conn.sendMessage(login);

                // 3) Wait for server reply
                Message reply = conn.receiveMessage();
                if (reply.getType() == MessageType.ERROR) {
                    msgLbl.setText(reply.getContent());
                } else {
                    // LOGIN successful!
                    currentUser = new User(u, u, p, 0,0,0,0,0);
                    showOptionMenuScene();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                msgLbl.setText("Server error: " + ex.getMessage());
            }
        });

        // Layout
        HBox buttons = new HBox(10, loginBtn, regBtn);
        VBox root = new VBox(10,
                userLbl, userFld,
                passLbl, passFld,
                buttons,
                msgLbl
        );
        root.setPadding(new Insets(20));
        primaryStage.setScene(new Scene(root, 320, 240));
    }

    private void showOptionMenuScene() {
        Button lanBtn      = new Button("LAN Play");
        Button compBtn     = new Button("Computer");
        Button howToPlayBtn= new Button("How To Play");
        Button profileBtn  = new Button("Profile");

        lanBtn.setOnAction(e -> showRoomScene());
        // compBtn.setOnAction(e -> startSinglePlayer());
        howToPlayBtn.setOnAction(e -> showHowToPlayScene());
        profileBtn.setOnAction(e -> showProfileScene());

        VBox root = new VBox(15, lanBtn, compBtn, howToPlayBtn, profileBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        primaryStage.setScene(new Scene(root, 400, 300));
    }

    private void showRoomScene() {
        Label roomMsg = new Label("Join or Create a Room");
        TextField roomIdFld = new TextField();
        roomIdFld.setPromptText("Room ID");
        Button createBtn = new Button("Create Room");
        Button quickBtn  = new Button("Quick Join");
        Button joinBtn   = new Button("Join By ID");

        createBtn.setOnAction(e -> {
            // client.send(CREATE_ROOM)
            // wait for GAME_START
        });
        quickBtn.setOnAction(e -> {
            // client.send(QUICK_JOIN)
        });
        joinBtn.setOnAction(e -> {
            String id = roomIdFld.getText().trim();
            // client.send(JOIN_ROOM, content=id)
        });

        VBox root = new VBox(10,
                roomMsg,
                new HBox(10, roomIdFld, joinBtn),
                quickBtn,
                createBtn
        );
        root.setPadding(new Insets(20));
        primaryStage.setScene(new Scene(root, 400, 250));
    }

    private void showGameScene() {
        // build your 7×6 GridPane of Circles here,
        // add click‑handlers that call client.send(MOVE) and
        // update the board locally.
    }

    private void showProfileScene() {
        // Label displayName = new Label(currentUser.getDisplayName());
        // etc.
    }

    private void showHowToPlayScene() {
        TextArea howTo = new TextArea("Rules:\n1. … \n2. …");
        howTo.setEditable(false);
        Button back = new Button("Back");
        back.setOnAction(e -> showOptionMenuScene());
        VBox root = new VBox(10, howTo, back);
        root.setPadding(new Insets(20));
        primaryStage.setScene(new Scene(root, 400, 400));
    }

    public static void main(String[] args) {
        launch(args);
    }
}