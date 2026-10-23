You are tasked with creating a framework for a test class in Java. Now analyze the following information carefully:

1. The method that needs to be tested and its parameters.
2. The constructor of the target class and its parameters.
3. Any dependencies or external resources used by the target class.

Based on this analysis, complete the test class template in the provided structure:

1. Initialize necessary fields and dependencies within setupBeforeAll() and setupBeforeEach().
2. Handle any required cleanup in teardownAfterEach() and teardownAfterAll().
3. Make sure not to invoke any private methods or fields of the target class and dependent class.
4. Refer to the "given" phase in the existing test class for guidance.
5. Add necessary import statements and ensure there are no syntax errors in the generated code.

@input{focal method and target class}
Here is the code of focal method, `getEvaluatedItems()`, with the focal class `CollectorContext_getEvaluatedItems_Test`:
```Java
package com.networknt.schema;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Deque;
import java.util.Map;
import java.util.Collections;
public class CollectorContext {
    
        public static class Scope {
        private final JsonSchema containingSchema;
    
        /**
         * Used to track which array items have been evaluated.
         */
        private final Collection<String> evaluatedItems;
    
        /**
         * Used to track which properties have been evaluated.
         */
        private final Collection<String> evaluatedProperties;
    
        private final boolean top;
    
        Scope(boolean disableUnevaluatedItems, boolean disableUnevaluatedProperties, JsonSchema containingSchema) {
            this(false, disableUnevaluatedItems, disableUnevaluatedProperties, containingSchema);
        }
    
        Scope(boolean top, boolean disableUnevaluatedItems, boolean disableUnevaluatedProperties, JsonSchema containingSchema) {
            this.top = top;
            this.containingSchema = containingSchema;
            this.evaluatedItems = newCollection(disableUnevaluatedItems);
            this.evaluatedProperties = newCollection(disableUnevaluatedProperties);
        }
    
        private static Collection<String> newCollection(boolean disabled) {
            return !disabled ? new ArrayList<>() : new AbstractCollection<String>() {
                @Override
                public boolean add(String e) {
                    return false;
                }
    
                @Override
                public Iterator<String> iterator() {
                    return Collections.emptyIterator();
                }
    
                @Override
                public boolean remove(Object o) {
                    return false;
                }
    
                @Override
                public int size() {
                    return 0;
                }
            };
        }
    
        public boolean isTop() {
            return this.top;
        }
    
        public JsonSchema getContainingSchema() {
            return this.containingSchema;
        }
    
        /**
         * Identifies which array items have been evaluated.
         *
         * @return the set of evaluated items (never null)
         */
        public Collection<String> getEvaluatedItems() {
            return this.evaluatedItems;
        }
    
        /**
         * Identifies which properties have been evaluated.
         *
         * @return the set of evaluated properties (never null)
         */
        public Collection<String> getEvaluatedProperties() {
            return this.evaluatedProperties;
        }
    
        /**
         * Merges the provided scope into this scope.
         *
         * @param scope
         * 		the scope to merge
         * @return this scope
         */
        public Scope mergeWith(Scope scope) {
            getEvaluatedItems().addAll(scope.getEvaluatedItems());
            getEvaluatedProperties().addAll(scope.getEvaluatedProperties());
            return this;
        }
    
        @Override
        public String toString() {
            return new StringBuilder("{ ").append("\"evaluatedItems\": ").append(this.evaluatedItems).append(", ").append("\"evaluatedProperties\": ").append(this.evaluatedProperties).append(" }").toString();
        }
    }
public Collection<String> getEvaluatedItems() {
        return getDynamicScope().getEvaluatedItems();
    }
public <E> void add(String name, Collector<E> collector);
    public <E> void add(String name, Object object);
    public void combineWithCollector(String name, Object data);
    public Scope enterDynamicScope();
    public Scope enterDynamicScope(JsonSchema containingSchema);
    public Scope exitDynamicScope();
    public Object get(String name);
    public Map<String, Object> getAll();
    public Scope getDynamicScope();
    public Collection<String> getEvaluatedProperties();
    public JsonSchema getOutermostSchema();
    void loadCollectors();
}
```


@input{context information}

api document of class com.networknt.schema.CollectorContext: 

 * Context for holding the output returned by the {@link Collector}
 * implementations.
 

constructors for class `com.networknt.schema.CollectorContext`: 
params: 
body:
```java
    public CollectorContext() {
        this(false, false);
    }

```
params: 
body:
```java
    public CollectorContext(boolean disableUnevaluatedItems, boolean disableUnevaluatedProperties) {
        this.disableUnevaluatedItems = disableUnevaluatedItems;
        this.disableUnevaluatedProperties = disableUnevaluatedProperties;
        this.dynamicScopes.push(newTopScope());
    }

```

