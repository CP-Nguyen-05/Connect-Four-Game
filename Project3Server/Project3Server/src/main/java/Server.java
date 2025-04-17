import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;


import javafx.application.Platform;
import javafx.scene.control.ListView;


public class Server{

	int count = 1;	
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;
	
	
	Server(){

		server = new TheServer();
		server.start();
	}
	
	
	public class TheServer extends Thread{
		
		public void run() {
		
			try(ServerSocket mysocket = new ServerSocket(5555);){
		    System.out.println("Server is waiting for a client!");
		  
			
		    while(true) {
		
				ClientThread c = new ClientThread(mysocket.accept(), count);
				clients.add(c);
				c.start();
				
				count++;
				
			    }
			} catch(Exception e) {
					System.err.println("Server did not launch");
				}
			}
		}
	

		class ClientThread extends Thread{
			
		
			Socket connection;
			int count;
			ObjectInputStream in;
			ObjectOutputStream out;
			String username;

			
			ClientThread(Socket s, int count){
				this.connection = s;
				this.count = count;	
			}

			public void updateClients(Message message) {
				System.out.println(message);  // Optional: still log on server

				if (message.recipientUser == null || message.recipientUser.isEmpty()) {
					// Broadcast message to all clients except sender
					for (ClientThread client : clients) {
						if (client != this) {
							client.send(message);
						}
					}
				} else {
					// Private message
					boolean found = false;

					for (ClientThread client : clients) {
						if (client.username != null && client.username.equals(message.recipientUser)) {
							client.send(message);
							found = true;
							break;
						}
					}

					if (!found) {
						// Inform the sender the user doesn't exist
						this.send(new Message("Server", message.sender, "User '" + message.recipientUser + "' does not exist or is not connected."));
					}
				}
			}

			public void send(Message message){
                try {
                    out.writeObject(message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
			public void run() {
				try {
					in = new ObjectInputStream(connection.getInputStream());
					out = new ObjectOutputStream(connection.getOutputStream());
					connection.setTcpNoDelay(true);

					username = (String) in.readObject();
					System.out.println("Client #" + count + " set username to: " + username);

					updateClients(new Message("Server", username + " has joined the chat."));

				} catch (Exception e) {
					System.err.println("Failed to set up streams or receive username.");
					return;
				}

				while (true) {
					try {
						Message data = (Message) in.readObject();
						updateClients(data);
					} catch (Exception e) {
						// ⛔ Handle disconnect or error gracefully
						System.out.println("Client '" + username + "' disconnected.");
						updateClients(new Message("Server", username + " has left the chat."));
						clients.remove(this);
						try {
							connection.close();
						} catch (IOException ioException) {
							ioException.printStackTrace();
						}
						break;
					}
				}
			}//end of run
			
			
		}//end of client thread
}


	
	

	
