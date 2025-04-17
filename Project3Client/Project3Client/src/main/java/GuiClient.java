

import java.util.Scanner;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

public class GuiClient extends Application{


	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);

		System.out.print("Enter your username: ");
		String uname = s.nextLine();

		Client clientThread = new Client();
		clientThread.username = uname;  // 👈 set username before starting thread
		clientThread.start();

		// Input loop for sending messages
		while (s.hasNext()) {
			String x = s.nextLine();

			// 🧠 Handle @private or public
			if (x.startsWith("@")) {
				String[] parts = x.split(" ", 2);
				if (parts.length < 2) continue;
				String recipient = parts[0].substring(1);
				String msg = parts[1];
				clientThread.send(new Message(uname, recipient, msg));
			} else {
				clientThread.send(new Message(uname, x));
			}
		}
		launch(args);  // JavaFX GUI (currently placeholder)
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		primaryStage.setScene(new Scene(new TextField("I am not yet implemented")));
		primaryStage.setTitle("Client");
		primaryStage.show();
		
	}

}
