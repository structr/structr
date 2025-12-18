# Keywords

## applicationRootPath
Refers to the root path of the Structr application.

The root path of the Structr application can be configured using the `application.root.path` setting in structr.conf, in case Structr is being run behind a reverse proxy. The default value of the setting is the empty string, i.e. no additional root path.

## applicationStore
Application-wide data store.

The application store can be used to store data in-memory as long as the instance is running. It can be accessed like a simple JavaScript object and can store primitive data and objects / arrays. Do NOT use the application store to store nodes or relationships since those are transaction-bound and cannot be cached.
### Notes
- Do NOT use the application store to store nodes or relationships since those are transaction-bound and cannot be cached.
- The keyword was introduced in version 4.0 and is not available in 3.x releases.
- Be aware that this consumes memory - storing big amounts of data is not recommended.

### Examples
#### Example 1 (JavaScript)
```
${{
	if (!$.applicationStore['code_was_run']) {
		$.log('running some code only once...');
		$.applicationStore['code_was_run'] = true;
	}
}}

```

## baseUrl
Refers to the base URL for this Structr application.

The value is assembled from the protocol, hostname and port of the server instance Structr is running on.

It produces `http(s)://<host>(:<port>)` depending on the configuration.";

### Notes
- If `application.baseurl.override` is set in structr.conf, the value of that setting will be returned.
- If HTTPS is enabled, the result string will always begin with https://
- If this keyword is used in a script called without a request (e.g. a CRON job), the configuration keys `application.host` and `application.http.port` (or `application.https.port`) are returned. If a request object is available, the information will be taken from there.


## children
Refers to the child nodes of the current node.
### Notes
- This keyword is only available in Page elements (DOM nodes).

### Examples
#### 1. (StructrScript) Render the HTML content of an element's children into the page
```
${render(children)}
```

## current
Refers to the object returned by URI Object Resolution, if available.

When a valid UUID is present in the URL of a page, Structr automatically retrieves the object associated with that UUID and makes it available to all scripts, templates, and logic executed during the page rendering process under the keyword `current`.
### Examples
#### 1. (Html) Print the name of the current object in page title and heading
```
<!DOCTYPE html>
<html>
	<head>
		<title>${current.name}</title>
	</head>
	<body>
		<h1>${current.name}</h1>
	</body>
</html>

```

## data
Refers to the current element in an `each()` loop iteration or in a `filter()` expression.

## host
Refers to the host name **of the server that Structr runs on**.
### Notes
- Only available in a context where Structr is responding to an HTTP request from the outside.


## id
Refers to the id of the object returned by URI Object Resolution, if available.

When a valid UUID is present in the URL of a page, Structr automatically retrieves the object associated with that UUID and makes its UUID available to all scripts, templates, and logic executed during the page rendering process under the keyword `id`.

## ip
Refers to the IP address of the interface on which the request was received.
### Notes
- Only available in a context where Structr is responding to an HTTP request from the outside.


## link
Refers to the linked filesystem element of an HTML element in a Page.

Only works in `a`, `link`, `script` or `img` tags/nodes. See Filesystem and Pages Tree View for more info.

The `link` keyword can only be accessed if a node of the above types is actually linked to a filesystem element. It can be linked via the link icon which is displayed when hovering over a node.

### Examples
#### 1. (Html) Provide a download link for a linked file
```
<!doctype html>
<html>
	<body>
		<a href="${link.path}">Download ${link.name}</a>
	</body>
</html>

```
#### 2. (Html) Display a linked image
```
<!doctype html>
<html>
	<body>
		<img src="/${link.id}" />
	</body>
</html>

```

## locale
Refers to the current locale.

The locale of a request is determined like this in descending priority:

1. Request parameter `locale`
2. User locale
3. Cookie `locale`
4. Browser locale
5. Default locale which was used to start the java process (evaluated via `java.util.Locale.getDefault();`)

### Examples
#### 1. (JavaScript) Print the current locale of a request to the log file
```
${{
	$.log('Current locale is: ' + $.locale);
}}

```

## me
Refers to the current user.

The `me` keyword allows you to access the user in the current request. It is often used to show/hide individual parts of a page depending on the permissions / status of a user etc.
### Notes
- The `me` keyword can be undefined in anonymous requests.

### Examples
#### Example 1 (Html)
```
<!doctype html>
<html>
	<body>
		<h1>Welcome ${me.name}!</h1>
	</body>
</html>

```
#### 2. (StructrScript) Check the `isAdmin` flag of the current user
```
${me.isAdmin}
```

## methodParameters
Refers to the arguments a method was called with.

The `methodParameters` keyword allows you to access the arguments of a method call.
### Examples
#### Example 1 (JavaScript)
```
${{
	let param1 = $.methodParameters.param1;
	let param2 = $.methodParameters.param2;
}}

```

