The range search feature allows you to specify range of values which the returned objects must match. This is done using a special parameter format in the URL:

    http://localhost:8082/structr/rest/files?size=[3 TO 5]

Since we are using `curl` on the command line, we need to urlencode the non-URL characters:

    $ curl -si -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/files?size=$(urlencode '[3 TO 5]')"

Response:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=1rkhi4o8nmzya1356x2oqzbx4s;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.1.3.v20140225)
    
    {
       "query_time": "0.003258328",
       "result_count": 3,
       "result": [
          {
             "type": "File",
             "name": "test3.txt",
             "contentType": "text/plain",
             "size": 3,
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
             "path": "/test3.txt",
             "id": "498db44cf6af4ade966f72114dd1fe83"
          },
          {
             "type": "File",
             "name": "test4.txt",
             "contentType": "text/plain",
             "size": 4,
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
             "path": "/test4.txt",
             "id": "420997552b8844ceb65a78bd68bc72fd"
          },
          {
             "type": "File",
             "name": "test5.html",
             "contentType": "text/html",
             "size": 5,
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
             "path": "/test5.html",
             "id": "b17269ac328248819d03051edbff7028"
          }
       ],
       "serialization_time": "0.000534139"
    }

