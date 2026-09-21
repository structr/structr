
The Pages area is the visual editor for building your application's user interface. It combines a page tree, property panels, and live preview in one workspace. Here you design layouts, configure data bindings, set up interactions, and preview the results.

![Pages Overview](/structr/docs/pages_page-created.png)

## The Workspace

The screen is divided into three parts: a left sidebar with the page tree, the site list and the translation tools, a main area for properties and preview, and a right sidebar with widgets, shared components, recycle bin, and preview panel. All sidebars are collapsible, so you can expand your workspace when you need more room.

## Left Sidebar

### The Page Tree

The Pages panel shows all your pages as expandable trees. Each page reveals its structure when expanded: HTML elements, templates, content nodes, and their nesting relationships.

#### Element Icons

Different element types have distinct icons:

- Window icon – Page elements
- Box icon – HTML elements
- Colored box (red, green, yellow) – Repeater elements
- Application icon – Template elements
- Document icon – Content elements (these cannot be expanded because they have no children)

#### Visibility Indicators

An icon on the right of each element indicates its visibility settings. An open lock means both visibility flags are set, so public and authenticated users can see the element. A key means exactly one of the two flags is set. A closed lock means neither flag is set, so only the owner and users with explicit permissions can see the element.

#### Interaction

Click an element to select it and view its properties in the main area. Right-click (or hover and click the menu icon) to open the context menu. Drag elements to reorder them or move them between parents.

### Sites

The Sites panel lists the sites of your application. A site binds a set of pages to a hostname and port, so that the same Structr instance can serve several applications. Click "Create Site" to add one; the context menu of a site offers the usual property and security dialogs.

### Translations

The Translations panel lets you manage translations for the current page. Select a page, enter a language code, and click refresh to see all `localize()` calls used in that page. You can create, edit, and delete translations directly here.

## Right Sidebar

### Widgets

The Widgets panel contains reusable page fragments. You can drag a widget onto your page tree to insert it. If the widget has configuration options, a dialog appears where you can enter values before insertion.

#### Suggested Widgets

Widgets can also appear as suggestions in the context menu when their selector matches the element you've right-clicked. Suggestions appear in three places: directly in the context menu for inserting a widget as a child, in the "Wrap Element In" submenu for widgets that can take the selected element as their content, and in the "Replace Element With" submenu for widgets that fit into the parent of the selected element. This speeds up common patterns and keeps the page structure valid.

#### Importing and Creating Widgets

All widgets are stored in your database. On a new application the panel is empty and offers to import the default widget set from structr.com with a single click. Click the plus button to create a new widget. Right-click a widget to edit or delete it, or to open its advanced properties.

### Shared Components

Shared components work differently from widgets. When you insert a widget, Structr copies its content into your page. When you insert a shared component, Structr creates a reference to the original. If you edit the shared component, every page that uses it updates automatically.

You can create a shared component by dragging an element from the page tree into the Shared Components panel. Headers, footers, and navigation menus are ideal candidates – anything that should look and behave the same across multiple pages.

### Recycle Bin

When you delete an element from a page, it goes to the recycle bin rather than being permanently removed. You can drag elements back into the page tree to restore them. This safety net is especially valuable when restructuring complex pages.

Note that pages themselves are not soft-deleted. When you delete a page, only its child elements go to the recycle bin.

### Preview

The Preview panel shows your page as users will see it. You can keep the preview visible while working with other tabs in the main area, watching your changes take effect in real time.

## Editing Elements

When you select an element in the page tree, the main area shows its properties organized in tabs. The available tabs depend on the element type.

### General Tab

This contains basic settings: name, CSS classes, HTML ID, and inline styles. For repeaters, the Function Query and Data Key fields are located here. Show and Hide Conditions control whether the element appears in the output.

When a template element has a ComponentConfiguration (because its Widget source contained a `config` attribute), the General tab shows the component configuration dialog. This dialog lets you set up the DataSource, configure which fields are displayed and how they are rendered, control pagination and filtering, and set the component's role in channel communication. For a detailed description of the dialog and all its settings, see the [Widgets & Components](/structr/docs/ontology/Building%20Applications/Widgets%20&%20Components) chapter.

### HTML Tab

This is available for HTML elements. Here you can manage HTML attributes – both global attributes and tag-specific ones. Click "Show all attributes" to reveal event handlers like `onclick`. You can add custom attributes with the plus button.

