{
  "tests": [
    {
      "group": "No discriminator – single valid schema (happy path)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock ExecutionContext with failFast=true, discriminator disabled, reportChildErrors=false, canShortCircuit=true" },
            { "parameter": "node", "value": "valid JsonNode (e.g., {\"a\":1})" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "walk", "value": "false" }
          ],
          "expected": "empty Set – no ValidationMessage (numberOfValidSchema == 1, errors == null)",
          "description": "Discriminator keyword is disabled, only one schema is supplied and its validate() returns an empty set, so the OneOfValidator should report success."
        }
      ]
    },
    {
      "group": "No discriminator – multiple valid schemas, short‑circuit enabled",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock ExecutionContext with failFast=false, discriminator disabled, reportChildErrors=false, canShortCircuit=true" },
            { "parameter": "node", "value": "valid JsonNode matching both schemas" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "walk", "value": "false" }
          ],
          "expected": "Set containing one ValidationMessage with messageKey \"oneOf.indexes\" and arguments \"2, 0, 1\" (two valid schemas, short‑circuit after the second).",
          "description": "Two schemas validate successfully. canShortCircuit() returns true, so the loop breaks after the second valid schema. The validator must return a OneOf error reporting the indexes of the two matching schemas."
        }
      ]
    },
    {
      "group": "No discriminator – multiple valid schemas, short‑circuit disabled",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock ExecutionContext with failFast=false, discriminator disabled, reportChildErrors=false, canShortCircuit=false" },
            { "parameter": "node", "value": "valid JsonNode matching all three schemas" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "walk", "value": "false" }
          ],
          "expected": "Set containing one ValidationMessage with messageKey \"oneOf.indexes\" and arguments \"3, 0, 1, 2\" (all three schemas are valid).",
          "description": "Three schemas are valid, canShortCircuit() returns false, therefore the loop iterates over all schemas and the validator reports a OneOf error listing all three matching indexes."
        }
      ]
    },
    {
      "group": "No discriminator – no schema matches",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock ExecutionContext with failFast=true, discriminator disabled, reportChildErrors=true, canShortCircuit=true" },
            { "parameter": "node", "value": "invalid JsonNode (e.g., {\"a\":\"string\"})" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "walk", "value": "false" }
          ],
          "expected": "Set containing one ValidationMessage with messageKey \"oneOf\" and argument \"0\" (zero valid schemas). Child errors from each schema are also present in the returned set.",
          "description": "All schemas return non‑empty error sets. reportChildErrors() is true, so child errors are collected. Since numberOfValidSchema == 0, a OneOf error with count 0 is produced together with the child errors."
        }
      ]
    },
    {
      "group": "Discriminator enabled – matching discriminator property",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock ExecutionContext with failFast=false, discriminator enabled, canShortCircuit=true, reportChildErrors=false, DiscriminatorContext active" },
            { "parameter": "node", "value": "JsonNode {\"type\":\"A\"}" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "walk", "value": "false" }
          ],
          "expected": "empty Set – validation succeeds (discriminator matches $ref of the only valid schema, numberOfValidSchema == 1, no errors).",
          "description": "Discriminator keyword is enabled. The parent schema contains a discriminator validator with propertyName \"type\" and mapping {\"A\":\"#/components/schemas/A\"}. The node contains \"type\":\"A\" which matches the $ref of the first schema, causing the discriminator context to mark a match. Because a match is found, no discriminator‑related error is emitted and the OneOf rule is satisfied."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator present but no matching $ref",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock ExecutionContext with failFast=false, discriminator enabled, canShortCircuit=true, reportChildErrors=false, DiscriminatorContext active" },
            { "parameter": "node", "value": "JsonNode {\"type\":\"B\"}" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "walk", "value": "false" }
          ],
          "expected": "Set containing one ValidationMessage with messageKey \"oneOf\" and argument \"0\" AND a discriminator‑specific message: \"based on the provided discriminator. No alternative could be chosen based on the discriminator property\".",
          "description": "Discriminator is enabled, the node contains the discriminator property but its value does not correspond to any $ref in the candidate schemas. The discriminator context does not mark a match, so after the loop the validator adds the special discriminator error and then the generic OneOf error (since numberOfValidSchema == 0)."
        }
      ]
    },
    {
      "group": "Discriminator enabled – discriminator property missing (ignore path)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock ExecutionContext with failFast=false, discriminator enabled, canShortCircuit=true, reportChildErrors=false, DiscriminatorContext active" },
            { "parameter": "node", "value": "JsonNode {} (no discriminator property)" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "walk", "value": "false" }
          ],
          "expected": "Set containing one ValidationMessage with messageKey \"oneOf\" and argument \"0\" (no valid schemas). No discriminator‑specific error because the context is marked as ignore.",
          "description": "The discriminator property is absent; the discriminator context marks IGNORE. Consequently the validator does not emit the special discriminator‑no‑match message. Since no schemas validate, a normal OneOf error with count 0 is returned."
        }
      ]
    },
    {
      "group": "Walk flag true – schema validation via walk()",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock ExecutionContext with failFast=false, discriminator disabled, reportChildErrors=false, canShortCircuit=true" },
            { "parameter": "node", "value": "valid JsonNode that passes walk() of first schema only" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "walk", "value": "true" }
          ],
          "expected": "empty Set – one schema validates via walk(), numberOfValidSchema == 1, no errors.",
          "description": "When the walk flag is true the validator calls schema.walk() instead of schema.validate(). The mock schema returns an empty set, leading to a successful OneOf validation."
        }
      ]
    },
    {
      "group": "Discriminator disabled – child errors collected when reportChildErrors true",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mock ExecutionContext with failFast=false, discriminator disabled, reportChildErrors=true, canShortCircuit=false" },
            { "parameter": "node", "value": "invalid JsonNode (fails both schemas)" },
            { "parameter": "rootNode", "value": "same as node" },
            { "parameter": "instanceLocation", "value": "JsonNodePath.root()" },
            { "parameter": "walk", "value": "false" }
          ],
          "expected": "Set containing one ValidationMessage with messageKey \"oneOf\" (count 0) and also the child ValidationMessages from both schemas (unioned).",
          "description": "Discriminator is off, but reportChildErrors() returns true, so the validator must collect the error sets from each failing schema and include them together with the final OneOf error."
        }
      ]
    }
  ]
}