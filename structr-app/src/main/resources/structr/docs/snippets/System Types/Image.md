# Image

Extends File with specialized image handling. When you upload an image, Structr automatically extracts EXIF metadata (camera info, GPS coordinates, date taken) and stores the dimensions. Two thumbnails are generated on first access and linked to the original via database relationships.

## Details

Supported formats include JPEG, PNG, GIF, WebP, and TIFF. You can scale, crop, and convert between formats. The Admin UI offers a built-in image editor and an optimized view mode for browsing image folders. With Tesseract OCR installed, Structr can extract text from images for full-text search. You can extend Image or create subtypes like ProductImage.
