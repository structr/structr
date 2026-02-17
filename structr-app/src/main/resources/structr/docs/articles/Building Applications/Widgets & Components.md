Widgets and Shared Components are two mechanisms for reusing page elements across your application. Shared Components are referenced elements that stay in sync everywhere they are used. Widgets are source code snippets that produce page elements when inserted, and can define Shared Components in the process. Together, they form the basis for building widget libraries with enforced nesting rules.


## Shared Components
A Shared Component is a reusable structure of HTML elements that you can insert into any page via drag and drop. Unlike a Widget, where Structr copies the content into the page, inserting a Shared Component creates a reference to the original. When you edit a Shared Component, the changes are immediately visible on all pages that use it. A typical example is the Main Page Template, which defines the overall layout and is shared across all pages of an application.

### How it works
When you drag a Shared Component onto a page, Structr creates a copy of the root element that is linked to the original via a SYNC relationship. This link ensures that changes to the original Shared Component are automatically propagated to all copies.

This has two important consequences:

1. **Single source of truth**: The Shared Component exists only once. Any changes you make to it are immediately reflected everywhere it is used.

2. **Smaller page trees**: Pages that use Shared Components contain only the linked root element, not copies of the entire element structure.

### Creating Shared Components
To create a Shared Component, select an element in the page tree, right-click, and select "Create Shared Component". Structr moves the element and all its children into a new Shared Component and replaces it with a reference.

Alternatively, you can drag an element from the page tree into the Shared Components area to convert it into a Shared Component.

Once created, you can work with Shared Components the same way you work with elements in the page tree, including context menus and all editing features.

### Deleting Shared Components
To delete a Shared Component, remove it in the Shared Components area. The reference elements on the pages where it was used are converted into regular elements and keep their children.

To remove a Shared Component from a page without deleting the original, simply delete the reference element in the page tree.

### Rendering children
Like templates, Shared Components do not automatically render their children. You must call `render(children)` to define where child elements appear. This gives you full control over the layout and lets you create components with multiple insertion points.

```html
<header>
    <nav>
        <a href="/">Home</a>
        <a href="/about">About</a>
    </nav>
    <div class="page-title">
        ${render(children)}
    </div>
</header>
```

This Shared Component defines a header with navigation. The `render(children)` call marks where child elements appear when the component is used on a page.

### Customization at render time
To customize a Shared Component before rendering, you can use the `sharedComponentConfiguration` property on the reference element. If present, Structr evaluates this expression before rendering continues with the Shared Component.

This is useful when you need to adapt a Shared Component based on the context where it is used. For example, you can pass data to a generic table component:

```javascript
$.store('data', $.find('Customer', $.predicate.sort('name')));
```

The Shared Component retrieves the data with `$.retrieve('data')` and displays the results. This way, the same table component can show different data on each page.

### Synchronization of Attributes
The SYNC relationship connects the reference element in the page with the root element of the Shared Component. When you rename a reference element in a page, the change is automatically applied to the original Shared Component. When you change the visibility of a Shared Component, Structr asks whether the changes should be applied to the reference elements as well.

Note that Widgets reference Shared Components by name. If you rename a Shared Component, Widgets that use the old name will create a new Shared Component instead of reusing the existing one.



## Widgets
Widgets are reusable building blocks for your pages. They can range from simple HTML snippets to complete, configurable components with their own logic and styling. You can use Widgets in several ways:

- Drag Widgets from the flyout to insert them into your pages
- Create page templates from Widgets to provide starting points for new pages
- Configure Widgets with variables that are filled in when inserting them
- Make Widgets appear as suggestions in the context menu for specific element types
- Share Widgets across applications using remote Widget servers

### Using Widgets
To add a Widget to your page, drag it from the Widgets flyout into the page tree. If the Widget has configuration options, a dialog appears where you can fill in the required values before the Widget is inserted.

Widgets can also appear in the context menu as suggested Widgets. When a Widget's selector matches the current element, it appears under "Suggested Widgets" and can be inserted directly as a child element.

