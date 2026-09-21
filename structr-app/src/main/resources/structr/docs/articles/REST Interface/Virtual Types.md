# Virtual Types

Virtual types are used by the CSV Import Wizard and by the ODS and ODT exporters of the ODF module; the REST interface does not serve them as endpoints.

A virtual type is a declarative mapping between the properties of a schema type and a different, external shape of the same data. It names a source type, lists the properties that take part in the mapping, and gives each of them a name in the external shape and optional transformation functions for both directions. In the output direction Structr reads objects of the source type, drops the ones that fail an optional filter expression and turns each remaining object into a flat map of target names and transformed values. In the input direction Structr takes a map keyed by target names, removes everything the mapping does not know, renames the keys back to the property names of the source type and converts the values. Nothing is stored along the way; a virtual type only describes how data crosses the boundary between the schema and a file format.

## Concepts

A virtual type consists of a `VirtualType` node and any number of `VirtualProperty` nodes connected to it. Both are ordinary node types that the api-builder module registers at startup, so the Admin UI, the REST interface and scripting can create and edit the definitions like any other objects. The definitions are part of a deployment export and land in `virtual-types.json`; importing a deployment replaces all existing virtual types with the ones from that file.

### Source Type

The source type is the schema type whose objects the mapping reads or produces. Source names of the virtual properties are resolved against this type, so every source name must be a property of the source type.

### Virtual Properties

Each virtual property connects one property of the source type (the source name) with one key in the external shape (the target name). A virtual property needs at least a source name or an output function; without one of the two the mapping fails with an error. When you leave the target name empty, the source name is used, and the value passes through unchanged. Virtual properties are sorted by position, which fixes the order of the keys in the output and the order in which input functions run.

### Filter Expression

The filter expression is evaluated once per source object in the output direction. The object is the current entity of the expression, so it is available as `this`. Only objects for which the expression returns the boolean `true` are transformed; an empty expression accepts every object. If the expression throws an error, Structr logs a warning and lets the object through.

## Property Reference

The `VirtualType` node has the following properties.

| Property | Description |
|----------|-------------|
| `name` | The name of the virtual type. The CSV importer generates one for its temporary mapping; for exporters you choose it yourself |
| `sourceType` | The schema type whose properties the source names refer to |
| `filterExpression` | An optional script expression that decides per source object whether it is transformed |
| `position` | An integer that sorts the virtual types in the Admin UI list |
| `properties` | The connected `VirtualProperty` nodes |
| `visibleToPublicUsers`, `visibleToAuthenticatedUsers` | Visibility flags of the definition node itself. They control who may read the definition and have no effect on the transformation |

The `VirtualProperty` node has the following properties.

| Property | Description |
|----------|-------------|
| `virtualType` | The `VirtualType` node this property belongs to |
| `sourceName` | The name of the property on the source type |
| `targetName` | The key in the external shape, for example a CSV column header, a spreadsheet cell address or a text document field name. Defaults to the source name |
| `inputFunction` | An optional script expression that converts a value in the input direction before it is written to the source property |
| `outputFunction` | An optional script expression that computes the value in the output direction |
| `position` | An integer that sorts the properties of one virtual type |
| `visibleToPublicUsers`, `visibleToAuthenticatedUsers` | Visibility flags of the definition node, without effect on the transformation |

## Transformation Functions

Input function, output function and filter expression are auto-script fields. Structr wraps the text in `${...}` before it evaluates it, so a plain expression is StructrScript and an expression enclosed in curly braces becomes JavaScript. Write `upper(input)` for StructrScript and `{ $.input.toUpperCase() }` for JavaScript.

The value a function works on is available under the keyword `input`, in JavaScript as `$.input`. What `input` holds depends on the direction.

### Output Functions

In the output direction `input` is the complete source object. An output function returns the value that is stored under the target name, so it can read any property of the source object, combine several of them or compute something new. The source object is not the current entity here, so `this` is not available; use `input` instead. When a virtual property has no output function, Structr copies the value of the source property.

A pass-through under a different name needs no function at all: set the source name to `internalProjectCode` and the target name to `projectCode`. A computed value takes an output function:

```javascript
{ $.input.firstName + ' ' + $.input.lastName }
```

Related objects are reachable in the same way:

```javascript
{ $.input.manager ? $.input.manager.name : 'Unassigned' }
```

Formatting for a document works with the usual built-in functions:

```javascript
{ $.dateFormat($.input.createdDate, 'yyyy-MM-dd') }
```

```javascript
{ $.input.amount.toFixed(2) + ' EUR' }
```

### Input Functions

In the input direction Structr looks up the target name in the incoming map, removes the entry, runs the input function if there is one and puts the result under the source name. Here `input` is the single incoming value, for a CSV import the text of one cell. Values that arrive as text and belong to a typed property are converted afterwards by the normal property conversion, so an input function is only needed when the text does not match the format Structr expects.

Parsing a date in a non-ISO format:

```javascript
{ $.parseDate($.input, 'MM/dd/yyyy') }
```

Turning a yes/no column into a boolean:

```javascript
{ $.input === 'yes' }
```

