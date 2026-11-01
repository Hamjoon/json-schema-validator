```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_isSchemaResourceRoot_Test {

    // Common collaborators used to build a JsonSchema instance via reflection
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;

    // Parent schema used in several tests
    @Mock JsonSchema parentSchema;

    // Helper to create a JsonSchema instance (real object) via the private constructor
    private JsonSchema createSchema(JsonSchema parent) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        // the last boolean flag is the “suppressSubSchemaRetrieval” flag – we can safely pass false
        return (JsonSchema) ctor.newInstance(validationContext, schemaLocation, evaluationPath, schemaNode, parent, false);
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these unit tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mocks before each test (MockitoExtension does this automatically)
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void whenIdIsNotNull_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);

        // Stub getId() to return a non‑null value
        doReturn("someId").when(spy).getId();

        // The other conditions are irrelevant – they can stay default (null)
        assertTrue(spy.isSchemaResourceRoot(),
                "Schema with a non‑null id must be considered a resource root");
    }

    @Test
    void whenIdIsNullAndParentIsNull_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);

        // Stub getId() to return null
        doReturn(null).when(spy).getId();
        // getParentSchema() will naturally return null because we passed null to the constructor
        assertTrue(spy.isSchemaResourceRoot(),
                "Schema with no id and no parent must be considered a resource root");
    }

    @Test
    void whenIdIsNullParentExistsAndIriDiffers_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        // Prepare distinct IRI objects – different mocks are sufficient
        AbsoluteIri childIri = mock(AbsoluteIri.class);
        AbsoluteIri parentIri = mock(AbsoluteIri.class);
        when(schemaLocation.getAbsoluteIri()).thenReturn(childIri);
        when(parentSchema.getSchemaLocation()).thenReturn(schemaLocation);
        // Parent's schema location returns a different IRI
        SchemaLocation parentSchemaLocation = mock(SchemaLocation.class);
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(parentIri);
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);

        JsonSchema schema = createSchema(parentSchema);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId();               // id is null
        doReturn(parentSchema).when(spy).getParentSchema(); // parent present

        assertTrue(spy.isSchemaResourceRoot(),
                "Schema whose IRI differs from its parent must be a resource root");
    }

    @Test
    void whenIdIsNullParentExistsAndIriEquals_thenIsSchemaResourceRootReturnsFalse() throws Exception {
        // Use the same IRI mock for child and parent – equals will be true (reference equality)
        AbsoluteIri sharedIri = mock(AbsoluteIri.class);
        when(schemaLocation.getAbsoluteIri()).thenReturn(sharedIri);
        when(parentSchema.getSchemaLocation()).thenReturn(schemaLocation); // same location instance

        JsonSchema schema = createSchema(parentSchema);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId();               // id is null
        doReturn(parentSchema).when(spy).getParentSchema(); // parent present

        // The method should now fall through to the final `return false;`
        assertFalse(spy.isSchemaResourceRoot(),
                "Schema with equal IRI to its parent and no id must NOT be a resource root");
    }

    // Helper to stub getParentSchema() – the real implementation is in BaseJsonValidator,
    // but we can safely override it via a spy.
    @Test
    void whenIdIsNullParentExistsAndParentIsNull_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId();
        // Simulate a parent that is null (already the case) – just double‑check the logic
        assertTrue(spy.isSchemaResourceRoot(),
                "When id is null and parent schema is null, should be a resource root");
    }
}
```