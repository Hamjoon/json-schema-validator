As a professional Java software testing expert, your task is to generate test cases for a focal method.

Guidelines:
1. Analyze the method's functionality and identify possible input combinations including normal cases and boundary conditions.
2. Review the provided existing test cases. Avoid duplication and create new test cases for untested input combinations.

Context Provided:
1. The focal method that needs testing.
2. Relevant code and metadata for the class containing the method, including fields and other methods.
3. The existing test cases (in JSON format) to help avoid redundancy.

Output Instructions:
- Organize the test cases by the name of the test function (group).
- Each test case must include:
    - input: A list of parameter names and values. Use an empty list `[]` if no input is required.
    - expected: The return value or expected observable behavior.
    - description: A clear and concise explanation of the scenario and the intent of the test case.

Maintain the following strict JSON format:
```json
[
    {
        "group": "test name",
        "cases": [
            {
                "input": [
                    {
                        "parameter": "param name",
                        "value": "param value"
                    }
                ],
                "expected": "expected exception or behavior",
                "description": "test scenario description"
            }
        ]
    },
    {...}
]
```

@input{focal method and target class}
Here is the code of focal method, `getEvaluatedProperties()`, with the focal class `CollectorContext`:
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
public Collection<String> getEvaluatedProperties() {
        return getDynamicScope().getEvaluatedProperties();
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
    public Collection<String> getEvaluatedItems();
    public JsonSchema getOutermostSchema();
    void loadCollectors();
}
```

@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```

@output{test cases}: output the test cases in the JSON format: