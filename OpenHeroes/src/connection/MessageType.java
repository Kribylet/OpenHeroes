package connection;

/**
 * Enumerators to help classify valid messageTypes. Each messageType declares how many
 * arguments they should be sent with. This is used to perform some rudimentary sanity
 * checks in the message protocol.
 */
public enum MessageType
{
    /** A request from the client to synchronize the GameModel. */
    SYNC(0),
    /** A command from the server to resynchronize the GameModel. */
    SYNC_SENT(0),
    /** Due notice to the receiver that this end of the connection intends to close. */
    STOP(0),
    /** Acknowledgement from any side of the connection that they are about to shut down. */
    STOP_ACK(0),
    /** A request to move the hero. Tentative if from a client, authorative if from the server. */
    MOVE_HERO(2),
    /** A client requesting a connection ID from the server, as well as an initial GameModel. */
    HANDSHAKE(0),
    /** A server command telling a client to read a new battlefield map. */
    GET_BATTLEFIELD(0),
    /** A request to end the current turn. Tentative if from a client, authorative if from the server. */
    TURN_END(0),
    /** A request to fight a map entity. Tentative if from a client, authorative if from the server. */
    FIGHT_MOB(0),
    /** Serverside declaration that a hero was defeated. */
    HERO_DEFEATED(1),
    /** A request to capture a map resource feature. Tentative if from a client, authorative if from the server. */
    CAPTURE_RESOURCE(2),
    /** A request to attack a town. Tentative if from a client, authorative if from the server. */
    TOWN_BATTLE(2),
    /** A client wants to send a hero into a friendly town . */
    TOWN_INTERACT(2),
    /** Serverside declaration that a town was captured. */
    TOWN_CAPTURED(2),
    /** A request to attack another hero. Tentative if from a client, authorative if from the server. */
    HERO_BATTLE(2),
    /** A request to trade with another hero. Tentative if from a client, authorative if from the server. */
    HERO_TRADE(2),
    /** Informs the server that the client is done trading between heroes. */
    TRADE_CONCLUDED(0),
    /** A serverside declaration that a team was defeated. */
    TEAM_DEFEATED(1),
    /** A serverside declaration that a team has won the game. */
    VICTORY(1),
    /** An assignment of connection ID from the server. */
    ASSIGN_ID(2),
    /** End a battlefield turn. */
    END_BATTLE_TURN(0),
    /** Surrender the game. */
    SURRENDER(0),
    /** Error Message sent by a TCPListener when it has lost connection. */
    CONNECTION_LOST(0),
    /** Notify a client that another client has lost connection. */
    PLAYER_DISCONNECTED(1),
    /** Buy a new hero */
    BUY_HERO(1),
    /** Read a new hero from the server */
    ADD_HERO(2);

    private final int expectedArguments;

    MessageType(final int expectedArguments) {
	this.expectedArguments = expectedArguments;
    }

    public int getExpectedArguments() {
	return expectedArguments;
    }
}
