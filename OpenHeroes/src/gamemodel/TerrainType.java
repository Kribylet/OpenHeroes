package gamemodel;

import resources.FileIO;
import resources.ImageReadException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enumerator for terrain types. Contains some of the terrain specific information
 * such as appearance and move cost.
 */
public enum TerrainType
{
    /** */
    SEA(Passability.IMPASSABLE, Color.BLUE, "img/sea.png"), /** */
GRASS(Passability.NORMAL, Color.GREEN, "img/grass.png"), /** */
MUD(Passability.ROUGH, new Color(89, 41, 0), "img/mud.png");


    private final Passability passability;
    private final Color color;
    private final boolean useImage;
    private final BufferedImage image;

    /**
     * If the TerrainType constructor fails to load its specified image, it will
     * flag as such, allowing calling functions to draw a primitive shape instead.
     */
    TerrainType(final Passability passability, final Color color, final String imagePath) {

	this.passability = passability;
	this.color = color;
	this.image = loadImage(imagePath);
	useImage = this.image != null;
    }

    private BufferedImage loadImage(String imagePath) {
	try {
	    return FileIO.readImage(imagePath);
	} catch (ImageReadException e) {
	    LOGGER.log(Level.WARNING, "Failed to load terrain image resources. Flagged for primitive rendering.", e);
	    return null;
	}
    }


    public Passability getPassability() {
	return passability;
    }

    public Color getColor() {
	return color;
    }

    public boolean hasImage() {
	return useImage;
    }

    public BufferedImage getImage() {
	return image;
    }

    private static final Logger LOGGER = Logger.getLogger(TerrainType.class.getName());
}

