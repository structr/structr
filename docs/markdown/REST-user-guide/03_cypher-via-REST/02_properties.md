    $ curl -i -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/cypher -d '{
            "query": "START n=node(4) RETURN n.name"
    }'

You can also access individual properties separately:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=3vf9wcqhl3b0hifc3o2458kj;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=UTF-8
    Vary: Accept-Encoding, User-Agent
    Content-Length: 103
    Server: Jetty(9.1.3.v20140225)
    
    {
      "result_count": 1,
      "result": {
        "n.name": "admin"
      },
      "serialization_time": "0.000028697"
    }
