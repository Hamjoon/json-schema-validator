{
  "testCases": [
    {
      "group": "Discriminator enabled – mapping overrides discriminator value",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator enabled, DiscriminatorContext active)"
            },
            {
              "parameter": "node",
              "value": "JSON node with discriminator property \"type\":\"A\" (mapping contains \"A\" -> \"B\")"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "empty ValidationMessage set because the discriminator mapping changes the value from \"A\" to \"B\" and the schema being evaluated has a $ref of \"B\", thus a match is recorded and the matched schema validates.",
          "description": "When a discriminator is enabled and the discriminator property value is remapped via the discriminator's mapping, the validator must use the mapped value to compare against the schema's $ref. If they match and the schema validates, the result should be success (no errors)."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator present but schema lacks $ref",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=true, discriminator enabled, DiscriminatorContext active)"
            },
            {
              "parameter": "node",
              "value": "JSON node with discriminator property \"type\":\"A\""
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "true"
            }
          ],
          "expected": "ValidationMessage set containing a single message with key \"oneOf\" and argument \"0\" (no valid schemas). The lack of $ref means the discriminator cannot mark a match, so normal one‑of logic applies.",
          "description": "If the discriminator property is present but the candidate schema does not contain a $ref, the discriminator matching logic cannot succeed. The validator should fall back to the regular oneOf evaluation and report an error when no schema validates."
        }
      ]
    },
    {
      "group": "Discriminator enabled – matching discriminator but schema validation fails",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator enabled, DiscriminatorContext active)"
            },
            {
              "parameter": "node",
              "value": "JSON node with discriminator property \"type\":\"A\" that maps to a schema whose internal validation fails"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "ValidationMessage set containing a single message with key \"oneOf\" and argument \"0\" plus the child errors produced by the failing schema. Because a discriminator match was found but the matched schema is invalid, the validator must report the normal oneOf error together with the child errors.",
          "description": "When a discriminator match is identified but the matched schema itself produces validation errors, the validator should not return the special \"no alternative could be chosen\" message. Instead it should behave like a normal oneOf failure, reporting the oneOf error and the child errors from the matched schema."
        }
      ]
    },
    {
      "group": "Discriminator enabled – exactly one schema validates (happy path with discriminator)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=true, discriminator enabled, DiscriminatorContext active)"
            },
            {
              "parameter": "node",
              "value": "JSON node with discriminator property \"type\":\"B\" that matches the $ref of schema[1] and satisfies that schema"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "empty ValidationMessage set (no errors) – numberOfValidSchema == 1, errors stays null, childErrors contain only errors from the matched schema (which are none).",
          "description": "The typical successful path when discriminators are enabled: the node contains the discriminator property, it maps to a $ref that points to exactly one schema, and that schema validates. The validator should return an empty set."
        }
      ]
    },
    {
      "group": "Discriminator enabled – multiple schemas validate (oneOf.indexes error despite match)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator enabled, DiscriminatorContext active, canShortCircuit=false)"
            },
            {
              "parameter": "node",
              "value": "JSON node with discriminator property \"type\":\"C\" that matches schema[0] $ref, but the node also satisfies schema[1] and schema[2]"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "true"
            }
          ],
          "expected": "ValidationMessage set containing a single message with key \"oneOf.indexes\", arguments \"3\" and \"0, 1, 2\" (three valid schemas). The presence of a discriminator match does not suppress the oneOf.indexes error because more than one schema validates.",
          "description": "Even when a discriminator match is found, the oneOf rule still requires exactly one matching schema. If several schemas validate, the validator must report a oneOf.indexes error listing all matching indexes."
        }
      ]
    },
    {
      "group": "ExecutionContext failFast flag restoration",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (initial failFast=true, discriminator disabled)"
            },
            {
              "parameter": "node",
              "value": "JSON node that fails all schemas"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "false"
            }
          ],
          "expected": "After validate() returns, executionContext.isFailFast() == true (original value restored). The method returns a ValidationMessage set with key \"oneOf\" and argument \"0\".",
          "description": "The validator temporarily sets failFast to false while iterating over child schemas. This test verifies that the original failFast flag is restored in the finally block, regardless of validation outcome."
        }
      ]
    },
    {
      "group": "Walk flag true with discriminator enabled – matching discriminator",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator enabled, DiscriminatorContext active)"
            },
            {
              "parameter": "node",
              "value": "JSON node with discriminator property \"type\":\"D\" that matches $ref of schema[0] and the schema validates only when walk() is used"
            },
            {
              "parameter": "rootNode",
              "value": "same as node"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "walk",
              "value": "true"
            }
          ],
          "expected": "empty ValidationMessage set – the validator called schema.walk() for each child schema, the discriminator matched, and the matched schema produced no errors.",
          "description": "Ensures that when walk == true the validator invokes the walk() method on child schemas, and that discriminator handling works identically for the walk path."
        }
      ]
    }
  ]
}