# Template

Contains text or markup that outputs directly into pages. Unlike HTML elements, templates give you full control over where children appear – you call `render(children)` explicitly. This lets you define layouts with multiple insertion points like sidebars and main content areas. Key properties include `name`, `content`, `contentType` (Markdown, AsciiDoc, HTML, JSON, XML, plaintext), and repeater settings.

## Details

The `render()` function controls exactly where each child appears, while `include()` pulls content from elsewhere in the page tree. The Main Page Template typically sits below the Page element and defines the overall structure. Templates can also produce non-HTML output by setting the content type to application/json or text/xml.
