package me.basiqueevangelist.pingspam.config;

import blue.endless.jankson.*;
import blue.endless.jankson.api.SyntaxError;
import dev.inkwell.conrad.api.value.serialization.AbstractTreeSerializer;
import dev.inkwell.conrad.api.value.util.Array;
import dev.inkwell.conrad.api.value.util.Table;
import dev.inkwell.conrad.api.value.util.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class JanksonSerializer extends AbstractTreeSerializer<JsonElement, JsonObject> {
    private static final Jankson JANKSON = Jankson.builder().build();

    public JanksonSerializer() {
        this.addSerializer(Boolean.class, BooleanSerializer.INSTANCE);
        this.addSerializer(Integer.class, IntSerializer.INSTANCE);
        this.addSerializer(Long.class, LongSerializer.INSTANCE);
        this.addSerializer(String.class, StringSerializer.INSTANCE);
        this.addSerializer(Float.class, FloatSerializer.INSTANCE);
        this.addSerializer(Double.class, DoubleSerializer.INSTANCE);

        this.addSerializer(Array.class, t -> new ArraySerializer<>(t));
        this.addSerializer(Table.class, t -> new TableSerializer<>(t));


        this.addSerializer(Identifier.class, IdentifierSerializer.INSTANCE);
    }

    @Override
    protected <V> ValueSerializer<JsonElement, ?, V> getDataSerializer(Class<V> clazz) {
        return new DataSerializer<>(clazz);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected <V> ValueSerializer<JsonElement, ?, V> getEnumSerializer(Class<V> valueClass) {
        return new EnumSerializer(valueClass);
    }

    @Override
    protected JsonObject start(@Nullable Iterable<String> comments) {
        return new JsonObject();
    }

    @Override
    protected <R extends JsonElement> R add(JsonObject object, String key, R representation, Iterable<String> comments) {
        object.put(key, representation, String.join("\n", comments));
        return representation;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <V extends JsonElement> V get(JsonObject object, String s) {
        return (V) object.get(s);
    }

    @Override
    protected void write(JsonObject root, Writer writer, boolean minimal) throws IOException {
        writer.write(root.toJson(true, true));
    }

    @Override
    public @NotNull String getExtension() {
        return "json5";
    }

    @Override
    public @Nullable Version getVersion(InputStream inputStream) throws IOException, VersionParsingException {
        try {
            JsonObject obj = JANKSON.load(inputStream);
            String name = obj.get(String.class, "Version");
            return name == null ? null : Version.parse(name);
        } catch (SyntaxError e) {
            return null;
        }
    }

    @Override
    public @NotNull JsonObject getRepresentation(InputStream inputStream) throws IOException {
        try {
            return JANKSON.load(inputStream);
        } catch (SyntaxError syntaxError) {
            throw new RuntimeException(syntaxError);
        }
    }

    private class DataSerializer<V> implements ValueSerializer<JsonElement, JsonObject, V> {
        private final Class<V> clazz;

        public DataSerializer(Class<V> clazz) {
            this.clazz = clazz;
        }

        @SuppressWarnings("unchecked")
        @Override
        public JsonObject serialize(V value) {
            try {
                JsonObject obj = new JsonObject();
                for (Field objField : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(objField.getModifiers())) continue;

                    objField.setAccessible(true);
                    ValueSerializer<JsonElement, ?, ?> serializer = JanksonSerializer.this.getSerializer((Class<Object>) objField.getType(), objField.get(value));
                    obj.put(objField.getName(), serializer.serializeValue(objField.get(value)));
                }
                return obj;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public V deserialize(JsonElement representation) {
            try {
                JsonObject obj = (JsonObject) representation;
                V inst = clazz.newInstance();
                for (Field objField : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(objField.getModifiers())) continue;

                    objField.setAccessible(true);
                    ValueSerializer<JsonElement, ?, ?> serializer = JanksonSerializer.this.getSerializer((Class<Object>) objField.getType(), objField.get(inst));
                    objField.set(inst, serializer.deserialize(obj.get(objField.getName())));
                }
                return inst;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class EnumSerializer<V extends Enum<V>> implements ValueSerializer<JsonElement, JsonPrimitive, V> {
        private final Class<V> clazz;

        public EnumSerializer(Class<V> clazz) {
            this.clazz = clazz;
        }

        @Override
        public JsonPrimitive serialize(V value) {
            return new JsonPrimitive(value.name());
        }

        @Override
        public V deserialize(JsonElement representation) {
            JsonPrimitive primitive = (JsonPrimitive) representation;
            return Enum.valueOf(clazz, primitive.asString());
        }
    }

    private enum BooleanSerializer implements ValueSerializer<JsonElement, JsonPrimitive, Boolean> {
        INSTANCE;

        @Override
        public JsonPrimitive serialize(Boolean value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Boolean deserialize(JsonElement representation) {
            JsonPrimitive primitive = (JsonPrimitive) representation;
            return primitive.asBoolean(false);
        }
    }

    private enum IntSerializer implements ValueSerializer<JsonElement, JsonPrimitive, Integer> {
        INSTANCE;

        @Override
        public JsonPrimitive serialize(Integer value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Integer deserialize(JsonElement representation) {
            JsonPrimitive primitive = (JsonPrimitive) representation;
            return primitive.asInt(0);
        }
    }

    private enum LongSerializer implements ValueSerializer<JsonElement, JsonPrimitive, Long> {
        INSTANCE;

        @Override
        public JsonPrimitive serialize(Long value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Long deserialize(JsonElement representation) {
            JsonPrimitive primitive = (JsonPrimitive) representation;
            return primitive.asLong(0);
        }
    }

    private enum StringSerializer implements ValueSerializer<JsonElement, JsonPrimitive, String> {
        INSTANCE;

        @Override
        public JsonPrimitive serialize(String value) {
            return new JsonPrimitive(value);
        }

        @Override
        public String deserialize(JsonElement representation) {
            JsonPrimitive primitive = (JsonPrimitive) representation;
            return primitive.asString();
        }
    }

    private enum FloatSerializer implements ValueSerializer<JsonElement, JsonPrimitive, Float> {
        INSTANCE;

        @Override
        public JsonPrimitive serialize(Float value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Float deserialize(JsonElement representation) {
            JsonPrimitive primitive = (JsonPrimitive) representation;
            return primitive.asFloat(0);
        }
    }

    private enum DoubleSerializer implements ValueSerializer<JsonElement, JsonPrimitive, Double> {
        INSTANCE;

        @Override
        public JsonPrimitive serialize(Double value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Double deserialize(JsonElement representation) {
            JsonPrimitive primitive = (JsonPrimitive) representation;
            return primitive.asDouble(0);
        }
    }

    private enum IdentifierSerializer implements ValueSerializer<JsonElement, JsonPrimitive, Identifier> {
        INSTANCE;

        @Override
        public JsonPrimitive serialize(Identifier value) {
            return new JsonPrimitive(value.toString());
        }

        @Override
        public Identifier deserialize(JsonElement representation) {
            JsonPrimitive primitive = (JsonPrimitive) representation;
            return new Identifier(primitive.asString());
        }
    }

    private class ArraySerializer<T> implements ValueSerializer<JsonElement, JsonArray, Array<T>> {
        private final Array<T> defaultValue;

        public ArraySerializer(Array<T> defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public JsonArray serialize(Array<T> value) {
            JsonArray array = new JsonArray();
            ValueSerializer<JsonElement, ?, T> serializer = JanksonSerializer.this.getSerializer(defaultValue.getValueClass(), defaultValue.getDefaultValue().get());
            for (int i = 0; i < value.size(); i++) {
                array.set(i, serializer.serialize(value.get(i)));
            }
            return array;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Array<T> deserialize(JsonElement representation) {
            JsonArray array = (JsonArray) representation;
            ValueSerializer<JsonElement, ?, T> serializer = JanksonSerializer.this.getSerializer(defaultValue.getValueClass(), defaultValue.getDefaultValue().get());
            T[] values = (T[]) java.lang.reflect.Array.newInstance(defaultValue.getValueClass(), array.size());
            for (int i = 0; i < array.size(); i++) {
                values[i] = serializer.deserialize(array.get(i));
            }
            return new Array<>(defaultValue.getValueClass(), defaultValue.getDefaultValue(), values);
        }
    }

    private class TableSerializer<T> implements ValueSerializer<JsonElement, JsonObject, Table<T>> {
        private final Table<T> defaultValue;

        public TableSerializer(Table<T> defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public JsonObject serialize(Table<T> value) {
            JsonObject obj = new JsonObject();
            ValueSerializer<JsonElement, ?, T> serializer = JanksonSerializer.this.getSerializer(defaultValue.getValueClass(), defaultValue.getDefaultValue().get());
            for (Table.Entry<String, T> entry : value) {
                obj.put(entry.getKey(), serializer.serialize(entry.getValue()));
            }
            return obj;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Table<T> deserialize(JsonElement representation) {
            JsonObject obj = (JsonObject) representation;
            ValueSerializer<JsonElement, ?, T> serializer = JanksonSerializer.this.getSerializer(defaultValue.getValueClass(), defaultValue.getDefaultValue().get());
            Table.Entry<String, T>[] entries = (Table.Entry<String, T>[]) java.lang.reflect.Array.newInstance(Table.Entry.class, obj.size());
            int i = 0;
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                entries[i++] = new Table.Entry<>(entry.getKey(), serializer.deserialize(entry.getValue()));
            }
            return new Table<>(defaultValue.getValueClass(), defaultValue.getDefaultValue(), entries);
        }
    }
}
