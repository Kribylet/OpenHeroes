package gamemodel;


/**
 * Exception thrown if an unsupported amount of players is suggested
 * to the GameModelFactory.
 */
public class UnsupportedPlayerAmountException extends Exception
{
    public UnsupportedPlayerAmountException(final String s) {
	super(s);
    }
}