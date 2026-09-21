# Creating Widgets

This chapter covers how to write your own Widgets: static building blocks, page templates, data-driven components and render templates. It builds on the [Widgets & Components](/structr/docs/ontology/Building%20Applications/Widgets%20&%20Components) chapter, which explains how Widgets and Shared Components are used, and on the template expressions described in the [Dynamic Content](/structr/docs/ontology/Building%20Applications/Dynamic%20Content) chapter.

The widget system consists of three subsystems that are separated from each other by design. The component type system controls where a Widget can be inserted and how Widgets nest. The replacement mechanism controls what happens when you replace an element with a Widget or wrap it in one. Data-driven components render their content from a data source and a ComponentConfiguration, using a small set of rendering functions. A good way to learn all three is to import the default widget set and read the source of its Widgets alongside this chapter.


## How a Widget Becomes Page Elements

A Widget is an object in the database with an HTML source code field. When you insert it, Structr parses the source and creates the corresponding elements in the page tree. In the simplest case the source is plain HTML, and the resulting elements are independent copies with no connection to the Widget. Inserting the same Widget twice creates two separate sets of elements.

The source can define Shared Components with `<structr:shared-template>` tags. Structr creates each Shared Component only once. When the Widget is inserted again, Structr finds the existing Shared Component by name and references it instead of creating a duplicate. This is the mechanism behind widget libraries, where every Widget produces the same Shared Components on every page.

The source can contain template variables in square brackets, such as `[variableName]`. Structr treats a square bracket expression as a variable only when a key with the same name exists in the Widget configuration. All other square bracket expressions are left unchanged. When a Widget has at least one recognized variable, the Insert Widget dialog asks for the values before insertion.

The source can also contain deployment annotations, the HTML comments that the application export writes in front of elements to preserve Structr attributes. Set `processDeploymentInfo` to true in the Widget configuration to have them interpreted on insertion. This is useful when you build the Widget source from an exported page.


## The Widget Editor

The plus button in the Widgets flyout creates a new Widget and opens the editor. The editor has five tabs: Source, Configuration, Description, Options and Help.

### Source

The Source tab holds the HTML source of the Widget, including Structr expressions. The easiest way to obtain it is to build the elements in a page and export their markup with the `_edit=1` URL parameter, which renders the page with all expressions and Structr attributes intact instead of evaluating them:

1. Build the Widget in a page called `myWidgetPage`
2. Open `http://localhost:8082/myWidgetPage?_edit=1`
3. View and copy the source code of the page
4. Paste it into the Source tab

#### Setting Structr Attributes in the Source

Widget source is plain HTML, but Structr elements have attributes that do not exist in HTML, such as `componentType` or `contentType`. To set them during insertion, use HTML attributes with the prefix `data-structr-meta-`. Take the Structr attribute name in camelCase, convert it to kebab-case and add the prefix. Structr strips the prefix on import, converts the name back to camelCase and sets the attribute on the element.

| HTML attribute | Structr attribute |
| -------------- | ----------------- |
| `data-structr-meta-name` | `name` |
| `data-structr-meta-component-type` | `componentType` |
| `data-structr-meta-content-type` | `contentType` |
| `data-structr-meta-dimensions` | `dimensions` |
| `data-structr-meta-show-conditions` | `showConditions` |
| `data-structr-meta-hide-conditions` | `hideConditions` |
| `data-structr-meta-item-type` | `itemType` |
| `data-structr-meta-repeater-type` | `repeaterType` |
| `data-structr-meta-data-key` | `dataKey` |
| `data-structr-meta-function-query` | `functionQuery` |
| `data-structr-meta-triggered-actions` | The event action mapping of the element, as a JSON object |

The mechanism works for any Structr attribute, not only the ones listed here.

### Configuration

The Configuration tab defines the template variables of the Widget as a JSON object. Each key is a variable name that matches a square bracket expression in the source, and its value describes the form field that the Insert Widget dialog shows for it. The configuration must be valid JSON.

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

