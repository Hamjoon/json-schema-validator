{
  "test_cases": [
    {
      "group": "Schema without discriminator but context already has one – registration overwrites",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "ContextWithRootMappingAndEntry"
            },
            {
              "parameter": "discriminator",
              "value": "{ }"
            },
            {
              "parameter": "schema",
              "value": "{ \"schemaLocation\": \"/root\", \"schemaNode\": { } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "The discriminator stored in the context for \"/root\" becomes the empty object passed in (i.e., previous mapping is overwritten). No exception is thrown.",
          "description": "When the schema does not contain a \"discriminator\" property but the context already holds a discriminator for the same schema location, the method should skip merging logic and simply register the supplied discriminator, effectively replacing the previous entry."
        }
      ]
    },
    {
      "group": "Child mapping null – root mapping unchanged",
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
              "value": "{ \"schemaLocation\": \"/child\", \"schemaNode\": { \"discriminator\": { } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "Root discriminator mapping remains exactly { \"cat\": { \"$ref\": \"#/definitions/Cat\" } }. No exception is thrown.",
          "description": "The child schema defines a discriminator object without a \"mapping\" field. Since the root already has a mapping, the method should perform no merge and leave the root mapping untouched."
        }
      ]
    },
    {
      "group": "Child mapping empty object – no merge performed",
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
              "value": "{ \"schemaLocation\": \"/child\", \"schemaNode\": { \"discriminator\": { \"mapping\": { } } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "Root discriminator mapping stays unchanged (contains only \"cat\"). No exception is thrown.",
          "description": "The child discriminator provides an empty \"mapping\" object. The merge loop iterates over zero fields, so the existing root mapping should remain unchanged."
        }
      ]
    },
    {
      "group": "Mapping key redefinition with equal content but different JsonNode instances throws",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "ContextWithRootMappingDifferentInstance"
            },
            {
              "parameter": "discriminator",
              "value": "{ \"propertyName\": \"type\", \"mapping\": { \"cat\": { \"$ref\": \"#/definitions/Cat\" } } }"
            },
            {
              "parameter": "schema",
              "value": "{ \"schemaLocation\": \"/child\", \"schemaNode\": { \"discriminator\": { \"mapping\": { \"cat\": { \"$ref\": \"#/definitions/Cat\" } } } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "JsonSchemaException with message containing \"discriminator mapping redefinition from cat\" is thrown.",
          "description": "Both root and child discriminators map the same key \"cat\" to JSON objects that are structurally identical but are distinct JsonNode instances. Because the implementation compares node references (using !=), this situation is treated as a conflict and must raise an exception."
        }
      ]
    }
  ]
}