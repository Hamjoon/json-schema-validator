{
  "testCases": [
    {
      "group": "Successful validation (no validators triggered)",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"name\": \"John\", \"age\": 30 }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == true and ValidationResult.getMessages() is empty",
          "description": "A well‑formed JSON node that satisfies all validators (no required, type or discriminator constraints). The method should walk the schema, collect no errors and return a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Null node handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "null"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message indicating that the instance is null",
          "description": "When the supplied JsonNode is null the method should treat it as a missing instance, record an appropriate ValidationMessage and return a failing ValidationResult."
        }
      ]
    },
    {
      "group": "Required validator failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"age\": 25 }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"required\" for missing property \"name\"",
          "description": "The schema contains a RequiredValidator for property \"name\". The supplied node does not contain this property, so the method must invoke the required validator, add a required‑error message and return a failing ValidationResult."
        }
      ]
    },
    {
      "group": "Type validator failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"name\": 12345 }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message with type = \"type\" indicating expected type \"string\" but found \"integer\"",
          "description": "The schema defines a TypeValidator that expects the property \"name\" to be a string. The node provides an integer, therefore the method must call the type validator, record a type‑mismatch message and return a failing ValidationResult."
        }
      ]
    },
    {
      "group": "Dynamic anchor handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"id\": \"#myAnchor\", \"value\": \"test\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == true and the dynamic anchor '#myAnchor' is registered in the ExecutionContext",
          "description": "The schema is marked as a dynamic anchor (isDynamicAnchor() returns true). When validateAndCollect is invoked the method should store the anchor URI in the ExecutionContext without producing validation errors."
        }
      ]
    },
    {
      "group": "Keyword walk listener execution",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"name\": \"Alice\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == true and the configured WalkListenerRunner's onWalkStart/onWalkEnd callbacks are invoked exactly once",
          "description": "The JsonSchema instance has a non‑null keywordWalkListenerRunner. During validateAndCollect the method must delegate to the runner before and after walking the schema, ensuring the listener callbacks are executed."
        }
      ]
    },
    {
      "group": "Switch‑case branch for $ref handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"$ref\": \"#/components/schemas/Address\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == true and the referenced schema is resolved and validated",
          "description": "When the schema contains a $ref keyword the method enters the switch‑case that resolves the reference via getRefSchemaNode(). The test verifies that the referenced schema is correctly retrieved, validated and that no extra errors are produced."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"$ref\": \"#/nonexistent\" }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == false and contains a message indicating unresolved reference",
          "description": "If the $ref cannot be resolved, the switch‑case should add an appropriate error message and return a failing ValidationResult."
        }
      ]
    },
    {
      "group": "If‑else branch for atRoot() flag",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"root\": true }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == true and the method treats the node as the root (atRoot() == true)",
          "description": "When the call originates from the public validateAndCollect(JsonNode) overload, atRoot() returns true. The method must follow the branch that sets the initial path to \"$\" and validates accordingly."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"nested\": { \"value\": 1 } }"
            }
          ],
          "expected": "ValidationResult.isSuccess() == true and the method processes the node as a non‑root child schema (atRoot() == false)",
          "description": "When validateAndCollect is invoked internally (e.g., from a child schema), atRoot() returns false, causing the method to take the else branch that prefixes the path with the parent location."
        }
      ]
    }
  ]
}