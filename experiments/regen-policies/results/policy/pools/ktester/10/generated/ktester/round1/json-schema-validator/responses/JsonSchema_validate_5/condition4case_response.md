{
    "group": "Normal Validation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "rootNode",
                    "value": "ObjectNode representing JSON {\"name\":\"John\",\"age\":30}"
                },
                {
                    "parameter": "format",
                    "value": "OutputFormat<List<ValidationMessage>> that returns the list of messages"
                }
            ],
            "expected": "Empty list [] (no validation errors)",
            "description": "Valid JSON that satisfies the schema; validate should return an empty list of ValidationMessage via the provided OutputFormat."
        },
        {
            "input": [
                {
                    "parameter": "rootNode",
                    "value": "ObjectNode representing JSON {\"age\":30}"
                },
                {
                    "parameter": "format",
                    "value": "OutputFormat<List<ValidationMessage>> that returns the list of messages"
                }
            ],
            "expected": "List containing a ValidationMessage about missing required property \"name\"",
            "description": "JSON missing a required property; the schema’s required‑validator should add a ValidationMessage which is returned by the OutputFormat."
        }
    ]
}