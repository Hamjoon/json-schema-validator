```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Unit‑tests for the protected {@code atRoot()} method of {@link BaseJsonValidator}.
 *
 * <p>The method simply delegates to {@code pathType.getRoot()}.  The tests cover:
 *
 * <ul>
 *   <li>Default {@code PathType} when the {@link ValidationContext} is {@code null}.</li>
 *   <li>Custom {@code PathType} returning various root strings.</li>
 *   <li>Custom {@code PathType} returning {@code null}.</li>
 *   <li>Behaviour when {@code PathType} itself is {@code null} (NPE).</li>
 *   <li>Propagation of a runtime exception thrown by {@code PathType#getRoot()}.</li>
 * </ul>
 *
 * <p>A minimal concrete subclass {@code DummyValidator} is used because
 * {@link BaseJsonValidator} is abstract.  No validation logic is required for the
 * {@code atRoot()} tests, therefore {@code validate(...)} simply returns an empty
 * set.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    /** Minimal concrete implementation used only for testing {@code atRoot()}. */
    private static class DummyValidator extends BaseJsonValidator {
        DummyValidator(String schemaPath,
                       JsonNode schemaNode,
                       JsonSchema parentSchema,
                       ValidatorTypeCode validatorType,
                       ValidationContext validationContext,
                       boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType,
                  validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // Validation not required for atRoot() tests.
            return Collections.emptySet();
        }
    }

    @Mock private JsonSchema mockParentSchema;
    @Mock private ValidatorTypeCode mockValidatorType;
    @Mock private ValidationContext mockValidationContext;
    @Mock private SchemaValidatorsConfig mockConfig;
    @Mock private PathType mockPathType;
    @Mock private JsonNode mockSchemaNode;

    @BeforeEach
    void setupBeforeEach() {
        // Default stubbing for the "custom PathType" tests.
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.getPathType()).thenReturn(mockPathType);
    }

    @AfterEach
    void teardownAfterEach() {
        Mockito.reset(mockParentSchema, mockValidatorType, mockValidationContext,
                     mockConfig, mockPathType, mockSchemaNode);
    }

    /** --------------------------------------------------------------
     *  1️⃣  Default behaviour – ValidationContext is null → PathType.DEFAULT
     *  -------------------------------------------------------------- */
    @Test
    void atRoot_returnsDefaultRoot_whenValidationContextIsNull() {
        DummyValidator validator = new DummyValidator(
                "schemaPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                null,                     // null ValidationContext triggers PathType.DEFAULT
                false);

        String root = validator.atRoot();

        // PathType.DEFAULT uses "$" as the root symbol in the library.
        Assertions.assertEquals("$", root,
                "When ValidationContext is null the validator should fall back to PathType.DEFAULT and return \"$\".");
    }

    /** --------------------------------------------------------------
     *  2️⃣  Normal behaviour with several PathType implementations
     *  -------------------------------------------------------------- */
    private static Stream<RootProvider> normalRootProvider() {
        return Stream.of(
                new RootProvider("$", "PathType returning JSONPath root \"$\""),
                new RootProvider("", "PathType returning empty string (JSON Pointer)"),
                new RootProvider("/", "PathType returning \"/\" (JSON Pointer root)")
        );
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("normalRootProvider")
    void atRoot_returnsProvidedRoot_forVariousPathTypes(RootProvider provider) {
        // Stub the mocked PathType to return the desired root string.
        when(mockPathType.getRoot()).thenReturn(provider.expectedRoot);

        DummyValidator validator = new DummyValidator(
                "schemaPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,    // non‑null context → uses mocked PathType
                false);

        String root = validator.atRoot();

        Assertions.assertEquals(provider.expectedRoot, root,
                "atRoot() should return the root supplied by PathType.getRoot()");
    }

    /** Helper holder for parameterised test data. */
    private static record RootProvider(String expectedRoot, String description) {}

    /** --------------------------------------------------------------
     *  3️⃣  Custom PathType that returns null
     *  -------------------------------------------------------------- */
    @Test
    void atRoot_propagatesNull_whenPathTypeReturnsNull() {
        when(mockPathType.getRoot()).thenReturn(null);

        DummyValidator validator = new DummyValidator(
                "schemaPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        Assertions.assertNull(validator.atRoot(),
                "If PathType.getRoot() returns null, atRoot() should return null without throwing.");
    }

    /** --------------------------------------------------------------
     *  4️⃣  Null PathType → NullPointerException
     *  -------------------------------------------------------------- */
    @Test
    void atRoot_throwsNullPointerException_whenPathTypeIsNull() throws Exception {
        DummyValidator validator = new DummyValidator(
                "schemaPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        // Force the private 'pathType' field to null via reflection.
        Field field = BaseJsonValidator.class.getDeclaredField("pathType");
        field.setAccessible(true);
        field.set(validator, null);

        Assertions.assertThrows(NullPointerException.class, validator::atRoot,
                "Calling atRoot() with a null PathType should result in NullPointerException.");
    }

    /** --------------------------------------------------------------
     *  5️⃣  PathType#getRoot() throws an unchecked exception → propagation
     *  -------------------------------------------------------------- */
    @Test
    void atRoot_propagatesRuntimeException_whenPathTypeThrows() {
        when(mockPathType.getRoot()).thenThrow(new IllegalStateException("boom"));

        DummyValidator validator = new DummyValidator(
                "schemaPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                validator::atRoot,
                "The exception from PathType.getRoot() should be propagated unchanged.");

        Assertions.assertEquals("boom", ex.getMessage(),
                "Propagated exception should retain the original message.");
    }

    /** --------------------------------------------------------------
     *  6️⃣  Custom PathType supplied via ValidationContext (already covered by
     *      the parameterised test, but retained for completeness).
     *  -------------------------------------------------------------- */
    @Test
    void atRoot_returnsCustomRoot_whenPathTypeIsProvided() {
        when(mockPathType.getRoot()).thenReturn("customRoot");

        DummyValidator validator = new DummyValidator(
                "schemaPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        String root = validator.atRoot();

        Assertions.assertEquals("customRoot", root,
                "atRoot() should delegate to the PathType supplied via ValidationContext.");
    }
}
```