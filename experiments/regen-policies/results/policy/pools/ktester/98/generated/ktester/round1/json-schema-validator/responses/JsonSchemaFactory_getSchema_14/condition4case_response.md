{
  "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schema/example-schema.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "instance of JsonSchema (non‑null) whose meta‑schema matches the supplied schemaUri",
      "description": "Happy‑path: all arguments are non‑null and refer to a valid schema; the method should forward the parameters to newJsonSchema and return a usable JsonSchema object."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "null"
        },
        {
          "parameter": "jsonNode",
          "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "NullPointerException",
      "description": "schemaUri is null – the factory cannot resolve a location, so the method is expected to throw NPE (or propagate the same exception from newJsonSchema)."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schema/example-schema.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "null"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "instance of JsonSchema (non‑null) that uses an empty ObjectNode as the schema source",
      "description": "jsonNode is null – the implementation should treat a missing JsonNode as an empty schema (ObjectNode) and still create a JsonSchema object."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schema/example-schema.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
        },
        {
          "parameter": "config",
          "value": "null"
        }
      ],
      "expected": "instance of JsonSchema (non‑null) with default SchemaValidatorsConfig applied",
      "description": "config is null – the factory should fall back to its default configuration when none is supplied."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "null"
        },
        {
          "parameter": "jsonNode",
          "value": "null"
        },
        {
          "parameter": "config",
          "value": "null"
        }
      ],
      "expected": "NullPointerException",
      "description": "All parameters are null – the method cannot operate and must raise an exception."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schema/non‑existent.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "SchemaLoadingException (or IOException) indicating that the schema resource cannot be found",
      "description": "The supplied SchemaLocation points to a resource that does not exist; newJsonSchema should attempt to load it and propagate the loading failure."
    }
  ]
}