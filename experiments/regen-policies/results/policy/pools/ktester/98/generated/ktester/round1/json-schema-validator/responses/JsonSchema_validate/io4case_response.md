{
  "newTestCases": [
    {
      "group": "No validators – empty validator list",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=false, validators=emptyList())"
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
          "expected": "errors == emptySet; executionContext.results.setResult is NOT called",
          "description": "When the schema has no validators, the method should return an empty set and must not mark the instance as failed."
        }
      ]
    },
    {
      "group": "Validator returns null – result ignored",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=false, validator=mockValidator(returnSet=null))"
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
          "expected": "errors == emptySet; executionContext.results.setResult is NOT called",
          "description": "A validator that returns null should be treated as no messages; the method must continue without adding a null collection to the error set."
        }
      ]
    },
    {
      "group": "Multiple validators – first returns errors, second empty",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=false, validators=[mockValidator(returnSet={msg(\"first\"), msg(\"second\")}), mockValidator(returnSet=emptySet())])"
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
          "expected": "errors contains exactly two messages (\"first\", \"second\"); executionContext.results.setResult called once with false",
          "description": "Ensures that errors from the first validator are retained while an empty result from a later validator does not affect the already‑collected errors."
        }
      ]
    },
    {
      "group": "Multiple validators – both return errors, union performed",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=false, validators=[mockValidator(returnSet={msg(\"a\")}), mockValidator(returnSet={msg(\"b\"), msg(\"c\")})])"
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
          "expected": "errors contains three distinct messages (\"a\", \"b\", \"c\"); executionContext.results.setResult called once with false",
          "description": "Validates that the SetView correctly unions messages from several validators, preserving uniqueness."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator present but property missing in instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=true, discriminatorContext=mockDiscriminatorContext(returnFromPath=objectNode({\"propertyName\":\"type\"})))"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"name\":\"Rex\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"name\":\"Rex\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.empty()"
            }
          ],
          "expected": "First discriminator block registers discriminator; second block retrieves same discriminator; discriminatorNode == null; discriminatorPropertyValue == null; checkDiscriminatorMatch called with null value; no errors unless checkDiscriminatorMatch adds them; errors set remains empty; executionContext.results.setResult NOT called",
          "description": "When the instance does not contain the discriminator property, the validator must still invoke checkDiscriminatorMatch with a null value, allowing the discriminator logic to decide if this is an error."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator property present but value is null node",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=true, discriminatorContext=mockDiscriminatorContext(returnFromPath=objectNode({\"propertyName\":\"type\"})))"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\":null})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\":null})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.empty()"
            }
          ],
          "expected": "discriminatorNode is a NullNode; discriminatorPropertyValue == null; checkDiscriminatorMatch called with null; errors set empty unless checkDiscriminatorMatch adds messages; executionContext.results.setResult NOT called",
          "description": "Even when the discriminator field exists but its value is JSON null, the method should treat the extracted value as null and forward it to the discriminator matching logic."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator property value non‑textual (number)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=true, discriminatorContext=mockDiscriminatorContext(returnFromPath=objectNode({\"propertyName\":\"type\"})))"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\":123})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\":123})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.empty()"
            }
          ],
          "expected": "discriminatorNode is numeric; discriminatorPropertyValue == \"123\" (asText conversion); checkDiscriminatorMatch called with \"123\"; errors empty unless checkDiscriminatorMatch adds them; executionContext.results.setResult NOT called",
          "description": "Ensures that non‑string discriminator values are converted to text via JsonNode.asText() before being passed to the matching routine."
        }
      ]
    }
  ]
}