# Image

Extends the File type with specialized handling for image files. When images are uploaded, Structr automatically extracts and stores EXIF metadata (camera information, date taken, GPS coordinates) and image dimensions (width and height). Every image automatically receives two thumbnails that are generated on first access, stored in a hidden `._structr_thumbnails` folder, and linked to the original image via database relationships.

## Formats and Editing

Supported formats include JPEG, PNG, GIF, WebP, and TIFF. Images support transformations including scaling to specific dimensions, cropping to aspect ratios, and format conversion between supported types. The Admin UI provides a built-in image editor for cropping and an Images view mode optimized for browsing image folders. When Tesseract OCR is installed, Structr can extract text from images for full-text search. You can extend the Image type with additional properties or create subtypes for specialized image categories like ProductImage or ProfilePhoto.
