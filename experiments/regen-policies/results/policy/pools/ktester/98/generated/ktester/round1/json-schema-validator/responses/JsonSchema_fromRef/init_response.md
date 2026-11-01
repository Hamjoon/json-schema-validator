```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#fromRef(JsonSchema, JsonNodePath)}.
 *
 * The tests focus on the *state* that {@code fromRef} copies from the
 * original {@link JsonSchema} instance to the newly created one.
 *
 * <p>
 * The method creates a new {@link JsonSchema} using a protected constructor.
 * Most of the state is private, therefore the tests use reflection to
 * inspect the fields {@code evaluationPath}, {@code evaluationParentSchema}
 * and {@code validationContext} of the returned instance.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_fromRef_Test {

    /** Factory used to create a baseline {@link JsonSchema}. */
    private static JsonSchemaFactory factory;

    /** A simple schema that will be the source of the {@code fromRef} call. */
    private JsonSchema sourceSchema;

    /** The parent schema that will be passed to {@code fromRef}. */
    private JsonSchema parentSchema;

    /** Evaluation path that will be passed to {@code fromRef}. */
    private JsonNodePath evaluationPath;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft – the concrete version does not matter for the test.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // a minimal valid schema – we only need a JsonSchema instance, not validation logic
        String schemaJson = "{\"type\":\"object\"}";
        sourceSchema = factory.getSchema(schemaJson);
        parentSchema = sourceSchema; // for simplicity we reuse the same instance as parent
        // JsonNodePath has a public constructor that requires a PathType enum.
        evaluationPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up – objects are GC‑ed automatically
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up at the class level
    }

    /**
     * Verifies that {@code fromRef} returns a new {@link JsonSchema} whose
     * {@code evaluationPath} and {@code evaluationParentSchema} fields are the
     * exact objects supplied to the method, and that the {@code validationContext}
     * is shared with the original schema.
     */
    @Test
    void fromRef_copiesStateCorrectly() throws Exception {
        // Act – create a new schema via the method under test
        JsonSchema derived = sourceSchema.fromRef(parentSchema, evaluationPath);

        // --------------------------------------------------------------------
        // Use reflection to read the private fields of JsonSchema.
        // --------------------------------------------------------------------
        Field evalPathField = JsonSchema.class.getDeclaredField("evaluationPath");
        evalPathField.setAccessible(true);
        Object actualEvalPath = evalPathField.get(derived);
        assertSame(evaluationPath, actualEvalPath,
                "evaluationPath should be the same instance that was passed to fromRef");

        Field evalParentField = JsonSchema.class.getDeclaredField("evaluationParentSchema");
        evalParentField.setAccessible(true);
        Object actualEvalParent = evalParentField.get(derived);
        assertSame(parentSchema, actualEvalParent,
                "evaluationParentSchema should be the same instance that was passed to fromRef");

        Field vcField = JsonSchema.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        Object actualVc = vcField.get(derived);
        Field originalVcField = JsonSchema.class.getDeclaredField("validationContext");
        originalVcField.setAccessible(true);
        Object originalVc = originalVcField.get(sourceSchema);
        assertSame(originalVc, actualVc,
                "validationContext should be carried over from the original JsonSchema");
    }
}
```