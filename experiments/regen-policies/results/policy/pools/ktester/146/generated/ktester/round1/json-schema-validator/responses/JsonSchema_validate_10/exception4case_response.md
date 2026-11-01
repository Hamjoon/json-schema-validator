{
    "group": "Malformed URL‑encoded input (percent‑encoding error)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "input",
                    "value": "%ZZ"
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.URL_ENCODED"
                },
                {
                    "parameter": "executionCustomizer",
                    "value": "null"
                }
            ],
            "expected": "IllegalArgumentException (message indicates malformed % encoding)",
            "description": "When the InputFormat is URL_ENCODED the method first decodes the string using URLDecoder. A percent‑sequence that is not followed by two hexadecimal digits (e.g., \"%ZZ\") causes URLDecoder to throw IllegalArgumentException. The validate method must let this exception propagate."
        }
    ]
}