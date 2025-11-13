# Built-In functions
## DELETE
Sends an HTTP DELETE request with an optional content type to the given URL and returns the response headers and body.

This method can be used in a script to make an HTTP DELETE request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `DELETE()` method will return a response object with the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

### Notes
- The `DELETE()` method will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `add_header()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

### Signatures

```
DELETE(url [, contentType ])
```

### Examples
##### Example 1
```
${DELETE('http://localhost:8082/structr/rest/User/6aa10d68569d45beb384b42a1fc78c50')}
```

## GET
Sends an HTTP GET request to the given URL and returns the response headers and body.

This method can be used in a script to make an HTTP GET request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `GET()` method will return a response object with the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

### Notes
- From version 3.5 onwards, GET() supports binary content by setting the `contentType` parameter to `application/octet-stream`. (This is helpful when creating files - see example.)
- v4.0+: `contentType` can be used like the `Content-Type` header - to set the **expected** response mime type and to set the `charset` with which the response will be interpreted (**unless** the server sends provides a charset, then this charset will be used).
- Prior to v4.0: `contentType` is the **expected** response content type (it does not influence the charset of the response - the charset from the **sending server** will be used).
- The parameters `username` and `password` are intended for HTTP Basic Auth. For header authentication use `add_header()`.
- The `GET()` method will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `add_header()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

### Signatures

```
GET(GET(url [, contentType [, username, password]]))
GET(GET(url [, contentType [, username, password]]))
GET(GET(url, 'text/html', selector))
GET(GET(url, 'application/octet-stream' [, username, password]]))
```

### Examples
##### 1. Return an 'Access denied' error message with code 401 from the local Structr instance (depending on the configuration of that instance), because you cannot access the User collection from the outside without authentication.
```
${GET('http://localhost:8082/structr/rest/User')}
```
##### 2. Return the list of users from the local Structr instance (depending on the configuration of that instance).
```
${
	(
	  add_header('X-User', 'admin'),
	  add_header('X-Password', 'admin'),
	  GET('http://localhost:8082/structr/rest/User')
	)
}

```
##### 3. Return the HTML source code of the front page of google.com.
```
${GET('http://www.google.com', 'text/html')}
```
##### 4. Return the HTML source code of the front page of google.com (since the server sends a charset in the response, the given charset parameter is overridden).
```
${GET('http://www.google.com', 'text/html; charset=UTF-8')}
```
##### 5. Return the HTML source code of the front page of google.com (since the server sends a charset in the response, the given charset parameter is overridden).
```
${GET('http://www.google.com', 'text/html; charset=ISO-8859-1')}
```
##### 6. Return the HTML content of the element with the ID 'footer' from google.com.
```
${GET('http://www.google.com', 'text/html', '#footer')}
```
##### 7. Create a new file with the google logo in the local Structr instance.
```
${set_content(create('File', 'name', 'google_logo.png'), GET('https://www.google.com/images/branding/googlelogo/1x/googlelogo_light_color_272x92dp.png', 'application/octet-stream'))}
```

## HEAD
Sends an HTTP HEAD request with optional username and password to the given URL and returns the response headers.

This method can be used in a script to make an HTTP HEAD request **from within the Structr Server**, triggered by a frontend control like a button etc. The optional username and password parameters can be used to authenticate the request.

The `HEAD()` method will return a response object with the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |

### Notes
- The `HEAD()` method will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `add_header()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

### Signatures

```
HEAD(url [, username, password ])
```


## PATCH
Sends an HTTP PATCH request to the given URL and returns the response headers and body.

This method can be used in a script to make an HTTP PATCH request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `PATCH()` method will return a response object containing the response headers, body and status code. The object has the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

### Notes
- The `PATCH()` method will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `add_header()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

### Signatures

```
PATCH(url, body [, contentType, charset ])
```


## POST
Sends an HTTP POST request to the given URL and returns the response body.

This method can be used in a script to make an HTTP POST request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `POST()` method will return a response object containing the response headers, body and status code. The object has the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

The configMap parameter can be used to configure the timeout and redirect behaviour (e.g. config = { timeout: 60, redirects: true } ). By default there is not timeout and redirects are not followed.

### Notes
- The `POST()` method will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `add_header()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

### Signatures

```
POST(url, body [, contentType, charset, username, password, configMap ])
```


## POST_multi_part
Sends a multi-part HTTP POST request to the given URL and returns the response body.

This method can be used in a script to make a multi-part HTTP POST request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `POST()` method will return a response object containing the response headers, body and status code. The object has the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

The configMap parameter can be used to configure the timeout and redirect behaviour (e.g. config = { timeout: 60, redirects: true } ). By default there is not timeout and redirects are not followed.

### Notes
- The `POST()` method will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `add_header()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

### Signatures

```
POST_multi_part(url, partsMap, contentType)
```


## PUT
Sends an HTTP PUT request with an optional content type to the given URL and returns the response headers and body.

This method can be used in a script to make an HTTP PUT request **from within the Structr Server**, triggered by a frontend control like a button etc.

The `PUT()` method will return a response object with the following structure:

| Field | Description | Type |
| --- | --- | --- |
status | HTTP status of the request | Integer |
headers | Response headers | Map |
body | Response body | Map or String |

### Notes
- The `PUT()` method will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `add_header()` (see the related articles for more information).
- As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.

