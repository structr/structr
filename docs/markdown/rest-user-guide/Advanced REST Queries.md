To support the examples in the following sections, we create a set of test files:

    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "test0.txt", "contentType": "text/plain", "size": 0 }'
    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "test1.txt", "contentType": "text/plain", "size": 1 }'
    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "test2.txt", "contentType": "text/plain", "size": 2 }'
    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "test3.txt", "contentType": "text/plain", "size": 3 }'
    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "test4.txt", "contentType": "text/plain", "size": 4 }'
    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "test5.html", "contentType": "text/html", "size": 5 }'
    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "test6.html", "contentType": "text/html", "size": 6 }'
    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "test7.html", "contentType": "text/html", "size": 7 }'
    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "test8.html", "contentType": "text/html", "size": 8 }'
    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "test9.html", "contentType": "text/html", "size": 9 }'
    $ curl -si -HX-User:admin -HX-Password:admin -XPOST http://localhost:8082/structr/rest/files -d '{ "name": "test10.html", "contentType": "text/html", "size": 10 }'

**Note**: Structr uses the Lucene search engine which is embedded into Neo4j.