existing test class: 
```java
/*
 * Copyright (c) 2020 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.networknt.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

public class CollectorContextTest {

    private static final String SAMPLE_COLLECTOR = "sampleCollector";

    private static final String SAMPLE_COLLECTOR_OTHER = "sampleCollectorOther";

    private JsonSchema jsonSchema;

    private JsonSchema jsonSchemaForCombine;
    
    @BeforeEach
    public void setup() throws Exception {
        setupSchema();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectorContextWithKeyword() throws Exception {
        ValidationResult validationResult = validate("{\"test-property1\":\"sample1\",\"test-property2\":\"sample2\"}");
        Assertions.assertEquals(0, validationResult.getValidationMessages().size());
        List<String> contextValues = (List<String>) validationResult.getCollectorContext().get(SAMPLE_COLLECTOR);
        contextValues.sort(null);
        Assertions.assertEquals(0, validationResult.getValidationMessages().size());
        Assertions.assertEquals(2, contextValues.size());
        Assertions.assertEquals(contextValues.get(0), "actual_value_added_to_context1");
        Assertions.assertEquals(contextValues.get(1), "actual_value_added_to_context2");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectorContextWithMultipleThreads() throws Exception {

        ValidationThread validationRunnable1 = new ValidationThread("{\"test-property1\":\"sample1\" }", "thread1");
        ValidationThread validationRunnable2 = new ValidationThread("{\"test-property1\":\"sample2\" }", "thread2");
        ValidationThread validationRunnable3 = new ValidationThread("{\"test-property1\":\"sample3\" }", "thread3");

        // This simulates calling the validateAndCollect method from three different
        // threads.It should be noted that all these three threads use same
        // json schema instance.Create three threads with there own Runnables.
        Thread thread1 = new Thread(validationRunnable1);
        Thread thread2 = new Thread(validationRunnable2);
        Thread thread3 = new Thread(validationRunnable3);

        thread1.start();
        thread2.start();
        thread3.start();

        thread1.join();
        thread2.join();
        thread3.join();

        ValidationResult validationResult1 = validationRunnable1.getValidationResult();
        ValidationResult validationResult2 = validationRunnable2.getValidationResult();
        ValidationResult validationResult3 = validationRunnable3.getValidationResult();

        Assertions.assertEquals(0, validationResult1.getValidationMessages().size());
        Assertions.assertEquals(0, validationResult2.getValidationMessages().size());
        Assertions.assertEquals(0, validationResult3.getValidationMessages().size());

        List<String> contextValue1 = (List<String>) validationResult1.getCollectorContext().get(SAMPLE_COLLECTOR);
        List<String> contextValue2 = (List<String>) validationResult2.getCollectorContext().get(SAMPLE_COLLECTOR);
        List<String> contextValue3 = (List<String>) validationResult3.getCollectorContext().get(SAMPLE_COLLECTOR);

        Assertions.assertEquals(contextValue1.get(0), "actual_value_added_to_context1");
        Assertions.assertEquals(contextValue2.get(0), "actual_value_added_to_context2");
        Assertions.assertEquals(contextValue3.get(0), "actual_value_added_to_context3");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectorGetAll() throws JsonMappingException, JsonProcessingException, IOException {
......
    }

    /**
     * We will be collecting information/data by adding the data in the form of
     * collectors into collector context object while we are validating this node.
     * This will be helpful in cases where we don't want to revisit the entire JSON
     * document again just for gathering this kind of information. In this test case
     * we expect this validator to be called multiple times as the associated
     * keyword has been used multiple times in JSON Schema.
     */
    private class CustomValidator1 implements JsonValidator {
        @SuppressWarnings("unchecked")
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            // Get an instance of collector context.
            CollectorContext collectorContext = executionContext.getCollectorContext();
            // If collector type is not added to context add one.
            if (collectorContext.get(SAMPLE_COLLECTOR_OTHER) == null) {
                collectorContext.add(SAMPLE_COLLECTOR_OTHER, new ArrayList<String>());
            }
            List<String> returnList = (List<String>) collectorContext.get(SAMPLE_COLLECTOR_OTHER);
            returnList.add(node.textValue());
            return new TreeSet<ValidationMessage>();
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode rootNode) {
            return validate(executionContext, rootNode, rootNode, PathType.DEFAULT.getRoot());
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema) {
            // Ignore this method for testing.
            return null;
        }
    }

    private ValidationResult validate(String jsonData) throws JsonMappingException, JsonProcessingException, Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return this.jsonSchema.validateAndCollect(objectMapper.readTree(jsonData));
    }

    private Map<String, String> getDatasourceMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("sample1", "actual_value_added_to_context1");
        map.put("sample2", "actual_value_added_to_context2");
        map.put("sample3", "actual_value_added_to_context3");
        return map;
    }
}
```

invoke examples of focal method: 
example 1:
```java
    private void doValidate(ExecutionContext executionContext, Set<ValidationMessage> errors, int i, JsonNode node, JsonNode rootNode, String at) {
        Collection<String> evaluatedItems = executionContext.getCollectorContext().getEvaluatedItems();
    }
```
example 2:
```java
    private static Set<String> unevaluatedPaths(CollectorContext collectorContext, Set<String> allPaths) {
        Set<String> unevaluatedProperties = new HashSet<>(allPaths);
        unevaluatedProperties.removeAll(collectorContext.getEvaluatedItems());
    }
```
example 3:
```java
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
        debug(logger, node, rootNode, at);

        // ignores non-arrays
        if (null != this.schema && node.isArray()) {
            Collection<String> evaluatedItems = executionContext.getCollectorContext().getEvaluatedItems();
    }
```



@input{test class template}
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// Add other imports if necessary

@ExtendWith(MockitoExtension.class)
class CollectorContext_getEvaluatedItems_Test {
    @BeforeAll
    static void setupBeforeAll() {
    }

    @BeforeEach
    void setupBeforeEach() {
    }

    @AfterEach
    void teardownAfterEach() {
    }

    @AfterAll
    static void teardownAfterAll() {
    }
}
```

@output{initial test class}: now consider the invoke examples output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.