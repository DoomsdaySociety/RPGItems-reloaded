package think.rpgitems.utils.nyaacore.configuration;

import java.util.Optional;

public interface Setter<T> {

    static <T extends ISerializable> Setter<T> from(T p, Class<? extends Setter<T>> cls) {
        return Getter.getAccessor(p, cls);
    }

    /**
     * @param value String representation of the object
     * @return The object to be set to field, or empty if field are already set by this setter
     * @throws IllegalArgumentException {@code value} is not a valid representation of the object
     */
    Optional<T> set(Object value) throws IllegalArgumentException;
}
