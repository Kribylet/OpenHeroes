package entity;


import connection.Message;
import gamelogic.ExitCode;
import gamemodel.Passability;
import gamemodel.Position;
import gamemodel.Team;
import resources.FileIO;
import resources.ImageReadException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static entity.InteractionType.HOSTILE_STATVIEW;
import static entity.InteractionType.OWNER_STATVIEW;
import static view.HeroesFrame.PIXEL_TILE_SIZE;

/**
 * The Interactable abstract class provides all the expected methods of entities
 * existing on an MoverPathMap.
 * <p>
 * They specify size, passability, and appearance through an image.
 * <p>
 * The most important feature of the Interactable superclass is that it allows
 * generalizations of context sensitive interactions. We implement context actions
 * for when we are left clicked, right clicked, and if we are directly interacted
 * with by another Interactable.
 * <p>
 * Also of note is that all Interactables are provided an unique InteractableID
 * through the superconstructor that allows us to identify it later using the
 * MessageProtocol.
 */
public abstract class Interactable implements Serializable
{
    private final static Logger LOGGER = Logger.getLogger(Interactable.class.getName());
    private Position interactionPointOffset;
    private Position interactionPoint = null;
    private Position position = null;
    private Passability[][] passabilityMap;
    private static int idCounter = 0;
    private final String interactableID;
    private final String imagePath;
    private transient BufferedImage image;
    private Team owner = Team.NEUTRAL;


    protected Interactable(final String imagePath, final Passability[][] passabilityMap,
			   final Position interactionPointOffset)
    {
	this.imagePath = imagePath;
	this.image = getImage();
	this.passabilityMap = passabilityMap;
	this.interactableID = getNewID();
	this.interactionPointOffset = interactionPointOffset;
    }

    public abstract Message interaction(final Interactable other);

    public InteractionType rightClick(final Team team) {
    	if (isFriendly(team)) {
    	    return OWNER_STATVIEW;
    	} else {
    	    return HOSTILE_STATVIEW;
    	}
    }

    protected boolean isFriendly(final Team team) {
   	return this.owner.equals(team);
    }
    protected boolean isFriendly(final Interactable interactable) {
       	return this.owner.equals(interactable.owner);
    }

    public abstract InteractionType leftClick(final Team team);

    private static String getNewID() {
	String stringId = Integer.toString(idCounter);
	idCounter++;
	return stringId;
    }

    public Team getOwner() {
            	return owner;
            }

    public void setOwner(final Team owner) {
        this.owner = owner;
}

    public Position getPosition() {
	return position;
    }

    public String getInteractableID() {
	return interactableID;
    }

    public void setPosition(final Position position) {
	this.position = position;
	this.interactionPoint = position.add(interactionPointOffset);
    }

    public Position getInteractionPoint() {
	return interactionPoint;
    }

    public int getWidth() {
	return passabilityMap.length;
    }

    public int getHeight() {
	return passabilityMap[0].length;
    }

    public BufferedImage getImage() {
	if (image != null) {
	    return image;
	}
	try {
	    image = FileIO.readImage(imagePath);
	    return image;
	} catch (ImageReadException e) {
	    LOGGER.log(Level.SEVERE, "Unable to load image resources.", e);
	    System.exit(ExitCode.IO.ordinal());
	}
	return null;
    }

    public Image getImageSlice(final int x, final int y) {
	BufferedImage image = getImage();
	return image.getSubimage(x * PIXEL_TILE_SIZE, y * PIXEL_TILE_SIZE, PIXEL_TILE_SIZE, PIXEL_TILE_SIZE);
    }

    @Override public boolean equals(final Object o) {
	if (this == o) return true;
	if (o == null || !Objects.equals(getClass(), o.getClass())) return false;
	final Interactable that = (Interactable) o;
	return getWidth() == that.getWidth() && getHeight() == that.getHeight() &&
	       Objects.equals(interactionPointOffset, that.interactionPointOffset) &&
	       Objects.equals(interactionPoint, that.interactionPoint) && Objects.equals(position, that.position) &&
	       Objects.equals(interactableID, that.interactableID);
    }

    @Override public int hashCode() {

	return (int) (interactionPoint.getX() * Integer.parseInt(interactableID) +
		      interactionPoint.getY() * Math.pow(Integer.parseInt(interactableID), 10.0d));
    }

    public boolean interactionPointIsPassable() {
	return !passabilityMap[interactionPointOffset.getX()][interactionPointOffset.getY()].equals(Passability.IMPASSABLE);
    }

    public Passability[][] getPassabilityMap() {
	return passabilityMap;
    }
}
