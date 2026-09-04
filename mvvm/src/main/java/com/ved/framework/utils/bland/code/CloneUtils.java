package com.ved.framework.utils.bland.code;
import com.ved.framework.utils.KLog;
import com.ved.framework.utils.Utils;

import java.lang.reflect.Type;

public final class CloneUtils {

    private CloneUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * Deep clone.
     *
     * @param data The data.
     * @param type The type.
     * @param <T>  The value type.
     * @return The object of cloned.
     */
    public static <T> T deepClone(final T data, final Type type) {
        try {
            return UtilsBridge.fromJson(UtilsBridge.toJson(data), type);
        } catch (Exception e) {
            KLog.e(e.getMessage());
            return null;
        }
    }
}
