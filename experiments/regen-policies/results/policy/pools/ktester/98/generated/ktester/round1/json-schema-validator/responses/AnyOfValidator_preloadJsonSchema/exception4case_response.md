{
    "test_cases": [
        {
            "group": "Concurrent modification during preload",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "Thread‑safe list that is modified by another thread during preloadJsonSchema execution"
                        }
                    ],
                    "expected": "ConcurrentModificationException thrown and propagated",
                    "description": "Create an AnyOfValidator with a mutable List (e.g., ArrayList) containing a valid JsonSchema. Start a background thread that adds or removes an element from the list while preloadJsonSchema() is running, causing the iterator inside preloadJsonSchemas to detect concurrent modification and throw ConcurrentModificationException. Verify that the exception propagates out of preloadJsonSchema()."
                }
            ]
        },
        {
            "group": "Immutable schemas list causing UnsupportedOperationException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "Collections.unmodifiableList([...valid JsonSchema objects...])"
                        }
                    ],
                    "expected": "UnsupportedOperationException thrown from preloadJsonSchemas when it attempts to modify the list",
                    "description": "Construct AnyOfValidator with an unmodifiable list (e.g., Collections.unmodifiableList) containing valid JsonSchema instances. If preloadJsonSchemas internally tries to add or replace entries (such as caching a preloaded schema), the operation will fail with UnsupportedOperationException. Verify that this exception is not swallowed by preloadJsonSchema()."
                }
            ]
        },
        {
            "group": "canShortCircuit() throws exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "List with a single valid JsonSchema"
                        },
                        {
                            "parameter": "validator subclass",
                            "value": "Anonymous subclass of AnyOfValidator that overrides canShortCircuit() to throw IllegalStateException"
                        }
                    ],
                    "expected": "IllegalStateException propagated from canShortCircuit()",
                    "description": "Create a subclass of AnyOfValidator where canShortCircuit() is overridden to throw an IllegalStateException (simulating a failure in caching the short‑circuit flag). Invoke preloadJsonSchema() and verify that the IllegalStateException bubbles up, confirming that the method does not catch exceptions from canShortCircuit()."
                }
            ]
        }
    ]
}