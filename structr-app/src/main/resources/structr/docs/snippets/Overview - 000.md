This chapter provides an overview of the individual steps involved in creating a Structr application.

## Fundamentals

First things first - there are some things you need to know before you start.

### Admin User Interface

Only administrators can use the Structr Admin User Interface. Regular users cannot log in, and attempting to do so produces the error message `User has no backend access`. That means every Structr application with a user interface needs a Login page to allow non-admin users to use it. There is no built-in login option for normal users.

### Permission Levels

Access to every object in Structr must be explicitly granted - this also applies to pages and their elements. There are different permission levels that play a role in application development.

1. Administrators (indicated by `isAdmin = true`) have unrestricted access to all database data and REST endpoints.
2. Each object has two visibility flags that can be set separately.
    - `visibleToPublicUsers = true` allows the object to be read without authentication (read-only)
    - `visibleToAuthenticatedUsers = true` makes the object accessible for authenticated users (read-only)
3. Each object has an ownership relationship to the user that created it.
4. Each object can have one or more security relationships that control access for individual users or groups.
5. Access rights for all objects of a specific type can be configured separately for individual groups in the schema.

### Access Control

- The data model may only be changed by administrators, as it is a security-critical component.
- Access to pages and templates is usually controlled by visibility flags or, in rarer cases, by group membership.
- Access to files in the file system is usually controlled by visibility flags or by ownership.

## Define the Data Model

<img src="http://localhost:8082/structr/docs/schema_type-created_Project.png" class="small-image" />

Defining the data model is usually the first step in developing a Structr application. The data model controls how data is stored in the database, which fields are present in the REST endpoints and much more. It contains all information about the data types (or classes), their properties and how the objects are related, as well as their methods.

### Types

The data model consists of data types that can have relationships between them. Types have attributes to store your data, and methods to implement business logic.

### Relationships

When you define a relationship between two types, it serves as a blueprint for the structures created in the database. Each type automatically receives a special attribute that manages the connections between instances of these types.

### Attributes

Data types and relationships can be extended with custom attributes and constraints. Structr ensures that structural and value-based schema constraints are never violated, guaranteeing consistency and compliance with the rules defined in your schema.

For example, you can define a uniqueness constraint on a specific attribute of a type so that there can only be one object with the same attribute value in the database, or you can require that a specific attribute cannot be null.

### Where To Go From Here?

There are currently two different areas in the Structr Admin User Interface where the data model can be edited: [Schema](/structr/docs/ontology/Admin%20User%20Interface/Schema) and [Code](/structr/docs/ontology/Admin%20User%20Interface/Code). The Schema area contains the visual schema editor, which can be used to manage types and relationships, while the Code area is more for developing business logic. In both areas, new types can be created and existing types can be edited.

[Read more about the Data Model.](/structr/docs/ontology/Building%20Applications/Data%20Model)

## Create or Import Data

If you are building an application to work with existing data, there are several ways to bring that data into the system.

### CSV

You can import CSV data in two different ways:

##### Using the CSV Import Wizard in the Files Section

This is the preferred option, although it is somewhat difficult to find. To use it, you first have to upload a CSV file to Structr. An icon will then appear in the context menu of this file, which you can use to open the import wizard.

##### Using the Simple Import Dialog in the Data Section.

This importer is limited to a single type and can only process inputs of up to 100,000 lines, but it is a good option for getting started.

### XML

The XML import works in the same way as the file-based CSV import. First, you need to upload an XML file, then you can access the XML Import Wizard in the context menu for this file in the Files area.

### JSON

If your data is in JSON format, you can easily import individual objects or even larger amounts of data via the REST interface by using the endpoints automatically created by Structr for the individual data types in your data model.

[Read more about Creating & Importing Data.](/structr/docs/ontology/Building%20Applications/Data%20Creation%20&%20Import)

## Create the User Interface

<img src="http://localhost:8082/structr/docs/pages_simple-table-added.png" class="small-image" />

A Structr application's user interface consists of one or more HTML pages. Each page is rendered by the page rendering engine and served at a specific URL. The [Pages](/structr/docs/ontology/Admin%20User%20Interface/Pages) area provides a visual editor for those pages and allows you to configure all aspects of the user interface.

### Pages and Elements

Individual pages consist of either larger template blocks or nested HTML elements, or a combination of both. In addition, it is possible to use reusable elements called Shared Components, and insertable templates, known as Widgets.

[Read more about Pages & Templates.](/structr/docs/ontology/Building%20Applications/Pages%20&%20Templates)

### CSS, Javascript, Images

Static resources like CSS files, JavaScript files, images and videos are stored in the integrated filesystem in the [Files](/structr/docs/ontology/Admin%20User%20Interface/Files) area and can be accessed directly via their full path. Please note that the visibility restrictions also apply to files and folders.

[Read more about the Filesystem.](/structr/docs/ontology/Operations/Filesystem)

### Navigation, Start Page and Error Pages

A Page is accessible at a URL path matching its name. For example, a page named "index" is available at `/index`. By default, navigating to a non-existent page returns a 404 Not Found error. To display a different page instead, set the `showOnErrorCodes` attribute of that page to "404". This also ensures that this page is displayed when someone navigates to the root URL of your application, but note that the page must be made visible to public users for this to work.

[Read more about Navigation & Routing.](/structr/docs/ontology/Building%20Applications/Navigation%20&%20Routing)

### Dynamic Content

Dynamic content is generated on the server and sent to the client as HTML. Individual values can be inserted into the page using template expressions. Database objects and HTML elements are combined using repeaters, which usually execute a database query and repeatedly write the element to the HTML output for each result.

[Read more about Dynamic Content.](/structr/docs/ontology/Building%20Applications/Dynamic%20Content)

### User Input & Forms

User input and form data can be stored in database objects through Event Action Mapping. User interface elements like buttons can also create or delete database objects directly, or execute business logic methods.

[Read more about User Input & Forms.](/structr/docs/ontology/Building%20Applications/User%20Input%20&%20Forms)

## Implement Business Logic

Structr offers a wide range of options for implementing business logic in your application. These include time-controlled processes like scheduled imports, event-driven processes triggered through external interfaces or the application front end, and lifecycle methods that respond to data creation, modification, and deletion in the database.

### Methods

You can define methods on your custom types to encapsulate type-specific logic. These methods come in two forms: instance methods and static methods.

#### Instance Methods
Instance methods work on individual objects of a type and access their data through the `this` keyword. You can use them to calculate values, generate documents, or perform operations on specific instances. For example, an instance method on a `Customer` type can calculate the total value of all orders for that particular customer, or a method on an `Invoice` type can generate a PDF document for that specific invoice.

#### Static Methods
Static methods operate at the type level rather than on individual instances. They do not have access to `this` because they are not associated with a specific object. They are used for operations that affect multiple objects, such as finding all customers in a specific region, performing batch operations, or implementing factory patterns that create new instances with specific configurations.

### Functions

Structr provides two categories of application-wide functions: built-in functions and user-defined functions.

#### Built-in Functions
Built-in functions offer ready-to-use functionality for common tasks like sending emails, making HTTP requests, parsing JSON and XML, working with files, and querying the database. These functions are available throughout the platform wherever you write script code.

#### User-defined Functions
You can also create user-defined functions for custom application-wide logic. These functions can be called from anywhere in your application and can be scheduled for automatic execution using the cron service, useful for maintenance tasks, periodic imports, or automated reports.

### Cron Jobs

### Lifecycle Methods

[Read more about Business Logic.](/structr/docs/ontology/Building%20Applications/Business%20Logic)

## Integrate With Other Systems