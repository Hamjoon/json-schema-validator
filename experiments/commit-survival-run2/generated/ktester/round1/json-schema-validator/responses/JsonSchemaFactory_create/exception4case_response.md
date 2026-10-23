{
    "group": "ParentSchema getCurrentUri exception propagation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "new ValidationContext()"
                },
                {
                    "parameter": "schemaPath",
                    "value": "\"/any\""
                },
                {
                    "parameter": "schemaNode",
                    "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
                },
                {
                    "parameter": "parentSchema",
                    "value": "new DummyParentSchemaThrowing(new NullPointerException(\"parent URI is null\"))"
                }
            ],
            "expected": "NullPointerException is thrown and propagated from parentSchema.getCurrentUri()",
            "description": "When parentSchema.getCurrentUri() throws a NullPointerException, create() must not catch it; the exception should bubble up to the caller."
        },
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "null"
                },
                {
                    "parameter": "schemaPath",
                    "value": "null"
                },
                {
                    "parameter": "schemaNode",
                    "value": "null"
                },
                {
                    "parameter": "parentSchema",
                    "value": "new DummyParentSchemaThrowing(new IllegalStateException(\"unexpected state\"))"
                }
            ],
            "expected": "IllegalStateException is thrown and propagated from parentSchema.getCurrentUri()",
            "description": "Even when validationContext, schemaPath, and schemaNode are null, the method first evaluates parentSchema.getCurrentUri(); if that call throws an IllegalStateException, create() should propagate it unchanged."
        }
    ]
}