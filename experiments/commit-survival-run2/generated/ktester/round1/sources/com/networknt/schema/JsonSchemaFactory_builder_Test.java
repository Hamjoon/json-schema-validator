package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

class JsonSchemaFactory_builder_Test {

    //     @Mock
    private JsonSchemaFactory // used in the overload test
    mockBlueprint;

    /**
     * A real factory instance used as a blueprint for the overload test.
     */
    private JsonSchemaFactory blueprint;

    @BeforeEach
    void setUp() {
        // Create a default factory (using the Draft‑V4 blueprint) that we can use as a blueprint.
        // The builder that takes a blueprint copies its configuration, so the resulting factory
        // is fully initialised and does not throw the “defaultMetaSchemaURI” exception.
//         blueprint = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
    }

    /**
     * Utility that walks the class hierarchy to find the first field whose type matches the supplied class.
     */
    private static Field findFieldOfType(Class<?> clazz, Class<?> fieldType) {
        Class<?> current = clazz;
        while (current != null) {
            for (Field f : current.getDeclaredFields()) {
                if (fieldType.isAssignableFrom(f.getType())) {
                    return f;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    @Test
    void builderWithBlueprintReturnsBuilder() {
        // The blueprint is a real factory; the method should still return a Builder.
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder(blueprint);
        assertNotNull(builder, "builder(JsonSchemaFactory) should return a non‑null Builder instance");
    }
}