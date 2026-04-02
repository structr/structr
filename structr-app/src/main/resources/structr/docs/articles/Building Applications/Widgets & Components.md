Widgets and Shared Components are two mechanisms for reusing page elements across your application. Shared Components are referenced elements that stay in sync everywhere they are used. Widgets are source code snippets that produce page elements when inserted, and can define Shared Components in the process. Together, they form the basis for building widget libraries with enforced nesting rules and data-driven components.

The widget system consists of three subsystems that are cleanly separated from each other by design. The widget type system controls where a widget can be inserted and how widgets relate to each other structurally. The widget replacement mechanism controls what happens when you swap one widget for another. Data-driven components are widgets that render their content from a DataSource and a ComponentConfiguration, with no manually inserted content.


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

To remove a Shared Component from a page without deleting the original, delete the reference element in the page tree.

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

- Right-click any element to see suggested Widgets that fit the current position in the page tree
- Use Widgets as page templates that provide starting points for new pages
- Configure Widgets with variables that are filled in when inserting them
- Drag Widgets from the flyout as an alternative way to insert them into your pages
- Share Widgets across applications using remote Widget servers

The component type system, suggested Widgets, and the widget replacement mechanism all work with local Widgets only. Remote Widgets can be inserted via drag and drop but do not participate in the component type logic.

### Using Widgets

The primary way to insert a Widget is through the context menu. When you right-click an element in the page tree, Structr checks which Widgets have selectors that match the current element and shows them under "Suggested Widgets". This approach respects the component type system and only offers Widgets that are compatible with the current position in the page tree.

As an alternative, you can drag a Widget from the Widgets flyout directly into the page tree. This method does not enforce the component type rules, so you are responsible for placing the Widget in a valid position.

If the Widget has configuration options, a dialog appears where you can fill in the required values before the Widget is inserted.

#### Page Templates

Widgets with the "Is Page Template" flag enabled appear in the "Create Page" dialog. When you create a page from a template, Structr imports the complete Widget structure including content, repeaters, permissions, and shared components. This provides a quick starting point for common page layouts.

### How it works

Widgets are stored as objects in the database with an HTML source code field. When you insert a Widget into a page, Structr parses the source code and creates the corresponding page elements.

In their simplest form, Widgets contain plain HTML. Structr parses the markup and creates the corresponding elements directly in the page tree. The resulting elements have no connection to the Widget source. They are independent copies, and inserting the same Widget twice creates two separate sets of elements.

Widgets can also define Shared Components using `<structr:shared-template>` tags. In that case, Structr creates the Shared Components only once. When you insert the Widget again, Structr recognizes that the Shared Components already exist and reuses them instead of creating duplicates. This is the mechanism behind widget libraries, where each Widget should produce consistent Shared Components across all pages. See the Building a Widget Library section for details.

Widget source code can contain template variables in square brackets like `[variableName]`. Structr only treats a square bracket expression as a template variable when a key with the same name exists in the Widget configuration. All other square bracket expressions are left unchanged. When a Widget has at least one recognized template variable, Structr displays a dialog where you fill in the values before insertion. See the Configuration section below for details on defining these variables.

Widgets can contain deployment annotations that preserve Structr-specific attributes like content types and visibility settings. Enable `processDeploymentInfo` in the Widget configuration to use this feature.

### The Widgets flyout

The Widgets flyout is divided into two sections: local Widgets stored in the database, and remote Widgets fetched from external servers.

#### Local Widgets

Local Widgets are stored in your application's database. Click the plus button in the upper right corner of the flyout to create a new Widget. The Widget appears in the list and can be dragged into the page tree. Right-click a Widget to open the context menu, where you can edit the Widget or select "Advanced" to access all attributes, including paths for thumbnails and icons.

##### Categorizing Widgets

Use the `treePath` attribute to organize Widgets into categories. The attribute contains a slash-separated path that defines nested categories. The string must begin with a slash, and categories can contain spaces. For example: `/Forms/Input Elements` creates a category "Forms" with a subcategory "Input Elements".

