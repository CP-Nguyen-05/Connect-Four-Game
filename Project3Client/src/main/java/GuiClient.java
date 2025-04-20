import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.UUID;

public class GuiClient extends Application {
	private Client client;
	private TextArea chatArea;
	private TextField inputField;
	private TextField usernameField;
	private Button connectButton;
	private Button sendButton;

	@Override
	public void start(Stage primaryStage) {
		// Top: username + connect
		usernameField = new TextField();
		usernameField.setPromptText("Username");
		connectButton = new Button("Connect");
		connectButton.setOnAction(e -> onConnect());
		HBox topBar = new HBox(10, usernameField, connectButton);
		topBar.setPadding(new Insets(10));

		// Center: chat display
		chatArea = new TextArea();
		chatArea.setEditable(false);
		chatArea.setWrapText(true);
		VBox.setVgrow(chatArea, Priority.ALWAYS);

		// Bottom: input + send
		inputField = new TextField();
		inputField.setPromptText("Type a message...");
		inputField.setDisable(true);
		inputField.setOnAction(e -> onSend());

		sendButton = new Button("Send");
		sendButton.setDisable(true);
		sendButton.setOnAction(e -> onSend());

		HBox bottomBar = new HBox(10, inputField, sendButton);
		bottomBar.setPadding(new Insets(10));
		HBox.setHgrow(inputField, Priority.ALWAYS);

		// Layout
		VBox root = new VBox(10, topBar, chatArea, bottomBar);
		Scene scene = new Scene(root, 600, 400);

		primaryStage.setTitle("Connect Four - Client Chat");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private void onConnect() {
		String uname = usernameField.getText().trim();
		if (uname.isEmpty()) {
			showAlert("Please enter a username before connecting.");
			return;
		}

		client = new Client();
		client.username = uname;
		client.start();

		usernameField.setDisable(true);
		connectButton.setDisable(true);
		inputField.setDisable(false);
		sendButton.setDisable(false);

		appendToChat(">> Connected as " + uname + "\n");

		// Thread to handle incoming messages
		Thread reader = new Thread(() -> {
			try {
				while (true) {
					Message msg = client.readMessage();
					Platform.runLater(() ->
							appendToChat(msg.toString() + "\n")
					);
				}
			} catch (Exception ex) {
				Platform.runLater(() ->
						appendToChat(">> Disconnected from server\n")
				);
			}
		});
		reader.setDaemon(true);
		reader.start();
	}

	private void onSend() {
		String text = inputField.getText().trim();
		if (text.isEmpty() || client == null) return;

		String uname = client.username;
		Message msg;

		if (text.startsWith("@")) {
			String[] parts = text.split(" ", 2);
			if (parts.length < 2) {
				inputField.clear();
				return;
			}
			String recipient = parts[0].substring(1);
			String content   = parts[1];
			msg = new Message(
					UUID.randomUUID().toString(),
					MessageType.CHAT,
					content,
					uname,
					recipient,
					System.currentTimeMillis()
			);
		} else {
			msg = new Message(
					UUID.randomUUID().toString(),
					MessageType.CHAT,
					text,
					uname,
					null,
					System.currentTimeMillis()
			);
		}

		client.send(msg);
		inputField.clear();
	}

	private void appendToChat(String text) {
		chatArea.appendText(text);
		chatArea.setScrollTop(Double.MAX_VALUE);
	}

	private void showAlert(String message) {
		Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
		alert.initOwner(usernameField.getScene().getWindow());
		alert.showAndWait();
	}

	public static void main(String[] args) {
		launch(args);
	}
}