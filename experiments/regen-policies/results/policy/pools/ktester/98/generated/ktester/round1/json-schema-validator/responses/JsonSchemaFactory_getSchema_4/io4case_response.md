{
  "group": "JsonSchemaFactory.getSchema(SchemaLocation) - additional scenarios",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"file:/tmp/valid-schema.json\")"
        }
      ],
      "expected": "returns a non‑null JsonSchema instance whose meta‑schema version matches the $schema keyword in the file (e.g., \"http://json-schema.org/draft/2020-12/schema#\")",
      "description": "Happy‑path for a file‑system URI: the file exists and contains a correct JSON Schema; the factory should read, parse and return a usable JsonSchema."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"http://example.com/schemas/remote-schema.json\")"
        }
      ],
      "expected": "returns a non‑null JsonSchema instance representing the remote schema",
      "description": "Valid HTTP URI where the remote resource is reachable (e.g., via a test HTTP server or WireMock). The method must perform an HTTP GET, parse the response and produce a JsonSchema."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schemas/with-ref.json\")"
        }
      ],
      "expected": "returns a JsonSchema that can resolve the external $ref defined in the schema (e.g., a definition located at classpath:/schemas/definitions.json)",
      "description": "Schema contains a $ref to another schema file. The factory must load the referenced schema and attach it to the resulting JsonSchema so that validation would succeed for referenced definitions."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schemas/circular-ref.json\")"
        }
      ],
      "expected": "returns a JsonSchema instance without throwing StackOverflowError; circular $ref is detected and handled via lazy resolution",
      "description": "Schema has a self‑referencing $ref (direct or indirect). The factory should create the JsonSchema without entering an infinite recursion."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schemas/space%20in%20name.json\")"
        }
      ],
      "expected": "returns a non‑null JsonSchema instance; the URL‑encoded space is correctly decoded and the resource is loaded",
      "description": "Schema location contains URL‑encoded characters (e.g., %20). The method must decode the path before locating the resource."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"file:/tmp/\")"
        }
      ],
      "expected": "throws IllegalArgumentException indicating that the location does not point to a file containing a JSON Schema",
      "description": "The supplied URI points to a directory rather than a file; the factory should reject it early."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"file:/tmp/schema.json?version=1\")"
        }
      ],
      "expected": "returns a non‑null JsonSchema instance; query parameters are ignored during loading",
      "description": "Schema URI includes a query string. The loader must strip the query part and load the underlying file."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schemas/cached-schema.json\")"
        }
      ],
      "expected": "the two calls to getSchema with the same SchemaLocation return the identical JsonSchema instance (reference equality)",
      "description": "Verifies that the factory caches schemas; repeated requests for the same location should return the same object, not a new instance."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schemas/malformed.json\")"
        }
      ],
      "expected": "throws JsonSchemaException (or IOException) because the content is not valid JSON",
      "description": "The resource exists but contains malformed JSON (e.g., missing a closing brace). The factory must detect the parsing error and surface an appropriate exception."
    }
  ]
}