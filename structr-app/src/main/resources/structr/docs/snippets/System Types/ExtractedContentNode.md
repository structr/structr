# ExtractedContentNode

A chunk of the extracted text of a file, created by the content-extraction module. Each chunk is connected to its file through `file` and to the next chunk through `next`, so the chunks of a file can be read in reading order. Key properties include `text`, `position`, `pageStart` and `pageEnd`, `headingPath` for the section the chunk belongs to, and `tokenCount`.

## Details

Chunks are created and replaced by the module whenever a file is extracted, and deleted together with their file. They have no permissions of their own: a chunk is visible exactly when its file is visible. The `text` property has a full-text index, which the content search of the Files area, the global search and `$.documents.fulltextSearch()` use.

{enterprise}
The `embedding` and `embeddingModel` properties hold a vector for semantic search: they are written by the AI module, indexed in the vector index `ExtractedContentNode_embedding_vector`, and a chunk whose text did not change keeps its embedding when the file is extracted again. In the Community Edition they are kept as they are.
{/enterprise}
