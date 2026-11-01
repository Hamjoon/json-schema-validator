{
    "group": "AnyOfValidator.walk - shouldValidateSchema false (empty schemas)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext"
                },
                {
                    "parameter": "node",
                    "value": "mockNode"
                },
                {
                    "parameter": "rootNode",
                    "value": "mockRootNode"
                },
                {
                    "parameter": "instanceLocation",
                    "value": "mockInstanceLocation"
                },
                {
                    "parameter": "shouldValidateSchema",
                    "value": false
                }
            ],
            "expected": "An empty LinkedHashSet (i.e., no ValidationMessage objects).",
            "description": "The AnyOfValidator instance is created with an empty `schemas` list. When `shouldValidateSchema` is false the method must iterate over the (non‑existent) schemas, invoke no `walk` on child schemas and finally return an empty set."
        }
    ]
}