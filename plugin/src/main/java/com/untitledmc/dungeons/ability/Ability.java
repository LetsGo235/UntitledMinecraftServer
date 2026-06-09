package com.untitledmc.dungeons.ability;

public interface Ability {
    String getId();

    default String getName() {
        return getId();
    }

    default String getDescription() {
        return "";
    }

    void execute(AbilityContext context);
}
