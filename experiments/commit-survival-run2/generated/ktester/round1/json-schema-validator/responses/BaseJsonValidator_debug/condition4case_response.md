{
    "group": "debugMethodTests",
    "cases": [
        {
            "input": [
                { "parameter": "logger", "value": "mockLogger1" },
                { "parameter": "node", "value": "jsonNodeA" },
                { "parameter": "rootNode", "value": "jsonRootNodeA" },
                { "parameter": "at", "value": "/path/to/element" }
            ],
            "expected": "logger.debug should be invoked once with message \"validate( {}, {}, {})\" and arguments [jsonNodeA, jsonRootNodeA, \"/path/to/element\"]",
            "description": "Standard case where all arguments are non‑null; verifies that the debug log contains the exact placeholders and supplied values."
        },
        {
            "input": [
                { "parameter": "logger", "value": "mockLogger2" },
                { "parameter": "node", "value": "null" },
                { "parameter": "rootNode", "value": "jsonRootNodeB" },
                { "parameter": "at", "value": "/another/path" }
            ],
            "expected": "logger.debug should be invoked once with message \"validate( {}, {}, {})\" and arguments [null, jsonRootNodeB, \"/another/path\"]",
            "description": "Node argument is null; ensures the method still forwards the null reference to the logger without throwing."
        },
        {
            "input": [
                { "parameter": "logger", "value": "mockLogger3" },
                { "parameter": "node", "value": "jsonNodeC" },
                { "parameter": "rootNode", "value": "null" },
                { "parameter": "at", "value": "/yet/another/path" }
            ],
            "expected": "logger.debug should be invoked once with message \"validate( {}, {}, {})\" and arguments [jsonNodeC, null, \"/yet/another/path\"]",
            "description": "RootNode argument is null; validates that null is correctly passed to the logger."
        },
        {
            "input": [
                { "parameter": "logger", "value": "mockLogger4" },
                { "parameter": "node", "value": "jsonNodeD" },
                { "parameter": "rootNode", "value": "jsonRootNodeD" },
                { "parameter": "at", "value": "\"\" (empty string)" }
            ],
            "expected": "logger.debug should be invoked once with message \"validate( {}, {}, {})\" and arguments [jsonNodeD, jsonRootNodeD, \"\"]",
            "description": "The 'at' path is an empty string; checks that the method does not alter empty values."
        },
        {
            "input": [
                { "parameter": "logger", "value": "mockLogger5" },
                { "parameter": "node", "value": "jsonNodeE" },
                { "parameter": "rootNode", "value": "jsonRootNodeE" },
                { "parameter": "at", "value": "/path/with/special/chars/!@#$%^&*()" }
            ],
            "expected": "logger.debug should be invoked once with message \"validate( {}, {}, {})\" and arguments [jsonNodeE, jsonRootNodeE, \"/path/with/special/chars/!@#$%^&*()\"]",
            "description": "Path contains special characters; ensures the string is passed unchanged to the logger."
        }
    ]
}