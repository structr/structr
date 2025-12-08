# Backend

This chapter describes how to create and access the integrated RESTful API that is the core backend of any Structr application.

For more a more general view on how data is modeled and stored in Structr, please see the articles about [the Data model](/2-Core concepts/1-Database.md).

## Overview

The REST interface is the universal application programming interface (API) in Structr. It provides access to all types in the data model, including the internal types that power Structr. The REST API allows you to create, read, update and delete any object in the database, as well as create and run business logic methods and maintenance tasks.

## Endpoints

Structr automatically creates REST endpoints for all types in the data model. There are different types of endpoints: collection resources, which provide access to collections of objects of the corresponding type, and entity resources that allow you to read, update or delete individual objects. To learn more about the basics of REST APIs in Structr, please read the chapter about the Structr REST API in the Fundamental Concepts document.

## Fundamental Concept: REST API

### How to access the API

To use the API, you need an HTTP client that supports GET, PUT, POST, PATCH (optional) and DELETE, and a JSON parser/formatter to process the response. Since these technologies are available in all modern browsers, it is very easy to work with the API in a web application.

Below are examples for `fetch()` and JQuery's `$.ajax()` that you can use in your client-side Javascript code. Using Javascript has the advantage that the result objects are usable without transformation or manual parsing, as shown in the example code.

#### fetch()

    fetch('/structr/rest/Project', {
        method: 'GET',
        credentials: 'include'
    }).then(function(response) {
        return response.json();
    }).then(function(json) {
        json.result.forEach(project => {
        // process result
        });
    });

#### jQuery

    $.ajax({
        url: '/structr/rest/Project',
        method: 'GET',
        statusCode: {
            200: function(json) {
                json.result.forEach(function(project) {
                // process result
                });
            }
        }
    });

#### curl

