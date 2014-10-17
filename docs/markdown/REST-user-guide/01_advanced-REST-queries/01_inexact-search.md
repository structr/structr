You can enable inexact search using the request parameter `loose=1`. In the following example, we look for files with the number "1" in the name, which should of course return file1.txt and file10.html.

    $ curl -si -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/files?name=1&loose=1"

Response:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=zbp76ut1zofm1esf23bya42fd;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.1.3.v20140225)
    
    {
       "query_time": "0.004745825",
       "result_count": 2,
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
          },
          {
             "type": "File",
             "name": "test10.html",
             "contentType": "text/html",
             "size": 10,
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
             "path": "/test10.html",
             "id": "6bcc3051881b450bba4f74dbf855b652"
          }
       ],
       "serialization_time": "0.000647588"
    }
