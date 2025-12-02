# System Types
## File
This type is one of Structr's built-in types for managing uploaded files and file system resources within your application.
### Properties

|Name|Description|
|---|---|
|id|UUID of this entity|
|type|type of this entity|
|createdDate|when this entity was created|
|lastModifiedDate|when this entity was last modified|
|visibleToPublicUsers|whether this entity is visible to public users|
|visibleToAuthenticatedUsers|whether this entity is visible to authenticated users|
|owner|owner of this node|
|path|full path of this file or folder (read-only)|
|parent|parent folder of this File or Folder|
|contentType|content type of the file|
|sha512|SHA512 checksum of the file's content (optional, see below)|
|isTemplate|when checked, the content of this file is evaluated as a script and the resulting content is returned|
|size|size of this file|
|md5|MD5 checksum of the file's content (optional, see below)|
|crc32|CRC32 checksum of the file's content (optional, see below)|
|sha1|SHA1 checksum of the file's content (optional, see below)|
|checksum|xxHash checksum of the file's content (generated automatically)|


### How Files Work
File nodes represent individual files stored in Structr's file system and provide access to file content, metadata, and permissions. Files are stored as nodes in the database with associated binary content stored on disk. Each File node contains metadata about the file (name, size, content type, etc.) and can be queried, secured, and manipulated like any other node type. Files can be uploaded through the Structr UI, via the REST API, or created programmatically.

### File Organization
Files can be organized into folders using the Folder type. The relationship between folders and files creates a hierarchical file system structure within Structr, similar to a traditional file system.

### Common Use Cases
- Files are used for storing user uploads, application assets (images, CSS, JavaScript), downloadable documents, and any other binary content your application needs to manage.
- Files in Structr can also be configured to generate dynamic content, so that a script is executed creates the content of the file at the time of access.

### Checksums
By default, only the (fast) xxHash checksum is calculated for a file. You can configure additional checksums in the `application.filesystem.checksums.default` setting in structr.conf.


## Folder
This type is one of Structr's built-in types for organizing files and creating hierarchical structures within your application.
### Properties

|Name|Description|
|---|---|
|id|UUID of this entity|
|type|type of this entity|
|createdDate|when this entity was created|
|lastModifiedDate|when this entity was last modified|
|visibleToPublicUsers|whether this entity is visible to public users|
|visibleToAuthenticatedUsers|whether this entity is visible to authenticated users|
|owner|owner of this node|
|path|full path of this file or folder (read-only)|
|parent|parent folder of this File or Folder|
|enabledChecksums|override for the global checksums setting, allows you to enable or disable individual checksums for all files in this folder (and sub-folders)|


### How Folders Work
Folder nodes represent directories in Structr's file system and provide a way to group and organize File nodes. Folders in Structr are stored as nodes in the database and can contain both files and other folders (subfolders). Each Folder node has properties like name and path, and can be queried, secured, and manipulated like any other node type. Folders can be created through the Structr UI, via the REST API, or programmatically.

### Folder Hierarchy
Folders create a tree-like structure by establishing parent-child relationships with other folders and files. A folder can have a parent folder and can contain multiple child folders and files. This creates a familiar file system hierarchy that mirrors traditional directory structures.

### Common Use Cases
- Folders are used for organizing uploaded files by type, user, project, or any other logical grouping that fits your application's needs.
- Folders help structure application resources like separating images, stylesheets, scripts, and documents into distinct directories.
- Folders enable permission management at the directory level, allowing you to control access to entire groups of files at once.

### External Storage
tbd


## Group
This type is one of the base classes for Structr's access control and permissions system.
### Properties

|Name|Description|
|---|---|
|id|UUID of this entity|
|type|type of this entity|
|createdDate|when this entity was created|
|lastModifiedDate|when this entity was last modified|
|visibleToPublicUsers|whether this entity is visible to public users|
|visibleToAuthenticatedUsers|whether this entity is visible to authenticated users|
|owner|owner of this node|


### How It Works
Groups enable collective permission management by allowing administrators to grant access rights to multiple users simultaneously rather than configuring permissions individually.

### Applying Groups to Schema Types
When defining custom types in Structr's schema, you can specify which groups have permission to work with instances of that type. For example, if you create a `Product` type and grant the "ProductManagers" group write permission on it, members of that group automatically get the configured permissions on `Product` nodes. This allows you to build applications where different user roles have different levels of access to your data model.


## Image
This type allows you to handle images within your application.
### Properties

|Name|Description|
|---|---|
|id|UUID of this entity|
|type|type of this entity|
|createdDate|when this entity was created|
|lastModifiedDate|when this entity was last modified|
|visibleToPublicUsers|whether this entity is visible to public users|
|visibleToAuthenticatedUsers|whether this entity is visible to authenticated users|
|owner|owner of this node|
|path|full path of this file or folder (read-only)|
|parent|parent folder of this File or Folder|
|contentType|content type of the file|
|sha512|SHA512 checksum of the file's content (optional, see below)|
|isTemplate|when checked, the content of this file is evaluated as a script and the resulting content is returned|
|size|size of this file|
|md5|MD5 checksum of the file's content (optional, see below)|
|crc32|CRC32 checksum of the file's content (optional, see below)|
|sha1|SHA1 checksum of the file's content (optional, see below)|
|checksum|xxHash checksum of the file's content (generated automatically)|
|exifSubIFDData|Exif SubIFD data|
|width|width of this image|
|gpsData|GPS data|
|exifIFD0Data|Exif IFD0 data|
|height|height of this image|
|orientation|orientation of this image|


