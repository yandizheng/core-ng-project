package core.framework.impl.mongo;

import core.framework.impl.asm.CodeBuilder;
import core.framework.impl.asm.DynamicInstanceBuilder;
import core.framework.impl.reflect.Classes;
import core.framework.impl.reflect.GenericTypes;
import core.framework.mongo.Id;
import core.framework.util.Maps;
import core.framework.util.Types;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

import static core.framework.impl.asm.Literal.type;
import static core.framework.impl.asm.Literal.variable;

/**
 * @author neo
 */
final class EntityDecoderBuilder<T> {
    final DynamicInstanceBuilder<EntityDecoder<T>> builder;
    private final Class<T> entityClass;
    private final Map<Class<? extends Enum<?>>, String> enumCodecFields = Maps.newHashMap();
    private final Map<Type, String> decodeMethods = Maps.newHashMap();
    private int index;

    EntityDecoderBuilder(Class<T> entityClass) {
        this.entityClass = entityClass;
        builder = new DynamicInstanceBuilder<>(EntityDecoder.class, EntityDecoder.class.getCanonicalName() + "$" + entityClass.getSimpleName());
    }

    public EntityDecoder<T> build() {
        builder.addField("private final {} logger = {}.getLogger({});", type(Logger.class), type(LoggerFactory.class), variable(EntityDecoder.class));
        String methodName = decodeEntityMethod(entityClass);
        CodeBuilder builder = new CodeBuilder()
                .append("public Object decode(org.bson.BsonReader reader) {\n")
                .indent(1).append("{} wrapper = new {}(reader);\n", type(BsonReaderWrapper.class), type(BsonReaderWrapper.class))
                .indent(1).append("return {}(reader, wrapper, {});\n", methodName, variable(""))
                .append("}");
        this.builder.addMethod(builder.build());
        return this.builder.build();
    }

    private String decodeEntityMethod(Class<?> entityClass) {
        String methodName = decodeMethods.get(entityClass);
        if (methodName != null) return methodName;

        methodName = "decode" + entityClass.getSimpleName() + (index++);
        CodeBuilder builder = new CodeBuilder();
        builder.append("public {} {}(org.bson.BsonReader reader, {} wrapper, String parentField) {\n", type(entityClass), methodName, type(BsonReaderWrapper.class))
               .indent(1).append("boolean hasContent = wrapper.startReadEntity(parentField);\n")
               .indent(1).append("if (!hasContent) return null;\n")
               .indent(1).append("{} entity = new {}();\n", type(entityClass), type(entityClass))
               .indent(1).append("reader.readStartDocument();\n")
               .indent(1).append("while (reader.readBsonType() != org.bson.BsonType.END_OF_DOCUMENT) {\n")
               .indent(2).append("String fieldName = reader.readName();\n")
               .indent(2).append("String fieldPath = parentField + \".\" + fieldName;\n");

        for (Field field : Classes.instanceFields(entityClass)) {
            builder.indent(2).append("if ({}.equals(fieldName)) {\n", variable(mongoField(field)));

            String variable = decodeValue(builder, field.getGenericType(), 3);
            builder.indent(3).append("entity.{} = {};\n", field.getName(), variable);

            builder.indent(3).append("continue;\n")
                   .indent(2).append("}\n");
        }

        builder.indent(2).append("logger.warn({}, fieldPath, reader.getCurrentBsonType());\n", variable("undefined field, field={}, type={}"));
        builder.indent(2).append("reader.skipValue();\n");
        builder.indent(1).append("}\n");

        builder.indent(1).append("reader.readEndDocument();\n");
        builder.indent(1).append("return entity;\n");
        builder.append('}');
        this.builder.addMethod(builder.build());

        decodeMethods.put(entityClass, methodName);
        return methodName;
    }

    private String mongoField(Field field) {
        if (field.isAnnotationPresent(Id.class)) return "_id";
        return field.getDeclaredAnnotation(core.framework.mongo.Field.class).name();
    }

    private String decodeMapMethod(Class<?> valueClass) {
        String methodName = decodeMethods.get(Types.map(String.class, valueClass));
        if (methodName != null) return methodName;

        methodName = "decodeMap" + valueClass.getSimpleName() + (index++);
        CodeBuilder builder = new CodeBuilder();
        builder.append("private java.util.Map {}(org.bson.BsonReader reader, {} wrapper, String parentField) {\n", methodName, type(BsonReaderWrapper.class))
               .indent(1).append("java.util.Map map = wrapper.startReadMap(parentField);\n")
               .indent(1).append("if (map == null) return null;\n")
               .indent(1).append("reader.readStartDocument();\n")
               .indent(1).append("while (reader.readBsonType() != org.bson.BsonType.END_OF_DOCUMENT) {\n")
               .indent(2).append("String fieldName = reader.readName();\n")
               .indent(2).append("String fieldPath = parentField + \".\" + fieldName;\n");

        String variable = decodeValue(builder, valueClass, 2);
        builder.indent(2).append("map.put(fieldName, {});\n", variable);

        builder.indent(1).append("}\n")
               .indent(1).append("reader.readEndDocument();\n")
               .indent(1).append("return map;\n")
               .append('}');
        this.builder.addMethod(builder.build());

        decodeMethods.put(Types.map(String.class, valueClass), methodName);
        return methodName;
    }

