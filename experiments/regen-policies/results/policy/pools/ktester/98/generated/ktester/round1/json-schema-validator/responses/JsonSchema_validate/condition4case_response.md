{
  "testCases": [
    {
      "group": "Discriminator keyword disabled – no discriminator handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=false)"
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
          "expected": "No interaction with DiscriminatorContext; errors set is empty; executionContext.results.setResult is NOT called.",
          "description": "When the configuration disables the discriminator keyword, both discriminator blocks are skipped regardless of schema content."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator absent in schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=true, discriminatorContext=null)"
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
          "expected": "Both discriminator blocks find null discriminator – no registration, no checkDiscriminatorMatch, errors empty.",
          "description": "Schema does not contain a \"discriminator\" property; the validator must safely ignore it."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator present, but ExecutionContext has no DiscriminatorContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=true, discriminatorContext=null)"
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
          "expected": "First discriminator block does not register (null context). Second block skips because context is null. No errors produced.",
          "description": "Ensures that a missing DiscriminatorContext prevents any discriminator processing."
        }
      ]
    },
    {
      "group": "Discriminator enabled – context present, discriminator not previously registered (null from context)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=true, discriminatorContext=mockDiscriminatorContext(returnFromPath=null))"
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
          "expected": "First block registers discriminator. Second block retrieves null from context, registers again, uses current discriminator, extracts propertyName, obtains value \"cat\", calls checkDiscriminatorMatch with that value. No errors unless validator adds them.",
          "description": "Tests the path where the discriminator is newly registered because it was not present in the context."
        }
      ]
    },
    {
      "group": "Discriminator enabled – context present, discriminator already registered (non‑null from context)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=true, discriminatorContext=mockDiscriminatorContext(returnFromPath=objectNode({\"propertyName\":\"type\"})))"
            },
            {
              "parameter": "jsonNode",
              "value": "objectNode({\"type\":\"dog\"})"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode({\"type\":\"dog\"})"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.empty()"
            }
          ],
          "expected": "First block registers discriminator. Second block finds existing discriminator from context, does NOT re‑register, uses the existing one, extracts propertyName \"type\", reads value \"dog\", invokes checkDiscriminatorMatch.",
          "description": "Ensures that when a discriminator is already stored in the context, it is reused."
        }
      ]
    },
    {
      "group": "Validator returns errors – errors collection populated",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=false, validator=mockValidator(returnSet={errorMessage(\"msg1\")}))"
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
          "expected": "errors set contains the returned ValidationMessage; executionContext.results.setResult called with false; method returns non‑empty set.",
          "description": "Verifies that when any validator produces messages, they are collected, the result is marked as failed, and the set is returned."
        }
      ]
    },
    {
      "group": "Validator returns empty set – no errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=false, validator=mockValidator(returnSet=emptySet()))"
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
          "expected": "errors remains null; executionContext.results.setResult NOT called; method returns emptySet.",
          "description": "Ensures that when validators produce no messages, the method returns an empty set and does not mark the instance as failed."
        }
      ]
    },
    {
      "group": "Validator throws exception – finally block still processes results",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(discriminatorKeywordEnabled=false, validator=mockValidator(throwsException=RuntimeException(\"boom\"), returnSet={errorMessage(\"msg2\")}))"
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
          "expected": "Exception propagates, but finally block adds any non‑null results to errors before propagation.",
          "description": "Confirms that the finally block executes even when a validator throws, preserving any partial results."
        }
      ]
    }
  ]
}