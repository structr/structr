# Web stack

Structr delivers a complete, integrated web development platform built on three foundational layers.

![pages_page-preview.png](pages_page-preview.png)

**Complete Web Stack**: Provides all essential components required for web applications—database, APIs, authentication, content management, and frontend rendering—in a unified platform that eliminates technology integration complexity.

**Graph-Based Architecture**: Leverages graph database technology for superior performance in dynamic DOM generation, complex content assembly through sophisticated queries, and efficient handling of complex JSON documents at the middleware level.

**Integrated Low-Code IDE**: Built-in visual development environment with drag-and-drop builders, schema designers, and automated API generation, enabling rapid application development for both technical and non-technical users.

**AI-Enhanced Development**: Advanced AI tooling using technologies like MCP (Model Context Protocol) that connects directly to Structr's APIs, providing intelligent assistance for schema design, content generation, and automated optimizations with full application context awareness.

This graph-powered, unified architecture transforms web development from complex coding to intuitive configuration, while delivering performance advantages and maintaining professional-grade flexibility.

## How Web Apps work in General

Web applications technically serve HTML (the so-called markup), CSS and script content, typically downloaded from a web server when a web client (browser) requests a page addressed by a URL. The content that the server responses with can either be static, dynamic or mixed.

With Structr, you can serve static HTML code, or content in general, from content elements in form of files or database objects.  and written in an HTML editor.

Structr provides both, an editor for writing static blocks of HTML code, but also the tools to create and manage a tree of atomic HTML elements.

There's also an integrated file system to serve CSS, JavaScript or any other other type of content (like images or videos) or code, directly from a database and/or filesystem, or from other data sources.

The content that is sent to the client can also be produced dynamically, either on the server or on the client, typically either by combining templates or markup elements with the results from database queries that are parameterized or personalized by user input which is streamed together with the static page elements from the client to the server.

## Client-side Markup Generation

Generating dynamic markup on the client is typically done by script (ECMAScript) executed in the web browser's script engine to combine template elements with data from a server backend, typically served in the form of JSON data.

Structr can be used in a backend-only mode and serves as a powerful JSON/REST application database backend that can be configured to create dynamic responses by a convenient schema editor.

## Server-side Markup Generation

One of the strengths of Structr is the sophisticated low-code web app builder tool that allows creating complex dynamic and interactive web applications that produce markup server-side.

The advantages of generating markup on the server are improved security by not exposing any business logic code to the client and a reduced application footprint and thus less CPU and memory load on the client, keeping the clients fast, responsive and stateless.

### Page Rendering Engine

The key to fast, secure and reliable web applications is assembling valid HTML from a variety of sources in real-time.

Structr's page rendering engine largely benefits from the graph database where all the DOM elements and templates are stored in an redunancy-free tree structure that is optimized for streaming dynamic HTML to the web client with minimal latency.

The way the elements of a web app are stored in Structr allow for the assembly of any HTML content in real-time without caching needed, allowing for fast streaming to the client. When accessing a page, the content is already rendered by the web browser even while the server is still fetching data from the database and producing dynamic HTML content.

### Routing and Details Object Lookup

Structr uses a classic approach for page routing and object lookup by splitting the URL path into a parts and interpreting the first part as the page name and the second part as the object identifier which can be a type-4 UUID or any other unique object attribute value.

### URL Parameters

In addition to the page and object path parts, arbitrary request parameters are received and validated by the Structr web server and passed the runtime engine processing the request.

Any request parameter can be accessed through the keyword `request`.

#### Example

For the example URL `https://localhost:8082/foo/75b89b39416d40a1b39e0da27b353e1c?projectId=abc123`, the script expression `${request.projectId}` returns `abc123`, `${page}` returns `foo` and `${current.id}` returns `75b89b39416d40a1b39e0da27b353e1c`.

## Serving Files, Images Videos etc.

Another important component of Structr's web stack is the integrated virtual file system that allows to upload, store and access (or download) any file.

For details see [File system](3-File system.md).

Not only can files be stored in an arbitrary directory structure, files and directories can optionally use custom classes, allowing for the definition of any custom metadata.

### Streaming

All of Structr's output channels support streaming of dynamically generated content to the client. There's a configuration setting to disable streaming which is on by default. 