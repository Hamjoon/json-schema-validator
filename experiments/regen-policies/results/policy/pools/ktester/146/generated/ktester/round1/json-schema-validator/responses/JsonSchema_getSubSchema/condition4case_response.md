{
  "group": "Successful navigation – array index segment",
  "cases": [
    {
      "input": [
        {
          "parameter": "fragment",
          "value": "JsonNodePath representing pointer \"/items/0\""
        }
      ],
      "expected": "JsonSchema instance representing the schema node at path /items/0 (the first element of the \"items\" array). The returned schema's getSchemaLocation().getFragment() should be \"#/items/0\".",
      "description": "The fragment points to an array element. The parent node is an array, the segment is a Number, so the branch `if ((segment instanceof Number) && parentNode.isArray())` is exercised. The method should create a sub‑schema for the array element and return it."
    }
  ]
}