{
    "group": "JsonSchema.toString() - schema node toString() exception propagation",
    "cases": [
        {
            "input": [],
            "expected": "java.lang.IllegalStateException: custom toString failure",
            "description": "The underlying schema node overrides toString() and deliberately throws an IllegalStateException. The toString() method of JsonSchema should not catch this exception, allowing it to propagate to the caller."
        },
        {
            "input": [],
            "expected": "java.lang.RuntimeException: unexpected error",
            "description": "The schema node's toString() throws a generic RuntimeException. Verify that JsonSchema.toString() does not wrap or swallow the exception, and the original RuntimeException is observed by the test."
        }
    ]
}