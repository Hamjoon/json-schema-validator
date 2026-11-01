{
  "group": "JSON format",
  "cases": [
    {
      "input": [
        {
          "parameter": "inputFormat",
          "value": "JSON"
        }
      ],
      "expected": "ObjectMapper instance returned by getJsonMapper() (same reference as JsonSchemaFactory#getJsonMapper)",
      "description": "When InputFormat.JSON is supplied, the method should take the first branch and return the JSON ObjectMapper."
    }
  ]
}