The process of creating a Structr application usually begins with the data model. This chapter focuses on the various steps required to create the data model and serves as a guide to help you navigate the multitude of possibilities.

## A Primer on Data Modeling

The schema should mirror the attributes and relationships that objects have in the real world as closely as possible. A few basic rules help you determine whether an object should be modeled as a node, a relationship, or a property.

### When to Use Nodes?

Most things that you would use a *noun* to describe should be modeled as nodes.

- real-world objects like people, companies, documents, products
- abstract objects that are distinct entities with a unique identity and one or more attributes
- properties that several objects can have in common, like an address or a category
- collections of property values (the items of a list, etc.)
- relationships between **more than two** objects (hyper-relationships)

### When to Use Properties?

Most things that you would use an *adjective* to describe should be modeled as a properties.

- single values like an ID, a name, a color, etc.
- time or date values (if you are not using a time tree index)

### When to Use Relationships?

Most things that you would use a verb to describe should be modeled as relationships.

- relationships between objects that are not based on a single property
- actions or activities
- facts

## Creating a Basic Type

To create a new type, click the green "Create Data Type" button in the top left corner of the [Schema](/structr/docs/ontology/Admin%20User%20Interface/Schema) area.

### Using the Create Type Dialog

![The Create Type Dialog](../../schema_create-type_Project.png)

#### Name & Traits

When you create a new data type, you will first be asked to enter a name for the new type and, if desired, select one or more traits. You can choose from a list of built-in traits to take advantage of functionality provided by Structr.

#### Changelog

The Disable Changelog checkbox allows you to exclude this type from the changelog - if the changelog is activated in the Structr settings.

[Read more about the Changelog.](/structr/docs/ontology/Operations/Auditing)

#### Default Visibility

The two visibility checkboxes allow you to automatically make all instances of the new type public or visible to logged-in users. This is useful, for example, if the data is used in the application, such as the topics in a forum.

#### OpenAPI

The OpenAPI settings allow you to include the new types in the automatically generated OpenAPI description provided by Structr at `/structr/openapi`.

All types for which you activate the "Include in OpenAPI output" checkbox and enter the same tag will be provided together with the standard endpoints for login, logout, etc. at `/structr/openapi/<tag>.json`.

[Read more about OpenAPI.](/structr/docs/ontology/APIs%20&%20Integrations/OpenAPI)

### Other Ways to Create Types in the Schema

Like all other parts of the application, the schema definition itself is stored in the database, so you can also create new types by adding objects of type `SchemaNode` with the name of the desired type in the `name` attribute, and you can also do this from a script or method using the `create()` function.

## Extending a Type with Properties

When you click Create in the Create Type dialog, the new type is created and the dialog switches to an Edit Type dialog. You can also open the Edit Type dialog by hovering over a type node and clicking the pencil icon.

### Using the Edit Type Dialog

The dialog consists of six tabs that configure type properties or display type information.

#### General

The General tab is similar to the Create Type dialog and provides configuration options for name, traits, changelog and visibility checkboxes, and a Permissions table. The Permissions table allows you to grant specific groups access rights to all instances of the type.

#### Direct Properties

<img src="http://localhost:8082/structr/docs/schema_property-added_projectId.png" class="small-image-50" />

The Direct Properties tab displays a table where you add and edit attributes for the type. Each row represents an attribute with the following configuration options.

##### JSON Name & DB Name¹

JSON Name specifies the attribute name used to access the attribute in code, REST APIs, and other interfaces.

<small>¹ There is an additional setting that is hidden by default: DB Name, which allows you to specify a different database name when working with a database schema you don't control. Enable this setting through the "Show database name for direct properties" checkbox in the configuration menu in the upper right corner of the Schema area.</small>

##### Type

Type specifies the attribute's data type. Common types include String for text values, Integer for whole numbers, and Date for timestamps and date values. Additional types are available, including array versions of these primitive data types.

The type controls what values are accepted as input. For example, an integer attribute only accepts numeric input. A date attribute accepts string values in ISO-8601 format or according to a custom date pattern specified in the format column. Structr stores dates as long values with millisecond precision in the database.

[Read more about Property Types.](/structr/docs/ontology/References/Built-in%20properties)

##### Format

The Format field is optional and has different meanings depending on the attribute type.

- For string attributes, the format is interpreted as a regular expression that validates input. All values written to the attribute must match this regular expression pattern.
- For numeric attributes, the format specifies a valid range using mathematical interval notation, allowing you to enforce that input values fall within a certain interval. For example, [2,100[ accepts values from 2 (inclusive) to 100 (exclusive).
- For date attributes, the format specifies a date pattern following Java's SimpleDateFormat specification. This allows you to accept dates in custom formats beyond the default ISO-8601 format.

##### Notnull

##### Comp.

##### Uniq.

#### Idx

##### Fulltext (Indexing)

##### Default Value




### Linked Properties

This section displays special attributes that are automatically created from relationships between types. These are called contextual or linked properties.

### Inherited Properties

This section displays attributes inherited from traits or base classes.

#### Default Attributes

Every node in Structr has at least the following attributes that it inherits from the base type `AbstractNode`.

| Name | Description | Type |
| --- | --- | --- |
| `id` | The primary identifer of the node, a UUIDv4 | string |
| `type` | The type of the node | string |
| `name` | The name of the node | string |
| `createdDate` | The creation timestamp | date |
| `lastModifiedDate` | The timestamp of the last modification | date |
| `visibleToPublicUsers` | The "public visibility" flag | boolean |
| `visibleToAuthenticatedUsers` | The "authenticated visibility" flag | boolean |

### Views

### Methods



## Linking Two Types

To create a relationship between two types, click the lower dot on the start type and drag the green connector to the upper dot on the target type. This will open the Create Relationship dialog.

### Using the Create Relationship Dialog

![The Create Relationship Dialog](../../schema_relationship-project-task-created.png)

#### Basic Relationship Properties

When you create a relationship, you are asked to configure the source cardinality, the relationship type, and the target cardinality. Below the cardinality selectors, you define the property names that determine how you access the relationship from each type in your code.

##### Cardinality

Select 1 or * from the dropdown for source and target cardinality to define how many objects can connect. Use 1 for single connections and * for multiple connections. For example, if each Project contains multiple Tasks but each Task belongs to one Project, select 1 for the source cardinality (Project side) and * for the target cardinality (Task side).

##### Relationship Type

Enter a name in the center input field that describes the relationship in your database schema. This is typically an action or connection like "OWNS", "MANAGES", or "BELONGS_TO".

>**Note:** Please be as specific as possible and try not to reuse existing relationship types, as this can lead to performance issues later on. For example, do not use “HAS” for everything, as you will then lose the advantage of being able to query different relationship types separately, and all data from the database will have to be filtered via the target type.

##### Property Names

Specify property names in the input fields below each cardinality selector to define the attribute names you use to retrieve related objects from each type. The property name on the Project side (e.g., "tasks") lets you retrieve all tasks for a project, while the property name on the Task side (e.g., "project") lets you access the parent project. Structr suggests names automatically based on the type names and cardinalities - plural names for *-cardinality and singular names for 1-cardinality. You can change these suggestions to match your domain model.


#### Cascading Delete

#### Automatic Creation of Related Nodes

#### Permission Resolution




## Inheritance & Traits

Unless otherwise specified, newly created types inherit from the base type `AbstractNode`, which already provides some attributes.


![["Built-in types" as table]]

## Transactions

## Indexing