Two kinds of top-level keys are reserved. `processDeploymentInfo` is the boolean described above. Keys that begin with an underscore are meta entries that are not shown as form fields. The process module uses `_autoVisibilityMapping` in this way to bind a Widget to a process step on insertion.

Each variable supports these attributes:

| Attribute | Applies to | Description |
|-----------|------------|-------------|
| `title` | all | The title displayed in the dialog. If omitted, the variable name is used. |
| `placeholder` | input, textarea | The placeholder text displayed when the field is empty. If omitted, the title is used. |
| `default` | all | The default value. For input and textarea it is prefilled, for select it is preselected. |
| `position` | all | A number for sorting the fields. Variables without a position appear after those with a position, in natural key order. |
| `help` | all | Help text displayed when hovering over the information icon. |
| `comment` | page, boolean | Help text for the typed fields that share their rendering with the render template settings. |
| `type` | all | The form field type. See the table below. Defaults to `input`. |
| `options` | select | An array of strings or an object with value-label pairs. Arrays render as simple options. Objects use the key as value and the object value as displayed text. |
| `dynamicOptionsFunction` | select | A function body that populates the options. The function receives a `callback` parameter that must be called with the options. If present, `options` is ignored. |
| `rows` | textarea | The number of rows. Defaults to 5. |

The `type` attribute selects the form field:

| Type | Form field |
|------|------------|
| `input` | A text input. This is the default. |
| `textarea` | A multi-line text input. |
| `select` | A dropdown with the given options. |
| `boolean` | A checkbox. |
| `page` | A dropdown listing the pages of the application. |
| `schema-type` | A dropdown listing the custom types of the application. |
| `schema-property` | A dropdown listing the properties of the type selected in the `schema-type` field of the same dialog. |
| `schema-method` | A dropdown listing the methods of the type selected in the `schema-type` field of the same dialog. |
| `datasource` | The data source menu. Structr replaces the variable with the resolved data source name. See Authoring Data-Driven Components below. |
| `process` | A dropdown listing the BPMN processes of the application. Requires the process module. |
| `user-task` | A dropdown listing the user tasks of the process selected in the `process` field of the same dialog. Requires the process module. |

The types `schema-property`, `schema-method` and `user-task` read their options from a sibling field, so they only work together with a `schema-type` or `process` field in the same configuration. The Action Button of the default set combines `schema-type` and `schema-method` to let the user pick the method the button calls.

### Description

The Description tab holds text that is shown at the top of the Insert Widget dialog and on the tile in the Create Page dialog for page templates. It can contain HTML and typically explains what the Widget does and what the configuration fields mean.

### Options

The Options tab holds the settings that control where and how the Widget is offered:

| Setting | Description |
|---------|-------------|
| Selectors | CSS selectors that decide under which elements the Widget is suggested. `[type='container']` suggests the Widget inside any element whose component type is `container`. Several selectors are combined with commas. For render templates, the selectors hold the compatible data types instead. |
| Is Page Template | Shows the Widget in the Create Page dialog. |
| Is Render Template | Marks the Widget as a render template for fields of data-driven components. |
| Is Exclusive In Parent | Allows only one instance per parent element. Once an element created by the Widget exists under a parent, the Widget is no longer suggested there. |
| Short Description | A one-line description of the Widget. |
| Component Type | The component type that Structr assigns to the root element created by the Widget. |
| Dimensions | The dimensions value that Structr assigns to the root element created by the Widget. |

The flyout context menu entry "Advanced" opens the full properties dialog of the Widget, which additionally holds the paths of the thumbnail and the icon shown in the flyout and in the Create Page dialog.


## The Component Type System

The component type system controls where Widgets can be inserted and how they nest. It uses two attributes on the page elements. `componentType` gives an element a role in the nesting hierarchy, and `dimensions` describes the shape of data the element is designed to display.

### Component Types and Selectors

The values of `componentType` are defined freely by the author of a widget set. Structr does not impose a fixed set. The selectors of a Widget are standard CSS selectors that Structr matches against a synthetic element built from the prospective parent: its tag name, CSS classes, id and name, plus the component type of the parent as the attribute `type`. If the parent is a linked copy of a Shared Component, the component type of the Shared Component is used. `[type='container']` therefore suggests a Widget inside every element whose component type is `container`, and `table` suggests it inside every table element.

