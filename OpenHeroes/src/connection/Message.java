package connection;

import entity.Interactable;
import gamemodel.Team;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The message class standardizes the communication packets sent between the server and client.
 * <p>
 * Header: A string-formatted version of the MessageType enum.
 * <p>
 * Sender: The string ID of the sending interface.
 * <p>
 * Hash: Clients must send a copy of their GameModel hash with their message so that the server can validate it.
 * Clients do not attempt to validate the server GameModel hash.
 * <p>
 * Args: Message-specific arguments that inform the handling of that event. InteractableIDs, positions, etc.
 * Properly decoding these arguments is left to the message receiver, based on the MessageType.
 * <p>
 * When communicating by Message, it's best to use the pre-defined static message functions to ensure
 * proper message structure. For this reason, the message constructor itself is private and static
 * methods are provided to build properly formatted Messages.
 */
public final class Message
{
    private static final Logger LOGGER = Logger.getLogger(Message.class.getName());
    private MessageType header;
    private Team sender = null;
    private String hash = "0";
    private String[] args;

    private Message(final MessageType header, final String... args) {
	this.header = header;
	this.args = args;
	LOGGER.log(Level.FINEST, "Constructed a new message " + this);
    }

    private Message(final MessageType header, final Team sender, final String hash, final String[] args) {
	this.header = header;
	this.sender = sender;
	this.hash = hash;
	this.args = args;
	LOGGER.log(Level.FINEST, "Constructed a new message " + this);
    }

    public static Message parseMessage(String message) {

	    String[] messageParts = message.split(";");

	    for (MessageType messagetype : MessageType.values()) {
		if (messagetype.name().equals(messageParts[0])) {
		    Message constructedMessage = new Message(messagetype,
						     Team.valueOf(messageParts[1]),
						     messageParts[2],
						     Arrays.copyOfRange(messageParts, 3, messageParts.length));

		    assert constructedMessage.getArgs().length != messagetype.getExpectedArguments();
		    return constructedMessage;
		}
	    }
	    return null;
    }

    public MessageType getHeader() {
	return header;
    }

    public Team getSender() {
	return sender;
    }

    public String getHash() {
	return hash;
    }

    public String[] getArgs() {
	return args;
    }

    @Override public String toString() {
	StringBuilder sb = new StringBuilder();

	sb.append("[").append(header.name()).append(";").append(sender).append(";").append(hash);

	for (final String arg : args) {
	    sb.append(";").append(arg);
	}

	return sb.append("]").toString();
    }

    public void rebrand(Team newSender) {
	LOGGER.log(Level.FINEST, "Rebranding " + this);
	this.sender = newSender;
	LOGGER.log(Level.FINEST, "Rebranded to " + this);
    }

    /* Convenience methods for translating data into message arguments. */

    public static String translate(int integer) {
	return Integer.toString(integer);
    }

    public void updateHash(final int newHash) {
	this.hash = translate(newHash);
    }

    public static Message move( final String heroID) {
	return new Message(MessageType.MOVE_HERO, heroID);
    }

    public static Message heroBattle( final String actorID, final String targetID)
    {
	return new Message(MessageType.HERO_BATTLE, actorID, targetID);
    }

    public static Message heroTrade( final String actorID, final String targetID)
    {
	return new Message(MessageType.HERO_TRADE, actorID, targetID);
    }

    public static Message captureResource( final String heroID, final String resourceMineID)
    {
	return new Message(MessageType.CAPTURE_RESOURCE, heroID, resourceMineID);
    }

    public static Message townBattle( final String heroID, final String townID)
    {
	return new Message(MessageType.TOWN_BATTLE, heroID, townID);
    }

    public static Message townInteract( final String heroID, final String townID)
    {
	return new Message(MessageType.TOWN_INTERACT, heroID, townID);
    }

    public static Message assignID(final Team team) {
	return new Message(MessageType.ASSIGN_ID, team.name());
    }

    public static Message endBattle() {
	return new Message(MessageType.END_BATTLE_TURN);
    }

    public static Message tradeConcluded() {
	return new Message(MessageType.TRADE_CONCLUDED);
    }

    public static Message sync() {
	return new Message(MessageType.SYNC);
    }

    public static Message syncSent() {
	return new Message(MessageType.SYNC_SENT);
    }

    public static Message stop() {
	return new Message(MessageType.STOP);
    }

    public static Message stopAck() {
	return new Message(MessageType.STOP_ACK);
    }

    public static Message handshake(final String playerName) {
	return new Message(MessageType.HANDSHAKE, playerName);
    }

    public static Message getBattlefield() {
	return new Message(MessageType.GET_BATTLEFIELD);
    }

    public static Message endTurn() {
	return new Message(MessageType.TURN_END);
    }

    public static Message surrender() {
	return new Message(MessageType.SURRENDER);
    }

    public static Message teamDefeated( final Team defeatedTeam) {
	return new Message(MessageType.TEAM_DEFEATED, defeatedTeam.name());
    }

    public static Message heroDefeated( final String heroID) {
	return new Message(MessageType.HERO_DEFEATED, heroID);
    }

    public static Message connectionLost() {
        return new Message(MessageType.CONNECTION_LOST);
    }

    public static Message playerDisconnected(final Team team) {
        return new Message(MessageType.PLAYER_DISCONNECTED, team.name());
    }

    public static Message buyHero(final Interactable interactable) {
        return new Message(MessageType.BUY_HERO, interactable.getInteractableID());
    }

    public static Message addHero(final Team team, final String townID) {
        return new Message(MessageType.ADD_HERO, team.name(), townID);
    }
}
