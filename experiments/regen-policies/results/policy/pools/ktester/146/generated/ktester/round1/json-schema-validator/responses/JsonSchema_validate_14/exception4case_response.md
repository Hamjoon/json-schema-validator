{
    "tests": [
        {
            "group": "Invalid YAML input (syntax error)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "name: John\nage: 30: invalid"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "format",
                            "value": "OutputFormat<List<ValidationMessage>> (collects messages)"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer that does nothing"
                        }
                    ],
                    "expected": "A list containing a ValidationMessage describing a YAML parsing error (e.g., \"while scanning a simple key\") is returned.",
                    "description": "When the supplied YAML string has a syntax error, the deserialize step should fail and the method must surface the parsing problem as a ValidationMessage."
                }
            ]
        },
        {
            "group": "URL_ENCODED input decodes to invalid JSON",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "%7B%22name%22%3A%22John%22%2C%22age%22%3A%22thirty%22%7D"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.URL_ENCODED"
                        },
                        {
                            "parameter": "format",
                            "value": "OutputFormat<List<ValidationMessage>> (collects messages)"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer that does nothing"
                        }
                    ],
                    "expected": "A list containing a ValidationMessage describing a JSON parsing error (type mismatch for property \"age\" if schema expects integer, or generic parsing error if the JSON is malformed) is returned.",
                    "description": "The method should URL‑decode the input, then attempt to deserialize it as JSON; if the resulting JSON is syntactically invalid or violates the schema, a ValidationMessage must be produced."
                }
            ]
        }
    ]
}