The default widget set uses these component types:

| componentType | Description |
| ------------- | ----------- |
| **canvas** | The main content area of a page. Accepts the top-level components. |
| **container** | A structural container for layout and grouping. |
| **form** | A container with submit semantics. Accepts inputs directly. |
| **content** | A leaf element that displays content. Cannot have children. |
| **input** | A leaf element for user input. Cannot have children. |
| **action** | A leaf element that triggers an action, such as a button. |
| **table**, **list**, **gallery** | Data-driven components that render a collection. Inserted into a canvas. |
| **table-cell** | The cells of a data-driven table. Accept input, action and text widgets, so a table can be edited in place. |

There are two ways to assign a component type to the elements a Widget creates. The Component Type and Dimensions settings in the Options tab are applied to the root element on insertion, or to the Shared Component behind it. Alternatively, `data-structr-meta-component-type` on a `<structr:shared-template>` or any other element in the source sets the type for that element. The second way is needed when a Widget defines several elements with different roles, for example a card whose content area is a `container` while the card itself is not.

### Dimensions

The `dimensions` attribute counts the axes along which the content of a component repeats:

| dimensions | Meaning | Content structure | Examples in the default set |
| ---------- | ------- | ----------------- | --------------------------- |
| **0** | A single object | Fixed layout with named areas, no repetition | Card, Edit Form |
| **1** | A list | A repeating sequence of identical areas | List, Gallery, Accordion |
| **2** | A table | Areas arranged in rows and columns | Table |

For data-driven components, the value also constrains the data source. A component with dimensions 0 expects a single object, dimensions 1 a collection, and dimensions 2 a collection with several properties per item. Structr checks this whenever the configuration of a component changes, see Data Source Compatibility below. Leaf elements and forms have no dimensions value, and a missing value means that the data source is not constrained.

### Exclusive Widgets

A Widget with "Is Exclusive In Parent" is suggested only while no element created by it exists under the prospective parent. Structr recognizes such elements by name, so the root element of an exclusive Widget must carry the Widget's name, either as the name of its Shared Component or via `data-structr-meta-name`. Use this for elements that make sense only once per container, such as a page heading or a pagination bar.


## Replacing and Wrapping

The context menu offers two operations that exchange an existing element for a Widget. "Replace Element With" removes the selected element and inserts the Widget in its place. "Wrap Element In" inserts the Widget in place of the selected element and moves the element into one of the Widget's content areas. For both, Structr suggests only Widgets whose selectors match the parent of the selected element, using the same matching as for insertion. For wrapping, the Widget must additionally contain an element whose component type matches the selectors of the Widget that created the selected element, so that the element has a valid place to go.

When an element is replaced, Structr transfers matching content areas and repeaters from the old element to the new Widget. Content areas are matched by `itemType`, repeaters by `repeaterType`. Both attributes are set freely by the widget author, matching is by string equality, and each value may occur only once in a Widget. The Card with heading of the default set marks its heading with `data-structr-meta-item-type="label"` and its body with `data-structr-meta-item-type="content"`, so replacing one card with another keeps heading and body in place.

`itemType` and `repeaterType` belong exclusively to the replacement mechanism. They are distinct from `componentType` and from the ComponentConfiguration described below. Data-driven components are not subject to content transfer. When one data-driven component is replaced with another, only the data source binding is carried over, since there is no manually placed content to preserve.


## Building a Widget Library

A widget library is a set of Widgets designed to work together through the component type system. Each Widget defines one or more Shared Components with `componentType`, `dimensions` and selectors that enforce the nesting rules. When a Widget from the library is inserted, Structr creates its Shared Components if they do not exist yet, or reuses them if they do. A library can contain static Widgets and data-driven components.

### Defining Widgets

Use `<structr:shared-template>` tags for the Shared Component definitions and `<structr:template>` tags to reference them. Container Widgets call `render(children)` to mark the content area where child elements go. Leaf Widgets omit the call. The following source defines four Shared Components and assembles them into a page:

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

