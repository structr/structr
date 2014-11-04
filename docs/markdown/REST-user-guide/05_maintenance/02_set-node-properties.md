Sometimes it's handy to set a certain property to a certain value on all nodes of a type. This can be done with the following command:

    POST /maintenance/setNodeProperties '{"type":"User", "password":null}'

This will f.e. set the password of all users to null (so noone can login after that).

You can even set a new type (please handle with care, it could make your nodes unusable).

    POST /maintenance/setNodeProperties '{"type":"Content", "newType": "Comment"}'
