###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.PathType;

/**
 * Unit tests for {@link JsonSchema}. The tests focus on the public and protected
 * behaviours that can be exercised without needing the full library runtime.
 * The test class resides in the same package so that the protected constructor
 * of {@link JsonSchema} can be used.
 */
public class JsonSchemaTest {

    /** Simple {@link JsonValidator} used to verify {@link JsonSchema#initializeValidators()} */
    private static class DummyValidator implements JsonValidator {
        private boolean preloaded = false;

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                JsonNode rootNode, JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return SchemaLocation.of("http://example.com/dummy");
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return new JsonNodePath(PathType.JSON_POINTER);
        }

        @Override
        public String getKeyword() {
            return "dummy";
        }

        @Override
        public void preloadJsonSchema() {
            this.preloaded = true;
        }

        public boolean isPreloaded() {
            return preloaded;
        }
    }

    /** Helper to build a minimal ValidationContext with a usable config */
    private ValidationContext createValidationContext() {
        // JsonMetaSchema and JsonSchemaFactory are not required for the methods under test,
        // therefore we can safely pass null.
        return new ValidationContext(null, null, new SchemaValidatorsConfig(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());
    }

    /** Helper to create a JsonSchema using the protected constructor */
    private JsonSchema createSchema(JsonNode schemaNode, String id, SchemaLocation location,
            JsonSchema parent) {
        return new JsonSchema(
                /* JsonSchema fields */
                Collections.emptyList(), // validators
                false,                    // validatorsLoaded
                false,                    // recursiveAnchor
                null,                     // typeValidator
                id,                       // id
                /* BaseJsonValidator fields */
                false,                    // suppressSubSchemaRetrieval
                schemaNode,               // schemaNode
                createValidationContext(), // validationContext
                null, null, null,         // errorMessageType, errorMessageKeyword, messageSource
                null,                     // keyword
                parent,                   // parentSchema
                location,                 // schemaLocation
                new JsonNodePath(PathType.JSON_POINTER), // evaluationPath
                null,                     // evaluationParentSchema
                null);                    // errorMessage
    }

    @Test
    public void testGetIdAndRecursiveAnchor() {
        JsonNode node = BooleanNode.TRUE;
        SchemaLocation loc = SchemaLocation.of("http://example.com/schema");
        JsonSchema schema = createSchema(node, "myId", loc, null);

        assertEquals("myId", schema.getId());
        assertFalse(schema.isRecursiveAnchor());
    }

    @Test
    public void testGetNode() {
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("name", "John");
        obj.put("age", 30);
        JsonSchema schema = createSchema(obj, null, SchemaLocation.of("http://example.com/schema"), null);

        JsonNode nameNode = schema.getNode("name");
        assertNotNull(nameNode);
        assertEquals("John