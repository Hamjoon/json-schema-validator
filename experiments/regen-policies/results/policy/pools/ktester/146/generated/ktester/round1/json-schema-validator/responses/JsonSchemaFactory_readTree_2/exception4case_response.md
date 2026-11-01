{
    "groups": [
        {
            "group": "ObjectMapper unchecked exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "InputStream containing valid JSON text: {\"key\":\"value\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "IllegalStateException is propagated",
                    "description": "When jsonNodeReader is null, the method obtains the ObjectMapper for JSON. The mocked ObjectMapper.readTree throws an unchecked IllegalStateException. The method must not catch it and should let the exception propagate."
                }
            ]
        },
        {
            "group": "ObjectMapper factory method exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "InputStream containing valid JSON text: {\"key\":\"value\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "NullPointerException is propagated",
                    "description": "When jsonNodeReader is null, getObjectMapper(inputFormat) itself throws a NullPointerException (simulating an internal factory failure). The method should propagate this unchecked exception unchanged."
                }
            ]
        }
    ]
}