#### Remote Widgets

Remote Widgets are fetched from external Structr servers. The Widgets on the remote server must be publicly visible. Use the "Configure Servers" dialog to add servers. The dialog shows a list of configured servers, with the default server that cannot be removed. Below the list, enter a name and URL for a new server and click save.

Remote Widgets can only be inserted via drag and drop. They do not appear as suggested Widgets, and they are not available for widget replacement.

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

The Configuration tab defines template variables for the Widget. Template variables use square brackets in the Widget source, for example `[configSwitch]`. Each variable needs a corresponding key in the configuration JSON. Only square bracket expressions with a matching key are treated as variables; all others are ignored.

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

The Description tab contains text that is displayed when you add the Widget to a page. It can contain HTML and is typically used to explain what the Widget does and how to use the configuration options. The description is only displayed when the Widget is a page template.

#### Options

The Options tab contains two settings:

- `Selectors`: CSS selectors that control under which elements the Widget appears as a suggested Widget in the context menu. For example, `[type='container']` makes the Widget appear inside any element with componentType `container`. Multiple selectors can be combined: `[type='container'], [type='form']`.
- `Is Page Template`: Check this box to make the Widget available as a page template when creating a new page.

### Shared Components vs. Widgets

| Aspect | Widget | Shared Component |
|--------|--------|------------------|
| Storage | External source code | Part of your application |
| Insertion | Creates a copy | Creates a reference |
| Changes | Only affect new insertions | Immediately visible everywhere |
| Use case | Starting points, boilerplate | Consistent layouts, headers, footers |



## The Component Type System

The component type system controls where widgets can be inserted and how they relate to each other structurally. It uses two attributes: `componentType` determines where a widget can be placed in the page tree, and `dimensions` describes the shape of data a widget is designed to display.

### Component Types

The `componentType` attribute assigns each element a role in a nesting hierarchy. The values are defined freely by the widget set author. Structr does not impose a fixed set of component types. The selectors on each widget are standard CSS selectors that match against the parent element's attributes, so the widget author has full control over the nesting rules.

The Structr widget library uses the following component types:

| componentType   | Description                                                              |
| --------------- | ------------------------------------------------------------------------ |
| **canvas**      | The root element. Only accepts top-level structure elements.             |
| **container**   | Structural container for layout and grouping.                            |
| **form**        | A container with submit semantics. Accepts inputs directly.              |
| **content**     | A leaf element that displays content. Cannot have children.              |
| **input**       | A leaf element for user input. Cannot have children.                     |
| **action**      | A leaf element that triggers an action, such as a button or a link.      |

You set the componentType on the `<structr:shared-template>` tag using the `data-structr-meta-component-type` attribute. The selectors in the widget Options tab match against the parent element. For example, `[type='container']` makes the widget appear as a suggestion inside any element whose componentType is `container`.

### Dimensions

The `dimensions` attribute describes how a widget organizes its content areas. The value corresponds to the number of axes along which content repeats:

| dimensions | Meaning | Content structure | Examples |
| ---------- | ------- | ----------------- | -------- |
| **0** | A single object | Fixed layout with one or more named areas, no repetition | Panel, Panel with Heading |
| **1** | A list | A repeating sequence of identical content areas | List, Accordion |
| **2** | A table | Content areas arranged in rows and columns | Table, Grid |

For data-driven components, the `dimensions` value also determines which DataSources are compatible. A component with dimensions 0 expects a single object, dimensions 1 expects a collection, and dimensions 2 expects a collection with multiple properties per item. See the Data-Driven Components section for details.

Leaf elements (content, input, action) and form do not have a dimensions value because they do not contain repeating content areas.


### Widget Replacement

When you replace a widget, Structr looks for matching content areas and repeaters in the old widget and transfers them to the new one. Content areas are matched by `itemType`, which describes the semantic role of a content region. Repeaters are matched by `repeaterType`, which describes how a repeater iterates. Both attributes are set freely by the widget set author, and matching works by string equality between the old and new widget's template elements.

