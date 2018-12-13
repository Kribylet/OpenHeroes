package gamemodel;


/**
 * Exception thrown when a MainMap is found in an erroneous state.
 */
public class InvalidMainMapStateException extends Exception
{
    public InvalidMainMapStateException(final String s) {
	super(s);
    }
}