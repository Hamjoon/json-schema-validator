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
    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    // Parent schema used in several tests
    @Mock
    JsonSchema parentSchema;

    // Helper to create a JsonSchema instance (real object) via the private constructor
    private JsonSchema createSchema(JsonSchema parent) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // the last boolean flag is the “suppressSubSchemaRetrieval” flag – we can safely pass false
        return (JsonSchema) ctor.newInstance(validationContext, schemaLocation, evaluationPath, schemaNode, parent, false);
    }

    @Test
    void whenIdIsNotNull_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        // Stub getId() to return a non‑null value
        doReturn("someId").when(spy).getId();
        // The other conditions are irrelevant – they can stay default (null)
        assertTrue(spy.isSchemaResourceRoot(), "Schema with a non‑null id must be considered a resource root");
    }

    @Test
    void whenIdIsNullAndParentIsNull_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        // Stub getId() to return null
        doReturn(null).when(spy).getId();
        // getParentSchema() will naturally return null because we passed null to the constructor
        assertTrue(spy.isSchemaResourceRoot(), "Schema with no id and no parent must be considered a resource root");
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
        // id is null
        doReturn(null).when(spy).getId();
        // parent present
        doReturn(parentSchema).when(spy).getParentSchema();
        assertTrue(spy.isSchemaResourceRoot(), "Schema whose IRI differs from its parent must be a resource root");
    }

    @Test
    void whenIdIsNullParentExistsAndIriEquals_thenIsSchemaResourceRootReturnsFalse() throws Exception {
        // Use the same IRI mock for child and parent – equals will be true (reference equality)
        AbsoluteIri sharedIri = mock(AbsoluteIri.class);
        when(schemaLocation.getAbsoluteIri()).thenReturn(sharedIri);
        // same location instance
        when(parentSchema.getSchemaLocation()).thenReturn(schemaLocation);
        JsonSchema schema = createSchema(parentSchema);
        JsonSchema spy = Mockito.spy(schema);
        // id is null
        doReturn(null).when(spy).getId();
        // parent present
        doReturn(parentSchema).when(spy).getParentSchema();
        // The method should now fall through to the final `return false;`
        assertFalse(spy.isSchemaResourceRoot(), "Schema with equal IRI to its parent and no id must NOT be a resource root");
    }

    // but we can safely override it via a spy.
    @Test
    void whenIdIsNullParentExistsAndParentIsNull_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId();
        // Simulate a parent that is null (already the case) – just double‑check the logic
        assertTrue(spy.isSchemaResourceRoot(), "When id is null and parent schema is null, should be a resource root");
    }

    // -----------------------------------------------------------------------
    @Test
    void whenIdIsEmptyString_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        // empty but non‑null
        doReturn("").when(spy).getId();
        assertTrue(spy.isSchemaResourceRoot(), "An empty string ID is still non‑null and should make the schema a resource root");
    }

    @Test
    void whenIdIsWhitespace_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        // whitespace but non‑null
        doReturn("   ").when(spy).getId();
        assertTrue(spy.isSchemaResourceRoot(), "A whitespace ID is non‑null and must cause the method to return true");
    }

    // -----------------------------------------------------------------------
    @Test
    void whenParentSchemaLocationIsNull_thenNPE() throws Exception {
        // child location is valid
        AbsoluteIri childIri = mock(AbsoluteIri.class);
        when(schemaLocation.getAbsoluteIri()).thenReturn(childIri);
        // parent returns null for getSchemaLocation()
        when(parentSchema.getSchemaLocation()).thenReturn(null);
        JsonSchema schema = createSchema(parentSchema);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId();
        doReturn(parentSchema).when(spy).getParentSchema();
        assertThrows(NullPointerException.class, spy::isSchemaResourceRoot, "If parent.getSchemaLocation() is null a NPE should be thrown");
    }

    @Test
    void whenChildSchemaLocationIsNull_thenNPE() throws Exception {
        // child schemaLocation returns null
        JsonSchema schema = createSchema(parentSchema);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId();
        doReturn(parentSchema).when(spy).getParentSchema();
        // Stub getSchemaLocation() on the child to return null
        doReturn(null).when(spy).getSchemaLocation();
        // Parent location can be anything (non‑null) – we stub it to avoid NPE on parent side
        SchemaLocation parentLocation = mock(SchemaLocation.class);
        when(parentLocation.getAbsoluteIri()).thenReturn(mock(AbsoluteIri.class));
        when(parentSchema.getSchemaLocation()).thenReturn(parentLocation);
        assertThrows(NullPointerException.class, spy::isSchemaResourceRoot, "If this.getSchemaLocation() is null a NPE should be thrown");
    }

    @Test
    void whenBothSchemaAndParentLocationAreNull_thenNPE() throws Exception {
        JsonSchema schema = createSchema(parentSchema);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId();
        doReturn(parentSchema).when(spy).getParentSchema();
        // Both return null
        doReturn(null).when(spy).getSchemaLocation();
        when(parentSchema.getSchemaLocation()).thenReturn(null);
        assertThrows(NullPointerException.class, spy::isSchemaResourceRoot, "When both child and parent locations are null a NPE is expected");
    }

    // -----------------------------------------------------------------------
    @Test
    void whenGetIdThrowsRuntimeException_thenExceptionPropagates() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        RuntimeException ex = new RuntimeException("boom from getId");
        doThrow(ex).when(spy).getId();
        RuntimeException thrown = assertThrows(RuntimeException.class, spy::isSchemaResourceRoot, "Exception from getId() should propagate unchanged");
        assertSame(ex, thrown, "The same RuntimeException instance should be re‑thrown");
    }

    @Test
    void whenGetSchemaLocationThrowsRuntimeException_thenExceptionPropagates() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        // id null to reach location logic
        doReturn(null).when(spy).getId();
        // Stub getSchemaLocation() to return a mock that throws on getAbsoluteIri()
        SchemaLocation badLocation = mock(SchemaLocation.class);
        when(badLocation.getAbsoluteIri()).thenThrow(new RuntimeException("boom from getAbsoluteIri"));
        doReturn(badLocation).when(spy).getSchemaLocation();
        RuntimeException thrown = assertThrows(RuntimeException.class, spy::isSchemaResourceRoot, "Exception from getSchemaLocation().getAbsoluteIri() should propagate");
        assertEquals("boom from getAbsoluteIri", thrown.getMessage());
    }

    @Test
    void whenParentGetSchemaLocationThrowsRuntimeException_thenExceptionPropagates() throws Exception {
        JsonSchema schema = createSchema(parentSchema);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId();
        doReturn(parentSchema).when(spy).getParentSchema();
        // Child location is fine
        AbsoluteIri childIri = mock(AbsoluteIri.class);
        when(schemaLocation.getAbsoluteIri()).thenReturn(childIri);
        // Parent location mock throws
        SchemaLocation parentBadLocation = mock(SchemaLocation.class);
        when(parentBadLocation.getAbsoluteIri()).thenThrow(new RuntimeException("parent IRI failure"));
        when(parentSchema.getSchemaLocation()).thenReturn(parentBadLocation);
        RuntimeException thrown = assertThrows(RuntimeException.class, spy::isSchemaResourceRoot, "Exception from parent.getSchemaLocation().getAbsoluteIri() should propagate");
        assertEquals("parent IRI failure", thrown.getMessage());
    }
}
