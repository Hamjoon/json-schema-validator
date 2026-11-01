{
    "group": "getSchema(String, InputFormat) - valid JSON schema",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"name\\\":{\\\"type\\\":\\\"string\\\"}}}\""
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.JSON"
                }
            ],
            "expected": "non‑null JsonSchema instance whose meta‑schema version matches the default (e.g., Draft‑7) and whose root node type is OBJECT",
            "description": "Standard case: a well‑formed JSON schema string is supplied together with the JSON input format. The method should parse the string and return a usable JsonSchema object."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"age\\\":{\\\"type\\\":\\\"integer\\\"}}}\""
                },
                {
                    "parameter": "inputFormat",
                    "value": "null"
                }
            ],
            "expected": "non‑null JsonSchema instance (same as when InputFormat.JSON is passed)",
            "description": "When the inputFormat argument is null the implementation should fall back to the default JSON format and still successfully parse the schema."
        }
    ]
}