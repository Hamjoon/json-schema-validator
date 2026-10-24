###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link OneOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>exactly one matching subschema (valid case)</li>
 *   <li>no matching subschema (error case)</li>
 *   <li>more than one matching subschema (error case)</li>
 *   <li>walk‑delegation when {@code shouldValidateSchema == false}</li>
 * </ul>
 *
 * The tests avoid any external mocking libraries – they use only the public
 * constructors / methods listed in the problem statement and lightweight
 * stub implementations of {@link JsonSchema}.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple stub that can be configured to be “valid” or “invalid”. */
    private static class StubSchema extends JsonSchema {
        private final boolean valid;
        private boolean walkCalled = false;

        StubSchema(boolean valid) {
            // All arguments are allowed to be {@code null} for the purpose of the test.
            super(null, null, null);
            this.valid = valid;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode jsonNode,
                                                JsonNode rootNode,
                                                String at) {
            // If the schema is configured as valid we return an empty set,
            // otherwise we return a set containing a single dummy ValidationMessage.
            if (valid) {
                return Collections.emptySet();
            } else {
                // Build a dummy ValidationMessage – the library permits {@code null}
                // for the enum / MessageFormat arguments.
                ValidationMessage vm = ValidationMessage.of(
                        "stub",               // type
                        null,                // ErrorMessageType (allowed to be null)
                        null,                // MessageFormat (allowed to be null)
                        at,                  // the JSON‑pointer location
                        "/stubSchema",       // dummy schema path
                        new String[]{"stub"} // dummy arguments
                );
                Set<ValidationMessage> set = new HashSet<>();
                set.add(vm);
                return set;
            }
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext executionContext,
                                           JsonNode node,
                                           JsonNode rootNode,
                                           String at,
                                           boolean shouldValidateSchema) {
            // Record that walk() has been invoked – the real implementation is irrelevant.
            this.walkCalled = true;
            // When walking we delegate to {@code validate} only if the flag is true.
            if (shouldValidateSchema) {
                return validate(executionContext, node, rootNode, at);
            }
            // Otherwise return an empty set (the OneOfValidator discards these).
            return Collections.emptySet();
        }

        public boolean isWalkCalled() {
            return walkCalled;
        }
    }

    private JsonNode dummyNode;       // a trivial JSON value used as the node under test
    private ExecutionContext execCtx; // execution context with a fresh CollectorContext

    @Before
    public void setUp() {
        dummyNode = MAPPER.createObjectNode();               // {} – content is irrelevant
        execCtx = new ExecutionContext(new CollectorContext()); // fresh collector
    }

    /**
     * Helper that creates a {@link OneOfValidator} with the supplied stub schemas.
     *
     * @param schemas the stub schemas that should constitute the “oneOf” list
     * @return a fully‑initialised validator ready for testing
     */
    private OneOfValidator createValidatorWithStubs(StubSchema... schemas) throws Exception {
        // Build a validator with an *empty* schema array – we will inject the stubs afterwards.
        JsonNode emptyArray = MAPPER.createArrayNode(); // schemaNode size == 0
        OneOfValidator validator = new OneOfValidator("testPath", emptyArray, null, null);

        // Use reflection to obtain the private “schemas” list and populate it.
        Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<JsonSchema> list = (java.util.List<JsonSchema>) f.get(validator);
        Collections.addAll(list, schemas);
        return validator;
    }

    @Test
    public void shouldValidateSuccessfullyWhenExactlyOneSchemaMatches() throws Exception {
        // first schema is invalid, second schema is valid → exactly one match
        StubSchema invalid = new StubSchema(false);
        StubSchema valid   = new StubSchema(true);

        OneOfValidator validator = createValidatorWithStubs(invalid, valid);

        Set<ValidationMessage> result = validator.validate(execCtx, dummyNode, dummyNode, "/");

        // No errors expected because exactly one subschema matched.
        assertTrue("Expected no validation errors", result.isEmpty());
    }

    @Test
    public void shouldReportErrorWhenNoSchemaMatches() throws Exception {
        // both schemas are invalid → zero matches
        StubSchema invalid1 = new StubSchema(false);
        StubSchema invalid2 = new StubSchema(false);

        OneOfValidator validator = createValidatorWithStubs(invalid1, invalid2);

        Set<ValidationMessage> result = validator.validate(execCtx, dummyNode, dummyNode, "/");

        // The validator must emit at least one error (the OneOf error message).
        assertFalse("Expected validation errors", result.isEmpty());

        // The message path should be the location we passed (“/”).
        boolean pathMatches = result.stream().anyMatch(m -> "/".equals(m.getPath()));
        assertTrue("At least one message should have path '/'", pathMatches);
    }

    @Test
    public void shouldReportErrorWhenMoreThanOneSchemaMatches() throws Exception {
        // both schemas are valid → two matches → error
        StubSchema valid1 = new StubSchema(true);
        StubSchema valid2 = new StubSchema(true);

        OneOfValidator validator = createValidatorWithStubs(valid1, valid2);

        Set<ValidationMessage> result = validator.validate(execCtx, dummyNode, dummyNode, "/");

        // Expect exactly one OneOf error (no child errors because both subschemas are valid).
        assertEquals("Exactly one validation message expected", 1, result.size());

        ValidationMessage vm = result.iterator().next();
        // The message's path must be the location we validated.
        assertEquals("Message path should be '/'", "/", vm.getPath());
        // The arguments contain the number of matching schemas (2 in this case).
        assertArrayEquals("Argument should indicate '2' matching schemas",
                new String[]{"2"}, vm.getArguments());
    }

    @Test
    public void walkShouldDelegateWhenShouldValidateSchemaIsFalse() throws Exception {
        // Create two stub schemas; we will verify that their walk() method is invoked.
        StubSchema schemaA = new StubSchema(true);
        StubSchema schemaB = new StubSchema(false);

        OneOfValidator validator = createValidatorWithStubs(schemaA, schemaB);

        // Invoke walk with shouldValidateSchema = false – the validator must delegate.
        Set<ValidationMessage> messages = validator.walk(execCtx, dummyNode, dummyNode, "/", false);

        // Walk should not perform any validation, therefore the returned set is empty.
        assertTrue("Walk with shouldValidateSchema=false must return empty set", messages.isEmpty());

        // Both child schemas must have received the walk call.
        assertTrue("Schema A should have been walked", schemaA.isWalkCalled());
        assertTrue("Schema B should have been walked", schemaB.isWalkCalled());
    }
}
```
###Test END##