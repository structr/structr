    $ curl -i -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/cypher -d '{
            "query": "START n=node(4) RETURN n"
    }'

This should return the first user node that is created when Structr first starts.

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=mv6fedlakpg6k0pry6wupyyu;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=UTF-8
    Vary: Accept-Encoding, User-Agent
    Content-Length: 269
    Server: Jetty(9.1.3.v20140225)
    
    {
      "result_count": 1,
      "result": {
        "id": "f02e59a47dc9492da3e6cb7fb6b3ac25",
        "type": "User",
        "name": "admin",
        "salutation": null,
        "firstName": null,
        "middleNameOrInitial": null,
        "lastName": null
      },
      "serialization_time": "0.000201955"
    }