## now
Refers to the current timestamp.

The `now` keyword allows you to access the current time and use it in calculations etc. This keyword is mainly used in StructrScript, because in JavaScript you can simply use `new Date()`.
### Examples
#### 1. (StructrScript) Display the current date, for example in an HTML attribute
```
${date_format(now, 'dd.MM.yyyy')}
```

## page
Refers to the current page in a page rendering context.

The `page` keyword allows you to access the current Page object that handles the request in which the current script is executed.
### Notes
- This keyword is only available in a Page rendering context.

### Examples
#### 1. (Html) Set that HTML page title to the name of the page that renders it
```
<!DOCTYPE html>
<html>
	<head>
		<title>${page.name}</title>
	</head>
</html>

```

## parameterMap
Refers to the HTTP request parameters of the current request.

The `parameterMap` keyword allows you to access the HTTP request parameters of the current request as a map, similar to the `request` keyword.

## parent
Refers to the parent element of the current in a page rendering context.

The `parent` keyword allows you to access the parent of the HTML element that is currently rendering.
### Examples
#### 1. (Html) Outputs a paragraph with the UUID of the H1 element above
```
<!DOCTYPE html>
<html>
	<body>
		<h1>Heading</h1>
		<p>Paragraph below ${parent.id}</p>
	</body>
</html>

```

## pathInfo
Refers to the HTTP path string of the current request.
### Notes
- Only available in a context where Structr is responding to an HTTP request from the outside.


## predicate
Refers to the set of query predicates for advanced `find()`.

**JavaScript only**


The `$.predicate` keyword allows you to access a set of query predicates for advanced `find()`.

The following predicates are available.

| Name | Description |
| --- | --- |
| `$.predicate.and()` | combine other predicates with AND |
| `$.predicate.or()` | combine other predicates with OR |
| `$.predicate.contains()` | contains query |
| `$.predicate.page()` | database-based pagination |
| `$.predicate.sort()` | database-based sorting |
| ... | ... |

### Notes
- This keyword is defined in JavaScript only.

### Examples
#### Example 1 (JavaScript)
```
${{
	let users = $.find('User', { eMail: $.predicate.contains('structr.com') });
}}

```

## queryString
Refers to the HTTP query string of the current request.

**StructrScript only**


The `queryString` keyword contains the raw query string, i.e. the part of the URL after the first `?` character, **excluding** the hash fragment (everything after the `#` character).
### Notes
- This keyword is defined in StructrScript only.
- Only available in a context where Structr is responding to an HTTP request from the outside.


## request
Refers to the current set of HTTP request parameters.

The `request` keyword allows you to access the URL parameters that were sent with the current HTTP request. This keyword is available in all custom methods and user-defined functions, as well as in Structr Pages and Dynamic Files.
### Notes
- Only available in a context where Structr is responding to an HTTP request from the outside.

### Examples
#### 1. (Html) Access request parameters in a Structr Page
```
<!DOCTYPE html>
<html>
	<head>
		<title>Hello ${request.name}!</title>
	</head>
	<body>
		<h1>Hello ${request.name}!</h1>
	</body>
</html>

```

## session
Refers to the current HTTP session.

The `session` keyword allows you to access the HTTP session, store data in it and query session metadata like the session ID, the creation time etc.

The following keys are read-only and return session metadata, all other keys can be used to store arbitrary data in the session.

| Name | Description | Type |
| ---| --- | --- |
| id | session ID | string |
| creationTime | creation timestamp (in milliseconds since epoch) | long |
| isNew | if the session was just created | boolean |
| lastAccessedTime | last access timestamp (milliseconds since epoch) | long |

### Notes
- Only available in a context where Structr is responding to an HTTP request from the outside.

### Examples
#### 1. (StructrScript) Log the session ID of the current request
```
${log(session.id)}
```
#### 2. (JavaScript) Store some arbitrary data in the current session
```
${{
	$.session.myData = 'test';
	$.session.cart = [ { name: 'item1', amount: 3 } ];
}}

```

## tenantIdentifier
Refers to the tenant identifier configured in structr.conf.

The tenant identifier is a configurable setting in structr.conf that allows you to run multiple Structr instances on a single Neo4j database. If the tenant identifier is configured, Structr adds it as a Neo4j label on all nodes and uses it in all queries, so that only those nodes and relationships are accessible that are labeled with the tenant identifier.

## this
Refers to the enclosing object instance of the currently executing method or script.
### Notes
- The value of `this` is `null` in user-defined functions and static methods.

### Examples
#### 1. (JavaScript) Example for an onCreate method that sets a default name if none was given
```
{
	if ($.empty($.this.name)) {
		$.this.name = 'Unnamed';
	}
}

```
