The process of creating a Structr application usually begins with the data model. This chapter focuses on the various steps required to create the data model and serves as a guide to help you navigate the multitude of possibilities.

## Types and Inheritance

In the simplest case, a data model consists of exactly one type with no additional attributes other than those already defined in the base type. Unless otherwise specified, the newly created type inherits from the base type `AbstractNode`, which already provides some attributes.

Note that node types are the primary objects in the data model, while relationship types can only be created when at least two node types are defined.

### Basic Type Creation

<img src="../schema_create-type_Project.png" class="small-image-60" />

#### Name & Traits

When you create a new data type, you will first be asked to enter a name for the new type and, if desired, select one or more traits. You can choose from a list of built-in traits to take advantage of functionality provided by Structr.

#### Changelog

The "Disable Changelog" checkbox allows you to exclude this type from the changelog - if the changelog is activated in the Structr settings.

[Read more about the Changelog.](/structr/docs/ontology/Operations/Auditing)

#### Default Visibility

The two visibility checkboxes allow you to automatically make all instances of the new type public or visible to logged-in users. This is useful, for example, if the data is used in the application, such as the topics in a forum.

#### OpenAPI

The OpenAPI settings allow you to include the new types in the automatically generated OpenAPI description provided by Structr at `/structr/openapi`.

All types for which you activate the “Include in OpenAPI output” checkbox and enter the same tag will be provided together with the standard endpoints for login, logout, etc. at `/structr/openapi/<tag>.json`.

[Read more about OpenAPI.](/structr/docs/ontology/APIs%20&%20Integrations/OpenAPI)

#### Permissions

### Other Ways to Create Types in The Schema

Like all other parts of the application, the schema definition itself is stored in the database, so you can also create new types by adding objects of type `SchemaNode´ with the name of the desired type in the `name` attribute.

You can also do this from a script or method using the `create()` function.

### Default Attributes

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

### Extending a Type With Properties

To add values to instances of the new type in the database, the type definition can be extended with properties of different types. There is a wide range of primitive data types that can be used.

#### Constraints

All attribute types have the same basic settings for simple constraints such as not-null, uniqueness, and other settings that depend on the respective type.

#### Indexing
In addition, Structr offers the option of marking an attribute as “indexed” to enable fully automatic management of a range, text, or full-text index in the database.