# File

Represents files in Structr's virtual filesystem. Files store binary content on configurable storage backends (local disk, Amazon S3, or archive systems) while maintaining consistent metadata, permissions, and search capabilities regardless of physical storage location. Key properties include `name` for identification, `contentType` for MIME type handling, `size` for file size, `parent` for folder location, and `isTemplate` for enabling dynamic content evaluation with template expressions.

## Processing and Extensibility

Files support automatic checksum calculation for integrity verification, full-text indexing powered by Apache Tika for searching document contents, and OCR text extraction from images when Tesseract is installed. Files use the same permission system as all other objects, supporting visibility flags, ownership, group-based access, and graph-based permission resolution. You can extend the File type with additional properties or create subtypes for specialized file categories like InvoiceDocument or ProductImage.
