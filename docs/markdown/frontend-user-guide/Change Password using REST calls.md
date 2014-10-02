First lookup the admin user (or any other user you like to change the password for):

    $ curl -HX-User:admin -HX-Password:admin http://<hostname>:<port>/structr/rest/users?name=admin
    {
        "query_time": "0.002282709",
        "result_count": 1,
        "result": [
        {
            "type": "User",
            "name": "admin",
            "salutation": null,
            "firstName": null,
            "middleNameOrInitial": null,
            "lastName": null,
            "id": "f02e59a47dc9492da3e6cb7fb6b3ac25"
        }],
        "serialization_time": "0.000288091"
    }
    
Then issue a PUT request on the object, using the UUID of the user as reference.

<p class="info">In Structr, all database objects have a unique ID (<a target="_blank" href="http://en.wikipedia.org/wiki/Universally_unique_identifier#Version_4_.28random.29">random version 4 UUID</a> without hyphens).

    $ curl -i -HX-User:admin -HX-Password:admin -XPUT 
    http://<hostname>:<port>/structr/rest/users/f02e59a47dc9492da3e6cb7fb6b3ac25 
    -d '{"password":"secret"}'

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=1vld2piha8che7mhlo1lt0agr;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=UTF-8
    Content-Length: 0
    Server: Jetty(9.1.3.v20140225)
    
If we issue the GET request from above again, using the old credentials admin/admin, we see that we're not able see the user data anymore.

    $ curl -i -HX-User:admin -HX-Password:admin http://<hostname>:<port>/structr/rest/users?name=admin
    HTTP/1.1 401 Unauthorized
    Set-Cookie: JSESSIONID=1mebd77ksm9v2dwgxyc3r6tua;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Length: 133
    Server: Jetty(9.1.3.v20140225)
    
    {
        "code": 401,
        "message": "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!"
    }
    
Using the new password, it works:

    $ curl -i -HX-User:admin -HX-Password:secret http://<hostname>:<port>/structr/rest/users?name=admin
    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=wecd8ce161qb1m06g6v1c2r1j;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.1.3.v20140225)
    
    {
        "query_time": "0.002020017",
        "result_count": 1,
        "result": [
        {
            "type": "User",
            "name": "admin",
            "salutation": null,
            "firstName": null,
            "middleNameOrInitial": null,
            "lastName": null,
            "id": "f02e59a47dc9492da3e6cb7fb6b3ac25"
        }],
        "serialization_time": "0.000170156"
    }
