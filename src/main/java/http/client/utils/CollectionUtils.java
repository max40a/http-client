package http.client.utils;

import java.util.Collection;

import static java.util.Objects.isNull;

public class CollectionUtils {

    private CollectionUtils() {
        throw new UnsupportedOperationException("Utils class");
    }

    public static boolean isEmpty(Collection<?> collection) {
        return isNull(collection) || collection.isEmpty();
    }
}
