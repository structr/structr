# Filesystem

Structr includes an integrated file storage system with a virtual filesystem that abstracts physical storage from the logical directory structure and metadata. Binary data can be stored on the local server filesystem, or on external storage backends through Structr's File Service Provider API. Structr's built-in web server can serve static HTML pages, CSS, JavaScript, images, and other web assets directly from this virtual filesystem, similar to how traditional web servers serve files from a document root.

## Virtual Filesystem

The virtual filesystem in Structr represents a tree of folders and files. Each folder can contain subfolders and files, creating a familiar hierarchical structure.

This filesystem is called "virtual" because the folder structure exists only in the database – it doesn't necessarily mirror a directory tree in the actual storage backend. A file's path in Structr (like `/documents/reports/quarterly.pdf`) is independent of where the binary content is physically stored.

### Benefits

This separation of metadata from storage provides flexibility:

- Files in different folders can be stored on different backends
- You can reorganize the virtual structure without moving physical files
- All files share consistent metadata, permissions, and search capabilities regardless of where they're stored
- The same file can appear in multiple virtual locations without duplicating storage

## Storage Backends

Since Structr 5.0, file content can be stored on various backends:

- **Local filesystem** (`LocalFSStorageProvider`) – The default, stores files on the server's disk
- **Cloud storage** (`GenericS3BucketStorageProvider`) – Amazon S3 and compatible services
- **In-memory storage** (`InMemoryStorageProvider`) – Keeps file content in memory only, for temporary files and tests

You can configure storage backends per folder, allowing different parts of your virtual filesystem to use different physical storage. For example, frequently accessed files might live on fast local storage while archives go to cheaper cloud storage.

## Custom Metadata

By extending the built-in File and Folder types in the schema, you can add custom metadata fields to your files. This allows you to create specialized types like:

- `InvoiceDocument` with fields for invoice number, amount, and vendor
- `ProductImage` with fields for product reference, dimensions, and alt text
- `BackupArchive` with fields for backup date, source system, and retention policy

Custom file types behave like any other schema type – you can query them, set permissions, and include them in your data model relationships.

## Working with Files

### Uploading

Files can be uploaded through:

- The Files area in the Admin UI
- A form in a page, either wired with an event action or posting straight to the upload endpoint
- The REST API upload endpoint
- Programmatically using the `$.create('File', ...)` function with file content

All uploads from outside the Admin UI go to the upload servlet at `/structr/upload` (setting `uploadservlet.path`). It needs a signed-in user: anonymous uploads are answered with 401 unless `uploadservlet.allowanonymousuploads` is set to `true` in `structr.conf`. The uploading user owns the new file, and nobody else can see it unless the upload says so with `visibleToPublicUsers` or `visibleToAuthenticatedUsers`.

#### Uploading From a Form With an Event Action