#### Page Templates
Widgets with the "Is Page Template" flag enabled appear in the "Create Page" dialog. When you create a page from a template, Structr imports the complete Widget structure including content, repeaters, permissions, and shared components. This provides a quick starting point for common page layouts.

### How it works
Widgets are stored as objects in the database with an HTML source code field. When you insert a Widget into a page, Structr parses the source code and creates the corresponding page elements.

In their simplest form, Widgets contain plain HTML. Structr parses the markup and creates the corresponding elements directly in the page tree. The resulting elements have no connection to the Widget source - they are independent copies. Inserting the same Widget twice creates two separate sets of elements.

Widgets can also define Shared Components using `<structr:shared-template>` tags. This works differently: Structr creates the Shared Components only once. When the Widget is inserted again, Structr recognizes that the Shared Components already exist and reuses them instead of creating duplicates. This distinction matters when building a Widget library, where each Widget should produce a Shared Component that is consistent across all pages. See the Building a Widget Library section for details.

If the Widget contains template expressions in square brackets like `[variableName]`, Structr checks the configuration for matching entries and displays a dialog where you fill in the values before insertion.

Widgets can contain deployment annotations that preserve Structr-specific attributes like content types and visibility settings. Enable `processDeploymentInfo` in the Widget configuration to use this feature.

### The Widgets flyout
The Widgets flyout is divided into two sections: local Widgets stored in the database, and remote Widgets fetched from external servers.

#### Local Widgets
Local Widgets are stored in your application's database. Click the plus button in the upper right corner of the flyout to create a new Widget. The Widget appears in the list and can be dragged into the page tree. Right-click a Widget to open the context menu, where you can edit the Widget or select "Advanced" to access all attributes, including paths for thumbnails and icons.

##### Categorizing Widgets
Use the `treePath` attribute to organize Widgets into categories. The attribute contains a slash-separated path that defines nested categories. The string must begin with a slash, and categories can contain spaces. For example: `/Forms/Input Elements` creates a category "Forms" with a subcategory "Input Elements".

#### Remote Widgets
Remote Widgets are fetched from external Structr servers. The Widgets on the remote server must be publicly visible. Use the "Configure Servers" dialog to add servers. The dialog shows a list of configured servers, with the default server that cannot be removed. Below the list, enter a name and URL for a new server and click save.

### Editing Widgets
The Widget editor has five tabs: Source, Configuration, Description, Options, and Help.

#### Source
The Source tab contains the HTML source code of the Widget, which can include Structr expressions.

The easiest way to create this source is to build the functionality in a Structr page and then export it. Add the `edit=1` URL parameter to view the page source with Structr expressions and configuration attributes intact, without evaluation. For example:

1. Create your Widget in the page "myWidgetPage"
2. Go to `http://localhost:8082/myWidgetPage?_edit=1`
3. View and copy the source code of that page
4. Paste it into the Source tab

##### Setting Structr Attributes in Widget Source

Widget source code is plain HTML, but Structr elements have attributes that do not exist in HTML, such as `componentType` or `contentType`. To set these attributes during widget import, use `data-structr-meta-` prefixed HTML attributes.

The naming convention is: take the Structr attribute name in camelCase, convert it to kebab-case, and prefix it with `data-structr-meta-`. When Structr imports the widget, it strips the prefix, converts the remainder back to camelCase, and sets the corresponding attribute on the element.

| HTML attribute | Structr attribute |
| -------------- | ----------------- |
| `data-structr-meta-component-type` | `componentType` |
| `data-structr-meta-content-type` | `contentType` |
| `data-structr-meta-dimensions` | `dimensions` |
| `data-structr-meta-show-conditions` | `showConditions` |
| `data-structr-meta-hide-conditions` | `hideConditions` |

This mechanism works for any Structr attribute, not just the ones listed above.

