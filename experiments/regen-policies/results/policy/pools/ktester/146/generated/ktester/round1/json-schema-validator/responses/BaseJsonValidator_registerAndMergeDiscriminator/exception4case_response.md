{
  "tests": [
    {
      "group": "Null discriminator argument throws NPE",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "ContextWithRootDiscriminator"
            },
            {
              "parameter": "discriminator",
              "value": "null"
            },
            {
              "parameter": "schema",
              "value": "{ \"schemaLocation\": \"/child\", \"schemaNode\": { \"discriminator\": { \"propertyName\": \"type\" } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the supplied discriminator argument is null, the method attempts to call discriminator.get(\"mapping\") and throws a NullPointerException before any registration."
        }
      ]
    },
    {
      "group": "Discriminator mapping non-object node throws ClassCastException",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentDiscriminatorContext",
              "value": "ContextWithRootDiscriminator"
            },
            {
              "parameter": "discriminator",
              "value": "{ \"propertyName\": \"type\", \"mapping\": \"notAnObject\" }"
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
          "expected": "ClassCastException",
          "description": "The discriminator argument contains a \"mapping\" field that is a JSON string (or any non‑ObjectNode). The method casts it to ObjectNode, causing a ClassCastException."
        }
      ]
    },
    {
      "group": "Schema discriminator mapping non-object node throws ClassCastException",
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
              "value": "{ \"schemaLocation\": \"/child\", \"schemaNode\": { \"discriminator\": { \"mapping\": \"notAnObject\" } } }"
            },
            {
              "parameter": "instanceLocation",
              "value": "/instance"
            }
          ],
          "expected": "ClassCastException",
          "description": "The schema's discriminator contains a \"mapping\" field that is a JSON string (or any non‑ObjectNode). When the method casts it to ObjectNode, a ClassCastException is thrown."
        }
      ]
    }
  ]
}