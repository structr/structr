<table>
<tr><th>Function</th><th>Description</th><th>Example</th></tr>

<tr><td>GET(URL[, contentType[, selector]])</td><td>Returns a page or partial page data fetched from the given URL.</td><td><code>${GET('http://structr.org', 'text/html', 'h1')} => Bringing Structr to the Graph</code></td></tr>

<tr><td>get_counter(level)</td><td>Returns a (LaTeX-like) counter value for the given numeric level</td><td><code>${get_counter(0)}</code></td></tr>
<tr><td>inc_counter(level [, resetLowerLevels])</td><td>Increases the counter value for the given level (with an optional boolean parameter that indicates whether lower levels should be reset to zero). Call this method in a Content element before you output the counter value.</td><td><code>${inc_counter(0)}</code></td></tr>
<tr><td>reset_counter(level)</td><td>Resets the counter value for the given level.</td><td><code>${reset_counter(0)}</code></td></tr>

<tr><td>send_plaintext_mail(fromAddress, fromName, toAddress, toName, subject, content)</td><td>Sends a plaintext e-mail with the given parameters</td><td><code>${send_plaintext_mail(...)}</code></td></tr>
<tr><td>send_html_mail(fromAddress, fromName, toAddress, toName, subject, htmlContent [, plaintextContent])</td><td>Sends an html e-mail with the given parameters, including an optional plaintext part</td><td><code>${send_html_mail(...)}</code></td></tr>

<tr><td>geocode(street, city, country)</td><td>Uses the geocoding provider configured in the Structr configuration file obtain geo-coordinates (latitude/longitude) for the given address. The result of this function is a composite object that contains two (key, value) pairs which can be used by the set() function, i.e. you can do<br/><code>${set(this, geocode(this.street, this.city, this.country))}</code>.<br/>The result of the geocode() function contains both the keys and the values, so the entity will have the latitude/longitude properties set afterwards.</td><td><code>${geocode(street, city, country)}</code></td></tr>

<tr><td>error(type, token [, value])</td><td>Raises an error in a Structr transaction, resulting in a 422 Unprocessable Entity error message. Use this method for example in an <code>onCreation</code> callback to signal an error condition and prevent the entity from being created.</td><td><code>${error('User', 'name_must_not_be_empty')}</code></td></tr>

<tr><td>xml(xmlSource)</td><td>Tries to parse the given XML source into a document object.</td><td><code>${xml(src)}</code></td></tr>
<tr><td>xpath(xmlDocument, expression)</td><td>Tries to evaluate the given XPath expression on the given XML document, returning the value as a string</td><td><code>${xpath(xml(src), '//column')}</code></td></tr>

<tr><td>merge_properties(source, target , mergeKeys...)</td><td>Copies all the property values of the keys given in mergeKeys from the given source entity to the given target entity.</td><td><code>${merge_properties(this, this.copy, 'name', 'eMail')}</code></td></tr>
<tr><td>keys(entity, viewName)</td><td>Returns all the property keys of the the given entity that are registered in the view with the given name</td><td><code>${keys(this, 'public')} => ['name', 'type', 'id']</code></td></tr>


</table>
