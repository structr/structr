
After defining a first version of the data model, the next step is usually to build a user interface. This can be done in the `Pages` area.

## Creating a Page

When you click the green "Create Page" button in the upper left corner of the Pages section, you can choose whether to create a page from a template or import one from a URL.

### Create Page Dialog

![Create Page Dialog](../../pages_create-page.png)

When you select "Create Page", you will see a list of templates that are used to create the structure of the new page. Templates are based on the Tailwind CSS framework and range from simple layouts like the Empty Page to more complex structures with sidebars and navigation menus, as well as specialized templates like the Sign-In Page.

When you create a page from a template, some components are created as shared components that you can reuse across your site, whereas the Simple Page option creates a page with standard HTML elements like `<html>`, `<head>`, and `<body>`.

#### Page Templates Are Widgets
Page templates are widgets with the `isPageTemplate` flag enabled. Structr looks at the widget server and your local widget collection and displays local and remote page templates together in the "Create Page" dialog.

### Import Page Dialog
The Import Page dialog lets you create pages from HTML source code or by importing from external URLs.

#### Create Page From Source Code
Paste your HTML code into the textarea. You can then configure the import options below before creating the page.

#### Fetch Page From URL
You can also import a page from an external URL using the text input below the textarea. This imports the page including all static resources like CSS, JavaScript, and images.

#### Configuration Options
Below the import options, you configure the name and visibility flags of the new page. You can also mark imported files to be included when exporting your application and enable parsing of deployment annotations in the imported HTML.

#### Deployment Annotations
Deployment annotations are special markers that Structr inserts when exporting HTML. They preserve Structr-specific attributes such as content types for content elements and visibility settings for individual HTML elements.



## Working With Pages
A page in Structr consists of HTML elements, template blocks, content elements, or a combination of these. The following sections explain each type of page element and how they work together to build your pages.

![Working With Pages](../../pages_page-expanded.png)

### Page Elements
The Page element sits at the top of a page's element tree and represents the page itself. Below the Page element, there is either a single Template element (the Main Page Template) or an <html> element containing <head> and <body> elements.

#### Appearance
The Page element appears as an expandable tree item with the page name and optional position attribute on the left side and a lock icon for the Access Control dialog on the right.

#### Interaction
When you hover over the Page element with your mouse, two additional icons appear: one opens the context menu (described below) and one opens the live page in a new tab.

Clicking the Page element opens the detail settings in the main area of the screen in the center.

#### Access Control
Clicking the access control icon (the lock symbol) opens the access control

#### General Settings
#### Advanced Settings
#### Preview
#### Security Settings
#### Active Elements
#### URL Routing


### HTML Elements
#### Access Control
#### General Settings
#### HTML Settings
#### Advanced Settings
#### Preview
#### Repeater Settings
#### Events Settings
#### Security Settings
#### Active Elements

### Templates & Content Elements

remember: doctype element hint:
Note that if you use a Template element, it must include the DOCTYPE definition that a classic page with an HTML element outputs automatically.

#### Access Control
#### General Settings
#### Advanced Settings
#### Preview
#### Editor
#### Repeater Settings
#### Security Settings
#### Active Elements


### Context Menu
#### Suggested Widgets
#### Insert HTML Element
#### Insert Content Element
#### Insert Div Element
#### Insert Before
#### Insert After
#### Clone
#### Wrap Element In
#### Replace Element With
#### Select Element
#### Expand / Collapse
#### Remove Node


### Static Resources

#### File download
#### Dynamic files
##### Replace template expressions"





## Translations


## Widgets
### Local Widgets
#### Creating a Widget
#### Widget Configuration
##### Source
##### Configuration
##### Description
##### Options
###### Selectors
###### Is Page Template

### Remote Widgets
#### Widget Servers


## Shared Components


## Recycle Bin

## Preview


## Search

