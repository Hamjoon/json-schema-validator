# Run 2 selected scope

Eligible: m >= 3, including abstract classes; no dedicated-test requirement. Sort by c descending, m descending, then fully qualified class name. Stop upon cumulative m >= 50, before an addition would exceed 80, or at 10 classes.

Selected 2 classes; cumulative m = 80.

| Rank | Class | c | m | Selected | Selected cumulative m |
|---:|---|---:|---:|---|---:|
| 1 | com.networknt.schema.JsonSchema | 26 | 23 | yes | 23 |
| 2 | com.networknt.schema.SchemaValidatorsConfig | 17 | 57 | yes | 80 |
| 3 | com.networknt.schema.BaseJsonValidator | 17 | 15 | no | 80 |
| 4 | com.networknt.schema.AnyOfValidator | 17 | 3 | no | 80 |
| 5 | com.networknt.schema.OneOfValidator | 17 | 3 | no | 80 |
| 6 | com.networknt.schema.JsonSchemaFactory | 15 | 20 | no | 80 |
| 7 | com.networknt.schema.JsonMetaSchema | 14 | 15 | no | 80 |
| 8 | com.networknt.schema.AllOfValidator | 14 | 3 | no | 80 |
| 9 | com.networknt.schema.ItemsValidator | 13 | 5 | no | 80 |
| 10 | com.networknt.schema.PrefixItemsValidator | 12 | 4 | no | 80 |
| 11 | com.networknt.schema.PropertiesValidator | 12 | 4 | no | 80 |
| 12 | com.networknt.schema.RefValidator | 11 | 5 | no | 80 |
| 13 | com.networknt.schema.ItemsValidator202012 | 11 | 4 | no | 80 |
| 14 | com.networknt.schema.AdditionalPropertiesValidator | 11 | 3 | no | 80 |
| 15 | com.networknt.schema.ValidationMessageHandler | 10 | 7 | no | 80 |
| 16 | com.networknt.schema.walk.WalkEvent | 9 | 12 | no | 80 |
| 17 | com.networknt.schema.NotValidator | 9 | 3 | no | 80 |
| 18 | com.networknt.schema.TypeValidator | 9 | 3 | no | 80 |
| 19 | com.networknt.schema.ValidationMessage | 8 | 20 | no | 80 |
| 20 | com.networknt.schema.ValidationContext | 8 | 15 | no | 80 |
| 21 | com.networknt.schema.IfValidator | 8 | 3 | no | 80 |
| 22 | com.networknt.schema.utils.JsonNodeUtil | 7 | 5 | no | 80 |
| 23 | com.networknt.schema.DependentSchemas | 7 | 3 | no | 80 |
| 24 | com.networknt.schema.CollectorContext | 6 | 13 | no | 80 |
| 25 | com.networknt.schema.walk.AbstractWalkListenerRunner | 5 | 3 | no | 80 |
| 26 | com.networknt.schema.walk.DefaultKeywordWalkListenerRunner | 5 | 3 | no | 80 |
| 27 | com.networknt.schema.SpecVersionDetector | 4 | 5 | no | 80 |
| 28 | com.networknt.schema.FormatKeyword | 4 | 4 | no | 80 |
| 29 | com.networknt.schema.JsonSchemaRef | 3 | 3 | no | 80 |
| 30 | com.networknt.schema.ValidationResult | 3 | 3 | no | 80 |
| 31 | com.networknt.org.apache.commons.validator.routines.DomainValidator | 2 | 15 | no | 80 |
| 32 | com.networknt.schema.ValidatorState | 2 | 8 | no | 80 |
| 33 | com.networknt.org.apache.commons.validator.routines.EmailValidator | 2 | 6 | no | 80 |
| 34 | com.networknt.org.apache.commons.validator.routines.InetAddressValidator | 2 | 4 | no | 80 |
| 35 | com.networknt.schema.utils.StringChecker | 2 | 3 | no | 80 |
| 36 | com.networknt.schema.ApplyDefaultsStrategy | 1 | 3 | no | 80 |
| 37 | com.networknt.schema.uri.ClasspathURLFactory | 1 | 3 | no | 80 |
| 38 | com.networknt.schema.uri.URISchemeFactory | 1 | 3 | no | 80 |
| 39 | com.networknt.schema.utils.UnicodeDatabase | 0 | 10 | no | 80 |
| 40 | com.networknt.org.apache.commons.validator.routines.RegexValidator | 0 | 5 | no | 80 |

The threshold is reached after the second class, exactly at the 80-method cap. A third eligible class would exceed that cap. The inherited run 1 stop condition (fewer than three selected classes) therefore applies. No class is skipped or added to evade either rule.
