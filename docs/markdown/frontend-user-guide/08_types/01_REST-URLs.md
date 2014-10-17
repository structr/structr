At the top of each type view, there's a URL pointing to the type's root REST resource. You can access the resource in the browser. As a web browser uses ``text/html`` as requested content-type, the REST server responds with HTML.

If you access the same URL with a command line tool or any other HTTP client using ``application/json`` as accepted content-type, the server responds with JSON.