The example requests in this chapter are made with `curl`, which is a command-line tool available at [https://curl.haxx.se/download.html](https://curl.haxx.se/download.html). It might already be installed on your system, but you can of course use any other REST client - or even your browser - to follow the examples in this chapter.

>**Note:** The output of all REST endpoints can also be viewed with a browser. Browser requests send an `HTTP Accept:text/html` header which causes Structr to generate the output as HTML.

### JSON

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

### Description

The JSON object above is a part of an example result produced by the /Project endpoint. You can see several different nested objects in the result: the root object is a Project node, the tasks array contains two Task objects, and the owner is an empty object because the view has no fields for this type. (All these details will be explained in the following sections).

### Nested Objects

One of the most important and powerful functions of the Structr REST API is the ability to transform nested JSON objects into graph structures, and vice-versa. The transformation is based on contextual properties in the data model, which encapsulate the relationships between nodes in the graph.

You can think of the data model as a blueprint for the structures that are created in the database. If you specify a relationship between two types, each of them gets a contextual property that manages the association. In this example, we use Project and Task, with the following schema.

## Schema

![Schema](../schema-project.png)

### Input and Output

The general rule is that the input format for objects is identical to the output format, so you can use (almost) the same JSON to create or update objects as you get as a response.

For example, when you create a new project, you can specify an existing Task object in the tasks array to associate it with the project. You can leave out the id and type properties for the new object, because they are filled by Structr once the object is created in the database.

#### Create a new object and associate an existing task

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

## Getting Data

You can use the HTTP GET method to fetch a JSON result from an endpoint, as shown in the following examples. For the sake of readability, the example requests in this chapter are made with a command-line tool, although it is very easy to use the REST API directly from within your browser.

There are two different endpoint types in the REST API:

- Collection Endpoints
- Entity Endpoints

>**Note:** The examples in this chapter use HTTP header-based authentication with the default admin user that Structr creates when you start it for the first time. You can read more about the different authentication methods in the Security article or in the Authentication section below.

[Security](/4-Advanced topics/4-Security guide.md)

[Authentication](#authentication)

## Collection Endpoints

Collection endpoints provide access to collections of objects and support pagination, searching, filtering and bulk editing. The example request below accesses the endpoint for the type Project and fetches all existing objects.

### Request

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project

If the request is successful, the result object contains one or more objects and some metadata.

### Response

    {
        "result": [
            {
                "id": "c624511baa534ece9678f9d212711a9d",
                "type": "Project",
                "name": "Project #1"
            },
            {
                "id": "85889e5ff1cd4bb39dc32076516504ce",
                "type": "Project",
                "name": "Project #2"
            }
        ],
        "query_time": "0.000026964",
        "result_count": 2,
        "page_count": 1,
        "result_count_time": "0.000076108",
        "serialization_time": "0.000425659"
    }

### Result Object

|Key|Description|
|---|---|
|result|Array of result objects|
|query_time|Time it took to run the query (in seconds)|
|result_count|Number of results in the database (if fewer than the soft limit)|
|page_count|Number of pages in a paginated result|
|result_count_time|Time it took to count the result objects (in seconds)|
|serialization_time|Time it took to serialize the JSON response (in seconds)|

### Pagination

The number of results returned by a GET request to a collection resource can be limited with the request parameter _pageSize. This so-called Pagination depends on a combination of the parameters _pageSize and _page. The _page parameter has a default value of 1, so you can omit it to get the first page of the paginated results.

>**Note:** Starting with Structr 4.0, the built-in request parameters must be prefixed with an underscore, i.e. pageSize becomes _pageSize. This change was introduced to prevent name clashes with user-defined attributes. If your installation runs on a version prior to 4.0, simply omit the underscore.

#### Fetch the First 10 Projects

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project?_pageSize=10

#### Fetch the Next 10 Projects

To fetch the next 10 results, you can use the page parameter with a value of 2:

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?_pageSize=10&_page=2"

Please note that the ampersand character & has a special meaning in some command-line environments, so you might need to put the URL string in quotes.

>**Note:** As of Structr version 3.4.3, the total number of results returned in a single GET request to a collection resource is soft-limited to 10,000 objects. This limit can be overridden by requesting a larger _pageSize, or by increasing the global soft limit in the Configuration Tool.

### Sorting

By default, the results of a GET request are *unorderd, and the desired sort order can be controlled with the (optional) parameters sort and order. You can sort the result by one or more indexed property value (including Function Property results), in ascending (_order=asc) or descending order (_order=desc), as shown in the following examples. The default sort order is ascending order. String sort order depends on the case of the value, i.e. upper case Z comes before lower case a.

To sort the result based on multiple fields, repeat the _sort and _order parameters in the request URL for each property you want to sort by. The order of appearance of the fields in the URL determines the priority.

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project?_sort=name
    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project?_sort=name&_order=desc
    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?_sort=status&_sort=name&_order=desc&_order=asc"

### Null Values

If a sorted collection includes objects with null values, those objects are placed at the end of the collection for ascending order (“nulls last”), or at the beginning of the collection for descending order.

### Empty Values

Empty string values are not treated like null, but are instead placed at the beginning of the collection for ascending order, or at the end of the collection for descending order.

### Searching
To search for objects with a specific value, you can simply put the property name with the desired value in a request parameter, as shown in the following example.

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project?name=Project1

These simple value-based filters can also be used on contextual properties, e.g. you can select all projects with a specific owner.

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project?owner=5c08c5bd41164e558e9388c22752d273

You can also search for multiple values at once, resulting in an “OR” conjunction, with the ; character between the values:

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?name=Project1;Project2"

### Indexing

Please note that you can only search for objects based on indexed and contextual properties, so make sure to set the indexed flag (and a type hint for Function Properties) in the data model. Setting the flag is necessary because Structr maintains indexes in Neo4j to provide better search performance, and the creation and deletion of these indexes is coupled to indexed flag.

### Advanced Search Capabilities

Besides simple value-based filters, Structr also supports other search methods:

- inexact search
- geographical distance search
- range queries
- empty / non-empty values

#### Inexact Search

To switch from exact search (which is the default) to inexact search, you can add the request parameter _loose=1 to the URL. (The name of the parameter was poorly chosen and will be replaced with a more fitting name in a future release.)

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?name=Proj&_loose=1"

This query will return all the project nodes whose name contain the string “proj”. The search value is automatically converted to lower case. Please also note that the search applies to the whole string, so it doesn’t mean “begins with”, but “contains”.

#### Distance Search

Distance search is based on the value of the properties latitude and longitude, which can be made available by extending the internal type Location in the data model. If a node has latitude and longitude, it can be found with a distance search as shown in the following example.

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Hotel?_latlon=50.1167851,8.7265218&_distance=0.1"

The parameters that activate distance search are _latlon and _distance. Latlon indicates the latitude and longitude values of the search origin, separated by a comma, and distance indicates the desired circular search radius around the given location, in kilometers.

#### Range Filters

To filter the result based on a range of property values, you can use the following syntax: [<start> TO <end>]. Please note that you need to do URL encoding for the spaces in the range expression if you construct the URL string manually.

Omitting results in a “lower than equal” search, omitting results in a “greater than equal” search. Omitting both does not raise an error, but does nothing.

>**Note:** The space character is still required.

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?priority=[1 TO 2]"

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?priority=[0 TO ]"

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?priority=[ TO 2]"

#### Date Ranges

The range filter is especially useful for date ranges as shown in the following example. The first query selects only those projects that were created in January 2020. The date format used in this example is the international date format as specified in ISO 8601. Please note that the date query string is timezone-specific, so your search results may vary. You can configure the timezone of your Structr installation in the start script.

>**Note:** The next query needs manual URL encoding if you want to test it with a command-line tool, or any other tool that does not encode URLs automatically.

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project?createdDate=[2020-01-01T00:00:00Z TO 2020-01-31T23:59:59Z]"

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project?createdDate=[ TO 2020-01-31T23:59:59Z]"

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project?createdDate=[2020-01-01T00:00:00Z TO ]"

#### Empty Values

To search for objects without a value for a given property, you simply put the name of the property in the URL, without specifying a value, which results in the following query.

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?name="

## More Search Options

If you need more search options, e.g. search on multiple types, facetted search etc., have a look at the the following options:

- create a common base type and use the collection resource of that type to search for objects of different types
- create a search page with an embedded script to run custom queries, process the results and return a JSON result
- use the Cypher endpoint to use graph queries

- Every type in the inheritance hierarchy defines its own collection resource, so you can access different types that share the same base class under a common URL.

## Entity Endpoints

Entity endpoints can be used to fetch the contents of a single object (using a GET request), to update an object (using PUT), or to delete an object (DELETE). Entity endpoints can also be used to execute schema methods. Access to entity endpoints and the corresponding responses are a little bit different to those of collection endpoints. The URL usually ends with a UUID, or with the name of method to be executed, and the response object contains only a single object instead of a collection.

### Fetching an object by UUID

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project/c431c1b5020f4430808f9b330a123159

Response

    {
        "result": {
            "id": "c431c1b5020f4430808f9b330a123159",
            "type": "Project",
            "name": "New Project 2"
        },
        "query_time": "0.000851438",
        "result_count": 1,
        "page_count": 1,
        "result_count_time": "0.000054867",
        "serialization_time": "0.000185543"
    }

>**Note:** The result key contains a single object instead of an array.

### Executing a schema method

You can execute schema methods by sending a POST request with optional parameters in the request body to the entity resource URL of an object, as shown in the following example.

The example request causes the schema method myUpdateMethod to be executed on the project node with the UUID c431c1b5020f4430808f9b330a123159, with the parameters parameter1=test, parameter2=123.

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project/c431c1b5020f4430808f9b330a123159/myUpdateMethod -XPOST -d '{
        "parameter1": "test",
        "parameter2: 123
    }'

#### Response

A schema method can return any value (including null, which results in an empty response object). Non-empty return values will be transformed to JSON objects and returned in the response. If the method runs without errors, the response status code is 200 OK and the response body contains the JSON result of the method.

In case of an error (syntax error, data error, call to the error method or failed assert calls), the response status code is the corresponding error code (401, 403, 404, 422 etc.) and the response body contains an error object.

### Executing a maintenance command

You can execute a maintenance command by sending a POST request to the maintenance endpoint of the corresponding maintenance command.

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/maintenance/rebuildIndex -XPOST

#### Response

The response of a maintenance command will always be 200 OK with an emtpy response object. Most commands will send their log output to the structr log file.

## View Selection

A View is a named group of properties that specifies the contents of an object in the JSON output. We recommend to create dedicated views for the individual consumers of your API, to optimize data transfer and allow for independent development and modification of the endpoints.

You can select any of the Views defined in the data model by appending its name to the request URL. If the URL does not contain a view, the public view is selected automatically.

### Default View (public)
    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project?_sort=name

#### Result
    {
        "result": [
            {
                "id": "d46a2d3b90c94e368a70bc30acd30572",
                "type": "Project",
                "name": "Project #1"
            },
            {
                "id": "6941a2af4c024b429ffc4851b404af72",
                "type": "Project",
                "name": "Project #2"
            },
            {
                "id": "8db88530ea5949ba89cef1234e04d8e4",
                "type": "Project",
                "name": "Project #3"
            }
       ],
        "query_time": "0.000122986",
        "result_count": 3,
        "page_count": 1,
        "result_count_time": "0.000062350",
        "serialization_time": "0.017995394"
    }

### Manual View Selection (info)

The next request selects the info view, so the result object is different than the previous one. Note that view selection is important in fetch() and jQuery $.ajax() as well, because the view controls which properties are available in the resulting JSON object.

#### fetch()

    fetch('/structr/rest/Project/info', {
        method: 'GET',
        credentials: 'include'
    }).then(function(response) {
        return response.json();
    }).then(function(json) {
        json.result.forEach(project => {
            // process result
        });
    });

#### jQuery

    $.ajax({
        url: '/structr/rest/Project/info',
        method: 'GET',
        statusCode: {
            200: function(result) {
                result.forEach(function(project) {
                    // process result
                });
            }
        }
    });

#### curl

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project/info?_sort=name

#### Result
    {
        "result": [
            {
                "id": "d46a2d3b90c94e368a70bc30acd30572",
                "type": "Project",
                "name": "Project #1",
                "tasks": [
                    {
                    "id": "4a6894302db74c94b989fcac7e68a38e",
                    "name": "Task #1",
                    "type": "Task"
                    }
                ],
                "description": "This is the description of the first project.",
                "owner": null,
                "priority": 2
            },
            {
                "id": "6941a2af4c024b429ffc4851b404af72",
                "type": "Project",
                "name": "Project #2",
                "tasks": [],
                "description": "My second project.",
                "owner": null,
                "priority": 3
            },
            {
                "id": "8db88530ea5949ba89cef1234e04d8e4",
                "type": "Project",
                "name": "Project #3",
                "tasks": [],
                "description": "Third project description.",
                "owner": null,
                "priority": 1
            }
        ],
        "query_time": "0.000159363",
        "result_count": 3,
        "page_count": 1,
        "result_count_time": "0.000065976",
        "serialization_time": "0.004041883"
    }

The selected view is applied to all objects in the result. This means that if your result contains objects with different types, the selected view must be defined on every type, otherwise you will get an empty result object for that type.

>**Note:** View selection takes precedence over all other options when resolving a URL. So if the name of a view matches the name of, for example, a property or nested type, the view is selected instead of the property.

### Output Depth of result

If a view is created on multiple node types and contains remote node objects, then the output depth of the result JSON is restricted to the level of 3 by default. This is to prevent the whole graph from being rendered, which could happen in some scenarios like trees for examples.

With the query paramteter _outputNestingDepth (outputNestingDepth for Structr versions < 4.x) the output depth of the result JSON can be adjusted to the desired level.

## Creating Objects

To create new objects in the database, you send a POST request to the collection resource endpoint of the target type. If the request body contains JSON data, the data will be stored in the new object. If the request does not contain a JSON body, it creates an “empty” object which only contains the properties that are assigned by Structr automatically.

### fetch()

    fetch('/structr/rest/Project/info', {
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({
            name: "New Project"
        }),
    }).then(function(response) {
        return response.json();
    }).then(function(json) {
        json.result.forEach(project => {
            // process result
        });
    });

### jQuery

    $.ajax({
        url: '/structr/rest/Project/info',
        method: 'POST',
        data: JSON.stringify({
            name: "New Project"
        }),
        statusCode: {
            201: function(result) {
                result.forEach(function(project) {
                    // process result
                });
            }
        }
    });

### curl

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project -XPOST -d '{ "name": "New Project" }'

### Response

If the request results in the creation of a new object, Structr responds with an HTTP status of 201 Created and the UUID of the new object in the result.

    {
        "result": [
            "b0f7e79a17c649a9934687554990acd5"
        ],
        "result_count": 1,
        "page_count": 1,
        "result_count_time": "0.000048183",
        "serialization_time": "0.000224176"
    }

## Creating Multiple Objects

You can create more than one object of the same type in a single request by sending an array of JSON objects in the request body, as shown in the next example. Doing so has the advantage that all objects are created in a single transaction, so either all objects are created, or none, if an error occurs. It is also much more efficient because the transaction overhead is incurred only once.

### fetch()

    fetch('/structr/rest/Project/info', {
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify([
            { "name": "New Project 2" },
            { "name": "Another project" },
            { "name": "Project X", "description": "A secret Project" }
        ]),
    }).then(function(response) {
        return response.json();
    }).then(function(json) {
        json.result.forEach(newProjectId => {
            // process result
        });
    });

### jQuery

    $.ajax({
        url: '/structr/rest/Project/info',
        method: 'POST',
        data: JSON.stringify([
            { "name": "New Project 2" },
            { "name": "Another project" },
            { "name": "Project X", "description": "A secret Project" }
        ]},
        statusCode: {
            201: function(result) {
                result.forEach(function(project) {
                    // process result
                });
            }
        }
    });

### curl
    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project -XPOST -d '
        [
        { "name": "New Project 2" },
        { "name": "Another project" },
        { "name": "Project X", "description": "A secret Project" }
        ]'

### Response

    {
        "result": [
            "c431c1b5020f4430808f9b330a123159",
            "4384f4685a4a41d09d4cfa1cb34c3024",
            "011c26a452e24af0a3973862a305907c"
        ],
        "result_count": 3,
        "page_count": 1,
        "result_count_time": "0.000038485",
        "serialization_time": "0.000152850"
    }

## Updating Objects

To update an existing object, you must know its UUID. You can then send a PUT request to the entity endpoint of the object which is either /structr/rest/<type>/<uuid> or /structr/rest/<uuid>. Both URLs are valid, but we recommend to use the typed endpoint whenever possible. The generic UUID resource /structr/rest/<uuid> can be used if you don’t know the type of the object you want to update.

### fetch()

    fetch('/structr/rest/Project/' + id, {
        method: 'PUT',
        credentials: 'include',
        body: JSON.stringify({
            "name": "Updated name"
        }),
    }).then(function(response) {
        return response.json();
    }).then(function(json) {
        // process result
    });

### jQuery

    $.ajax({
        url: '/structr/rest/Project/' + id,
            method: 'PUT',
            data: JSON.stringify({
            "name": "Updated name"
        }),
        statusCode: {
            200: function(result) {
                // process result
            }
        }
    });

### curl

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project/c431c1b5020f4430808f9b330a123159 -XPUT -d '{ "name": "Updated name" }'

The response of a successful PUT request contains the status code 200 OK with an empty result object.

### Updating Multiple Objects

To update multiple objects at once, you can use the HTTP PATCH method on the collection resource of the target type, as shown in the following example.

#### fetch()
    fetch('/structr/rest/Project', {
        method: 'PATCH',
        credentials: 'include',
        body: JSON.stringify([
            { "id": "6b0381c05a864bb0ab8dd5dcb937e391", "name": "New name" },
            { "id": "9acef23248f943f687e5d787202b9cda", "name": "Bulk Update" }
        ])
    }).then(function(response) {
        return response.json();
    }).then(function(json) {
        // process result
    });

#### jQuery

    $.ajax({
        url: '/structr/rest/Project',
        method: 'PATCH',
        data: JSON.stringify([
            { "id": "6b0381c05a864bb0ab8dd5dcb937e391", "name": "New name" },
            { "id": "9acef23248f943f687e5d787202b9cda", "name": "Bulk Update" }
        ]),
        statusCode: {
            200: function(result) {
                // process result
            }
        }
    });

#### curl
    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project -XPATCH -d '
        [
        { "id": "6b0381c05a864bb0ab8dd5dcb937e391", "name": "New name" },
        { "id": "9acef23248f943f687e5d787202b9cda", "name": "Bulk Update" }
        ]'

## Deleting Objects

To delete objects, you can send a DELETE request, either to the entity resource of an existing object, or to the collection resource of a type. If you want to delete more than one object, you can use DELETE analogous to GET to delete all objects that would be returned by a GET request, including filters but without pagination.

>**Note:** If you send an unintended DELETE request to the collection resource, for example, because you have not checked that the id parameter is empty, you delete all objects in that collection.

### fetch()

    fetch('/structr/rest/Project/' + id, {
        method: 'DELETE',
        credentials: 'include',
    }).then(function(response) {
        return response.json();
    }).then(function(json) {
        // process result
    });

### jQuery

    $.ajax({
        url: '/structr/rest/Project/' + id,
        method: 'DELETE',
        statusCode: {
            200: function(result) {
                // process result
            }
        }
    });

### curl

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project/c431c1b5020f4430808f9b330a123159 -XDELETE

The response of a successful DELETE request contains the status code 200 OK with an empty result object.

### Deleting Multiple Objects

#### fetch()

    fetch('/structr/rest/Project?name=' + encodeURIComponent(name) + '&_loose=1', {
        method: 'DELETE',
        credentials: 'include',
    }).then(function(response) {
        return response.json();
    }).then(function(json) {
        // process result
    });

#### jQuery

    $.ajax({
        url: '/structr/rest/Project?name=' + encodeURIComponent(name) + '&_loose=1',
        method: 'DELETE',
        statusCode: {
            200: function(result) {
                // process result
            }
        }
    });

#### curl

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?name=New&_loose=1" -XDELETE

### Deleting All Objects Of The Same Type

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project"

This will delete all objects in the target resource.

>**Note:** Beware that this will also delete all objects of inheriting types!

To only delete objects of a certain type without deleting inheriting types, we can use the internal type attribute as an additional filter as shown in the next query.

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?type=Project"

##  Authentication

The REST API supports the following authentication methods, including no authentication at all (anonymous access).

- HTTP headers
- Session cookies
- HTTP basic authentication

Additionally, REST endpoints are protected by a security layer called Resource Access Grants that depend on the type of user making the request.

### Resource Access Permissions 

In all the examples until now, we used the admin user to authenticate the request. The admin user is a special user that has access to all endpoints, database objects and functions. If you want a non-admin user to be able to fetch data from a REST endpoint, you need to allow access to each resource separately. This is what the so-called Resource Access Grants are for.

Consider the following request:

    $ curl -s http://localhost:8082/structr/rest/User
    {
        "code": 401,
        "message": "Forbidden",
        "errors": []
    }

You can see that access to the User collection was denied. If you look at the log file, you can see that there is a warning message because access to resources without authentication is prohibited by default:

    2020-04-19 11:40:15.775 [qtp1049379734-90] INFO  org.structr.web.auth.UiAuthenticator - Resource access grant found for signature 'User', but method 'GET' not allowed for public users.

### Signature

  Resource Access Grants consist of a signature and a set of flags that control access to individual REST endpoints. The signature of an endpoint is based on its URL, omitting any UUID, plus a special representation for the view, which is the view’s name, capitalized and with a leading underscore. The signature part of a schema method is equal to its name, but capitalized. The following table contains examples for different URLs and the resulting signatures.

| Type | URL | Signature |
| --- | --- | --- |
| Collection                  | /structr/rest/Project | Project |
| Collection with view        | /structr/rest/Project/ui | Project/_Ui |
| Collection with view        | /structr/rest/Project/info | Project/_Info |
| Object with UUID            | /structr/rest/Project/362cc05768044c7db886f0bec0061a0a | Project |
| Object with UUID and view   | /structr/rest/Project/362cc05768044c7db886f0bec0061a0a/info | Project/_Info |
| Subcollection               | /structr/rest/Project/362cc05768044c7db886f0bec0061a0a/tasks | Project/Task |
| Schema Method               | /structr/rest/Project/362cc05768044c7db886f0bec0061a0a/doUpdate | Project/DoUpdate |


If access to an endpoint is denied because of a missing Resource Access Permission, you can find the corresponding signature in the log file.

### Flags

The flags property of a Resource Access Grant is a bitmask, which means that it is based on an integer value where each bit controls one of the permissions. You can either set all flags at once with the corresponding integer value, or click the checkboxes to toggle the corresponding permission.

## Anonymous Access

HTTP Requests that are not authenticated by one of the possible authentication methods are so-called anonymous requests, and we call the corresponding user anonymous user or public user. Anonymous users are at the lowest level of the access control hierarchy.

### Endpoints

With the default configuration, anonymous users are not allowed to access any endpoints. If you want to allow anonymous access to an endpoint, you must grant permission explicitly, and separately for each HTTP method. This is what the “Non-authenticated Users” flags in Resource Access Grants are for.

#### Without Endpoint Access Permission

    $ curl -s http://localhost:8082/structr/rest/Project
    {
        "code": 401,
        "message": "Forbidden",
        "errors": []
    }

#### With Endpoint Access Permission

    $ curl -s http://localhost:8082/structr/rest/Project
    {
        "result": [],
        "query_time": "0.000127127",
        "result_count": 0,
        "page_count": 0,
        "result_count_time": "0.000199823",
        "serialization_time": "0.001092944"
    }

Now you are allowed to access the endpoint, but you still don’t see any data because no project nodes are visible for anonymous users.

### Database Contents

By default, any object in Structr's database is visible to its owner, admin users and users that are have been granted the read permission on the object.

An object created by an anonymous user (provided the request was allowed by a Resource Access Permission) is a so-called ownerless node.

You can configure permissions for ownerless nodes in the Configuration Tool, for example to allow not only read access, but write access as well.

> **Note:** An object must first be visible to the user who wants to change it.

### Visibility

Database objects can be made visible for all anonymous users with the visibleToPublicUsers flag. Visible in this case implies the read permission, i.e. the object appears in the result set, and all its local properties can be read. Please note that this flag does not imply visibility for non-admin users, which is controlled by visibleToAuthenticatedUsers.

## Authentication Headers

You can use the HTTP headers X-User and X-Password to provide username und password to a request. If the connection is secured with TLS, the headers are included in the encryption and your credentials are safe. Please note that the authentication headers must be sent with every request. If you don’t want to do that, you can switch to session-based authentication described below.

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project
    {
        "result": [
            {
            "id": "362cc05768044c7db886f0bec0061a0a",
            "type": "Project",
            "name": "Project #1"
            }
        ],
        "query_time": "0.000035672",
        "result_count": 1,
        "page_count": 1,
        "result_count_time": "0.000114435",
        "serialization_time": "0.001253579"
    }

### Authenticated Users

A request that includes authentication headers is an authenticated request, and we call the corresponding user authenticated user. Authenticated users are at a higher level in the access control hierarchy, and have their own permission set in Resource Access Grants.

#### Admin Users

The group of authenticated users is further divided into admin users and non-admin users. Admin users can generally create, read, modify and delete all nodes and relationships in the database, access all endpoints, modify the schema and execute maintenance tasks etc. You can enable the admin status for a user by setting the `isAdmin` flag on it.

>**Note:** The isAdmin flag is required for the user to be able to log into the Structr Admin UI.

#### Non-Admin Users

Authenticated users that don’t have the `isAdmin` flag set are non-admin users.

### Endpoints

With the default configuration, non-admin users are not allowed to access any endpoints. If you want to allow non-admin users access to an endpoint, you must grant permission explicitly, and separately for each HTTP method. This is what the “Authenticated Users” flags in Resource Access Grants are for.

### Database Contents

Non-admin users are subject to node-level security, which you can read more about in the Security chapter. In short, a node can have an owner and a set of optional Security Relationships that determine the permissions of a user or a group on that node. Security Relationships are direct relationships between a user and some other node.

## Security

### Visibility

Database objects can be made visible for all non-admin users with the `visibleToAuthenticatedUsers` flag. Visible in this case implies the read permission, i.e. the object appears in the result set, and all its local properties can be read. Please note that this flag does not imply visibility for anonymous users which is controlled by `visibleToPublicUsers`.

### When (not) to Use Authentication Headers

If you use authentication headers over an unencrypted remote connection, username and password are not protected. This is a big security risk, so do not use `http://` URLs for REST calls to remote servers.

Unencrypted connections should only be used when connecting to resources on the same server, i.e. when the URL starts with http://localhost…

>**Note:** Do not use authentication headers over unencrypted remote connections (http://…).

## Sessions

To use session-based authentication, a user can be logged by a request to the `login` endpoint, typically at `/structr/rest/login`, that returns a session cookie which is used to authenticate subsequent requests.

Since the `login` endpoint is a REST endpoint, you must create a Resource Access Permission with the special signature `_login` to allow the POST method for non-authenticated users.

### Login Permission

The special `_login` permission does not exist by default, so you must create it in order to use session-based authentication like described above.

Since the user making the request is not authenticated yet, you must allow `POST` for non-authenticated users.

### Login Request

    $ curl -si http://localhost:8082/structr/rest/login -XPOST -d '{ "name": "user", "password": "password" }'

>**Note:** The -i flag in the above curl command line causes the HTTP response headers to be printed, so we can examine the response.

### Response

    HTTP/1.1 200 OK
    Date: Fri, 24 Apr 2020 21:00:07 GMT
    Strict-Transport-Security: max-age=60
    X-Content-Type-Options: nosniff
    X-Frame-Options: SAMEORIGIN
    X-XSS-Protection: 1;mode=block
    Content-Type: application/json;charset=utf-8
    Set-Cookie: JSESSIONID=f49d1dbb60be23612b0820453d996e411vcm341pfmeoj1xgh7j37n9a277.f49d1dbb60be23612b0820453d996e41;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    X-Structr-Edition: Enterprise
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.4.18.v20190429)

    {
        "result": {
            "id": "0490bebcbc2f4018857a492c532334c2",
            "type": "User",
            "isUser": true,
            "name": "user"
        },
        "result_count": 1,
        "page_count": 1,
        "result_count_time": "0.000179484",
        "serialization_time": "0.000734390"
    }

The server responds with a set of HTTP headers that include a `Set-Cookie` header with the session ID of the authenticated session. The session handling will most likely be handled by the REST client of your choice, so you don’t have to do this manually.

### Logout

To log out of a session, you can send a POST request containing the session cookie to the `logout` endpoint, typically at `/structr/rest/logout`.

This endpoint needs a Resource Access Permission as well, so in order to use the `logout` endpoint, you have to create a Resource Access Permission with the signature `_logout` to allow `POST` for authenticated users.

## Errors

If a request causes an error on the server, Structr responds with a corresponding HTTP status code and an error response object. You can find a list of possible status codes in the Troubleshooting Guide. The error response object looks like this.

### Error Response Object

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


### Error Objects

Error objects contain detailed information about an error. There can be multiple error objects in a single error response. An error object contains the following fields.

|Name|Description|
|---|---|
|type|Data type of the erroneous object|
|property|Name of the property that caused the error (optional)|
|token|Error token|
|details|Details (optional)|

>**Note:** If an error occurs in a request, the whole transaction is rolled back and no changes will be made to the database contents.

## List of REST Endpoints

The following endpoints exist in addition to the endpoints that are maintained automatically based on the schema.

|URL|Description|Supported methods|
|---|---|---|
|/structr/rest/_env|Structr runtime environment information|GET|
|/structr/rest/_schema|Schema information endpoint|GET|
|/structr/rest/_schemaJson|Schema JSON output endpoint|GET, POST|
|/structr/rest/cypher|Direct Cypher query endpoint|GET, POST|
|/structr/rest/globalSchemaMethods/...|Root path for global schema method execution|POST|
|/structr/rest/login|Login endpoint|POST|
|/structr/rest/logout|Logout endpoint|POST|
|/structr/rest/token|JWT endpoint|POST|
|/structr/rest/maintenance|Root path for maintenance command execution|POST|
|/structr/rest/registration|Endpoint for the User Self-Registration process|POST|
|/structr/rest/reset-password|Endpoint for the Reset Password process|POST|

## List of supported HTTP Headers

The following HTTP headers are supported by the REST server and allow special configuration options.

|Header| Description |
|---|---|
|Accept|Allows you to specify content type and properties, see below|
|X-User|Username for header-based authentication|
|X-Password|Password for header-based authentication|

### Accept

You can use the `Accept` header to provide a special configuration option that allows you to specify the exact set of properties that are returned in a JSON response, even if the property is not a part of the selected view. You can do this by including the string properties=... with a comma-separated list of property names in the Accept header as shown in the following example.

    Accept: application/json; properties=id,type,name

### X-User

With the `X-User` header, you can specify the username of an existing account to authenticate the request.

### X-Password

With the `X-Password` header, you can specify the password of an existing account to authenticate the request.

