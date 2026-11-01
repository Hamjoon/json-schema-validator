Invalid control character at: line 122 column 67 (char 4937)
{
  "testCases": [
    {
      "group": "No discriminator processing (condition false)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "DiscriminatorContext stub where getDiscriminatorForPath returns null"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (empty)"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema stub with schemaNode {} (no \"discriminator\" field)"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath \"/\""
            }
          ],
          "expected": "No exception thrown; currentDiscriminatorContext.registerDiscriminator is called with the original empty discriminator object",
          "description": "When the schema does not contain a \"discriminator\" field or the context does not have a discriminator for the schema location, the method should skip all inner logic and simply register the (unchanged) discriminator."
        }
      ]
    },
    {
      "group": "Discriminator propertyName redefinition exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "DiscriminatorContext stub where getDiscriminatorForPath returns a non‑null ObjectNode"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (empty)"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema stub with schemaNode {\"discriminator\":{\"propertyName\":\"type\"}}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath \"/items\""
            }
          ],
          "expected": "JsonSchemaException thrown with message containing \"attempts redefining the discriminator property\"",
          "description": "If the discriminator object on the schema defines a \"propertyName\", the method must reject it because the root discriminator already defines the property."
        }
      ]
    },
    {
      "group": "Root mapping added when context mapping is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "DiscriminatorContext stub where getDiscriminatorForPath returns a non‑null ObjectNode"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {} (no \"mapping\" field)"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema stub with schemaNode {\"discriminator\":{\"mapping\":{\"A\":\"#/components/schemas/A\"}}}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath \"/\""
            }
          ],
          "expected": "discriminator now contains a \"mapping\" field equal to the schema's discriminator mapping; no exception thrown",
          "description": "When the root discriminator has no mapping but the nested schema provides one, the method should copy the nested mapping to the root discriminator."
        }
      ]
    },
    {
      "group": "Merge mapping without conflict (both mappings present, distinct keys)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "DiscriminatorContext stub where getDiscriminatorForPath returns a non‑null ObjectNode"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema stub with schemaNode {\"discriminator\":{\"mapping\":{\"B\":\"#/components/schemas/B\"}}}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath \"/\""
            }
          ],
          "expected": "discriminator.mapping now contains both entries A and B; no exception thrown",
          "description": "When both the context discriminator and the schema discriminator have a mapping object, the method should merge them, adding any keys that are not already present."
        }
      ]
    },
    {
      "group": "Merge mapping with existing identical value (no conflict)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "DiscriminatorContext stub where getDiscriminatorForPath returns a non‑null ObjectNode"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":<node1>}}   // <node1> is a JsonNode instance"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema stub with schemaNode {\"discriminator\":{\"mapping\":{\"A\":<node1>}}}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath \"/\""
            }
          ],
          "expected": "discriminator.mapping remains unchanged (still contains only key A with <node1>); no exception thrown",
          "description": "If the same key exists in both mappings and points to the exact same JsonNode instance, the method should treat it as a no‑op and not raise an error."
        }
      ]
    },
    {
      "group": "Merge mapping with conflict (different values for same key)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "DiscriminatorContext stub where getDiscriminatorForPath returns a non‑null ObjectNode"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema stub with schemaNode {\"discriminator\":{\"mapping\":{\"A\":\"#/components/schemas/OtherA\"}}}"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath \"/\""
            }
          ],
          "expected": "JsonSchemaException thrown with message containing \"discriminator mapping redefinition\" and the conflicting key A",
          "description": "When merging mappings, if a key already exists in the context mapping but the associated value differs from the one in the schema mapping, the method must reject the redefinition."
        }
      ]
    },
    {
      "group": "No mapping on current schema (both mappings null)",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "DiscriminatorContext stub where getDiscriminatorForPath returns a non‑null ObjectNode"
            },
            {
              "parameter": "discriminator",
              "value": "ObjectNode {\"mapping\":{\"A\":\"#/components/schemas/A\"}}"
            },
            {
              "parameter": "schema",
              "value": "JsonSchema stub with schemaNode {\"discriminator\":{}}   // no \"mapping\" field"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath \"/\""
            }
          ],
          "expected": "discriminator.mapping remains unchanged (only key A); no exception thrown",
          "description": "If the schema discriminator does not contain a \"mapping\" field, the method should leave the existing context mapping untouched."
        }
      ]
    }
  ]
}