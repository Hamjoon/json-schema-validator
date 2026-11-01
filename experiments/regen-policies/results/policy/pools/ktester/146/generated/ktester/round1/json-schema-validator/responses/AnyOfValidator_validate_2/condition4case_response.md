{
  "testCases": [
    {
      "group": "Discriminator Disabled - Simple Walk=False, All Schemas Invalid",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=true, discriminator disabled, no discriminator context"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing a string value \"foo\""
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath root"
            },
            {
              "parameter": "walk",
              "value": false
            }
          ],
          "expected": "Set containing validation messages from each schema (allErrors) because none passed and short‑circuit is false",
          "description": "Discriminator is disabled, walk is false, each schema is evaluated via validate(). All schemas return non‑empty error sets, so numberOfValidSubSchemas stays 0 and the method returns the accumulated allErrors."
        }
      ]
    },
    {
      "group": "Discriminator Disabled - Short‑Circuit Enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=false, discriminator disabled"
            },
            {
              "parameter": "node",
              "value": "JsonNode of type integer 5"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath root"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "empty Set (short‑circuit returns immediately after first schema validates successfully)",
          "description": "First schema validates without errors, canShortCircuit() returns true, canShortCircuit(executionContext) returns true, discriminator disabled, so the method clears allErrors and returns the empty error set from the first schema."
        }
      ]
    },
    {
      "group": "Discriminator Disabled - Valid Subschema After Several Failures",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with failFast=true, discriminator disabled"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing an object {\"type\":\"A\"}"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath root"
            },
            {
              "parameter": "walk",
              "value": false
            }
          ],
          "expected": "empty Set (because numberOfValidSubSchemas >= 1)",
          "description": "First two schemas return errors, third schema validates successfully (errors.isEmpty()). No short‑circuit because canShortCircuit() is false. After the loop numberOfValidSubSchemas is 1, so the method returns Collections.emptySet()."
        }
      ]
    },
    {
      "group": "Discriminator Enabled - Match Found with Errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator enabled, current DiscriminatorContext.isDiscriminatorMatchFound() = true"
            },
            {
              "parameter": "node",
              "value": "JsonNode of any type"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath root"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "Set containing a single ValidationMessage with arguments = \"and the discriminator‑selected candidate schema didn't pass validation\"",
          "description": "Discriminator is enabled and a match has been found. The evaluated schema returns a non‑empty error set, so the validator adds the special discriminator remark message to allErrors (if needed) and returns the original errors set immediately."
        }
      ]
    },
    {
      "group": "Discriminator Enabled - Match Found without Errors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator enabled, DiscriminatorContext.isDiscriminatorMatchFound() = true"
            },
            {
              "parameter": "node",
              "value": "JsonNode of any type"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath root"
            },
            {
              "parameter": "walk",
              "value": false
            }
          ],
          "expected": "empty Set (no errors, discriminator match found)",
          "description": "Discriminator enabled, match found, and the evaluated schema validates with no errors. The validator clears allErrors and returns the empty error set."
        }
      ]
    },
    {
      "group": "Discriminator Enabled - Ignore Flag",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator enabled, DiscriminatorContext.isDiscriminatorIgnore() = true"
            },
            {
              "parameter": "node",
              "value": "JsonNode of any type"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath root"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "Set containing a single ValidationMessage with arguments = \"and the discriminator‑selected candidate schema didn't pass validation\" (if errors non‑empty) or empty Set (if errors empty)",
          "description": "When discriminator is enabled and the context is set to ignore, the validator follows the same early‑return logic as the match‑found branch, adding the discriminator remark only when errors are present."
        }
      ]
    },
    {
      "group": "Discriminator Enabled - No Match After Loop",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator enabled, DiscriminatorContext active but neither match nor ignore flags set"
            },
            {
              "parameter": "node",
              "value": "JsonNode representing an object without discriminator property"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath root"
            },
            {
              "parameter": "walk",
              "value": false
            }
          ],
          "expected": "Set with a single ValidationMessage whose arguments contain \"based on the provided discriminator. No alternative could be chosen based on the discriminator property\"",
          "description": "After evaluating all subschemas, discriminator is enabled and the context is still active but no match/ignore occurred. The method returns the specific message indicating that no alternative could be chosen."
        }
      ]
    },
    {
      "group": "TypeValidator Mismatch (Non‑UNION)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator disabled"
            },
            {
              "parameter": "node",
              "value": "JsonNode of type boolean true"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath root"
            },
            {
              "parameter": "walk",
              "value": true
            }
          ],
          "expected": "Set containing the ValidationMessage produced by the TypeValidator (type mismatch) for the schema that has a non‑UNION TypeValidator",
          "description": "The schema's TypeValidator reports a concrete type (e.g., STRING) that does not match the node type (BOOLEAN). Because the schema type is not UNION, the validator skips further validation of that schema and adds the type‑validator errors to allErrors."
        }
      ]
    },
    {
      "group": "TypeValidator UNION (always evaluated)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with discriminator disabled"
            },
            {
              "parameter": "node",
              "value": "JsonNode of type integer 10"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath root"
            },
            {
              "parameter": "walk",
              "value": false
            }
          ],
          "expected": "Result depends on the underlying schema validation; the TypeValidator is called but does not cause a continue because schema type is UNION.",
          "description": "When the schema's TypeValidator reports UNION, the validator does **not** skip the schema even if the node type does not match a specific type, ensuring the schema is still validated."
        }
      ]
    },
    {
      "group": "Fail‑Fast Flag Restoration",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext with initial failFast=true, discriminator disabled"
            },
            {
              "parameter": "node",
              "value": "JsonNode of any type"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath root"
            },
            {
              "parameter": "walk",
              "value": false
            }
          ],
          "expected": "After validate() returns, executionContext.isFailFast() == true (original value restored)",
          "description": "The method temporarily sets failFast to false for nested schema evaluation and must restore the original value in the finally block."
        }
      ]
    }
  ]
}