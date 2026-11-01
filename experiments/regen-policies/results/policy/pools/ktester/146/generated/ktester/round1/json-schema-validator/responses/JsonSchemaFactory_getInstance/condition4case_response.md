{
    "group": "JsonSchemaFactory.getInstance(SpecVersion.VersionFlag) - valid version flags",
    "cases": [
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V4"
                }
            ],
            "expected": "Returns a non‑null JsonSchemaFactory whose internal meta‑schema corresponds to Draft‑V4 (e.g., getMetaSchema(\"http://json-schema.org/draft-04/schema#\") is not null).",
            "description": "Verify that the factory is correctly created for the Draft‑V4 flag."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V6"
                }
            ],
            "expected": "Returns a non‑null JsonSchemaFactory whose internal meta‑schema corresponds to Draft‑V6 (e.g., getMetaSchema(\"http://json-schema.org/draft-06/schema#\") is not null).",
            "description": "Verify that the factory is correctly created for the Draft‑V6 flag."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V7"
                }
            ],
            "expected": "Returns a non‑null JsonSchemaFactory whose internal meta‑schema corresponds to Draft‑V7 (e.g., getMetaSchema(\"http://json-schema.org/draft-07/schema#\") is not null).",
            "description": "Verify that the factory is correctly created for the Draft‑V7 flag."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.DRAFT_2019_09"
                }
            ],
            "expected": "Returns a non‑null JsonSchemaFactory whose internal meta‑schema corresponds to the 2019‑09 draft (e.g., getMetaSchema(\"https://json-schema.org/draft/2019-09/schema\") is not null).",
            "description": "Verify that the factory is correctly created for the 2019‑09 draft flag."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.DRAFT_2020_12"
                }
            ],
            "expected": "Returns a non‑null JsonSchemaFactory whose internal meta‑schema corresponds to the 2020‑12 draft (e.g., getMetaSchema(\"https://json-schema.org/draft/2020-12/schema\") is not null).",
            "description": "Verify that the factory is correctly created for the 2020‑12 draft flag."
        }
    ]
}