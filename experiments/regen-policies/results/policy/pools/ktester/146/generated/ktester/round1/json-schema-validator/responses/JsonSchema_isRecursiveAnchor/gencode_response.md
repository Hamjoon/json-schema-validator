```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#isRecursiveAnchor()}.
 *
 * <p>These tests cover the default flag value, handling of the
 * {@code $recursiveAnchor} keyword (true, false, non‑boolean), independence of
 * flags between different schema instances and preservation of the flag after
 * calling {@link JsonSchema#withConfig(SchemaValidatorsConfig)}.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_isRecursiveAnchor_Test {

    /** Helper to build a {@link JsonSchema} from a JSON string using the 2019‑09 spec. */
    private static JsonSchema schemaFromString(String json) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        return factory.getSchema(json);
    }

    /* --------------------------------------------------------------
     *  Parameterised tests for the basic $recursiveAnchor handling
     * -------------------------------------------------------------- */
    @Nested
    @DisplayName("Basic $recursiveAnchor handling")
    class BasicHandling {

        /** Provides test data for the parameterised test. */
        static Stream<Arguments> basicCases() {
            return Stream.of(
                // default state – empty schema, flag should be false
                Arguments.of("{}", false,
                        "A newly created JsonSchema instance should have recursiveAnchor defaulted to false."),
                // true flag
                Arguments.of("{ \"type\": \"object\", \"$recursiveAnchor\": true }", true,
                        "When the schema contains the \"$recursiveAnchor\": true keyword, isRecursiveAnchor() must return true."),
                // explicit false flag
                Arguments.of("{ \"type\": \"object\", \"$recursiveAnchor\": false }", false,
                        "When the schema explicitly sets \"$recursiveAnchor\": false, isRecursiveAnchor() must return false."),
                // non‑boolean value – should be ignored and treated as absent
                Arguments.of("{ \"type\": \"object\", \"$recursiveAnchor\": \"yes\" }", false,
                        "If the \"$recursiveAnchor\" keyword is not a boolean, it should be ignored and isRecursiveAnchor() must return false.")
            );
        }

        @ParameterizedTest(name = "{2}")
        @MethodSource("basicCases")
        void testIsRecursiveAnchor(String schemaJson, boolean expected, String description) {
            JsonSchema schema = schemaFromString(schemaJson);
            assertEquals(expected, schema.isRecursiveAnchor(),
                    "Unexpected isRecursiveAnchor() result for schema: " + schemaJson);
        }
    }

    /* --------------------------------------------------------------
     *  Independent flags between different schema instances
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Parent and child schemas have independent $recursiveAnchor flags")
    void testParentChildIndependentFlags() {
        // Parent schema with $recursiveAnchor = true
        String parentJson = "{ \"type\": \"object\", \"$recursiveAnchor\": true }";
        JsonSchema parentSchema = schemaFromString(parentJson);
        assertTrue(parentSchema.isRecursiveAnchor(),
                "Parent schema should report recursiveAnchor = true");

        // Child schema with its own $recursiveAnchor = false and a reference to the parent.
        // The reference is expressed via an allOf $ref to the root of the parent schema.
        // The child flag must be evaluated independently of the parent.
        String childJson = "{ \"type\": \"object\", \"$recursiveAnchor\": false, \"allOf\": [{ \"$ref\": \"#\" }] }";
        JsonSchema childSchema = schemaFromString(childJson);
        assertFalse(childSchema.isRecursiveAnchor(),
                "Child schema should report recursiveAnchor = false, independent of the parent");
    }

    /* --------------------------------------------------------------
     *  Preservation of the flag after cloning with withConfig()
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Recursive anchor flag is preserved after withConfig()")
    void testFlagPreservedAfterWithConfig() {
        String schemaJson = "{ \"type\": \"object\", \"$recursiveAnchor\": true }";
        JsonSchema original = schemaFromString(schemaJson);
        assertTrue(original.isRecursiveAnchor(),
                "Original schema must have recursiveAnchor = true before cloning");

        // Clone the schema with a (potentially) modified config. The flag should stay unchanged.
        SchemaValidatorsConfig config = new SchemaValidatorsConfig(); // no modifications
        JsonSchema cloned = original.withConfig(config);
        assertTrue(cloned.isRecursiveAnchor(),
                "Cloned schema must preserve recursiveAnchor = true after withConfig()");
    }
}
```