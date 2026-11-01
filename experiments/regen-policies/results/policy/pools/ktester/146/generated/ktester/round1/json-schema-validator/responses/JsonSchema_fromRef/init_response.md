```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#fromRef(JsonSchema, JsonNodePath)}.
 *
 * The test creates a minimal concrete subclass of {@link JsonSchema} that
 * exposes the protected constructor and the evaluation‑path related getters.
 * A real {@link ValidationContext} is built from mocked {@link JsonMetaSchema}
 * and {@link JsonSchemaFactory} instances – this is sufficient for the
 * {@code fromRef} implementation which only copies a few fields.
 *
 * The assertions verify that the returned schema:
 * <ul>
 *   <li>is a new instance (not the same as the source schema)</li>
 *   <li>keeps the original {@code id}</li>
 *   <li>receives the supplied {@code evaluationPath}</li>
 *   <li>receives the supplied {@code evaluationParentSchema}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_fromRef_Test {

    /** Minimal concrete subclass that forwards to the protected constructor
     *  and provides public accessors for the evaluation‑path information. */
    private static final class TestJsonSchema extends JsonSchema {
        TestJsonSchema(List<JsonValidator> validators,
                       boolean validatorsLoaded,
                       boolean recursiveAnchor,
                       TypeValidator typeValidator,
                       String id,
                       boolean suppressSubSchemaRetrieval,
                       JsonNode schemaNode,
                       ValidationContext validationContext,
                       ErrorMessageType errorMessageType,
                       String errorMessageKeyword,
                       MessageSource messageSource,
                       Keyword keyword,
                       JsonSchema parentSchema,
                       SchemaLocation schemaLocation,
                       JsonNodePath evaluationPath,
                       JsonSchema evaluationParentSchema,
                       Map<String, String> errorMessage) {
            super(validators, validatorsLoaded, recursiveAnchor, typeValidator, id,
                    suppressSubSchemaRetrieval, schemaNode, validationContext,
                    errorMessageType, errorMessageKeyword, messageSource, keyword,
                    parentSchema, schemaLocation, evaluationPath, evaluationParentSchema,
                    errorMessage);
        }

        /** expose the protected getter from {@link BaseJsonValidator} */
        JsonNodePath getEvaluationPathPublic() {
            return super.getEvaluationPath();
        }

        /** expose the protected getter from {@link BaseJsonValidator} */
        JsonSchema getEvaluationParentSchemaPublic() {
            return super.getEvaluationParentSchema();
        }

        /** expose the id (package‑private field) via the public accessor */
        @Override
        public String getId() {
            return super.getId();
        }
    }

    /** Shared objects used by all tests. */
    private static ValidationContext sharedValidationContext;
    private static JsonNode emptySchemaNode;

    @BeforeAll
    static void setupBeforeAll() {
        // Minimal but functional ValidationContext – the meta‑schema and factory are mocked
        JsonMetaSchema metaSchema = org.mockito.Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        sharedValidationContext = new ValidationContext(metaSchema, factory, config);
        emptySchemaNode = JsonNodeFactory.instance.objectNode();   // an empty object node
    }

    @BeforeEach
    void setupBeforeEach() {
        // no per‑test state needed
    }

    @AfterEach
    void teardownAfterEach() {
        // no per‑test cleanup needed
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up
    }

    @Test
    void fromRef_copiesRelevantState() {
        // -----------------------------------------------------------------
        // Arrange – create a “parent” schema that will be passed as the
        // evaluationParentSchema argument.
        // -----------------------------------------------------------------
        JsonNodePath parentEvalPath = org.mockito.Mockito.mock(JsonNodePath.class);
        JsonSchema parentSchema = new TestJsonSchema(
                Collections.emptyList(),          // validators
                false,                            // validatorsLoaded
                false,                            // recursiveAnchor
                null,                             // typeValidator
                "parent-id",                      // id
                false,                            // suppressSubSchemaRetrieval
                emptySchemaNode,                  // schemaNode
                sharedValidationContext,          // validationContext
                null,                             // errorMessageType
                null,                             // errorMessageKeyword
                null,                             // messageSource
                null,                             // keyword
                null,                             // parentSchema (root)
                null,                             // schemaLocation (not needed for this test)
                parentEvalPath,                   // evaluationPath
                null,                             // evaluationParentSchema
                Collections.emptyMap()            // errorMessage map
        );

        // -----------------------------------------------------------------
        // Arrange – create the schema on which we will invoke fromRef().
        // -----------------------------------------------------------------
        JsonNodePath originalEvalPath = org.mockito.Mockito.mock(JsonNodePath.class);
        JsonSchema schemaUnderTest = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "original-id",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                originalEvalPath,
                null,
                Collections.emptyMap()
        );

        // The new evaluation path that we want the derived schema to carry.
        JsonNodePath newEvalPath = org.mockito.Mockito.mock(JsonNodePath.class);

        // -----------------------------------------------------------------
        // Act – call the method under test.
        // -----------------------------------------------------------------
        JsonSchema derived = schemaUnderTest.fromRef(parentSchema, newEvalPath);

        // -----------------------------------------------------------------
        // Assert – verify the derived schema has the expected state.
        // -----------------------------------------------------------------
        assertNotNull(derived, "fromRef must return a non‑null JsonSchema");
        assertNotSame(schemaUnderTest, derived, "Derived schema must be a new instance");

        // The id should be copied from the original schema.
        assertEquals("original-id", ((TestJsonSchema) derived).getId(),
                "Derived schema should retain the original id");

        // The evaluation path and parent supplied to fromRef must be stored.
        assertSame(newEvalPath,
                ((TestJsonSchema) derived).getEvaluationPathPublic(),
                "EvaluationPath should be the one supplied to fromRef");
        assertSame(parentSchema,
                ((TestJsonSchema) derived).getEvaluationParentSchemaPublic(),
                "EvaluationParentSchema should be the one supplied to fromRef");
    }
}
```