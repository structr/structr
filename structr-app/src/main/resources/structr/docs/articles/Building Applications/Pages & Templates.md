After defining a first version of the data model, the next step is usually to build a user interface. This can be done in the Pages area.

## Working with Pages

<img src="Structr_6.jpg" class="small-image-50" />

A page in Structr consists of HTML elements, template blocks, content elements, or a combination of these. Pages are rendered on the server, so the browser receives fully rendered HTML rather than JavaScript that builds the page client-side.

### Why Server-Side Rendering
Modern web development often defaults to client-side frameworks where JavaScript builds the page in the browser. This approach has trade-offs: users wait for JavaScript to load and execute before seeing content, build pipelines add complexity, and search engines may not index pages correctly.

In Structr, the server renders complete HTML and sends it to the browser, ready to display. There is no build step, no hydration, no waiting for JavaScript to construct the page. When something looks wrong, you debug in one place rather than tracing through client-side state management and component lifecycles.

### From Design to Application
The Structr way of building applications is to start with an HTML structure or design template and make it dynamic by adding repeaters and data bindings. This approach lets you convert a page layout directly into a working application – the design stays intact while you add functionality. It works especially well with professionally designed web application templates from sources like ThemeForest.

### Modifying the Page Tree
Once you have created a page, you can modify it by adding and arranging elements in the page tree. Add elements by right-clicking and selecting from the context menu, or by dragging widgets from the Widgets flyout into the page.

### Element Types
HTML elements provide the familiar tag-based structure - `<div>`, `<section>`, `<article>`, and other standard tags. Template elements contain larger blocks of markup and can include logic that pre-processes data for use further down the page. Content elements insert text or dynamic values wherever text appears: in headings, labels, table cells, or paragraphs. Widgets are pre-built page fragments that you can drag into your page to add common functionality. Shared components are reusable elements that you define once and reference across multiple pages. Changes to a shared component are reflected everywhere it is used.

### Static Resources
Static resources like CSS files, JavaScript files, and images are stored in the Structr file system and can be included in your pages by referencing their path. For details on how to work with files, including dynamic file content with template expressions, see the Files chapter.

### Dynamic Content
Pages can produce static output or dynamic content that changes based on data, user permissions, or request parameters. Template expressions let you insert dynamic values in content elements, HTML attributes, or template markup.

### Repeaters
To display collections of database objects - such as a list of users or a product catalog - configure an element as a repeater. The repeater retrieves a collection of objects and renders the element once for each result. For example, a `<tr>` element configured as a repeater produces one table row for each object in the collection. You can call methods on your types to retrieve the data, or call flows if you use Flows.

### Partial Reload
For updates without full page reloads, you can configure individual elements to refresh independently - after a delay, when they become visible, or at regular intervals. Event action mappings can also trigger partial reloads in response to user interactions, updating specific parts of the page while keeping the rest intact.

### Controlling Visibility
Show and hide conditions determine whether a part of the page appears in the output, based on runtime data or user state. Visibility flags and permissions offer another layer of control - you can make entire branches of the page tree visible only to specific users or groups, for example an admin menu that only administrators can see.

### Preview and Testing
The preview tab shows how your page is rendered. You can assign a preview detail object and request parameters in the page settings to test how your page behaves with different data. The preview also allows you to edit content directly - clicking on text in the preview selects the corresponding content element, where you can modify it in place.




## Creating a Page
When you click the green "Create Page" button in the upper left corner of the Pages section, you can choose whether to create a page from a template or import one from a URL.

### Create Page Dialog

![Create Page Dialog](pages_create-page.png)

#### Templates
When you select "Create Page", you will see a list of templates that are used to create the structure of the new page. Templates are based on the Tailwind CSS framework and range from simple layouts like the Empty Page to more complex structures with sidebars and navigation menus, as well as specialized templates like the Sign-In Page.

When you create a page from a template, you import a pre-built page structure. This can include content, repeaters, permissions, and also shared components for reuse across your site. The Simple Page option, on the other hand, creates a minimal page with only the standard HTML elements `<html>`, `<head>`, and `<body>`.

#### Page Templates Are Widgets
Page templates are widgets with the `isPageTemplate` flag enabled. Structr looks at the widget server and your local widget collection and displays local and remote page templates together in the "Create Page" dialog.