A file input in a form that is wired with an event action (create or update, on the form's **submit** event) is uploaded automatically before the action runs. The action receives the UUID of the new file under the input's name, so naming the input after a relationship property links the file to the record, for example `img` for the profile image of a User:

```html
<form>
    <input type="file" name="img" accept="image/*" data-structr-upload-visible-to-authenticated="true">
    <button type="submit">Save</button>
</form>
```

Attributes on the file input control the upload:

| Attribute | Effect |
|-----------|--------|
| `data-structr-upload-visible-to-authenticated` | `true` makes the file visible to signed-in users, e.g. for an avatar others see |
| `data-structr-upload-visible-to-public` | `true` makes the file visible to everyone |
| `data-structr-upload-parent` | UUID of the target folder |
| `data-structr-upload-type` | The type to create, e.g. `Image` or a type that extends File |

A file input with `multiple` sends a list of UUIDs, for a to-many relationship. Only a form wired with the submit event uploads its file inputs; a button wired with click does not.

#### Uploading From a Plain Form

When the record *is* a file, such as an `Invoice` type that extends File, a plain form can post straight to the upload servlet. Hidden fields set the type, the properties of the type, the visibility and where to go afterwards:

```html
<form action="/structr/upload" method="post" enctype="multipart/form-data">
    <input type="hidden" name="type" value="Invoice">
    <input type="hidden" name="project" value="${current.id}">
    <input type="hidden" name="visibleToAuthenticatedUsers" value="true">
    <input type="hidden" name="redirectOnSuccess" value="/invoices">
    <input type="file" name="file">
    <button type="submit">Upload</button>
</form>
```

#### REST API Upload

To upload files via REST, send a multipart form-data POST request to `/structr/upload`:

```bash
curl -X POST http://localhost:8082/structr/upload \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -F "file=@/path/to/document.pdf" \
  -F "parent=9aae3f6db3f34a389b84b91e2f4f9761"
```

Every part with a file becomes a file of its own. The other fields of the request:

| Parameter | Description |
|-----------|-------------|
| `type` | The type to create, e.g. `Image` or a type that extends File. It can also be given in the URL: `/structr/upload/Invoice`. Without it, the type follows the content: `Image` for images, `VideoFile` for videos, otherwise `File` |
| `parent` or `parentId` | UUID of the target folder. Outside the default upload folder and the user's home directory, the user needs write permission on it |
| `uploadFolderPath` | Path of the target folder, created if it does not exist. Requires an authenticated user and is limited to paths below the default upload folder or the user's home directory |
| `name` | Override the filename |
| `visibleToPublicUsers` | `true` makes the file visible to everyone |
| `visibleToAuthenticatedUsers` | `true` makes the file visible to signed-in users |
| `redirectOnSuccess` | Answer with a redirect to this URL instead of the UUID |
| `appendUuidOnRedirect` | `true` appends `/<uuid>` of the new file to the redirect URL |
| any property of a custom type | Properties defined in the schema, including relationship properties: the UUID of a related object links the file to it |

Built-in properties of File other than the ones above are refused with 422. Without `redirectOnSuccess`, the response is the UUID of the uploaded file as plain text.

Without `parent` or `uploadFolderPath`, files land in the folder named by `application.uploads.folder`, which defaults to `/._structr_uploads`. When `application.filesystem.enabled` is set, an authenticated non-admin user's uploads go to that user's home directory instead.

After an upload, the `onUpload` method of the file's type runs, e.g. to set its permissions or link it to other data.

#### Uploading to an External Service

The sections above cover getting a file *into* Structr. To send one *out*, pass the `File` itself as the
request body of an outbound HTTP call. Its content is streamed straight from the storage backend, so the
file is never base64-encoded or wrapped in a multipart envelope, and its size is not limited by memory.

```javascript
{
    let file = $.first($.find('File', 'name', 'report.pdf'));

    $.PUT('https://api.example.com/documents/42', file, 'application/pdf');
}
```

This works for `$.POST`, `$.PUT`, `$.PATCH`, `$.DELETE` and `$.FETCH`. Anything that is not a `File` or a
byte array is sent as text, exactly as before.

> **Credentials must be preemptive.** Basic authentication normally waits for the server to answer `401`
> and then repeats the request, which is impossible once a stream has been read. Set `preemptive` so the
> credentials go out with the first request:
>
> ```javascript
> $.PUT(url, file, 'application/pdf', { username: 'u', password: 'p', preemptive: true });
> ```
>
> Without it the call is refused with a message saying so, rather than failing later as an empty upload.

Use `$.POSTMultiPart` instead when the receiving endpoint expects a form upload with several parts.

### Accessing

Files are accessible via:

- Direct URL using the file's path or UUID
- The REST API for metadata and content
- Script functions for reading content programmatically

### Permissions

Files use the same permission system as all other objects in Structr. You can control access through:

- Visibility flags (public, authenticated users)
- Owner permissions
- Group-based access grants
- Graph-based permission resolution through relationships

## Advanced Features

### Dynamic File Content

Files can be configured to evaluate their content as a template, similar to a Template element in Pages. When the `isTemplate` flag is enabled on a file, Structr processes template expressions in the file content before serving it. This allows you to mix static and dynamic content in CSS, JavaScript, or any other file type.

The text editor in the Files area has a "Show Preview" checkbox that displays a preview of the rendered output with template expressions evaluated.

> **Note:** When `isTemplate` is enabled to generate dynamic file content, you should usually also enable `dontCache` (shown as "Caching disabled" in the General tab of the file properties dialog).

### Image Processing

When images are uploaded, Structr automatically extracts metadata and can create variants.

#### Automatic Metadata

For images, Structr automatically reads and stores:

- EXIF data (camera information, date taken, GPS coordinates, etc.)
- Image dimensions (width and height)

#### Automatic Thumbnails

Every image automatically gets two thumbnails that are:

- Generated on first access (not at upload time)
- Stored in a hidden folder `._structr_thumbnails`
- Linked to the original image via a thumbnail relationship in the database

#### Image Editing

Images can be cropped directly in the editor. This is currently the only image editing function available – other transformations like rotation or filters are not supported.

#### Supported Formats and Transformations

**Supported formats:** JPEG, PNG, GIF, WebP, TIFF

**Transformations:**
- Scaling to specific dimensions
- Cropping to aspect ratios
- Format conversion between supported types
- Thumbnail generation

### Video Processing

The media module adds the `VideoFile` type. It relies on an external `ffmpeg` binary on the server, which Structr locates with `which ffmpeg`; without it the video functions do nothing. The instance method `updateVideoInfo()` reads codec, pixel format, audio channel and duration information from the file into the corresponding properties. Converted versions of a video are linked through the `convertedVideos` property (with `originalVideo` pointing back), and a `posterImage` can hold a frame grabbed from the video. Conversion and frame grabbing call ffmpeg through converter scripts that you register in `structr.conf` as settings named `VideoFile.<scriptName>`; a conversion whose script is not registered is skipped with a warning in the server log. Structr serves video files like any other file; it does not offer timestamp-based playback or adaptive streaming.

### Content Extraction

The content-extraction module uses Apache Tika to extract text and structure (headings, pages, tables, lists, links) from documents. Supported formats include PDF, Microsoft Office documents (Word, Excel, PowerPoint), HTML, Markdown, e-mails and many others – over a thousand file types in total. Scanned documents and images are read with OCR when Tesseract is installed on the server.

Extraction is opt-in: an admin switches it on for a folder and its subfolders with the Content Extraction dialog of the Files area. New uploads, changed content and files moved into the folder are then extracted in the background, and the option "Process Existing Files" extracts the files already there. Single subfolders or files can be excluded in the same dialog. Scripts can extract any file with `$.documents.extractContent()` or `$.documents.processContent()`.

The extracted text is stored with the file and indexed for full-text search. The search box of the Files area and the global search find files by their content, ignoring case and tolerating small typos; scripts search with `$.documents.fulltextSearch()`. The functions of the module are listed under `documents` in the function reference. See [Content Extraction](/structr/docs/ontology/APIs%20&%20Integrations/Content%20Extraction) for configuration, OCR and all settings.

## Configuration

Key settings in `structr.conf`:

| Setting | Description |
|---------|-------------|
| `application.filesystem.enabled` | Enable per-user home directories |
| `application.filesystem.unique.paths` | Prevent duplicate filenames in folders |
| `application.filesystem.checksums.default` | Checksums to calculate on upload |

## Checksums

By default, Structr calculates an xxHash checksum for every uploaded file. You can configure additional checksums:

- **crc32** – Fast cyclic redundancy check
- **md5** – 128-bit hash (32 hex characters)
- **sha1** – 160-bit hash (40 hex characters)  
- **sha512** – 512-bit hash (128 hex characters)

Checksums enable integrity verification and duplicate detection.

## Scripting Access

Files and folders can be created and manipulated programmatically from any scripting context.

### Creating Files and Folders

Use `$.create()` to create files and folders:

```javascript
// Create a folder
let folder = $.create('Folder', { name: 'documents' });

// Create a file in that folder
let file = $.create('File', { 
    name: 'report.txt', 
    parent: folder,
    contentType: 'text/plain'
});
```

### Creating Folder Hierarchies

Structr provides functions to create entire folder hierarchies in one operation, automatically creating any missing parent folders.

### Reading and Writing Binary Content

You can read and write binary content programmatically:

```javascript
// Read file content
let content = $.getContent(file);

// Write file content
$.setContent(file, 'New content');
```

### Minifying Web Resources

`$.minify()` writes a minified copy of a JavaScript, CSS, HTML, SVG, XML or JSON file and returns the new file. The type is detected from the content type of the file, or from its extension when the content type is missing or not specific, and decides how the file is minified: JavaScript with terser, CSS with csso, HTML, SVG and XML by removing comments and redundant whitespace, JSON by removing all whitespace. In HTML files, inline scripts and styles are minified as well.

```javascript
// Writes /js/app.min.js next to the source
$.minify('/js/app.js');

// Writes to a path of your choice, relative to the folder of the source file
$.minify(file, 'dist/app.js');
```

The file can be passed as a file object or as its absolute path. Missing folders on the output path are created, and an existing file at the output path is overwritten, so calling `$.minify()` again after editing the source updates the minified file. A file that cannot be parsed is not written; the error names the problem.

### Custom File Types

For more control, create custom types that inherit from the File trait. This allows you to add custom properties and methods to your files while retaining all standard file functionality. For example, an `InvoiceDocument` type could have properties for invoice number and amount, plus a method to generate a PDF.

## Serving Static Websites

Structr can serve complete static websites directly from its virtual filesystem. You can upload HTML files, stylesheets, JavaScript files, images, and fonts into a folder structure, and Structr's web server delivers them to browsers just like Apache, Nginx, or any other traditional web server would.

This is useful for hosting static landing pages, documentation sites, or marketing websites alongside your dynamic Structr application. You can also use it during migration projects, serving an existing static site from Structr while gradually converting pages into dynamic Structr pages with data bindings and business logic.

To set up a static site, upload your files into the virtual filesystem while preserving the original directory structure. Files are served at URLs that match their path in the virtual filesystem, so a file at `/assets/css/theme.css` is accessible at that exact URL.

### Differences from traditional web servers

While Structr serves static files in much the same way as traditional web servers, there is one important difference: Structr does not automatically resolve directory paths to index files. A request to `/product/` resolves to the folder named `product`, not to a file like `index.html` inside it.

This means that directory-style links commonly used in static websites, such as `href="/product/"`, will not work as expected. You need to use explicit file references like `href="/product/index.html"` instead.

Note that this only applies to static files in the virtual filesystem. Dynamic Structr pages behave differently: `/product`, `/product/`, and `/product/index.html` all resolve to the page named `product`. See the Navigation & Routing chapter for details on how Structr resolves page URLs.

### Visibility and permissions

Static files follow the same permission model as all other objects in Structr. To make files accessible to public visitors, enable `visibleToPublicUsers` on the files and their parent folders. You can also restrict specific files or folders to authenticated users or individual groups, giving you fine-grained access control that traditional web servers typically require separate configuration for.
