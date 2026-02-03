# Files

The Files area is Structr's virtual file system – a familiar file browser interface where you can manage your application's static assets. CSS, JavaScript, images, documents, and any other files live here.

![Files](files_navigated-to-folder.png)

## Secondary Menu

### Create Folder

Creates a new folder in the currently selected directory. If you've created types that extend Folder, a dropdown lets you choose which type to create.

### Create File

Creates a new empty file in the current directory. Like with folders, a dropdown appears if you have custom file types.

### Mount Folder

Opens the Mount Dialog for connecting external storage locations to Structr's virtual file system.

#### The Mount Dialog

Mounting lets you integrate external directories or storage providers into Structr. When you mount a folder, Structr monitors it for changes and automatically updates metadata when files are added or modified.

The dialog includes:

- Storage Provider dropdown – Select the storage backend (currently local file system; S3 and others may be available through modules)
- Folder name – The name for the new mounted folder in Structr
- Mount target – The path or location to mount
- Scan settings – Configure how Structr detects changes: either through file system events (Unix-based systems only) or periodic scanning with a configurable interval

### Search

The search field on the right searches across all files, including their contents. This full-text search is powered by Apache Tika and can index text from PDFs, images (via OCR), Office documents, and many other formats. Type your query and press Enter to see results.

## Left Sidebar

### Favorites

At the top of the directory tree, Favorite Files provides quick access to frequently used files. Drag any file here during development to keep it handy – this is useful for JavaScript files, stylesheets, or configuration files you edit often.

### Directory Tree

Below Favorites, the familiar folder hierarchy shows your file system structure. Click a folder to view its contents on the right. Click a file or folder name to rename it inline.

## Main Area

The main area shows the contents of the selected folder.

### View Controls

At the top right, three buttons switch between view modes:

- List – Compact rows with detailed information
- Tiles – Medium-sized previews with thumbnails
- Images – Larger previews, ideal for browsing image folders

A pager on the left handles large directories, and a filter box lets you narrow down the displayed files.

### The File Table

In list view, each row shows:

- Icon – Click to download or open the file (depending on content type)
- Name – The file name
- Lock icon – Opens the Access Control dialog
- Export checkbox – Marks the file for inclusion in deployment exports (only available below the top level)
- UUID – The file's unique identifier
- Modified – Last modification timestamp
- Size – File size
- Type – Both the Structr type and MIME type
- Owner – The file's owner

Hold Ctrl while clicking to select multiple files for bulk operations.

### Uploading Files

Drag files from your desktop onto the right side of the Files area to upload them. Files are Base64-encoded and uploaded in chunks via WebSocket. This works well for smaller files; for large files or bulk uploads, consider using the REST API or deployment import.

### Search Results

When you search, results appear in a table similar to the file list. A magnifying glass icon at the start of each row shows the search context – click it to see where your search term appears within the file.

## Context Menu

Right-click a file or hover and click the menu icon to open the context menu.

### Edit File

Opens the file in a built-in editor. The editor warns you before opening binary files or files that are too large. For text files, you get syntax highlighting based on the file type.

If the file has the isTemplate flag enabled, a checkbox in the editor lets you preview the rendered output with template expressions evaluated.

### General

Opens the file's property dialog with:

- Name – The file name
- Content Type – The MIME type (affects how browsers handle the file)
- Cache for Seconds – Controls HTTP cache headers when serving the file
- isTemplate – When enabled, Structr evaluates template expressions in the file content before serving it, allowing you to mix static and dynamic content
- Caching disabled – Prevents browser caching
- Include in Frontend Export – Marks the file for deployment export

### Advanced

The raw attribute table, same as in other areas.

### Add to Favorites

Adds the file to the Favorites section for quick access.

### Copy Download URL

Copies the file's download URL to your clipboard.

### Download File

Downloads the file directly.

### Security

A submenu with:

- Access Control / Visibility – Opens the full access control dialog
- Quick toggles – Make the file visible to authenticated users, public users, or both

### Delete File

Removes the file. When multiple files are selected, this becomes "Delete Files" and removes all selected items.

## Folder Context Menu

Folders have a simpler context menu with General (just name and export checkbox), Advanced, Security, and Delete Folder.

## Content Type Features

Some content types unlock additional functionality.

### CSV and XML Files

Files with `text/csv` or `text/xml` content type show an "Import CSV" or "Import XML" menu entry that opens the import wizard documented in the Importing Data chapter.

### ZIP Archives

ZIP files show two additional options:

- Extract Archive Here – Extracts contents into the current folder
- Extract Archive to New Folder – Creates a new folder and extracts there

### Images

Images get special treatment:

#### Automatic Thumbnails

Structr generates thumbnails on first access, stored in a hidden `._structr_thumbnails` folder. Two thumbnail sizes are created automatically and linked to the original image via database relationships.

#### Metadata Extraction

EXIF data like camera settings, GPS coordinates, and timestamps are automatically extracted and stored as properties. Width and height are also read and stored.

#### Edit Image

Opens a simple editor for cropping images.

### Checksums

Structr automatically calculates checksums for all files. By default, a fast xxHash is computed; you can configure additional algorithms in structr.conf.

## Naming Conflicts

If you create a file with a name that already exists in the folder, Structr automatically appends a timestamp to make the name unique.

## Related Topics

- Importing Data – CSV and XML import wizards
- Data Model – Creating custom file types with the File trait
- Security – File permissions and visibility
- Pages & Templates – Using files in pages, the isTemplate feature
