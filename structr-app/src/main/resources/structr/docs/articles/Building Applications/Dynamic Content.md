Structr renders all page content on the server. To display data from the database or other sources, you use template expressions and scripting. Template expressions let you insert dynamic values into your pages, while scripting gives you full control over data retrieval and processing. This chapter builds on the concepts introduced in the Pages & Templates chapter.

## How Structr Differs from Client-Side Frameworks

If you are familiar with client-side frameworks like React, Vue, or Angular, Structr's approach to dynamic content may feel different at first. Understanding these differences helps you work with Structr effectively.

### Server-Side by Default

Structr is not a rich-client stack. Like other server-side rendering approaches, most of the work happens on the server. Structr follows principles similar to the ROCA style (Resource-Oriented Client Architecture): the server renders HTML, the URL identifies resources, and the server controls the application logic.

### Everything is Accessible

What sets Structr apart is how accessible everything is. All the layers of a typical web application exist – data model, business logic, rendering, user interaction – but they are thin and within reach. You sit in a control room where everything is at your fingertips, rather than having to dig through separate codebases for each concern.

The data model can be changed live without migrations – existing data immediately gets the new attributes. Repeaters give you direct access to query results. Template expressions let you bind data to elements without intermediate layers. Event Action Mappings connect user input directly to backend operations.

### State on the Server

Structr does have state management, but it happens on the server by default. The URL determines what is displayed, and the `current` keyword gives you the object resolved from the URL. For user-specific state, you can store values directly on the user object via `me`, or create dedicated Settings objects in the database.

### A Different Mindset

Structr brings data and frontend closer together than traditional frameworks. You access data directly in your page elements through template expressions and repeaters, without the need for client-side state management or passing data through component hierarchies. This directness can feel unfamiliar at first, but once you embrace it, you may find that many common tasks require less code than you expect.

## Template Expressions

Template expressions allow you to insert dynamic values anywhere in your pages. You can use them in content elements, template elements, and HTML attributes.

### Syntax

A template expression is enclosed in `${...}` and uses StructrScript, which is concise for simple expressions. For more complex logic, use JavaScript with double braces `${{...}}`, which opens a full JavaScript context for calculations, loops, and more complex operations.

### Auto-Script Fields

Some input fields in the Structr UI are marked as auto-script fields. These fields automatically interpret their content as script expressions, so you do not need the `${...}` wrapper. Auto-script fields include Function Query, Show Conditions, and Hide Conditions. You can recognize auto-script fields in the Admin User Interface by their characteristic `${}` or `${{}}` prefixes displayed next to the input field.

### Keywords

Template expressions have access to built-in keywords that provide context about the current request, user, and page. The most commonly used keywords are:

- `current` – the object resolved from the URL (see Navigation & Routing)
- `me` – the current user
- `page` – the current page
- `request` – HTTP request parameters
- `now` – the current timestamp

For a complete list of available keywords, see the Keyword Reference.

### Calling Methods

In addition to built-in functions and keywords, you can call methods defined on your types. This connects your pages to the business logic in your data model.

Static methods are called on the type itself:

`${Project.getActiveProjects()}`

Instance methods are called on objects you have access to, such as `current`, `me`, or objects retrieved via `find()`:

`${current.calculateTotal()}`

This allows you to keep complex logic in your data model and call it from your pages as needed. For details on defining methods, see the Business Logic chapter.

### Error Handling

When accessing properties on objects that might be null, use the `if()` or `empty()` functions to avoid errors. For example, instead of `${current.name}`, use `${if(empty(current), '', current.name)}` to return an empty string when no object is available.

### Example: A Page Template

This example shows a typical page template that uses several template expressions:

```html
<!doctype html>
<html class="h-full bg-gray-100">
	<head>
		<title>Structr - ${localize(titleize(page.name, '-'))}</title>
		<meta charset="utf-8">
		<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
		<script src="https://cdn.tailwindcss.com?plugins=forms,typography,aspect-ratio,line-clamp"></script>
		<script type="module" defer src="/structr/js/frontend/frontend.js"></script>
	</head>
	<body class="h-full antialiased font-sans">
		${render(children)}
	</body>
</html>
```

The title combines several functions: `titleize()` converts the page name to title case (replacing dashes with spaces), and `localize()` looks up a translation for the result. The `render(children)` call in the body marks where child elements will appear. For details on `render()` and other page functions, see the Page Functions section at the end of this chapter.

Note the include of `/structr/js/frontend/frontend.js`. This is an internal JavaScript file provided by Structr that is required for Event Action Mapping to work.

## Working with Data

To display collections of data, you configure an element as a repeater. The repeater executes a query and renders the element once for each result. This is the primary way to display lists, tables, and other data-driven content in Structr.

### Repeater Basics

A repeater has two essential settings: a data source and a data key. The data key is the variable name under which each object is available during rendering.

For the data source, you can choose one of three options:

- **Flow**: A Structr Flow that returns a collection
- **Cypher Query**: A Neo4j Cypher query
- **Function Query**: A script expression

Only one data source can be active at a time.

A Function Query can return data from the database, but you can also return static data directly by defining a JavaScript object or array:

`${{ [{ name: 'Draft' }, { name: 'Active' }, { name: 'Completed' }] }}`

