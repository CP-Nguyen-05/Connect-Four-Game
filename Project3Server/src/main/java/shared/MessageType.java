package shared;
public enum MessageType {
    LOGIN,       // client → server: authenticate
    LOGIN_SUCCESS,
    REGISTER,    // client → server: create account
    DELETE_ACCOUNT,
    DELETE_ACCOUNT_SUCCESS,
    CREATE_ROOM, // client → server: create a new game room
    ROOM_CREATED,
    JOIN_ROOM,   // client → server: join a specific room
    QUICK_JOIN,  // client → server: join any open room
    LIST_ROOMS,      // client → server: “send me all open rooms”
    ROOM_LIST,        // server → client: here’s your serialized list of RoomView
    SPECTATE,    // client → server: watch a full room
    MOVE,        // client → server: drop a disc in a column
    CHAT,        // client ⇄ server: chat text
    GAME_START,  // server → clients: game is beginning
    SURRENDER,   // client → server: I give up
    GAME_END,    // server → clients: game over
    ERROR        // server → client: error or invalid request
}