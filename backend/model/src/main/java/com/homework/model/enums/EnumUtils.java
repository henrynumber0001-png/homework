package com.homework.model.enums;

import java.lang.reflect.Method;

public final class EnumUtils {

    private EnumUtils() {
    }

    public static <E extends Enum<E>> E fromCode(Class<E> enumClass, Object code) {
        if (code == null) {
            return null;
        }
        for (E item : enumClass.getEnumConstants()) {
            try {
                Method method = enumClass.getMethod("getCode");
                Object enumCode = method.invoke(item);
                if (String.valueOf(enumCode).equals(String.valueOf(code))) {
                    return item;
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Enum does not expose getCode(): " + enumClass.getName(), e);
            }
        }
        throw new IllegalArgumentException("Unknown enum code " + code + " for " + enumClass.getSimpleName());
    }
}
