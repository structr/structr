Access to a REST endpoints is protected by a corresponding node of type "ResourceAccess". A ResourceAccess node has the following fields:

    "signature": Normalized path of the REST endpoint
    "flags": Integer bitmask

Examples for the "signature" field:

<table>
<tr><td><b>Signature</b></td><td><b>Applies to endpoint(s)</b></td><td><b>Note</b></td></tr>
<tr><td>Foo</td><td>/foo, /foos, /Foo, /Foos, /foo/, /Foo/</td><td>captures permutations of 'Foo' endpoints</td></tr>
<tr><td>Foo/Bar</td><td>/foo/bar, /foo/Bar, /foo/Bars</td><td></td></tr>
<tr><td>/Foo/Id</td><td>/foo/45d94d5511ca477788e1de3e05abd4d6</td><td></td></tr>
<tr><td>/Foo/_Ui</td><td>/foo/ui</td><td>(here, 'ui' is a view)</td></tr>
</table>

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
		AUTH_USER_HEAD              = 1024;
		NON_AUTH_USER_HEAD          = 2048;
		
		
To make the resource visible in Structr's backend UI, you just have to make the ResourceAccess object 'visibleToAuthenticatedUsers' (setting the boolean field to true).

So for example, if you want to grant GET and PUT access to authenticated users on the REST endpoint "/foo/bar" and make it accessible in the Data UI, you have to create a ResourceAccess node as follows:

    post resource_access '{"signature":"Foo/Bar", "flags":3, "visibleToAuthenticatedUsers":true}'

Another excample: To allow POST to non-authenticated users, but not GET, PUT, DELETE, OPTIONS and HEAD to "/registration", but hide the endpoint for the Data UI:

    post resource_access '{"signature":"Registration", "flags": 64}'
