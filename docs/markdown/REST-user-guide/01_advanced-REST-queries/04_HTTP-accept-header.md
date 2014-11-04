In addition to the various query methods, you can use the HTTP `Accept` header field to select individual properties in the REST result.

    $ curl -si -HX-User:admin -HX-Password:admin -H"Accept:application/json; properties=name,type" "http://localhost:8082/structr/rest/files?name=test1.txt;test2.txt"

Response:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=6a32plzqhbat13k60o0n3774a;Path=/
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
             "name": "test1.txt",
             "type": "File"
          },
          {
             "name": "test2.txt",
             "type": "File"
          }
       ],
       "serialization_time": "0.006950845"
    }
