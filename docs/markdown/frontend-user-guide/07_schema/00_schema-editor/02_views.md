A "view" in Structr is a set of local and remote properties that is bound to a name and can be accessed via REST. Entity types can have an arbitrary number of views that can contain different properties. You can select a view by appending its name to a REST URL. The default view is selected automatically if you don't supply a view name in the REST URL.

<table>
<tr><th>URL</th><th>View</th></tr>
<tr><td><code>/projects</code></td><td>default</td></tr>
<tr><td><code>/projects/c539c26cc20e4e33b66e99a2ac031457</code></td><td>default</td><tr>
<tr><td><code>/projects/appData</code></td><td>appData</td></tr>
<tr><td><code>/projects/c539c26cc20e4e33b66e99a2ac031457/webApp</code></td><td>webApp</td><tr>
</table>

<h5>Example</h5>
Let's assume we have defined a "Project" entity with the properties "name", "members" and "title", all of which should be included in the default view, so we define the default view like this:
<table>
<tr><th>Name</th><th>Attributes</th></tr>
<tr><td>default</td><td>name, members, title</td></tr>
</table>



<p class="info">Please note that in the recent versions of Structr, you have to explicitly declare the properties that the default view should contain.</p>
