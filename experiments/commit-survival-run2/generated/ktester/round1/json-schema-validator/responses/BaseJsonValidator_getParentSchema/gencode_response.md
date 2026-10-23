```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseJsonValidator#getParentSchema()}.
 *
 * <p>The concrete {@link TestValidator} class is a minimal implementation that only
 * satisfies the abstract {@code BaseJsonValidator} contract – we do not need any
 * real validation logic for these tests.</p>
 *
 * <p>The test suite covers:</p>
 * <ul>
 *   <li>Normal retrieval of a supplied parent schema.</li>
 *   <li>Correct handling of a {@code null} parent schema.</li>
 *   <li>Reference integrity – the returned object must be the exact same instance
 *       that was injected, even if the injected object is mutated after construction.</li>
 *   <li>Compatibility with Mockito‑generated proxies.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getParentSchema_Test {

    /** Minimal concrete implementation of the abstract {@link BaseJsonValidator}. */
    private static final class TestValidator extends BaseJsonValidator {
        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext,
                  suppressSubSchemaRetrieval);
        }

        @Override
        public java.util.Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                         JsonNode node) {
            // No validation logic required for the getter tests.
            return java.util.Collections.emptySet();
        }
    }

    /**
     * Simple mutable subclass of {@link JsonSchema}.  The constructor mirrors the
     * protected constructor used by the library; all arguments are {@code null}
     * because the internal state of the schema is irrelevant for these tests.
     */
    private static final class MutableJsonSchema extends JsonSchema {
        int mutableState = 0;

        MutableJsonSchema() {
            super(null, null, null, null, null);
        }

        void setState(int newState) {
            this.mutableState = newState;
        }

        int getState() {
            return this.mutableState;
        }
    }

    @Mock
    private JsonSchema mockParentSchema;               // a regular Mockito mock

    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    private TestValidator validatorWithParent;
    private TestValidator validatorWithoutParent;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Instance with a non‑null parent (the mock defined above).
        validatorWithParent = new TestValidator(
                "schemaPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        // Instance where the parent schema is intentionally left null.
        validatorWithoutParent = new TestValidator(
                "schemaPath",
                mockSchemaNode,
                null,
                mockValidatorType,
                mockValidationContext,
                false);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /*  Group: Parent Schema Retrieval                                        */
    /* --------------------------------------------------------------------- */

    @Test
    void getParentSchema_returnsProvidedParent() {
        JsonSchema returned = validatorWithParent.getParentSchema();
        assertNotNull(returned, "Parent schema should not be null when supplied in constructor");
        assertSame(mockParentSchema, returned,
                "Returned parent schema must be the exact instance that was injected");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentSupplied() {
        JsonSchema returned = validatorWithoutParent.getParentSchema();
        assertNull(returned, "Parent schema should be null when none is supplied in constructor");
    }

    /* --------------------------------------------------------------------- */
    /*  Group: Parent Schema Retrieval - Reference Integrity                  */
    /* --------------------------------------------------------------------- */

    /**
     * Supplies two different {@link JsonSchema} implementations:
     * <ul>
     *   <li>A mutable concrete subclass ({@link MutableJsonSchema}) whose internal
     *       state is changed after the validator is constructed.</li>
     *   <li>A Mockito‑generated proxy.</li>
     * </ul>
     * The test asserts that {@code getParentSchema()} always returns the original
     * reference, confirming that the method does not create a copy or otherwise
     * alter the supplied object.
     */
    @ParameterizedTest(name = "{index}: parentSchema={0}")
    @MethodSource("parentSchemaProvider")
    void getParentSchema_preservesReferenceIntegrity(JsonSchema injectedParent) {
        // Build a validator that receives the supplied parent schema.
        TestValidator validator = new TestValidator(
                "schemaPath",
                mockSchemaNode,
                injectedParent,
                mockValidatorType,
                mockValidationContext,
                false);

        // Mutate the injected instance if it is our mutable subclass.
        if (injectedParent instanceof MutableJsonSchema mutable) {
            mutable.setState(42); // change internal state after construction
        }

        JsonSchema returned = validator.getParentSchema();
        assertSame(injectedParent, returned,
                "The validator must return the exact same JsonSchema instance that was injected");
    }

    /** Provides the different {@link JsonSchema} instances for the parameterized test. */
    private static Stream<JsonSchema> parentSchemaProvider() {
        // 1. A mutable concrete subclass.
        MutableJsonSchema mutable = new MutableJsonSchema();
        mutable.setState(1); // initial state (any value is fine)

        // 2. A Mockito proxy (different instance from the field‑level mock to avoid
        //    accidental reuse across tests).
        JsonSchema mockProxy = org.mockito.Mockito.mock(JsonSchema.class);

        return Stream.of(mutable, mockProxy);
    }
}
```