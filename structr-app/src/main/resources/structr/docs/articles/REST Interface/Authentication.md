# Authentication

The REST API supports the following authentication methods, including no authentication at all (anonymous access).

- HTTP headers
- Session cookies
- HTTP basic authentication

Additionally, REST endpoints are protected by a security layer called Resource Access Grants that depend on the type of user making the request.

## Resource Access Permissions 

Non-admin users require explicit permission to fetch data from REST endpoints. Resource Permissions define which endpoints each user or group can access. Consider the following request:

    $ curl -s http://localhost:8082/structr/rest/User
    {
        "code": 401,
        "message": "Forbidden",
        "errors": []
    }

You can see that access to the User collection was denied. If you look at the log file, you can see that there is a warning message because access to resources without authentication is prohibited by default:

    2020-04-19 11:40:15.775 [qtp1049379734-90] INFO  o.structr.web.auth.UiAuthenticator - Found no resource access permission for anonymous users with signature 'User' and method 'GET'.

### Signature

  Resource Access Grants consist of a signature and a set of flags that control access to individual REST endpoints. The signature of an endpoint is based on its URL, replacing any UUID with `_id`, plus a special representation for the view, which is the view’s name, capitalized and with a leading underscore.
  
The signature part of a schema method is equal to its name, but capitalized. The following table contains examples for different URLs and the resulting signatures.

| Type | URL | Signature            |
| --- | --- |----------------------|
| Collection                  | /structr/rest/Project | Project              |
| Collection with view        | /structr/rest/Project/ui | Project/_Ui          |
| Collection with view        | /structr/rest/Project/info | Project/_Info        |
| Object with UUID            | /structr/rest/Project/362cc05768044c7db886f0bec0061a0a | Project/_id          |
| Object with UUID and view   | /structr/rest/Project/362cc05768044c7db886f0bec0061a0a/info | Project/_id/_Info    |
| Subcollection               | /structr/rest/Project/362cc05768044c7db886f0bec0061a0a/tasks | Project/_id/Task     |
| Schema Method               | /structr/rest/Project/362cc05768044c7db886f0bec0061a0a/doUpdate | Project/_id/DoUpdate |

### Logging

If access to an endpoint is denied because of a missing Resource Access Permission, you can find the corresponding signature in the log file.

    Found no resource access permission for anonymous users with signature 'User/_id' and method 'GET'.

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