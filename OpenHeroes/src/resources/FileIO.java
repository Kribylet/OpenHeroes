package resources;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages all file reading. So far, only images. Throws a custom exception
 * to allow callers to build custom error handling for missing or corrupt images.
 * <p>
 * Uses the Java ClassLoader to load files, so we can build .jars easily.
 */
public final class FileIO
{
    private static final Logger LOGGER = Logger.getLogger(FileIO.class.getName());


    private FileIO() {}

    public static BufferedImage readImage(URL url) throws ImageReadException {
	try {
	    return ImageIO.read(url);
	} catch (IOException e) {
	    LOGGER.log(Level.FINE, "Failed to read image from: " + url, e);
	}

	throw new ImageReadException("Failed to read image: " + url);
    }

    public static BufferedImage readImage(String filepath) throws ImageReadException {
	URL imageURL = FileIO.class.getClassLoader().getResource(filepath);

	return readImage(imageURL);
    }
}