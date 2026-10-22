
The content-extraction module reads the text of your documents, so that you can find files by what they contain and work with their text in scripts. It understands over a thousand file types, including PDF, Word, Excel, PowerPoint, OpenDocument, HTML, Markdown, plain text and e-mails. Scanned documents and images are read with OCR (text recognition) when Tesseract is installed on the server.

Structr stores the extracted text with the file, split into smaller sections called chunks. The search box of the Files area and the global search use the stored text to find files by their content.

> **Note:** Automatic extraction is off until you switch it on for a folder. Files outside such folders are not extracted automatically, but you can still extract them with the built-in functions.

## Enabling Automatic Extraction

You switch automatic extraction on per folder in the Files area. You need to be an admin to do this.

1. Open the Files area and navigate to the folder.
2. Click **Content Extraction** in the function bar, or right-click the folder and choose **Content Extraction...**.
3. Make sure **Extract Automatically** is checked.
4. Check **Process Existing Files** if the folder already contains files.
5. Click **Create**.

The setting covers the folder and all of its subfolders. From now on, Structr extracts files in the background whenever:

- a file is uploaded to the folder, in the Files area, through the REST interface, via FTP or SFTP, or by a script;
- the content of a file in the folder is replaced;
- a file or folder is moved into the folder.

With **Process Existing Files**, Structr also extracts the files that are already in the folder. Files that have not changed since their last extraction are skipped, so you can safely use the option again later.

The extraction runs in the background, and you can follow its progress in the Job Queue area. Large documents and documents that need OCR can take a while.

You can also open the dialog for a single file. It shows whether the file is extracted automatically, which folder this setting comes from, and the result of its last extraction.

### Adjusting the Extraction

The dialog contains further options, for example how large the chunks are and whether OCR is used. Every option shows a short explanation when you hover over its name. Empty fields use the defaults, which are shown in the fields, and the defaults work well for most documents.

If you change an option for a folder whose files are already extracted, check **Process Existing Files** when you save. Changing an option alone does not extract the files again.

## Disabling Automatic Extraction

There are three ways to stop automatic extraction, depending on what you want to exclude:

- **Exclude a subfolder or a single file** – open the dialog for the subfolder or file, uncheck **Extract Automatically** and click **Create**. Files in that subfolder are no longer extracted automatically, while the rest of the folder stays as it is.
- **Switch it off for a folder** – open the dialog for the folder and click **Remove Configuration**. Alternatively, uncheck **Extract Automatically** and click **Save**, which keeps your options for later.
- **Switch it off everywhere** – disable the setting `contentextraction.autoextraction.enabled` in the Configuration Interface. This stops automatic extraction for all folders without changing their configurations.

Disabling automatic extraction does not delete text that was already extracted, so these files can still be found by their content. To remove the extracted text of a file, use `$.documents.deleteExtractedContent()` (see below).

## Searching by Content

The search box of the Files area and the global search find files by their content once the files are extracted. The search ignores case, matches word beginnings and parts of words, and tolerates small typos. A file is found when its text contains all search words, and the results show snippets around the matches.

In scripts, `$.documents.searchExtractedContent()` searches the chunks and returns the matching chunks with their file, page and section:

```javascript
{
    let manuals = $.first($.find('Folder', { path: '/manuals' }));
    let hits    = $.documents.searchExtractedContent('"reset password"', { folder: manuals, limit: 5, contextChunks: 1 });

    for (let hit of hits) {
        $.log(hit.file.name + ', page ' + hit.pageStart + ': ' + hit.context);
    }
}
```

By default, the query uses Lucene syntax, so phrases, `AND`, `OR` and wildcards work. With `mode: 'simple'`, the query is read like the search box input. Only files the current user can read are searched.

### Searching With Cypher

If you write Cypher, you can query the extracted text directly with `$.cypher()`, for example to combine the content search with your own data model. Every chunk is an `ExtractedContentNode` with its text in the `text` property. It is connected to its file by a `HAS_EXTRACTED_CONTENT` relationship. The text is indexed in the full-text index `ExtractedContentNode_content_fulltext`, together with the `embeddedPath` property, so a chunk is also found by the name of the archive entry or attachment it comes from.

The following query finds the ten chunks that match best, together with their file:

