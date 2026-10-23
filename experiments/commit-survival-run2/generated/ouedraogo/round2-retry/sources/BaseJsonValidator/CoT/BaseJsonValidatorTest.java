/****************************************************************************************
 * JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}
 *
 * The test class follows the steps requested:
 *   1. Lists all public methods of BaseJsonValidator.
 *   2. Provides a basic test for each public method.
 *   3. Identifies edge‑case / exception scenarios.
 *   4. Adds dedicated tests for those scenarios.
 *   5. Merges everything into a single, self‑contained test file.
 *
 * The tests use Jackson’s {@link com.fasterxml.jackson.databind.node.JsonNodeFactory}
 * for concrete {@link JsonNode} instances and Mockito for the heavy‑weight collaborators
 * ( {@link JsonSchema}, {@link ValidationContext}, {@link ValidationContext.Config},
 * {@link ValidationContext.URIFactory}, {@link JsonSchemaFactory} ).
 *
 * Because {@code BaseJsonValidator} is abstract we create a minimal concrete subclass
 * {@code TestValidator} that:
 *   • forwards the 4‑argument {@code validate} call (required by the default
 *     implementation of {@code BaseJsonValidator.validate(...)}) and
 *   • exposes the protected helper methods so that they can be exercised from the
 *     test code.
 *
 * The test suite is written for JUnit‑4 (annotations {@code @Test}, {@code @Before},
 * assertions from {@code org.junit.Assert}) and can be run with any standard JUnit‑4
 * runner.
 ****************************************************************************************/

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Concrete test‑only implementation of {@link BaseJsonValidator}.
 *
 * <p>The class implements the missing 4‑argument {@code validate} method that the
 * base class delegates to and makes a few protected helpers public so that they
 * can be verified from the test suite.</p>
 */
class TestValidator extends BaseJsonValidator {

    // ------------------------------------------------------------------------
    // Constructors – simply delegate to the super‑class
    // ------------------------------------------------------------------------
    TestValidator(String schemaPath,
                  JsonNode schemaNode,
                  JsonSchema parentSchema,
                  ValidatorTypeCode validatorType,
                  ValidationContext validationContext) {
        super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
    }

    TestValidator(String schemaPath,
                  JsonNode schemaNode,
                  JsonSchema parentSchema,
                  ValidatorTypeCode validatorType,
                  ValidationContext validationContext,
                  boolean suppressSubSchemaRetrieval) {
        super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
    }

    // ------------------------------------------------------------------------
    // The abstract 4‑argument validate method required by the base class.
    // It simply records the received parameters and returns a predefined set.
    // ------------------------------------------------------------------------
    private Set<ValidationMessage> stubResult = Collections.emptySet();
    private ExecutionContext capturedExecutionContext;
    private JsonNode capturedNode;
    private JsonNode capturedRootNode;
    private String capturedAt;

    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                           JsonNode node,
                                           JsonNode rootNode,
                                           String at) {
        this.capturedExecutionContext = executionContext;
        this.capturedNode = node;
        this.capturedRootNode = rootNode;
        this.capturedAt = at;
        return stubResult;
    }

    // ------------------------------------------------------------------------
    // Helper methods to expose protected functionality for testing
    // ------------------------------------------------------------------------
    /** expose {@code fetchSubSchemaNode} */
    JsonSchema fetchSubSchemaNodePublic(ValidationContext vc) {
        return super.fetchSubSchemaNode(vc);
    }

    /** expose {@code getNodeFieldType} */
    String getNodeFieldTypePublic() {
        return super.getNodeFieldType();
    }

    /** expose {@code atRoot} */
    String atRootPublic() {
        return super.atRoot();
    }

    /** expose {@code atPath(String,String)} */
    String atPathPublic(String currentPath, String token) {
        return super.atPath(currentPath, token);
    }

    /** expose {@code atPath(String,int)} */
    String atPathPublic(String currentPath, int index) {
        return super.atPath(currentPath, index);
    }

    // ------------------------------------------------------------------------
    // Mutators used by the test cases
    // ------------------------------------------------------------------------
    void setStubResult(Set<ValidationMessage> result) {
        this.stubResult = result;
    }

    ExecutionContext getCapturedExecutionContext() { return capturedExecutionContext; }
    JsonNode getCapturedNode()               { return capturedNode; }
    JsonNode getCapturedRootNode()           { return capturedRootNode; }
    String   getCapturedAt()                 { return capturedAt; }
}

