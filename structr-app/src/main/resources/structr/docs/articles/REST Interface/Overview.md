The REST interface is the universal application programming interface (API) in Structr. It provides access to all types in the data model, including the internal types that power Structr. The REST API allows you to create, read, update and delete any object in the database, as well as create and run business logic methods and maintenance tasks.

## Basics

### Endpoints

Structr automatically creates REST endpoints for all types in the data model. There are different types of endpoints: collection resources, which provide access to collections of objects of the corresponding type, and entity resources that allow you to read, update or delete individual objects.

### Transactions

All requests made via the REST interface are subject to the ACID principle, meaning each request is processed as a single atomic transaction. If a request is successful, all changes made within that request are guaranteed to be saved in the database. Conversely, if any part of the request fails, all changes are rolled back and the database remains in its previous state. This ensures data consistency even when multiple related objects are created or modified in a single request.

### How to access the API

To use the API, you need an HTTP client that supports GET, PUT, POST, PATCH (optional) and DELETE, and a JSON parser/formatter to process the response. Since these technologies are available in all modern browsers, it is very easy to work with the API in a web application using `fetch()` or libraries like jQuery. The Data Access article shows examples for different HTTP clients.

The example requests in this chapter use `curl`, a command-line tool available at [https://curl.haxx.se/download.html](https://curl.haxx.se/download.html). It might already be installed on your system.

>**Note:** The output of all REST endpoints can also be viewed with a browser. Browser requests send an `HTTP Accept:text/html` header which causes Structr to generate the output as HTML.

### Data Format

The REST API uses JSON to transfer data. The request body must be valid JSON and it can either be a single object or an array of objects. The response body of any endpoint will always be a JSON Result Object, which is a single object with at least a result entry.

### Objects

A valid JSON object consists of quoted key-value mappings where the values can be strings, numbers, objects, arrays or null. Date values are represented by the ISO 8601 international date format string.

    {
        "id": "a01e2889c25045bdae6b33b9fca49707",
        "name": "Project #1",
        "type": "Project",
        "description": "An example project",
        "priority": 2,
        "tasks": [
            {
                "id": "dfa33b9dcda6454a805bd7739aab32c9",
                "name": "Task #1",
                "type": "Task"
            },
            {
                "id": "68122629f8e6402a8b684f4e7681653c",
                "name": "Task #2",
                "type": "Task"
            }
        ],
        "owner": {},
        "other": null,
        "example": {
            "name": "A nested example object",
            "fields": []
        },
        "createdDate": "2020-04-21T18:31:52+0200"
    }

The JSON object above is a part of an example result produced by the `/Project` endpoint. You can see several different nested objects in the result: the root object is a Project node, the tasks array contains two Task objects, and the owner is an empty object because the view has no fields for this type. (All these details will be explained in the following sections).

### Nested Objects

One of the most important and powerful functions of the Structr REST API is the ability to transform nested JSON objects into graph structures, and vice versa. The transformation is based on contextual properties in the data model, which encapsulate the relationships between nodes in the graph.

You can think of the data model as a blueprint for the structures that are created in the database. If you specify a relationship between two types, each of them gets a contextual property that manages the association. In this example, we use Project and Task, with the following schema.

## Input and Output

The general rule is that the input format for objects is identical to the output format, so you can use (almost) the same JSON to create or update objects as you get as a response.

For example, when you create a new project, you can specify an existing Task object in the tasks array to associate it with the project. You can leave out the id and type properties for the new object, because they are filled by Structr once the object is created in the database.

### Create a new object and associate an existing task

    {
        "name": "Project #2",
        "description": "A second example project",
        "priority": 1,
        "tasks": [
            {
            "id": "dfa33b9dcda6454a805bd7739aab32c9",
            }
        ]
    }

### Reference by UUID

The reference to an existing object is established with the id property. The property contains the UUID of the object, which is the primary identifier. Because the UUID represents the identity, you can use it instead of the object itself when specifying a reference to an existing object, like in the following example.

#### Short Form

    {
        "name": "Project #2",
        "description": "A second example project",
        "priority": 1,
        "tasks": [ "dfa33b9dcda6454a805bd7739aab32c9" ]
    }

If you want to reference an existing object in JSON, you can use the UUID instead of the object itself.

### Reference by property value

It is also possible to use properties other than id to reference an object, for example name, or a numeric property like originId etc. The only requirement is a uniqueness constraint on the corresponding property to avoid ambiguity.

#### Short Form

    {
        "name": "Project #2",
        "description": "A second example project",
        "priority": 1,
        "tasks": [ { "name": "Task #1" } ]
    }

## Errors

If a request causes an error on the server, Structr responds with a corresponding HTTP status code and an error response object. You can find a list of possible status codes in the Troubleshooting Guide. The error response object looks like this.

#### Error Response Object

    {
        "code": 422,
        "message": "Unable to commit transaction, validation failed",
        "errors": [
            {
            "type": "Project",
            "property": "name",
            "token": "must_not_be_empty"
            }
        ]
    }

It contains the following fields:

|Name|Description|
|---|---|
|code|HTTP status code|
|message|Status message|
|errors|Array of error objects|


#### Error Objects

Error objects contain detailed information about an error. There can be multiple error objects in a single error response. An error object contains the following fields.

|Name|Description|
|---|---|
|type|Data type of the erroneous object|
|property|Name of the property that caused the error (optional)|
|token|Error token|
|details|Details (optional)|

>**Note:** If an error occurs in a request, the whole transaction is rolled back and no changes will be made to the database contents.

## Related Topics

- Authentication – Explains how to authenticate requests and configure access permissions for endpoints
- Data Access – Covers reading, creating, updating, and deleting objects through the API
- Views – Describes how to control which properties appear in API responses
