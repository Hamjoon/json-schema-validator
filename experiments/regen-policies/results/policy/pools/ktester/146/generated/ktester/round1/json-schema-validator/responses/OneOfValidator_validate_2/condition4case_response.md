{
  "groups": [
    {
      "group": "No discriminator – exactly one schema validates (happy path)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=true, discriminator disabled)"
            },
            {
              "parameter": "node",
              "value": "JSON node that satisfies schema[0] and fails schema[1]"
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
          "expected": "empty ValidationMessage set (no errors) – numberOfValidSchema == 1, errors stays null",
          "description": "When discriminator is disabled and exactly one of the supplied schemas validates, OneOfValidator must return an empty error set."
        }
      ]
    },
    {
      "group": "No discriminator – no schema validates",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator disabled)"
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
              "value": "true"
            }
          ],
          "expected": "ValidationMessage set containing a single message with key \"oneOf\" and argument \"0\" (no valid schemas)",
          "description": "When none of the schemas validate, OneOfValidator must produce a oneOf error with numberOfValidSchema == 0."
        }
      ]
    },
    {
      "group": "No discriminator – multiple schemas validate, short‑circuit allowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=true, discriminator disabled)"
            },
            {
              "parameter": "node",
              "value": "JSON node that satisfies schema[0] and schema[1]"
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
          "expected": "ValidationMessage set containing a single message with key \"oneOf.indexes\", arguments \"2\" and \"0, 1\" (two valid schemas). Child errors may be present if schemas produce them, but the main message must be oneOf.indexes.",
          "description": "When more than one schema validates and canShortCircuit() returns true, the loop breaks after the second match; the validator must still report a oneOf.indexes error because numberOfValidSchema != 1."
        }
      ]
    },
    {
      "group": "No discriminator – multiple schemas validate, short‑circuit NOT allowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator disabled)"
            },
            {
              "parameter": "node",
              "value": "JSON node that satisfies three schemas"
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
          "expected": "ValidationMessage set containing a single message with key \"oneOf.indexes\", arguments \"3\" and \"0, 1, 2\" (three valid schemas). Loop iterates over all schemas because canShortCircuit() returns false.",
          "description": "Ensures the validator does not break early when short‑circuiting is disabled and still reports a oneOf.indexes error."
        }
      ]
    },
    {
      "group": "Discriminator enabled – matching discriminator found",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=true, discriminator enabled, DiscriminatorContext active)"
            },
            {
              "parameter": "node",
              "value": "JSON node with property \"type\":\"A\" (discriminator property present)"
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
          "expected": "empty ValidationMessage set if the schema referenced by \"$ref\" matches the discriminator value; otherwise a oneOf error as in the non‑discriminator case.",
          "description": "When discriminator is enabled and the node contains the discriminator property that maps to a $ref of the currently evaluated schema, the validator marks a match, collects child errors only from the matching schema and ultimately returns no error if that schema validates."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator property missing (ignore path)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=false, discriminator enabled, DiscriminatorContext active)"
            },
            {
              "parameter": "node",
              "value": "JSON node without the discriminator property"
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
          "expected": "ValidationMessage set containing a oneOf error (same rules as without discriminator) because the context is marked as ignore and childErrors are aggregated from all schemas.",
          "description": "If the discriminator property is absent, the context is marked ignore; the validator must fall back to normal handling and report a oneOf error when the number of valid schemas is not exactly one."
        }
      ]
    },
    {
      "group": "Discriminator enabled – no matching schema and not ignored",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock ExecutionContext (failFast=true, discriminator enabled, DiscriminatorContext active)"
            },
            {
              "parameter": "node",
              "value": "JSON node with discriminator property \"type\":\"UNKNOWN\" that does not match any $ref"
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
          "expected": "ValidationMessage set containing a single message built from message().instanceNode(...).arguments(\"based on the provided discriminator. No alternative could be chosen based on the discriminator property\")",
          "description": "When discriminator is enabled, a discriminator validator exists, but none of the schemas match the discriminator value, the validator must produce the special error indicating that no alternative could be chosen."
        }
      ]
    }
  ]
}