#### Configuration
The Configuration tab allows you to make Widgets configurable by inserting template expressions in the Widget source. Template expressions use square brackets like `[configSwitch]` and can contain any characters except the closing bracket. When a corresponding entry exists in the configuration, Structr displays a dialog when adding the Widget to a page.

Elements that look like template expressions are only treated as such if a corresponding entry is found in the configuration. This allows the use of square brackets in the Widget source without interpretation as template expressions.

The configuration must be a valid JSON string. Here is an example:

    {
        "configSwitch": {
            "position": 2,
            "default": "This is the default text"
        },
        "selectArray": {
            "position": 3,
            "type": "select",
            "options": [
                "choice_one",
                "choice_two",
                "choice_three"
            ],
            "default": "choice_two"
        },
        "selectObject": {
            "position": 1,
            "type": "select",
            "options": {
                "choice_one": "First choice",
                "choice_two": "Second choice",
                "choice_three": "Third choice"
            },
            "default": "choice_two"
        },
        "processDeploymentInfo": true
    }

The reserved top-level key `processDeploymentInfo` (boolean, default: false) allows Widgets to contain deployment annotations.

Configuration elements support the following attributes:

| Attribute | Applies to | Description |
|-----------|------------|-------------|
| `title` | all | The title displayed in the dialog. If omitted, the template expression name is used. |
| `placeholder` | input, textarea | The placeholder text displayed when the field is empty. If omitted, the title is used. |
| `default` | all | The default value. For input and textarea, this value is prefilled. For select, this value is preselected. |
| `position` | all | A numeric value for sorting options. Elements without a position appear after those with a position, in natural key order. |
| `help` | all | Help text displayed when hovering over the information icon. |
| `type` | all | The input type. Supported values are `input` (default), `textarea`, and `select`. |
| `options` | select | An array of strings or an object with value-label pairs. Arrays render as simple options. Objects use the key as the value and the object value as the displayed text. |
| `dynamicOptionsFunction` | select | A function body that populates the options array. The function receives a `callback` parameter that must be called with the resulting options. If provided, the `options` key is ignored. |
| `rows` | textarea | The number of rows. Defaults to 5. |

#### Description
The Description tab contains text that is displayed when the user adds the Widget to a page. It can contain HTML and is typically used to explain what the Widget does and how to use the configuration options. The description is only displayed when the Widget is a page template.

#### Options
The Options tab contains two settings:

- `Selectors`: Controls under which elements the Widget appears as a suggested Widget in the context menu. Selectors are written as CSS selectors that match against the parent element. For matching purposes, the `componentType` attribute is available as `type`. For example, the selector `*[type='container']` makes a Widget appear as a suggestion inside any element with componentType `container`. Multiple selectors can be combined: `*[type='container'], *[type='form']`.
- `Is Page Template`: Check this box to make the Widget available as a page template when creating a new page.

### Widgets can define Shared Components
Use `<structr:shared-template name="...">` to define a Shared Component in the Widget source, and `<structr:template src="...">` to reference it. The source code has two parts: first the definitions, then the references that assemble them into a page structure. See the Building a Widget Library section below for a detailed example.

### Shared Components vs. Widgets

| Aspect | Widget | Shared Component |
|--------|--------|------------------|
| Storage | External source code | Part of your application |
| Insertion | Creates a copy | Creates a reference |
| Changes | Only affect new insertions | Immediately visible everywhere |
| Use case | Starting points, boilerplate | Consistent layouts, headers, footers |



## The Component Type System

The component type system controls which elements can be nested inside each other and which widgets can replace each other. It uses two attributes: `componentType` determines where a widget can be inserted, and `dimensions` determines which widgets are compatible for replacement.

### Component Types

The `componentType` attribute assigns each element a role in a nesting hierarchy. When a componentType is set on a widget, Structr enforces the nesting rules when the user inserts the widget into a page.

