# Image

Extends File with specialized image handling. When you upload an image, Structr automatically extracts EXIF metadata (camera info, GPS coordinates, date taken) and stores the dimensions. Two thumbnails, `tnSmall` (100 x 100) and `tnMid` (300 x 300), are generated asynchronously on first access and linked to the original via database relationships.

## Details

Supported formats include JPEG, PNG, GIF, WebP, and TIFF. For thumbnails of other sizes, add a property of type `Thumbnail` to a subtype. To create a scaled, cropped or re-encoded copy right away, as a standalone image, use `createImageVariant()`; it writes JPEG, PNG and GIF. The Admin UI offers a built-in image editor and an optimized view mode for browsing image folders. With Tesseract OCR installed, Structr can extract text from images for full-text search. You can extend Image or create subtypes like ProductImage.
