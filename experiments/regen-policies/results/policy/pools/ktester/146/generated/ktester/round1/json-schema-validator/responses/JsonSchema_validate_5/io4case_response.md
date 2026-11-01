{
  "testCases": [
    {
      "group": "Missing required property",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"age\":30}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage with messageId \"required\" and path \"\" (root) because the schema requires property \"name\" which is absent",
          "description": "The schema declares a required property \"name\". Supplying JSON that lacks this property should trigger the required‑property branch and produce a ValidationMessage."
        }
      ]
    },
    {
      "group": "Disallowed additional property",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"id\":1,\"extra\":\"value\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage with messageId \"additionalProperties\" and path \"/extra\"",
          "description": "The schema sets \"additionalProperties\": false. An unexpected property \"extra\" should be reported by the additional‑properties validator."
        }
      ]
    },
    {
      "group": "Enum constraint violation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"status\":\"UNKNOWN\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage with messageId \"enum\" and path \"/status\"",
          "description": "The schema defines an enum [\"ACTIVE\",\"INACTIVE\"]. Providing a value outside the enum exercises the enum‑validation branch."
        }
      ]
    },
    {
      "group": "Numeric boundary validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"score\":-1}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage with messageId \"minimum\" and path \"/score\" because the schema sets \"minimum\":0",
          "description": "The schema specifies a minimum of 0 for the numeric property \"score\". Supplying -1 should trigger the minimum validator."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"score\":100}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "Empty List – the value equals the exclusiveMaximum boundary (if exclusiveMaximum is 100) and therefore passes",
          "description": "The schema defines \"exclusiveMaximum\":100. Providing exactly 100 should be accepted, testing the exclusiveMaximum branch."
        }
      ]
    },
    {
      "group": "Array items validation failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ArrayNode representing [{\"email\":\"bad\"},{\"email\":\"good@example.com\"}]"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage with messageId \"format\" and path \"/0/email\" because the first array element violates the email format",
          "description": "The schema defines an array of objects where each object’s \"email\" must match the email format. The first element fails, exercising the validator that iterates over array items."
        }
      ]
    },
    {
      "group": "oneOf validator failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"value\":5}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage with messageId \"oneOf\" and path \"\" because the instance does not satisfy any of the subschemas in the oneOf array",
          "description": "The schema contains a oneOf with two subschemas (e.g., integer >=10 and string pattern). Supplying a value that matches none triggers the oneOf failure branch."
        }
      ]
    },
    {
      "group": "Conditional validator failure (if/then/else)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"type\":\"A\",\"code\":123}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage with messageId \"minimum\" and path \"/code\" because the \"then\" schema requires \"code\" >=200 when \"type\" is \"A\"",
          "description": "The schema uses an if/then clause: if \"type\" == \"A\" then \"code\" must be >=200. Supplying 123 violates the then‑branch, testing the conditional validation path."
        }
      ]
    },
    {
      "group": "OutputFormat exception propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"age\":30}"
            },
            {
              "parameter": "format",
              "value": "ThrowingOutputFormat (throws RuntimeException on any call to addMessage)"
            }
          ],
          "expected": "RuntimeException thrown from the OutputFormat, propagated out of validate method",
          "description": "A custom OutputFormat that throws an exception when a ValidationMessage is added ensures that validate does not swallow the exception and propagates it to the caller."
        }
      ]
    },
    {
      "group": "Root node type mismatch (array vs object)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ArrayNode representing []"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage with messageId \"type\" and path \"\" because the schema expects an object but the instance is an array",
          "description": "The schema’s top‑level type is \"object\". Supplying an array triggers the type‑validator branch for the root node."
        }
      ]
    },
    {
      "group": "PatternProperties validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing {\"prop_1\":\"value\",\"invalid\":\"value\"}"
            },
            {
              "parameter": "format",
              "value": "CollectingOutputFormat"
            }
          ],
          "expected": "List containing a ValidationMessage with messageId \"patternProperties\" and path \"/invalid\" because property name does not match the allowed pattern \"^prop_.*$\"",
          "description": "The schema defines \"patternProperties\" with a regex that only allows property names starting with \"prop_\". An unexpected property name should be flagged."
        }
      ]
    }
  ]
}