| componentType   | Description                                                              |
| --------------- | ------------------------------------------------------------------------ |
| **canvas**      | The root element. Only accepts top-level structure elements.             |
| **container**   | Structural container for layout and grouping.                            |
| **form**        | A container with submit semantics. Accepts inputs directly.              |
| **content**     | A leaf element that displays content. Cannot have children.              |
| **input**       | A leaf element for user input. Cannot have children.                     |
| **action**      | A leaf element that triggers an action, such as a button or a link.      |

The componentType is set on the `<structr:shared-template>` tag using the `data-structr-meta-component-type` attribute. The selectors in the widget Options tab use `type` to match against the parent element's componentType.

### Dimensions

The `dimensions` attribute describes how a widget organizes its content areas. The value corresponds to the number of axes along which content repeats:

| dimensions | Meaning | Content structure | Examples |
| ---------- | ------- | ----------------- | -------- |
| **0** | A single object | Fixed layout with one or more named areas, no repetition | Panel, Panel with Header |
| **1** | A list | A repeating sequence of identical content areas | List, Accordion, Tabs |
| **2** | A table | Content areas arranged in rows and columns | Table, Grid |

These values form a hierarchy: dimension 0 is a special case of dimension 1 (a list with exactly one item), and dimension 1 is a special case of dimension 2 (a table with one column). This means a widget with fewer dimensions can always be replaced by one with more dimensions. The reverse is possible but may lose data - for example, replacing a List with a Panel keeps only the first item.

The dimensions attribute also determines which data sources are compatible with a widget. A widget with dimensions 0 expects a single object, a widget with dimensions 1 expects a collection, and a widget with dimensions 2 expects a collection with multiple properties per item.

#### Replacing Widgets

The context menu offers a "Replace Widget" option that shows compatible replacement widgets. A widget is compatible when it has the same `componentType` and the same or higher `dimensions` value. This prevents replacements that would break the page structure or silently discard content.

| Original        | Compatible replacements                    |
| --------------- | ------------------------------------------ |
| Panel (0)       | Panel with Header (0), List (1), Accordion (1), Tabs (1), Table (2), Grid (2) |
| List (1)        | Accordion (1), Tabs (1), Table (2), Grid (2) |
| Table (2)       | Grid (2)                                   |

Replacing a widget with one of lower dimensions is also offered, but Structr shows a warning when content would be lost.

### Configuring Selectors

The Allowed Parents column in the following table determines which selectors to configure for each widget. For example, Paragraph has allowed parents `container` and `form`, so its selectors should be `*[type='container'], *[type='form']`. This makes the Paragraph widget appear as a suggestion in the context menu whenever the user right-clicks inside a container or form.

| Widget              | componentType | dimensions | Allowed Parents              | Selectors                                                  |
| ------------------- | ------------- | ---------- | ---------------------------- | ---------------------------------------------------------- |
| Main Page Template  | canvas        | -          | *(root)*                     | *(page template, no selectors needed)*                     |
| Panel               | container     | 0          | canvas, container, form      | `*[type='canvas'], *[type='container'], *[type='form']`    |
| Panel with Header   | container     | 0          | canvas, container, form      | `*[type='canvas'], *[type='container'], *[type='form']`    |
| Grid                | container     | 2          | canvas, container, form      | `*[type='canvas'], *[type='container'], *[type='form']`    |
| Accordion           | container     | 1          | canvas, container, form      | `*[type='canvas'], *[type='container'], *[type='form']`    |
| List                | container     | 1          | canvas, container, form      | `*[type='canvas'], *[type='container'], *[type='form']`    |
| Table               | container     | 2          | canvas, container, form      | `*[type='canvas'], *[type='container'], *[type='form']`    |
| Tabs                | container     | 1          | canvas, container, form      | `*[type='canvas'], *[type='container'], *[type='form']`    |
| Form                | form          | -          | canvas, container            | `*[type='canvas'], *[type='container']`                    |
| Paragraph           | content       | -          | container, form              | `*[type='container'], *[type='form']`                      |
| Textfield           | input         | -          | container, form              | `*[type='container'], *[type='form']`                      |
| Textarea            | input         | -          | container, form              | `*[type='container'], *[type='form']`                      |
| Button              | action        | -          | container, form              | `*[type='container'], *[type='form']`                      |
| Link                | action        | -          | container, form              | `*[type='container'], *[type='form']`                      |

