You can add new properties (which are called "local attributes" to distinguish them from remote attributes) by entering a property (JSON) name and setting the type.

* JSON Name: The primary JSON key to reference a property
* DB Name: Optional name of the database property if different from the JSON key
* Type: Internal data type
* Format: You can set a format on Date or Double properties for output formatting. Note that starting with 1.0.RC3, there are more complex attribute types like Function, Counter, Notion, Cypher. The active code for these types has to be entered in the Format column.
* Not Null: If set, the value must not be null
* Unique: If set, the value must be unique
* Default: Default value for this property