### How It Works
Image nodes store information about images and include built-in properties and methods that make it easy to use images in your application.

### Common Use Cases
- You can store images in the Structr filesystem to use them as static resources in your web application.
- You can extend the data model to link images (and other files as well) with other types in your data model.


## Localization
This type represents translations for text snippets.
### Properties

|Name|Description|
|---|---|
|id|UUID of this entity|
|type|type of this entity|
|createdDate|when this entity was created|
|lastModifiedDate|when this entity was last modified|
|visibleToPublicUsers|whether this entity is visible to public users|
|visibleToAuthenticatedUsers|whether this entity is visible to authenticated users|
|name|name of this node|
|owner|owner of this node|
|localizedName|translated text|
|domain|domain in which the translation is valid|
|locale|locale for which the translation is valid|


### How It Works
A localization is an expression that is included in the content of a page or any other textual element in the following form:

`localize(key, domain)`

`key` is a term describing the concept that should be rendered depending on the locale which is currently active for the accessing user, and domain describes the context for this term. The combination of key and domain has to be unique.

The retrieval process works just as rendering the page. If you request the locale en_US you might get localizations for en as a fallback if no exact match is found. If no localization could be found, an empty input field is rendered where you can quickly create the missing localization.


## MailTemplate
This type represents customizable email templates.
### Properties

|Name|Description|
|---|---|
|id|UUID of this entity|
|type|type of this entity|
|createdDate|when this entity was created|
|lastModifiedDate|when this entity was last modified|
|visibleToPublicUsers|whether this entity is visible to public users|
|visibleToAuthenticatedUsers|whether this entity is visible to authenticated users|
|name|name of this node|
|owner|owner of this node|
|text|text content of the template|
|locale|locale for the template|


### How It Works
A MailTemplate is a node with a name, text content and locale that you can use to send customized emails. The text content can contain Script Expressions that are evaluated before the next processing step, and MailTemplates with pre-defined keys are used in internal processes in Structr.

### Common Use Cases
- MailTemplates are used to customize emails that are sent to the new users, if you configure Structr to allow User Self-Registration.

### Notes
- To send emails from Structr, the appropriate settings must be made in structr.conf.


## Page
This type is the main entry point for Structr's Page Rendering Engine.
### Properties

|Name|Description|
|---|---|
|id|UUID of this entity|
|type|type of this entity|
|createdDate|when this entity was created|
|lastModifiedDate|when this entity was last modified|
|visibleToPublicUsers|whether this entity is visible to public users|
|visibleToAuthenticatedUsers|whether this entity is visible to authenticated users|
|name|name of this node|
|owner|owner of this node|


### How It Works
When a user accesses the URL of a Page with their browser, a process is started that runs through this tree structure from top to bottom and generates the HTML representation for each element. This process is called page rendering. Because of this sequential processing, it is possible to run through the same element more than once without additional effort (e.g. for each result of a database query) or to hide the content of specific elements depending on certain conditions (for access control).

### Keywords
The following keywords are valid in the context of the page rendering process: `page`, `this`, `request`, ...

### Common Use Cases
- Pages can be used to server static content, like text, images etc. from Structr's filesystem.
- Pages are also the main element of a dynamic web application.

### Additional Information
- A Page either consist of HTML elements or a single Template element (a so-called "Main Page Template")
- HTML elements be configured to execute a database query (or a script) and loop over the results, creating a Repeater
- The information that comes with the request URL (path elements, request parameters, etc.) are also available in the Page Rendering process.


## ShadowDocument
This type is the main entry point for Structr's Page Rendering Engine.
### Properties

|Name|Description|
|---|---|
|id|UUID of this entity|
|type|type of this entity|
|createdDate|when this entity was created|
|lastModifiedDate|when this entity was last modified|
|visibleToPublicUsers|whether this entity is visible to public users|
|visibleToAuthenticatedUsers|whether this entity is visible to authenticated users|
|name|name of this node|
|owner|owner of this node|


### How It Works
When a user accesses the URL of a Page with their browser, a process is started that runs through this tree structure from top to bottom and generates the HTML representation for each element. This process is called page rendering. Because of this sequential processing, it is possible to run through the same element more than once without additional effort (e.g. for each result of a database query) or to hide the content of specific elements depending on certain conditions (for access control).

### Keywords
The following keywords are valid in the context of the page rendering process: `page`, `this`, `request`, ...

### Common Use Cases
- Pages can be used to server static content, like text, images etc. from Structr's filesystem.
- Pages are also the main element of a dynamic web application.

### Additional Information
- A Page either consist of HTML elements or a single Template element (a so-called "Main Page Template")
- HTML elements be configured to execute a database query (or a script) and loop over the results, creating a Repeater
- The information that comes with the request URL (path elements, request parameters, etc.) are also available in the Page Rendering process.


## User
The type `User` is one of the base classes for Structr's access control and permissions system.
### Properties

|Name|Description|
|---|---|
|id|UUID of this entity|
|type|type of this entity|
|createdDate|when this entity was created|
|lastModifiedDate|when this entity was last modified|
|visibleToPublicUsers|whether this entity is visible to public users|
|visibleToAuthenticatedUsers|whether this entity is visible to authenticated users|
|name|name of this node|
|owner|owner of this node|


### How It Works
All requests to Structr are evaluated in the context of the user making the request.

You can also impersonate other users if you need to, using the built-in function `doAs()`.

If you want to execute a script in the context of an admin user, you can use the `doPrivileged()` function.

