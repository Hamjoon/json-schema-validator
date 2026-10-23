```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_validate_Test {

    /** shared objects for the whole test suite */
    private static ObjectMapper mapper;
    private static SchemaValidatorsConfig config;

    /** objects created for each test */
    @Mock private URIFactory uriFactory;
    @Mock private URNFactory urnFactory;
    @Mock private JsonMetaSchema metaSchema;
    @Mock private JsonSchemaFactory jsonSchemaFactory;

    private ValidationContext validationContext;
    private ExecutionContext executionContext;

    /** the validator under test */
    private AnyOfValidator anyOfValidator;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        config = new SchemaValidatorsConfig();               // default – discriminators disabled
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // 1. Build a real ValidationContext – we only need the config part,
        //    all other collaborators are mocked because they are not touched
        //    by the AnyOfValidator.validate logic we are exercising.
        // -----------------------------------------------------------------
        validationContext = new ValidationContext(
                uriFactory,
                urnFactory,
                metaSchema,
                jsonSchemaFactory,
                config);

        // -----------------------------------------------------------------
        // 2. ExecutionContext – a simple instance with a fresh CollectorContext.
        // -----------------------------------------------------------------
        executionContext = new ExecutionContext();   // uses default CollectorContext

        // -----------------------------------------------------------------
        // 3. Prepare a dummy ANY‑OF schema node (an array with two subschemas).
        // -----------------------------------------------------------------
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode());   // {} – first subschema
        anyOfArray.add(mapper.createObjectNode());   // {} – second subschema

        // -----------------------------------------------------------------
        // 4. Mock the ValidationContext#newSchema method so that it returns
        //    our own mock JsonSchema objects (one per array element).
        // -----------------------------------------------------------------
        JsonSchema subSchema1 = mock(JsonSchema.class);
        JsonSchema subSchema2 = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(subSchema1)   // first call → first subschema
                .thenReturn(subSchema2);  // second call → second subschema

        // -----------------------------------------------------------------
        // 5. Instantiate the validator under test.
        // -----------------------------------------------------------------
        anyOfValidator = new AnyOfValidator(
                "anyOfTest",          // schemaPath (arbitrary)
                anyOfArray,           // the ANY‑OF node
                null,                 // parentSchema – not needed for these tests
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No static resources to clean up.
    }

    /* --------------------------------------------------------------------- *
     * Test case 1 – one subschema validates successfully, the other returns
     * errors. According to the implementation, as soon as a subschema validates
     * without errors the validator clears the accumulated errors and returns
     * an empty set.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenOneSubschemaValid_returnsEmptySet() {
        // -----------------------------------------------------------------
        // Arrange – first subschema returns no errors, second returns one.
        // -----------------------------------------------------------------
        JsonSchema subSchema1 = validationContext.newSchema("anyOfTest/0", null, null);
        JsonSchema subSchema2 = validationContext.newSchema("anyOfTest/1", null, null);

        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema2.hasTypeValidator()).thenReturn(false);

        // first subschema – successful validation
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        // second subschema – produces a single ValidationMessage
        ValidationMessage error = mock(ValidationMessage.class);
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(error));

        JsonNode node = mapper.createObjectNode();   // arbitrary instance node
        JsonNode root = mapper.createObjectNode();   // arbitrary root node
        String at = "$";

        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);

        // -----------------------------------------------------------------
        // Assert – because the first subschema succeeded, result must be empty.
        // -----------------------------------------------------------------
        assertTrue(result.isEmpty(), "Result should be empty when any subschema validates");
    }

    /* --------------------------------------------------------------------- *
     * Test case 2 – all subschemas return errors. The validator must aggregate
     * those errors (excluding “required” errors when appropriate) and return
     * them unchanged.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenAllSubschemasInvalid_aggregatesErrors() {
        // -----------------------------------------------------------------
        // Arrange – both subschemas return distinct ValidationMessage objects.
        // -----------------------------------------------------------------
        JsonSchema subSchema1 = validationContext.newSchema("anyOfTest/0", null, null);
        JsonSchema subSchema2 = validationContext.newSchema("anyOfTest/1", null, null);

        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema2.hasTypeValidator()).thenReturn(false);

        ValidationMessage err1 = mock(ValidationMessage.class);
        ValidationMessage err2 = mock(ValidationMessage.class);
        when(err1.getType()).thenReturn("type1");
        when(err2.getType()).thenReturn("type2");

        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(err1));
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(err2));

        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "$";

        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);

        // -----------------------------------------------------------------
        // Assert – both errors must be present.
        // -----------------------------------------------------------------
        assertEquals(2, result.size(), "Both validation errors should be aggregated");
        assertTrue(result.containsAll(Set.of(err1, err2)), "Result set must contain the two mocked errors");
    }
}
```