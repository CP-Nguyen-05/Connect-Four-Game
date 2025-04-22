package shared;

import shared.Message;
import shared.MessageType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.UUID;

public class RegisterController {
    private final ConnectFourApp app;
    private Scene scene;

    private TextField    usernameField;
    private PasswordField passwordField;
    private PasswordField confirmField;
    private Label         messageLabel;

    public RegisterController(ConnectFourApp app) {
        this.app = app;
        buildScene();
    }

    private void buildScene() {
        usernameField = new TextField();
        passwordField = new PasswordField();
        confirmField  = new PasswordField();
        messageLabel  = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        Button submitBtn = new Button("Submit");
        Button backBtn   = new Button("Back");

        submitBtn.setOnAction(e -> doRegister());
        backBtn  .setOnAction(e -> app.showLoginScene());

        HBox buttons = new HBox(10, submitBtn, backBtn);
        buttons.setPadding(new Insets(10));
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(10,
                new Label("Username:"), usernameField,
                new Label("Password:"), passwordField,
                new Label("Confirm Password:"), confirmField,
                buttons,
                messageLabel
        );
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        scene = new Scene(root, 350, 300);
    }

    public Scene getScene() {
        return scene;
    }

    private void doRegister() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        String c = confirmField.getText();
        if (u.isEmpty() || p.isEmpty() || c.isEmpty()) {
            messageLabel.setText("All fields are required");
            return;
        }
        if (!p.equals(c)) {
            messageLabel.setText("Passwords don’t match");
            return;
        }

        try {
            ClientConnection conn = app.getOrCreateConnection();
            // content = password, sender = username
            Message m = new Message(
                    UUID.randomUUID().toString(),
                    MessageType.REGISTER,
                    p,    // password
                    u,    // username
                    null,
                    System.currentTimeMillis()
            );
            conn.sendMessage(m);

            Message reply = conn.receiveMessage();
            if (reply.getType() == MessageType.CHAT) {
                messageLabel.setText("Registration succeeded. Please log in.");
                app.showLoginScene();
            } else {
                messageLabel.setText(reply.getContent());
            }
        } catch (Exception ex) {
            messageLabel.setText("Server error: " + ex.getMessage());
        }
    }
}