# File

Represents files in Structr's virtual filesystem. Files can live on different storage backends – local disk, Amazon S3, or archive systems – while keeping consistent metadata and permissions. Key properties include `name`, `contentType` for MIME type, `size`, `parent` for folder location, and `isTemplate` for dynamic content evaluation.

## Details

Structr automatically calculates checksums, extracts text via Apache Tika for full-text search, and supports OCR when Tesseract is installed. Files use the same permission system as all other objects. You can extend the File type with custom properties or create subtypes like InvoiceDocument or ProductImage.
