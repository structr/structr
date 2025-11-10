# Middleware

The Structr platform has powerful middleware functionality by providing connectivity for many open exchange protocols like HTTP/JSON/REST, Kafka, MQTT, Pulsar, or XMPP.

Also, data ingested through interfaces are mostly automatically converted and normalized. In addition, they can be parsed based on custom rulesets. Together with a transactional validation logic, this ensures a high data quality and consistency.

## Supported Protocols and Formats

- JSON
- XML
- Text with encoding
- Binary formats

## Data Conversion

When processing and storing data, Structr automatically converts data between the input format, the internal format Structr is using to process data and the database format which is optimized for data persistence and dependent from the database implementation.

#### Example: Floating-point numbers

<svg width="100%" viewBox="0 0 100 10" xmlns="http://www.w3.org/2000/svg"><text style="font-family:sans-serif;font-size:2px;font-weight:600" x="2" y="2">External format</text><text style="font-family:monospace;font-size:1.75px;font-weight:400" x="2" y="6">{ "aNumber": 123.456 }</text><polyline points="28,2 26,3 28,4" stroke="currentColor" stroke-width="0.5" fill="none"/><line x1="26" y1="3" x2="35" y2="3" stroke="currentColor" stroke-width="0.5" /><polyline points="33,2 35,3 33,4" stroke="currentColor" stroke-width="0.5" fill="none"/><text style="font-family:sans-serif;font-size:2px;font-weight:600" x="39" y="2">Internal format</text><text style="font-family:monospace;font-size:1.75px;font-weight:400" x="39" y="6">java.lang.Double</text><polyline points="60,2 58,3 60,4" stroke="currentColor" stroke-width="0.5" fill="none"/><line x1="58" y1="3" x2="67" y2="3" stroke="currentColor" stroke-width="0.5" /><polyline points="65,2 67,3 65,4" stroke="currentColor" stroke-width="0.5" fill="none"/><text style="font-family:sans-serif;font-size:2px;font-weight:600" x="70" y="2">Database format</text><text style="font-family:monospace;font-size:1.75px;font-weight:400" x="70" y="6">FLOAT</text></svg>

When data is sent to a Structr API endpoint, the format is JSON: `{ "aNumber": 123.456 }`

The JSON document is parsed and the values are automatically converted into an internal format, in this case the Java standard datatype `java.lang.Double` which is implemented by Structr's core property class `org.structr.core.property.DoubleProperty`.

When stored in the database, Structr's Neo4j database driver automatically converts the value to the Neo4j/Cypher datatype `FLOAT`.

#### Example: Dates

<svg width="100%" viewBox="0 0 100 10" xmlns="http://www.w3.org/2000/svg"><text style="font-family:sans-serif;font-size:2px;font-weight:600" x="2" y="2">External format</text><text style="font-family:monospace;font-size:1.75px;font-weight:400" x="2" y="6">{ "aDate": </text><text style="font-family:monospace;font-size:1.75px;font-weight:400" x="2" y="8.5">"2025-03-01T12:34:56Z" }</text><polyline points="28,2 26,3 28,4" stroke="currentColor" stroke-width="0.5" fill="none"/><line x1="26" y1="3" x2="35" y2="3" stroke="currentColor" stroke-width="0.5" /><polyline points="33,2 35,3 33,4" stroke="currentColor" stroke-width="0.5" fill="none"/><text style="font-family:sans-serif;font-size:2px;font-weight:600" x="39" y="2">Internal format</text><text style="font-family:monospace;font-size:1.75px;font-weight:400" x="39" y="6">java.util.Date</text><polyline points="60,2 58,3 60,4" stroke="currentColor" stroke-width="0.5" fill="none"/><line x1="58" y1="3" x2="67" y2="3" stroke="currentColor" stroke-width="0.5" /><polyline points="65,2 67,3 65,4" stroke="currentColor" stroke-width="0.5" fill="none"/><text style="font-family:sans-serif;font-size:2px;font-weight:600" x="70" y="2">Database format</text><text style="font-family:monospace;font-size:1.75px;font-weight:400" x="70" y="6">INTEGER</text></svg>

The JSON `{ "aDate": "2025-03-01T12:34:56Z" }` is parsed with the default format for parsing dates: `yyyy-MM-dd'T'HH:mm:ssZ`. It can be configured in `structr.conf`.

The resulting value is automatically converted into an internal format, in this case the Java standard datatype `java.util.Date` which is implemented by Structr's core property class `org.structr.core.property.DateProperty`.

In the database, date values are stored as the Neo4j/Cypher data type `INTEGER`.

>**Note**: Cypher also provides a `DATE` data type. Structr is storing dates as fixed-point numbers for two reasons: #1 When Structr started, there was no Cypher or an explicit data type for dates, and #2 for compatibility reasons.

## Custom data types

A custom data type is the template for storing information in the form of objects of a certain type. A data type has a name and other custom properties that store data in form of simple or complex attributes.

## Lifecycle Methods

Any data type definition in Structr's schema has a default set of so-called lifecycle methods which are, when present, automatically executed on a specific event.

- **onNodeCreation**: When an object is created, at the begin of the transaction
- **onCreate**: When an object is created, at the end of the transaction
- **afterCreate**: After an object has been created
- **onSave**: When object data is saved
- **afterSave**: After object data has been saved
- **onDelete**: When an object is deleted
- **afterDelete**: After an object has been deleted

A lifecycle method can contain script code implementing basic behaviour like changing visibility and access rights or modify metadata like status, dates etc..

But it's also possible to call other methods with more complex business logic so that the lifecycle methods work like "hooks" to trigger built-in or custom functionality.

## Reports

Structr has a range of features that enable or facilitate the creation of reports, such as dynamic files. You can also create reports in the form of dynamic web pages, using all the web IDE features that make up the Structr platform.

In both cases, you benefit from fast database queries, so reports can be generated in real time rather than having to be generated in the background, as is the case with traditional systems that have to process and aggregate large amounts of data. Thanks to the graph database architecture, this is much faster in Structr. 

