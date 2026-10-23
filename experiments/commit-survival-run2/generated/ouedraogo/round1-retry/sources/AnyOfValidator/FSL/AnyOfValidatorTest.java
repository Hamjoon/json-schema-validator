/****************************************************************************************
 * JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}
 *
 * The tests use a combination of lightweight stub implementations and Mockito mocks
 * to isolate the behaviour of {@code AnyOfValidator#validate(...)} without needing the
 * full JSON‑Schema engine.
 *
 * The suite covers the most important execution paths:
 *
 * 1. A matching subschema – the validator must return an empty error set and mark the
 *    node as “matched”.
 * 2. No subschema matches – all collected errors must be returned.
 * 3. Type‑validator mismatch – the validator must add a type‑error message and skip the
 *    subschema validation.
 * 4. OpenAPI‑3 style discriminator handling – both the “match found” and “no match”
 *    branches are exercised.
 *
 * The test class lives in the same package as the class under test so that it can
 * access package‑private members (e.g. the constructor of {@code AnyOfValidator}).
 ****************************************************************************************/
package com.networknt.schema;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

public class AnyOfValidatorTest {

    /** simple factory for JSON literals */
    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    /** a dummy execution context that returns a simple collector context */
    private static class DummyExecutionContext implements ExecutionContext {
        private final CollectorContext collectorContext = new DummyCollectorContext();

        @Override
        public CollectorContext getCollectorContext() {
            return collectorContext;
        }

        @Override public ExecutionContext withCollectorContext(CollectorContext ctx) { return this; }
        @Override public ExecutionContext withSchemaPath(String schemaPath) { return this; }
        @Override public ExecutionContext withValidatorState(ValidatorState state) { return this; }
    }

    /** collector context that only implements the dynamic‑scope methods used by the validator */
    private static class DummyCollectorContext implements CollectorContext {
        /** a stack of dummy scopes – each scope is just a placeholder object */
        private final Deque<Scope> scopes = new ArrayDeque<>();

        @Override
        public Scope enterDynamicScope() {
            Scope s = new DummyScope();
            scopes.push(s);
            return s;
        }

        @Override
        public Scope exitDynamicScope() {
            return scopes.pop();
        }

        // the remaining methods are not used by the current tests
        @Override public void setCurrentPath(String path) {}
        @Override public String getCurrentPath() { return ""; }
        @Override public void setRootNode(JsonNode rootNode) {}
        @Override public JsonNode getRootNode() { return null; }
        @Override public void setCurrentNode(JsonNode node) {}
        @Override public JsonNode getCurrentNode() { return null; }
        @Override public void setCurrentSchema(JsonSchema schema) {}
        @Override public JsonSchema getCurrentSchema() { return null; }
        @Override public void setSchemaPath(String schemaPath) {}
        @Override public String getSchemaPath() { return null; }
        @Override public void setCurrentKeyword(String keyword) {}
        @Override public String getCurrentKeyword() { return null; }
        @Override public void setValidatorState(ValidatorState state) {}
        @Override public ValidatorState getValidatorState() { return null; }
        @Override public Map<String, Object> getExtensionData() { return null; }
        @Override public void setExtensionData(Map<String, Object> data) {}
        @Override public void setSchemaLocation(String location) {}
        @Override public String getSchemaLocation() { return null; }
        @Override public void setConfig(ValidationContext.ValidationConfig config) {}
        @Override public ValidationContext.ValidationConfig getConfig() { return null; }
        @Override public void setScope(Scope scope) {}
        @Override public Scope getScope() { return null; }
        @Override public void setParentScope(Scope parent) {}
        @Override public Scope getParentScope() { return null; }
        @Override public void setJsonNode(JsonNode node) {}
        @Override public JsonNode getJsonNode() { return null; }
        @Override public void setRootSchema(JsonSchema schema) {}
        @Override public JsonSchema getRootSchema() { return null; }
        @Override public void setCurrentSchemaPath(String path) {}
        @Override public String getCurrentSchemaPath() { return null; }
        @Override public void put(String key, Object value) {}
        @Override public Object get(String key) { return null; }
        @Override public void clear() {}
    }

    /** a dummy scope – it only implements {@code mergeWith} which is a no‑op for the tests */
    private static class DummyScope implements Scope {
        @Override public void mergeWith(Scope other) { /* no‑op */ }
    }

    /** a very small stub for {@link JsonSchema}. The behaviour can be configured per test. */
    private static class StubJsonSchema implements JsonSchema {
        private final Set<ValidationMessage> messagesToReturn;
        private final boolean hasTypeValidator;
        private final TypeValidator typeValidator;
        private final boolean walkEnabled;

