Structr features a built-in schema which can be extended at runtime. The predefined data model provides some basic entity classes useful for typical web and mobile apps, like User, Group, File, Folder, Page, DOMElement, Content etc.. The dynamic model can be administered either through Structr's backend UI, or using the JSON/REST interface.

To illustrate the schema administration via REST, we can for example create a new type "Project" by POSTing the following JSON string to the /schema_nodes resource:

    POST /schema_nodes '{"name":"Project", "_name":"+String"}'

After that, Structr has created a new toplevel resource for the type "Project", i.e. you can access the /projects REST resource:

    GET /projects
    {
       "query_time": "0.005291008",
       "result_count": 0,
       "result": [],
       "serialization_time": "0.000052170"
    }

which is of course empty. You can then create entities of your newly created type.

    POST /projects '{name:Test}'
    GET /projects
    {
       "query_time": "0.004956646",
       "result_count": 1,
       "result": [
          {
             "name": "Test",
             "id": "654aae951a2f467e8264f3d234f73e04",
             "type": "Project"
          }
       ],
       "serialization_time": "0.000240133"
    }


### Properties and Constraints
As you can see, you can declare properties on a schema node by prefixing the desired name with "_", i.e. the "_name" property on a schema node corresponds to the "name" property on the Project node. Additionally, you can define constraints on individual properties, for example "Name must not be null", or "Name must be unique". This is done by using special characters in the type definition: the type of the "name" property of a Project is "+String!", which means "not null(+), String, unique(!)".

The possible types for a property are

* String
* Integer
* Long
* Double
* Float
* Boolean
* Enum
* Date.

Each can be annotated with the "not null" and "unique" characters, as well as additional, type-dependent validation and range expressions. You can even define the individual enum constants (and let Structr validate it).

### Examples

##### String, not empty, unique
    +String!

##### String, not empty, lowercase only
    +String([a-z]+)

##### String, not empty, alphanumeric
    +String([a-zA-Z0-9]+)

##### Integer, not null
    +Integer

##### Integer, not null, between 10 and 20
    +Integer([10, 20])

##### Double, not null, between 1.0 and 2.0
    +Double([1.0, 2.0])

##### Date, in the format yyyy/MM/dd
    Date(yyyy/MM/dd)

##### Date, in the format dd.MM.yyyy HH:mm:ss
    Date(dd.MM.yyyy HH:mm:ss)

##### Enum with the values male, female and other
    Enum(male,female,other)


For example, the following POST request will not create a new Project, because the validaton fails:

    POST /projects
    {
      "code": 422,
      "errors": {
        "Project": {
          "name": [
            "must_not_be_empty"
          ]
        }
      }
    }

