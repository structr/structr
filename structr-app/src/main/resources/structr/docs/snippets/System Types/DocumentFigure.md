# DocumentFigure

A figure of a document (a chart, diagram, table or picture) stored as an image of its own. It is an `Image`, and so a `File`, with what figure extraction found out about the figure: `label` (e.g. "Figure 3"), `caption`, `page`, `position`, `source`, `bbox`, `mentions` (the sentences of the document that refer to it), `imageHash` and `text`. `document` links it to the file it is from; the file lists its figures in `figures`.

## Details

Content extraction creates DocumentFigures when the content extraction configuration of a folder has Extract Figures checked: in a folder next to the document, with the owner, visibility and grants of the document. When the document is extracted again, a figure with the same image keeps its node; figures the document no longer has are deleted. A DocumentFigure can also be created from a result of `$.documents.extractFigures()` with `$.create('DocumentFigure', figure)`.

`text` is the one text of the figure: what extraction read in it (caption, labels, legends, table cells, OCR), until an application writes another, e.g. what a vision model says about the image. It is kept as a chunk of the document (`content`), so the content searches find it; the chunk follows every change of the text.

A DocumentFigure is a normal file: it has its own permissions after it was created and is kept when its document is deleted. Use it as the base type of your own figure types in the schema, and name that type as Figure Type in the content extraction configuration.