/**
 * JUnit‑4 test class for {@link BaseJsonValidator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    // ------------------------------------------------------------------------
    // Test data & collaborators
    // ------------------------------------------------------------------------
    private static final String SCHEMA_PATH = "/mySchema";
    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    @Mock private JsonSchema mockParentSchema;
    @Mock private ValidationContext mockContext;
    @Mock private ValidationContext.Config mockConfig;
    @Mock private ValidationContext.URIFactory mockUriFactory;
    @Mock private JsonSchemaFactory mockJsonSchemaFactory;
    @Mock private ExecutionContext mockExecutionContext;

    private ObjectNode simpleSchemaNode;
    private TestValidator validator;

    // ------------------------------------------------------------------------
    // Common set‑up executed before each test method
    // ------------------------------------------------------------------------
    @Before
    public void setUp() {
        // a very simple schema node – can be enriched per test if needed
        simpleSchemaNode = FACTORY.objectNode();

        // default mock behaviour – most methods are not needed for the basic tests
        when(mockContext.getConfig()).thenReturn(mockConfig);
        when(mockContext.getURIFactory()).thenReturn(mockUriFactory);
        when(mockContext.getJsonSchemaFactory()).thenReturn(mockJsonSchemaFactory);
        when(mockConfig.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(mockConfig.getPathType()).thenReturn(PathType.DEFAULT);

        validator = new TestValidator(SCHEMA_PATH,
                                      simpleSchemaNode,
                                      mockParentSchema,
                                      ValidatorTypeCode.UNKNOWN, // using a placeholder enum value
                                      mockContext);
    }

    // ------------------------------------------------------------------------
    // 1️⃣  PUBLIC METHOD LIST (for documentation purposes only)
    // ------------------------------------------------------------------------
    //
    //   public BaseJsonValidator(String schemaPath,
    //                            JsonNode schemaNode,
    //                            JsonSchema parentSchema,
    //                            ValidatorTypeCode validatorType,
    //                            ValidationContext validationContext)
    //
    //   public BaseJsonValidator(String schemaPath,
    //                            JsonNode schemaNode,
    //                            JsonSchema parentSchema,
    //                            ValidatorTypeCode validatorType,
    //                            ValidationContext validationContext,
    //                            boolean suppressSubSchemaRetrieval)
    //
    //   public String getSchemaPath()
    //   public JsonNode getSchemaNode()
    //   public JsonSchema getParentSchema()
    //   public Set<ValidationMessage> validate(ExecutionContext executionContext,
    //                                          JsonNode node)
    //
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // BASIC FUNCTIONALITY TESTS
    // ------------------------------------------------------------------------

    @Test
    public void testGetSchemaPath() {
        assertEquals("Schema path should be returned unchanged", SCHEMA_PATH, validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode() {
        assertSame("Schema node reference should be identical", simpleSchemaNode, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        assertSame("Parent schema reference should be identical", mockParentSchema, validator.getParentSchema());
    }

    @Test
    public void testValidateDelegatesToFourArgMethod() {
        // arrange – set a known stub result
        ValidationMessage dummyMessage = ValidationMessage.of("dummy", "dummy message");
        Set<ValidationMessage> expected = Collections.singleton(dummyMessage);
        validator.setStubResult(expected);

        // act
        JsonNode node = FACTORY.objectNode();
        Set<ValidationMessage> actual = validator.validate(mockExecutionContext, node);

        // assert – the returned set is the stubbed one
        assertSame("validate(...) should forward to the 4‑arg version and return its result",
                   expected, actual);

        // also verify that the 4‑arg method received the expected arguments
        assertSame("ExecutionContext should be passed through", mockExecutionContext, validator.getCapturedExecutionContext());
        assertSame("Node argument should be passed as both node and rootNode", node, validator.getCapturedNode());
        assertSame("RootNode argument should be the same as node", node, validator.getCapturedRootNode());
        assertEquals("The 'at' argument must be the root path as defined by PathType.DEFAULT",
                     validator.atRootPublic(), validator.getCapturedAt());
    }

    // ------------------------------------------------------------------------
    // EDGE‑CASE / EXCEPTION TESTS
    // ------------------------------------------------------------------------

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        // create a validator with suppressSubSchemaRetrieval = true
        TestValidator suppressed = new TestValidator(SCHEMA_PATH,
                                                      simpleSchemaNode,
                                                      mockParentSchema,
                                                      ValidatorTypeCode.UNKNOWN,
                                                      mockContext,
                                                      true);

        JsonSchema result = suppressed.fetchSubSchemaNodePublic(mockContext);
        assertNull("When suppressSubSchemaRetrieval is true fetchSubSchemaNode should return null", result);
    }

    @Test
    public void testGetNodeFieldTypeWhenParentHasNoType() {
        // parent schema's own schema node does NOT contain a "type" field
        when(mockParentSchema.getSchemaNode()).thenReturn(FACTORY.objectNode());

        String type = validator.getNodeFieldTypePublic();
        assertNull("getNodeFieldType should return null when parent schema lacks a 'type' field", type);
    }

    @Test
    public void testGetNodeFieldTypeWhenParentHasType() {
        ObjectNode parentNode = FACTORY.objectNode().put("type", "object");
        when(mockParentSchema.getSchemaNode()).thenReturn(parentNode);

        String type = validator.getNodeFieldTypePublic();
        assertEquals("Should return the textual value of the parent's 'type' field", "object", type);
    }

    @Test
    public void testAtRootUsesConfiguredPathType() {
        // PathType.DEFAULT returns "$"
        when(mockConfig.getPathType()).thenReturn(PathType.DEFAULT);
        // need to recreate validator so it picks up the new config mock
        validator = new TestValidator(SCHEMA_PATH,
                                      simpleSchemaNode,
                                      mockParentSchema,
                                      ValidatorTypeCode.UNKNOWN,
                                      mockContext);
        assertEquals("Root path should be provided by the configured PathType",
                     PathType.DEFAULT.getRoot(),
                     validator.atRootPublic());
    }

    @Test
    public void testAtPathWithStringToken() {
        String base = "$.data";
        String token = "items";
        String expected = PathType.DEFAULT.append(base, token);
        assertEquals("atPath(String, String) should delegate to PathType.append",
                     expected,
                     validator.atPathPublic(base, token));
    }

    @Test
    public void testAtPathWithArrayIndex() {
        String base = "$.array";
        int index = 3;
        String expected = PathType.DEFAULT.append(base, index);
        assertEquals("atPath(String, int) should delegate to PathType.append",
                     expected,
                     validator.atPathPublic(base, index));
    }

    // ------------------------------------------------------------------------
    // Additional sanity test for the overloaded constructor (5‑arg version)
    // ------------------------------------------------------------------------
    @Test
    public void testFiveArgumentConstructorInitialisesFields() {
        TestValidator v = new TestValidator(SCHEMA_PATH,
                                            simpleSchemaNode,
                                            mockParentSchema,
                                            ValidatorTypeCode.UNKNOWN,
                                            mockContext,
                                            false);
        // No exception means fields were set; verify a couple of getters
        assertEquals(SCHEMA_PATH, v.getSchemaPath());
        assertSame(simpleSchemaNode, v.getSchemaNode());
    }
}