The `itemType` and `repeaterType` attributes belong exclusively to the replacement mechanism. They are distinct from `componentType` and from the ComponentConfiguration concepts described later.

Data-driven components are not subject to the content-transfer mechanism. When you replace one data-driven component with another, Structr transfers only the DataSource binding, since there is no manually placed content to preserve.



## Building a Widget Library

A widget library is a set of Widgets that are designed to work together using the component type system. Each Widget defines one or more Shared Components in its source code, with `componentType`, `dimensions`, and `selectors` attributes that enforce the nesting rules. When you insert a Widget from the library, Structr creates the Shared Components if they do not already exist, or reuses them if they do. A library can contain both static widgets and data-driven components.

A good starting point for building your own library is to study the Structr default widget library. The default Widgets are available on every Structr instance and demonstrate all the patterns described in this section.

### Defining Widgets

You define widgets in the Widget source code using `<structr:shared-template>` tags for the Shared Component definitions and `<structr:template>` tags to reference them. The `<structr:shared-template>` tag can carry `data-structr-meta-` attributes to set Structr attributes like `componentType` and `dimensions`. Container widgets use `render(children)` to mark the content area where child elements can be inserted. Leaf widgets that do not accept children omit this call.

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

The definitions at the top create the Shared Components. The references at the bottom assemble them into a nested structure: Main Page Template contains Main Content, which contains a Panel with a Paragraph inside. When you insert this widget, Structr creates the Shared Components and builds the page tree from the references. Inserting the widget again reuses the existing Shared Components instead of creating duplicates.

### Multi-Area Widgets

Some container widgets provide more than one content area. The Accordion widget, for example, has multiple collapsible sections that each accept child components. The Card with Header widget has a header area and a content area. In both cases, the widget manages its internal structure and exposes the content areas for you to fill. You do not create the internal elements directly; the widget handles that.

### Organizing Widgets

Use the `treePath` attribute to organize widgets into categories in the Widgets flyout. For a widget library, a structure based on component types works well:

    /Layout/Panel
    /Layout/Panel with Heading
    /Layout/Grid
    /Content/Paragraph
    /Input/Textfield
    /Input/Textarea
    /Action/Button
    /Action/Link

### Using the Widget Library

Once you have configured your widgets with component types, dimensions, and selectors, the library integrates into the normal page building workflow. Create a new page from the Main Page Template. The page starts with a canvas element that accepts container and form widgets. Right-click the canvas to see the suggested widgets; only widgets whose selectors match appear in the menu. Insert a Panel, then right-click inside the Panel to see the next level of suggestions: other containers, content elements, inputs, and actions. The component type system guides you through building a valid page structure step by step, without requiring knowledge of the nesting rules.



## Data-Driven Components

The widgets described so far (panels, grids, paragraphs, buttons) are static building blocks. You assemble them into pages and place content into them manually. Data-driven components work differently: you point them at a DataSource such as a schema type, configure which fields to display, and the component renders the data at runtime. A Table component bound to a `Customer` type, for example, automatically renders a table with rows for each customer and columns for the configured fields. An Edit Form bound to the same type renders input fields for editing a single customer record.

Each data-driven component has a ComponentConfiguration that controls which fields are displayed, in what order, and how each field is rendered. The DataSource delivers the raw data along with field metadata (for schema types), and the ComponentConfiguration can extend or override this metadata, for example by adding a custom render template or changing the label for a specific field. Because the configuration is stored per component, two tables bound to the same schema type can show different fields with different render templates. You set all of this up in the component configuration dialog, without writing code or building the HTML structure by hand.

This separation of data and presentation means that you can change the visual layout without touching the underlying data, and you can switch the DataSource without rebuilding the component. It also enables controller-subscriber patterns where selecting a record in one component (for example a table) automatically updates another component (for example a detail form) on the same page.

### How a Data-Driven Component is Created

