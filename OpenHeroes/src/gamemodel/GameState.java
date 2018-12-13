package gamemodel;

/**
 * Keeps track of what is going on in the game model.
 *
 * View objects use these enum values to display relevant interface components.
 */
public enum GameState
{
    /** The game model is not loaded */
    MAIN_MENU,
    /** The game is operating on the main map. */
    MAIN_MAP,
    /** -||- */
    BATTLEFIELD,
    /** A client is trading between heroes. This is only known to the server and client in question. */
    TRADE,
    // A client is managing a town. This is only known to the server and client in question.
    // Not implemented yet!
    //TOWN,
    /** The game has concluded on this client. The server reaches this state when someone has won. */
    GAME_OVER
}
