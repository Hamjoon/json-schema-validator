{
  "testCases": [
    {
      "group": "Normal operation – valid inputs",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Returns a non‑null JsonSchema object representing the supplied schema; the schema is pre‑loaded without throwing any exception.",
          "description": "Happy‑path test: a well‑formed schema location and a default configuration should result in a successfully created JsonSchema instance."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/complex-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setFailFast(true)"
            }
          ],
          "expected": "Returns a non‑null JsonSchema object; preload is executed with the supplied configuration (fail‑fast enabled).",
          "description": "Ensures that a custom configuration is correctly propagated to the preload step."
        }
      ]
    },
    {
      "group": "Null or missing arguments",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) because schemaUri is required.",
          "description": "Verifies that the method validates the mandatory schema location argument."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "Returns a non‑null JsonSchema object; the factory should fall back to a default SchemaValidatorsConfig internally.",
          "description": "Checks that a null config does not cause a failure and that a default configuration is used."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "Throws NullPointerException (or IllegalArgumentException) because schemaUri is required, irrespective of config.",
          "description": "Confirms that the method fails fast when both arguments are null."
        }
      ]
    },
    {
      "group": "Schema loading failures",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/non‑existent.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws a RuntimeException (e.g., JsonSchemaException) indicating that the schema could not be found or loaded.",
          "description": "Ensures that a missing schema resource propagates an appropriate exception from loadSchema."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/invalid‑json-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws a JsonSchemaException (or IOException) because the schema content is not valid JSON/YAML.",
          "description": "Validates that parsing errors inside loadSchema are surfaced to the caller."
        }
      ]
    },
    {
      "group": "Pre‑load edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/circular‑ref-schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Returns a JsonSchema instance; preload completes without StackOverflowError, handling circular $ref gracefully.",
          "description": "Tests that the private preload method correctly detects and handles circular references."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new SchemaLocation(\"classpath:/schemas/large‑schema.json\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setCacheEnabled(true)"
            }
          ],
          "expected": "Returns a JsonSchema instance; preload caches sub‑schemas as configured.",
          "description": "Verifies that configuration flags affecting preload (e.g., caching) are honoured."
        }
      ]
    }
  ]
}