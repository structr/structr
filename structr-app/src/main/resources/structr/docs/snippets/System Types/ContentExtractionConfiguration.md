# ContentExtractionConfiguration

Configures automatic content extraction for one folder and its subfolders, or for one file. Key properties include `target` for the folder or file, `enabled` to switch extraction on or off, and the extraction and split options such as `splitter`, `chunkSize`, `chunkOverlap`, `ocr`, `includeEmbedded` and `includeImages`.

## Details

The configuration closest to a file decides whether and how it is extracted; options it leaves empty come from the `contentextraction.*` settings. Each folder or file can have at most one configuration, and it is deleted together with its target. Only admin users can create, change and remove configurations, usually with the Content Extraction dialog of the Files area. Configurations are included in the deployment export; extracted content is not, so after an import the files of enabled configurations are extracted again in the background.

{enterprise}
The AI module adds `embeddingProvider` (the LLM provider configuration that embeds the chunks of the files the configuration covers), `embeddingModel`, `embeddingDimensions`, `embeddingMaxInputTokens`, `embeddingVectorSize`, `embeddingDocumentPrefix` and `embeddingQueryPrefix` (what the model expects in front of a chunk and a search query). These links are exported by the AI module. In the Community Edition they are ignored.
{/enterprise}
