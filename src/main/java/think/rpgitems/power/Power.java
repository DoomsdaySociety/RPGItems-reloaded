package think.rpgitems.power;

import org.bukkit.configuration.ConfigurationSection;
import think.rpgitems.item.RPGItem;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base interface for all powers
 */
public interface Power {
    /**
     * Loads configuration for this power
     *
     * @param s Configuration
     */
    void init(ConfigurationSection s);

    /**
     * Saves configuration for this power
     *
     * @param s Configuration
     */
    void save(ConfigurationSection s);

    /**
     * Name of this power
     *
     * @return name
     */
    String getName();

    /**
     * Display text of this power
     *
     * @return Display text
     */
    String displayText();

    /**
     * Item it belongs to
     */
    RPGItem getItem();

    void setItem(RPGItem item);

    Set<TriggerType> getTriggers();

    default void deinit() {
    }

    @SuppressWarnings("unchecked")
    static Set<TriggerType> getTriggerTypes(Class<? extends Power> cls) {
        return Arrays.stream(cls.getInterfaces())
                     .filter(Power.class::isAssignableFrom)
                     .filter(i -> !Objects.equals(i, Power.class))
                     .map(i -> TriggerType.fromInterface((Class<? extends Power>) i))
                     .collect(Collectors.toSet());
    }
}
