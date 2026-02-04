# Widget

Provides reusable building blocks for pages – from simple HTML snippets to complete configurable components. Structr parses the widget's HTML source when you insert it into a page. Key properties include `name`, `source` for HTML content, `configuration` for customizable variables (JSON), `selectors` for context menu suggestions, `treePath` for categories, and `isPageTemplate` for page creation.

## Details

Template expressions in square brackets like `[variableName]` become configurable options that users fill in when inserting. The configuration JSON defines input types, defaults, and help text. Widgets can define shared components with `<structr:shared-template>` tags. The Widgets flyout shows both local and remote widgets, enabling sharing across applications.
