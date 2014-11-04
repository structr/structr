Shared Components are DOM elements which are used on multiple pages. In Structr, they act as templates.

You can create a Shared Component (SC) by simply dragging and dropping an existing DOM element from the page into the SC area. The icon will then change to a yellow block in both, the page and the SC area.

<img src="/shared_components.png">


Technically, when creating a SC, Structr moves the children of the original node to become children of the SC which and replaces the original node with a new placeholder node connected by SYNC relationships. When rendering a page, Structr renders the children of the synced SC node before continuing with rendering the nodes of the page tree.

<p class="info">Note that Structr does not support "outer templates", e.g. a page framework consisting of outer nodes like html, head, and body, to be filled with inner (content or data) nodes. We will add this "page type" template functionality in an upcoming version.</p>
