# File Storage System

The Structr platform has an integrated file storage system with a virtual file system that abstracts the physical storage away from the logical directory structure and the meta data. The binary data can be stored in the local file system of the server Structr is running on, or any other file server or cloud backend that is supported by Structr through its flexible File Service Provider API implementation. 

The architectural decision to keep the file system hierarchy and metadata in the integrated [graph database](1-Database.md) and the binary data in any separate system allows great flexibility in storing files of different types and with different usage requirements on different backends.


For example, you can define that certain directory paths of the virtual file system are mapped to a local file system while other files in other directories are stored in a cloud file system or on archive or backup systems.

Structr's file storage system manages the synchronization between the virtual file system and the file storage backend systems while providing a unified access experience for its users with custom metadata and seamless integration into the platform functionality.

## Virtual File System

The virtual file system in Structr is a representation of a tree of folders (or directories). Each folder can contain multiple subfolders and files.

The file system is called "virtual", because the folder structure exists only in the database of the Structr instance and does not necessarily represent a directory tree in the actual storage backends.

## Custom Metadata

By means of the integrated custom data schema, which can be managed with the [Schema editor](../4-Admin%20User%20Interface/1-Schema editor.md), administrators can extend the built-in system data types used to store folders, files, images and videos to create a custom set of metadata fields.

This allows for creating custom file or folder types like f.e. "BackupFolder", "TravelExpenseReport" or "PassportPhoto".

## Storage Backend Provider

Since Structr 5.0, the location where the binary content of files is stored is no longer limited to a local file system that can be accessed by the Structr server process for reading and writing. Thanks to a flexible file storage provider implementation, the physical medium on which the file content is stored can be configured as a cloud service, backup or archive system, file database or still as a local file system.

The configuration of the file storage backend can even be set individually for each virtual folder in the Structr file system, providing unparalleled flexibility for file storage, while all files and all their metadata are stored in the same virtual file system.

## Advanced Functionality

There's a lot of additional functionality that comes with Structr's file storage system, such as image and video processing as well as text extraction and even OCR to extract text from images.

### Image Processing

When images are added to the storage system, Structr can optionally create variants of the original image such as thumbnail images, with a different geometry or storage format.

The following image formats are supported for conversion between them:

- JPEG
- PNG
- GIF
- WebP
- TIFF

The following geometry transformations are supported:

- Scaling
- Cropping

### Video Processing

Video files can be uploaded and processed on the Structr server. Structr supports the following video features:

- Transcoding
- Play video from timestamp
- Streaming

### Text Extraction

Structr integrates the [Apache Tika](https://tika.apache.org/) library which detects and extracts metadata and text from over a thousand different file types (such as PPT, XLS, and PDF).

### Optical Character Recognition (OCR) from Images

If the OCR library [Tesseract](https://github.com/tesseract-ocr/tessdoc) is installed on the Structr server, text can also be extracted from images.

### Fulltext Indexing

Structr generates full-text indexes for all textual content, provided that indexing is defined in the schema.

This allows text to be extracted from documents using Apache Tika and used in a full-text search.





