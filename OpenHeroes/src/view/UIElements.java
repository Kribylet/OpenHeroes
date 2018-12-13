package view;

import gamelogic.ExitCode;
import resources.FileIO;
import resources.ImageReadException;

import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Graphics resources used to render the user interface.*/
public enum UIElements
{
    /** A green dot for paths. */
    STEP("img/pathmarker.png"),
    /** A green cross for travel destinations. */
    END("img/pathmarker_end.png"),
    /** A red dot for currently unreachable paths. */
    NM_STEP("img/pathmarker_nm.png"),
    /** A red cross for a currently unreachable end destination. */
    NM_END("img/pathmarker_end_nm.png"),
    /** A frame for showing that a hero is selected. */
    SEL_FRAME("img/selection_frame.png"),
    /** A repeatable wooden background panel. */
    BG_IMAGE("img/wood2_bg_offset.png");


    private final BufferedImage mapImage;

    UIElements(final String imagePath) {
	this.mapImage = loadImage(imagePath);
    }

    private static final Logger LOGGER = Logger.getLogger(UIElements.class.getName());

    private BufferedImage loadImage(String imagePath) {
	try {
	    return FileIO.readImage(imagePath);
	} catch (ImageReadException e) {
	    LOGGER.log(Level.SEVERE, "Failed to read image.", e);
	    System.exit(ExitCode.IO.ordinal());
	}
	LOGGER.log(Level.SEVERE, "Failed to read image and failed to exit from ImageReadException. Pigs fly message.");
	return null;
    }

    public BufferedImage getMapImage() {
	return mapImage;
    }
}