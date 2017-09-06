package core.framework.impl.db;

import core.framework.api.db.DBEnumValue;
import core.framework.api.util.Exceptions;
import core.framework.api.util.Maps;
import core.framework.impl.reflect.Classes;

import java.util.Map;

/**
 * @author neo
 */
final class DBEnumMapper<T extends Enum<T>> {
    private final Class<T> enumClass;
    private final Map<String, T> mappings;

    DBEnumMapper(Class<T> enumClass) {
        this.enumClass = enumClass;
        mappings = mappings(enumClass);
    }

    // used by generated code, must be public
    public T getEnum(String value) {
        if (value == null) return null;
        T enumValue = mappings.get(value);
        if (enumValue == null)
            throw Exceptions.error("can not parse value to enum, enumClass={}, value={}", enumClass.getCanonicalName(), value);
        return enumValue;
    }

    private Map<String, T> mappings(Class<T> enumClass) {
        T[] constants = enumClass.getEnumConstants();
        Map<String, T> mapping = Maps.newHashMapWithExpectedSize(constants.length);
        for (T constant : constants) {
            String dbValue = Classes.enumValueAnnotation(enumClass, constant, DBEnumValue.class).value();
            mapping.put(dbValue, constant);
        }
        return mapping;
    }
}
