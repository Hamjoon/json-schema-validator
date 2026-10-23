{
  "testCases": [
    {
      "group": "Discriminator present but no registered context entry",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with empty internal map (no entry for \"/root\")"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"X\":\"#/components/schemas/X\"}}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{\"mapping\":{\"Y\":\"#/components/schemas/Y\"}}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "No exception; the provided discriminator is registered unchanged (still only contains mapping for \"X\").",
          "description": "When a discriminator node exists on the schema but the current DiscriminatorContext does not have a discriminator registered for the schema path, the method must skip all merging logic and simply register the original discriminator."
        }
      ]
    },
    {
      "group": "Both root and schema discriminators have no mapping fields",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (discriminator ObjectNode {} – no mapping field)"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (empty object)"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "No exception; discriminator remains empty after registration.",
          "description": "When both the root discriminator and the nested schema discriminator contain no \"mapping\" field, the method should perform no merge and simply register the unchanged discriminator."
        }
      ]
    },
    {
      "group": "Root mapping present, schema mapping absent",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (discriminator ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}})"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "No exception; discriminator mapping stays as {\"A\":\"#/components/schemas/A\"}.",
          "description": "If the nested schema discriminator has no \"mapping\" field while the root already defines a mapping, the method must leave the existing mapping untouched."
        }
      ]
    },
    {
      "group": "Merge with overlapping key (same value) plus new key",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (discriminator ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}})"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{\"mapping\":{\"A\":\"#/components/schemas/A\",\"B\":\"#/components/schemas/B\"}}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "No exception; after execution discriminator mapping contains both keys \"A\" (unchanged) and \"B\" (added).",
          "description": "When the nested schema provides a mapping that re‑uses an existing key with the identical value and also adds a new key, the method should merge without error, preserving the original entry and adding the new one."
        }
      ]
    },
    {
      "group": "Invalid discriminator object type in context (ClassCastException)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (discriminator is a plain JsonNode of type TextNode \"invalid\")"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{\"mapping\":{\"B\":\"#/components/schemas/B\"}}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "ClassCastException (or JsonSchemaException caused by cast) because the context discriminator is not an ObjectNode.",
          "description": "The method casts the existing discriminator from the context to ObjectNode without checking its type. Supplying a non‑ObjectNode should trigger a ClassCastException, confirming the lack of defensive type checking."
        }
      ]
    },
    {
      "group": "Discriminator on schema contains unrelated fields",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "MockDiscriminatorContext with entry for path \"/root\" (discriminator ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}})"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema with schemaNode {\"discriminator\":{\"mapping\":{\"B\":\"#/components/schemas/B\"},\"extraField\":\"ignored\"}} and schemaPath \"/root\""
            },
            {
              "parameter": "at",
              "value": "\"/root\""
            }
          ],
          "expected": "No exception; discriminator mapping after merge contains keys \"A\" and \"B\"; the unrelated field \"extraField\" is ignored.",
          "description": "The method should only consider the \"mapping\" node inside a discriminator. Additional fields must not affect merging logic or cause errors."
        }
      ]
    }
  ]
}