### Import Page Dialog
The Import Page dialog lets you create pages from HTML source code or by importing from external URLs.

![Import Page Dialog](pages_import-page.png)

#### Create Page From Source Code
Paste your HTML code into the textarea. You can then configure the import options below before creating the page.

#### Fetch Page From URL
You can also import a page from an external URL using the text input below the textarea. This imports the page including all static resources like CSS, JavaScript, and images.

#### Configuration Options
Below the import options, you configure the name and visibility flags of the new page. You can also mark imported files to be included when exporting your application and enable parsing of deployment annotations in the imported HTML.

#### Deployment Annotations
Deployment annotations are special markers that Structr inserts when exporting HTML. They preserve Structr-specific attributes such as content types for content elements and visibility settings for individual HTML elements.







## The Page Element

![Page Elements](pages_page-expanded.png)

The Page element sits at the top of a page's element tree and represents the page itself. Below the Page element, there is either a single Template element (the Main Page Template) or an `<html>` element containing `<head>` and `<body>` elements. Templates can also be used to create non-HTML pages: by setting the content type to `application/json`, `text/xml`, or `text/plain`, you can make the page return any content you want.

### Appearance
Page elements appear as an expandable tree item with a little window icon, the page name and optional position attribute on the left, and a lock icon on the right. Click the lock icon to open the Access Control dialog. The icon's appearance indicates the visibility settings: no icon means both visibility flags are enabled, while a lock icon with a key means only one flag is enabled.

### Interaction
When you hover over the Page element with your mouse, two additional icons appear: one opens the context menu (described below) and one opens the live page in a new tab. Note that you can also open the context menu by right-clicking the page element. Left-clicking the Page element opens the detail settings in the main area of the screen in the center.

### Access Control Dialog
Clicking the lock icon on the page element opens the access control dialog for that page. {{"Access Control Dialog",+1,shortDescription,children}}

### Permissions Influence Rendering
Visibility flags and permissions don't just control database access, they also determine what renders in the page output. You can make entire branches of the HTML tree visible only to specific user groups or administrators, allowing you to create permission-based page structures. For example, an admin navigation menu can be visible only to users with administrative permissions.

For conditional rendering based on runtime conditions, see the Show and Hide Conditions section in the Dynamic Content chapter.

### The General Tab
The General tab of a page contains important settings that affect how the page is rendered for users and displayed in the preview.

![General Settings](pages_page-expanded.png)

#### Name
The page name identifies the page in the page tree and determines its URL. A page named "about" is accessible at `/about`.

#### Content Type
The content type can be used to control the page's output format. The default is `text/html`, but you can use `application/json` for JSON responses, `text/xml` for XML, or any other content type including binary. The content type is sent along with the response in the `ContentType` HTTP header, so it can also include the charset.

#### Category
The category field can be used to organize your pages into groups: assign a category to the page, and you can then use the category filter to show only pages from that category.

#### Show on Error Codes
You can configure this page to be displayed when specific HTTP errors occur. Enter a comma-separated list of status codes, for example, 404 when content isn't found or users lack permission, 401 when authorization is required, or 403 when access is forbidden.

#### Position
When users access the root URL of your application, Structr uses the position attribute to determine which page is displayed. Among all visible pages, the one with the lowest position value is shown. See the Navigation & Routing chapter for a detailed explanation of page ordering and selection.

#### Custom Path
You can assign an alternative URL to the page using this field. Note that URL routing has replaced this setting and provides more flexibility, including support for type-safe path-based arguments that are directly mapped to keywords you can use in your page.

#### Caching disabled
Enable this when your page contains dynamic data that changes frequently or personalized content. Structr sends cache control headers that prevent browsers and proxies from caching the page output. Pages for authenticated users are never cached, so this flag only affects public users.

#### Use binary encoding for output
Enable this if your page generates binary data to make Structr use the correct character encoding automatically.

#### Autorefresh
Enable this to automatically reload the page preview in the Structr Admin UI whenever you make changes.

#### Preview Detail Object
The preview detail object allows you to assign a fixed object that Structr uses as the detail object when rendering the preview, making it available under the `current` keyword.

#### Preview Request Parameters
The preview request parameters field allows you to provide fixed parameters that Structr includes when rendering the preview.

