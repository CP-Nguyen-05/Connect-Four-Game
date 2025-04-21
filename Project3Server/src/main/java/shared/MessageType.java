package shared;
public enum MessageType {
    LOGIN,       // client → server: authenticate
    REGISTER,    // client → server: create account
    DELETE_ACCOUNT,
    CREATE_ROOM, // client → server: create a new game room
    JOIN_ROOM,   // client → server: join a specific room
    QUICK_JOIN,  // client → server: join any open room
    SPECTATE,    // client → server: watch a full room
    MOVE,        // client → server: drop a disc in a column
    CHAT,        // client ⇄ server: chat text
    GAME_START,  // server → clients: game is beginning
    GAME_END,    // server → clients: game over
    ERROR        // server → client: error or invalid request
}