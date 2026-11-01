{
    "group": "BaseJsonValidator.debug method behavior",
    "cases": [
        {
            "input": [
                {
                    "parameter": "logger",
                    "value": "mockLogger"
                },
                {
                    "parameter": "node",
                    "value": "sampleNode"
                },
                {
                    "parameter": "rootNode",
                    "value": "rootNode"
                },
                {
                    "parameter": "instanceLocation",
                    "value": "JsonNodePath.of(\"/a/b\")"
                }
            ],
            "expected": "logger.debug called once with format string \"validate( {}, {}, {})\" and arguments sampleNode, rootNode, JsonNodePath.of(\"/a/b\")",
            "description": "Standard case: all parameters are non‑null. Verify that the debug logger receives the correct formatted message and the three arguments."
        },
        {
            "input": [
                {
                    "parameter": "logger",
                    "value": "mockLogger"
                },
                {
                    "parameter": "node",
                    "value": "null"
                },
                {
                    "parameter": "rootNode",
                    "value": "rootNode"
                },
                {
                    "parameter": "instanceLocation",
                    "value": "JsonNodePath.of(\"/a\")"
                }
            ],
            "expected": "logger.debug called once with format string \"validate( {}, {}, {})\" and arguments null, rootNode, JsonNodePath.of(\"/a\")",
            "description": "Node argument is null. The method should still invoke logger.debug with a null placeholder for the first argument."
        },
        {
            "input": [
                {
                    "parameter": "logger",
                    "value": "mockLogger"
                },
                {
                    "parameter": "node",
                    "value": "sampleNode"
                },
                {
                    "parameter": "rootNode",
                    "value": "null"
                },
                {
                    "parameter": "instanceLocation",
                    "value": "JsonNodePath.of(\"/b\")"
                }
            ],
            "expected": "logger.debug called once with format string \"validate( {}, {}, {})\" and arguments sampleNode, null, JsonNodePath.of(\"/b\")",
            "description": "RootNode argument is null. Verify that logger.debug receives a null for the second placeholder."
        },
        {
            "input": [
                {
                    "parameter": "logger",
                    "value": "mockLogger"
                },
                {
                    "parameter": "node",
                    "value": "sampleNode"
                },
                {
                    "parameter": "rootNode",
                    "value": "rootNode"
                },
                {
                    "parameter": "instanceLocation",
                    "value": "null"
                }
            ],
            "expected": "logger.debug called once with format string \"validate( {}, {}, {})\" and arguments sampleNode, rootNode, null",
            "description": "InstanceLocation argument is null. The method should still call logger.debug with a null for the third placeholder."
        },
        {
            "input": [
                {
                    "parameter": "logger",
                    "value": "mockLogger"
                },
                {
                    "parameter": "node",
                    "value": "null"
                },
                {
                    "parameter": "rootNode",
                    "value": "null"
                },
                {
                    "parameter": "instanceLocation",
                    "value": "null"
                }
            ],
            "expected": "logger.debug called once with format string \"validate( {}, {}, {})\" and arguments null, null, null",
            "description": "All arguments are null. Ensure the method does not throw and still forwards the nulls to the logger."
        }
    ]
}