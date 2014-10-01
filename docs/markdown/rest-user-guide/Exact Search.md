Using exact search, you can search for a list of objects using the exact value you want the search field to contain. We look for a file with the name `test1.txt` first.

    $ curl -si -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/files?name=test1.txt"

Response:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=xkn5bvu0vt3h124xf8c9w7a8x;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.1.3.v20140225)
    
    {
       "query_time": "0.004941183",
       "result_count": 1,
       "result": [
          {
             "type": "File",
             "name": "test1.txt",
             "contentType": "text/plain",
             "size": 1,
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
             "path": "/test1.txt",
             "id": "e9545499d9144aada64909d9106c99d1"
          }
       ],
       "serialization_time": "0.000527189"
    }

Exact search can of course return multiple results:

    $ curl -si -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/files?contentType=text/plain"

Response:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=1c2jh95zjoggf1pylyfodp4t9u;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.1.3.v20140225)
    
    {
       "query_time": "0.005492747",
       "result_count": 5,
       "result": [
          {
             "type": "File",
             "name": "test0.txt",
             "contentType": "text/plain",
             "size": 0,
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
             "path": "/test0.txt",
             "id": "2790ed17fce146f58b59f85cce8416f6"
          },
          {
             "type": "File",
             "name": "test1.txt",
             "contentType": "text/plain",
             "size": 1,
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
             "path": "/test1.txt",
             "id": "e9545499d9144aada64909d9106c99d1"
          },
          {
             "type": "File",
             "name": "test2.txt",
             "contentType": "text/plain",
             "size": 2,
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
             "path": "/test2.txt",
             "id": "f706618d96064049b33765865d206d7b"
          },
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
             "id": "67de3d9ed3c449c5abb7eac14377b83e"
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
             "id": "8a85108f688f4097a2633cc7eb2df0e5"
          }
       ],
       "serialization_time": "0.001301977"
    }

Or you can search for files using numerical values:

    $ curl -si -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/files?size=5"

Response:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=w22w8i0yyy2y811h3fhfn3nk;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.1.3.v20140225)
    
    {
       "query_time": "0.004543886",
       "result_count": 1,
       "result": [
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
       "serialization_time": "0.000345816"
    }
