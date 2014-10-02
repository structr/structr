The first example shows how to use `curl` to access an empty REST resource for a given type, in this case the resource for the builtin type `File`. Please note that you have to supply a username and a password, otherwise you will only see objects that have intentionally been made public.

    $ curl -si -HX-User:admin -HX-Password:admin http://localhost:8082/structr/rest/files

As you can see, the server responds with a JSON result object containing zero results:

    HTTP/1.1 200 OK
    Set-Cookie: JSESSIONID=16oi7uxv9x8mh1mik2p2lapb1l;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Content-Type: application/json; charset=utf-8
    Vary: Accept-Encoding, User-Agent
    Transfer-Encoding: chunked
    Server: Jetty(9.1.3.v20140225)
    
    {
       "query_time": "0.002825731",
       "result_count": 0,
       "result": [],
       "serialization_time": "0.000088917"
    }
