
The Application Map shows your whole application on one zoomable map: the pages and shared components, the data model, the methods and functions, the REST endpoints, the external services your scripts talk to, and the relations between them. Use it when you need to understand how an application works, for example when you take over an application someone else built, when you want to know what a change to a type affects, or when you look for the parts of an application that are actually used. The map is read from the application itself, so it is always up to date and needs no maintenance.

![Application Map](/structr/docs/appmap.png)

The Application Map is available to administrators. On your first visit, a short introduction explains the shapes and the controls. You can open it again at any time with the question mark above the map.

![The introduction](/structr/docs/appmap_introduction.png)

## Reading the Map

### Areas

The map is divided into sectors, one for each area of Structr. The areas always appear in the same order, so you find your way around in every application. The width of a sector shows how much of the application lies in it, so the sectors also tell you at a glance where the weight of an application is.

| Area | What it contains |
|------|------------------|
| Pages | The pages of the application, grouped into sections |
| System Types | The built-in types the application uses, such as User or File |
| Scheduled Tasks | Methods that the CronService runs |
| Integrations | The external services your scripts call, such as web hosts, databases, mail and shell scripts |
| Services & Functions | Service classes and user-defined functions |
| REST API | The endpoints that resource access grants open, for types and for methods |
| Security | Groups and the parts of the application that manage users and rights |
| Data Model | The types of the application, each with its methods |
| Shared Components | The shared components the pages include |

### Elements

Each kind of element has its own shape and colour: types are circles, pages and shared components are sheets, methods are rectangles, REST endpoints are triangles, groups are hexagons and external services are stars. The size of a type or method shows how strongly it is connected, the size of a page shows how important it is for the application: how many pages lead to it, which data it changes and whether it is public.

Elements that belong together, such as a type and its methods or the pages of one section, form a grouping with a faint outline and a name. The integrations are named after what the application does with them, for example "Call date.nager.at", "Query db.example.com (JDBC)" or "Send e-mail via smtp.example.com".

### Relations

The lines between the elements show what the application does. Functional relations, such as a method that creates, updates or deletes objects of a type, a page that reads data, or a function that sends mail, are drawn in colour and bundled along their way like the branches of a tree. Green means that something is created, amber that it is updated and red that it is deleted; the other functional relations take the colour of the element they start from. The structure of the application, such as inheritance, schema relationships and links between pages, is drawn as fine grey lines.

### Hotspots, Tags and Badges

The names of the most important elements are printed in bold: the hotspots, which many other parts of the application depend on, and the key pages. Below a page, small tags mark the start page and the pages shown for error codes such as 404. A badge on a page or shared component shows who can see it, as in the Pages area: red for public users, blue for signed in users and grey for administrators only.

## Exploring the Application

### Hover

When you move the pointer over an element, the map highlights everything it is connected to, on exactly the lines the map draws for these relations. For an element inside a grouping, the map also shows the elements of the groupings it reaches, such as the page that holds a repeater. The tooltip explains the element and its relations.

![Hovering a type](/structr/docs/appmap_hover.png)

### Click

A click holds the focus on an element. At the ends of its relations, labels appear that name the elements on the other side, also when they lie outside the screen. Click one of these labels to move on to that element. The path you took appears as a trail at the top of the map, and you can go back to any step of it. Press Esc or click the empty map to stop exploring.

![Exploring from a type](/structr/docs/appmap_explore.png)

### Zoom and Double-click

Use the mouse wheel to zoom and drag the map to move it. The closer you get, the more names the map shows. When you come close to a page or shared component, its card opens and shows its HTML structure, with the relations of its repeaters and event action mappings leaving from their elements. A method shows its code when you come close.

A double-click on a page or method glides to it and opens it, a double-click on an element inside an open page opens it with its controls (see below), and a double-click on the empty map brings back the whole map. Zooming and gliding never change the positions of the elements: the map keeps its layout, so you find everything where you left it.

![An open page](/structr/docs/appmap_page.png)

