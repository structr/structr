Now we can of course modify the entity as well, using the `PUT` verb on the element resource that contains the newly created file.

    $ curl -si -HX-User:admin -HX-Password:admin -XPUT http://localhost:8082/structr/rest/files/559271b86c9a43efa9ebfb94eedc7b96 -d '{ "name": "renamed.txt" }'

and the server responds with:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=1sajlbgesemsizw0c8uhfc9l;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=UTF-8
    Content-Length: 0
    Server: Jetty(9.1.3.v20140225)

Now we access the **element resource** that contains only the modified element by addressing the nested resource directly.

    $ curl -si -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/files/559271b86c9a43efa9ebfb94eedc7b96

Note that the result in the JSON document is not an array since we're accessing the element directly using its UUID:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=tdojo6jg7cfr934ifv0ni527;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.1.3.v20140225)
    
    {
       "query_time": "0.003190386",
       "result_count": 1,
       "result": {
          "type": "File",
          "name": "renamed.txt",
          "contentType": "text/plain",
          "size": null,
          "url": null,
          "owner": {
             "type": "User",
             "name": "admin",
             "salutation": null,
             "firstName": null,
             "middleNameOrInitial": null,
             "lastName": null,
             "id": "f02e59a47dc9492da3e6cb7fb6b3ac25"
          },
          "path": "/renamed.txt",
          "id": "559271b86c9a43efa9ebfb94eedc7b96"
       },
       "serialization_time": "0.000341750"
    }
