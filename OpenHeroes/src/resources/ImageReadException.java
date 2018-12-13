package resources;

/**
 * Exception thrown when unable to parse an image file.
 */
public class ImageReadException extends Exception
{
    public ImageReadException(final String s) {
	super(s);
    }
}