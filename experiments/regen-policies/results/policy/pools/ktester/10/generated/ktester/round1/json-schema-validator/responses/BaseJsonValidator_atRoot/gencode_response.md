```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.i18n.DefaultMessageSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#atRoot()}.
 *
 * <p>These tests cover:
 * <ul>
 *   <li>Correct PathType propagation (JSON, SCHEMA, default, and null).</li>
 *   <li>Behaviour of successive calls (equality vs. identity).</li>
 *   <li>Root‑path semantics (no parent, no segment, textual representation).</li>
 *   <li>Independence of the returned {@link JsonNodePath} objects.</li>
 *   <li>Exception when the internal {@code pathType} field is {@code null}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    @Mock
    private ValidationContext validationContext;
    @Mock
    private SchemaValidatorsConfig config;
    @Mock
    private SchemaLocation schemaLocation;
    @Mock
    private JsonNodePath evaluationPath;
    @Mock
    private JsonNode schemaNode;
    @Mock
    private JsonSchema parentSchema;
    @Mock
    private ValidatorTypeCode validatorType;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the ValidationContext to return the mocked config
        when(validationContext.getConfig()).thenReturn(config);
        // Default stubs for config – they can be overridden in individual tests
        when(config.isFailFast()).thenReturn(false);
        when(config.isCustomMessageSupported()).thenReturn(true);
        when(config.getMessageSource()).thenReturn(DefaultMessageSource.getInstance());
        // By default config.getPathType() returns null → DEFAULT will be used
        when(config.getPathType()).thenReturn(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Concrete subclass of {@link BaseJsonValidator} exposing the protected {@code atRoot()} method.
     */
    private static class TestValidator extends BaseJsonValidator {
        private final JsonNodePath evaluationPath;

        TestValidator(SchemaLocation schemaLocation,
                     JsonNodePath evaluationPath,
                     JsonNode schemaNode,
                     JsonSchema parentSchema,
                     ValidatorTypeCode validatorType,
                     ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
            this.evaluationPath = evaluationPath;
        }

        @Override
        public String getKeyword() {
            return "test";
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return evaluationPath;
        }

        @Override
        protected String getNodeFieldType() {
            return null;
        }

        /** Exposes the protected {@code atRoot()} method for testing. */
        public JsonNodePath callAtRoot() {
            return super.atRoot();
        }
    }

    /* --------------------------------------------------------------------- */
    /* Existing tests (kept unchanged)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    void testAtRootUsesDefaultPathType() throws Exception {
        // No explicit stub for config.getPathType() – it returns null → DEFAULT is used
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        JsonNodePath rootPath = validator.callAtRoot();
        assertNotNull(rootPath, "Root path should not be null");

        // Verify that the path is a root (no parent, no segment)
        Field parentField = JsonNodePath.class.getDeclaredField("parent");
        parentField.setAccessible(true);
        assertNull(parentField.get(rootPath), "Root path should have no parent");

        Field segmentField = JsonNodePath.class.getDeclaredField("pathSegment");
        segmentField.setAccessible(true);
        assertNull(segmentField.get(rootPath), "Root path should have no segment");

        // Verify the path type is DEFAULT
        assertEquals(PathType.DEFAULT, rootPath.getPathType(),
                "Path type should be DEFAULT when config does not specify a type");
    }

    @Test
    void testAtRootUsesCustomPathType() throws Exception {
        // Stub config to return a specific PathType
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);

        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        JsonNodePath rootPath = validator.callAtRoot();
        assertNotNull(rootPath, "Root path should not be null");

        // Verify that the path is a root (no parent, no segment)
        Field parentField = JsonNodePath.class.getDeclaredField("parent");
        parentField.setAccessible(true);
        assertNull(parentField.get(rootPath), "Root path should have no parent");

        Field segmentField = JsonNodePath.class.getDeclaredField("pathSegment");
        segmentField.setAccessible(true);
        assertNull(segmentField.get(rootPath), "Root path should have no segment");

        // Verify the path type matches the stubbed value
        assertEquals(PathType.JSON_POINTER, rootPath.getPathType(),
                "Path type should reflect the configured value");
    }

    /* --------------------------------------------------------------------- */
    /* New tests derived from the supplied JSON test cases                  */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that the {@code atRoot()} method propagates the configured {@link PathType}
     * (JSON and SCHEMA) to the created {@link JsonNodePath}.
     */
    @ParameterizedTest(name = "atRoot() should use PathType.{0}")
    @EnumSource(value = PathType.class, names = {"JSON", "SCHEMA"})
    void testAtRootRespectsConfiguredPathType(PathType configured) throws Exception {
        when(config.getPathType()).thenReturn(configured);

        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        JsonNodePath rootPath = validator.callAtRoot();
        assertNotNull(rootPath, "Root path must not be null");
        assertEquals(configured, rootPath.getPathType(),
                "The JsonNodePath should carry the PathType supplied by the config");
    }

    /**
     * Edge‑case: when the internal {@code pathType} field is forcibly set to {@code null}
     * the constructor of {@link JsonNodePath} is expected to throw a {@link NullPointerException}.
     */
    @Test
    void testAtRootThrowsWhenInternalPathTypeIsNull() throws Exception {
        // Use default config (null) – the BaseJsonValidator will set its internal field to null.
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        // Force the private final field to null via reflection
        Field pathTypeField = BaseJsonValidator.class.getDeclaredField("pathType");
        pathTypeField.setAccessible(true);
        pathTypeField.set(validator, null);

        // The call should now raise NPE (JsonNodePath does not accept null)
        assertThrows(NullPointerException.class, validator::callAtRoot,
                "Calling atRoot() with a null internal PathType should throw NPE");
    }

    /**
     * Two successive calls to {@code atRoot()} must return equal {@link JsonNodePath}
     * instances that are *not* the same object.
     */
    @Test
    void testAtRootReturnsEqualButDistinctInstances() {
        when(config.getPathType()).thenReturn(PathType.JSON);

        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        JsonNodePath first = validator.callAtRoot();
        JsonNodePath second = validator.callAtRoot();

        assertNotSame(first, second, "Each call should create a new JsonNodePath instance");
        assertEquals(first, second, "Both instances should be logically equal (root location)");
    }

    /**
     * The root {@link JsonNodePath} should have a textual representation that
     * corresponds to the root location (commonly \"$\" or an empty string).
     */
    @Test
    void testRootPathStringRepresentation() {
        when(config.getPathType()).thenReturn(PathType.JSON);

        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        JsonNodePath root = validator.callAtRoot();

        // The exact string depends on JsonNodePath implementation – we accept either "$" or "".
        String textual = root.toString();
        assertTrue(textual.equals("$") || textual.isEmpty(),
                "Root path string should be \"$\" or empty, but was: \"" + textual + "\"");
    }

    /**
     * Modifying a {@link JsonNodePath} obtained from {@code atRoot()} must not affect
     * a subsequently obtained root path – the validator must not retain mutable state.
     */
    @Test
    void testRootPathIndependenceAfterModification() throws Exception {
        when(config.getPathType()).thenReturn(PathType.JSON);

        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        JsonNodePath first = validator.callAtRoot();

        // Mutate the first instance via reflection (set a dummy segment)
        Field segmentField = JsonNodePath.class.getDeclaredField("pathSegment");
        segmentField.setAccessible(true);
        segmentField.set(first, "dummy");

        // Obtain a fresh root path
        JsonNodePath second = validator.callAtRoot();

        // The second path must still be a clean root (no segment, no parent)
        Field parentField = JsonNodePath.class.getDeclaredField("parent");
        parentField.setAccessible(true);
        assertNull(parentField.get(second), "Second root path must have no parent");
        assertNull(segmentField.get(second), "Second root path must have no segment");
    }

    /**
     * The same root semantics should hold for {@link PathType#SCHEMA}.
     */
    @Test
    void testRootPathForSchemaPathType() {
        when(config.getPathType()).thenReturn(PathType.SCHEMA);

        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        JsonNodePath root = validator.callAtRoot();

        // Verify root characteristics (no parent, no segment) regardless of PathType
        assertNotNull(root, "Root path must not be null");
        assertEquals(PathType.SCHEMA, root.getPathType(), "PathType should be SCHEMA");

        // Use reflection to ensure internal structure is still a root
        try {
            Field parentField = JsonNodePath.class.getDeclaredField("parent");
            Field segmentField = JsonNodePath.class.getDeclaredField("pathSegment");
            parentField.setAccessible(true);
            segmentField.setAccessible(true);
            assertNull(parentField.get(root), "Root (SCHEMA) should have no parent");
            assertNull(segmentField.get(root), "Root (SCHEMA) should have no segment");
        } catch (ReflectiveOperationException e) {
            fail("Reflection on JsonNodePath failed: " + e.getMessage());
        }
    }
}
```