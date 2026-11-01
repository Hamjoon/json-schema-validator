package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaLocation_Test {

    /**
     * Simple concrete implementation of the abstract {@link BaseJsonValidator}
     *  used only for exercising {@code getSchemaLocation()}.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for these tests
            return Collections.emptySet();
        }
    }

    /**
     * A mutable subclass of {@link SchemaLocation} that allows its URI to be changed
     *  after construction.  This is used to verify that {@code getSchemaLocation()} returns
     *  the *same* object reference, not a defensive copy.
     */
    private static class MutableSchemaLocation extends SchemaLocation {

        public MutableSchemaLocation(String uri, String pointer) {
            super(uri, pointer);
        }

        /**
         * Mutates the internal URI.  The real {@link SchemaLocation} class does not expose a setter,
         *  but for the purpose of the test we extend it and add one.
         */
        public void setUri(String newUri) {
            // Use reflection because the original field is private in the library.
            try {
                java.lang.reflect.Field f = SchemaLocation.class.getDeclaredField("uri");
                f.setAccessible(true);
                f.set(this, newUri);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Custom subclass that adds an extra attribute.  Used to ensure that the exact
     *  subclass instance is returned by the getter.
     */
    private static class MySchemaLocation extends SchemaLocation {

        private final String extra;

        public MySchemaLocation(String uri, String pointer, String extra) {
            super(uri, pointer);
            this.extra = extra;
        }

        public String getExtra() {
            return extra;
        }
    }

    @Test
    void getSchemaLocation_returnsConstructorProvidedInstance() {
        // Arrange – create mocks for all constructor dependencies
        SchemaLocation mockLocation = mock(SchemaLocation.class);
        JsonNodePath mockEvalPath = mock(JsonNodePath.class);
        JsonNode mockSchemaNode = mock(JsonNode.class);
        JsonSchema mockParentSchema = mock(JsonSchema.class);
        ValidatorTypeCode mockValidatorType = mock(ValidatorTypeCode.class);
        ValidationContext mockContext = mock(ValidationContext.class);
        // Act – instantiate the concrete test validator
        BaseJsonValidator validator = new TestValidator(mockLocation, mockEvalPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockContext);
        // Assert – the getter must return the same instance supplied to the constructor
        assertSame(mockLocation, validator.getSchemaLocation(), "BaseJsonValidator#getSchemaLocation should return the SchemaLocation passed to the constructor");
    }

    @Test
    void getSchemaLocation_returnsNullWhenConstructorSuppliedNull() {
        // Arrange – all constructor arguments are mocked except the location which is null
        JsonNodePath mockEvalPath = mock(JsonNodePath.class);
        JsonNode mockSchemaNode = mock(JsonNode.class);
        JsonSchema mockParentSchema = mock(JsonSchema.class);
        ValidatorTypeCode mockValidatorType = mock(ValidatorTypeCode.class);
        ValidationContext mockContext = mock(ValidationContext.class);
        // Act
        BaseJsonValidator validator = new TestValidator(// <-- explicitly null
        null, mockEvalPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockContext);
        // Assert
        assertNull(validator.getSchemaLocation(), "BaseJsonValidator#getSchemaLocation should return null when the validator was constructed with a null SchemaLocation");
    }

    @Test
    void getSchemaLocation_multipleCallsReturnSameInstance() {
        // Arrange
        SchemaLocation location = mock(SchemaLocation.class);
        BaseJsonValidator validator = new TestValidator(location, mock(JsonNodePath.class), mock(JsonNode.class), mock(JsonSchema.class), mock(ValidatorTypeCode.class), mock(ValidationContext.class));
        // Act
        SchemaLocation firstCall = validator.getSchemaLocation();
        SchemaLocation secondCall = validator.getSchemaLocation();
        // Assert
        assertSame(firstCall, secondCall, "Subsequent invocations of getSchemaLocation() must return the identical instance");
    }

    @Test
    void getSchemaLocation_mutableReferenceReflectsMutations() {
        // Arrange – a mutable implementation of SchemaLocation
        MutableSchemaLocation mutableLocation = new MutableSchemaLocation("http://example.com/old.json", "#/old");
        BaseJsonValidator validator = new TestValidator(mutableLocation, mock(JsonNodePath.class), mock(JsonNode.class), mock(JsonSchema.class), mock(ValidatorTypeCode.class), mock(ValidationContext.class));
        // Act – mutate the original object
        mutableLocation.setUri("http://example.com/new.json");
        // Assert – the getter returns the same instance and the mutation is visible
        SchemaLocation returned = validator.getSchemaLocation();
        assertSame(mutableLocation, returned, "Returned instance must be the same mutable object");
        assertEquals("http://example.com/new.json", returned.getUri(), "Mutations performed on the original SchemaLocation must be reflected in the object returned by getSchemaLocation()");
    }

    @Test
    void getSchemaLocation_customSubclassInstancePreserved() {
        // Arrange – a custom subclass with an extra attribute
        MySchemaLocation customLocation = new MySchemaLocation("http://example.com/custom.json", "#/custom", "extra-data");
        BaseJsonValidator validator = new TestValidator(customLocation, mock(JsonNodePath.class), mock(JsonNode.class), mock(JsonSchema.class), mock(ValidatorTypeCode.class), mock(ValidationContext.class));
        // Act
        SchemaLocation returned = validator.getSchemaLocation();
        // Assert – identity and extra attribute accessibility
        assertSame(customLocation, returned, "The exact custom SchemaLocation instance must be returned");
        assertTrue(returned instanceof MySchemaLocation, "Returned instance should be of type MySchemaLocation");
        assertEquals("extra-data", ((MySchemaLocation) returned).getExtra(), "Custom attribute must be accessible on the returned instance");
    }

    @Test
    void getSchemaLocation_threadSafety() throws Exception {
        // Arrange
        SchemaLocation location = mock(SchemaLocation.class);
        BaseJsonValidator validator = new TestValidator(location, mock(JsonNodePath.class), mock(JsonNode.class), mock(JsonSchema.class), mock(ValidatorTypeCode.class), mock(ValidationContext.class));
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<SchemaLocation> results = new ConcurrentLinkedQueue<>();
        // Act – launch threads that wait on the latch, then call the getter
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // ensure all threads start at the same time
                    startLatch.await();
                    results.add(validator.getSchemaLocation());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // release all threads
        startLatch.countDown();
        // wait for completion
        doneLatch.await();
        executor.shutdownNow();
        // Assert – every collected reference is the same as the original
        for (SchemaLocation result : results) {
            assertSame(location, result, "All concurrent calls to getSchemaLocation() must return the identical instance");
        }
    }

    @Test
    void getSchemaLocation_unchangedAfterValidation() {
        // Arrange – a validator with a known location
        SchemaLocation location = mock(SchemaLocation.class);
        BaseJsonValidator validator = new TestValidator(location, mock(JsonNodePath.class), mock(JsonNode.class), mock(JsonSchema.class), mock(ValidatorTypeCode.class), mock(ValidationContext.class));
        // Act – invoke validate (the concrete TestValidator does nothing)
        ExecutionContext execCtx = mock(ExecutionContext.class);
        JsonNode dummyNode = mock(JsonNode.class);
        validator.validate(execCtx, dummyNode);
        // Assert – location is still the same instance
        assertSame(location, validator.getSchemaLocation(), "SchemaLocation must remain unchanged after a validation run");
    }
}
