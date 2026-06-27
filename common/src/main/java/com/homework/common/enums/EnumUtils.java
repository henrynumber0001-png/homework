package com.homework.common.enums;

import java.lang.reflect.Method;

public final class EnumUtils {

    private EnumUtils() {
    }

    public static <E extends Enum<E>> E fromValue(Class<E> enumClass, Object value) {
        if (value == null) {
            return null;
        }
        for (E item : enumClass.getEnumConstants()) {
            try {
                Method method = enumClass.getMethod("getValue");
                Object enumValue = method.invoke(item);
                if (String.valueOf(enumValue).equals(String.valueOf(value))) {
                    return item;
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Enum does not expose getValue(): " + enumClass.getName(), e);
            }
        }
        throw new IllegalArgumentException("Unknown enum value " + value + " for " + enumClass.getSimpleName());
    }
}
