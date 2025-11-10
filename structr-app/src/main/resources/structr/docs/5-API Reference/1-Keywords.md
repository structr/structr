# Keywords

In Structr, a keyword is a reserved word that allows access to a constant value or system object within a code context.

## Basic usage and examples

If the value of a constant is an object, its contents can be accessed by using the so-called "dot notation", e.g. `page.name` returns the name of the current page.

>**Note:** Unless stated otherwise, Structr supports both the `snake_case` and `lowerCamelCase` naming styles.

Most keywords can be used in the same way regardless of the type of the script context, e.g. JavaScript or StructrScript. In a StructrScript context, they can be accessed directly StructrScript always returns the string value of an object or scalar value.

In a JavaScript context, they can be used either via the `$.get('<keyword>')` function or  also directly via `$.<keyword>` (since version 3.5).

>**Note** A StructrScript context is defined by single curly braces after a dollar sign (`${...}`). A JavaScript context is defined by opening and closing double curly braces after a dollar sign (`${{...}}`). There are a couple of keywords that exist only in JavaScript context. They are made available by functions.

The following examples show the direct access to keyword object values in StructrScript.

`${page.name}` Returns the name of the current page.
`${current.owner.id}` Returns the id of the owner of the current object.

The following examples show how to access keyword attributes via dot notation and the `$.get()` function in a JavaScript context.

|Example expression|Result|
|---|---|
|`${{ $.print($.get('page').name; }}`|Returns the name of the current page.|
|`${{ $.print($.page.name); }}`|Returns the name of the current page, equivalent to the example above.|
|`${{ $.print($.current.owner.id); }}`|Returns the id of the owner of the current object.|

The `$.print()` expression is necessary because in a JavaScript context, in contrast to the ScriptScript context, the result value is not automatically written to the output stream. 

>**Note:** Since version 3.5, direct access is recommended.

## A-E

### base_url
The `base_url`/`baseUrl` keyword returns the base URL for the application. The value is assembled from the protocol, hostname and port of the server instance Structr is running on and results in URLs in the notation `http(s)://<host>[:<port>]` depending on the configuration.

#### Examples

Given HTTPS is enabled, the server name is `foo.bar` and the configured port is `1234`,

`${base_url}` results in `https://foo.bar:1234` (without a trailing `/`). 

