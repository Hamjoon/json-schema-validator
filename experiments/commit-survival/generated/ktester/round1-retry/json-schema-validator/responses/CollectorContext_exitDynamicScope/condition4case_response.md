{
    "tests": [
        {
            "group": "Normal operation - exiting a populated dynamic scope stack",
            "cases": [
                {
                    "input": [],
                    "expected": "Returns the Scope instance that was most recently entered via enterDynamicScope()",
                    "description": "Create a CollectorContext, push a Scope onto the internal deque by calling enterDynamicScope(), then call exitDynamicScope(). The method should pop and return the same Scope object, leaving the deque empty."
                }
            ]
        },
        {
            "group": "Error handling - exiting when the dynamic scope stack is empty",
            "cases": [
                {
                    "input": [],
                    "expected": "Throws java.util.NoSuchElementException",
                    "description": "Create a fresh CollectorContext with no scopes added. Invoking exitDynamicScope() should attempt to pop from an empty Deque, which results in a NoSuchElementException. The test verifies that the exception is thrown."
                }
            ]
        }
    ]
}