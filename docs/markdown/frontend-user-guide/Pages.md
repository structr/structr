The Pages section is the main view for Web Content Management in Structr. In this section, you can create or import individual HTML pages, modify static content and structure of a page, and - most importantly - connect your data to markup elements in a page. This process is called *Data Binding*. It allows you to specify a database query, loop over the results, and display a markup element (including its children) for each database object. This is explained in more detail in one of the following sections.

You will very likely spend most of your time in the Pages section. The Structr user interface features a modern drag and drop and inline editing style, allowing you to do most of the editing in-page. You can of course also use offline tools like your favourite text or markup editor, but this option is limited to static content, because an offline editor lacks the Structr-specific data binding options. Structr provides a built-in FTP server through which you can access pages, files and images and even modify the source code of an HTML page by uploading it to the server.

The most important element of the Pages section is the page preview in the center of the page. Initially, it is empty since there is no page to display, and all the slideouts are closed when you first log in. By clicking on one of the vertical handles, you can open a slideout, for example the Pages Tree View. There are five more slideouts which are described in the following sections.

<img src="/StructrPagesTreeView.png_thumb_300x169" class="zoomable" alt="The Pages sections with the Pages Tree View slideout opened" />

In the above screenshot, you can see two small icons towards the center of the page, just below the "Files" and "Images" links in the header. These icons allow you to create new pages and/or import existing pages from HTML source or from an URL. Click on the little plus icon, and a new page will be created from scratch, initialized with a head, a body, a title and a paragraph element. This newly created page will serve as starting point for the next steps in this section.

#### The first page

When you open the Pages Tree View slideout on the left, you will see approximately the following image.

<img src="/PageElement.png_thumb_300x173" class="zoomable" alt="A first page" />

As you can see, the preview area now displays the page as it looks like for the outside world. You can also see the page tree on the left, which we will focus on for now. The toplevel element there is the page element, which will be highlighted when you move the mouse cursor over it. Each element in the tree view has one or more icons. When you create a new page, you will see a small key icon at the end of each row, which means that the element's visibility is restricted. You can click on that little key icon to open the <a href="#Access Control & Visibility">Access Control & Visibility</a> Dialog.
