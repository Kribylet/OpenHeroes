package entity;

import connection.Message;
import gamemodel.Passability;
import gamemodel.Position;
import gamemodel.Team;

/**
 * Towns are crucial game entities. Players must control at least one in order to avoid defeat.
 * In the fuller scope of the project, towns contain TownStructures, which afford them additional
 * functionality such as recruiting army units, researching spells and generating additional resources.
 * <p>
 * As of right now, it allows you to recruit a new hero by clicking on its gate, provided you have the
 * gold for it.
 */
public class Town extends Interactable
{

    private final int[] resourceGeneration;

    protected Town(final String imagePath, final int[] resourceGeneration) {
	super(imagePath, new Passability[][] {
		      { Passability.IMPASSABLE, Passability.IMPASSABLE },
		      { Passability.IMPASSABLE, Passability.NORMAL },
		      { Passability.IMPASSABLE, Passability.IMPASSABLE } },
	      new Position(1, 1));
	this.resourceGeneration = resourceGeneration;
    }

    @Override public InteractionType leftClick(final Team team) {
	if (isFriendly(team)) {
	    return InteractionType.BUY_HERO;
	} else {
	    return InteractionType.NONE;
	}
    }

    @Override public Message interaction(final Interactable other) {
	if (isFriendly(other)) {
	    return Message.townInteract(other.getInteractableID(), this.getInteractableID());
	} else {
	    return Message.townBattle(other.getInteractableID(), this.getInteractableID());
	}
    }

    public void addResources(final int[] resourceList) {
	for (int i = 0; i < resourceGeneration.length; i++) {
	    resourceList[i] += resourceGeneration[i];
	}
    }
}
