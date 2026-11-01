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

/**
 * Unit tests for {@link JsonSchema#isSchemaResourceRoot()}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>normal logical branches (id present, no parent, different IRI, same IRI)</li>
 *   <li>edge‑cases where IDs are empty or whitespace strings</li>
 *   <li>situations that lead to {@link NullPointerException}</li>
 *   <li>propagation of unchecked exceptions from overridden methods</li>
 * </ul>
 *
 * <p>All instances of {@link JsonSchema} are created via reflection using the
 * private constructor.  A {@code spy} is used so that we can stub the
 * protected methods {@code getId()}, {@code getParentSchema()},
 * {@code getSchemaLocation()} etc. without touching private fields.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_isSchemaResourceRoot_Test {

    // -----------------------------------------------------------------------
    // Mocks required to build a JsonSchema instance
    // -----------------------------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;          // child location
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;

    // Parent schema mock – reused in many tests
    @Mock JsonSchema parentSchema;

    /**
     * Helper that builds a real {@link JsonSchema} instance via the private
     * constructor.  The {@code parent} argument can be {@code null} or a mock.
     */
    private JsonSchema createSchema(JsonSchema parent) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        // The last boolean flag is the “suppressSubSchemaRetrieval” flag – we pass false.
        return ctor.newInstance(validationContext, schemaLocation, evaluationPath, schemaNode, parent, false);
    }

    // -----------------------------------------------------------------------
    // Standard lifecycle methods (do not modify)
    // -----------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these unit tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension resets mocks automatically
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    // -----------------------------------------------------------------------
    //  Branch‑coverage tests (already present in the original framework)
    // -----------------------------------------------------------------------
    @Test
    void whenIdIsNotNull_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        doReturn("someId").when(spy).getId(); // non‑null id
        assertTrue(spy.isSchemaResourceRoot(),
                "Schema with a non‑null id must be considered a resource root");
    }

    @Test
    void whenIdIsNullAndParentIsNull_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId(); // id null, parent already null
        assertTrue(spy.isSchemaResourceRoot(),
                "Schema with no id and no parent must be considered a resource root");
    }

    @Test
    void whenIdIsNullParentExistsAndIriDiffers_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        // Child IRI
        AbsoluteIri childIri = mock(AbsoluteIri.class);
        // Parent IRI – different instance
        AbsoluteIri parentIri = mock(AbsoluteIri.class);

        when(schemaLocation.getAbsoluteIri()).thenReturn(childIri);
        when(parentSchema.getSchemaLocation()).thenReturn(schemaLocation); // child location for parent is irrelevant
        // Parent's own location returns a different IRI
        SchemaLocation parentLocation = mock(SchemaLocation.class);
        when(parentLocation.getAbsoluteIri()).thenReturn(parentIri);
        when(parentSchema.getSchemaLocation()).thenReturn(parentLocation);

        JsonSchema schema = createSchema(parentSchema);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId();
        doReturn(parentSchema).when(spy).getParentSchema();

        assertTrue(spy.isSchemaResourceRoot(),
                "Schema whose IRI differs from its parent must be a resource root");
    }

    @Test
    void whenIdIsNullParentExistsAndIriEquals_thenIsSchemaResourceRootReturnsFalse() throws Exception {
        // Same IRI instance for child and parent → Objects.equals returns true
        AbsoluteIri sharedIri = mock(AbsoluteIri.class);
        when(schemaLocation.getAbsoluteIri()).thenReturn(sharedIri);
        when(parentSchema.getSchemaLocation()).thenReturn(schemaLocation); // same location instance

        JsonSchema schema = createSchema(parentSchema);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId();
        doReturn(parentSchema).when(spy).getParentSchema();

        assertFalse(spy.isSchemaResourceRoot(),
                "Schema with equal IRI to its parent and no id must NOT be a resource root");
    }

    // -----------------------------------------------------------------------
    //  Edge‑case tests (empty / whitespace IDs)
    // -----------------------------------------------------------------------
    @Test
    void whenIdIsEmptyString_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        doReturn("").when(spy).getId(); // empty but non‑null
        assertTrue(spy.isSchemaResourceRoot(),
                "An empty string ID is still non‑null and should make the schema a resource root");
    }

    @Test
    void whenIdIsWhitespace_thenIsSchemaResourceRootReturnsTrue() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        doReturn("   ").when(spy).getId(); // whitespace but non‑null
        assertTrue(spy.isSchemaResourceRoot(),
                "A whitespace ID is non‑null and must cause the method to return true");
    }

    // -----------------------------------------------------------------------
    //  NullPointerException edge cases
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

        assertThrows(NullPointerException.class, spy::isSchemaResourceRoot,
                "If parent.getSchemaLocation() is null a NPE should be thrown");
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

        assertThrows(NullPointerException.class, spy::isSchemaResourceRoot,
                "If this.getSchemaLocation() is null a NPE should be thrown");
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

        assertThrows(NullPointerException.class, spy::isSchemaResourceRoot,
                "When both child and parent locations are null a NPE is expected");
    }

    // -----------------------------------------------------------------------
    //  Exception‑propagation tests
    // -----------------------------------------------------------------------
    @Test
    void whenGetIdThrowsRuntimeException_thenExceptionPropagates() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        RuntimeException ex = new RuntimeException("boom from getId");
        doThrow(ex).when(spy).getId();

        RuntimeException thrown = assertThrows(RuntimeException.class, spy::isSchemaResourceRoot,
                "Exception from getId() should propagate unchanged");
        assertSame(ex, thrown, "The same RuntimeException instance should be re‑thrown");
    }

    @Test
    void whenGetSchemaLocationThrowsRuntimeException_thenExceptionPropagates() throws Exception {
        JsonSchema schema = createSchema(null);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(null).when(spy).getId(); // id null to reach location logic

        // Stub getSchemaLocation() to return a mock that throws on getAbsoluteIri()
        SchemaLocation badLocation = mock(SchemaLocation.class);
        when(badLocation.getAbsoluteIri()).thenThrow(new RuntimeException("boom from getAbsoluteIri"));
        doReturn(badLocation).when(spy).getSchemaLocation();

        RuntimeException thrown = assertThrows(RuntimeException.class, spy::isSchemaResourceRoot,
                "Exception from getSchemaLocation().getAbsoluteIri() should propagate");
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

        RuntimeException thrown = assertThrows(RuntimeException.class, spy::isSchemaResourceRoot,
                "Exception from parent.getSchemaLocation().getAbsoluteIri() should propagate");
        assertEquals("parent IRI failure", thrown.getMessage());
    }
}
```