For only the host or port part, see [`host`](#host) and [`port`](#port).

>**Note:** If the configuration parameter `application.baseurl.override` is set in structr.conf, the set value will be returned.

>**Note:** If HTTPS is enabled, the base URL will always begin with `https://`.

>**Note:** If this keyword is used in a script called without a request (e.g. a Cron job), the configuration keys `application.host` and `application.http.port` (or `application.https.port`) are returned. If a request is available, the information will be taken from there.

### children
The `children` keyword returns the child nodes of the node the keyword is interpreted.

#### Examples
${render(children)}

>**Note:** This keyword is only available within the context of Page elements (DOM nodes like Content or Template nodes as well as DOM elements).

### current

The `current` keyword returns the object resolved looking up the second URL path part.

#### Examples

| Key               | Value                                                                                 |
|----|-------|
| **URL**           | https://myserver.mydomain.org/user/2de2042681c046deabc7323086e2a6ca                   |
| **Page name**     | `user`                                                                                |
| **Object UUID**   | 2de2042681c046deabc7323086e2a6ca                                                      |
| **Lookup result** | User object `{ "type": "User", "name": "Alice", "id": "2de2042681c046deabc7323086e2a6ca" }` |
| `${current.type}` | `User`                                                                                |
| `${current.name}` | `Alice`                                                                               |
| `${current.id}`   | `2de2042681c046deabc7323086e2a6ca`                                                    |

When requesting the example URL `https://myserver.mydomain.org/user/2de2042681c046deabc7323086e2a6ca`, Structr performs a lookup for page by the name `user` and for an object by the UUID `2de2042681c046deabc7323086e2a6ca`.  Under the assumption that a User object is found and the value of the `name` property is `Alice`, the expression `${current.name}` returns `Alice`.

The URL path resolution works by splitting the URL path at the path separator character `/`, interpreting the first part as the page name and the second part as the UUID, name or any other property key of any object in the database.

>**Note:** By default is configured to do a lookup by UUID or name. Other property keys have to be configured in `structr.conf` by using the setting `htmlservlet.resolveproperties`.

By using the UUID for looking up an object, the application can work with the page and the object. This principle is also known as "URL routing", and Structr follows a typical standard pattern. You can read more in the [Web stack](/docs?topic=2-Web%20stack) section.

### data

The `data` keyword returns the current element in an `each()` loop iteration or in a `filter()` expression. It is available in StructrScript only.

#### Examples

The following example expression in StructrScript makes all Task objects that begin with `a` visible to signed-in users:

    ${each(find('Task', 'name', starts_with('a')), set(data, 'visibleToAuthenticatedUsers', true))}

In JavaScript, the equivalent example is the following:

    ${{ $.find('Task', $.predicate.startsWith('name', 'a')).forEach(task => task.visibleToAuthenticatedUsers = true); }}

## F-O

### host

The `host` keyword returns the host name of the server to which the request was sent. It's the value of the part before `:` in the [HTTP header `Host`](https://developer.mozilla.org/de/docs/Web/HTTP/Reference/Headers/Host), if any, or the resolved server name, or the server IP address.

#### Examples

|---|---|---|
|**URL**|`${host}`|
|https://myserver.mydomain.org/user/2de2042681c046deabc7323086e2a6ca|`myserver.mydomain.org`|
|http://localhost:8082/structr/rest/Task/4a5adfac7c9645e19a8ac53d03eadd3f|`localhost`|
|http://192.168.12.34/user/2de2042681c046deabc7323086e2a6ca|`192.168.12.34`|

### id

The `id` keyword is short for `current.id` and returns the id of the object returned by URL resolution, if available.

### ip

The `ip` keyword returns the Internet Protocol (IP) address of the interface on which the request was received.

#### Examples

StructrScript: `${ip}`
JavaScript: `${{ return $.ip; }}`

|---|---|---|
|**URL**|`${ip}`|
|https://myserver.mydomain.org/user/2de2042681c046deabc7323086e2a6ca|IP address `myserver.mydomain.org` resolves to|
|http://localhost:8082/structr/rest/Task/4a5adfac7c9645e19a8a-c53d03eadd3f|`127.0.0.1`|
|http://192.168.12.34/user/2de2042681c046deabc7323086e2a6ca|`192.168.12.34`|

>**Note:** If the Structr server is running behind a proxy, the `ip` keyword returns the IP address of the local interface, typically `127.0.0.1`.

>**Note:** The `ip` keyword is only available from v3.6 and only available in a request context.

### input 

The `input` keyword returns the current object (of the Virtual Type) when used in a VirtualProperty context.

The so-called Virtual Types are non-persisting objects that have Virtual Property objects assigned to them which have input and output transformation functions. They can be accessed in a similar way to the persisting Schema Types but function as interface endpoints for ETL and data processing.  

### link

The `link` keyword references a linked file. 

#### Examples

`<link href="${link.path}" rel="stylesheet">` results in `<link href="/css/custom.css" rel="stylesheet">` if the `link` element is linked to the CSS file at `/css/custom.css`. 

>**Note**: The `link` keyword is only supported in the context of `a`, `link`, `script` and `img` elements and only returns a value if the element is actually linked to a file which is visible to the accessing user.
 
To manage the relationship between the element and the file, use the link icon which is displayed when hovering over a node.

### locale

The `locale` keyword returns the I18N locale string of the current request or session, derived from different possible input values, ordered by the following priority (highest priority first):

1. Request parameter `_locale` (example: `https://localhost:8082/foo?_locale=en_UK`)
2. User locale (StructrScript example: `${set(me, 'locale', 'fr_FR'}`)
3. Cookie locale (cookie name `locale`)
4. Browser locale (as set in the language settings of the accessing browser)
5. Default locale which was used to start the Java process (evaluated via `java.util.Locale.getDefault()`)

>**Note:** When processing the input value, Structr always replaces the dash character (`-`) by the underscore character (`_`) for consistency reasons.

### me

The `me` keyword references the current user object as stored in the database.

The keyword can be used in any code context like HTML element attributes, Template expressions, Hide and Show Conditions etc. It is often used to show/hide individual parts of a page depending on the permissions / status of a user etc.

#### Examples

`Hello ${me.name}!` => `Hello admin!`

>**Note:** In an anonymous context, the `me` keyword returns an empty result or `null`.

>**Note:** In a privileged context, the `me` keyword returns an instance of the SuperUser class. It is not recommended to work with this.


## P-R

### page

The `page` keyword returns the page object currently being rendered.

#### Examples

Given the example URL `https://localhost:8082/people?sort=lastName&order=asc`:

`${page.name}` => `people`

>**Note:** The `page` keyword is only available in the context of a page.

### parameter_map

The `parameter_map`/`parameterMap` returns a map (String, String) containing all request parameters of the current request.

#### Examples

URL `https://localhost:8082/people?sort=lastName&order=asc`:

`${parameter_map}` => `{ "sort": [ "lastName" ], "order": [ "asc" ]}`

>**Note:** Request parameter values are always arrays.

URL `https://localhost:8082/people?sort=lastName&sort=firstName&order=asc`:

`${parameter_map}` => `{ "sort": [ "lastName", "firstName" ], "order": [ "asc" ]}`

### path_info

The `path_info`/`pathInfo` keyword returns any extra path information associated with the URL the client sent when it made this request.

The extra path information follows the servlet path but precedes the query string and will start with a `/` character.

### port

The `port` keyword returns the port number to which the request was sent.

It is the value of the part after `:` in the Host header value, if any, or the server port where the client connection was accepted on.

### query_string

The `query_string`/`queryString` returns the complete literal query string of the current request.

For a resolved map of the query request parameters, see the [`parameter_map`](#parameter-map) keyword above.

#### Examples

URL `https://localhost:8082/people?sort=lastName&sort=firstName&order=asc`

`${query_string}` => `sort=lastName&sort=firstName&order=asc`

### request

The `request` keyword returns a reference to the current HTTP request object (see javax.servlet.http.HttpServletRequest). It can be used to access HTTP GET request parameters.

>**Note:** To access values in the body of an HTTP POST request, please use the `retrieve()` function.

If you are running methods from a Cron job, the request object is null, so if you are using that method in another context where you want to supply request parameters, in order for it to stay error-free in the Cron context, you need to null-check the request.

If a GET parameter occurs multiple times it will be made available as an array. If it is only present once, the value will be returned.

#### Examples

URL `https://localhost:8082/people?sort=lastName&order=asc`

`${request.sort}` => `lastName`

URL `https://localhost:8082/people?sort=firstName&sort=lastName&order=asc`

`${first(request.sort)}` => `firstName`

### remote_address

The `remote_address`/`remoteAddress` keyword returns the IP from which the current request originated.

#### Examples

When accessing a Structr instance running on your local machine, `${remote_address}` typically results in `127.0.0.1`.

### response

The `response` keyword returns a reference to the current output stream of the HTTP response that is sent to the client when the rendering process is finished.

The `response` keyword can be used in the special functions `create_jar_file()` and `exec_binary()` to allow direct streaming of binary data from a StructrScript to the client.

## S-Z

### status_code

The `status_code`/`statusCode` returns the HTTP status code for the current response.

### template

The `template` keyword returns the Template element closest to the current element in the page tree.

### tenant_identifier

The `tenant_identifier`/`tenantIdentifier` keyword returns the tenant identifier from structr.conf (database.tenant.identifier).

### value

The `value` keyword returns the value passed to the write function of a FunctionProperty.

>**Note:** This keyword is only available in the context of a write function of a FunctionProperty.

