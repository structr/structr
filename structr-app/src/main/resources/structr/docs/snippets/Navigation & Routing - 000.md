## Basics

Navigating to the URL of a page with the browser starts a multi-step process that determines if the page can be rendered. A page can only be rendered if it exists, and if it is visible for the current user. If there is no session, the user is considered _anonymous_ or _public user_.

### Error Pages
If the page doesn't exist (for example of the user navigated to an invalid URL) OR if the user is not allowed to see the page, the system responds with a 404 error. If there is a page that has the `showOnErrorCodes` attribute set, and one of the values in that attribute corresponds to error code 404, that page will be displayed instead.

### Page Position
If the URL did not contain a page name (i.e. the user navigated to `/`), the pages are ordered by their `position` attribute, and the first page that is visible for the given user will be rendered.


If your application has multiple pages

- important: we need to talk about the different permission levels and how to setup the application correctly
- not sure if that belongs to pages & templates ore navigation & routing
- we also need to talk about error pages, page positions etc., see below


To define your application's start page (displayed when users navigate to the root URL), set the `position` attribute of that page to a positive value. Pages are sorted in ascending order by this attribute, and the first visible page becomes the start page.

