The process of building a Structr app involves the following steps.

## 1. Define the Data Model

Defining the data model is usually the first step in developing a Structr application. The data model controls how data is stored in the database, which fields are present in the REST endpoints, how the REST endpoints behave, and much more. It contains all information about the data types (or classes), their properties and how the objects are related, as well as their methods.

### Where To Go From Here?

There are currently two different areas in the Structr Admin user interface where the data model can be edited: Schema and Code. The Schema area contains the visual schema editor, which can be used to manage types and relationships, while the Code area is more for developing business logic. In both areas, new types can be created and existing types can be edited.

[Continue to the article on the data model to learn more about this.]()


<a href="/structr/docs/ontology/Admin+User+Interface/Code">Test1 </a>

<a href="/structr/docs/ontology/Admin User Interface/Code">Test 2</a>


## 2. Create the User Interface

The user interface of a Structr application consists of one or more HTML pages that are created and output by the page rendering engine under a specific URL. To access the application, you simply point your browser to the location of your Structr server, e.g. http://localhost:8082/structr.

### Pages and Their Elements

Individual pages consist of either larger template blocks or nested HTML elements, or a combination of both. In addition, it is possible to use reusable elements called Shared Components, and insertable templates, known as Widgets.

### Static Resources

Static resources like CSS files, JavaScript files, images and videos are stored in the integrated filesystem in the Files area.

### Access Levels

An important point to note from the outset is the visibility of objects in Structr, which also applies to pages and their elements. **The Structr admin user interface can only be used by administrators**, who have unrestricted access to the data in the database, so the visibility of objects is irrelevant in the initial phase.

However, as soon as a web application is used by non-administrators, the visibility of individual pages and their elements must be explicitly defined. 

### Navigation




### 1. Create Pages

### 2. Display your Data

### 3. Make your Data Editable