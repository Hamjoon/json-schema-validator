package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchema.findAncestor() tests")
class JsonSchema_findAncestor_Test {

    /**
     * Mock representing the top‑most schema (no parent).
     */
    @Mock
    JsonSchema rootSchema;

    /**
     * Mock representing a child of {@link #rootSchema}.
     */
    @Mock
    JsonSchema childSchema;

    /**
     * Mock representing a grand‑child of {@link #rootSchema}.
     */
    @Mock
    JsonSchema grandChildSchema;

    /**
     * Minimal concrete implementation of {@link JsonSchema} used in the tests.
     * It stores a reference to its parent and delegates {@code findAncestor()}
     * to the default implementation defined in the interface.
     */
    private static class SimpleJsonSchema implements JsonSchema {

        private final JsonSchema parent;

        SimpleJsonSchema(JsonSchema parent) {
            this.parent = parent;
        }

        @Override
        public JsonSchema getParentSchema() {
            return parent;
        }

        /* -----------------------------------------------------------------
         * The remaining methods of the {@link JsonSchema} interface are not
         * required for the behaviour under test.  They are implemented with
         * harmless defaults so that the class can be instantiated.
         * ----------------------------------------------------------------- */
        @Override
        public JsonSchema findLexicalRoot() {
            return this;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return null;
        }

        @Override
        public com.fasterxml.jackson.databind.JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public JsonSchemaFactory getFactory() {
            return null;
        }

        @Override
        public String getSchemaPath() {
            return "";
        }

        @Override
        public String getId() {
            return "";
        }

        @Override
        public boolean isSchemaResourceRoot() {
            return false;
        }

        @Override
        public boolean isRef() {
            return false;
        }

        @Override
        public boolean isConditional() {
            return false;
        }

        @Override
        public boolean isSchemaNode(com.fasterxml.jackson.databind.JsonNode node) {
            return false;
        }

        @Override
        public JsonSchema getRefSchema() {
            return null;
        }

        @Override
        public Set<ValidationMessage> validate(com.fasterxml.jackson.databind.JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> validate(com.fasterxml.jackson.databind.JsonNode node, com.networknt.schema.ExecutionContext ctx) {
            return Collections.emptySet();
        }

        @Override
        public void initialize() {
        }

        @Override
        public void setEvaluationParentSchema(JsonSchema parent) {
        }

        @Override
        public void setParentSchema(JsonSchema parent) {
        }

        @Override
        public void setSchemaPath(String path) {
        }

        @Override
        public void setId(String id) {
        }

        @Override
        public void setSchemaNode(com.fasterxml.jackson.databind.JsonNode schemaNode) {
        }

        @Override
        public void setFactory(JsonSchemaFactory factory) {
        }

        @Override
        public void setSchemaResourceRoot(boolean root) {
        }

        @Override
        public void setRefSchema(JsonSchema refSchema) {
        }

        @Override
        public void setEvaluationParentSchema(JsonSchema evaluationParentSchema) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build the parent chain: root <- child <- grandChild
        rootSchema = new SimpleJsonSchema(null);
        childSchema = new SimpleJsonSchema(rootSchema);
        grandChildSchema = new SimpleJsonSchema(childSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Null out references to help the garbage collector; not strictly needed.
        rootSchema = null;
        childSchema = null;
        grandChildSchema = null;
    }

    @Test
    void findAncestorReturnsSelfWhenNoParent() {
        JsonSchema result = rootSchema.findAncestor();
        assertSame(rootSchema, result, "When a schema has no parent, findAncestor() must return the schema itself.");
    }

    @Test
    void findAncestorWalksUpTheChainToRoot() {
        JsonSchema result = grandChildSchema.findAncestor();
        assertSame(rootSchema, result, "findAncestor() should return the top‑most ancestor in the parent chain.");
    }

    @Test
    void findAncestorHandlesTwoLevelChain() {
        JsonSchema result = childSchema.findAncestor();
        assertSame(rootSchema, result, "For a one‑level child, findAncestor() must still return the root schema.");
    }

    @Test
    void findAncestorSelfReferencingParentCycleThrowsStackOverflow() {
        // Create a schema whose parent points to itself → infinite recursion.
        SimpleJsonSchema selfCycle = new SimpleJsonSchema(null);
        // Manually create the cycle.
        // (We cannot change the final field, so we use a subclass that overrides getParentSchema.)
        SimpleJsonSchema cyclic = new SimpleJsonSchema(selfCycle) {

            @Override
            public JsonSchema getParentSchema() {
                return this;
            }
        };
        assertThrows(StackOverflowError.class, cyclic::findAncestor, "A self‑referencing parent should cause a StackOverflowError.");
    }

    @Test
    void findAncestorIndirectCycleThrowsStackOverflow() {
        // A → B → C → A creates an indirect cycle.
        SimpleJsonSchema a = new SimpleJsonSchema(null);
        SimpleJsonSchema b = new SimpleJsonSchema(a);
        SimpleJsonSchema c = new SimpleJsonSchema(b);
        // Introduce the cycle by overriding getParentSchema on 'a'.
        SimpleJsonSchema cyclicA = new SimpleJsonSchema(c) {

            @Override
            public JsonSchema getParentSchema() {
                return c;
            }
        };
        assertThrows(StackOverflowError.class, cyclicA::findAncestor, "An indirect parent cycle should eventually cause a StackOverflowError.");
    }

    @Test
    void findAncestorDeepHierarchyTenLevels() {
        // Build a chain of 10 schemas (leaf … root).
        List<SimpleJsonSchema> chain = new ArrayList<>();
        // root (index 0) has null parent.
        chain.add(new SimpleJsonSchema(null));
        for (int i = 1; i < 10; i++) {
            chain.add(new SimpleJsonSchema(chain.get(i - 1)));
        }
        SimpleJsonSchema leaf = chain.get(9);
        SimpleJsonSchema root = chain.get(0);
        JsonSchema result = leaf.findAncestor();
        assertSame(root, result, "A ten‑level hierarchy should return the top‑most root schema after recursion.");
    }

    @Test
    void findAncestorIdempotentCallsReturnSameInstance() {
        JsonSchema firstCall = grandChildSchema.findAncestor();
        JsonSchema secondCall = grandChildSchema.findAncestor();
        assertSame(rootSchema, firstCall, "First call should return the root schema.");
        assertSame(rootSchema, secondCall, "Second call should return the same root schema instance, proving idempotence.");
    }

    @Test
    void findAncestorDoesNotModifyParentReference() {
        JsonSchema parentBefore = childSchema.getParentSchema();
        // invoke method under test
        childSchema.findAncestor();
        JsonSchema parentAfter = childSchema.getParentSchema();
        assertSame(parentBefore, parentAfter, "The parent reference must remain unchanged after findAncestor() execution.");
    }

    @Test
    void findAncestorPropagatesExceptionFromParent() {
        // Simulate a parent whose findAncestor() throws an exception.
        JsonSchema problematicParent = new SimpleJsonSchema(null) {

            @Override
            public JsonSchema findAncestor() {
                throw new IllegalArgumentException("parent failure");
            }
        };
        JsonSchema child = new SimpleJsonSchema(problematicParent);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, child::findAncestor, "Exception thrown by the parent should propagate unchanged.");
        assertEquals("parent failure", thrown.getMessage(), "Propagated exception should retain the original message.");
    }
}