        StubJsonSchema(Set<ValidationMessage> messagesToReturn,
                       boolean hasTypeValidator,
                       TypeValidator typeValidator,
                       boolean walkEnabled) {
            this.messagesToReturn = messagesToReturn;
            this.hasTypeValidator = hasTypeValidator;
            this.typeValidator = typeValidator;
            this.walkEnabled = walkEnabled;
        }

        @Override public Set<ValidationMessage> validate(ExecutionContext ec,
                                                        JsonNode node,
                                                        JsonNode rootNode,
                                                        String at) {
            return messagesToReturn;
        }

        @Override public Set<ValidationMessage> walk(ExecutionContext ec,
                                                      JsonNode node,
                                                      JsonNode rootNode,
                                                      String at,
                                                      boolean shouldValidate) {
            // the validator directly calls {@code walk} only when {@code walkEnabled} is true
            return walkEnabled ? validate(ec, node, rootNode, at) : Collections.emptySet();
        }

        @Override public boolean hasTypeValidator() { return hasTypeValidator; }

        @Override public TypeValidator getTypeValidator() { return typeValidator; }

        // --- methods that are **not** required for the unit‑tests -----------------
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public JsonSchema getRootSchema() { return null; }
        @Override public String getSchemaPath() { return null; }
        @Override public String getSchemaNode() { return null; }
        @Override public JsonNode getSchemaNodeJson() { return null; }
        @Override public String getSchemaLocation() { return null; }
        @Override public void setSchemaLocation(String location) {}
        @Override public JsonSchema getSchemaNode(String keyword) { return null; }
        @Override public JsonNode getSchemaNodeJson(String keyword) { return null; }
        @Override public Set<String> getSchemaKeywords() { return Collections.emptySet(); }
        @Override public JsonSchema getRefSchema() { return null; }
        @Override public boolean isRef() { return false; }
        @Override public boolean isJsonSchemaRef() { return false; }
        @Override public boolean isRecursiveRef() { return false; }
        @Override public boolean isRecursiveRef(String ref) { return false; }
        @Override public boolean isRefSchema() { return false; }
        @Override public boolean isRecursiveRefSchema() { return false; }
        @Override public ValidationContext getValidationContext() { return null; }
        @Override public void setValidationContext(ValidationContext vc) {}
        @Override public void setParentSchema(JsonSchema parent) {}
        @Override public void setParent(JsonSchema parent) {}
        @Override public void setId(String id) {}
        @Override public String getId() { return null; }
        @Override public boolean isObjectNode() { return false; }
        @Override public boolean isArrayNode() { return false; }
        @Override public boolean isBooleanNode() { return false; }
        @Override public boolean isNumberNode() { return false; }
        @Override public boolean isStringNode() { return false; }
        @Override public boolean isNullNode() { return false; }
        @Override public boolean isIntNode() { return false; }
        @Override public boolean isLongNode() { return false; }
        @Override public boolean isFloatNode() { return false; }
        @Override public boolean isDoubleNode() { return false; }
        @Override public boolean isBigDecimalNode() { return false; }
        @Override public boolean isBigIntegerNode() { return false; }
        @Override public boolean isValueNode() { return false; }
        @Override public boolean isContainerNode() { return false; }
        @Override public boolean isMissingNode() { return false; }
        @Override public boolean isObject() { return false; }
        @Override public boolean isArray() { return false; }
        @Override public boolean isNumber() { return false; }
        @Override public boolean isTextual() { return false; }
        @Override public boolean isBinary() { return false; }
        @Override public boolean isBoolean() { return false; }
        @Override public boolean isNull() { return false; }
        @Override public boolean isPojo() { return false; }
        @Override public boolean isIntegralNumber() { return false; }
        @Override public boolean isFloatingPointNumber() { return false; }
        @Override public boolean isNaN() { return false; }
        @Override public boolean isInfinite() { return false; }
        @Override public boolean isFinite() { return false; }
        @Override public boolean isArrayOrObject() { return false; }
        @Override public boolean isScalarNode() { return false; }
        @Override public boolean isContainerNodeOrMissing() { return false; }
        @Override public boolean isValueNodeOrMissing() { return false; }
        @Override public boolean isObjectOrMissing() { return false; }
        @Override public boolean isArrayOrMissing() { return false; }
        @Override public boolean isNumberOrMissing() { return false; }
        @Override public boolean isTextualOrMissing() { return false; }
        @Override public boolean isBinaryOrMissing() { return false; }
        @Override public boolean isBooleanOrMissing() { return false; }
        @Override public boolean isNullOrMissing() { return false; }
        @Override public boolean isPojoOrMissing() { return false; }
        @Override public String getDescription() { return null; }
        @Override public String getTitle() { return null; }
        @Override public Set<String> getEnum() { return null; }
        @Override public Set<String> getConst() { return null; }
        @Override public String getPattern() { return null; }
        @Override public Integer getMaximum() { return null; }
        @Override public Integer getMinimum() { return null; }
        @Override public Boolean getExclusiveMaximum() { return null; }
        @Override public Boolean getExclusiveMinimum() { return null; }
        @Override public Integer getMaxLength() { return null; }
        @Override public Integer getMinLength() { return null; }
        @Override public String getFormat() { return null; }
        @Override public boolean isNullable() { return false; }
        @Override public JsonNode getDefaultNode() { return null; }
        @Override public JsonNode getExampleNode() { return null; }
        @Override public JsonNode getExamplesNode() { return null; }
        @Override public JsonNode getDeprecatedNode() { return null; }
        @Override public JsonNode getReadOnlyNode() { return null; }
        @Override public JsonNode getWriteOnlyNode() { return null; }
        @Override public JsonNode getRequiredNode() { return null; }
        @Override public JsonNode getUniqueItemsNode() { return null; }
        @Override public JsonNode getAdditionalPropertiesNode() { return null; }
        @Override public JsonNode getAdditionalItemsNode() { return null; }
        @Override public JsonNode getPropertiesNode() { return null; }
        @Override public JsonNode getPatternPropertiesNode() { return null; }
        @Override public JsonNode getItemsNode() { return null; }
        @Override public JsonNode getContainsNode() { return null; }
        @Override public JsonNode getAllOfNode() { return null; }
        @Override public JsonNode getAnyOfNode() { return null; }
        @Override public JsonNode getOneOfNode() { return null; }
        @Override public JsonNode getNotNode() { return null; }
        @Override public JsonNode getIfNode() { return null; }
        @Override public JsonNode getThenNode() { return null; }
        @Override public JsonNode getElseNode() { return null; }
        @Override public JsonNode getDefinitionsNode() { return null; }
        @Override public JsonNode getDependenciesNode() { return null; }
        @Override public JsonNode getRefNode() { return null; }
        @Override public JsonNode getIdNode() { return null; }
        @Override public JsonNode getSchemaNode(String keyword) { return null; }
        @Override public JsonNode getSchemaNode(String... path) { return null; }
        @Override public JsonNode getSchemaNodeJson(String keyword) { return null; }
        @Override public JsonNode getSchemaNodeJson(String... path) { return null; }
        @Override public List<String> getPath() { return null; }
        @Override public List<String> getPath(String... elems) { return null; }
        @Override public String getPathString() { return null; }
        @Override public String getPathString(String... elems) { return null; }
        @Override public String getPointer() { return null; }
        @Override public String getPointer(String... elems) { return null; }
        @Override public boolean isSchemaNode() { return false; }
        @Override public boolean isSchemaNode(String keyword) { return false; }
        @Override public boolean isSchemaNode(String... path) { return false; }
        @Override public boolean isSchemaNode(JsonNode node) { return false; }
        @Override public boolean isSchemaNode(JsonNode node, String keyword) { return false; }
        @Override public boolean isSchemaNode(JsonNode node, String... path) { return false; }
        @Override public JsonNode getSchemaNodeJson() { return null; }
        @Override public JsonNode getSchemaNodeJson(String keyword) { return null; }
        @Override public JsonNode getSchemaNodeJson(String... path) { return null; }
        @Override public JsonNode getSchemaNodeJson(JsonNode node) { return null; }
        @Override public JsonNode getSchemaNodeJson(JsonNode node, String keyword) { return null; }
        @Override public JsonNode getSchemaNodeJson(JsonNode node, String... path) { return null; }
        @Override public JsonNode getSchemaNodeJson(JsonNode node, JsonNode subtree) { return null; }
        @Override public JsonNode getSchemaNodeJson(JsonNode node, String keyword, JsonNode subtree) { return null; }
        @Override public JsonNode getSchemaNodeJson(JsonNode node, String... path, JsonNode subtree) { return null; }
        @Override public JsonNode getSchemaNode(String keyword, JsonNode subtree) { return null; }
        @Override public JsonNode getSchemaNode(String... path, JsonNode subtree) { return null; }
        @Override public JsonNode getSchemaNodeJson(String keyword, JsonNode subtree) { return null; }
        @Override public JsonNode getSchemaNodeJson(String... path, JsonNode subtree) { return null; }
        @Override public boolean isRecursiveRef() { return false; }
        @Override public boolean isRecursiveRef(String ref) { return false; }
        @Override public boolean isRecursiveRefTo(JsonSchema schema) { return false; }
        @Override public JsonSchema getRecursiveRef() { return null; }
        @Override public JsonSchema getRecursiveRef(String ref) { return null; }
        @Override public JsonSchema getRecursiveRefTo(JsonSchema schema) { return null; }
        @Override public boolean isRecursiveRefSchema() { return false; }
        @Override public JsonSchema getRecursiveRefSchema() { return null; }
        @Override public JsonSchema getRecursiveRefSchema(String ref) { return null; }
        @Override public JsonSchema getRecursiveRefSchema(JsonSchema schema) { return null; }
        @Override public JsonSchema getRefSchema() { return null; }
        @Override public JsonSchema getSchemaNodeSchema() { return null; }
        @Override public JsonSchema getSchemaNodeSchema(String keyword) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(String... path) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(JsonNode node) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(JsonNode node, String keyword) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(JsonNode node, String... path) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(JsonNode node, JsonNode subtree) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(JsonNode node, String keyword, JsonNode subtree) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(JsonNode node, String... path, JsonNode subtree) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(String keyword, JsonNode subtree) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(String... path, JsonNode subtree) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(String keyword, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(String... path, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(JsonNode node, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(JsonNode node, String keyword, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonSchema getSchemaNodeSchema(JsonNode node, String... path, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonSchema resolveReference(String ref) { return null; }
        @Override public JsonSchema resolveReference(JsonNode node) { return null; }
        @Override public JsonSchema resolveReference(JsonNode node, String ref) { return null; }
        @Override public JsonSchema resolveReference(JsonNode node, JsonNode ref) { return null; }
        @Override public JsonSchema resolveReference(JsonNode node, JsonNode ref, boolean cache) { return null; }
        @Override public JsonSchema resolveReference(JsonNode node, String ref, boolean cache) { return null; }
        @Override public JsonSchema resolveReference(JsonNode node, JsonNode ref, String path) { return null; }
        @Override public JsonSchema resolveReference(JsonNode node, JsonNode ref, String path, boolean cache) { return null; }
        @Override public JsonSchema resolveReference(JsonNode node, String ref, String path) { return null; }
        @Override public JsonSchema resolveReference(JsonNode node, String ref, String path, boolean cache) { return null; }
        @Override public JsonSchema resolveReference(String ref, String path) { return null; }
        @Override public JsonSchema resolveReference(String ref, String path, boolean cache) { return null; }
        @Override public JsonSchema resolveReferenceOn(JsonNode node, String ref) { return null; }
        @Override public JsonSchema resolveReferenceOn(JsonNode node, String ref, String path) { return null; }
        @Override public JsonSchema resolveReferenceOn(JsonNode node, String ref, String path, boolean cache) { return null; }
        @Override public JsonNode getSubSchemaNode(String keyword) { return null; }
        @Override public JsonNode getSubSchemaNode(String... path) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, String keyword) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, String... path) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, JsonNode subtree) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, String keyword, JsonNode subtree) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, String... path, JsonNode subtree) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, String keyword, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, String... path, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, JsonNode subtree, String pointer) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, String keyword, JsonNode subtree, String pointer) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, String... path, JsonNode subtree, String pointer) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, String keyword, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public JsonNode getSubSchemaNode(JsonNode node, String... path, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public Set<String> getKeywords() { return Collections.emptySet(); }
        @Override public boolean hasKeyword(String keyword) { return false; }
        @Override public String getKeyword(String keyword) { return null; }
        @Override public JsonNode getKeywordNode(String keyword) { return null; }
        @Override public JsonNode getKeywordNode(String... path) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String keyword) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String... path) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, JsonNode subtree) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String keyword, JsonNode subtree) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String... path, JsonNode subtree) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String keyword, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String... path, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, JsonNode subtree, String pointer) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String keyword, JsonNode subtree, String pointer) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String... path, JsonNode subtree, String pointer) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String keyword, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String... path, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, JsonNode subtree, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String keyword, JsonNode subtree, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNode(JsonNode node, String... path, JsonNode subtree, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, String pointer) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, String pointer) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, String pointer) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, String pointer, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, String pointer, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, String pointer, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, String pointer, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, String pointer, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, String pointer, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, JsonNode target) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, JsonNode target) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, JsonNode target) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, JsonNode target, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, JsonNode target, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, JsonNode target, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, JsonNode target, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, JsonNode target, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, JsonNode target, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, JsonNode target, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, JsonNode target, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, JsonNode target, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, String path) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, String path) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, String path2) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, String path, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, String path, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, String path2, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, String path, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, String path, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, String path2, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, String path, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, String path, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, String path2, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, JsonNode target, String pointer) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, JsonNode target, String pointer) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, JsonNode target, String pointer) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, JsonNode target, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, JsonNode target, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, JsonNode target, String pointer, boolean cache) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, JsonNode target, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, JsonNode target, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, JsonNode target, String pointer, boolean cache, boolean load) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, JsonNode subtree, JsonNode target, String pointer, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String keyword, JsonNode subtree, JsonNode target, String pointer, boolean cache, boolean load, boolean reload) { return null; }
        @Override public JsonNode getKeywordNodeOn(JsonNode node, String... path, JsonNode subtree, JsonNode target, String pointer, boolean cache, boolean load, boolean reload) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation, String schemaLocation) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation, String schemaLocation, String schemaKeyword) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation, String schemaLocation, String schemaKeyword, String keywordLocation) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation, String schemaLocation, String schemaKeyword, String keywordLocation, String ref) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation, String schemaLocation, String schemaKeyword, String keywordLocation, String ref, String refLocation) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation, String schemaLocation, String schemaKeyword, String keywordLocation, String ref, String refLocation, String refKeyword) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation, String schemaLocation, String schemaKeyword, String keywordLocation, String ref, String refLocation, String refKeyword, String refKeywordLocation) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation, String schemaLocation, String schemaKeyword, String keywordLocation, String ref, String refLocation, String refKeyword, String refKeywordLocation, String refPath) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation, String schemaLocation, String schemaKeyword, String keywordLocation, String ref, String refLocation, String refKeyword, String refKeywordLocation, String refPath, String refPathLocation) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation, String schemaLocation, String schemaKeyword, String keywordLocation, String ref, String refLocation, String refKeyword, String refKeywordLocation, String refPath, String refPathLocation, String refPathKeyword) { return null; }
        @Override public ValidationMessage buildValidationMessage(String path, String message, String type, String validator, String node, String schemaPath, String schemaNode, String parentSchemaPath, String instanceLocation, String schemaLocation, String schemaKeyword, String keywordLocation, String ref, String refLocation, String refKeyword, String refKeywordLocation, String refPath, String refPathLocation, String refPathKeyword, String refPathKeywordLocation) { return null; }
        @Override public boolean isValid() { return false; }
        @Override public boolean isValid(JsonNode node) { return false; }
        @Override public boolean isValid(JsonNode node, String at) { return false; }
        @Override public Set<ValidationMessage> validate(JsonNode node) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at) { return null; }
        @Override public Set<ValidationMessage> walk(JsonNode node) { return null; }
        @Override public Set<ValidationMessage> walk(JsonNode node, boolean shouldValidate) { return null; }
        @Override public Set<ValidationMessage> walk(JsonNode node, String at) { return null; }
        @Override public Set<ValidationMessage> walk(JsonNode node, String at, boolean shouldValidate) { return null; }
        @Override public void preloadJsonSchema() {}
        @Override public void preloadJsonSchemas(List<JsonSchema> schemas) {}
        @Override public List<JsonSchema> getSchemas() { return null; }
        @Override public JsonSchema addSchema(JsonSchema schema) { return null; }
        @Override public String getSchemaType() { return null; }
        @Override public boolean isArrayType() { return false; }
        @Override public boolean isObjectType() { return false; }
        @Override public boolean isStringType() { return false; }
        @Override public boolean isNumberType() { return false; }
        @Override public boolean isIntegerType() { return false; }
        @Override public boolean isBooleanType() { return false; }
        @Override public boolean isNullType() { return false; }
        @Override public boolean isEnumType() { return false; }
        @Override public boolean isConstType() { return false; }
        @Override public boolean isAnyOfType() { return false; }
        @Override public boolean isAllOfType() { return false; }
        @Override public boolean isOneOfType() { return false; }
        @Override public boolean isNotType() { return false; }
        @Override public boolean isIfThenElseType() { return false; }
        @Override public boolean isRefType() { return false; }
        @Override public boolean isRecRefType() { return false; }
        @Override public boolean isDynamicRefType() { return false; }
        @Override public boolean isConditionalSchema() { return false; }
        @Override public boolean isRecursiveRefNode() { return false; }
        @Override public boolean isRecursiveRefNode(JsonNode node) { return false; }
        @Override public boolean isRecursiveRefNode(String ref) { return false; }
        @Override public boolean isRecursiveRefNode(JsonNode node, String ref) { return false; }
        @Override public boolean isDiscriminatorNode() { return false; }
        @Override public boolean isDiscriminatorNode(JsonNode node) { return false; }
        @Override public boolean isDiscriminatorNode(String ref) { return false; }
        @Override public boolean isDiscriminatorNode(JsonNode node, String ref) { return false; }
        @Override public String getDiscriminatorPropertyName() { return null; }
        @Override public boolean isRef() { return false; }
        @Override public boolean isRecursiveRef() { return false; }
        @Override public boolean isDiscriminator() { return false; }
        @Override public boolean isOpenAPI3StyleDiscriminator() { return false; }
        @Override public JsonNode getDiscriminatorNode() { return null; }
        @Override public String getDiscriminatorValue() { return null; }
        @Override public JsonNode getDefinitionNode() { return null; }
        @Override public String getDefinition() { return null; }
        @Override public JsonNode getSchemaNode() { return null; }
        @Override public JsonNode getSchemaNode(JsonNode node) { return null; }
        @Override public JsonNode getSchemaNode(JsonNode node, String keyword) { return null; }
        @Override public JsonNode getSchemaNode(JsonNode node, String... path) { return null; }
        @Override public JsonNode getSchemaNode(JsonNode node, JsonNode subtree) { return null; }
        @Override public JsonNode getSchemaNode(JsonNode node, String keyword, JsonNode subtree) { return null; }
        @Override public JsonNode getSchemaNode(JsonNode node, String... path, JsonNode subtree) { return null; }
        @Override public JsonNode getSchemaNode(JsonNode node, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getSchemaNode(JsonNode node, String keyword, JsonNode subtree, boolean cache) { return null; }
        @Override public JsonNode getSchemaNode(JsonNode node, String... path, JsonNode subtree, boolean cache) { return null; }
        @Override public boolean isCustomValidator() { return false; }
        @Override public boolean isCustomValidator(JsonNode node) { return false; }
        @Override public boolean isCustomValidator(String name) { return false; }
        @Override public boolean isCustomValidator(JsonNode node, String name) { return false; }
        @Override public String getCustomValidatorName() { return null; }
        @Override public String getCustomValidatorName(JsonNode node) { return null; }
        @Override public String getCustomValidatorName(String name) { return null; }
        @Override public String getCustomValidatorName(JsonNode node, String name) { return null; }
        @Override public List<String> getCustomValidatorNames() { return null; }
        @Override public List<String> getCustomValidatorNames(JsonNode node) { return null; }
        @Override public List<String> getCustomValidatorNames(String name) { return null; }
        @Override public List<String> getCustomValidatorNames(JsonNode node, String name) { return null; }
        @Override public boolean isConditional() { return false; }
        @Override public boolean isConditional(JsonNode node) { return false; }
        @Override public boolean isConditional(String name) { return false; }
        @Override public boolean isConditional(JsonNode node, String name) { return false; }
        @Override public boolean isIfThenElse() { return false; }
        @Override public boolean isIfThenElse(JsonNode node) { return false; }
        @Override public boolean isIfThenElse(String name) { return false; }
        @Override public boolean isIfThenElse(JsonNode node, String name) { return false; }
        @Override public boolean isIfThenElse(JsonNode node, String name, boolean isCase) { return false; }
        @Override public boolean isIfThenElse(JsonNode node, String name, boolean isCase, boolean isDuplicate) { return false; }
        @Override public boolean isUndefined() { return false; }
        @Override public boolean isUndefined(JsonNode node) { return false; }
        @Override public boolean isUndefined(JsonNode node, String name) { return false; }
        @Override public boolean isValid(JsonNode node, String at, boolean walk) { return false; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at, boolean walk) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at, boolean walk, boolean isRoot) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at, boolean walk, boolean isRoot, boolean isArrayItem) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at, boolean walk, boolean isRoot, boolean isArrayItem, boolean isAdditionalProperties) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at, boolean walk, boolean isRoot, boolean isArrayItem, boolean isRootItem) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at, boolean walk, boolean isRoot, boolean isRootItem) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at, boolean walk, boolean isRoot, boolean isRootItem, boolean isUnion) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at, boolean walk, boolean isRoot, boolean isRootItem, boolean isUnion, boolean isUnionType) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at, boolean walk, boolean isRoot, boolean isRootItem, boolean isUnion) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at, boolean walk, boolean isRoot, boolean isRootItem, boolean isUnion, boolean isUnionType, boolean isUnionValue) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at, boolean walk, boolean isParent, boolean isParentArray, boolean isParentObject) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, boolean walk) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, boolean walk, boolean isRoot) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, boolean walk, boolean isRoot, boolean isArrayItem) { return null; }
        @Override public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) { return null; }
        @Override public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema) { return null; }
        @Override public void preloadJsonSchema() {}
        @Override public void preloadJsonSchemas(List<JsonSchema> schemas) {}
    }

    /** a tiny stub for the {@link TypeValidator} used in the tests */
    private static class StubTypeValidator implements TypeValidator {
        private final JsonType schemaType;
        private final JsonNode sampleNode; // node that *does* match the type

        StubTypeValidator(JsonType schemaType, JsonNode sampleNode) {
            this.schemaType = schemaType;
            this.sampleNode = sampleNode;
        }

        @Override public JsonType getSchemaType() { return schemaType; }

        @Override public boolean equalsToSchemaType(JsonNode node) {
            return node != null && node.getNodeType() == sampleNode.getNodeType();
        }
    }

    /** Helper to create a {@link ValidationMessage} mock with a given type */
    private static ValidationMessage messageOfType(String type) {
        ValidationMessage vm = Mockito.mock(ValidationMessage.class);
        Mockito.when(vm.getType()).thenReturn(type);
        return vm;
    }

    private ValidationContext validationContext;
    private ValidationContext.ValidationConfig config;

    @Before
    public void setUp() {
        // ----------------------------------------------------------------------
        // Minimal validation‑context stub – only the bits required by AnyOfValidator
        // ----------------------------------------------------------------------
        config = Mockito.mock(ValidationContext.ValidationConfig.class);
        Mockito.when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        validationContext = Mockito.mock(ValidationContext.class);
        Mockito.when(validationContext.getConfig()).thenReturn(config);
        Mockito.when(validationContext.newSchema(Mockito.anyString(), Mockito.any(JsonNode.class), Mockito.any(JsonSchema.class)))
               .thenAnswer(invocation -> {
                   // the AnyOfValidator constructor will call newSchema for each subschema.
                   // For the tests we replace the returned schema later (see each test method).
                   return null; // placeholder – will be overwritten.
               });
    }

    /** ------------------------------------------------------------
     *  1️⃣  A simple “any‑of” where one subschema validates successfully.
     * ------------------------------------------------------------ */
    @Test
    public void shouldReturnEmptyWhenAtLeastOneSubschemaMatches() {
        // a node that will be accepted by the first stub schema
        JsonNode node = FACTORY.textNode("ok");

        // first subschema – returns no errors (valid)
        JsonSchema okSchema = new StubJsonSchema(Collections.emptySet(),
                                                 false, null, false);

        // second subschema – returns a required‑field error (invalid)
        ValidationMessage requiredMsg = messageOfType(ValidatorTypeCode.REQUIRED.getValue());
        JsonSchema badSchema = new StubJsonSchema(Collections.singleton(requiredMsg),
                                                  false, null, false);

        // replace the placeholder schemas with the concrete stubs
        Mockito.when(validationContext.newSchema(Mockito.anyString(),
                Mockito.any(JsonNode.class), Mockito.any(JsonSchema.class)))
                .thenReturn(okSchema)   // index 0
                .thenReturn(badSchema); // index 1

        AnyOfValidator validator = new AnyOfValidator("#/anyOf", FACTORY.arrayNode()
                .add(FACTORY.objectNode())
                .add(FACTORY.objectNode()), null, validationContext);

        ExecutionContext ec = new DummyExecutionContext();

        Set<ValidationMessage> result = validator.validate(ec, node, node, "$");

        // the validator should treat the whole anyOf as valid → empty error set
        assertTrue("Expected no validation errors", result.isEmpty());
    }

    /** ------------------------------------------------------------
     *  2️⃣  No subschema matches – the validator must return the union of errors.
     * ------------------------------------------------------------ */
    @Test
    public void shouldAggregateErrorsWhenNoSubschemaMatches() {
        JsonNode node = FACTORY.textNode("bad");

        ValidationMessage err1 = messageOfType("type");
        ValidationMessage err2 = messageOfType("minimum");

        JsonSchema schema1 = new StubJsonSchema(Collections.singleton(err1),
                                                false, null, false);
        JsonSchema schema2 = new StubJsonSchema(Collections.singleton(err2),
                                                false, null, false);

        Mockito.when(validationContext.newSchema(Mockito.anyString(),
                Mockito.any(JsonNode.class), Mockito.any(JsonSchema.class)))
                .thenReturn(schema1)
                .thenReturn(schema2);

        AnyOfValidator validator = new AnyOfValidator("#/anyOf", FACTORY.arrayNode()
                .add(FACTORY.objectNode())
                .add(FACTORY.objectNode()), null, validationContext);

        ExecutionContext ec = new DummyExecutionContext();

        Set<ValidationMessage> result = validator.validate(ec, node, node, "$");

        // both error messages must be present
        assertEquals(2, result.size());
        assertTrue(result.contains(err1));
        assertTrue(result.contains(err2));
    }

    /** ------------------------------------------------------------
     *  3️⃣  Subschema with a type validator that does NOT match the node.
     *      The validator should add a type‑error and skip the subschema validation.
     * ------------------------------------------------------------ */
    @Test
    public void shouldAddTypeErrorWhenTypeValidatorDoesNotMatch() {
        JsonNode node = FACTORY.numberNode(10); // an integer node

        // type validator expects a STRING, but we give it a NUMBER → mismatch
        TypeValidator typeValidator = new StubTypeValidator(JsonType.STRING,
                FACTORY.textNode("sample"));

        // the subschema itself would be valid, but we never reach it because of the type mismatch
        JsonSchema schema = new StubJsonSchema(Collections.emptySet(),
                true, typeValidator, false);

        Mockito.when(validationContext.newSchema(Mockito.anyString(),
                Mockito.any(JsonNode.class), Mockito.any(JsonSchema.class)))
                .thenReturn(schema);

        AnyOfValidator validator = new AnyOfValidator("#/anyOf", FACTORY.arrayNode()
                .add(FACTORY.objectNode()), null, validationContext);

        ExecutionContext ec = new DummyExecutionContext();

        Set<ValidationMessage> result = validator.validate(ec, node, node, "$");

        // we expect exactly one error – the type‑error generated by AnyOfValidator
        assertEquals(1, result.size());
        ValidationMessage vm = result.iterator().next();
        // the type error message contains the schema type name ("string")
        assertTrue(vm.getMessage().toLowerCase().contains("string"));
    }

    /** ------------------------------------------------------------
     *  4️⃣  OpenAPI‑3 style discriminator – a match is found and the matching
     *      subschema validates without errors.
     * ------------------------------------------------------------ */
    @Test
    public void shouldSucceedWhenDiscriminatorMatchFoundAndSubschemaValid() {
        // enable discriminator handling
        Mockito.when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        JsonNode node = FACTORY.objectNode().put("type", "A");

        // a dummy discriminator context that reports a match
        ValidationContext.DiscriminatorContext discCtx = new ValidationContext.DiscriminatorContext() {
            @Override public boolean isDiscriminatorMatchFound() { return true; }
            @Override public boolean isActive() { return true; }
        };

        // the validator will create its own discriminator context – we need to
        // inject our stub after construction (reflection is the easiest way)
        AnyOfValidator validator = new AnyOfValidator("#/anyOf", FACTORY.arrayNode()
                .add(FACTORY.objectNode()), null, validationContext);
        // replace the internal discriminatorContext with our stub
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            f.setAccessible(true);
            f.set(validator, discCtx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // a subschema that validates successfully (empty error set)
        JsonSchema goodSchema = new StubJsonSchema(Collections.emptySet(),
                false, null, false);
        Mockito.when(validationContext.newSchema(Mockito.anyString(),
                Mockito.any(JsonNode.class), Mockito.any(JsonSchema.class)))
                .thenReturn(goodSchema);

        ExecutionContext ec = new DummyExecutionContext();

        Set<ValidationMessage> result = validator.validate(ec, node, node, "$");

        // because the discriminator matched and the subschema is valid, we must get
        // an empty error set.
        assertTrue(result.isEmpty());
    }

    /** ------------------------------------------------------------
     *  5️⃣  OpenAPI‑3 discriminator active but no match could be chosen.
     *      The validator must return a single “discriminator” error.
     * ------------------------------------------------------------ */
    @Test
    public void shouldReturnDiscriminatorErrorWhenNoMatchFound() {
        // enable discriminator handling
        Mockito.when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        JsonNode node = FACTORY.objectNode().put("type", "UNKNOWN");

        // discriminator context reports that it is active but no match was found
        ValidationContext.DiscriminatorContext discCtx = new ValidationContext.DiscriminatorContext() {
            @Override public boolean isDiscriminatorMatchFound() { return false; }
            @Override public boolean isActive() { return true; }
        };

        AnyOfValidator validator = new AnyOfValidator("#/anyOf", FACTORY.arrayNode()
                .add(FACTORY.objectNode()), null, validationContext);
        // inject the stub discriminator context
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            f.setAccessible(true);
            f.set(validator, discCtx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // two failing subschemas – their errors are irrelevant because the discriminator
        // will dominate the final result.
        ValidationMessage err = messageOfType("type");
        JsonSchema badSchema = new StubJsonSchema(Collections.singleton(err),
                false, null, false);
        Mockito.when(validationContext.newSchema(Mockito.anyString(),
                Mockito.any(JsonNode.class), Mockito.any(JsonSchema.class)))
                .thenReturn(badSchema)
                .thenReturn(badSchema);

        ExecutionContext ec = new DummyExecutionContext();

        Set<ValidationMessage> result = validator.validate(ec, node, node, "$");

        // exactly one error, containing the discriminator‑related remark
        assertEquals(1, result.size());
        ValidationMessage vm = result.iterator().next();
        assertTrue(vm.getMessage().toLowerCase()
                .contains("discriminator"));
    }
}
