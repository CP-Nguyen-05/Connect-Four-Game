package shared;

import shared.Message;
import shared.MessageType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.util.UUID;

public class RegisterController {
    private final ConnectFourApp app;
    private Scene scene;

    private TextField usernameField;
    private PasswordField passwordField;
    private PasswordField confirmField;
    private Label messageLabel;

    public RegisterController(ConnectFourApp app) {
        this.app = app;
        buildScene();
    }

    private void buildScene() {
        // Title label
        Label title = new Label("Register");
        title.setFont(Font.font(ConnectFourApp.globalFontFamily, 36));
        HBox titleBox = new HBox(title);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // Input fields
        usernameField = new TextField();
        usernameField.setPromptText("Username");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        confirmField = new PasswordField();
        confirmField.setPromptText("Confirm Password");

        // Message label
        messageLabel = new Label();
        HBox messageBox = new HBox(messageLabel);
        messageBox.setAlignment(Pos.CENTER_LEFT);

        // Buttons
        Button submitBtn = new Button("Sign Up");
        submitBtn.setMaxWidth(Double.MAX_VALUE);

        Button backBtn = new Button("Already have an account? Log in");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #26268C; -fx-underline: true; -fx-cursor: hand;");

        submitBtn.setOnAction(e -> doRegister());
        backBtn.setOnAction(e -> app.showLoginScene());

        // Form layout
        VBox form = new VBox(10,
                titleBox,
                messageBox,
                usernameField,
                passwordField,
                confirmField,
                submitBtn,
                backBtn
        );
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(500);
        form.setMaxHeight(200);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #C0C0C0; -fx-border-color: #FFF; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Root layout
        StackPane root = new StackPane(form);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #008081;");

        // Scene
        scene = new Scene(root, 1600, 900);
        app.applyGlobalStyles(scene);  // ✅ Apply global font style
    }

    public Scene getScene() {
        return scene;
    }

    private void doRegister() {
        String userName = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmField.getText();

        if (userName.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            messageLabel.setText("All fields are required");
            return;
        }

        if (!password.equals(confirm)) {
            messageLabel.setText("Please make sure your passwords match");
            return;
        }

        try {
            ClientConnection conn = app.getOrCreateConnection();
            Message m = new Message(
                    UUID.randomUUID().toString(),
                    MessageType.REGISTER,
                    password,
                    userName,
                    null,
                    System.currentTimeMillis()
            );
            conn.sendMessage(m);

            Message reply = conn.receiveMessage();
            if (reply.getType() == MessageType.REGISTER_SUCCESS) {
                messageLabel.setText("Registration succeeded. Please log in.");
                usernameField.clear();
                passwordField.clear();
                confirmField.clear();
            } else {
                messageLabel.setText(reply.getContent());
            }
        } catch (Exception ex) {
            messageLabel.setText("Server error: " + ex.getMessage());
        }
    }
}
