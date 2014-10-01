In the second example, we will create a File using the `POST` verb and examine the result.

    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "myfile.txt", "contentType": "text/plain" }'

and the server will respond with something like this:

    HTTP/1.1 201 Created
    Set-Cookie: JSESSIONID=w214ue6bmsb01aje3k60oy41r;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=UTF-8
    Location: http://localhost:8082/structr/rest/files/559271b86c9a43efa9ebfb94eedc7b96
    Vary: Accept-Encoding, User-Agent
    Content-Length: 0
    Server: Jetty(9.1.3.v20140225)

Issuing a GET request again, we can now see that the files resource contains one element.

    $ curl -si -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/files

Response:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=7rf5rzj4vi3v508vt09izvg1;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.1.3.v20140225)
    
    {
       "query_time": "0.003137433",
       "result_count": 1,
       "result": [
          {
             "type": "File",
             "name": "myfile.txt",
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
             "path": "/myfile.txt",
             "id": "559271b86c9a43efa9ebfb94eedc7b96"
          }
       ],
       "serialization_time": "0.000332605"
    }