    private String decodeListMethod(Class<?> valueClass) {
        String methodName = decodeMethods.get(Types.list(valueClass));
        if (methodName != null) return methodName;

        methodName = "decodeList" + valueClass.getSimpleName() + (index++);
        CodeBuilder builder = new CodeBuilder();
        builder.append("private java.util.List {}(org.bson.BsonReader reader, {} wrapper, String fieldPath) {\n", methodName, type(BsonReaderWrapper.class));
        builder.indent(1).append("java.util.List list = wrapper.startReadList(fieldPath);\n")
               .indent(1).append("if (list == null) return null;\n");
        builder.indent(1).append("reader.readStartArray();\n");
        builder.indent(1).append("while (reader.readBsonType() != org.bson.BsonType.END_OF_DOCUMENT) {\n");

        String variable = decodeValue(builder, valueClass, 2);
        builder.indent(2).append("list.add({});\n", variable);

        builder.indent(1).append("}\n");
        builder.indent(1).append("reader.readEndArray();\n");
        builder.indent(1).append("return list;\n");
        builder.append('}');
        this.builder.addMethod(builder.build());

        decodeMethods.put(Types.list(valueClass), methodName);
        return methodName;
    }

    private String decodeValue(CodeBuilder builder, Type valueType, int indent) {
        Class<?> valueClass = GenericTypes.rawClass(valueType);
        String variable = "$" + (index++);
        builder.indent(indent);
        if (Integer.class.equals(valueType)) {
            builder.append("{} {} = wrapper.readInteger(fieldPath);\n", type(valueType), variable);
        } else if (String.class.equals(valueType)) {
            builder.append("{} {} = wrapper.readString(fieldPath);\n", type(valueType), variable);
        } else if (Long.class.equals(valueType)) {
            builder.append("{} {} = wrapper.readLong(fieldPath);\n", type(valueType), variable);
        } else if (LocalDateTime.class.equals(valueType)) {
            builder.append("{} {} = wrapper.readLocalDateTime(fieldPath);\n", type(valueType), variable);
        } else if (ZonedDateTime.class.equals(valueType)) {
            builder.append("{} {} = wrapper.readZonedDateTime(fieldPath);\n", type(valueType), variable);
        } else if (GenericTypes.rawClass(valueType).isEnum()) {
            String enumCodecVariable = registerEnumCodec(GenericTypes.rawClass(valueType));
            builder.append("{} {} = ({}) {}.read(reader, fieldPath);\n", type(valueType), variable, type(valueType), enumCodecVariable, type(valueType));
        } else if (Double.class.equals(valueType)) {
            builder.append("{} {} = wrapper.readDouble(fieldPath);\n", type(valueType), variable);
        } else if (ObjectId.class.equals(valueType)) {
            builder.append("{} {} = wrapper.readObjectId(fieldPath);\n", type(valueType), variable);
        } else if (Boolean.class.equals(valueType)) {
            builder.append("{} {} = wrapper.readBoolean(fieldPath);\n", type(valueType), variable);
        } else if (GenericTypes.isGenericList(valueType)) {
            String method = decodeListMethod(GenericTypes.listValueClass(valueType));
            builder.append("java.util.List {} = {}(reader, wrapper, fieldPath);\n", variable, method);
        } else if (GenericTypes.isGenericStringMap(valueType)) {
            String method = decodeMapMethod(GenericTypes.mapValueClass(valueType));
            builder.append("java.util.Map {} = {}(reader, wrapper, fieldPath);\n", variable, method);
        } else {
            String method = decodeEntityMethod(valueClass);
            builder.append("{} {} = {}(reader, wrapper, fieldPath);\n", type(valueType), variable, method);
        }
        return variable;
    }

    private String registerEnumCodec(Class<?> fieldClass) {
        @SuppressWarnings("unchecked")
        Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) fieldClass;
        return enumCodecFields.computeIfAbsent(enumClass, key -> {
            String fieldVariable = "enumCodec" + fieldClass.getSimpleName() + (index++);
            builder.addField("private final {} {} = new {}({});", type(EnumCodec.class), fieldVariable, type(EnumCodec.class), variable(fieldClass));
            return fieldVariable;
        });
    }
}
