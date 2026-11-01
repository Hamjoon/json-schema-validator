{
    "groups": [
        {
            "group": "EvaluationPathException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.IllegalStateException",
                    "description": "When getEvaluationPath() throws an IllegalStateException (simulated via a subclass or mock), toString() should propagate the exception without catching it."
                },
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "When getEvaluationPath() internally dereferences a null field and throws a NullPointerException, the exception should bubble up from toString()."
                }
            ]
        },
        {
            "group": "SchemaNodeRetrievalException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.IllegalArgumentException",
                    "description": "When getSchemaNode() itself throws an IllegalArgumentException (e.g., due to an invalid internal state), toString() should not catch it and the exception propagates."
                }
            ]
        },
        {
            "group": "SchemaNodeToStringException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.RuntimeException",
                    "description": "When the JsonNode returned by getSchemaNode() overrides toString() and throws a RuntimeException, toString() of JsonSchema should propagate that RuntimeException."
                },
                {
                    "input": [],
                    "expected": "java.lang.UnsupportedOperationException",
                    "description": "When the JsonNode's toString() implementation throws an UnsupportedOperationException, the exception should be propagated by JsonSchema.toString()."
                }
            ]
        }
    ]
}