package entity;

import gamemodel.Passability;
import gamemodel.Position;

/**
 * Subclass of Interactables that are able to move on an MoverPathMap.
 * <p>
 * This abstraction layer will be useful for Battlefield units later, right now
 * only heroes inherit from it.
 */
public abstract class Mover extends Interactable
{
    protected int maxMoveLength;
    protected int remainingMoveLength;

    protected Mover(final String imagePath, final Passability[][] passabilityMap, final Position interactionPointOffset,
		    final int maxMoveLength)
    {
	super(imagePath, passabilityMap, interactionPointOffset);
	this.maxMoveLength = maxMoveLength;
	this.remainingMoveLength = maxMoveLength;
    }

    public void refreshMove() {
	remainingMoveLength = maxMoveLength;
    }

    public int getRemainingMoveLength() {
	return remainingMoveLength;
    }

    public void setRemainingMoveLength(final int remainingMoveLength) {
	this.remainingMoveLength = remainingMoveLength;
    }
}
