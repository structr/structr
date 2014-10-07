Access to a REST endpoints is protected by a corresponding node of type "ResourceAccess". A ResourceAccess node has the following fields:

    "signature": Normalized path of the REST endpoint, f.e.
    "flags": Integer bitmask

Examples for the "signature" field:

    /foo, /foos, /Foo, /Foos, /foo/, /Foo/ => Foo
    /foo/bar, /foo/Bar, /foo/Bars etc. (including variants of Foo) => Foo/Bar
    /foo/45d94d5511ca477788e1de3e05abd4d6 => /Foo/Id
    /foo/ui => /Foo/_Ui (here, 'ui' is a view)

The "flags" field has the following semantics:

		FORBIDDEN                   = 0;
		AUTH_USER_GET               = 1;
		AUTH_USER_PUT               = 2;
		AUTH_USER_POST              = 4;
		AUTH_USER_DELETE            = 8;
		NON_AUTH_USER_GET           = 16;
		NON_AUTH_USER_PUT           = 32;
		NON_AUTH_USER_POST          = 64;
		NON_AUTH_USER_DELETE        = 128;
		AUTH_USER_OPTIONS           = 256;
		NON_AUTH_USER_OPTIONS       = 512;
To make the resource visible in Structr's backend UI, you just have to make the ResourceAccess object 'visibleToAuthenticatedUsers' (setting the boolean field to true).

So for example, if you want to grant GET and PUT access to authenticated users on the REST endpoint "/foo/bar" and make it accessible in the Data UI, you have to create a ResourceAccess node as follows:

    post resource_access '{"signature":"Foo/Bar", "flags":3, "visibleToAuthenticatedUsers":true}'

Another excample: To allow POST to non-authenticated users, but not GET, PUT, DELETE and OPTIONS to "/registration", but hide the endpoint for the Data UI:

    post resource_access '{"signature":"Registration", "flags": 64}'