```javascript
{
    let hits = $.cypher(`
        CALL db.index.fulltext.queryNodes('ExtractedContentNode_content_fulltext', $query) YIELD node, score
        MATCH (file:File)-[:HAS_EXTRACTED_CONTENT]->(node)
        WITH file, node, score
        ORDER BY score DESC
        LIMIT 10
        RETURN { file: file, chunk: node, score: score }
    `, { query: 'invoice AND reminder' });

    for (let hit of hits) {
        if (hit.file) {
            $.log(hit.file.name + ', page ' + hit.chunk.pageStart + ': ' + hit.score);
        }
    }
}
```

To get one result per file instead of one per chunk, group the chunks by their file:

```cypher
CALL db.index.fulltext.queryNodes('ExtractedContentNode_content_fulltext', $query) YIELD node, score
MATCH (file:File)-[:HAS_EXTRACTED_CONTENT]->(node)
WITH file, max(score) AS score, count(node) AS matches
ORDER BY score DESC
LIMIT 10
RETURN { file: file, matches: matches, score: score }
```

The query uses Lucene syntax, like `searchExtractedContent()`: phrases in quotes, `AND`, `OR`, wildcards such as `remind*`, and fuzzy terms such as `reminder~`. Each chunk node also has `position`, `pageStart`, `pageEnd` and `headingPath`, which tell you where in the document the text is. To search only one property of the index, put its name in front of the term, for example `text:invoice` or `embeddedPath:invoice*`.

### Archives, Attachments and Images

With the option **Include Embedded**, the entries of archives (ZIP and others), the attachments of e-mails and documents embedded in office files are extracted as well. Each chunk then has an `embeddedPath`: the path of the entry inside the archive, such as `docs/guide.txt`, with nested archives joined by `/`, as in `inner.zip/docs/guide.txt`. Chunks of the file itself have no embedded path. A chunk never contains text of two entries, and page numbers and section headings start over in every entry.

With the option **Include Images**, images inside documents are read with OCR, too: the images of a PDF page, pictures in office documents and inline images of e-mails. Tiny images such as icons are left out, and so are the images of PDF pages that OCR reads as a whole anyway, like scanned pages, so their text does not appear twice. An image that is an attachment or an archive entry of its own is a document, which **Include Embedded** covers. Reading images takes time, so the option is off by default.

> **Important:** Return nodes, not their properties. Structr checks permissions for every node a Cypher query returns and leaves out files and chunks the current user cannot read, so `hit.file` is empty for them. Plain values such as `node.text` are returned without this check, so returning them directly would show the text of files the user is not allowed to read.

## Extracting Content With Built-in Functions

The built-in functions of the module work on any file, whether or not automatic extraction is enabled for its folder. They are in the `documents` namespace: `$.documents.extractContent()` in JavaScript and `${documents.extractContent()}` in StructrScript. The [Built-in Functions](/structr/docs/ontology/References/Built-in%20Functions) reference describes every function and its options.

### Reading the Text of a File

`$.documents.extractContent()` reads a file and returns its text right away. Nothing is stored, so the file is not found by the content search afterwards.

```javascript
{
    let file   = $.first($.find('File', { name: 'handbook.pdf' }));
    let result = $.documents.extractContent(file);

    $.log(result.metadata.title + ' (' + result.pageCount + ' pages)');
    $.log(result.text);
}
```

The result contains the plain `text`, the same text as `markdown` with headings, lists and tables, the `language` of the document, its `pageCount` and its `metadata`, such as title and author.

For specific parts of a document, use `$.documents.extractTables()`, `$.documents.extractLinks()` or `$.documents.extractMetadata()`.

### Extracting and Storing the Text

`$.documents.processContent()` extracts a file, a list of files or a whole folder and stores the text, just like automatic extraction. Afterwards, the files can be found by their content. You need write permission on the files.

```javascript
{
    let file    = $.first($.find('File', { name: 'handbook.pdf' }));
    let results = $.documents.processContent(file, { wait: true });

    $.log(results[0].status);
}
```

With `wait: true`, the function waits until the file is extracted and returns the result. Without it, the extraction runs in the background, which is better for folders and large files:

```javascript
{
    let archive = $.first($.find('Folder', { path: '/archive' }));

    $.documents.processContent(archive);
}
```

