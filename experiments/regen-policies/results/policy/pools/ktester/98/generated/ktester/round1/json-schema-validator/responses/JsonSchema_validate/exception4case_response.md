{
  "test_cases": [
    {
      "group": "Null schemaNode – NullPointerException when accessing discriminator",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=true, discriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "nullNode()"
            },
            {
              "parameter": "rootNode",
              "value": "nullNode()"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.empty()"
            }
          ],
          "expected": "java.lang.NullPointerException",
          "description": "The JsonSchema instance is constructed with a null schemaNode. The method attempts to call schemaNode.get(\"discriminator\") which throws NPE before any other logic is executed."
        }
      ]
    },
    {
      "group": "Discriminator node is not an ObjectNode – ClassCastException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=true, discriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "nullNode()"
            },
            {
              "parameter": "rootNode",
              "value": "nullNode()"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.empty()"
            }
          ],
          "expected": "java.lang.ClassCastException",
          "description": "The schemaNode contains a \"discriminator\" field whose value is a JSON array (or text). The cast to ObjectNode in both discriminator blocks fails, resulting in a ClassCastException."
        }
      ]
    },
    {
      "group": "Discriminator object missing required \"propertyName\" – NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=true, discriminatorContext=mockDiscriminatorContext())"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\":\"cat\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\":\"cat\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.empty()"
            }
          ],
          "expected": "java.lang.NullPointerException",
          "description": "The schemaNode contains a valid ObjectNode discriminator but does not define the mandatory \"propertyName\" field. When the code executes discriminatorToUse.get(\"propertyName\").asText(), get returns null and asText() throws NPE."
        }
      ]
    },
    {
      "group": "ExecutionContext results null – NullPointerException when marking failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=false, validator=mockValidator(returnSet={errorMessage(\"msg\")}), results=null)"
            },
            {
              "parameter": "jsonNode",
              "value": "nullNode()"
            },
            {
              "parameter": "rootNode",
              "value": "nullNode()"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.empty()"
            }
          ],
          "expected": "java.lang.NullPointerException",
          "description": "A validator returns a non‑empty set, causing errors to be populated. After the loop the method attempts to call executionContext.getResults().setResult(...). Because getResults() returns null, a NPE is thrown."
        }
      ]
    },
    {
      "group": "Validator throws unchecked exception and finally block still processes results",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=false, validator=mockValidator(throwsException=IllegalStateException(\"boom\"), returnSet={errorMessage(\"msg2\")}))"
            },
            {
              "parameter": "jsonNode",
              "value": "nullNode()"
            },
            {
              "parameter": "rootNode",
              "value": "nullNode()"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.empty()"
            }
          ],
          "expected": "java.lang.IllegalStateException",
          "description": "The validator throws an unchecked exception. The finally block must still execute, adding any non‑null results (the mock returns a set with one message) to the errors collection before the exception propagates."
        }
      ]
    }
  ]
}