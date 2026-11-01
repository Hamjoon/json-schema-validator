{
    "group": "builder(JsonSchemaFactory) null argument handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "blueprint",
                    "value": "null"
                }
            ],
            "expected": "non‑null Builder instance",
            "description": "Calling JsonSchemaFactory.builder(null) should behave like the no‑arg overload and return a fresh, non‑null Builder object rather than throwing an exception."
        },
        {
            "input": [
                {
                    "parameter": "blueprint",
                    "value": "null"
                }
            ],
            "expected": "different Builder instances on successive calls",
            "description": "Two successive calls to JsonSchemaFactory.builder(null) must produce distinct Builder objects (different references), confirming that the method does not cache a singleton Builder."
        }
    ]
}