{
  "testCases": [
    {
      "group": "Null and Empty Inputs",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling validate with a null JsonNode and a null customizer should throw NPE because the method expects a non‑null root node."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {} (empty JSON object)"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "empty set",
          "description": "Validate an empty JSON object with no execution customizer. The schema (assumed to have no required properties) should produce no validation messages."
        }
      ]
    },
    {
      "group": "Valid JSON Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op lambda () -> {}"
            }
          ],
          "expected": "empty set",
          "description": "A JSON document that fully complies with the schema (e.g., required string and integer fields) should return an empty ValidationMessage set."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\",\"age\":30,\"extra\":\"value\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "lambda that sets ExecutionContext.setValidateSchema(false)"
            }
          ],
          "expected": "empty set",
          "description": "When the customizer disables schema validation, even a document containing unknown properties should return no validation messages."
        }
      ]
    },
    {
      "group": "Invalid JSON Validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":123,\"age\":\"thirty\"}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op lambda"
            }
          ],
          "expected": "set containing messages: ['type mismatch for property name', 'type mismatch for property age']",
          "description": "A JSON document with wrong types for required fields should produce ValidationMessages for each type error."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"age\":30}" 
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op lambda"
            }
          ],
          "expected": "set containing message: ['required property name is missing']",
          "description": "Missing a required property should generate a single required‑property ValidationMessage."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer Effects",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "lambda that adds a custom attribute to ExecutionContext (e.g., ctx.setAttribute(\"test\", true))"
            }
          ],
          "expected": "empty set",
          "description": "A customizer that mutates the ExecutionContext but does not alter validation behavior should still return a successful validation result."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "lambda that throws RuntimeException(\"customizer failure\")"
            }
          ],
          "expected": "RuntimeException with message 'customizer failure'",
          "description": "If the consumer throws an exception, validate should propagate that exception."
        }
      ]
    },
    {
      "group": "Branch Coverage for Internal Flags",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "lambda that sets ExecutionContext.setRecursiveAnchor(true)"
            }
          ],
          "expected": "empty set",
          "description": "When the customizer enables the recursive‑anchor flag, the validator should still succeed for a conforming document, covering the branch that checks `isRecursiveAnchor()`."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "lambda that sets ExecutionContext.setSchemaResourceRoot(false)"
            }
          ],
          "expected": "empty set",
          "description": "Disabling the schema‑resource‑root flag exercises the branch that decides whether to treat the current schema as a resource root."
        }
      ]
    }
  ]
}