# Building Your First Application

This tutorial walks you through building a simple project management application with Structr. You'll learn the essential elements of Structr by creating a real application with a custom data model, dynamic pages, and reusable components.

The best way to learn Structr is to use it. We'll build the application step by step, starting with a basic data model and simple page, then progressively adding more sophisticated features.

## What You'll Build

By the end of this tutorial, you'll have created:

- A data model with a `Project` type
- A page that displays projects from the database
- Dynamic content using repeater elements
- An imported Bootstrap template with shared components
- A reusable page template for consistent layouts

## Part 1: A First Simple Page

In this section we develop the foundation of our project management application. We'll create a simple webpage, define a data model, create test data, and connect everything together.

### Create a Page

Pages are the basic elements for rendering content in Structr. Static content displays the same way every time, while dynamic content combines markup with data from the database.

To create a new page:

1. Navigate to the Pages area by clicking the pages icon in the main menu
2. Click "Create Page" in the dropdown menu at the top-right corner of the Page Tree

This creates a fresh page with a minimal DOM tree structure, visible in the Page Tree on the left. Click on the page element to see its basic attributes.

To preview the page, click the Preview tab in the functions bar above the main area. In preview mode, you can edit static content inline by clicking on text.

The initial page structure includes a `<title>` and a `<div>` inside the body. Let's configure these elements.

### Understanding Template Expressions

The page's title contains a content element with this text:

```
${capitalize(page.name)}
```

This is a template expression. The `${` and `}` delimiters create a scripting environment where the content is interpreted rather than displayed literally.

The expression calls the `capitalize()` function with `page.name` as its argument. The `page` keyword references the current page, and `.name` accesses its name property. The result: the page title automatically reflects the page name with its first letter capitalized.

Set the page name to "overview" (click Basic or Advanced, enter the name, click outside to save). The title should now display as "Overview".

> **Note:** The title starts with an uppercase letter even though the name uses lowercase. The `capitalize()` function transforms the first character.

### Add Page Content

The `<body>` contains a `<div>` with a content element showing "Initial body text". This content isn't in `${}` delimiters, so it renders literally.

Let's replace this with a table to display projects:

1. Remove the content element by right-clicking and selecting "Remove Node"
2. On the empty `<div>`, select "Insert HTML element" → "t" → "table"
3. Build this structure:

```
<table>
    <thead>
        <tr>
            <th> with content "Project Name"
    <tbody>
        <tr>
            <td> with content "Project 1"
```

> **Note:** Removed elements go to the Unused Elements area and can be dragged back if needed.

### Create the Data Model

Our static table needs a data model to become dynamic. Navigate to the Schema area by clicking the schema icon.

Create a new type by typing "Project" into the "New type" field and clicking "Add". Wait for the schema to update.

> **Note:** Schema changes trigger a recompilation. The definition graph is modified, source code is generated and validated. Once complete, the updated schema is available throughout the application without restart or deployment.

### Create Example Data

With our Project type ready, let's create some data:

1. Navigate to the Data area
2. Filter the type list by entering "Project"
3. Click "Create new Project" three times
4. Name the projects "Project 1", "Project 2", and "Project 3" by clicking in the name cells

### Make Content Dynamic with Repeaters

A repeater element transforms data into HTML markup. It executes a database query and renders its element once for each result.

To create a repeater:

1. In the `<tbody>`, delete all `<tr>` elements except one
2. Click the remaining `<tr>` and select "Repeater"
3. Click "Function Query" and enter:

```
find('Project')
```

4. Set the Data Key to "project"
5. Click Save

The `find()` function returns all instances of the specified type. The data key "project" lets us access each result in template expressions.

> **Note:** Function queries don't need `${}` delimiters – they're auto-script environments where everything is interpreted as code.

Notice the page tree icon changed to indicate an active element. The preview shows multiple rows even though there's only one `<tr>` in the DOM – that's the repeater in action.

### Display Dynamic Data

The rows still show "Project 1" because the content is static. Replace it with:

```
${project.name}
```

Now each row displays its project's name.

To sort the results, modify the function query:

```
sort(find('Project'), 'name')
```

This completes the basic application. You now have a dynamic page displaying sorted data from the database.

## Part 2: Import a Bootstrap Template

Let's enhance the user interface by importing an existing web page as a template.

### Import the Template

1. Click "Import page" in the Page Tree dropdown menu
2. Enter the URL of a Bootstrap template (or paste HTML directly)
3. Click "Start Import"

The import runs in the background. When finished, select the new page and click Preview to see it.

### Create Shared Components

The imported page is more complex than our simple table. For larger pages, it's best to organize parts into shared components – reusable elements that can appear on multiple pages.

Benefits of shared components:

- **DRY principle:** Develop once, use everywhere. Changes automatically apply to all pages.
- **Modularity:** Group related elements logically (navigation, footer, cards).

Let's convert the navigation and footer into shared components:

1. Open the Shared Components fly-out on the right side of the Pages area
2. Drag the `<nav>` element into the drop zone
3. Click Advanced and rename it to "Navigation (top)"
4. Repeat for the footer, naming it "Footer"

> **Note:** The page output doesn't change when creating shared components – you're reorganizing without affecting the user experience.

### Configure the Cards as Repeaters

Now simplify the page body:

1. Remove all card elements except one (hover to highlight elements in preview)
2. Click the remaining card element (with CSS selector `.col-lg-6.col-xxl-4.mb-5`)
3. Configure it as a repeater with function query:

```
sort(find('Project'), 'name')
```

4. Set Data Key to "project"
5. Replace the card title with `${project.name}`

## Part 3: Create a Page Template

Templates let you define a consistent structure for multiple pages. We'll create a template that includes the navigation and footer, with a placeholder for page-specific content.

### Create the Template

1. Create a new page
2. Delete the `<html>` node
3. Add a template element as the page's only child
4. Copy the HTML from your styled page into the template
5. Set the template's content type to `text/html`

### Use Include Functions

Replace the navigation HTML:

```html
<nav>
...
</nav>
```

with:

```
${include('Navigation (top)')}
```

Do the same for the footer: `${include('Footer')}`

### Add Dynamic Content Placeholder

Replace the main content section:

```html
<!-- Page Content-->
<section class="pt-4">
...
</section>
```

with:

```html
<!-- Page Content-->
${render(children)}
```

The `render(children)` expression renders any child elements of the template.

### Make It a Shared Component

1. Name the template "Main Page Template"
2. Drag it to the Shared Components area

Now you can use this template on any new page. Add the template as a shared component, then add page-specific content as its children.

## Next Steps

You've built a functional application with:

- A custom data model
- Dynamic pages using repeaters
- Imported styles and templates
- Reusable shared components

To extend this application, consider:

- Adding more types (Task, Deadline) with relationships to Project
- Implementing editing functionality with Event Action Mapping
- Adding images using Structr's file system integration
- Creating user authentication and permissions

> **Note:** When creating relationships in the schema, use unique, descriptive names. Generic names like `HAS` or `IS_PART_OF` can hurt performance if reused across different type pairs.
