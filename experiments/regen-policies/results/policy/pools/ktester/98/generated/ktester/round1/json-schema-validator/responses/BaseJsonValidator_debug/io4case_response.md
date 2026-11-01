{
  "testCases": [
    {
      "group": "BaseJsonValidator.debug null handling",
      "cases": [
        {
          "input": [
            { "parameter": "logger", "value": "null" },
            { "parameter": "executionContext", "value": "mockExecutionContext(debugEnabled=true)" },
            { "parameter": "node", "value": "mockJsonNode(toString=\"{}\")" },
            { "parameter": "rootNode", "value": "mockJsonNode(toString=\"{}\")" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(toString=\"/path\")" }
          ],
          "expected": "NullPointerException",
          "description": "Logger is null; the method should throw a NullPointerException when attempting to call logger.isDebugEnabled()."
        },
        {
          "input": [
            { "parameter": "logger", "value": "mockLogger(debugEnabled=true)" },
            { "parameter": "executionContext", "value": "null" },
            { "parameter": "node", "value": "mockJsonNode(toString=\"{}\")" },
            { "parameter": "rootNode", "value": "mockJsonNode(toString=\"{}\")" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(toString=\"/path\")" }
          ],
          "expected": "NullPointerException",
          "description": "ExecutionContext is null; the method should throw a NullPointerException when accessing executionContext.getExecutionConfig()."
        },
        {
          "input": [
            { "parameter": "logger", "value": "mockLogger(debugEnabled=true)" },
            { "parameter": "executionContext", "value": "mockExecutionContext(debugEnabled=true)" },
            { "parameter": "node", "value": "null" },
            { "parameter": "rootNode", "value": "mockJsonNode(toString=\"{}\")" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(toString=\"/path\")" }
          ],
          "expected": "NullPointerException",
          "description": "Node is null; the method should throw a NullPointerException when calling node.toString()."
        },
        {
          "input": [
            { "parameter": "logger", "value": "mockLogger(debugEnabled=true)" },
            { "parameter": "executionContext", "value": "mockExecutionContext(debugEnabled=true)" },
            { "parameter": "node", "value": "mockJsonNode(toString=\"{}\")" },
            { "parameter": "rootNode", "value": "null" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(toString=\"/path\")" }
          ],
          "expected": "NullPointerException",
          "description": "RootNode is null; the method should throw a NullPointerException when calling rootNode.toString()."
        },
        {
          "input": [
            { "parameter": "logger", "value": "mockLogger(debugEnabled=true)" },
            { "parameter": "executionContext", "value": "mockExecutionContext(debugEnabled=true)" },
            { "parameter": "node", "value": "mockJsonNode(toString=\"{}\")" },
            { "parameter": "rootNode", "value": "mockJsonNode(toString=\"{}\")" },
            { "parameter": "instanceLocation", "value": "null" }
          ],
          "expected": "NullPointerException",
          "description": "InstanceLocation is null; the method should throw a NullPointerException when calling instanceLocation.toString()."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.debug special characters handling",
      "cases": [
        {
          "input": [
            { "parameter": "logger", "value": "mockLogger(debugEnabled=true)" },
            { "parameter": "executionContext", "value": "mockExecutionContext(debugEnabled=true)" },
            { "parameter": "node", "value": "mockJsonNode(toString=\"{\\\"key\\\":\\\"value, with, commas\\\"}\")" },
            { "parameter": "rootNode", "value": "mockJsonNode(toString=\"{\\\"root\\\":\\\"line\\nbreak\\\"}\")" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(toString=\"/path/with space\")" }
          ],
          "expected": "logger.debug called once with string \"validate( {\\\"key\\\":\\\"value, with, commas\\\"}, {\\\"root\\\":\\\"line\\nbreak\\\"}, /path/with space)\"",
          "description": "All debug flags enabled and the JsonNode strings contain commas, spaces and a newline; the method must correctly embed these characters in the logged message."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.debug empty string handling",
      "cases": [
        {
          "input": [
            { "parameter": "logger", "value": "mockLogger(debugEnabled=true)" },
            { "parameter": "executionContext", "value": "mockExecutionContext(debugEnabled=true)" },
            { "parameter": "node", "value": "mockJsonNode(toString=\"\")" },
            { "parameter": "rootNode", "value": "mockJsonNode(toString=\"\")" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(toString=\"\")" }
          ],
          "expected": "logger.debug called once with string \"validate( , , )\"",
          "description": "When node, rootNode and instanceLocation all return empty strings, the built log message should contain empty placeholders separated by commas and spaces."
        }
      ]
    }
  ]
}