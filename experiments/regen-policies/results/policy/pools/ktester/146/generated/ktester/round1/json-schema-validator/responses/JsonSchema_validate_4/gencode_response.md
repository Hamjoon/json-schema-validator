```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode, ExecutionContextCustomizer)}.
 *
 * The tests cover successful validation, validation failures, customizer effects,
 * edge‑cases (null input, exceptions from the customizer) and $ref handling.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_4_Test {

    private static JsonSchemaFactory factory;
    private ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – the public way to obtain a JsonSchemaFactory
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        mapper = new ObjectMapper();
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for this test class
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally
    }

    /* --------------------------------------------------------------------- */
    /*  Successful validation                                               */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_Successful_NoCustomizer_ReturnsEmptySet() throws Exception {
        // schema: name (string) and age (integer) required
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "age":  { "type": "integer" }
                  },
                  "required": [ "name", "age" ]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode validNode = mapper.readTree("""{ "name":"John","age":30 }""");

        Set<ValidationMessage> messages = schema.validate(validNode, null);
        assertTrue(messages.isEmpty(), "A valid document must produce no validation messages");
    }

    @Test
    void validate_Successful_NoOpCustomizer_ReturnsEmptySet(@Mock ExecutionContextCustomizer noop) {
        // same schema as above
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "age":  { "type": "integer" }
                  },
                  "required": [ "name", "age" ]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode validNode = mapper.createObjectNode()
                .put("name", "John")
                .put("age", 30);

        // a no‑op customizer does nothing
        doAnswer(invocation -> {
            // simply return, no mutation
            return null;
        }).when(noop).customize(any());

        Set<ValidationMessage> messages = schema.validate(validNode, noop);
        assertTrue(messages.isEmpty(), "No‑op customizer must not affect a successful validation");
        verify(noop, times(1)).customize(any());
    }

    /* --------------------------------------------------------------------- */
    /*  Validation failures                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_Failure_TypeMismatches_ReturnsTwoMessages() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "age":  { "type": "integer" }
                  },
                  "required": [ "name", "age" ]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode badNode = mapper.readTree("""{ "name":123, "age":"thirty" }""");

        Set<ValidationMessage> messages = schema.validate(badNode, null);
        assertEquals(2, messages.size(), "Both 'name' and 'age' have wrong types");

        // simple sanity check – messages should contain the property names
        List<String> msgs = messages.stream().map(ValidationMessage::getPath).toList();
        assertTrue(msgs.contains("$.name"), "Message for 'name' must be present");
        assertTrue(msgs.contains("$.age"), "Message for 'age' must be present");
    }

    @Test
    void validate_MissingRequired_DisabledByCustomizer_ReturnsEmptySet() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": [ "name" ]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode emptyNode = mapper.readTree("{}");

        // customizer disables the required‑field validator
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateRequired(false);

        Set<ValidationMessage> messages = schema.validate(emptyNode, customizer);
        assertTrue(messages.isEmpty(),
                "Disabling the required validator should allow an otherwise invalid instance to pass");
    }

    /* --------------------------------------------------------------------- */
    /*  Edge cases & error handling                                         */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_NullRootNode_ThrowsException() {
        String schemaJson = """
                { "$schema":"https://json-schema.org/draft/2020-12/schema", "type":"object" }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class,
                () -> schema.validate((JsonNode) null, null),
                "Passing a null JsonNode should result in NPE");
    }

    @Test
    void validate_CustomizerThrowsRuntimeException_Propagates() throws Exception {
        String schemaJson = """
                { "$schema":"https://json-schema.org/draft/2020-12/schema", "type":"object" }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("{}");

        ExecutionContextCustomizer exploding = ctx -> {
            throw new RuntimeException("customizer failure");
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(node, exploding),
                "Exception from customizer must propagate");
        assertEquals("customizer failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  Customizer modifies validation behaviour                             */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_DisableMinimumValidator_AllowsNegativeValue() throws Exception {
        // schema with minimum 0 on property "price"
        String schemaJson = """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "properties":{ "price":{ "type":"number", "minimum":0 } },
                  "required":["price"]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""{ "price": -5 }""");

        // customizer disables the minimum validator
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateMin(false);

        Set<ValidationMessage> messages = schema.validate(node, customizer);
        assertTrue(messages.isEmpty(),
                "When the minimum validator is disabled the negative value must not produce an error");
    }

    @Test
    void validate_DisableRequiredValidator_AllowsMissingPrice() throws Exception {
        // same schema as above
        String schemaJson = """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "properties":{ "price":{ "type":"number", "minimum":0 } },
                  "required":["price"]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("{}");

        // disable required validator
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateRequired(false);

        Set<ValidationMessage> messages = schema.validate(node, customizer);
        assertTrue(messages.isEmpty(),
                "Disabling the required validator should allow the missing 'price' property");
    }

    @Test
    void validate_FailFastEnabled_ReturnsSingleMessage() throws Exception {
        // schema with three independent constraints
        String schemaJson = """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "properties":{
                    "name":{ "type":"string" },
                    "age": { "type":"integer" },
                    "email":{ "type":"string", "format":"email" }
                  },
                  "required":["name","age","email"]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""{ "name":123, "age":"thirty", "email":null }""");

        // enable fail‑fast mode
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setFailFast(true);

        Set<ValidationMessage> messages = schema.validate(node, customizer);
        assertEquals(1, messages.size(),
                "Fail‑fast should stop after the first encountered validation error");
    }

    /* --------------------------------------------------------------------- */
    /*  Array handling                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_ArraySchema_TypeMismatchInSecondElement() throws Exception {
        // schema: array of objects with integer "id"
        String schemaJson = """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"array",
                  "items":{
                    "type":"object",
                    "properties":{ "id":{ "type":"integer" } },
                    "required":["id"]
                  },
                  "minItems":1
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""[ {"id":1}, {"id":"two"} ]""");

        Set<ValidationMessage> messages = schema.validate(node, null);
        assertFalse(messages.isEmpty(), "A type mismatch in the second element must be reported");
        // ensure the path points to the second element
        boolean containsSecond = messages.stream()
                .anyMatch(m -> m.getPath().equals("$[1].id"));
        assertTrue(containsSecond, "Message should reference $.[1].id");
    }

    @Test
    void validate_ArraySchema_MinItemsDisabled_EmptyArrayPasses() throws Exception {
        // same schema as above
        String schemaJson = """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"array",
                  "items":{
                    "type":"object",
                    "properties":{ "id":{ "type":"integer" } },
                    "required":["id"]
                  },
                  "minItems":1
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode emptyArray = mapper.readTree("[]");

        // disable the min‑items validator
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateMinItems(false);

        Set<ValidationMessage> messages = schema.validate(emptyArray, customizer);
        assertTrue(messages.isEmpty(),
                "When minItems validation is disabled an empty array must pass");
    }

    /* --------------------------------------------------------------------- */
    /*  $ref resolution errors                                               */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_RefWithInvalidPercentEncoding_ThrowsUnsupportedEncodingException() {
        // schema that contains a malformed $ref
        String schemaJson = """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "$ref":"invalid%ZZschema.json"
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode dummy = mapper.createObjectNode();

        // the exception is thrown during validation when the $ref is resolved
        assertThrows(UnsupportedEncodingException.class,
                () -> schema.validate(dummy, null),
                "Malformed percent‑encoding in $ref must result in UnsupportedEncodingException");
    }

    @Test
    void validate_RefToNonexistentSchema_ThrowsIllegalArgumentException() {
        // $ref points to a file that does not exist
        String schemaJson = """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "$ref":"nonexistent-schema.json"
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode dummy = mapper.createObjectNode();

        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(dummy, null),
                "A $ref that cannot be resolved must cause an IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    /*  Internationalisation (i18n)                                         */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_MessageSourceFrench_ProducesFrenchMessage(@Mock ExecutionContextCustomizer customizer) throws Exception {
        String schemaJson = """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "properties":{ "name":{ "type":"string" } },
                  "required":["name"]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""{ "name":123 }""");

        // customizer switches the MessageSource to French
        ExecutionContextCustomizer frenchCustomizer = ctx -> ctx.setMessageSource(MessageSource.forLocale(Locale.FRENCH));

        Set<ValidationMessage> messages = schema.validate(node, frenchCustomizer);
        assertFalse(messages.isEmpty(), "Validation should produce a message");
        String text = messages.iterator().next().getMessage();
        // we only assert that the message contains a French word (e.g., "type")
        assertTrue(text.toLowerCase(Locale.FRENCH).contains("type") ||
                   text.toLowerCase(Locale.FRENCH).contains("chaîne"),
                "Message should be localized to French");
    }

    /* --------------------------------------------------------------------- */
    /*  Invalid configuration via customizer                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_CustomizerSetsNegativeMaxDepth_ThrowsIllegalArgumentException() {
        String schemaJson = """
                { "$schema":"https://json-schema.org/draft/2020-12/schema", "type":"object" }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.createObjectNode();

        ExecutionContextCustomizer badCustomizer = ctx -> ctx.getConfig().setMaxDepth(-1);

        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(node, badCustomizer),
                "Negative maxDepth must be rejected with IllegalArgumentException");
    }

    @Test
    void validate_CustomizerDisablesAllValidators_ThrowsIllegalStateException() {
        String schemaJson = """
                { "$schema":"https://json-schema.org/draft/2020-12/schema", "type":"object" }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.createObjectNode();

        ExecutionContextCustomizer disableAll = ctx -> ctx.getConfig().setValidateAll(false);

        assertThrows(IllegalStateException.class,
                () -> schema.validate(node, disableAll),
                "Disabling all validators should make the schema unusable and raise IllegalStateException");
    }

    /* --------------------------------------------------------------------- */
    /*  MessageSource null handling                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_NullMessageSource_ThrowsNullPointerException() throws Exception {
        String schemaJson = """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "properties":{ "age":{ "type":"integer" } },
                  "required":["age"]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""{ "age":"thirty" }""");

        ExecutionContextCustomizer nullMessageSource = ctx -> ctx.setMessageSource(null);

        assertThrows(NullPointerException.class,
                () -> schema.validate(node, nullMessageSource),
                "A null MessageSource must cause NPE when a message is formatted");
    }

    /* --------------------------------------------------------------------- */
    /*  UnsupportedEncodingException from URLDecoder (internal)            */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_RefWithIncompletePercentEscape_ThrowsUnsupportedEncodingException() {
        String schemaJson = """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "$ref":"#/definitions/invalid%2"
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode dummy = mapper.createObjectNode();

        assertThrows(UnsupportedEncodingException.class,
                () -> schema.validate(dummy, null),
                "Incomplete percent‑escape in $ref fragment must raise UnsupportedEncodingException");
    }
}
```