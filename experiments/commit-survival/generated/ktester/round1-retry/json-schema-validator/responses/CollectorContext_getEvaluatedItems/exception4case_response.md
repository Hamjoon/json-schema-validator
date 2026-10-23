{
    "testCases": [
        {
            "group": "Null dynamic scope – NPE",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "Create a subclass of CollectorContext that overrides getDynamicScope() to return null. Invoking getEvaluatedItems() should throw a NullPointerException because the method attempts to call getEvaluatedItems() on a null Scope."
                }
            ]
        },
        {
            "group": "Immutable evaluated items collection – UnsupportedOperationException on add",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.UnsupportedOperationException",
                    "description": "Create a custom Scope whose evaluatedItems collection is an unmodifiable collection (e.g., Collections.unmodifiableList(new ArrayList<>())). Override CollectorContext#getDynamicScope() to return this Scope. Calling getEvaluatedItems() returns the unmodifiable collection; attempting to add an element to that collection must throw UnsupportedOperationException."
                }
            ]
        },
        {
            "group": "Exiting beyond root scope – NPE on getEvaluatedItems",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "Instantiate a fresh CollectorContext (default). Call exitDynamicScope() more times than enterDynamicScope() (e.g., call exitDynamicScope() twice). After the extra exit, the internal dynamic scope reference becomes null. A subsequent call to getEvaluatedItems() should throw NullPointerException."
                }
            ]
        }
    ]
}