The definitions create the Shared Components, the references assemble them: Main Page Template contains Main Content, which contains a Panel with a Paragraph. Inserting the Widget again reuses the existing Shared Components. With "Is Page Template" set, this Widget appears in the Create Page dialog, and every page created from it shares the same Main Page Template.

### Multi-Area Widgets

Some container Widgets provide more than one content area. The Card with heading of the default set renders the card frame in its Shared Component and places two elements inside it through the referencing `<structr:template>`: a heading with `data-structr-meta-item-type="label"` and a container with `data-structr-meta-item-type="content"` and `data-structr-meta-component-type="container"`. The `itemType` values name the areas for the replacement mechanism, and the component type of the content area decides which Widgets are suggested inside it. The Widget owns its internal structure and exposes the areas for the user to fill.

### Organizing the Library

Use the `treePath` attribute to group Widgets into categories in the flyout and in the Suggested Widgets submenu. The path is slash-separated, must begin with a slash, and may contain spaces. Widgets without a path appear under "Uncategorized". The default set uses these categories:

    /Layout
    /Components
    /Component Parts
    /Form Elements
    /Text
    /Actions
    /Render Templates
    /Page Templates

Render templates and page templates get categories of their own because they are not inserted directly.

### Using the Library

Once the Widgets carry component types, dimensions and selectors, the library integrates into the page building workflow. A page created from the page template starts with a canvas that accepts containers and components. Right-clicking the canvas suggests exactly those, right-clicking inside a container suggests content, inputs and actions. The component type system guides the user through a valid page structure without knowledge of the rules.


## Authoring Data-Driven Components

A Template element becomes a data-driven component when a ComponentConfiguration is attached to it. This happens when the Widget source contains a `<structr:template>` with a `config` attribute. The attribute holds the initial values of the configuration as a JSON-like object. The Table of the default set uses:

```html
<structr:template src="Table" config="{ dataSource: '[dataSource]', reload: 'partial' }"></structr:template>
```

The keys correspond to the properties of the ComponentConfiguration: `dataSource`, `displayMode` (`output` or `input`), `role` (`none`, `controller` or `subscriber`), `selectionChannel`, `reload` (`partial`, `page` or `others`), `labels`, `pageSize`, `columns` and `transform`. The Edit Form presets `displayMode: 'input', role: 'subscriber', labels: true` so that the user only has to pick the channel.

The `[dataSource]` variable is filled from the Insert Widget dialog. The Widget declares it in its configuration with the type `datasource`:

    {
        "dataSource": {
            "title": "Data Source",
            "type": "datasource"
        }
    }

The dialog then shows the data source menu. When the user picks an existing type or channel, the variable is replaced with the data source name, for example `node:Project` or `channel:current`. When the user creates a new type, Structr creates the schema type and its properties, fills the field set of the component with the new attributes and, if requested, generates example data. When the user creates a script-based or query-based data source, Structr creates the empty node and binds the component to it by id.

### Keywords in Component Templates

Inside the Shared Component or template of a data-driven component, these keywords are available:

`component` refers to the closest data-driven component above the current element and exposes its state and helper methods: `pagination()`, `page()`, `pageCount`, `filterControls()` and `isEditable`, which is true in input mode and serves as a show condition for a save button. From outside the component, `getComponent('Name')` returns the component with the given name.

`dataSource` refers to the data source of the component. `dataSource.selectedValue` is the record that a subscriber received via its channel and is only available when the data source is a channel. The Edit Form uses it as the id expression of its update action and in `empty(dataSource.selectedValue)` to hide the save button while nothing is selected. `dataSource.currentValue` is the record of the current iteration inside `renderEach()`. The Delete Button uses `dataSource.currentValue.id` in a table row. `dataSource.dataType` returns the name of the type the data source delivers.

`adapter` refers to the data adapter of the component. `adapter.dataType` returns the type name and is used by the default forms in their button labels.

### Rendering Functions