Leaf elements (content, input, action) and form do not have a dimensions value because they do not contain repeating content areas.



## Building a Widget Library

A widget library is a collection of widgets that work together using the component type system. Each widget is defined as a Shared Component with a componentType, dimensions value, and selectors that enforce the nesting and replacement rules.

### Defining Widgets

The `<structr:shared-template>` tag that defines each widget can carry `data-structr-meta-` attributes to set Structr attributes like `componentType` and `dimensions`. Container widgets use `render(children)` to mark the content area where child elements can be inserted. Leaf widgets that do not accept children omit this call.

The following example defines a widget with four Shared Components and assembles them into a page layout:

    <!-- Shared Component definitions -->

    <structr:shared-template name="Main Page Template"
        data-structr-meta-content-type="text/html">
        <html>
            <head>
                <title>${page.name}</title>
            </head>
            <body>
                ${render(children)}
            </body>
        </html>
    </structr:shared-template>

    <structr:shared-template name="Main Content"
        data-structr-meta-component-type="canvas"
        data-structr-meta-content-type="text/html">
        <main>${render(children)}</main>
    </structr:shared-template>

    <structr:shared-template name="Panel"
        data-structr-meta-component-type="container"
        data-structr-meta-dimensions="0"
        data-structr-meta-content-type="text/html">
        <div class="panel panel-content">
            ${render(children)}
        </div>
    </structr:shared-template>

    <structr:shared-template name="Paragraph"
        data-structr-meta-component-type="content"
        data-structr-meta-content-type="text/html">
        <p>Text</p>
    </structr:shared-template>

    <!-- References: assemble the page structure -->

    <structr:template src="Main Page Template">
        <structr:template src="Main Content">
            <structr:template src="Panel">
                <structr:template src="Paragraph"></structr:template>
            </structr:template>
        </structr:template>
    </structr:template>

The definitions at the top create the Shared Components. The references at the bottom assemble them into a nested structure: Main Page Template contains Main Content, which contains a Panel with a Paragraph inside. When this widget is inserted, Structr creates the Shared Components and builds the page tree from the references. Inserting the widget again reuses the existing Shared Components instead of creating duplicates.

### Self-Contained Widgets

Some container widgets manage their internal structure automatically. Widgets like Grid, Accordion, Tabs, and Table provide multiple content areas that each accept child components as if they were containers. The user does not create internal elements like grid cells or accordion items directly. Instead, the widget manages its internal structure and exposes content areas for the user to fill.

Configuration of the internal structure (e.g. number of columns in a grid, number of tabs, table columns and rows) is handled through widget properties, not through manual nesting.

### Organizing Widgets

Use the `treePath` attribute to organize widgets into categories in the Widgets flyout. For a widget library, a structure based on component types works well:

    /Layout/Panel
    /Layout/Panel with Header
    /Layout/Grid
    /Content/Paragraph
    /Input/Textfield
    /Input/Textarea
    /Action/Button
    /Action/Link

### Using the Widget Library

Once the widgets are configured with component types, dimensions, and selectors, the library integrates into the normal page building workflow. Create a new page from the Main Page Template. The page starts with a canvas element that accepts container and form widgets. Right-click the canvas to see the suggested widgets - only widgets whose selectors match appear in the menu. Insert a Panel, then right-click inside the Panel to see the next level of suggestions: other containers, content elements, inputs, and actions. The component type system guides the user through building a valid page structure step by step, without requiring knowledge of the nesting rules.

To replace a widget, right-click it and select "Replace Widget". Structr shows only compatible widgets based on the componentType and dimensions of the selected element. Replacing a List with an Accordion preserves all items. Replacing a List with a Panel triggers a warning because only the first item can be kept.