### Signatures

```
PUT(url, body [, contentType, charset ])
```


## abbr
Abbreviates the given string to the given length and appends the abbreviation (default = '…').


### Signatures

```
abbr(str, maxLength[, abbr = '…'])
```


## add
Returns the sum of the given arguments.


### Signatures

```
add(values...)
```


## add_header
Adds the given header field and value to the next request.


### Signatures

```
add_header(name, value)
```


## add_labels
Adds the given set of labels to the given node.


### Signatures

```
add_labels(node, labels)
```


## add_to_group
Adds a user to a group.


### Signatures

```
add_to_group(group, user)
```

### Parameters

|Name|Description|Optional|
|---|---|---|
|group|The group to add to|no|
|principal|The user or group to add to the given group|no|


## ancestor_types
Returns the names of the parent types of the given type and filters out all entries of the blacklist collection.


### Signatures

```
ancestor_types(type [, blacklist ])
```


## and
Returns the conjunction of the given arguments.


### Signatures

```
and(bool1, bool2, ...)
```


## append
Appends to the given file in the exchange directory.


### Signatures

```
append(fileName, text)
```


## append_content
Appends the content to the given file. Content can either be of type String or byte[].


### Signatures

```
append_content(file, content [, encoding=UTF-8 ])
```


## application_store_delete
Removes a stored value from the application level store.


### Signatures

```
application_store_delete(key)
```


## application_store_get
Retrieves a stored value from the application level store.


### Signatures

```
application_store_get(key)
```


## application_store_get_keys
Lists all keys stored in the application level store.


### Signatures

```
application_store_get_keys()
```


## application_store_has
Checks if a key is present in the application level store.


### Signatures

```
application_store_has(key)
```


## application_store_put
Stores a value in the application level store.


### Signatures

```
application_store_put(key, value)
```


## assert
Aborts the current request if the given condition evaluates to false.


### Signatures

```
assert(condition, statusCode, message)
```


## barcode
Creates a barcode of given type with the given data.


### Signatures

```
barcode(type, data [, width, height, hintKey, hintValue ])
```


## base64decode
Decodes the given base64-encoded value and returns a string.


### Signatures

```
base64decode(text [, scheme, charset ])
```


## base64encode
Encodes the given string and returns a base64-encoded string.


### Signatures

```
base64encode(text [, scheme, charset ])
```


## broadcast_event
Triggers the sending of a sever-sent event all authenticated and/or anonymous users with an open connection.


### Signatures

```
broadcast_event(eventType, message [, authenticatedUsers = true [ , anonymousUsers = false ]])
```


## bson
Creates BSON document from a map / object.


### Signatures

```
bson(data)
```


## call
Calls the given global schema method in the current users context.


### Signatures

```
call(functionName [, parameterMap ])
```


## call_privileged
Calls the given global schema method with a superuser context.


### Signatures

```
call_privileged(functionName [, parameterMap ])
```


## capitalize
Capitalizes the given string.


### Signatures

```
capitalize(str)
```


## ceil
Returns the smallest integer that is greater than or equal to the argument.


### Signatures

```
ceil(value)
```


## changelog
Returns the changelog object.


### Signatures

```
changelog(entity [, resolve=false [, filterKey, filterValue ]... ])
```


## clean
Cleans the given string.


### Signatures

```
clean(str)
```


## clear_headers
Clears headers for the next request.


### Signatures

```
clear_headers()
```


## coalesce
Returns the first non-null value in the list of expressions passed to it. In case all arguments are null, null will be returned.


### Signatures

```
coalesce(value1, value2, value3, ...)
```

### Parameters

|Name|Description|Optional|
|---|---|---|
|strings..|a list of strings to coalesce|no|

### Examples
##### 1. Returns either the name, the title or the UUID of a node, depending on which one is non-null
```
coalesce(node.name, node.title, node.id)
```
##### 2. Returns either the name, the title or the UUID of a node, depending on which one is non-null
```
$.coalesce(node.name, node.title, node.id)
```

## coalesce_objects
Returns the first non-null value in the list of expressions passed to it. In case all arguments are null, null will be returned.


### Signatures

```
coalesce_objects(obj1, obj2, obj3, ...)
```


## complement
Returns the complement of all lists.


### Signatures

```
complement(sourceList, obj, ...)
```


## concat
Concatenates all its parameters to a single string with the given separator.


### Signatures

```
concat(values...)
```


## config
Returns the structr.conf value for the given key.


### Signatures

```
config(configKey [, defaultValue ])
```


## confirmation_key
Creates a confirmation key to use as a one-time token. Used for user confirmation or password reset.


### Signatures

```
confirmation_key()
```


## contains
Returns true if the given string or collection contains an element.


### Signatures

```
contains(stringOrList, wordOrObject)
```


## copy_file_contents
Creates a copy of the file content linked to the given File entity and links it to the other File entity.


### Signatures

```
copy_file_contents(sourceFile, destinationFile)
```


## copy_permissions
Copies the security configuration of an entity to another entity.


### Signatures

```
copy_permissions(source, target [, overwrite ])
```


## create
Creates a new entity with the given key/value pairs in the database.


### Signatures

```
create(type [, parameterMap ])
```


## create_access_and_refresh_token
Creates an access token and an refresh token for the given user.