Files that have not changed since their last extraction are skipped. Pass `force: true` to extract them again anyway.

### Working With Stored Text

| Function | Description |
|----------|-------------|
| `getExtractedText(file)` | Returns the stored text of a file |
| `deleteExtractedContent(file)` | Deletes the stored text of a file |
| `extractContentInfo(file)` | Explains whether and how a file is extracted |

```javascript
{
    let file = $.first($.find('File', { name: 'handbook.pdf' }));

    $.log($.documents.getExtractedText(file));
}
```

### Other Functions

| Function | Description |
|----------|-------------|
| `extractEmbedded(file)` | Lists the attachments of an e-mail or the entries of an archive |
| `splitContent(textOrFile)` | Splits a text or file into chunks without storing them |
| `detectLanguage(text)` | Detects the language of a text |
| `countTokens(text)` | Counts the tokens of a text, as language models count them |
| `truncateTokens(text, maxTokens)` | Shortens a text to a number of tokens |
| `normalizeText(text)` | Cleans up extracted text, e.g. words hyphenated at line breaks |
| `stopWords(language)` | Returns the common words of a language, such as "the" and "and" |
| `getContentContext(chunk)` | Returns a chunk together with the chunks before and after it |

## Embeddings and Semantic Search (Enterprise Edition)

Available in Structr Enterprise with the AI module.

Keyword search finds the words of a query. Semantic search finds the meaning: a question like "How do I get back into my account?" finds a passage about resetting a password, although they share no word. For this, each chunk is turned into an embedding, a vector of numbers that describes its meaning, by an embedding model such as `nomic-embed-text` (Ollama) or `text-embedding-3-small` (OpenAI). Structr stores the vector in the chunk's `embedding` property and keeps a vector index over it, `ExtractedContentNode_embedding_vector`.

### Enabling It for a Folder

Open the Content Extraction dialog of a folder and choose an **Embedding Provider** - an LLM provider configuration of type Ollama or OpenAI-compatible; Anthropic has no embedding models - and an **Embedding Model**. The dialog suggests the embedding models the provider reports, and refuses a chat model. Choosing a model fills in the **Document Prefix** and **Query Prefix** it expects - `search_document: ` and `search_query: ` for nomic-embed-text, `passage: ` and `query: ` for e5 - which are put in front of every chunk and every search query; most other models need none. Changing the document prefix later embeds all files again. When you save, Structr embeds one short text to check the provider and to learn the size of the vectors, and creates the vector index. From then on, every file in the folder and its subfolders is embedded after it is extracted, in the job queue `embedding`. Files that were extracted before are embedded right away. In scripts, `$.documents.linkEmbeddingProvider(folder, 'ollama', 'nomic-embed-text')` does the same.

Below the provider and model, the dialog shows **Max. Input Tokens** - the longest text the model reads; longer chunks are cut and the file shows a warning - and the state of the embeddings: how many chunks are embedded, the model, the vector index and failed files. Ollama reports the limit of each model, for OpenAI's own models it is known, and it is filled in when you save; choose a chunk size the model can read. If the provider cannot be reached, the dialog says why, e.g. "unknown host": if Structr runs in Docker and Ollama on the same machine, the `host` of the provider configuration is `http://host.docker.internal:11434`.

Documents are only sent to an embedding provider for folders where an administrator linked one. A file shows its state in `contentEmbeddingStatus` (`pending`, `running`, `done`, `failed`) and `contentEmbeddingError`. All folders must use the same model; to change it everywhere, use `$.documents.resetEmbeddings({ model: '...' })`, which embeds everything again. Chunks whose text did not change keep their embedding when a file is extracted again, so small edits are cheap.

### Three Ways to Search

| Function | Finds | Edition |
|---|---|---|
| `$.documents.searchExtractedContent(query)` | the words of the query, through the full-text index | Community |
| `$.documents.searchContentSemantic(query)` | the meaning of the query, through the vector index | Enterprise |
| `$.documents.searchContentSemantic(query, { hybrid: true })` | the meaning and the words: chunks that match both come first, and rare words such as product numbers are found even if the model does not know them | Enterprise |

