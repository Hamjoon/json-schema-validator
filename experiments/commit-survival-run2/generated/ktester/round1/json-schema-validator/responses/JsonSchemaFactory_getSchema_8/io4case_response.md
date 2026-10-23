{
    "group": "Unsupported URI scheme",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "ftp://example.com/schema.json"
                },
                {
                    "parameter": "jsonNode",
                    "value": "{ \"type\": \"object\" }"
                }
            ],
            "expected": "IllegalArgumentException (unsupported scheme)",
            "description": "The factory should reject URIs with schemes it does not handle (e.g., ftp) and throw an IllegalArgumentException."
        }
    ]
}