    $ curl -i -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/cypher -d '{
            "query": "START u=node(4) MATCH u-[r:OWNS]->f RETURN u, r, f, f.size LIMIT 1"
    }

You can even get Structr to return a mixed set of nodes, relationships and single property entries.

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=1w6olls7g3r561e7ltws1qy57f;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=UTF-8
    Vary: Accept-Encoding, User-Agent
    Content-Length: 1009
    Server: Jetty(9.1.3.v20140225)
    
    {
      "result_count": 4,
      "result": [
        {
          "id": "380d9da41ceb46a99aa17b70e3d5bf87",
          "type": "File",
          "name": "test10.html",
          "contentType": "text/html",
          "size": 10,
          "url": null,
          "owner": {
            "id": "f02e59a47dc9492da3e6cb7fb6b3ac25",
            "type": "User",
            "name": "admin",
            "salutation": null,
            "firstName": null,
            "middleNameOrInitial": null,
            "lastName": null
          },
          "path": "/test10.html"
        },
        {
          "f.size": 10
        },
        {
          "id": "f02e59a47dc9492da3e6cb7fb6b3ac25",
          "type": "User",
          "name": "admin",
          "salutation": null,
          "firstName": null,
          "middleNameOrInitial": null,
          "lastName": null
        },
        {
          "id": "73e88da5ff0b45b7819816a3b7cb33f6",
          "type": "PrincipalOwnsNode",
          "relType": "OWNS",
          "sourceId": "f02e59a47dc9492da3e6cb7fb6b3ac25",
          "targetId": "380d9da41ceb46a99aa17b70e3d5bf87"
        }
      ],
      "serialization_time": "0.000790774"
    }