### The Advanced Tab
The Advanced tab provides a raw view of the current object, showing all its attributes grouped by category, in an editable table for quick access. This tab includes the base attributes like `id`, `type`, `createdBy`, `createdDate`, `lastModifiedDate`, and `hidden` that are not available elsewhere.

![Advanced Settings](pages_page-details-advanced.png)

#### Hidden Flag
The `hidden` flag prevents rendering of the element and all its children. When you enable this flag, Structr excludes the element from the page output entirely, making it useful for temporarily disabling parts of your page structure without deleting them.

### The Preview Tab
The Preview tab displays how your page appears to visitors, while also allowing you to edit text content directly. Hovering over elements highlights them in both the preview and the page tree. You can click highlighted elements to edit them inline or select them in the tree for detailed editing. This inline editing capability is especially valuable for repeater-generated lists or tables, where you can access and modify the underlying template expressions directly in context.

#### Preview Settings
You can configure the preview in the page's General tab settings. Assign a specific object to make it available under the current keyword for testing, or provide fixed request parameters to test your page with specific data. These settings help you preview how your page renders with different objects and parameters.

### The Security Tab
The Security tab contains the Access Control settings for the current page, with owner, visibility flags and individual user / group access rights, just as the Access Control dialog.

### The Active Elements Tab
The Active Elements tab provides a structural overview of the page. Key page components are highlighted, such as templates, repeaters and elements with event action mappings. Clicking a component jumps directly to its location in the page tree.

![Active Elements](pages_page-details-active-elements.png)

### The URL Routing Tab
The URL Routing tab allows you to configure additional URL paths under which the page is made available. You can define typed parameters in the path that Structr automatically validates and makes available in the page under the corresponding key.

#### How it works
You start by writing a path expression with placeholders (e.g., `/project/{lang}/{name}`). For each placeholder, the dialog displays a type selection field, and the variable is made available in the page under its respective name when present in the path.

The arguments are optional, meaning empty path segments (e.g., `/projects//my-example-page`) can be passed, in which case the variable is not set (null value).



## The HTML Element
HTML elements form the structured content of a page. An element always has a tag and can include both global attributes like id, class, and style, additional tag-specific attributes defined by the HTML specification, and custom data attributes. HTML elements can be inserted anywhere in the page tree, as Structr does not strictly enforce valid HTML.

HTML elements automatically render their tag, all attributes with non-null values, and their children. An empty string causes the attribute to be output as a boolean attribute without a value (e.g., `<option selected>`).

### Appearance
HTML elements appear as expandable tree items with a box icon, showing their tag name and CSS classes. You can rename HTML elements to better communicate their purpose - when renamed, the custom name is displayed in the tree instead of the tag. Elements configured as repeaters display a colored box icon with red, green, and yellow instead of the standard box. The lock icon on the right indicates visibility settings: no icon means both visibility flags are enabled, a lock icon with a key means only one flag is enabled.

### Interaction
When you hover over an HTML element with your mouse, the context menu icon appears. You can also open the context menu by right-clicking the element. Left-clicking the HTML element selects it in the page tree and opens the detail settings in the main area of the screen in the center. Clicking the lock icon opens the Access Control dialog.

### The General Tab
The General tab of an HTML element contains important settings that affect how the element is rendered and displayed in the page tree.

![General Settings](pages_element-details_general.png)

#### Name
The name is used to identify the element in the page tree and can help communicate the element's purpose in your page structure.

#### CSS Class
You can specify one or more CSS classes (separated by spaces) that will be applied to the element when rendered. You can also create dynamic CSS classes by inserting template expressions - this is the primary use case for StructrScript expressions. For example: `button ${current.status}` to apply a class based on the current data object's status.

#### HTML ID
This sets the element's unique identifier in the DOM, which can be used for styling, scripting, or linking.

#### Style
Use this to apply inline styles to the element. Template expressions allow you to generate dynamic styles as well. For example: color: `${current.textColor}` to set a color based on the current data object.

#### Function Query
An auto-script field (surrounded with `${` and `}`) for defining repeater queries. This allows you to write a script expression that retrieves data to be iterated over by the repeater.

