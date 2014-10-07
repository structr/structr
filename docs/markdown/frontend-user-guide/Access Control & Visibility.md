<img src="/visibility.png_thumb_300x193" class="zoomable" alt="Access Control & Visibility Dialog" />

The Access Control & Visibility dialog allows you to control ownership, visibility and permissions of an element.

The visibility flags have the following implications:

* _Visible to public users_ means that every front-end user can see the object (i.e. the page, HTML element, image or custom object). You can use this setting to hide a "Logout" button in your application.
* _Visible to autenticated users_ means that only users that are logged into your application can see the given element. You can use this setting to show a "Logout" button for example.
* If none of the visibility flags are set, the object is only visible to its <b>owner</b> or any <b>groups</b> that are associated with the object.

There are three different security levels:

1. Ownership: For everything you created in Structr, you're the owner. There's only one owner, but it can be a group as well. Only the owner and everyone with Access Control rights can change the ownership (so it can be a one-way action, so be careful. :-))

2. Visibility: This useful for publishing things to either everyone (public, not-logged-in users) or to authenticated users only.

3. Individual Access Rights: Here, you can set more granular access rights for single users or groups.

All users contained in a group inherit the same access rights of the group (and ownership, too).

