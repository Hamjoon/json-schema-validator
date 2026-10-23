package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

/**
 * Unit tests for {@link BaseJsonValidator}.
 *
 * Because {@code BaseJsonValidator} is abstract we create a tiny concrete subclass
 * that implements the protected three‑argument {@code validate} method.  This allows
 * us to verify the behaviour of the public methods without pulling in the whole
 * validation engine.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    // -----------------------------------------------------------------------
    // Helper concrete implementation
    // -----------------------------------------------------------------------
    private static class TestValidator extends BaseJsonValidator {
        private final Set<ValidationMessage> result;

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval,
                      Set<ValidationMessage> result) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
            this.result = result;
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   String at) {
            // simply return the pre‑configured result set
            return result;
        }
    }

    // -----------------------------------------------------------------------
    // Mocks shared by many tests
    // -----------------------------------------------------------------------
    @Mock private JsonSchema parentSchemaMock;
    @Mock private JsonNode parentSchemaNodeMock;
    @Mock private ValidationContext validationContextMock;
    @Mock private ValidationContext.DiscriminatorContext discriminatorContextMock;
    @Mock private ExecutionContext executionContextMock;
    @Mock private Logger loggerMock;
    @Mock private JsonNode schemaNodeMock;
    @Mock private JsonNode idNodeMock;
    @Mock private JsonNode schemaNodeDollarMock;
    @Mock private JsonNode mappingNodeMock;
    @Mock private JsonSchemaFactory jsonSchemaFactoryMock;
    @Mock private URIFactory uriFactoryMock;
    @Mock private JsonSchema subSchemaMock;
    @Mock private Config configMock;
    @Mock private PathType pathTypeMock;

    private static final String SCHEMA_PATH = "/mySchema";
    private static final String PARENT_SCHEMA_PATH = "/parent";

    @Before
    public void setUp() {
        // Default behaviour for the parent schema – it returns a node that may contain a "type" field
        when(parentSchemaMock.getSchemaNode()).thenReturn(parentSchemaNodeMock);
        when(parentSchemaMock.getSchemaPath()).thenReturn(PARENT_SCHEMA_PATH);

        // ValidationContext returns a config and factories
        when(validationContextMock.getConfig()).thenReturn(configMock);
        when(validationContextMock.getURIFactory()).thenReturn(uriFactoryMock);
        when(validationContextMock.getJsonSchemaFactory()).thenReturn(jsonSchemaFactoryMock);
        when(validationContextMock.getDiscriminatorContext()).thenReturn(discriminatorContextMock);

        // Default PathType – we use the real implementation for simplicity
        when(configMock.getPathType()).thenReturn(PathType.DEFAULT);
    }

    // -----------------------------------------------------------------------
    // 1. Getter tests
    // -----------------------------------------------------------------------
    @Test
    public void getSchemaPath_returnsConstructorValue() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        assertEquals(SCHEMA_PATH, validator.getSchemaPath());
    }

    @Test
    public void getSchemaNode_returnsSuppliedNode() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        assertSame(schemaNodeMock, validator.getSchemaNode());
    }

    @Test
    public void getParentSchema_returnsSuppliedParent() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        assertSame(parentSchemaMock, validator.getParentSchema());
    }

    // -----------------------------------------------------------------------
    // 2. fetchSubSchemaNode – normal and edge cases
    // -----------------------------------------------------------------------
    @Test
    public void fetchSubSchemaNode_whenSuppressed_returnsNull() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, true, Collections.emptySet());

        assertNull(validator.fetchSubSchemaNode(validationContextMock));
    }

    @Test
    public void fetchSubSchemaNode_withoutId_returnsNull() {
        when(schemaNodeMock.get("id")).thenReturn(null);

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        assertNull(validator.fetchSubSchemaNode(validationContextMock));
        verifyNoInteractions(uriFactoryMock, jsonSchemaFactoryMock);
    }

    @Test
    public void fetchSubSchemaNode_idEqualsDollarSchema_returnsNull() {
        when(schemaNodeMock.get("id")).thenReturn(idNodeMock);
        when(schemaNodeMock.get("$schema")).thenReturn(schemaNodeDollarMock);
        when(idNodeMock.equals(schemaNodeDollarMock)).thenReturn(true);
        when(idNodeMock.textValue()).thenReturn("http://example.com/schema.json");

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        assertNull(validator.fetchSubSchemaNode(validationContextMock));
    }

    @Test
    public void fetchSubSchemaNode_idIsNullText_returnsNull() {
        when(schemaNodeMock.get("id")).thenReturn(idNodeMock);
        when(idNodeMock.textValue()).thenReturn(null);   // id present but empty
        when(schemaNodeMock.get("$schema")).thenReturn(null);

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        assertNull(validator.fetchSubSchemaNode(validationContextMock));
    }

    @Test
    public void fetchSubSchemaNode_invalidUri_returnsNull() {
        when(schemaNodeMock.get("id")).thenReturn(idNodeMock);
        when(idNodeMock.textValue()).thenReturn("not a valid uri");
        when(schemaNodeMock.get("$schema")).thenReturn(null);
        // URIFactory throws IllegalArgumentException
        when(uriFactoryMock.create("not a valid uri")).thenThrow(new IllegalArgumentException());

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        assertNull(validator.fetchSubSchemaNode(validationContextMock));
        verify(uriFactoryMock).create("not a valid uri");
        verifyNoMoreInteractions(jsonSchemaFactoryMock);
    }

    @Test
    public void fetchSubSchemaNode_validId_returnsSubSchema() {
        when(schemaNodeMock.get("id")).thenReturn(idNodeMock);
        when(idNodeMock.textValue()).thenReturn("http://example.com/sub.json");
        when(schemaNodeMock.get("$schema")).thenReturn(null);
        URI uri = URI.create("http://example.com/sub.json");
        when(uriFactoryMock.create("http://example.com/sub.json")).thenReturn(uri);
        when(jsonSchemaFactoryMock.getSchema(uri, configMock)).thenReturn(subSchemaMock);

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        JsonSchema result = validator.fetchSubSchemaNode(validationContextMock);
        assertSame(subSchemaMock, result);
    }

    // -----------------------------------------------------------------------
    // 3. validate – delegation test
    // -----------------------------------------------------------------------
    @Test
    public void validate_delegatesToProtectedValidate() {
        Set<ValidationMessage> expected = new HashSet<>();
        expected.add(new ValidationMessage.Builder().message("dummy").build());

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, expected);

        JsonNode nodeMock = mock(JsonNode.class);
        Set<ValidationMessage> actual = validator.validate(executionContextMock, nodeMock);

        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------------
    // 4. getNodeFieldType – present and absent
    // -----------------------------------------------------------------------
    @Test
    public void getNodeFieldType_whenTypeExists_returnsValue() {
        JsonNode typeNode = mock(JsonNode.class);
        when(typeNode.asText()).thenReturn("string");
        when(parentSchemaNodeMock.get("type")).thenReturn(typeNode);

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        assertEquals("string", validator.getNodeFieldType());
    }

    @Test
    public void getNodeFieldType_whenMissing_returnsNull() {
        when(parentSchemaNodeMock.get("type")).thenReturn(null);

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        assertNull(validator.getNodeFieldType());
    }

    // -----------------------------------------------------------------------
    // 5. preloadJsonSchemas – verify initialise call
    // -----------------------------------------------------------------------
    @Test
    public void preloadJsonSchemas_callsInitializeOnAllSchemas() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(s1, s2);

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        validator.preloadJsonSchemas(schemas);

        verify(s1).initializeValidators();
        verify(s2).initializeValidators();
    }

    // -----------------------------------------------------------------------
    // 6. Path handling – atRoot, atPath(String,String), atPath(String,int)
    // -----------------------------------------------------------------------
    @Test
    public void getPathType_returnsConfiguredPathType() {
        when(configMock.getPathType()).thenReturn(PathType.LEGACY); // pick a non‑default for verification

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        assertEquals(PathType.LEGACY, validator.getPathType());
    }

    @Test
    public void atRoot_usesPathTypeRoot() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        assertEquals(PathType.DEFAULT.getRoot(), validator.atRoot());
    }

    @Test
    public void atPath_stringToken_concatenatesCorrectly() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        String result = validator.atPath("/a/b", "c");
        // PathType.DEFAULT appends with '/' separator
        assertEquals("/a/b/c", result);
    }

    @Test(expected = NullPointerException.class)
    public void atPath_stringToken_nullToken_throwsException() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        // PathType.DEFAULT will NPE when token is null – this is the defined behaviour
        validator.atPath("/a/b", (String) null);
    }

    @Test
    public void atPath_intIndex_concatenatesCorrectly() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNodeMock,
                parentSchemaMock, null, validationContextMock, false, Collections.emptySet());

        String result = validator.atPath("/arr", 2);
        assertEquals("/arr/2", result);
    }

    // -----------------------------------------------------------------------
    // 7. equals(double,double) and debug(Logger,JsonNode,JsonNode,String) are private/static helpers,
    //    not part of the public API – they are exercised indirectly by other tests.
    // -----------------------------------------------------------------------
}
