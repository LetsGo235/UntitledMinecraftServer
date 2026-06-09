package com.untitledmc.dungeons.ability;

import com.untitledmc.dungeons.ability.impl.FlameDashAbility;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AbilityRegistry {
    private final Map<String, Ability> abilities = new HashMap<>();

    public void registerDefaults() {
        register(new FlameDashAbility());
    }

    public void register(Ability ability) {
        abilities.put(normalize(ability.getId()), ability);
    }

    public Optional<Ability> getAbility(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(abilities.get(normalize(id)));
    }

    private String normalize(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}
