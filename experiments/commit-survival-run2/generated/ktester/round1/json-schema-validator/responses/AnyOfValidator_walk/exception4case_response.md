{
    "group": "walk_whenShouldValidateSchemaFalse_schemaListContainsNull",
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
                    "parameter": "at",
                    "value": "\"/nullElement\""
                },
                {
                    "parameter": "shouldValidateSchema",
                    "value": "false"
                }
            ],
            "expected": "NullPointerException",
            "description": "When shouldValidateSchema is false and the internal schemas list contains a null element, the for‑each loop attempts to invoke walk() on the null entry, causing a NullPointerException to be thrown."
        }
    ]
}