### Signatures

```
create_access_and_refresh_token(user, accessTokenTimeout, refreshTokenTimeout)
```


## create_access_token
Creates an access token for the given user.


### Signatures

```
create_access_token(user, accessTokenTimeout)
```


## create_archive
Packs the given files and folders into zipped archive.


### Signatures

```
create_archive(fileName, files [, customFileTypeName ])
```


## create_folder_path
Creates a new folder in the virtual file system including all parent folders if they don't exist already.


### Signatures

```
create_folder_path(type [, parameterMap ])
```


## create_or_update
Creates an object with the given properties or updates an existing object if it could be identified by a unique property.


### Signatures

```
create_or_update(type, propertyMap)
```


## create_relationship
Creates a relationship of the given type between two entities.


### Signatures

```
create_relationship(from, to, relType [, parameterMap ])
```


## create_zip
Create a ZIP archive file with the given files and folders.


### Signatures

```
create_zip(archiveFileName, files [, password [, encryptionMethod ] ])
```


## cypher
Returns the result of the given Cypher query.


### Signatures

```
cypher(query [, parameterMap, runInNewTransaction])
```


## date_add
Adds the given values to a date.


### Signatures

```
date_add(date, years[, months[, days[, hours[, minutes[, seconds]]]]])
```


## date_format
Formats the given value as a date string with the given format string.


### Signatures

```
date_format(value, pattern)
```


## decrypt
Decrypts the given string with a secret key from structr.conf or argument 2.


### Signatures

```
decrypt(value [, secret ])
```


## delete
Deletes the given entity from the database.


### Signatures

```
delete(objectOrList)
```


## delete_cache_value
Removes the cached value for the given key (if present).


### Signatures

```
delete_cache_value(cacheKey)
```


## disable_cascading_delete
Disables cascading delete in the Structr Backend for the current transaction.


### Signatures

```
disable_cascading_delete()
```


## disable_notifications
Disables the Websocket notifications in the Structr Ui for the current transaction.


### Signatures

```
disable_notifications()
```


## disable_prevent_duplicate_relationships
Disables the check that prevents the creation of duplicate relationships in the Structr Backend for the current transaction.


### Notes
- USE AT YOUR OWN RISK!

### Signatures

```
disable_prevent_duplicate_relationships()
```


## disable_uuid_validation
Disables the validation of user-supplied UUIDs when creating objects.


### Notes
- This is a performance optimization for large imports, use at your own risk!

### Signatures

```
disable_uuid_validation()
```


## div
Integer division, first argument / second argument.


### Signatures

```
div(value1, value2)
```


## doAs
Runs the given function in the context of the given user.


### Notes
- **Important**: Any node resource that was loaded outside of the function scope must be looked up again **inside** the function scope to prevent access problems.

### Signatures

```
doAs(user, function)
```

### Examples
##### Example 1
```
${{
    let user = $.find('User', { name: 'user_to_impersonate' })[0];

    $.doAs(user, () => {

        // code to be run as the given user
    });
}}

```

## doInNewTransaction
Runs the given function in a new transaction context.

This makes all sorts of use-cases possible, for example
batching of a given expression, i.e. if the expression contains a long-running function (for example the deletion of all nodes of a given type).
Useful in situations where large (or unknown) numbers of nodes are created, modified or deleted.

The paging/batching must be done manually.

The return value of the function (and of the error handler) determines if the processing continues or stops.
Returning a truthy value lets the function run again. Returning a truthy value in the error handler
lets the first function run again.

Be careful to never simply put "return true;" in the first function as this will create an infinite loop.

### Signatures

```
doInNewTransaction(workerFunction [, errorHandlerFunction])
```

### Parameters

|Name|Description|Optional|
|---|---|---|
|function|the lambda function to execute|no|
|errorHandler|an optional error handler that receives the error / exception as an argument|yes|

### Examples
##### Example 1
```
${{
    /* Iterate over all users in packets of 10 and do stuff */
    let pageSize = 10;
    let pageNo    = 1;

    $.doInNewTransaction(() => {
        let nodes = $.find('User', $.predicate.page(pageNo, pageSize));

        // compute-heavy stuff
        // ...

        pageNo++;

		// Only run again if nodes were found in the current iteration
        return (nodes.length > 0);

    }, (exception) => {

        $.log('Error occurred in batch function. Stopping.');
        return false;
    });
}}

```

## doPrivileged
Runs the given function in a privileged (superuser) context.

This can be useful in scenarios where no security checks should run (e.g. bulk import, bulk deletion).

**Important**: Any node resource, which was loaded outside of the function scope, must be looked up again inside the function scope to prevent access problems.

### Signatures

```
doPrivileged(function)
```

### Examples
##### Example 1
```
${{
	let userToDelete = $.find('User', { name: 'user_to_delete' })[0];

	$.doPrivileged(() => {

		// look up user again to set correct access rights
		let user = $.find('User', userToDelete.id);

		// delete all projects owned by user
		$.delete($.find('Project', { projectOwner: user }));

		// delete user
		$.delete(user);
	});
}}

```

## double_sum
Returns the sum of the given arguments as a floating-point number.


### Signatures

```
double_sum(list)
```


## empty
Returns true if the given value is null or empty.


### Signatures

```
empty(value)
```