#### Data Key
Specifies the data key for the repeater. This defines the variable name under which each item from the Function Query result will be available during iteration. Note that data keys with the same names in nested repeaters overwrite each other.

#### Show Conditions
Defines when the element should be shown. The element is rendered only when this expression evaluates to true. Show conditions are evaluated at rendering time, before the page rendering engine starts rendering the element. For example: `me.isAdmin` to show the element only to admin users. This is an auto-script field.

#### Hide Conditions
Like Show Conditions, but defines when the element should be hidden. The element is not rendered when this expression evaluates to true. This is also an auto-script field evaluated at rendering time.

#### Load / Update Mode
Configuration for rendering behavior of the element. {{"Load / Update Mode",shortDescription,table}}

{{"Delay or Interval (ms)",h4,shortDescription}}

### The HTML Tab
The HTML tab enables management of HTML-specific attributes for an element. In addition to the global attributes (`class`, `id`, and `style`), the tab displays the type-specific attributes for each element. For example, `<option>` elements have the `selected` and `value` attributes.

There is a button that allows you to add custom attributes that will be included in the HTML output. We recommend prefixing custom attributes with `data-`, though this is not required. You can also use attributes required by JavaScript frameworks, such as `is`.

At the end of each row is a small cross icon that allows you to remove the attribute's value (i.e., set it to null).

#### Show All
The "Show all attributes" button reveals the complete list of HTML attributes, including event handlers like `onclick`, `ondrag`, or `onmouseover`. By default, only attributes with values are displayed. Attributes containing an empty string display a special warning icon because the distinction between null and empty string is important, but not immediately visible.

