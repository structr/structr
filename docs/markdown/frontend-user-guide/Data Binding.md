Data Binding in Structr means turning DOM elements into data repeaters by assigning a query (REST, Cypher, or XPath) and a key which can be used to access the query results. If you for example want to display a paragraph element for each Image in your database, you would create a template paragraph in the desired place, and assign a query that returns all the images. You can choose from three different query types (REST, Cypher, XPath), however only two of them make sense in the current example. Since we want to access the database, we can only use REST or Cypher here. The XPath option allows you to use page elements as a data source for your repeater, which will be covered in a more advanced example later. To simplify matters in this example, we use the REST query `/images` and bind it to the data key `image` like in the following screenshot.

<img src="/query-and-data-binding.png_thumb_300x193" class="zoomable" />

The result should look roughly like this.

<img src="/repeated-element.png_thumb_300x157" class="zoomable" />

<p class="warning">Please note that you need to have some images in your database in order for this example to work as described.</p>

Now Structr displays the paragraph element for each Image in the result list, but since the paragraph is not parameterized with the data key, it displays the static text "Initial text for p". Let's say we want to display the name of the image in each paragraph, so we just replace the "Initial text for p" with a data key reference `${image.name}` like so:

<img src="/repeater-placeholder.png_thumb_300x171" class="zoomable" />

And the result should look like this:

<img src="/repeater-result.png_thumb_300x172" class="zoomable" />

<p class="info">You can use a data key reference in almost every dialog in Structr, for example ${image.id}.</p>