Trimming and normalising a text value in StructrScript:

```
trim(lower(input))
```

## CSV Import Wizard

The CSV Import Wizard in the Files area is the main consumer of virtual types. When you open the context menu of a file with the content type `text/csv` and choose Import CSV, the wizard shows a mapping table with one row per CSV column. Each row has the column name, an optional Transformation field and a select box with the properties of the chosen target type, plus the entry "-- skip --". The wizard preselects the property whose name matches the column header best.

When you start the import, Structr turns this table into a virtual type behind the scenes. The importer requires the api-builder module and refuses to run without it. It creates a `VirtualType` named `ImportFromCsv` followed by a timestamp, with the target type of the import as source type, and one `VirtualProperty` per mapped column: the CSV column header becomes the target name, the selected property becomes the source name and the text in the Transformation field becomes the input function. Columns set to skip get no virtual property. The importer then reads the file row by row, applies the mapping in the input direction, which drops unmapped columns, renames the remaining ones to property names and runs the input functions, and creates one object per row. When the import finishes, it deletes the temporary virtual type again.

Because the importer builds its own mapping, virtual types you define in the Admin UI are not offered in the wizard. The wizard's Transformation field is where you write input functions for imports, and the value of the current cell is available there as `input`.

As an example, a CSV file with the columns `Customer No`, `Signup` and `Newsletter` imported into a type `Customer` could be mapped like this.

| Column name | Transformation | Property |
|-------------|----------------|----------|
| `Customer No` | | `customerNumber` |
| `Signup` | `{ $.parseDate($.input, 'dd.MM.yyyy') }` | `registeredAt` |
| `Newsletter` | `{ $.input === 'yes' }` | `subscribed` |

The resulting temporary virtual type has the source type `Customer` and three virtual properties with the target names `Customer No`, `Signup` and `Newsletter`. The [Data Creation & Import](/structr/docs/ontology/Building%20Applications/Data%20Creation%20&%20Import) chapter describes the other options of the wizard.

## ODS and ODT Exporters

The ODF module adds the node types `ODSExporter` for spreadsheets and `ODTExporter` for text documents. Both share the base type `ODFExporter` with three properties: `documentTemplate` is the file that serves as template, `resultDocument` is the file the exporter writes, and `transformationProvider` is the virtual type that decides which values end up in the document. Here the virtual type is a stored definition that you create in the Admin UI and connect to the exporter; the exporters are the reason the Virtual Types area exists.

An exporter offers three methods that you call on an exporter object, for example from a schema method or over REST with `POST /structr/rest/ODSExporter/{uuid}/createDocumentFromTemplate`. `createDocumentFromTemplate` copies the template into the result document and creates that file next to the template if the exporter has none yet. `exportAttributes` takes the parameter `uuid` of the object to export; it loads that object, runs the virtual type in the output direction and writes the result into the result document. `exportImage` takes the `uuid` of an image and replaces the picture with that name in the document.

The two exporters interpret the target names differently. The ODS exporter treats each target name as a cell address in the first sheet, such as `B2`, and writes the value into that cell; a collection value is written into consecutive cells downwards from that address. The ODT exporter treats each target name as the name of a user field declared in the text document and sets the field to the value; collection values are joined with line breaks. The filter expression applies here as well, so if the exported object does not pass the filter, nothing is written and the exporter logs an error.

To export an `Invoice` into a spreadsheet template, proceed as follows.

1. Create a virtual type `InvoiceSheet` with the source type `Invoice`.
2. Add a virtual property with the source name `number` and the target name `B2`, and another with the source name `customer`, the target name `B3` and the output function `{ $.input.customer.name }`.
3. Add a virtual property with the target name `B5` and the output function `{ $.dateFormat($.input.issuedAt, 'dd.MM.yyyy') }`.
4. Create an `ODSExporter` object, set its `documentTemplate` to the uploaded template file and its `transformationProvider` to `InvoiceSheet`.
5. Call `createDocumentFromTemplate` on the exporter, then `exportAttributes` with the `uuid` of the invoice.

The same virtual type serves a text document when its target names are field names instead of cell addresses, for example `invoiceNumber`, `customerName` and `issueDate`.

## Virtual Types and REST

The REST interface resolves the first path segment of a URL against the schema, and it only creates a resource when a type of that name exists. A virtual type's name is not a type, so `/structr/rest/{VirtualTypeName}` does not return transformed data; it returns an error, or, when a schema type of the same name exists, the plain objects of that schema type without any transformation. The Admin UI still shows a preview link built from the virtual type name, which does not work for the same reason. The definitions themselves are reachable as usual under `/structr/rest/VirtualType` and `/structr/rest/VirtualProperty`.

## Related Topics

- [Data Creation & Import](/structr/docs/ontology/Building%20Applications/Data%20Creation%20&%20Import) - The CSV Import Wizard whose mapping table becomes a temporary virtual type
- [Virtual Types](/structr/docs/ontology/Admin%20User%20Interface/Virtual%20Types) - The Admin UI area for creating virtual types and their properties
- [Data Model](/structr/docs/ontology/Building%20Applications/Data%20Model) - The schema types that virtual types map from
