package entity;

/**
 * The various possible base types of town. Each is supposed to represent a unique game faction with
 * its own unique set of units and buildable TownStructures.
 */
public enum TownType
{
    /** The "vanilla" faction. Varied army composition with no obvious drawbacks or focuses.
     * Decent all-around. */
    CASTLE,
    /** Generally slightly weaker army units with regenerative properties. Shadow Bog heroes
     * start with Necromancy, letting them revive a fraction of fallen friendly and enemy
     * army units as basic Shadow Bog units after battle.*/
    SHADOW_BOG,
    /** Cheap, mass-produced melee units early on. Expensive but strong end-game units.
     * Spell focus favors spells that augment your army units. Cave heroes typically
     * have specializations that give strong bonuses when commanding a large army.*/
    CAVE,
    /** Generally expensive units with slightly higher rightClick. Stronger focus on fragile ranged units.
     * Wizard Tower cities can build better spell research structures and tend to be better at
     * researching offensive spells. Their heroes have large reserves of magic power. */
    WIZARD_TOWER,
    /** Fast-moving army units with defensive special effects. They have more flying army units than
     * other Town types. Their heroes tend to start with defensive army boosting skills. Spells focus
     * on weakening enemy army units. */
    ELVEN_TREE,
    /** Slow-moving but tough and strong army units, with retaliatory special effects. Their heroes favor
     *  offensive specializations, but vary from magic to army type focus. Their spells focus on debilitating
     *  and confusing enemy army units.*/
    HELL_PIT
}
