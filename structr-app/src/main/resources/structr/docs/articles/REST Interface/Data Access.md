This article explains how to read, create, update, and delete objects through the REST API. It assumes you understand the basics covered in the Overview article and have configured authentication as described in the Authentication article.

There are two different endpoint types in the REST API:

- Collection Endpoints – Access collections of objects, with support for pagination, searching, and filtering
- Entity Endpoints – Access individual objects by UUID, including executing methods

The examples in this chapter use HTTP header-based authentication with the default admin user that Structr creates on first startup.

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
|`result`|Array of result objects|
|`query_time`|Time it took to run the query (in seconds)|
|`result_count`|Number of results in the database (if fewer than the soft limit)|
|`page_count`|Number of pages in a paginated result|
|`result_count_time`|Time it took to count the result objects (in seconds)|
|`serialization_time`|Time it took to serialize the JSON response (in seconds)|

### Pagination

The number of results returned by a GET request to a collection resource can be limited with the request parameter `_pageSize`. This so-called pagination depends on a combination of the parameters `_pageSize` and `_page`. The _page parameter has a default value of 1, so you can omit it to get the first page of the paginated results.

>**Note:** Starting with Structr 4.0, the built-in request parameters must be prefixed with an underscore, i.e. pageSize becomes _pageSize. This change was introduced to prevent name clashes with user-defined attributes. If your installation runs on a version prior to 4.0, simply omit the underscore.

#### Fetch the First 10 Projects

    $ curl -s -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/Project?_pageSize=10

#### Fetch the Next 10 Projects

To fetch the next 10 results, you can use the page parameter with a value of 2:

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?_pageSize=10&_page=2"

Please note that the ampersand character & has a special meaning in some command-line environments, so you might need to put the URL string in quotes.

>**Note:** As of Structr version 3.4.3, the total number of results returned in a single GET request to a collection resource is soft-limited to 10,000 objects. This limit can be overridden by requesting a larger _pageSize, or by increasing the global soft limit in the Configuration Tool.

### Sorting

By default, the results of a GET request are *unordered*, and the desired sort order can be controlled with the (optional) parameters `_sort` and `_order`. You can sort the result by one or more indexed property value (including Function Property results), in ascending (_order=asc) or descending order (_order=desc), as shown in the following examples. The default sort order is ascending order. String sort order depends on the case of the value, i.e. upper case Z comes before lower case a.

To sort the result based on multiple fields, repeat the `_sort` and `_order` parameters in the request URL for each property you want to sort by. The order of appearance of the fields in the URL determines the priority.

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

Please note that you can only search for objects based on indexed and contextual properties, so make sure to set the indexed flag (and a type hint for Function Properties) in the data model. Setting the flag is necessary because Structr maintains indexes in the database to provide better search performance, and the creation and deletion of these indexes is coupled to indexed flag.

### Advanced Search Capabilities

Besides simple value-based filters, Structr also supports other search methods:

- inexact search
- geographical distance search
- range queries
- empty / non-empty values

#### Inexact Search

To switch from exact search (which is the default) to inexact search, you can add the request parameter `_inexact=1` to the URL.

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?name=Proj&_inexact=1"

This query will return all the project nodes whose name contain the string “proj”. The search value is automatically converted to lower case. Please also note that the search applies to the whole string, so it doesn’t mean “begins with”, but “contains”.

#### Distance Search

Distance search is based on the value of the properties latitude and longitude, which can be made available by extending the internal type Location in the data model. If a node has latitude and longitude, it can be found with a distance search as shown in the following example.

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Hotel?_latlon=50.1167851,8.7265218&_distance=0.1"

The parameters that activate distance search are `_latlon` and `_distance`. Latlon indicates the latitude and longitude values of the search origin, separated by a comma, and distance indicates the desired circular search radius around the given location, in kilometers.

#### Range Filters

To filter the result based on a range of property values, you can use the following syntax: `[<start> TO <end>]`. Please note that you need to do URL encoding for the spaces in the range expression if you construct the URL string manually.

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

### More Search Options

If you need more search options, e.g. search on multiple types, faceted search etc., have a look at the following options:

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

The example request causes the schema method `myUpdateMethod` to be executed on the project node with the UUID c431c1b5020f4430808f9b330a123159, with the parameters parameter1=test, parameter2=123.

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

The response of a maintenance command will always be 200 OK with an empty response object. Most commands will send their log output to the structr log file.

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

The next request selects the info view, so the result object is different from the previous one. Note that view selection is important in fetch() and jQuery $.ajax() as well, because the view controls which properties are available in the resulting JSON object.

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

With the query parameter `_outputNestingDepth` the output depth of the result JSON can be adjusted to the desired level.

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

    fetch('/structr/rest/Project?name=' + encodeURIComponent(name) + '&_inexact=1', {
        method: 'DELETE',
        credentials: 'include',
    }).then(function(response) {
        return response.json();
    }).then(function(json) {
        // process result
    });

#### jQuery

    $.ajax({
        url: '/structr/rest/Project?name=' + encodeURIComponent(name) + '&_inexact=1',
        method: 'DELETE',
        statusCode: {
            200: function(result) {
                // process result
            }
        }
    });

#### curl

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?name=New&_inexact=1" -XDELETE

### Deleting All Objects Of The Same Type

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project"

This will delete all objects in the target resource.

>**Note:** Beware that this will also delete all objects of inheriting types!

To only delete objects of a certain type without deleting inheriting types, we can use the internal type attribute as an additional filter as shown in the next query.

    $ curl -s -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/Project?type=Project"

## Related Topics

- Authentication – Authentication methods and access permissions
- Data Model – Defining types, properties, and relationships that the API exposes
