A widget is a source-code representation of a reusable component.

It can contain Cypher commands to insert or modify data in the database, queries to access the data, HTML code to define the rendering out0put as well as CSS and Javascript for styling and dynamic behaviour.

Here's an example of a widget, representing a simple search function:

    <div class="structr-search">
      <form>
        <input name="search" placeholder="Search string" type="text" value="${request.search}" id="search-string">
      </form>
      <h1>Search Results</h1>
      <div class="search-results">
        <div data-structr-meta-data-key="item"
               data-structr-meta-cypher-query="
    START n=node:keywordAllNodes(type='Content'),p=node:keywordAllNodes(type='Page')
    MATCH n<-[:CONTAINS*]-p
    WHERE n.content? =~ '(?i).*${if(empty(request.search), 'sdsdf', request.search)}.*'
    RETURN DISTINCT p" class="result-item"><a href="${item.name}"><span class="name"> ${titleize(item.name, '-')} </span></a></div>
      </div>
    </div>
