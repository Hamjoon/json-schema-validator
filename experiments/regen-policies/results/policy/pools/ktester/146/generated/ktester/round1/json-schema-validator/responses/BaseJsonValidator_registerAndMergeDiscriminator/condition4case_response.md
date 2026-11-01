{
  "testCases": [
    {
      "group": "No discriminator on schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "EmptyDiscriminatorContext"
            },
            {
              "parameter": "discriminator",
              "value": "{}"
            },
            {
              "parameter": "schema",
              "value": "{ \"schemaLocation\": \"/root\", \"schemaNode\": {} }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "Discriminator registered in context with empty mapping; no exception thrown.",
          "description": "When the schema does not contain a \"discriminator\" property, the method should skip all conditional logic and simply register the provided discriminator."
        }
      ]
    },
    {
      "group": "Discriminator present but no existing context entry",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "ContextWithoutEntry"
            },
            {
              "parameter": "discriminator",
              "value": "{}"
            },
            {
              "parameter": "schema",
              "value": "{ \"schemaLocation\": \"/root\", \"schemaNode\": { \"discriminator\": { \"propertyName\": \"type\" } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "Discriminator registered in context; no exception thrown.",
          "description": "Even though the schema defines a discriminator, because the context does not already have a discriminator for the schema location, the method should bypass the merging logic and just register the discriminator."
        }
      ]
    },
    {
      "group": "Redefine discriminator propertyName throws",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "ContextWithRootDiscriminator"
            },
            {
              "parameter": "discriminator",
              "value": "{ \"propertyName\": \"type\", \"mapping\": {} }"
            },
            {
              "parameter": "schema",
              "value": "{ \"schemaLocation\": \"/child\", \"schemaNode\": { \"discriminator\": { \"propertyName\": \"type\", \"mapping\": {} } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "JsonSchemaException with message containing \"attempts redefining the discriminator property\"",
          "description": "When a nested schema tries to redefine the discriminator's propertyName, the method must throw a JsonSchemaException."
        }
      ]
    },
    {
      "group": "Add mapping when root has no mapping",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "ContextWithRootDiscriminatorNoMapping"
            },
            {
              "parameter": "discriminator",
              "value": "{ \"propertyName\": \"type\" }"
            },
            {
              "parameter": "schema",
              "value": "{ \"schemaLocation\": \"/child\", \"schemaNode\": { \"discriminator\": { \"mapping\": { \"dog\": { \"$ref\": \"#/definitions/Dog\" } } } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "Root discriminator now contains mapping { \"dog\": { \"$ref\": \"#/definitions/Dog\" } }; no exception thrown.",
          "description": "If the root discriminator lacks a \"mapping\" field but the nested schema provides one, the method should copy the entire mapping object to the root discriminator."
        }
      ]
    },
    {
      "group": "Merge mappings without conflict",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "ContextWithRootMapping"
            },
            {
              "parameter": "discriminator",
              "value": "{ \"propertyName\": \"type\", \"mapping\": { \"cat\": { \"$ref\": \"#/definitions/Cat\" } } }"
            },
            {
              "parameter": "schema",
              "value": "{ \"schemaLocation\": \"/child\", \"schemaNode\": { \"discriminator\": { \"mapping\": { \"dog\": { \"$ref\": \"#/definitions/Dog\" }, \"cat\": { \"$ref\": \"#/definitions/Cat\" } } } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "Root mapping now contains both \"cat\" and \"dog\" entries; the existing \"cat\" entry is unchanged because the node reference is identical; no exception thrown.",
          "description": "When both root and child discriminators have mappings, the method should merge new keys and ignore keys that already exist with the same JsonNode instance."
        }
      ]
    },
    {
      "group": "Merge mappings with conflict throws",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "ContextWithRootMappingConflict"
            },
            {
              "parameter": "discriminator",
              "value": "{ \"propertyName\": \"type\", \"mapping\": { \"cat\": { \"$ref\": \"#/definitions/OldCat\" } } }"
            },
            {
              "parameter": "schema",
              "value": "{ \"schemaLocation\": \"/child\", \"schemaNode\": { \"discriminator\": { \"mapping\": { \"cat\": { \"$ref\": \"#/definitions/NewCat\" } } } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "JsonSchemaException with message containing \"discriminator mapping redefinition from cat\"",
          "description": "If a child schema attempts to map a key that already exists in the root discriminator but points to a different JsonNode, the method must throw a JsonSchemaException."
        }
      ]
    },
    {
      "group": "Both mappings null – no merge performed",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "ContextWithRootDiscriminatorNoMapping"
            },
            {
              "parameter": "discriminator",
              "value": "{ \"propertyName\": \"type\" }"
            },
            {
              "parameter": "schema",
              "value": "{ \"schemaLocation\": \"/child\", \"schemaNode\": { \"discriminator\": {} } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "Root discriminator unchanged (still no mapping); no exception thrown.",
          "description": "When both the root discriminator and the child discriminator lack a \"mapping\" field, the method should simply register the discriminator without attempting any merge."
        }
      ]
    }
  ]
}