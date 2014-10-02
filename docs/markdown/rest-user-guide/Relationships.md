    $ curl -i -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/cypher -d '{
            "query": "START n=node(4) MATCH n-[r:OWNS]->m RETURN r LIMIT 1"
    }'

This query returns only the relationship between the user and a file.

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=1c8quzyvqp9i0qxvysnl9sby0;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=UTF-8
    Vary: Accept-Encoding, User-Agent
    Content-Length: 286
    Server: Jetty(9.1.3.v20140225)
    
    {
      "result_count": 1,
      "result": {
        "id": "73e88da5ff0b45b7819816a3b7cb33f6",
        "type": "PrincipalOwnsNode",
        "relType": "OWNS",
        "sourceId": "f02e59a47dc9492da3e6cb7fb6b3ac25",
        "targetId": "380d9da41ceb46a99aa17b70e3d5bf87"
      },
      "serialization_time": "0.000346881"
    }
