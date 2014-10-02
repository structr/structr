You can even query for multiple values in the same field, using `;` (semicolon) as a field separator for **OR**.

    $ curl -si -HX-User:admin -HX-Password:admin "http://localhost:8082/structr/rest/files?name=test1.txt;test2.txt"

Response:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=4yl2j1j259wbmz03lvz85alj;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.1.3.v20140225)
    
    {
       "query_time": "0.017328418",
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
             "id": "c712190737374462a8c7b220e06f5ca7"
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
             "id": "37b648f875f046aebbab91fad6660710"
          }
       ],
       "serialization_time": "0.006950845"
    }

