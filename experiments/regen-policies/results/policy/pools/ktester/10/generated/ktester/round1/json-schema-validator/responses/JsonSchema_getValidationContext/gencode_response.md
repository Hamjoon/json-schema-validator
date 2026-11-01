```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#getValidationContext()}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Basic behaviour when the internal field is set directly via reflection.</li>
 *   <li>Behaviour when the field is {@code null} (no hidden initialisation).</li>
 *   <li>Result of the package‑private static factory method {@code JsonSchema.from(...)}.</li>
 *   <li>Inheritance of the {@link ValidationContext} by a child schema created via {@code getSubSchema(...)}.</li>
 *   <li>Effect of {@code withConfig(...)} – the configuration must not replace the context.</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidationContext_Test {

    /** Shared objects that can be reused for all tests */
    private static JsonMetaSchema metaSchema;
    private static JsonSchemaFactory schemaFactory;
    private static SchemaValidatorsConfig config;

    /** Objects that are created per‑test */
    private ValidationContext validationContext;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a concrete meta‑schema (draft‑2019‑09) – this is sufficient for the test.
        metaSchema = JsonMetaSchema.getInstance(VersionFlag.V201909);
        schemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        config = new SchemaValidatorsConfig();
    }

    @BeforeEach
    void setupBeforeEach(@Mock SchemaLocation schemaLocation,
                         @Mock JsonNodePath evaluationPath,
                         @Mock JsonNode schemaNode) {
        /*
         * For most tests we need a ValidationContext that is stored inside a JsonSchema.
         * The static factory method {@code JsonSchema.from(...)} does exactly that,
         * therefore we create a fresh JsonSchema instance here and keep it for the
         * test methods that need it.
         */
        validationContext = new ValidationContext(metaSchema, schemaFactory, config);
        JsonSchema jsonSchema = JsonSchema.from(validationContext,
                                                schemaLocation,
                                                evaluationPath,
                                                schemaNode,
                                                null,               // no parent schema
                                                false);             // do not suppress sub‑schema retrieval
        this.jsonSchemaUnderTest = jsonSchema;
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for this simple test.
    }

    /** Instance under test – created in @BeforeEach */
    private JsonSchema jsonSchemaUnderTest;

    // -------------------------------------------------------------------------
    // 1. Basic behaviour – field set via reflection / field left null
    // -------------------------------------------------------------------------

    @Test
    void getValidationContext_returnsTheSameInstanceProvidedAtConstruction() {
        // Act
        ValidationContext returnedContext = jsonSchemaUnderTest.getValidationContext();

        // Assert – the returned context must be the exact same object that was
        // supplied to the JsonSchema constructor.
        assertSame(validationContext, returnedContext,
                "JsonSchema#getValidationContext should return the ValidationContext used during construction");
    }

    @Test
    void getValidationContext_returnsNullWhenFieldWasNeverInitialised() throws Exception {
        // -----------------------------------------------------------------
        // Build a JsonSchema instance via the package‑private constructor,
        // passing {@code null} for the ValidationContext.
        // -----------------------------------------------------------------
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);

        JsonSchema schemaWithNullContext = ctor.newInstance(
                null,               // ValidationContext = null
                mock(SchemaLocation.class),
                mock(JsonNodePath.class),
                mock(JsonNode.class),
                null,               // parent
                false);

        // Act
        ValidationContext result = schemaWithNullContext.getValidationContext();

        // Assert
        assertNull(result, "When the internal validationContext field is null, getValidationContext() must return null");
    }

    @Test
    void getValidationContext_returnsInstanceSetViaReflection() throws Exception {
        // -----------------------------------------------------------------
        // Create a JsonSchema via the factory (so we have a usable instance)
        // and then replace its private field with a freshly created ValidationContext.
        // -----------------------------------------------------------------
        ValidationContext newContext = new ValidationContext(metaSchema, schemaFactory, config);

        // The field lives in the superclass BaseJsonValidator
        Field ctxField = BaseJsonValidator.class.getDeclaredField("validationContext");
        ctxField.setAccessible(true);
        ctxField.set(jsonSchemaUnderTest, newContext);

        // Act
        ValidationContext returned = jsonSchemaUnderTest.getValidationContext();

        // Assert
        assertSame(newContext, returned,
                "After setting the private field via reflection, getValidationContext() must return that exact instance");
    }

    // -------------------------------------------------------------------------
    // 2. After static factory `from`
    // -------------------------------------------------------------------------

    @Test
    void getValidationContext_afterStaticFactoryFrom_returnsSuppliedInstance() throws Exception {
        // The @BeforeEach already created a schema via JsonSchema.from(...).
        // We simply verify that the returned context is the one we injected.
        ValidationContext ctx = jsonSchemaUnderTest.getValidationContext();
        assertSame(validationContext, ctx,
                "JsonSchema.from(...) must store the supplied ValidationContext unchanged");
    }

    // -------------------------------------------------------------------------
    // 3. Child schema inherits parent context
    // -------------------------------------------------------------------------

    @Test
    void childSchema_inheritsParentValidationContext() throws Exception {
        // Build a simple parent schema containing a child property.
        ObjectMapper mapper = JsonMapperFactory.getMapper();
        JsonNode parentNode = mapper.readTree("{\"type\":\"object\",\"properties\":{\"child\":{\"type\":\"string\"}}}");

        // Re‑create the parent JsonSchema using the real node (so that getSubSchema works).
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        JsonSchema parent = ctor.newInstance(
                validationContext,
                mock(SchemaLocation.class),
                JsonNodePath.ROOT,
                parentNode,
                null,
                false);

        // Resolve the child schema via the public API.
        JsonSchema child = parent.getSubSchema(JsonNodePath.of("#/properties/child"));

        // Verify that both schemas share the exact same ValidationContext instance.
        assertSame(validationContext, child.getValidationContext(),
                "A child schema obtained via getSubSchema(...) must inherit the parent's ValidationContext");
    }

    // -------------------------------------------------------------------------
    // 4. Configuration via withConfig does not replace ValidationContext
    // -------------------------------------------------------------------------

    @Test
    void withConfig_returnsNewSchemaButKeepsOriginalValidationContext() {
        // Act – apply a fresh configuration
        SchemaValidatorsConfig newConfig = new SchemaValidatorsConfig();
        JsonSchema configuredSchema = jsonSchemaUnderTest.withConfig(newConfig);

        // The returned instance must be a different object (with a different config)
        assertNotSame(jsonSchemaUnderTest, configuredSchema,
                "withConfig should return a new JsonSchema instance");

        // However the ValidationContext must be the original one.
        assertSame(validationContext, configuredSchema.getValidationContext(),
                "withConfig must not replace the underlying ValidationContext");
    }
}
```