## enable_cascading_delete
Enables cascading delete in the Structr Backend for the current transaction.


### Signatures

```
enable_cascading_delete()
```


## enable_notifications
Enables the Websocket notifications in the Structr Ui for the current transaction.


### Signatures

```
enable_notifications()
```


## encrypt
Encrypts the given string with a secret key from structr.conf or argument 2.


### Signatures

```
encrypt(value [, key])
```


## ends_with
Returns true if the given string ends with the given suffix.


### Signatures

```
ends_with(str, suffix)
```


## enum_info
Returns the enum values as an array.


### Signatures

```
enum_info(type, propertyName [, raw])
```


## equal
Returns true if the given arguments are equal.


### Signatures

```
equal(value1, value2)
```


## equal
Returns true if the given arguments are equal.


### Signatures

```
equal(value1, value2)
```


## error
Signals an error to the caller.


### Signatures

```
error(propertyName, errorToken [, errorMessage])
```


## escape_html
Replaces HTML characters with their corresponding HTML entities.


### Signatures

```
escape_html(string)
```


## escape_javascript
Escapes the given string for use with Javascript.


### Signatures

```
escape_javascript(string)
```


## escape_json
Escapes the given string for use within JSON.


### Signatures

```
escape_json(string)
```


## escape_xml
Replaces XML characters with their corresponding XML entities.


### Signatures

```
escape_xml(string)
```


## evaluate_script
Evaluates a serverside script string in the context of the given entity.


### Signatures

```
evaluate_script(entity, source)
```


## exec
Executes a script configured in structr.conf with the given configuration key, a collection of parameters and the desired logging behaviour, returning the standard output of the script. The logging behaviour for the command line has three possible values: [0] do not log command line [1] log only full path to script [2] log path to script and each parameter either unmasked or masked. In JavaScript the function is most flexible - each parameter can be given as a simple string or as a configuration map with a 'value' and a 'masked' flag.


### Signatures

```
exec(scriptConfigKey [, parameterCollection [, logBehaviour ] ])
```


## exec_binary
Executes a script configured in structr.conf with the given configuration key, a collection of parameters and the desired logging behaviour, returning the raw output directly into the output stream. The logging behaviour for the command line has three possible values: [0] do not log command line [1] log only full path to script [2] log path to script and each parameter either unmasked or masked. In JavaScript the function is most flexible - each parameter can be given as a simple string or as a configuration map with a 'value' and a 'masked' flag.


### Signatures

```
exec_binary(outputStream, scriptConfigKey [, parameterCollection [, logBehaviour ] ])
```


## extract
Returns a collection of all the elements with a given name from a collection.


### Signatures

```
extract(list, propertyName)
```


## find
Returns a collection of entities of the given type from the database, takes optional key/value pairs.


### Signatures

```
find(type, map)
find(type, key, value)
```

### Parameters