A template becomes a data-driven component when a ComponentConfiguration object is attached to it. This happens when the template's widget source contains a `<structr:template>` element with a `config` attribute. The `config` attribute causes Structr to create a ComponentConfiguration object when the widget is inserted into a page. For example:

```html
<structr:template src="Table" config="{ dataSource: '[dataSource]', reload: 'partial' }"></structr:template>
```

The `config` value is only the initial bootstrap. You assign the DataSource and configure all other settings after insertion. Templates without a ComponentConfiguration are treated as static widgets.

You insert a data-driven component the same way you insert any other widget: right-click an element in the page tree and select it from the suggested widgets, or drag it from the Widgets flyout. Once inserted, select the template in the page tree. Structr shows the component configuration dialog in the General tab of the properties panel, where you set up the DataSource, choose fields, and configure the component's behavior.


### The Component Configuration Dialog

The component configuration dialog is organized from top to bottom as follows.

The first row contains the DataSource and Selection fields. You enter the name of a schema type or a ScriptDataSource in the DataSource field, and optionally a Selection expression and a channel name. The Selection and channel are used when the component acts as a subscriber (see Channels below).

The second row contains the Display Mode and the Role. The display mode controls whether the component renders in read mode (`output`) or edit mode (`input`). The role determines whether the component acts as a controller or a subscriber.

The next row contains the Reload Behavior and the Page Size. Reload behavior controls how the component updates when its data changes (full page reload or partial). Page size sets the number of records per page for collection components.

Below that, a Show Labels checkbox controls whether field labels are rendered, and a column width selector lets you set the component's width in the 6-column grid. The column width selector works visually: hover over a column marker and all columns to the left are highlighted, so you can set the desired width by clicking.

#### Configured Fields and Available Fields

The lower part of the dialog shows two lists: Configured Fields at the top and Available Fields below.

When you first assign a DataSource, the Configured Fields list contains only the `name` field. The Available Fields list shows all fields delivered by the DataSource. For a SchemaNode DataSource, these are the properties of the schema type. For a ScriptDataSource, this list is empty because ScriptDataSources do not deliver field metadata; you must add fields manually in that case.

To add a field, click it in the Available Fields list. Structr moves it into the Configured Fields list and adds it to the component's field set. Only fields in the Configured Fields list are rendered, and the order controls the render order. You can reorder fields by dragging the handle on the right side of each entry. To remove a field, uncheck its checkbox.

Click a field to expand its detail settings:

- The **label** displayed for the field
- The **value expression** that determines what data is rendered
- The **sort key** for sorting when the user clicks the column header
- The **columns** (1 to 6) the field occupies in the grid layout
- The **data type** that controls which render templates are offered (for example `date`, `string`, or `node`)
- The **render template** (or edit template in input mode) that controls how the field value is displayed
- The **Include in Filter** checkbox that controls whether the field participates in text filtering

A field that matches a DataSource field by name augments the DataSource's metadata with whatever you configure here. A field with no corresponding DataSource field is sourced entirely from the configuration.


### DataSources

A DataSource delivers data to a component. Every data-driven component references a DataSource by name.

#### SchemaNode DataSource

Every schema type in Structr automatically acts as a DataSource that delivers all instances of that type. Enter the type name in the DataSource field, and Structr populates the Available Fields list with the properties of that schema type. This is the most common DataSource for data-driven components.

#### ScriptDataSource

A ScriptDataSource executes an arbitrary JavaScript or StructrScript expression at runtime and returns its result as the data for the component. Because a ScriptDataSource does not deliver field metadata, all fields must be configured manually. ScriptDataSources are more flexible but also more work to set up. They are the right choice when the data does not come from a single schema type, or when the query requires joins, aggregations, or other transformations.

#### ChannelDataSource

A ChannelDataSource is not a node that you create. Structr derives it implicitly when you set a component up as a subscriber. It delivers a single record identified by a UUID stored in a URL parameter. The built-in channel `current` stores its UUID in the URL path segment; any other channel stores its UUID as a query parameter named after the channel (for example `project.id=<uuid>`).


