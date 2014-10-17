It is also possible to set permissions via the REST interface as you can also access relationships this way.

To change the permissions a user/group has for a particular node, you need to lookup the security relationships first:

	curl -HX-User:admin -HX-Password:admin "http://0.0.0.0:8082/structr/rest/security?accessControllableId=941f54875b9446f7b7b929061f04fde5&principalId=f02e59a47dc9492da3e6cb7fb6b3ac25"
	{
	   "query_time": "0.001492947",
	   "result_count": 1,
	   "result": [
	      {
	         "id": "d3ec4083707e4e1ea037a1ec2727a560",
	         "type": "Security",
	         "relType": "SECURITY",
	         "sourceId": "f02e59a47dc9492da3e6cb7fb6b3ac25",
	         "targetId": "941f54875b9446f7b7b929061f04fde5",
	         "principalId": "f02e59a47dc9492da3e6cb7fb6b3ac25",
	         "accessControllableId": "941f54875b9446f7b7b929061f04fde5",
	         "lastModifiedDate": "2014-07-30T10:43:47+0200",
	         "createdDate": "2014-07-30T10:43:47+0200",
	         "cascadeDelete": 0,
	         "allowed": [
	            "read",
	            "write",
	            "delete",
	            "accessControl"
	         ]
	      }
	   ],
	   "serialization_time": "0.000228114"
	}

Then change the ``allowed`` attribute to the desired value:

    curl -i -HX-User:admin -HX-Password:admin "http://0.0.0.0:8082/structr/rest/d3ec4083707e4e1ea037a1ec2727a560" -XPUT -d '{"allowed":["read","write"]}'
    
Check the updated relationship object:


	curl -HX-User:admin -HX-Password:admin "http://0.0.0.0:8082/structr/rest/d3ec4083707e4e1ea037a1ec2727a560"
	{
	   "query_time": "0.002009954",
	   "result_count": 1,
	   "result": {
	      "id": "d3ec4083707e4e1ea037a1ec2727a560",
	      "type": "Security",
	      "relType": "SECURITY",
	      "sourceId": "f02e59a47dc9492da3e6cb7fb6b3ac25",
	      "targetId": "941f54875b9446f7b7b929061f04fde5",
	      "principalId": "f02e59a47dc9492da3e6cb7fb6b3ac25",
	      "accessControllableId": "941f54875b9446f7b7b929061f04fde5",
	      "lastModifiedDate": "2014-08-03T15:06:04+0200",
	      "createdDate": "2014-07-30T10:43:47+0200",
	      "cascadeDelete": 0,
	      "allowed": [
	         "read",
	         "write"
	      ]
	   },
	   "serialization_time": "0.000384896"
	}

You can also create new permissions by POSTing to the security resource:

    curl -i -HX-User:admin -HX-Password:admin "http://0.0.0.0:8082/structr/rest/security" -d '{"principalId":"467f7c423c0e4b689468ebca163d7791","accessControllableId":"941f54875b9446f7b7b929061f04fde5","allowed":["read"]}' -XPOST
    HTTP/1.1 201 Created
    Content-Type: application/json; charset=utf-8
    Set-Cookie: JSESSIONID=e2ullk9o2592vf8jj10mtk6d;Path=/
    Expires: Thu, 01 Jan 1970 00:00:00 GMT
    Location: http://0.0.0.0:8082/structr/rest/security/d3ec4083707e4e1ea037a1ec2727a560
    Vary: Accept-Encoding, User-Agent
    Content-Length: 0
    Server: Jetty(9.1.4.v20140401)

``principalId`` is the UUID of the user or group and ``accessControllableId`` is the UUID of the node you want grant permissions on to the user/group.

A new security relationship has been created:

	curl -HX-User:admin -HX-Password:admin http://0.0.0.0:8082/structr/rest/bfa7b7b3639b4525a441a1bec68be508
	{
	   "query_time": "0.008050873",
	   "result_count": 1,
	   "result": {
	      "id": "bfa7b7b3639b4525a441a1bec68be508",
	      "type": "Security",
	      "relType": "SECURITY",
	      "sourceId": "467f7c423c0e4b689468ebca163d7791",
	      "targetId": "941f54875b9446f7b7b929061f04fde5",
	      "principalId": "467f7c423c0e4b689468ebca163d7791",
	      "accessControllableId": "941f54875b9446f7b7b929061f04fde5",
	      "lastModifiedDate": "2014-08-03T15:36:52+0200",
	      "createdDate": "2014-08-03T15:36:52+0200",
	      "cascadeDelete": 0,
	      "allowed": [
	         "read"
	      ]
	   },
	   "serialization_time": "0.001714507"
	}