#### REST API Representation
If you retrieve HTML elements via REST, you will see that HTML attributes are prefixed with `_html_` to uniquely identify them. This reflects how Structr handles these attributes internally - for example, to distinguish between `_html_id` (the HTML id attribute) and `id` (the element's internal UUID). While the user interface hides this implementation detail, it remains visible in the REST API.

### The Advanced Tab
Like the Advanced tab for Page elements, this tab provides a raw view of the current HTML element, showing all its attributes grouped by category in an editable table for quick access.

### The Preview Tab
Like the Preview tab for Page elements, this tab displays the same rendered output for all elements within a page, as the preview always renders from the root of the page hierarchy. This means whether you are viewing the Page element itself or any child element, you will see the complete page output here.

### The Repeater Tab
The Repeater tab allows you to configure an element to render dynamically based on a data source, repeating its output for each object in a collection.

![Repeater Settings](pages_element-details_repeater.png)

#### Result Collection
At the top, you select the repeater source: Flow, Cypher Query, or Function Query (a scripting expression).

#### Repeater Keyword
The repeater keyword or data key field defines the variable name for accessing each object in the result.

#### How it works
The repeater and its children are rendered once for each object returned by the source. The data key is available throughout the rendering and can be referenced in content nodes, templates, and attributes.

#### Example
For example, a repeater with the Function Query `find('Project')` and data key `project` would render once for each Project object returned by the query. Within the repeater's children, you could use `${project.name}` to display each project's name.

### The Events Tab
The Events tab allows you to configure Event Action Mappings for individual elements.

![Event Action Mappings](pages_element-details_events.png)

#### How it works
You start by selecting the DOM event that the Event Action Mapping should respond to in the Event field. After selecting an event, the Action field appears where you select the action to perform.

Actions include creating objects, modifying objects, login, logout, and more. Once you have selected an action, additional input fields appear progressively, allowing you to configure the mapping step-by-step.

#### Parameter Mapping
Below the configuration fields, there is a Parameter Mapping section where you can add individual parameters. When the action configuration includes a type, the parameters can be automatically populated based on the attributes of that type using the second button next to the Parameter Mapping heading.

#### Confirmation Dialog
This section determines whether the action requires confirmation. When Dialog Type is set to Confirm Dialog, a `window.confirm` dialog is displayed before the Event Action is executed.

#### Notifications
This section allows you to display notifications based on whether the action was executed successfully or not. The following options are available: System Alert, Inline Text Message, Custom Elements, and the option to send a custom JavaScript event.

#### Follow-up Actions
Additionally, you can configure follow-up actions to be performed after the main Event Action. For example, you can reload the entire page or individual elements. You can navigate to a new page based on the action's result. You can also trigger a custom JavaScript event here. You can access variables returned from the action in the follow-up configuration.

#### Further Information
For detailed instructions about how to configure the individual settings of Event Action Mappings, see the [Event Action Mapping](/structr/docs/ontology/Building%20Applications/Event%20Action%20Mapping) chapter below.

### The Security Tab
The Security tab contains the Access Control settings for the current element, with owner, visibility flags and individual user / group access rights.

### The Active Elements Tab
The Active Elements tab displays the same structural overview as its counterpart on page elements, but scoped to the current element and its descendants.





## Templates and Content Elements
Template and content elements contain text or markup that is output directly into the page, instead of building structure from nested HTML elements. They have a content type setting that controls how the text is processed before rendering - Markdown, AsciiDoc, and several other markup dialects are automatically converted to HTML, while plaintext, XML, JSON, and other formats are output as-is.

Content elements are the simpler variant: they output their text and cannot have children. Template elements can have children, but this is where they differ fundamentally from HTML elements.

Note that when using a template element as the root of a page, it must include the `DOCTYPE` declaration that an HTML element would output automatically.

### Composable Page Structures
Unlike HTML elements, templates do not render their children automatically. If you don't explicitly call `render(children)`, the children exist in the page tree but produce no output. This is intentional as it gives you full control over placement rather than forcing a fixed parent-child rendering order.

The result is a composable system. A template can define a layout with multiple insertion points - a sidebar, a navigation area, a main content section - and then render specific children into each slot. Using the `render()` function, you control exactly where each child appears in the output. This lets you build complex page structures from reusable, composable building blocks.

### Including External Content
You can also use `include()` or `includeChild()` in a template to pull content from other parts of the page tree or from objects in the database.

### Appearance
Template elements appear as expandable tree items with an application icon, showing their name or `#template` when unnamed. Content elements are not expandable because they cannot have children - they display a document icon and show the first few words of their content, or `#content` when empty. Elements configured as repeaters display a yellow icon. Rename template elements to better communicate their purpose.

### Interaction
The lock icon on the right indicates visibility settings: no icon means both visibility flags are enabled, a lock icon with a key means only one flag is enabled. When you hover over a template or content element, the context menu icon appears. You can also open the context menu by right-clicking the element. Left-clicking selects it in the page tree and opens the detail settings in the main area. Clicking the lock icon opens the Access Control dialog.

### The General Tab
The General tab of template and content elements contains the name field and the following four configuration options, which work the same as on HTML elements:

#### Function Query
An auto-script field for defining repeater queries. This allows you to write a script expression that retrieves data to be iterated over by the repeater.

#### Data Key
Specifies the data key for the repeater. This defines the variable name under which each item from the Function Query result will be available during iteration. Note that data keys with the same names in nested repeaters overwrite each other.

#### Show Conditions
Defines when the element should be shown. The element is rendered only when this expression evaluates to true. Show conditions are evaluated at rendering time, before the page rendering engine starts rendering the element. For example: `me.isAdmin` to show the element only to admin users. This is an auto-script field.

#### Hide Conditions
Like Show Conditions, but defines when the element should be hidden. The element is not rendered when this expression evaluates to true. This is also an auto-script field evaluated at rendering time.


### The Advanced Tab
Like the Advanced tab for HTML elements, this tab provides a raw view of the current template element, showing all its attributes grouped by category in an editable table for quick access.

### The Preview Tab
Like the Preview tab for Page elements, this tab displays the same rendered output for all elements within a page, as the preview always renders from the root of the page hierarchy. This means whether you are viewing the Page element itself or any child element, you will see the complete page output here.

### The Editor Tab
The Editor tab is where you edit the actual content of template and content elements. It provides a full-featured code editor based on Monaco (the editor from VS Code) with syntax highlighting and autocompletion. At the bottom of the tab, the content type selector controls how the text is processed before rendering. Select Markdown or AsciiDoc to have your content converted to HTML, or choose plaintext, XML, JSON, or other formats for direct output. For HTML templates like the Main Page Template, set the content type to `text/html` to output the markup directly.

### The Repeater Tab
The Repeater tab provides the same configuration options as on HTML elements, allowing you to configure the element as a repeater with a data source and data key.

### The Security Tab
The Security tab contains the Access Control settings for the current element, with owner, visibility flags and individual user / group access rights.

### The Active Elements Tab
The Active Elements tab displays the same structural overview as its counterpart on page elements, but scoped to the current element and its descendants.


## The Context Menu
The context menu provides quick access to common operations on page elements. Open it by right-clicking an element in the page tree or by clicking the context menu icon that appears when hovering over an element.

The context menu varies depending on the element type. For page elements, it only allows inserting an `<html>` element or a template element, cloning the page, expanding or collapsing the tree, and deleting the page. For content elements, the insert options are limited to Insert Before and Insert After, since content elements cannot have children. The following sections describe the full context menu available for HTML and template elements.

### Suggested Widgets (when available)
This menu item appears when a local or remote Widget exists whose `selectors` property matches the current element. Selectors are written like CSS selectors, for example `table` to match table elements or `div.container` to match div elements with the `container` class. This provides quick access to Widgets that are designed to work with the selected element type, allowing you to insert them directly as children.

### Suggested Elements (when available)
This menu item appears for elements that have commonly used child elements. For example, when you open the context menu on a `<table>` element, Structr suggests `<thead>`, `<tbody>`, `<tr>`, and other table-related elements. Similarly, a `<ul>` element suggests `<li>`, a `<select>` suggests `<option>`, and so on. This speeds up page building by offering the most relevant elements for your current context.

### Insert HTML Element
This submenu lets you insert an HTML element as a child of the selected element. It contains submenus with alphabetically grouped tag names and includes an option to insert a custom element with a tag name you specify.

### Insert Content Element
This submenu lets you insert a template or content element as a child of the selected element.

### Insert Div Element
Quickly inserts a `<div>` element as a child of the selected element.

### Insert Before
This submenu lets you insert a new element as a sibling before the selected element. It contains the same options as the main insert menu: Insert HTML Element, Insert Content Element, and Insert Div Element.

### Insert After
This submenu lets you insert a new element as a sibling after the selected element. It contains the same options as the main insert menu: Insert HTML Element, Insert Content Element, and Insert Div Element.

### Clone
Creates a copy of the selected element including all its children and inserts it immediately after the original.

### Wrap Element In
This submenu lets you wrap the selected element in a new parent element. It contains Insert HTML Element, Insert Template Element, and Insert Div Element options. Content elements are not available here because they cannot have children. The selected element becomes a child of the newly created element.

### Replace Element With
This submenu lets you replace the selected element with a different element type while preserving its children. It contains Insert HTML Element, Insert Template Element, and Insert Div Element options. Content elements are not available here because they cannot have children.

### Select / Deselect Element
Selects or deselects the element. A selected element displays a dashed border in the page tree and can be cloned or moved to a different location using the context menu.

### Clone Selected Element Here (when available)
This menu item appears when an element is selected. It clones the selected element and inserts the copy as a child of the element where you opened the context menu.

### Move Selected Element Here (when available)
This menu item appears when an element is selected. It moves the selected element from its current position and inserts it as a child of the element where you opened the context menu.

### Convert to Shared Component (when available)
This menu item appears for HTML and template elements. It converts the element and its children into a Shared Component that can be reused across multiple pages. Changes to the Shared Component are reflected everywhere it is used.

### Expand / Collapse
This submenu controls the visibility of children in the page tree. It offers three options: expand subtree, expand subtree recursively, and collapse subtree.

### Remove Node
Removes the selected element and all its children from the page. Removed elements are moved to the Recycle Bin and can be restored from there.



## Translations
Structr supports building localized frontends, allowing you to serve content in multiple languages. Instead of hardcoded text, you use the `localize()` function in content elements or templates to reference translations stored in the database. Structr then looks up the translation for the current locale and displays it. If no translation is found, the key itself is returned.

The typical workflow is to first add `localize()` calls in your page, then open the Translations flyout to create the corresponding translations for each language.

For example, to translate a table header for a list of database objects, create a content element inside the `<th>` element with the following content:

    ${localize('column_name')}

### Using domains
If the same key needs different translations in different contexts, add a domain as second parameter:

    ${localize('title', 'movies')}
    ${localize('title', 'books')}

### Managing translations
The Structr Admin UI provides two places to manage translations: the Translations flyout in the Pages area and the dedicated Localization area. The Translations flyout allows you to manage translations per page and shows which translations are used in a specific page. The Localization area is for managing translations independent of pages.

### Using the Translations flyout
Select a page from the dropdown at the top, enter a language code, and click the refresh button to load the translations. Structr scans the selected page for occurrences of the `localize()` function and lists them. For each translation, the flyout shows the key, domain, locale, and the localized text. You can create, edit, and delete translations directly here. When you change the page or language, click the refresh button to update the list.

Note that the list is empty until you use the `localize()` function in your page.

### How it works
Translations are stored as `Localization` objects in the database. Each object has four values: the key, the domain, the locale, and the translated text.

When you call `localize()`, Structr searches for a matching translation in the following order:

1. Key, domain, and full locale (e.g. `en_US`)
2. Key and full locale, without domain
3. Key, domain, and language only (e.g. `en`)
4. Key and language only, without domain

Structr stops searching as soon as it finds a match. If no translation is found, Structr can try again with a fallback locale (configurable in structr.conf). If there is still no match, the function returns the key itself.

### Locale resolution
Structr determines the current locale in the following order of priority:

1. Request parameter `locale`
2. User locale
3. Cookie `locale`
4. Browser locale
5. Default locale of the Java process



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
Widgets are stored as objects in the database with an HTML source code field. When you insert a Widget into a page, Structr parses the source code and creates the corresponding page elements. If the Widget contains template expressions in square brackets like `[variableName]`, Structr checks the configuration for matching entries and displays a dialog where you fill in the values before insertion.

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
2. Go to `http://localhost:8082/myWidgetPage?edit=1`
3. View and copy the source code of that page
4. Paste it into the Source tab

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

- `Selectors`: Controls under which elements the Widget appears as a suggested Widget in the context menu. Selectors are written like CSS selectors.
- `Is Page Template`: Check this box to make the Widget available as a page template when creating a new page.

### Widgets can define Shared Components
Widgets can define reusable Shared Components that are created when the Widget is inserted. Use `<structr:shared-template name="...">` to define a Shared Component, and `<structr:template src="...">` to insert a reference to it.

The Widget source has two parts: first the definitions of the Shared Components, then the structure that references them.

Example:

    <!-- Define Shared Components -->
    <structr:shared-template name="Layout">
        <div class="layout">
            ${render(children)}
        </div>
    </structr:shared-template>
    
    <structr:shared-template name="Header">
        <header>
            ${render(children)}
        </header>
    </structr:shared-template>
    
    <!-- Reference and nest them -->
    <structr:template src="Layout">
	    <structr:template src="Header">
		    <h1>Welcome</h1>
	    </structr:template>
    </structr:template>

This Widget defines two Shared Components: "Layout" and "Header". At the bottom, it references them and nests "Header" inside "Layout". When you insert this Widget again, Structr reuses the existing Shared Components instead of creating duplicates.


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

### Shared Components vs. Widgets

| Aspect | Widget | Shared Component |
|--------|--------|------------------|
| Storage | External source code | Part of your application |
| Insertion | Creates a copy | Creates a reference |
| Changes | Only affect new insertions | Immediately visible everywhere |
| Use case | Starting points, boilerplate | Consistent layouts, headers, footers |




## Additional Tools
The Pages area includes several additional tools for managing and searching page elements.

### Recycle Bin
When you remove an element from a page, Structr does not delete it permanently. Instead, it moves the element to the Recycle Bin. This soft-delete approach allows you to restore elements that were removed by accident.

Pages are not soft-deleted. When you delete a page, Structr removes the page itself but moves all its child elements to the Recycle Bin.

The Recycle Bin flyout shows all elements that have been removed from pages, including their children. To restore an element, drag it back into the page tree on the left. The context menu lets you permanently delete individual elements. At the top of the flyout, the "Delete All" button permanently deletes all elements in the Recycle Bin.

The Recycle Bin is not cleared automatically, but its contents are not included in deployment exports. The flyout is located on the right side of the Pages area.

### Preview
The Preview flyout shows a preview of the current page, just like the Preview tab in the center panel. This allows you to keep the preview visible while working with other tabs in the center panel. The flyout is located on the right side of the Pages area.