### Channels

A channel connects a controller component to its subscribers. When a user selects a record in a controller, that component writes the selected record's UUID into a URL parameter. Any subscriber reading from the same channel detects the new value and reloads with the newly selected record.

This requires no explicit event wiring. The URL parameter is the shared state. Structr builds a dependency graph from the DataSource and channel relationships of all components on the page and uses it to determine the correct reload order when a selection changes.

The built-in channel `current` is always available. Its UUID is stored in the URL path segment and is typically used on detail pages where the record identity is part of the URL.

#### Controllers and Subscribers

A controller writes to a channel when a user interacts with it. A subscriber reads from a channel to determine which record to display. You set the role in the component configuration dialog.

A typical pattern is a list-detail layout: a Table component (controller) shows a list of projects, and an Edit Form (subscriber) shows the details of the selected project. When the user clicks a row in the table, the table writes the project UUID to the channel, and the form reloads with that project's data.

#### The Selection

Sometimes a subscriber needs to display not the selected record itself, but a related collection. In that case, you add a Selection expression in the component configuration. For example, a task list on a project detail page can use `current` as its channel and `.tasks` as the Selection. Structr evaluates `current.tasks` to read the selected project and traverse its tasks relation, then applies pagination, sorting, and filtering to the result.

The Selection belongs to the component configuration, not to the channel. Multiple components on the same page can share a channel while each uses a different Selection.

#### selectedValue and currentValue

In templates, two keywords give access to the current record depending on context.

`selectedValue` is the record a subscriber received via its channel. It exists once per component render and is only available in subscribers. A detail form uses `selectedValue` to access the record the user selected in a list.

`currentValue` is the iteration variable inside a `renderEach()` loop. It changes with every iteration and is only available within that loop scope. A table row template uses `currentValue` to access the record being rendered.

These two keywords are distinct. Using the wrong one produces either an empty value or data from the wrong context.


### Rendering Functions

Data-driven components use a set of built-in functions to render their content. These functions are available inside component templates and handle the repetitive work of iterating over data and rendering fields.

#### renderEach()

`renderEach()` iterates over the collection delivered by the DataSource and renders the component's template once for each record. Inside the loop, the keyword `currentValue` gives access to the record being rendered. This is the primary function for collection components like tables and lists.

#### renderFields()

`renderFields()` renders the Configured Fields for the current record. It iterates over the fields in order and applies each field's render template (or edit template in input mode). You can pass a slot name as an argument to render only the fields assigned to that slot. Without an argument, all Configured Fields are rendered in order, which is sufficient for most components.

#### renderLabels()

`renderLabels()` renders the labels of the Configured Fields without any data values. It iterates over the field list and uses the render templates to display each field's label instead of a data value. This is typically used for table headers: the `<thead>` row calls `renderLabels()` to produce column headers, while the `<tbody>` rows use `renderEach()` with `renderFields()` to produce the data rows.

#### component and component()

From within a component template, the keyword `component` gives access to the component's state and helper methods. From outside the component (for example in a pagination bar placed elsewhere on the page) the function `component('Name')` returns a reference to the named component. Both access paths expose the same API, including `paginationControls()` and `filterControls()` described below.


### Slots

A slot is a named position in a component template that allows fields to be rendered in structurally distinct areas. The four well-known slot names are `label`, `content`, `media`, and `action`.

In practice, slots are rarely needed. Calling `renderFields()` without a slot argument renders all Configured Fields in order, which works for most tables, lists, and forms. Slots become useful when a component template has fixed structural positions, for example a card layout where the image, title, and action button must appear in specific areas regardless of field order.


### View Modes

A data-driven component operates in either read mode or edit mode. In read mode, fields use their render template. In edit mode, fields use their edit template. You set the mode via Display Mode in the component configuration dialog: `output` for read mode, `input` for edit mode.

You can override the mode for individual fields. In a field's detail settings, set `editable: false` to lock that field in read mode regardless of the component mode. This is useful for partially editable tables where some columns must remain read-only.


