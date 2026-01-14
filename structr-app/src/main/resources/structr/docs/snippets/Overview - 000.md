## Fundamentals

This chapter provides an overview of the individual steps involved in creating a Structr application. First, however, we need to cover a few basics.

- Only administrators can access the Structr Admin User Interface. Regular users cannot log in, and attempting to do so produces the error message `User has no backend access`. That means every Structr application with a user interface needs a Login page
- Administrators have unrestricted access to all database data and REST endpoints whereas regular users require explicit permission grants to access REST endpoints or database data.

## 1. Define the Data Model

Defining the data model is usually the first step in developing a Structr application. The data model controls how data is stored in the database, which fields are present in the REST endpoints and much more. It contains all information about the data types (or classes), their properties and how the objects are related, as well as their methods.

### Dynamic Types


### Relationships



### Where To Go From Here?
There are currently two different areas in the Structr Admin User Interface where the data model can be edited: [Schema](/structr/docs/ontology/Admin%20User%20Interface/Schema) and [Code](/structr/docs/ontology/Admin%20User%20Interface/Code). The Schema area contains the visual schema editor, which can be used to manage types and relationships, while the Code area is more for developing business logic. In both areas, new types can be created and existing types can be edited.

[Read more about the Data Model.](/structr/docs/ontology/Building%20Applications/Data%20Model)

## 2. Create the User Interface

A Structr application's user interface consists of one or more HTML pages. Each page is rendered by the page rendering engine and served at a specific URL. The [Pages](/structr/docs/ontology/Admin%20User%20Interface/Pages) area provides a visual editor for those pages and allows you to configure all aspects of the user interface.

### Pages and Elements

Individual pages consist of either larger template blocks or nested HTML elements, or a combination of both. In addition, it is possible to use reusable elements called Shared Components, and insertable templates, known as Widgets.

[Read more about Pages & Templates.](/structr/docs/ontology/Building%20Applications/Pages%20&%20Templates)

### Access Levels

Access to every object in Structr must be explicitly granted - this includes pages and their elements. During early development, this restriction can be safely ignored since only administrators use the Structr admin interface. However, once non-admin users need to access the application, you must explicitly define the visibility of pages and their elements and create a Login page.

[Read more about Navigation & Routing.](/structr/docs/ontology/Building%20Applications/Navigation%20&%20Routing)

### CSS, Javascript, Images

Static resources like CSS files, JavaScript files, images and videos are stored in the integrated filesystem in the [Files](/structr/docs/ontology/Admin%20User%20Interface/Files) area and can be accessed directly via their full path. Please note that the visibility restrictions also apply to files and folders.

[Read more about the Filesystem.](/structr/docs/ontology/Operations/Filesystem)

### Navigation, Start Page and Error Pages

A Page is accessible at a URL path matching its name. For example, a page named "index" is available at `/index`. By default, navigating to a non-existent page returns a 404 Not Found error. To display a different page instead, set the `showOnErrorCodes` attribute of that page to "404". This also ensures that this page is displayed when someone navigates to the root URL of your application.

[Read more about Navigation & Routing.](/structr/docs/ontology/Building%20Applications/Navigation%20&%20Routing)

### Dynamic Content

Dynamic content is generated on the server and sent to the client as HTML. Individual values can be inserted into the page using template expressions. Database objects and HTML elements are combined using repeaters, which usually execute a database query and repeatedly write the element to the HTML output for each result.

[Read more about Dynamic Content.](/structr/docs/ontology/Building%20Applications/Dynamic%20Content)

### User Input & Forms

User input and form data can be stored in database objects through Event Action Mapping. User interface elements like buttons can also create or delete database objects directly, or execute business logic methods.

[Read more about User Input & Forms.](/structr/docs/ontology/Building%20Applications/User%20Input%20&%20Forms)

## 3. Implement Business Logic













