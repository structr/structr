Structr can search two different kinds of data, and each has its own functions:

- **Your own types**: the objects of the types you define in the schema, e.g. `Article`, `Product` or `Customer`. You decide which attributes are searchable and how.
- **Text extracted from files**: when content extraction is enabled for a folder, Structr reads the text of the uploaded files (PDFs, Office documents, e-mails, archives, ...) and stores it in chunks (`ExtractedContentNode`). The functions in the `documents` namespace search only these chunks.

The two families do not mix: `searchFulltext()` and `searchVector()` never find text in files, and the `documents` functions never find objects of your own types.

## Which Function to Use

| You want to find ... | Data | Function | Edition | Setup |
|---|---|---|---|---|
| objects by exact values or ranges | your types | `$.find()` with predicates | all | - |
| objects whose text attributes contain certain words | your types | `searchFulltext()` | all | Fulltext checkbox on the attribute |
| objects by meaning | your types | `llmEmbed()` + `searchVector()` | `searchVector()`: all; `llmEmbed()`: Enterprise | `Double[]` attribute with a vector index, an embedding model |
| how similar two vectors are | - | `cosineSimilarity()` | Enterprise | - |
| passages in files that contain certain words | files | `documents.fulltextSearch()` | all | content extraction for the folder |
| passages in files by meaning | files | `documents.vectorSearch()` | Enterprise | content extraction plus an embedding provider linked to the folder |
| passages in files by meaning and words | files | `documents.vectorSearch(query, { hybrid: true })` | Enterprise | as above |
| an answer to a question, with the passages it is based on | files | `documents.answer()` | Enterprise | as above, plus a chat model |

## Searching Your Own Types

### By Words: searchFulltext()

Activate the *Fulltext* checkbox of a `String` or `String[]` attribute in the schema. Structr creates a fulltext index named `<type>_<attribute>_fulltext`, which `searchFulltext()` queries with Lucene syntax: words, "quoted phrases", wildcards (`Senior*`) and fuzzy matches (`alex~`).

```javascript
$.searchFulltext('Article_text_fulltext', 'vector AND index')
```

### By Meaning: searchVector() with llmEmbed()

A search by meaning finds objects whose content is similar to the search text, also when they use other words. It works with *embeddings*: an embedding model turns a text into a vector, a list of numbers, and texts with a similar meaning get vectors that point in a similar direction. `searchVector()` finds the stored vectors closest to a given vector, through a vector index.

`searchVector()` itself does not know about texts or models: you provide both the stored vectors and the vector to search with.

{enterprise}
`llmEmbed()` makes these vectors with an embedding model of a configured LLM provider, so the two are used together:

1. **Prepare the schema.** Add a `Double[]` attribute, e.g. `embedding`, save it, and open its **Vector Index** settings: set the vector size to the size of the model's vectors (the wand next to it detects the size) and keep the similarity *cosine*. See *Vector Index* in [Data Model](/structr/docs/ontology/Building%20Applications/Data%20Model).
2. **Store a vector for each object**, e.g. in the `onCreate` and `onSave` lifecycle methods of the type:

    ```javascript
    {
        const config = $.first($.find('LLMProviderConfiguration', { name: 'ollama' }));

        $.this.embedding = $.llmEmbed(config, 'nomic-embed-text', $.this.title + '\n' + $.this.text, { prefix: 'search_document: ' });
    }
    ```

3. **Search** by embedding the search text with the same model, and passing that vector to `searchVector()`:

    ```javascript
    {
        const config = $.first($.find('LLMProviderConfiguration', { name: 'ollama' }));
        const query  = $.llmEmbed(config, 'nomic-embed-text', $.methodParameters.q, { prefix: 'search_query: ' });

        return $.searchVector('Article_embedding_vector', query, 10).map(hit => hit.node);
    }
    ```

Each embedding is a request to the embedding model. For types that are saved often, embed in `onSave` only when the embedded attributes changed, which the `modifications` object tells (see *The Modifications Object* in [Data Model](/structr/docs/ontology/Building%20Applications/Data%20Model)):

