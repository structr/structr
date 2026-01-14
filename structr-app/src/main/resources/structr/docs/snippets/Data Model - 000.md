The process of creating a Structr application usually begins with the data model. This chapter focuses on the various steps required to create the data model and serves as a guide to help you navigate the multitude of possibilities.

## Creating Your First Type

In the simplest case, a data model consists of exactly one type with no additional attributes other than those already defined in the base class. Unless otherwise specified, the newly created type inherits from the base class `AbstractNode`, which already provides some attributes.

Note that node types are the primary objects in the data model, while relationship types can only be created when at least two node types are defined.



### Default Attributes

Every node in Structr has at least the following attributes.

| Name | Description | Type |
| --- | --- | --- |
| `id` | The primary identifer of the node, a UUIDv4 | string |
| `type` | The type of the node | string |
| `name` | The name of the node | string |
| `createdDate` | The creation timestamp | date |
| `lastModifiedDate` | The timestamp of the last modification | date |
| `visibleToPublicUsers` | The "public visibility" flag | boolean |
| `visibleToAuthenticatedUsers` | The "authenticated visibility" flag | boolean |


### Adding Attributes

### Attribute Types

### Indexing

### Inheritance

## 
Next:
- default attributes
- adding attributes
- attribute types
- indexing

- relationships
- ....