### Render Templates

A render template controls how a single field is displayed inside a data-driven component. You assign render templates to fields in the Configured Fields list, and Structr uses them to render field values at runtime.

Render templates are Widgets with the `isRenderTemplate` flag set. What makes them different from regular Widgets is that Structr instantiates them on demand: the first time a component is rendered, Structr creates the corresponding Shared Component from the render template's widget source. On subsequent renders, it reuses the existing Shared Component. You never need to insert render templates manually.

For this mechanism to work, the render template's widget source must set `data-structr-meta-name` to the desired Shared Component name. Without this attribute, Structr cannot find the already-instantiated component and creates a new instance on every render.

The template dropdown in a field's detail settings is filtered by the render template's `selectors` attribute, which for render templates contains the compatible data type (for example `date`, `string`, or `node`). The data type you set on the field determines which templates appear in the dropdown.

Render templates can define additional configuration parameters. You set these in the field's `config` object in JSON format. The parameters are surfaced in the field's detail settings so you can see which keys a given template expects.


### The 6-Column Grid

The widget system uses a consistent 6-column grid throughout. Layout containers, form components, and the Grid widget all share the same system so that column widths are predictable and composable across a page.

You set the component's overall width in the component configuration dialog using the visual column width selector. For individual fields, you set the column width in the field's detail settings. Fields default to 6 columns (full width). Setting a smaller number allows multiple fields to sit side by side. The grid manages line wrapping automatically.

Grid layout widgets use the same column system. A child component inside a Grid widget sets its `columns` attribute to a value from 1 to 6, and Structr renders the corresponding `col-span-N` CSS class on the component root.


### Pagination

`paginationControls()` generates HTML attributes that wire a button to the component's pagination state. The first argument specifies the pagination mode, the second the offset:

```html
<div class="flex flex-row gap-4">
    <button ${component('Table').paginationControls('offset', -1)}>Prev</button>
    <button ${component('Table').paginationControls('window', -2)}>${component('Table').page('window', -2)}</button>
    <button ${component('Table').paginationControls('window', -1)}>${component('Table').page('window', -1)}</button>
    <button ${component('Table').paginationControls('window',  0)}>${component('Table').page('window',  0)}</button>
    <button ${component('Table').paginationControls('window',  1)}>${component('Table').page('window',  1)}</button>
    <button ${component('Table').paginationControls('window',  2)}>${component('Table').page('window',  2)}</button>
    <button ${component('Table').paginationControls('offset',  1)}>Next</button>
</div>
```

The function also writes a `data-pagination-state` attribute on each button to indicate whether it is active, disabled, or invisible. Buttons outside the valid page range receive `visibility: hidden` so the pagination bar maintains a constant width. The page size is set in the component configuration dialog.


### Filtering

`filterControls()` wires a plain HTML input element to a component's filter state. When a user types in the input, the component performs a contains-search across all fields that have "Include in Filter" enabled and reloads with the filtered result. The filter term is stored as a URL parameter, so it survives page refresh.

```html
<input type="text" placeholder="Filter.." ${component('Table').filterControls()} />
```


### Component State

A data-driven component holds two kinds of state. Design-time configuration (DataSource binding, page size, reload behavior, display mode, field configuration) is stored in the ComponentConfiguration object and does not change during user interaction. Runtime state (selected record UUID, current page, sort key, filter values) is stored in URL parameters and changes as the user interacts with the page. Because runtime state lives in the URL, it survives browser navigation and page refresh.


### Reload Cascades

When multiple components on a page share channels, selecting a record must trigger reloads in the correct dependency order. A subscriber that reloads before its upstream channel has settled would render stale data.

Structr solves this by computing a dependency graph from the DataSource and channel relationships of all components on the page at render time. The computed reload order is written as an attribute on the active element (the element whose interaction triggered the state change). The Frontend-JS reads this attribute and resets the affected URL parameters in dependency order before triggering partial reloads. No manual event wiring is needed.