### Editor Tab

This is available for templates and content elements. It provides a Monaco-based code editor with syntax highlighting and autocompletion. The Content-Type selector at the bottom offers text/plain, text/html, text/xml, text/css, text/javascript, text/markdown, text/textile, text/mediawiki, text/tracwiki, text/confluence and text/asciidoc. The markup formats (Markdown, Textile, MediaWiki, TracWiki, Confluence and AsciiDoc) are converted to HTML when the page is rendered; the other types are output as they are.

### Repeater Tab

Here you can configure data-driven rendering. Select a source (Flow, Cypher Query, or Function Query), define the data key, and the element renders once for each object in the result.

### Events Tab

Here you can set up Event Action Mappings – what happens when users interact with the element. Select a DOM event, choose an action, configure parameters, and define follow-up behaviors.

### Process Tab

This tab connects an element to a BPMN process. The Component Binding section attaches the component configuration of a process-bound component to a user task of a process; the task's process then determines the subject type the component works on. The Visibility section holds process visibility rules: the element is shown only while one of the configured process states is active for the current user. Several rules are OR-combined, and the show and hide conditions of the General tab still apply on top. See [BPMN Process Control](/structr/docs/ontology/Building%20Applications/BPMN%20Process%20Control) for the underlying concepts.

### Link Tab

This tab is available for elements that can point to a file or page: `a`, `link`, `script`, `img`, `video` and `object`. It shows the pages and files of your application; click one to link the element to it, and click the highlighted entry again to remove the link. If the element's link attribute (`href` for `link` elements, `src` otherwise) is still empty, Structr sets it to `${link.path}`, so the rendered path follows the linked target when it is renamed or moved.

### Security Tab

This shows access control settings: owner, visibility flags, and individual permissions.

### Advanced Tab

This provides a raw view of all attributes in an editable table. It is useful for properties that are not exposed in other tabs.

### Preview Tab

This shows the rendered page. Hover over elements to highlight them in both the preview and the tree. Click to select for editing.

### Active Elements Tab

This gives you an overview of key components: templates, repeaters, and elements with event action mappings. Click any item to jump to its location in the tree.

### URL Routing Tab

This is available for pages. Here you can configure additional URL paths with typed parameters. See the Navigation & Routing chapter for details.

## The Context Menu

Right-click any element to open the context menu. What you see depends on the element type.

### Insert Options

These let you add new elements as children or siblings. Suggested Widgets appear when widgets match the current element's selector. Suggested Elements offer common children for the current tag (for example, `<tr>` for tables, `<li>` for lists).

### Edit Options

- Edit: Opens an inline editor for the text content of an empty `div` element
- Clone – Copies the element and inserts it after the original
- Clone Page: Copies a whole page including all its elements
- Wrap Element In – Wraps the element with a new parent
- Replace Element With – Swaps the element while keeping its children
- Convert to Shared Component – Moves the element to shared components

### Select/Deselect

This marks elements for move or clone operations. After selecting, you can right-click elsewhere and choose "Clone selected element here" or "Move selected element here". If the selected element is a shared component, the entry "Link shared component here" inserts a reference to it instead of a copy.

### Expand / Collapse

For elements with children, this submenu offers Expand subtree, Expand subtree recursively and Collapse subtree.

### Remove Node

This sends the element to the recycle bin. It is available for elements that have a parent.

### Delete

Pages, sites and elements without a parent show a "Delete <Type>" entry instead, for example "Delete Page". This removes the node and its children permanently, without a detour through the recycle bin.

## Creating Pages

The Create Page button in the secondary menu offers two options:

### Create

Opens a dialog with the built-in tiles Empty Page, Simple Page and Custom Page, followed by the page templates of your application. Page templates are widgets with the "Is Page Template" flag enabled. The default widget set contributes the Default Page, a layout with header, sidebar and menu. If no page template exists yet, the dialog offers to import the default widget set.

### Import

Lets you create pages from HTML source code or by fetching from an external URL. This is how you bring existing designs into Structr and make them dynamic.

## Related Topics

- Pages & Templates – Explains how to build page structures, work with templates, and create widgets and shared components
- Dynamic Content – Covers data binding, template expressions, and repeaters
- Event Action Mapping – Details how to handle user interactions
- Navigation & Routing – Describes URL configuration and the `current` keyword
- Security – Explains visibility flags and access control
