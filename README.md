# 🎮 Connect Four Game

A **multiplayer Connect Four application** built with **Java**, **JavaFX**, **Maven**, and **CSS**.  
Players can **connect via a local server**, **chat during matches**, **rematch instantly**, and **track top scores** on a leaderboard.  
You can also **practice against a computer opponent** to hone your skills!

> **Multiplayer-first:** Multiple game rooms can run simultaneously, each hosting two players connected through a shared `localhost` server.

---

## ✨ Features

- 🧑‍🤝‍🧑 **Multiplayer Rooms**
  - Supports multiple matches running at the same time.
  - Each room is isolated with its own game and chat session.
- 💬 **In-Game Chat**
  - Real-time chat messaging between players in the same room.
  - Messages transmitted over socket-based communication.
- 🔁 **Rematch System**
  - Players can quickly rematch after finishing a game.
- 🧠 **Single-Player Mode**
  - Practice mode lets you play against the built-in computer AI.
- 🏆 **Leaderboard**
  - Tracks wins and displays the **Top 10 players** on the server.
  - Persistent score storage (`players.txt`) for replay sessions.
- 🎨 **JavaFX Interface**
  - Clean, responsive UI with FXML scenes and CSS styling.
- ⚙️ **Maven Build**
  - Independent client and server projects with fully managed dependencies.

---

## 🧰 Tech Stack

| Component | Technology / Library |
|------------|----------------------|
| **Language** | Java 8+ |
| **Framework** | JavaFX (Controls, FXML) |
| **Build Tool** | Apache Maven |
| **Networking** | Java Socket Programming (Multithreaded) |
| **Testing** | JUnit 5.6.2 |
| **Plugin** | `org.openjfx:javafx-maven-plugin (v0.0.8)` |
| **Styling** | CSS (JavaFX stylesheets) |
| **Persistence** | Text-based player storage (`PlayerData/players.txt`) |

---

## 📦 Dependencies (Managed by Maven)

| Dependency | Purpose |
|-------------|----------|
| **org.openjfx:javafx-controls (v23)** | JavaFX UI components |
| **org.openjfx:javafx-fxml (v12)** | FXML layout loading |
| **org.junit.jupiter:junit-jupiter (v5.6.2)** | Unit testing |
| **org.openjfx:javafx-maven-plugin (v0.0.8)** | Enables `mvn javafx:run` command |

---

## 🏗 Project Structure

### 🧩 Client: `Project3Client`
```
Project3Client/
├─ src/
│ └─ main/java/
| │ ├─ Controller/
| │ │ ├─ LoginController.java
| │ │ ├─ ProfileController.java
| │ │ ├─ RegisterController.java
| │ │ ├─ RoomController.java
| │ │ └─ RoomView.java
| │ │
| │ ├─ shared/
| | │ ├─ Client.java
| │ │ ├─ ClientConnection.java
| │ │ ├─ ConnectFourApp.java # Entry point for client
| │ │ ├─ Message.java
| │ │ ├─ MessageType.java
| │ │ └─ User.java
│
├─ src/main/resources/
│ ├─ backgrounds
│ ├─ fonts
│ └─ styles  
│
└─ pom.xml
```

### 🖥 Server: `Project3Server`
```
Project3Server/
├─ PlayerData/
│ └─ players.txt # Persistent leaderboard data
│
├─ src/
│ └─ main/java/
| │ ├─ server/
| │ │ ├─ ConnectionHandler.java
| │ │ ├─ GameSession.java
| │ │ ├─ GuiServer.java
| │ │ ├─ Server.java
| │ │ ├─ ServerMain.java # Server entry point
| │ │ ├─ UserDataStore.java
| │ │ └─ UserService.java
| │ │
| │ └─ shared/
| | │ ├─ Message.java
| | │ ├─ MessageType.java
| | │ ├─ Room.java
| | │ ├─ RoomManager.java
| | │ └─ User.java
│
└─ pom.xml
```
🧠 Key Components
| Component                       | Description                                                       |
| ------------------------------- | ----------------------------------------------------------------- |
| **ServerMain / GuiServer**      | Initializes the server and shows live status (connections, rooms) |
| **Server.java**                 | Accepts new socket connections and starts handler threads         |
| **ConnectionHandler.java**      | Handles communication between server and a specific client        |
| **RoomManager.java**            | Manages multiple active game rooms                                |
| **GameSession.java**            | Executes Connect Four gameplay logic and win detection            |
| **UserService / UserDataStore** | Updates leaderboard and stores player data                        |
| **ClientConnection.java**       | Sends and receives messages to/from the server                    |
| **Controllers**                 | Manage JavaFX UI interactions (login, profile, room, etc.)        |

🖼 Demo Screenshots
| Login Screen            | Home Screen             | Rooms                   | Waiting Room            | Game Screen             |
| ----------------------- | ----------------------- | ----------------------- | ----------------------- | ----------------------- |
| <img width="1199" height="674" alt="login" src="https://github.com/user-attachments/assets/dde11944-c8e0-4d97-af84-8668b30b686c" />| *(Add screenshot here)* | *(Add screenshot here)* | *(Add screenshot here)* | *(Add screenshot here)* |

