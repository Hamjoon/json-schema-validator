{
  "group": "TypeValidator.getSchemaType() returns the schema type that was set",
  "cases": [
    {
      "input": [],
      "expected": "JsonType.STRING",
      "description": "When the internal field `schemaType` is set to JsonType.STRING (via reflection or a suitable constructor), getSchemaType() should return JsonType.STRING."
    },
    {
      "input": [],
      "expected": "JsonType.NUMBER",
      "description": "When the internal field `schemaType` is set to JsonType.NUMBER, getSchemaType() should return JsonType.NUMBER."
    },
    {
      "input": [],
      "expected": "JsonType.INTEGER",
      "description": "When the internal field `schemaType` is set to JsonType.INTEGER, getSchemaType() should return JsonType.INTEGER."
    },
    {
      "input": [],
      "expected": "JsonType.BOOLEAN",
      "description": "When the internal field `schemaType` is set to JsonType.BOOLEAN, getSchemaType() should return JsonType.BOOLEAN."
    },
    {
      "input": [],
      "expected": "JsonType.OBJECT",
      "description": "When the internal field `schemaType` is set to JsonType.OBJECT, getSchemaType() should return JsonType.OBJECT."
    },
    {
      "input": [],
      "expected": "JsonType.ARRAY",
      "description": "When the internal field `schemaType` is set to JsonType.ARRAY, getSchemaType() should return JsonType.ARRAY."
    },
    {
      "input": [],
      "expected": "JsonType.NULL",
      "description": "When the internal field `schemaType` is set to JsonType.NULL, getSchemaType() should return JsonType.NULL."
    },
    {
      "input": [],
      "expected": "null",
      "description": "When the internal field `schemaType` has not been initialized (remains null), getSchemaType() should return null."
    }
  ]
}