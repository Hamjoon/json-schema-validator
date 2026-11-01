{
  "testCases": [
    {
      "group": "Non‑object rootNode (array) – type mismatch handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "[{\"age\":30}]"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat (returns List<ValidationMessage>)"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op Consumer"
            }
          ],
          "expected": "A List containing at least one ValidationMessage indicating that the instance is not an object (type mismatch)",
          "description": "The schema expects an object with properties; providing a JSON array should trigger a type‑mismatch validation message. The custom OutputFormat should collect that message."
        }
      ]
    },
    {
      "group": "Empty object – required property violations",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat (returns List<ValidationMessage>)"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op Consumer"
            }
          ],
          "expected": "A List containing two ValidationMessage objects (missing required 'age' and missing required 'name')",
          "description": "When the instance is an empty object, the validator must report each required property that is absent."
        }
      ]
    },
    {
      "group": "Additional properties not allowed – unexpected field detection",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"age\":30,\"name\":\"Bob\",\"extra\":\"value\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat (returns List<ValidationMessage>)"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op Consumer"
            }
          ],
          "expected": "A List containing one ValidationMessage indicating that property 'extra' is not allowed",
          "description": "Assuming the schema has \"additionalProperties\": false, an unexpected property should be reported as a validation error."
        }
      ]
    },
    {
      "group": "Null Consumer handling – fail‑fast behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"age\":25,\"name\":\"Eve\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "If the Consumer argument is null the method should throw a NullPointerException before performing any validation."
        }
      ]
    },
    {
      "group": "Custom OutputFormat – returns transformed result",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"age\":30,\"name\":\"Alice\"}"
            },
            {
              "parameter": "format",
              "value": "CountingOutputFormat (returns Integer – number of messages)"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op Consumer"
            }
          ],
          "expected": "Integer value 0",
          "description": "A custom OutputFormat that counts validation messages should receive an empty list for a valid instance and therefore return 0."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"age\":\"thirty\",\"name\":null}"
            },
            {
              "parameter": "format",
              "value": "CountingOutputFormat (returns Integer – number of messages)"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op Consumer"
            }
          ],
          "expected": "Integer value >= 2",
          "description": "When the instance violates the schema, the CountingOutputFormat should return the number of validation messages (at least two in this case)."
        }
      ]
    },
    {
      "group": "OutputFormat throws unchecked exception – propagation test",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"age\":30,\"name\":\"Alice\"}"
            },
            {
              "parameter": "format",
              "value": "ExceptionThrowingOutputFormat (throws IllegalArgumentException)"
            },
            {
              "parameter": "executionCustomizer",
              "value": "no‑op Consumer"
            }
          ],
          "expected": "IllegalArgumentException propagated to the caller",
          "description": "If the OutputFormat implementation throws an unchecked exception while processing the validation messages, the validate method must not catch it; the exception should propagate unchanged."
        }
      ]
    },
    {
      "group": "ExecutionContext customization – fail‑fast mode",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"age\":\"thirty\",\"name\":\"Bob\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            },
            {
              "parameter": "executionCustomizer",
              "value": "Consumer that sets ExecutionContext.setFailFast(true)"
            }
          ],
          "expected": "A List containing exactly one ValidationMessage (validation stops after first error)",
          "description": "When the Consumer configures the ExecutionContext to fail fast, the validator should stop after the first detected error, resulting in a single ValidationMessage."
        }
      ]
    }
  ]
}