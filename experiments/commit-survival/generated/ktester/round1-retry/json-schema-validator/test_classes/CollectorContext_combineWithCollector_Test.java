package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CollectorContext_combineWithCollector_Test {

    private static final String COLLECTOR_NAME = "myCollector";

    private CollectorContext collectorContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CollectorContext for every test to avoid state leakage.
        collectorContext = new CollectorContext();
    }

    private static Stream<Arguments> provideVariousPayloads() {
        return Stream.of(// Integer
        Arguments.of("typeCollector", 42), // List<Integer>
        Arguments.of("typeCollector", Arrays.asList(1, 2, 3)), // Map<String, String>
        Arguments.of("typeCollector", Map.of("key", "value")), // Boolean
        Arguments.of("typeCollector", true), // Set<String>
        Arguments.of("typeCollector", new HashSet<>(List.of("a", "b"))));
    }

    /**
     * Verify that when a {@link Collector} is registered under the supplied name,
     * {@code combineWithCollector} forwards the data to the collector's {@code combine}
     * method.
     */
    @Test
    void shouldCombineDataWhenCollectorIsPresent() {
        // given
        @SuppressWarnings("unchecked")
        Collector<Object> mockCollector = mock(Collector.class);
        collectorContext.add(COLLECTOR_NAME, mockCollector);
        String data = "sample-data";
        // when
        collectorContext.combineWithCollector(COLLECTOR_NAME, data);
        // then
        verify(mockCollector, times(1)).combine(data);
    }

    /**
     * Verify that if the name resolves to an object that is *not* a {@link Collector},
     * the method completes silently without throwing and without invoking any
     * collector logic.
     */
    @Test
    void shouldDoNothingWhenEntryIsNotACollector() {
        // given
        Object notACollector = new ArrayList<>();
        collectorContext.add(COLLECTOR_NAME, notACollector);
        String data = "irrelevant";
        // when / then – no exception should be thrown
        Assertions.assertDoesNotThrow(() -> collectorContext.combineWithCollector(COLLECTOR_NAME, data));
    }

    /**
     * Verify that when the supplied name is absent from the context, the method
     * simply returns without side effects.
     */
    @Test
    void shouldDoNothingWhenNoEntryExists() {
        // given – no entry added for COLLECTOR_NAME
        String data = "any-data";
        // when / then – should not throw
        Assertions.assertDoesNotThrow(() -> collectorContext.combineWithCollector(COLLECTOR_NAME, data));
    }

    /**
     * The stored {@link Collector} should receive a {@code null} payload when the
     * data argument is {@code null}. The collector must still be invoked.
     */
    @Test
    void shouldPassNullDataToCollector() {
        // given
        @SuppressWarnings("unchecked")
        Collector<Object> mockCollector = mock(Collector.class);
        collectorContext.add("nullDataCollector", mockCollector);
        // when
        collectorContext.combineWithCollector("nullDataCollector", null);
        // then
        verify(mockCollector, times(1)).combine(isNull());
    }

    /**
     * A {@link Collector} stored under a {@code null} key must be retrieved when the
     * caller also supplies {@code null} as the name.
     */
    @Test
    void shouldCombineWhenNameIsNullAndCollectorExists() {
        // given
        @SuppressWarnings("unchecked")
        Collector<Object> mockCollector = mock(Collector.class);
        collectorContext.add(null, mockCollector);
        String data = "nullKeyPayload";
        // when
        collectorContext.combineWithCollector(null, data);
        // then
        verify(mockCollector, times(1)).combine(data);
    }

    /**
     * Supplying {@code null} as the name when there is no matching entry must result
     * in a no‑op.
     */
    @Test
    void shouldDoNothingWhenNullNameHasNoCollector() {
        // given – no entry with a null key
        String data = "anyData";
        // when / then – no exception and no interaction with any collector
        assertDoesNotThrow(() -> collectorContext.combineWithCollector(null, data));
    }

    /**
     * If the underlying collector throws an exception, {@code combineWithCollector}
     * must let it propagate.
     */
    @Test
    void shouldPropagateExceptionFromCollector() {
        // given
        @SuppressWarnings("unchecked")
        Collector<Object> mockCollector = mock(Collector.class);
        RuntimeException ex = new RuntimeException("collector failure");
        doThrow(ex).when(mockCollector).combine(any());
        collectorContext.add("exceptionCollector", mockCollector);
        // when / then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> collectorContext.combineWithCollector("exceptionCollector", "trigger"));
        assertSame(ex, thrown);
    }

    /**
     * Parameterized test that verifies a variety of payload types are forwarded
     * unchanged to the collector's {@code combine} method.
     */
    @ParameterizedTest(name = "[{index}] name={0}, data={1}")
    @MethodSource("provideVariousPayloads")
    void shouldForwardVariousPayloadTypes(String name, Object payload) {
        // given
        @SuppressWarnings("unchecked")
        Collector<Object> mockCollector = mock(Collector.class);
        collectorContext.add(name, mockCollector);
        // when
        collectorContext.combineWithCollector(name, payload);
        // then
        verify(mockCollector, times(1)).combine(payload);
    }
}
