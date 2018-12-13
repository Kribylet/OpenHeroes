package entity;

/**
 * Identifies various kinds of UI context interactions, outside of
 * immediate interactable to interactable interactions.
 */

public enum InteractionType
{

    /** NOT IMPLEMENTED:
     * Display terse rightClick about a hostile interactable. Eg. army unit types, name */
    HOSTILE_STATVIEW,
    /** NOT IMPLEMENTED:
     * Display verbose rightClick about a friendly interactable. Eg. Exact army rightClick, inventory */
    OWNER_STATVIEW,
    /** */
    SELECT,
    /** */
    DESELECT,
    /** Attempt to buy a hero from a town. */
    BUY_HERO,
    /** */
    NONE
}


