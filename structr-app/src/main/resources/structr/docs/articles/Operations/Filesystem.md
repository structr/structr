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

- **Local filesystem** – The default, stores files on the server's disk
- **Cloud storage** – Amazon S3 and compatible services
- **Archive systems** – For long-term storage with different access patterns

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
- The REST API upload endpoint
- Programmatically using the `$.create('File', ...)` function with file content

#### REST API Upload

To upload files via REST, send a multipart form-data POST request to `/structr/upload`:

```bash
curl -X POST http://localhost:8082/structr/upload \
  -H "X-User: admin" \
  -H "X-Password: admin" \
  -F "file=@/path/to/document.pdf" \
  -F "parent=9aae3f6db3f34a389b84b91e2f4f9761"
```

The `file` parameter contains the file data. You can include additional parameters for any property defined on the File type:

| Parameter | Description |
|-----------|-------------|
| `file` | The file content (required) |
| `parent` | UUID of the target folder |
| `name` | Override the filename |
| `visibleToPublicUsers` | Set public visibility |
| `visibleToAuthenticatedUsers` | Set authenticated visibility |

You can also pass UUIDs of related objects to link the file directly to existing data during upload.

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

Video files support:

- Transcoding between formats
- Playback from specific timestamps
- Streaming delivery

### Text Extraction

Structr integrates Apache Tika to extract text from documents. Supported formats include PDF, Microsoft Office documents (Word, Excel, PowerPoint), and many others – over a thousand file types in total.

Extracted text can be indexed for full-text search, making document contents searchable alongside structured data.

### Optical Character Recognition

If Tesseract OCR is installed on the server, Structr can extract text from images. This enables searching scanned documents or processing image-based PDFs.

### Fulltext Indexing

When indexing is enabled for a file type, Structr builds full-text indexes from extracted content. This allows searching across document contents using the same query mechanisms as structured data.

## Configuration

Key settings in `structr.conf`:

| Setting | Description |
|---------|-------------|
| `application.filesystem.enabled` | Enable per-user home directories |
| `application.filesystem.indexing.enabled` | Enable text extraction and indexing |
| `application.filesystem.indexing.maxsize` | Maximum file size (MB) for indexing |
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
