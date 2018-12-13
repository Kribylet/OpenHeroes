import gamelogic.ExitCode;
import resources.ImageReadException;
import view.HeroesFrame;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simply starts the game.
 */
public final class OpenHeroes
{

    private OpenHeroes() {}

    private static final Logger LOGGER = Logger.getLogger(OpenHeroes.class.getName());

    public static void main(String[] args) {

	try {
	    HeroesFrame heroesFrame = new HeroesFrame();
	    heroesFrame.start();
	} catch (ImageReadException e) {
	    LOGGER.log(Level.SEVERE, "Unable to read game graphics resources!", e);
	    System.exit(ExitCode.IO.ordinal());
	}
    }
}