`$.documents.answerFromContent(question, { config })` goes one step further: it finds the best chunks and has a chat model answer the question from them, with citations like [1], [2] that point to file, page and section. The chunks reach the model as data that is never followed, so an instruction written into a document is quoted, not obeyed. LLM agents get the same search as the `knowledge-search` tool.

```javascript
{
    let hits = $.documents.searchContentSemantic('How do I get back into my account?', { hybrid: true, limit: 3, contextChunks: 1 });

    for (let hit of hits) {
        $.log(hit.file.name + ', page ' + hit.pageStart + ' (' + hit.source + '): ' + hit.context);
    }
}
```

As with keyword search, only chunks of files the current user can read are found. `$.documents.embeddingStatus()` shows how many chunks are embedded, which files failed and the state of the vector index; `$.documents.embedContent()` embeds files again. The article *LLM Agents and Chatbots* (Enterprise) describes providers, models and the agent tool in detail.

The vector index is an ordinary Structr vector index, so the Community function `$.searchVector('ExtractedContentNode_embedding_vector', vector, limit)` queries it as well, with a vector from the same model.

### Jobs and Settings

Embedding runs in the job queue `embedding`, next to the extraction jobs in the queue `content`; the Job Queue area shows its progress and can pause and abort it. Two settings in the category "AI Module" of the Configuration Interface control it:

| Setting | Default | Description |
|---------|---------|-------------|
| `ai.embedding.enabled` | `true` | Embeds automatically where a folder links a provider; without a link nothing is embedded |
| `ai.embedding.queue` | `embedding` | The job queue of the embedding jobs |

### Downgrading to the Community Edition

When an instance that used embeddings runs the Community Edition again, nothing is lost and nothing fails:

- The embeddings stay on the chunks and the vector index stays in the database. They are used again after a return to Structr Enterprise, without embedding anything again. At startup, Structr logs once how many chunks carry embeddings.
- Keyword search and automatic extraction work as before.
- Scripts that call `$.documents.searchContentSemantic()`, `answerFromContent()` or the other Enterprise functions get an error that says they need Structr Enterprise.
- The links between folders and LLM providers and the embedding state of files are ignored.

A deployment export of the Community Edition into a folder that holds an earlier export of Structr Enterprise keeps the old files in `modules/ai/`, including the LLM provider configurations and their API keys. Export into an empty folder, or remove that folder.

## OCR

OCR reads text from images and scanned PDF pages. It uses Tesseract, which has to be installed on the server:

- The Structr Docker image includes Tesseract with English, German, French, Spanish, Italian and Dutch.
- The Debian package recommends Tesseract, so it is installed along with Structr unless recommended packages are skipped.
- On other systems, install Tesseract and its language packages, for example with `brew install tesseract tesseract-lang` on macOS.

Without Tesseract, documents are extracted without OCR, so images and scanned pages yield no text. By default, OCR is used for images and for PDF pages that contain no text, in English and German. You can change this in the Content Extraction dialog or with the settings below.

## Settings

The settings are in the category "Content Extraction Module" of the Configuration Interface. They are the defaults for all folders; the Content Extraction dialog can override most of them per folder. The most important ones:

| Setting | Default | Description |
|---------|---------|-------------|
| `contentextraction.autoextraction.enabled` | `true` | Switches automatic extraction on or off for all folders |
| `contentextraction.extraction.maxfilesize` | `50` | Maximum file size in MB; larger files are skipped |
| `contentextraction.ocr.mode` | `auto` | When OCR is used: `auto`, `always` or `off` |
| `contentextraction.ocr.languages` | `eng+deu` | OCR languages |
| `contentextraction.ocr.tesseractpath` | (empty) | Directory of the Tesseract program, if it is not found automatically |

The [Settings](/structr/docs/ontology/References/Settings) reference lists all settings of the module.

## Related Topics

- [Files](/structr/docs/ontology/Admin%20User%20Interface/Files) - The Files area and its search box
- [Job Queue](/structr/docs/ontology/Admin%20User%20Interface/Job%20Queue) - Following the progress of extraction jobs
- [Filesystem](/structr/docs/ontology/Operations/Filesystem) - Files, folders and storage backends
- [Migration](/structr/docs/ontology/Expert%20Topics/Migration) - Changes for applications that used the text-search module
