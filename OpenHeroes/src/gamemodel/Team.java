package gamemodel;

import gamelogic.ExitCode;
import resources.FileIO;
import resources.ImageReadException;

import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enumerator containing the game resources associated with specific teams.
 */
public enum Team
{

    /** */
    TEAM_1("img/team1_flag.png"),
    /** */
    TEAM_2("img/team2_flag.png"),
    /** */
    TEAM_3("img/team3_flag.png"),
    /** */
    TEAM_4("img/team4_flag.png"),
    /** */
    TEAM_5("img/team5_flag.png"),
    /** */
    TEAM_6("img/team6_flag.png"),
    /** */
    TEAM_7("img/team7_flag.png"),
    /** */
    TEAM_8("img/team8_flag.png"),
    /** */
    NEUTRAL("img/neutral_flag.png");

    private final BufferedImage flagImage;

    Team(final String imagePath) {
	this.flagImage = loadImage(imagePath);
    }

    public BufferedImage getFlagImage() {
	return flagImage;
    }

    private static final Logger LOGGER = Logger.getLogger(Team.class.getName());

    private BufferedImage loadImage(String imagePath) {
	try {
	    return FileIO.readImage(imagePath);
	} catch (ImageReadException e) {
	    LOGGER.log(Level.SEVERE, "Failed to read image. Unable to recover.", e);
	    System.exit(ExitCode.IO.ordinal());
	}
	LOGGER.log(Level.SEVERE, "Failed to load image and failed to call system.exit in Team enum image read.");
	System.exit(ExitCode.IO.ordinal());
	return null;
    }
}