Three functions render the configured fields. Each takes a tag specification as its first argument, a small CSS selector consisting of a tag name with optional classes and an id, such as `th.sw-table-header`. The rendered wrapper receives the `col-span-N` class for the width of the field, or `col-span-6` when no width is set, so the six-column grid works inside every component.

`renderEach(tags)` iterates over the paginated, filtered and sorted collection of the data source and renders the configured fields for each record. The argument names two tags separated by a space, the first for each record and the second for each field. The Table uses `renderEach('tr td.sw-table-cell')`, the List uses `renderEach('li.sw-list-item')`, the Accordion uses `renderEach('details summary')`. Each record wrapper receives the attributes that make the record selectable when the component is a controller.

`renderFields(tag [, slot])` renders the configured fields for a single record, applying each field's render template in output mode or its edit template in input mode. The Edit Form calls `renderFields('div')` inside a `grid grid-cols-6` container so that the field widths take effect.

`renderLabels(tag [, slot])` renders the labels of the configured fields without values. The Table calls `renderLabels('th.sw-table-header')` in its header row. The rendered labels carry the attributes that make a column sortable. Clicking a label sorts by the field's sort key and toggles the direction, and the active label receives the CSS class `ascending` or `descending`.

### Slots

A slot is a named position in a component template. A render template can assign its fields to slots, and `renderFields('div', 'media')` then renders only the fields of the `media` slot. `renderLabels()` accepts the slot argument in the same way. Slots are rarely needed. Without a slot argument all configured fields are rendered in order, which works for tables, lists and forms. Slots become useful when a template has fixed structural positions, for example a card where image, title and action must appear in specific places regardless of field order.

### Pagination and Filtering

The component method `pagination()` returns the HTML attributes that wire a button to the pagination state. Its first argument names the kind of button: `prev`, `next`, `first` and `last` lead to the neighbouring pages and to the first and last page, `window` takes an offset relative to the current page as second argument and produces the numbered buttons, and `ellipsis` takes `low` or `high` and marks the placeholder buttons that appear when the first or last page is far away. `page()` takes the same arguments and returns the page number the button leads to. `pageCount` returns the total number of pages. The pagination bar of the Table reads:

```html
<div class="flex flex-row p-4 gap-4 justify-between">
    <button ${component.pagination('prev')}>Prev</button>
    <div class="flex gap-4">
        <button ${component.pagination('first')}>1</button>
        <button ${component.pagination('ellipsis', 'low')}>&hellip;</button>
        <button ${component.pagination('window', -2)}>${component.page('window', -2)}</button>
        <button ${component.pagination('window', -1)}>${component.page('window', -1)}</button>
        <button ${component.pagination('window',  0)}>${component.page('window',  0)}</button>
        <button ${component.pagination('window',  1)}>${component.page('window',  1)}</button>
        <button ${component.pagination('window',  2)}>${component.page('window',  2)}</button>
        <button ${component.pagination('ellipsis', 'high')}>&hellip;</button>
        <button ${component.pagination('last')}>${component.pageCount}</button>
    </div>
    <button ${component.pagination('next')}>Next</button>
</div>
```

Buttons that lead outside the valid range receive the `hidden` attribute, buttons without a valid target are `disabled`, and the button for the current page carries `data-current-page`. The same markup is shipped as the standalone "Pagination" Widget in the Component Parts category, which can be inserted into any container inside a component. The number of numbered buttons is controlled by the `paginationWindowSize` property of the ComponentConfiguration, default 5, which has no field in the dialog and is set in the Advanced properties.

`filterControls()` returns the attributes that wire a text input to the filter state of the component:

```html
<input type="text" placeholder="Filter.." ${component.filterControls()} />
```

Typing performs a contains-search across all fields with "Include in filter" enabled, resets the pagination to the first page and stores the term in the URL. Escape clears the filter.

### Component State and Reload Cascades

A data-driven component holds two kinds of state. Design-time configuration, that is data source, page size, reload behaviour, display mode and field configuration, lives in the ComponentConfiguration and does not change during user interaction. Runtime state, that is the selected record, current page, sort key and filter text, lives in URL parameters and changes as the user interacts. Because runtime state is in the URL, it survives navigation and refresh.