```javascript
{
    const changed = $.retrieve('modifications').after;

    if ('title' in changed || 'text' in changed) {
        // embed as above
    }
}
```
{/enterprise}

The stored vectors and the search vector must come from the same model, with the same `dimensions` option. Vectors of different models cannot be compared, even if they have the same size. If you change the model, embed all objects again.

`searchVector()` returns objects with `node` and `score`, best first. With cosine similarity the score is `(1 + cos) / 2`: 1 for the same meaning, about 0.5 for unrelated texts. Only objects the current user can read are returned, so there can be fewer results than the limit.

### Comparing Two Vectors: cosineSimilarity() {enterprise}

To compare a few vectors directly, without an index, use `cosineSimilarity(a, b)`, e.g. to check whether two records describe the same thing. It returns the plain cosine, between -1 and 1, where 0 means unrelated. Note the different scale from the scores of the search functions, where unrelated is 0.5.

## Searching Text Extracted From Files

When content extraction is enabled for a folder, Structr extracts the text of every file uploaded there and stores it in chunks, with the page, the heading path and the archive entry or attachment it comes from. How to set it up is described in [Content Extraction](/structr/docs/ontology/APIs%20&%20Integrations/Content%20Extraction). The `documents` functions search these chunks, and only these: they return passages of files, not objects of your types.

- **`documents.fulltextSearch(query)`** finds passages by words, through the fulltext index of the chunks. It is part of the Community Edition. Besides Lucene syntax it has a `simple` mode for the input of a search box.

{enterprise}
- **`documents.vectorSearch(query)`** finds passages by meaning. Unlike `searchVector()`, it takes the search text itself: Structr embeds it with the model the chunks of the folder were embedded with. The chunks are embedded automatically once an LLM provider and an embedding model are linked to the folder's content extraction configuration. With `{ hybrid: true }`, the words count as well, so rare terms such as product numbers or names are found even if the model does not know them.
- **`documents.answer(question, { config })`** goes one step further: it finds the passages with `documents.vectorSearch()` (by default as a hybrid search) and hands them to a chat model, which writes an answer from these passages alone, with citations. Each call costs one request to the chat model; if nothing is found, no model is called.
{/enterprise}

```javascript
{
    const manuals = $.first($.find('Folder', { name: 'manuals' }));

    // passages by words, anywhere in the manuals
    $.documents.fulltextSearch('"reset password"', { folder: manuals, contextChunks: 1 });

    // passages by meaning (Enterprise)
    $.documents.vectorSearch('I forgot my login', { folder: manuals, hybrid: true });

    // an answer with citations (Enterprise)
    return $.documents.answer('How do I reset my password?', { config: 'ollama', folder: manuals });
}
```

All three functions only find passages of files the current user can read.

## Things to Keep in Mind

- **`searchVector()` takes a vector, `documents.vectorSearch()` takes a text.** For your own types, you embed the search text yourself with `llmEmbed()`; for files, Structr does it.
- **Same model on both sides.** A search by meaning only works if the stored vectors and the search vector come from the same embedding model.
- **Do not search the chunks with `searchVector()`.** The chunks have a vector index of their own, but `documents.vectorSearch()` embeds the query with the right model and prefix, checks the permissions per file and returns the file, page and context of each passage.
- **Scores differ.** The scores of `searchFulltext()` and `documents.fulltextSearch()` are relevance scores of the fulltext index, without a fixed range. The scores of `searchVector()` and `documents.vectorSearch()` are similarities between 0 and 1. A hybrid search returns a score that only orders the results. `cosineSimilarity()` returns a value between -1 and 1.
- **Editions.** `searchFulltext()`, `searchVector()` and `documents.fulltextSearch()` are part of the Community Edition. Everything that needs an embedding or a chat model - `llmEmbed()`, `cosineSimilarity()`, `documents.vectorSearch()` and `documents.answer()` - needs Structr Enterprise with the AI module; see *LLM Agents and Chatbots* there.
