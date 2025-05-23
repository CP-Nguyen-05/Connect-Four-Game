package server;
import shared.Message;
import shared.MessageType;
import shared.User;
import shared.Room;
import shared.RoomManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import shared.UdpDiscoveryServer;
import shared.Utils;

import java.io.OutputStream;
import java.io.PrintStream;

public class GuiServer extends Application {
	private TextArea logArea;

	@Override
	public void start(Stage primaryStage) {
		logArea = new TextArea();
		logArea.setPrefSize(600, 400);
		logArea.setEditable(false);

		VBox root = new VBox(10, logArea);
		primaryStage.setTitle("Connect Four - Server");
		primaryStage.setScene(new Scene(root, 600, 400));
		primaryStage.show();

		// Redirect System.out and System.err into the TextArea
		PrintStream ps = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) {
				Platform.runLater(() -> logArea.appendText(String.valueOf((char)b)));
			}
		}, true);
		System.setOut(ps);
		System.setErr(ps);

		int tcpPort = 12345;  // must match Server.SERVER_PORT
		System.out.println("[GuiServer] Starting discovery on UDP " + Utils.DISCOVERY_PORT
				+ " → will reply with TCP port " + tcpPort);
		new Thread(new UdpDiscoveryServer(tcpPort)).start();

		// Kick off the server in a daemon thread
		Thread serverThread = new Thread(() -> {
			try {
				Server.main(new String[0]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
		serverThread.setDaemon(true);
		serverThread.start();
	}

	public static void main(String[] args) {
		launch(args);
	}
}