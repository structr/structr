And finally, to complete this tiny REST introduction, we use the DELETE verb to remove the file we just created.

    $ curl -si -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/files/559271b86c9a43efa9ebfb94eedc7b96 -XDELETE

Response:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=1icahpgj3n675ryac3psjm47;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Content-Length: 0
    Server: Jetty(9.1.3.v20140225)

And we can see that the collection resource again contains zero elements.

    $ curl -si -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/files

Response:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=gem2ldhtdeaxv8xllnn2j89c;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.1.3.v20140225)
    
    {
       "query_time": "0.003196869",
       "result_count": 0,
       "result": [],
       "serialization_time": "0.000030503"
    }

Direct access to the element resource results in a 404 Not Found error:

    $ curl -i --silent -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/files/559271b86c9a43efa9ebfb94eedc7b96

Response:

    HTTP/1.1 404 Not Found
    Set-Cookie: JSESSIONID=vwguwkl9ekxvjuzbhwg1mwy0;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Length: 18
    Server: Jetty(9.1.3.v20140225)
    
    {
      "code": 404
    }
