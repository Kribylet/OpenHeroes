package entity;

import connection.Message;
import gamemodel.Passability;
import gamemodel.Position;
import gamemodel.Resource;
import gamemodel.Team;

/**
 * ResourceMines are a special type of map feature that generate resources for player teams at the end of each turn.
 * <p>
 * All mines are intended to have the same size and layout on the map, even if their appearance differs.
 * <p>
 * They are constructed simply by providing a Resource enum.
 */
public class ResourceMine extends Interactable
{


    private Resource type;

    public ResourceMine(final Resource type) {
	super(type.getResourceMineImagePath(), new Passability[][] {
		      { Passability.IMPASSABLE, Passability.IMPASSABLE },
		      { Passability.IMPASSABLE, Passability.NORMAL },
		      { Passability.IMPASSABLE, Passability.IMPASSABLE } },
	      new Position(1, 1));
	this.type = type;
    }

    @Override public Message interaction(final Interactable other) {
	if (!isFriendly(other)) {
	    return Message.captureResource(other.getInteractableID(), this.getInteractableID());
	}
	return Message.move(this.getInteractableID());
    }

    @Override public InteractionType leftClick(final Team team) {
	return InteractionType.NONE;
    }

    public void addResources(final int[] resourceList) {
	resourceList[type.ordinal()] += type.getMineAmountPerTurn();
    }
}
