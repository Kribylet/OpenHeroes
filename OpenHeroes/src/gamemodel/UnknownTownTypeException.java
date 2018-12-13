package gamemodel;


/** Exception thrown when TownFactory is offered unknown TownType values.*/
public class UnknownTownTypeException extends Exception
{
    public UnknownTownTypeException(final String s) {
	super(s);
    }
}