### Filtering and Sorting

In JavaScript, you can refine your query by passing an object with filter criteria to `$.find()`. For sorting, use the `$.predicate.sort()` function.

`$.find('Project', { status: 'active' }, $.predicate.sort('name'))`

This returns all projects with status "active", sorted by name.

### Nested Repeaters

Repeaters can be nested to display hierarchical data. The inner repeater can use relationships from the outer repeater's data key as its function query.

For example, to display a list of projects with their tasks, you create an outer repeater with `find('Project')` and data key `project`. Inside, you add an inner repeater with `project.tasks` and data key `task`. The outer repeater iterates over all projects, and for each project, the inner repeater iterates over its tasks.

Note that data keys in nested repeaters must be unique. If you use the same data key in a nested repeater, the inner value overwrites the outer one.

### Dynamic Attribute Values

You can use template expressions in any HTML attribute. This allows you to create elements that change their appearance or behavior based on data, such as dynamic CSS classes, inline styles, or link URLs.

`<tr class="project-row ${project.status}">`

The `is()` function is useful for conditionally adding values. It returns null when the condition is false, which means the value is omitted from the output.

`<tr class="project-row ${is(project.isUrgent, 'urgent')}">`

Structr handles attribute values as follows: attributes with null values are not rendered at all, and attributes with an empty string are rendered as boolean attributes without a value.


## Show and Hide Conditions

Show and hide conditions control whether an element appears in the page output. Structr evaluates these conditions at render time, before the element and its children are rendered.

### How it works

Each element can have a show condition, a hide condition, or both. The element is rendered only when the show condition evaluates to true (if set) and the hide condition evaluates to false (if set). If both are set, both must be satisfied for the element to render.

Show and hide conditions are auto-script fields. You write the expression directly without the `${...}` wrapper.

### Show Conditions vs. Permissions

Show and hide conditions control visual output only. They are not a security mechanism. To restrict access to data, use visibility flags and permissions instead. For details, see the Access Control section in the Overview chapter.

### Combining List and Detail View

A common pattern in Structr is to implement both a list view and a detail view on the same page. You control which view is displayed using show and hide conditions based on the `current` keyword.

When a user navigates to `/projects`, no object is resolved and `current` is empty – the list view is displayed. When a user navigates to `/projects/a3f8b2c1-...`, Structr resolves the Project object and makes it available as `current` – the detail view is displayed.

To implement this, you create two sibling elements: one for the list view with a show condition of `empty(current)`, and one for the detail view with a show condition of `not(empty(current))`. Only one of them is rendered, depending on whether an object was resolved from the URL.

#### Side-by-Side Layout

A more advanced version displays both views side by side, similar to an email inbox. The list remains visible on the left, and the detail view appears on the right when an item is selected. You can highlight the selected item in the list by adding a dynamic CSS class that compares each item with `current`:

`${is(equal(project, current), 'selected')}`

This layout is easy to build using card components or similar block-level elements. Each card has a header and content area, one for the project list and one for the project details. This eliminates the need for separate pages and routing logic that you would typically write in other frameworks.

## Form Data

To build a form for editing an object, you bind the form fields to the object's properties using template expressions. The form displays the current values, and the user can modify them.

### Input Fields

An input field that displays the current project's name:

`<input type="text" name="name" value="${current.name}">`

A textarea with the project description:

`<textarea name="description">${current.description}</textarea>`

### Select Fields with Enum Values

For properties with a predefined set of values, you can use the `enumInfo()` function to get the possible values. Configure the `<option>` element as a repeater over the enum values, and use the `is()` function to mark the current value as selected.

`<option value="${opt}" ${is(equal(current.status, opt), 'selected')}>${opt}</option>`

### Connecting Objects

A common pattern is to display related objects in a select field. For example, to show which Project a Task belongs to, you create a select field and configure the `<option>` element as a repeater over the available projects.

The `<option>` repeater iterates over `find('Project')`. Each option has its value set to `${project.id}` and displays `${project.name}`. The current selection is marked with `${is(equal(current.project, project), 'selected')}`.

This displays all available projects in the dropdown and highlights the one that is currently assigned to the task.

## User Input

To handle user input – submitting forms, clicking buttons, and other interactions – you use Event Action Mappings. They connect DOM events to backend operations and allow you to create, update, and delete data based on user actions.

When a form is submitted, Structr can automatically resolve IDs to objects and create relationships. For example, updating a Task with `{ "project": "a3f8b2c1-..." }` automatically links the Task to that Project.

For details on handling user input, see the Event Action Mapping chapter.

## Page Functions

Structr provides several functions that are specifically designed for use in pages and templates.

### render()

The `render()` function outputs child elements at a specific position. Templates and Shared Components do not render their children automatically, so you use `render(children)` to control where they appear. You can also render specific children using `render(first(children))` or `render(nth(children, 2))`.

### include()

The `include()` function lets you include content from other elements or objects. You can include elements from other parts of the page tree or render objects from the database.

### includeChild()

The `includeChild()` function works like `include()`, but specifically for child elements. It allows you to include a child element by name or position.

### localize()

The `localize()` function returns a translated string for the current locale. You pass a key and optionally a domain. For details on translations, see the Translations section in the Pages & Templates chapter.