The relations of an open page leave its sheet bundled. All relations toward one side of the sheet swing from their elements to a common point on that edge and leave the sheet there together; outside, one line leads on to each element they reach, the stronger the more relations it carries. The relations of the page as a whole, such as links to other pages or the shared components it uses, are hidden while the page is open, so that only those of its elements remain.

### Search

The global search in the header also works on the map. The elements that match your search are marked, and a click on a search result moves the map to it.

## Editing Code

To edit the code of a method, the content of a template or the code of a script or style element, click or double-click it. The map glides so close that the element fills the screen, and the editor opens there. The editor only appears while the element fills the screen: it also opens when you zoom in that far, and it closes again when you zoom out, unless there are unsaved changes. Smaller on the map, the code stays readable but cannot be edited. In the editor, the text has the same size in every element. The editor is the same as in the Code area, with syntax highlighting and autocompletion. It stays on the card of the element while you zoom and move the map.

![Editing a method](/structr/docs/appmap_editor.png)

Press Ctrl+S to save. After saving, the map loads its data again, because a change of code can change relations, for example when a method now creates objects of another type. The positions of the elements and the editor stay where they are. Press Esc or click beside the editor to close it; it opens again once the element has left the screen and fills it anew. If there are unsaved changes, Structr asks whether you want to discard them. To change the name or the other settings of a method, use the Code area.

## Changing Elements of a Page

Inside an open page you can keep zooming. Every HTML element opens inside its own box, just as a page opens inside its sheet, and shows its settings as controls you can change in place. A small magnifier marks the elements that already have something to show, such as a repeater, an event action mapping, a text or a condition.

An open element shows the source of its repeater (function query or Cypher query), the query and the data key, its show and hide conditions, the event and action of each event action mapping, and the text it holds. The content of a template opens in the same editor as in the Code area. A template, and a script or style element that contains code, stands upright like the sheet of a page, so that more of its content fits when it opens; its code is edited like the content of a template. This lets you check and adjust how a page gets its data and what its elements do without leaving the map.

The controls belong to the open element nearest the middle of the screen and move with the map. Press Ctrl+S or click Save to save; the map then loads its data again, because a new query can show another type. Unsaved changes stay with their element until you save them. A repeater that takes its data from a flow shows the flow, but you change it in the Pages area, which also offers all other settings of an element. Click Open in Pages Area to switch there with the element selected in the tree of its page.

## Lenses

### Access

The "Access of" selection shows the application from the point of view of a role: public users, authenticated users or a group. The map then shows which pages the role sees and what it may do with the types the schema governs: read, write or delete. A page that the role sees but that does something the role may not do is marked as a gap in red. Such a gap is not always a mistake, but it is worth a look.

![The access of public users](/structr/docs/appmap_access.png)

### Usage

The Usage button shows how much the parts of the application are used: how often the pages were rendered, how often the REST endpoints and types were requested with any HTTP method, and how often the scheduled methods ran. The numbers count since the last start of Structr and are updated every 30 seconds. The more an element is used, the stronger it glows. Only entry points are counted, so a method that a page calls on its way gets no glow of its own.

![Usage](/structr/docs/appmap_usage.png)

### Hotspots

While the pointer rests on the Hotspots button, the map turns grey and shows only the glow of the most used elements, with their names and numbers. This is the fastest way to see where an application is under load.

![Hotspots](/structr/docs/appmap_hotspots.png)

## Light and Dark

The map has a light and a dark look. Choose the one that suits you in the header; the map remembers your choice.

![The dark look](/structr/docs/appmap_dark.png)

## Using the Map Data Elsewhere

The map is built by the endpoint `/structr/rest/_appmap`, which returns all elements, relations and groupings as JSON. You can use it with other tools, for example to check the architecture of an application in a build pipeline. The endpoint `/structr/rest/_appmap/activity` returns only the current usage numbers. Both endpoints are available to administrators only.

## Related Topics

The Application Map shows what you build in the other areas of the Admin User Interface: the data model in the Schema area, the methods in the Code area, the pages in the Pages area and the resource access grants in the Security area.
