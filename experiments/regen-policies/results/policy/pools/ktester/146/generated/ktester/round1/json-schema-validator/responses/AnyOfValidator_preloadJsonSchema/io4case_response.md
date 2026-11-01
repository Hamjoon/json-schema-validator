{
    "tests": [
        {
            "group": "Preload with list containing null element",
            "cases": [
                {
                    "input": [],
                    "expected": "preloadJsonSchemas is called with a list that includes a null entry and canShortCircuit() is invoked exactly once",
                    "description": "Create an AnyOfValidator with a schemas list containing a valid JsonSchema object followed by a null element. Verify that preloadJsonSchema forwards the list unchanged (including the null) to preloadJsonSchemas and still calls canShortCircuit() once."
                }
            ]
        },
        {
            "group": "Preload with unmodifiable schemas list",
            "cases": [
                {
                    "input": [],
                    "expected": "preloadJsonSchemas receives the same unmodifiable list instance and canShortCircuit() is invoked exactly once",
                    "description": "Supply an AnyOfValidator with Collections.unmodifiableList([...]) as the schemas collection. Ensure that preloadJsonSchema does not attempt to modify the list (no UnsupportedOperationException) and forwards the list reference to preloadJsonSchemas, then calls canShortCircuit() once."
                }
            ]
        },
        {
            "group": "Preload called multiple times (idempotent behavior)",
            "cases": [
                {
                    "input": [],
                    "expected": "preloadJsonSchemas and canShortCircuit() are each invoked once per call; calling preloadJsonSchema twice results in two total invocations of each helper method",
                    "description": "Invoke preloadJsonSchema() on the same AnyOfValidator instance twice. Verify that each invocation triggers a separate call to preloadJsonSchemas and canShortCircuit(), confirming that the method does not cache a single execution and can be safely called repeatedly."
                }
            ]
        },
        {
            "group": "Preload verifies single delegation to preloadJsonSchemas",
            "cases": [
                {
                    "input": [],
                    "expected": "preloadJsonSchemas is called exactly once per preloadJsonSchema execution, even if the internal schemas list contains many elements",
                    "description": "Use a spy or mock for the AnyOfValidator that overrides preloadJsonSchemas to count invocations. Provide a list of several JsonSchema objects and call preloadJsonSchema(). Assert that the helper method is invoked only once."
                }
            ]
        },
        {
            "group": "Preload propagates exception from a schema during preload",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException thrown by a schema's preloadJsonSchema is propagated, and canShortCircuit() is still invoked exactly once before the exception bubbles up",
                    "description": "Mock one JsonSchema in the schemas list so that its preloadJsonSchema() throws a RuntimeException. Call AnyOfValidator.preloadJsonSchema() and verify that the exception is re‑thrown while canShortCircuit() has been called."
                }
            ]
        },
        {
            "group": "Preload propagates exception from canShortCircuit",
            "cases": [
                {
                    "input": [],
                    "expected": "Exception thrown by canShortCircuit() is propagated and preloadJsonSchemas is still called beforehand",
                    "description": "Create a subclass of AnyOfValidator that overrides canShortCircuit() to throw a custom RuntimeException. Ensure that preloadJsonSchemas is invoked first, then the exception from canShortCircuit() propagates out of preloadJsonSchema()."
                }
            ]
        },
        {
            "group": "Preload with schemas list that is a custom List implementation tracking iteration",
            "cases": [
                {
                    "input": [],
                    "expected": "preloadJsonSchemas receives the custom List instance and iterates over it exactly once; canShortCircuit() is invoked once",
                    "description": "Provide a custom List implementation that records each call to get(int) and size(). After calling preloadJsonSchema(), verify that size() and get() are each called the expected number of times (size once, get for each element) and that canShortCircuit() is called once."
                }
            ]
        }
    ]
}