
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    // ----- Mocks -------------------------------------------------------------
    @Mock
    ValidationContext // used by JsonSchema
    validationContext;

    @Mock
    JsonMetaSchema // returned by validationContext.getMetaSchema()
    metaSchema;

    // ----- Shared test data -------------------------------------------------
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // the JSON schema used for all tests
    private JsonNode rootSchemaNode;

    // instance under test
    private JsonSchema jsonSchema;

    /**
     * Very small stub of {@link ValidationContext}. Only the methods used by
     * {@link JsonSchema} in the tests are implemented.
     */
    private static class SimpleValidationContext implements ValidationContext {

        private final JsonMetaSchema metaSchema;

        SimpleValidationContext(JsonMetaSchema metaSchema) {
            this.metaSchema = metaSchema;
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            return metaSchema;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return null;
        }

        // All other methods are not required for the current test suite.
        @Override
        public String getCurrentUri() {
            return null;
        }

        @Override
        public URI getCurrentUriObject() {
            return null;
        }

        @Override
        public void setCurrentUri(String uri) {
        }

        @Override
        public void setCurrentUri(URI uri) {
        }

        @Override
        public void setCurrentUri(String uri, boolean resolveRelative) {
        }

        @Override
        public void setCurrentUri(URI uri, boolean resolveRelative) {
        }

        @Override
        public String getCurrentBaseUri() {
            return null;
        }

        @Override
        public URI getCurrentBaseUriObject() {
            return null;
        }

        @Override
        public void setCurrentBaseUri(String uri) {
        }

        @Override
        public void setCurrentBaseUri(URI uri) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3) {
        }

        @Override
        public void setCurrentBaseUri(String uri) {
        }

        @Override
        public void setCurrentBaseUri(URI uri) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3, boolean resolveQueryOnly3) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3, boolean resolveQueryOnly3) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3, boolean resolveQueryOnly3) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3, boolean resolveQueryOnly3) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(String uri) {
        }

        @Override
        public void setCurrentBaseUri(URI uri) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(String uri) {
        }

        @Override
        public void setCurrentBaseUri(URI uri) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
        }

        @Override
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }

        @Override
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
        }
    }

    /**
     * Very small stub of {@link JsonMetaSchema} that allows us to
     * control the result of {@code getNodeByFragmentRef}.
     */
    private static class SimpleJsonMetaSchema extends JsonMetaSchema {

        private final Map<String, JsonNode> overrides = new HashMap<>();

        SimpleJsonMetaSchema() {
            // The constructor of JsonMetaSchema expects an id and a URI.
            // The concrete values are irrelevant for these tests.
            super("simple", URI.create("http://example.com/simple"), false, false);
        }

        /**
         * Register a node that should be returned for a given fragment.
         */
        void addOverride(String fragment, JsonNode node) {
            overrides.put(fragment, node);
        }

        @Override
        public JsonNode getNodeByFragmentRef(String fragment, JsonNode rootNode) {
            // First check if a test has explicitly registered an override.
            if (overrides.containsKey(fragment)) {
                return overrides.get(fragment);
            }
            // Default behaviour – try to resolve via standard JSON Pointer.
            JsonNode candidate = rootNode.at(fragment);
            return candidate.isMissingNode() ? null : candidate;
        }

        // The remaining methods of JsonMetaSchema are not needed for these tests.
        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getUri() {
            return null;
        }

        @Override
        public boolean isFormat(String format) {
            return false;
        }

        @Override
        public boolean isKeyword(String keyword) {
            return false;
        }

        @Override
        public boolean isKeywordSupported(String keyword) {
            return false;
        }

        @Override
        public boolean isKeywordSupported(String keyword, boolean strict) {
            return false;
        }

        @Override
        public boolean isFormatSupported(String format) {
            return false;
        }

        @Override
        public boolean isFormatSupported(String format, boolean strict) {
            return false;
        }

        @Override
        public boolean isCustomFormat(String format) {
            return false;
        }

        @Override
        public boolean isCustomKeyword(String keyword) {
            return false;
        }

        @Override
        public boolean isCustomKeyword(String keyword, boolean strict) {
            return false;
        }

        @Override
        public String getFormat(String format) {
            return null;
        }

        @Override
        public String getKeyword(String keyword) {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        String schemaJson = """
            {
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              },
              "definitions": {
                "address": { "type": "object" }
              },
              "encoded path": { "type": "string" },
              "components": {
                "schemas": {
                  "User": { "type": "object" }
                }
              }
            }
            """;
        rootSchemaNode = MAPPER.readTree(schemaJson);
        // Create a concrete stub for JsonMetaSchema
        metaSchema = new SimpleJsonMetaSchema();
        // Configure the ValidationContext mock to return our stub meta‑schema
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // Build the JsonSchema instance (package‑private factory method)
        jsonSchema = JsonSchema.from(validationContext, "test-schema.json", new URI("http://example.com/test-schema.json"), rootSchemaNode, null, false);
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithHashSlashPointer_ReturnsTargetNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("#/properties/name");
        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isObject(), "Result should be an object node");
        assertEquals("string", result.get("type").asText(), "The 'type' field should be 'string'");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithSlashPointer_ReturnsTargetNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("/properties/age");
        assertNotNull(result);
        assertEquals("integer", result.get("type").asText());
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithFragmentReference_DelegatesToMetaSchema() {
        JsonNode addressNode = rootSchemaNode.at("/definitions/address");
        assertFalse(addressNode.isMissingNode(), "Address node must exist in the test schema");
        // Register the expected fragment result in the meta‑schema stub.
        ((SimpleJsonMetaSchema) metaSchema).addOverride("#/definitions/address", addressNode);
        JsonNode result = jsonSchema.getRefSchemaNode("#/definitions/address");
        assertNotNull(result);
        assertEquals(addressNode, result);
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithUnresolvableFragment_ReturnsMissingNodeOrFallback() {
        // No override is added – the stub will return null for this fragment.
        JsonNode result = jsonSchema.getRefSchemaNode("#/unknown");
        assertNotNull(result, "Result should never be null even for unknown fragments");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithSlashPointer_MissingNode_TriggersFallback() {
        // "/nonexistent" does not exist – node.at will return MissingNode.
        JsonNode result = jsonSchema.getRefSchemaNode("/nonexistent");
        assertNotNull(result, "Even for missing targets the method must return a non‑null node");
        assertFalse(result.isMissingNode(), "Fallback should produce a non‑missing node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithFragmentReference_ResolvedByMetaSchema() {
        ObjectNode dummy = MAPPER.createObjectNode().put("dummy", true);
        ((SimpleJsonMetaSchema) metaSchema).addOverride("#foo", dummy);
        JsonNode result = jsonSchema.getRefSchemaNode("#foo");
        assertNotNull(result);
        assertSame(dummy, result, "The node returned by the meta‑schema should be forwarded");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithUrnFragment_Unresolvable_UsesFallback() {
        // No override for this URN – stub will return null.
        JsonNode result = jsonSchema.getRefSchemaNode("urn:example:schema");
        assertNotNull(result, "Method must return a non‑null node even when meta‑schema cannot resolve");
        assertFalse(result.isMissingNode(), "Fallback should provide a non‑missing node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithPlainReference_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("simple");
        assertSame(rootSchemaNode, result, "Plain references should return the original schema node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithUrlEncodedPointer_ReturnsDecodedNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("#/encoded%20path");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText(), "URL‑decoded pointer should resolve to the correct node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_ReferenceHashOnly_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("#");
        assertSame(rootSchemaNode, result, "Reference \"#\" should return the original schema node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_EmptyString_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("");
        assertSame(rootSchemaNode, result, "Empty reference should return the original schema node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_UrnFourChars_ReturnsOriginalNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("urn:");
        assertSame(rootSchemaNode, result, "\"urn:\" is too short for the fragment branch and should return the original node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_HashSlashMissingNode_Fallback() {
        JsonNode result = jsonSchema.getRefSchemaNode("#/nonexistent");
        assertNotNull(result);
        assertFalse(result.isMissingNode(), "Fallback should replace the missing node");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithEncodedSlashInsidePath() {
        JsonNode result = jsonSchema.getRefSchemaNode("#/components%2Fschemas%2FUser");
        assertNotNull(result);
        assertEquals("object", result.get("type").asText(), "Decoded pointer should resolve to the User schema object");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_NullReference_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> jsonSchema.getRefSchemaNode(null));
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_FindAncestorNull_ThrowsNPE() throws Exception {
        // Spy on the instance and force findAncestor() to return null.
        JsonSchema spy = spy(jsonSchema);
        doReturn(null).when(spy).findAncestor();
        assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("#/properties/name"), "When findAncestor() returns null a NPE should be thrown");
    }

    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_AncestorSchemaNodeNull_ThrowsNPE() {
        // Mock an ancestor schema whose getSchemaNode() returns null.
        JsonSchema ancestorMock = mock(JsonSchema.class);
        when(ancestorMock.getSchemaNode()).thenReturn(null);
        JsonSchema spy = spy(jsonSchema);
        doReturn(ancestorMock).when(spy).findAncestor();
        // Both a "#/…" and a plain "/…" reference should hit the node.at(...)
        assertAll(() -> assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("#/properties/name"), "Null schema node should cause NPE for \"#/…\" reference"), () -> assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("/properties/name"), "Null schema node should cause NPE for \"/…\" reference"));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:12: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:13: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:14: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:41: error: interface expected here
    private static class SimpleValidationContext implements ValidationContext {
                                                            ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:233: error: method setCurrentBaseUri(String,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:237: error: method setCurrentBaseUri(URI,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:241: error: method setCurrentBaseUri(String,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:245: error: method setCurrentBaseUri(URI,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:249: error: method setCurrentBaseUri(String,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:253: error: method setCurrentBaseUri(URI,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:257: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:261: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:265: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:269: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:273: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:277: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:281: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:285: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:289: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:293: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:297: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:301: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:305: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:309: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:313: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:317: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:321: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:325: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:329: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:333: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:337: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:341: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:345: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:349: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:353: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:357: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveRelative, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:369: error: method setCurrentBaseUri(String) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:373: error: method setCurrentBaseUri(URI) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:377: error: method setCurrentBaseUri(String,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:381: error: method setCurrentBaseUri(URI,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:385: error: method setCurrentBaseUri(String,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:389: error: method setCurrentBaseUri(URI,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:393: error: method setCurrentBaseUri(String,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:397: error: method setCurrentBaseUri(URI,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:401: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:405: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:409: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:413: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:417: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:421: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:425: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:429: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:433: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:437: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:441: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:445: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:449: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:453: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:457: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:461: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:465: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:469: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:473: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:477: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:481: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:485: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:489: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:493: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:497: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:501: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:505: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3, boolean resolveQueryOnly3) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:509: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2, boolean resolvePathOnly2, boolean resolveFragmentOnly3, boolean resolveQueryOnly3) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:513: error: method setCurrentBaseUri(String,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:517: error: method setCurrentBaseUri(URI,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:521: error: method setCurrentBaseUri(String,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:525: error: method setCurrentBaseUri(URI,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:529: error: method setCurrentBaseUri(String,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:533: error: method setCurrentBaseUri(URI,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:537: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:541: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:545: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:549: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:553: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:557: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:561: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:565: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:569: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:573: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:577: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:581: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:585: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:589: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:593: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:597: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:601: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:605: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:609: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:613: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:617: error: method setCurrentBaseUri(String,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(String uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchemaNode_Test.java:621: error: method setCurrentBaseUri(URI,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class SimpleValidationContext
        public void setCurrentBaseUri(URI uri, boolean resolveFragment, boolean resolveQuery, boolean resolvePath, boolean resolveScheme, boolean resolveAuthority, boolean resolveUserInfo, boolean resolveHost, boolean resolvePort, boolean resolvePathSegments, boolean resolveFragmentOnly, boolean resolveQueryOnly, boolean resolvePathOnly, boolean resolveFragmentOnly2, boolean resolveQueryOnly2) {
                    ^
100 errors
only showing the first 100 errors, of 465 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'api document of method getRefSchemaNode(String)': '\n     * Find the schema node for $ref attribute.\n     *\n     * @param ref String\n     * @return JsonNode\n     ', 'return type': 'com.fasterxml.jackson.databind.JsonNode', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonMetaSchema com.networknt.schema.JsonSchema.metaSchema;\n\trelated functions:\n\t\tmethod `private static boolean isUriFragmentWithNoContext(URI, String)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.schema.uri.URITranslator.PrefixReplacer`:\n\trelated functions:\n\t\tmethod `public URI translate(URI)` returns `java.net.URI`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.org.apache.commons.validator.routines.DomainValidator`:\n\trelated functions:\n\t\tmethod `private String chompLeadingDot(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public boolean isValidInet4Address(String)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: \n     * Validates an IPv4 address. Returns true if valid.\n     *\n     * @param inet4Address the IPv4 address to validate\n     * @return true if the argument contains a valid IPv4 address\n     ;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod ` static JsonSchemaRef getRefSchema(JsonSchema, ValidationContext, String)` returns `com.networknt.schema.JsonSchemaRef`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;\nclass `com.networknt.schema.PathType`:\n\trelated functions:\n\t\tmethod ` static String fromJsonPath(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


