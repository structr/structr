# ContentExtractionConfiguration

Configures automatic content extraction for one folder and its subfolders, or for one file. Key properties include `target` for the folder or file, `enabled` to switch extraction on or off, and the extraction and split options such as `splitter`, `chunkSize`, `chunkOverlap` and `ocr`.

## Details

The configuration closest to a file decides whether and how it is extracted; options it leaves empty come from the `contentextraction.*` settings. Each folder or file can have at most one configuration, and it is deleted together with its target. Only admin users can create, change and remove configurations, usually with the Content Extraction dialog of the Files area. Configurations are included in the deployment export.