|Name|Description|Optional|
|---|---|---|
|type|the type to return (includes inherited types|no|
|predicates|a list of predicates|yes|

### Examples
##### 1. Returns the user with the given UUID.'.
```
$.find('User', '168f6c0b775a4118a160bf928fed8dae');
```
##### 2. Returns all users with the name 'tester'.
```
$.find('User', { name: 'tester' });
```

## find.and
Returns a query predicate that can be used with find() or search().


### Signatures

```
find.and(predicates)
```


## find.contains
Returns a query predicate that can be used with find() or search().


### Signatures

```
find.contains(key, value)
```


## find.empty
Returns a query predicate that can be used with find() or search().


### Signatures

```
find.empty(key)
```


## find.ends_with
Returns a query predicate that can be used with find() or search().


### Signatures

```
find.ends_with(key, value)
```


## find.equals
Returns a query predicate that can be used with find() or search().


### Signatures

```
find.equals(value)
```


## find.gt
Returns a gt predicate that can be used in find() function calls.


### Signatures

```
find.gt(value)
```


## find.gte
Returns a gte predicate that can be used in find() function calls.


### Signatures

```
find.gte(value)
```


## find.lt
Returns an lt predicate that can be used in find() function calls.


### Signatures

```
find.lt(value)
```


## find.lte
Returns an lte predicate that can be used in find() function calls.


### Signatures

```
find.lte(value)
```


## find.not
Returns a query predicate that can be used with find() or search().


### Signatures

```
find.not(predicate)
```


## find.or
Returns a query predicate that can be used with find() or search().


### Signatures

```
find.or(predicates)
```


## find.page
Returns a query predicate that can be used with find() or search().


### Signatures

```
find.page(page, pageSize)
```


## find.range
Returns a range predicate that can be used in find() function calls.


### Signatures

```
find.range(key, value)
```


## find.sort
Returns a query predicate that can be used with find() or search().


### Signatures

```
find.sort(key, value)
```


## find.starts_with
Returns a query predicate that can be used with find() or search().


### Signatures

```
find.starts_with(key, value)
```


## find.within_distance
Returns a query predicate that can be used with find() or search().


### Signatures

```
find.within_distance(latitude, longitude, distance)
```

### Parameters

|Name|Description|Optional|
|---|---|---|
|latitude|The latitude of the distance search|no|
|longitude|The longitude of the distance search|no|
|distance|The circumference of the distance search|no|


## find_privileged
Returns a collection of entities of the given type from the database, takes optional key/value pairs. Executed in a super user context.


### Signatures

```
find_privileged(type, options...)
```


## find_relationship
Returns a collection of entities of the given type from the database, takes optional key/value pairs.


### Signatures

```
find_relationship(type [, parameterMap ])
```


## first
Returns the first element of the given collection.


### Signatures

```
first(list)
```


## floor
Returns the largest integer that is less than or equal to the argument.


### Signatures

```
floor(value)
```


## formurlencode
Encodes the given object to an application/x-www-form-urlencoded string.


### Signatures

```
formurlencode(object)
```


## from_json
Parses the given JSON string and returns an object.


### Signatures

```
from_json(source)
```


## from_xml
Parses the given XML and returns a JSON representation of the XML.


### Signatures

```
from_xml(source)
```


## function_info
Returns information about the currently running Structr method, OR about the method defined in the given type and name.


### Signatures

```
function_info([type, name])
```


## geocode
Returns the geolocation (latitude, longitude) for the given street address using the configured geocoding provider.


### Signatures

```
geocode(street, city, country)
```


## get
Returns the value with the given name of the given entity, or an empty string.


### Signatures

```
get(entity, propertyName)
```


## get_available_serverlogs
Returns the last n lines from the server log file.


### Signatures

```
get_available_serverlogs()
```


## get_cache_value
Retrieves the cached value for the given key. Returns null if no cached value exists.


### Signatures

```
get_cache_value(key)
```


## get_content
Returns the content of the given file.


### Signatures

```
get_content(file [, encoding=UTF-8 ])
```


## get_cookie
Returns the requested cookie if it exists.


### Signatures

```
get_cookie(name)
```


## get_counter
Returns the value of the counter with the given index.


### Signatures

```
get_counter(level)
```


## get_incoming_relationships
Returns the incoming relationships of the given entity with an optional relationship type.


### Signatures

```
get_incoming_relationships(source, target [, relType ])
```


## get_or_create
Returns an entity with the given properties, creating one if it doesn't exist.


### Signatures

```
get_or_create(type, propertyMap)
```


## get_or_null
Returns the value with the given name of the given entity, or null.


### Signatures

```
get_or_null(entity, propertyName)
```


## get_outgoing_relationships
Returns the outgoing relationships of the given entity with an optional relationship type.


### Signatures

```
get_outgoing_relationships(source, target [, relType ])
```


## get_relationship_types
Returns the list of available relationship types form and/or to this node. Either potentially available (schema) or actually available (database).


### Signatures

```
get_relationship_types(node, lookupType [, direction ])
```


## get_relationships
Returns the relationships of the given entity with an optional relationship type.


### Signatures

```
get_relationships(source, target [, relType ])
```


## get_request_header
Returns the value of the given request header field.


### Signatures

```
get_request_header(name)
```


## get_session_attribute
Retrieve a value for the given key from the user session.


### Signatures

```
get_session_attribute(key)
```


## get_source
Returns the rendered HTML content for the given element.


### Signatures

```
get_source(element, editMode)
```


## getenv
Returns the value of the specified environment variable. If no value is specified, all environment variables are returned as a map. An environment variable is a system-dependent external named value.


### Signatures

```
getenv([variable])
```


## grant
Grants the given permissions on the given entity to a user.


### Signatures

```
grant(user, node, permissions)
```


## gt
Returns true if the first argument is greater than the second argument.


### Signatures

```
gt(value1, value2)
```


## gte
Returns true if the first argument is greater or equal to the second argument.


### Signatures

```
gte(value1, value2)
```


## has_cache_value
Checks if a cached value exists for the given key.


### Signatures

```
has_cache_value(key)
```


## has_css_class
Returns whether the given element has the given CSS class(es).


### Signatures

```
has_css_class(element, css)
```


## has_error
Allows checking if an error occurred in the scripting context.


### Signatures

```
has_error()
```


## has_incoming_relationship
Returns true if the given entity has incoming relationships of the given type.


### Signatures

```
has_incoming_relationship(source, target [, relType ])
```


## has_outgoing_relationship
Returns true if the given entity has outgoing relationships of the given type.


### Signatures

```
has_outgoing_relationship(source, target [, relType ])
```


## has_relationship
Returns true if the given entity has relationships of the given type.


### Signatures

```
has_relationship(source, target [, relType ])
```


## hash
Returns the hash (as a hexadecimal string) of a given string, using the given algorithm (if available via the underlying JVM).


### Signatures

```
hash(algorithm, value)
```


## hmac
Returns a keyed-hash message authentication code generated out of the given payload, secret and hash algorithm.


### Signatures

```
hmac(value, secret [, hashAlgorithm ])
```


## import_css
Imports CSS classes, media queries etc. from given CSS file.


### Signatures

```
import_css(file)
```


## import_html
Imports HTML source code into an element.


### Signatures

```
import_html(parent, html)
```


## inc_counter
Increases the value of the counter with the given index.


### Signatures

```
inc_counter(level [, resetLowerLevels=false ])
```


## include
Includes the content of the node with the given name (optionally as a repeater element).


### Signatures

```
include(name [, collection, dataKey])
```


## include_child
Includes the content of the child node with the given name (optionally as a repeater element).


### Signatures

```
include_child(name [, collection, dataKey])
```


## incoming
Returns the incoming relationships of the given entity.


### Signatures

```
incoming(entity [, relType ])
```


## index_of
Returns the position of the given word in the given string, or -1.


### Signatures

```
index_of(str, word)
```


## inheriting_types
Returns the names of the child types of the given type and filters out all entries of the blacklist collection.


### Signatures

```
inheriting_types(type [, blacklist ])
```


## insert_html
Inserts a new HTML subtree into the DOM.


### Signatures

```
insert_html(parent, html)
```


## instantiate
Instantiates the given Neo4j node into a Structr node.


### Signatures

```
instantiate(node)
```


## int
Converts the given string to an integer.


### Signatures

```
int(value)
```


## int_sum
Returns the sum of the given arguments as an integer.


### Signatures

```
int_sum(list)
```


## invalidate_cache_value
Invalidates the cached value for the given key (if present).


### Signatures

```
invalidate_cache_value(cacheKey)
```


## is_allowed
Returns whether the principal has all of the permission(s) on the given node.


### Signatures

```
is_allowed(user, node, permissions)
```


## is_collection
Returns true if the given argument is a collection.


### Signatures

```
is_collection(value)
```


## is_entity
Returns true if the given argument is a Structr entity.


### Signatures

```
is_entity(value)
```


## is_in_group
Returns true if a user is in the given group. If the optional parameter checkHierarchy is set to false, only a direct group membership is checked. Otherwise the group hierarchy is checked.


### Signatures

```
is_in_group(group, user [, checkHierarchy = false ])
```


## is_locale
Returns true if the current user locale is equal to the given argument.


### Signatures

```
is_locale(locales...)
```


## is_valid_email
Checks if the given address conforms to the syntax rules of RFC 822. The current implementation checks many, but not all, syntax rules. Only email addresses without personal name are accepted and leading/trailing fails validation.


### Signatures

```
is_valid_email(address)
```


## is_valid_uuid
Returns true if the given string is a valid UUID according to the configured UUID format. Returns false otherwise and if non-string arguments are given.


### Signatures

```
is_valid_uuid(string)
```


## jdbc
Fetches data from a JDBC source.


### Signatures

```
jdbc(jdbcUrl, sqlQuery[, username, password])
```


## job_info
Returns job information for the given job id - if the job does not exist, false is returned.


### Signatures

```
job_info(jobId)
```


## job_list
Returns a list of running jobs.


### Signatures

```
job_list()
```


## join
Joins all its parameters to a single string using the given separator.


### Signatures

```
join(list, separator)
```


## keys
Returns the property keys of the given entity.


### Signatures

```
keys(entity [, viewName ])
```


## last
Returns the last element of the given collection.


### Signatures

```
last(list)
```


## length
Returns the length of the given string.


### Signatures

```
length(str)
```


## localize
Returns a (cached) Localization result for the given key and optional domain.


### Signatures

```
localize(key [, domain ])
```


## log
Logs the given string to the logfile.


### Signatures

```
log(str)
```


## log_event
Logs an event to the Structr log.


### Signatures

```
log_event(action, message [, subject [, object ]])
```


## login
Logs the given user in if the given password is correct. Returns true on successful login.


### Signatures

```
login(user, password)
```


## long
Converts the given string to a long.


### Signatures

```
long(str)
```


## lower
Returns the lowercase value of its parameter.


### Signatures

```
lower(str)
```


## lt
Returns true if the first argument is less than the second argument.


### Signatures

```
lt(value1, value2)
```


## lte
Returns true if the first argument is less or equal to the second argument.


### Signatures

```
lte(value1, value2)
```


## maintenance
Executes a maintenance command.


### Signatures

```
maintenance(command [, key, value [, ... ]])
```


## max
Returns the larger value of the given arguments.


### Signatures

```
max(value1, value2)
```


## md5
Returns the MD5 hash of its parameter.


### Signatures

```
md5(str)
```


## merge
Merges the given collections / objects into a single collection.


### Signatures

```
merge(list1, list2, list3...)
```


## merge_properties
Copies property values from source entity to target entity, using the given list of keys.


### Signatures

```
merge_properties(source, target, keys)
```


## merge_unique
Merges the given collections / objects into a single collection, removing duplicates.


### Signatures

```
merge_unique(list1, list2, list3...)
```


## min
Returns the smaller value of the given arguments.


### Signatures

```
min(value1, value2)
```


## mod
Returns the remainder of the division.


### Signatures

```
mod(value1, value2)
```


## mongodb
Opens and returns a connection to an external MongoDB instance.


### Signatures

```
mongodb(url, database, collection)
```


## mult
Multiplies the first argument by the second argument.


### Signatures

```
mult(value1, value2)
```


## not
Negates the given argument.


### Signatures

```
not(bool)
```


## nth
Returns the element with the given index of the given collection.


### Signatures

```
nth(list, index)
```


## num
Converts the given string to a floating-point number.


### Signatures

```
num(str)
```


## number_format
Formats the given value using the given locale and format string.


### Signatures

```
number_format(value, locale, format)
```


## one
Checks if a number is equal to 1, returns the oneValue if yes, the otherValue if no.


### Signatures

```
one(number, oneValue, otherValue)
```


## or
Returns the disjunction of the given arguments.


### Signatures

```
or(bool1, bool2, ...)
```


## outgoing
Returns the outgoing relationships of the given entity.


### Signatures

```
outgoing(entity [, relType ])
```


## parse_date
Parses the given date string using the given format string.


### Signatures

```
parse_date(str, pattern)
```


## parse_number
Parses the given string using the given (optional) locale.


### Signatures

```
parse_number(number [, locale ])
```


## prefetch
Prefetches a subgraph.


### Signatures

```
prefetch(query, listOfKeys)
```


## print
Prints the given strings or objects to the output buffer.


### Signatures

```
print(objects...)
```


## property_info
Returns the schema information for the given property.


### Signatures

```
property_info(type, propertyName)
```


## quot
Divides the first argument by the second argument.


### Signatures

```
quot(value1, value2)
```


## random
Returns a random alphanumeric string of the given length.


### Signatures

```
random(length)
```


## random_uuid
Returns a random UUID.


### Signatures

```
random_uuid()
```


## read
Reads and returns the contents of the given file from the exchange directoy.


### Signatures

```
read(filename)
```


## remote_cypher
Returns the result of the given Cypher query against a remote instance.


### Signatures

```
remote_cypher(url, username, password, query [, parameterMap ])
```


## remove_dom_child
Removes a node from the DOM.


### Signatures

```
remove_dom_child(parent, child)
```


## remove_from_group
Removes the given user from the given group.


### Signatures

```
remove_from_group(group, user)
```


## remove_labels
Removes the given set of labels from the given node.


### Signatures

```
remove_labels(node, labels)
```


## remove_response_header
Removes the given header field from the server response.


### Signatures

```
remove_response_header(field)
```


## remove_session_attribute
Remove key/value pair from the user session.


### Signatures

```
remove_session_attribute(key)
```


## render
Renders the children of the current node.


### Signatures

```
render(list)
```


## replace
Replaces script expressions in the given template with values from the given entity.


### Signatures

```
replace(template, entity)
```


## replace_dom_child
Replaces a node from the DOM with new HTML.


### Signatures

```
replace_dom_child(parent, child, html)
```


## request_store_delete
Removes a stored value from the request level store.


### Signatures

```
request_store_delete(key)
```


## request_store_get
Retrieves a stored value from the request level store.


### Signatures

```
request_store_get(key)
```


## request_store_get_keys
Lists all keys stored in the request level store.


### Signatures

```
request_store_get_keys()
```


## request_store_has
Checks if a key is present in the request level store.


### Signatures

```
request_store_has(key)
```


## request_store_put
Stores a value in the request level store.


### Signatures

```
request_store_put(key, value)
```


## reset_counter
Resets the value of the counter with the given index.


### Signatures

```
reset_counter(level)
```


## retrieve
Returns the value associated with the given key from the temporary store.


### Signatures

```
retrieve(key)
```


## revoke
Revokes the given permissions on the given entity from a user.


### Signatures

```
revoke(user, node, permissions)
```


## rint
Returns a random integer in the given range.


### Signatures

```
rint(bound)
```


## rollback_transaction
Marks the current transaction as failed and prevents all objects from being persisted in the database.


### Signatures

```
rollback_transaction()
```


## round
Rounds the given argument to the nearest integer.


### Signatures

```
round(value [, decimalPlaces ])
```


## schedule
Schedules a script or a function to be executed in a separate thread.


### Signatures

```
schedule(script [, title ])
```


## search
Returns a collection of entities of the given type from the database, takes optional key/value pairs. Searches case-insensitve / inexact.


### Signatures

```
search(type, options...)
```


## search_fulltext
Returns a map of entities and search scores matching the given search string from the given fulltext index. Searches case-insensitve / inexact.


### Signatures

```
search_fulltext(indexName, searchString)
```


## search_relationships_fulltext
Returns a map of entities and search scores matching the given search string from the given fulltext index. Searches case-insensitve / inexact.


### Signatures

```
search_relationships_fulltext(indexName, searchString)
```


## send_event
Triggers the sending of a sever-sent event to a given list of recipients. The message will only be sent if they have an open connection.


### Signatures

```
send_event(eventType, message, recipient(s))
```


## send_html_mail
Sends an HTML e-mail.


### Signatures

```
send_html_mail(fromAddress, fromName, toAddress, toName, subject, htmlContent, textContent [, files])
```


## send_plaintext_mail
Sends a plaintext e-mail.


### Signatures

```
send_plaintext_mail(from, fromName, to, toName, subject, content)
```


## serverlog
Returns the last n lines from the server log file.


### Signatures

```
serverlog([ lines = 50 [, truncateLinesAfter = -1 [, logFile = '/var/log/structr.log' (default different based on configuration) ] ] ])
```


## set
Sets a value or multiple values on an entity. The values can be provided as a map or as a list of alternating keys and values.


### Signatures

```
set(entity, parameterMap)
```


## set_content
Sets the content of the given file. Content can either be of type String or byte[].


### Signatures

```
set_content(file, content[, encoding = "UTF-8"])
```


## set_cookie
Sets the given cookie.


### Signatures

```
set_cookie(name, value[, secure[, httpOnly[, maxAge[, domain[, path]]]]])
```


## set_details_object
Sets the given object as the detail object.


### Signatures

```
set_details_object(obj)
```


## set_encryption_key
Sets the secret key for encryt()/decrypt(), overriding the value from structr.conf.


### Signatures

```
set_encryption_key(secretKey)
```


## set_locale
Sets the locale in the current context to the given value.


### Signatures

```
set_locale(locale)
```


## set_log_level
Sets the application log level to the given level, if supported. Change takes effect immediately until another call is made or the application is restarted. On system start, the configuration value is used.


### Signatures

```
set_log_level(str)
```


## set_privileged
Sets the given key/value pair(s) on the given entity with super-user privileges.


### Signatures

```
set_privileged(entity, parameterMap)
```


## set_response_code
Sets the response code of the current rendering run.


### Signatures

```
set_response_code(code)
```


## set_response_header
Adds the given header field and value to the response of the current rendering run.


### Signatures

```
set_response_header(name, value [, override = false ])
```


## set_session_attribute
Store a value under the given key in the users session.


### Signatures

```
set_session_attribute(key, value)
```


## size
Returns the size of the given collection.


### Signatures

```
size(collection)
```


## sleep
Pauses the execution of the current thread for the given number of milliseconds.


### Signatures

```
sleep(milliseconds)
```


## sort
Sorts the given collection or array according to the given property key. Default sort key is 'name'.


### Signatures

```
sort(list [, propertyName [, descending=false] ])
```


## split
Splits the given string by the whole separator string.


### Signatures

```
split(str [, separator ])
```


## split_regex
Splits the given string by given regex.


### Signatures

```
split_regex(str [, regex ])
```


## stack_dump
Logs the current execution stack.


### Signatures

```
stack_dump()
```


## starts_with
Returns true if the given string starts with the given prefix.


### Signatures

```
starts_with(str, prefix)
```


## store
Stores the given value with the given key in the temporary store.


### Signatures

```
store(key, value)
```


## str_replace
Replaces each substring of the subject that matches the given regular expression with the given replacement.


### Signatures

```
str_replace(str, substring, replacement)
```


## strip_html
Strips all (HTML) tags from the given string.


### Signatures

```
strip_html(html)
```


## structr_env
Returns Structr runtime env information.


### Signatures

```
structr_env()
```


## substring
Returns the substring of the given string.


### Signatures

```
substring(str, start [, length ])
```


## subt
Subtracts the second argument from the first argument.


### Signatures

```
subt(value1, value2)
```


## system_info
Returns information about the system.


### Signatures

```
system_info()
```


## template
Returns a MailTemplate object with the given name, replaces the placeholders with values from the given entity.


### Signatures

```
template(name, locale, entity)
```


## timer
Starts/Stops/Pings a timer.


### Signatures

```
timer(name, action)
```


## titleize
Titleizes the given string.


### Signatures

```
titleize(str)
```


## to_date
Converts the given number to a date.


### Signatures

```
to_date(number)
```


## to_graph_object
Converts the given entity to GraphObjectMap.


### Signatures

```
to_graph_object(obj)
```


## to_json
Serializes the given object to JSON.


### Signatures

```
to_json(obj [, view, depth = 3, serializeNulls = true ])
```


## trim
Removes whitespace at the edges of the given string.


### Signatures

```
trim(str)
```


## type_info
Returns the type information for the specified type.


### Signatures

```
type_info(type [, view])
```


## unarchive
Unarchives given file to an optional parent folder.


### Signatures

```
unarchive(file, [, parentFolder ])
```


## unescape_html
Relaces escaped HTML entities with the actual characters, e.g. &lt; with <.


### Signatures

```
unescape_html(text)
```


## unlock_readonly_properties_once
Unlocks any read-only property for a single access.


### Signatures

```
unlock_readonly_properties_once()
```


## unlock_system_properties_once
Unlocks any system property for a single access.


### Signatures

```
unlock_system_properties_once()
```


## unwind
Converts a list of lists into a flat list.


### Signatures

```
unwind(list1, list2, ...)
```


## upper
Returns the uppercase value of its parameter.


### Signatures

```
upper(str)
```


## urlencode
URL-encodes the given string.


### Signatures

```
urlencode(str)
```


## user_changelog
Returns the changelog object.


### Signatures

```
user_changelog(user [, resolve=false [, filterKey, filterValue ]... ])
```


## validate_certificates
Disables or enables strict certificate checking when performing a request in a scripting context. The setting remains for the whole request.


### Signatures

```
validate_certificates(boolean)
```


## validate_email
Validates the given address against the syntax rules of RFC 822.

The current implementation checks many, but not all, syntax rules. If it is a valid email according to the RFC, nothing is returned. Otherwise the error text is returned.
### Signatures

```
validate_email(address)
```


## values
Returns the property values of the given entity.


### Signatures

```
values(entity, viewName)
```


## week_days
Calculates the number of week days (working days) between given dates.


### Signatures

```
week_days(from, to)
```


## write
Writes to the given file in the exchange directory.


### Signatures

```
write(fileName, text)
```


## xml
Parses the given string to an XML DOM.


### Signatures

```
xml(source)
```


## xpath
Returns the value of the given XPath expression from the given XML DOM. The optional third parameter defines the return type, possible values are: NUMBER, STRING, BOOLEAN, NODESET, NODE, default is STRING.


### Signatures

```
xpath(document, xpath [, returnType ])
```

