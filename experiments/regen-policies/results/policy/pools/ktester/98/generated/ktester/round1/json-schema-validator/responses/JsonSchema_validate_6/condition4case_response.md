Invalid control character at: line 9 column 23 (char 321)
{
  "group": "validate(String, InputFormat) - successful validation",
  "cases": [
    {
      "input": [
        { "parameter": "input", "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}" },
        { "parameter": "inputFormat", "value": "InputFormat.JSON" }
      ],
      "expected": "[]  // empty Set<ValidationMessage> indicating the JSON instance conforms to the schema",
      "description": "Valid JSON string that matches the schema – the method should deserialize the input and return an empty validation result."
    },
    {
      "input": [
        { "parameter": "input", "value": "name: John\nage: 30" },
        { "parameter": "inputFormat", "value": "InputFormat.YAML" }
      ],
      "expected": "[]  // empty Set<ValidationMessage> because the YAML instance satisfies the schema",
      "description": "Valid YAML input (when InputFormat.YAML is supported). The method should correctly decode YAML, convert it to a JsonNode and perform validation without errors."
    },
    {
      "input": [
        { "parameter": "input", "value": "{}" },
        { "parameter": "inputFormat", "value": "InputFormat.JSON" }
      ],
      "expected": "[]  // empty Set<ValidationMessage> – empty object is still valid against the schema",
      "description": "Empty JSON object – still a valid instance for the given schema, expecting no validation messages."
    }
  ]
}