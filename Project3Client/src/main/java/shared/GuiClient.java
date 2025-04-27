package shared;
import shared.User;
import shared.Message;
import shared.MessageType;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.UUID;

public class GuiClient extends Application {
	private ClientConnection conn;
	private Thread readerThread;

	private TextArea chatArea;
	private TextField inputField;
	private TextField usernameField;
	private Button connectButton;
	private Button sendButton;

	@Override
	public void start(Stage primaryStage) {
		// Top bar: username + connect
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

		// Bottom bar: input + send
		inputField = new TextField();
		inputField.setPromptText("Type a message...");
		inputField.setDisable(true);

		sendButton = new Button("Send");
		sendButton.setDisable(true);
		sendButton.setOnAction(e -> onSend());

		HBox bottomBar = new HBox(10, inputField, sendButton);
		bottomBar.setPadding(new Insets(10));
		HBox.setHgrow(inputField, Priority.ALWAYS);

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

		// 1) Perform the connect + login synchronously
		try {
			conn = new ClientConnection();
			conn.connect("localhost", 12345);

			// Send LOGIN message
			Message login = new Message(
					MessageType.LOGIN,
					"",
					uname,
					null
			);
			conn.sendMessage(login);
		} catch (IOException ex) {
			showAlert("Unable to connect to server:\n" + ex.getMessage());
			return;
		}

		// 2) Enable UI only after connect succeeded
		usernameField.setDisable(true);
		connectButton.setDisable(true);
		inputField.setDisable(false);
		sendButton.setDisable(false);

		appendToChat(">> Connected as " + uname + "\n");

		// 3) Start reader thread for incoming messages
		readerThread = new Thread(() -> {
			try {
				while (true) {
					Message msg = conn.receiveMessage();
					Platform.runLater(() -> appendToChat(msg.toString() + "\n"));
				}
			} catch (Exception e) {
				Platform.runLater(() -> appendToChat(">> Disconnected from server\n"));
			}
		});
		readerThread.setDaemon(true);
		readerThread.start();
	}

	private void onSend() {
		String text = inputField.getText().trim();
		if (text.isEmpty() || conn == null) return;

		String uname = usernameField.getText().trim();
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
					MessageType.CHAT,
					content,
					uname,
					recipient
			);
		} else {
			msg = new Message(
					MessageType.CHAT,
					text,
					uname,
					null
			);
		}

		try {
			conn.sendMessage(msg);
		} catch (IOException ex) {
			appendToChat(">> Failed to send message: " + ex.getMessage() + "\n");
		}
		inputField.clear();
	}

	private void appendToChat(String text) {
		chatArea.appendText(text);
		chatArea.setScrollTop(Double.MAX_VALUE);
	}

	private void showAlert(String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
		alert.initOwner(usernameField.getScene().getWindow());
		alert.showAndWait();
	}

	public static void main(String[] args) {
		launch(args);
	}
}