When several components on a page share channels, a selection must trigger reloads in dependency order, otherwise a subscriber could render before its upstream channel has settled. Structr computes a dependency graph from the data source and channel relationships of all components on the page at render time and writes the reload order as an attribute on the active element. The frontend script reads it, resets the affected URL parameters in order and triggers the partial reloads. The `data-channel` attribute on a component's root marks it as a listener for the named channels, and a component with reload behaviour `others` omits its own name so that it is not reloaded by its own actions.

### Data Source Compatibility

When the data source, the transform or the expected type of a component changes, Structr checks the combination against the dimensions of the component and rejects it with an error if it does not fit. A transform can only be applied to a data source that delivers a single object, because a property cannot be navigated on a collection. A collection cannot be bound to a component with dimensions 0. When the transform points to a collection property, the result counts as a collection. Components without a dimensions value accept any data source. The check runs on every modification of the ComponentConfiguration, including deployment import, so a widget set with wrong dimensions fails early.

### Data Source Names

The `dataSource` property holds a name that Structr resolves at render time. `node:<Type>` delivers all instances of a type, `node:<uuid>` a user-defined script-based or query-based data source by id, `channel:<name>` the record selected on a channel, `parent` the record that a surrounding repeater or component provides under the data key `parent`, and `root-folders` the root folders of the file system. The data source menu in the dialogs produces exactly these names.


## Authoring Render Templates

A render template is a Widget with "Is Render Template" set. Structr instantiates it on demand: the first time a component needs the template, Structr expands the Widget source into a Shared Component in the hidden document, and reuses that Shared Component afterwards. Users never insert render templates.

Structr finds the instantiated Shared Component by the name of the render template, so the root element of the source must carry `data-structr-meta-name` with the Widget's name. Without it, Structr creates a new instance on every render.

The selectors of a render template hold the data types it can display, for example `date`, `string`, `boolean`, `node`, `enum` or `custom`. The template dropdown of a field offers only templates whose selectors contain the field's data type.

Inside the template, `value` holds the field value and `field` gives access to the field metadata: `field.label`, `field.propertyName`, `field.required`, `field.options` for enum and relationship fields, `field.multiple`, `field.rows` and `field.config` for the parameters described next. The textfield template of the default set renders:

```html
<input class="sw-input" name="${field.propertyName}" placeholder="${field.label}"
    required="${is(field.required, 'required')}" type="text" value="${value}"
    data-structr-meta-name="textfield" />
```

A render template can declare parameters in its Configuration tab. The format differs from the template variables of other Widgets: each parameter has a `label`, an optional `type` (`page` or `boolean`, otherwise a text input), optional `options`, an optional `comment` and an optional `defaultValue`. The parameters appear in the Render Template Settings of every field that uses the template, their values are stored per field and read via `field.config`. The formatted-date template declares:

    {
        "dateFormat": {
            "label": "Date Format (ISO 8601)",
            "defaultValue": "dd.MM.yyyy"
        }
    }

and renders `${dateFormat(value, field.config.dateFormat)}`. The related-link template declares a `detailPage` parameter of type `page` and a `labelProperty` parameter and renders a link to the detail page of the related record with the chosen property as link text.


## Process-Bound Widgets

With the process module installed, a Widget can bind the component it creates to a user task of a BPMN process. The configuration uses the `process` and `user-task` field types so that the user picks the task on insertion, and a `_autoVisibilityMapping` meta entry whose values reference those fields in square brackets, for example `"boundProcess": "[process]"`. On insertion, Structr resolves the references and creates a visibility mapping that shows the component only while the task is active. The binding mode of the ComponentConfiguration is then `processBound`, the data source is fixed to the `current` channel, and the subject type of the process appears as the read-only Expected Type. The Process Subject Form of the default set is an example. Processes and their pages are covered in the [BPMN Process Control](/structr/docs/ontology/Building%20Applications/BPMN%20Process%20Control) chapter.
