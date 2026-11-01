{
    "group": "walk_nullExecutionContext_throwsNullPointerException",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": null
                },
                {
                    "parameter": "node",
                    "value": "\"valid\""
                },
                {
                    "parameter": "outputFormat",
                    "value": "SetOutputFormat"
                },
                {
                    "parameter": "validate",
                    "value": true
                },
                {
                    "parameter": "executionCustomizer",
                    "value": null
                }
            ],
            "expected": "NullPointerException",
            "description": "Passing a null ExecutionContext should cause a NullPointerException because the walk implementation dereferences the context without a null‑check."
        }
    ]
}