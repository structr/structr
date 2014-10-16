You can declare custom methods in dynamic schema entities which allows you to specify behaviour / business logic in a functional way.

<h5>Predefined Methods</h5>
There are three pre-defined methods which are called automatically on specifiy lifecycle events.
<table>
<tr><th>Method name</th><th>Description</th></tr>
<tr><td>onCreate</td><td>Called when a new instance of this dynamic type is created in the database</td></tr>
<tr><td>onSave</td><td>Called when an instance of this type is modified (this does <b>not</b> include creation)</td></tr>
<tr><td>onDelete</td><td>Called when an instance of this type is deleted</td></tr>
</table>

You can of course declare more than one method of each type, suffixing the individual methods with a number (e.g. <code>onCreate01, onCreate02, ...</code>). This allows you to specify a series of functions that will be executed when a new entity is created.

<h5>Custom Methods</h5>
In addition to the predefined lifecycle methods, you can declare arbitrary methods that can later be executed using a REST call to a special URL. If you for example declare a method named <code>doSend</code> on an <code>Email</code> entity, you can execute this method using the UUID of the desired entity using <code>POST /emails/<uuid>/doSend</code>.

<h5>Examples</h5>
<p>With the following <code>onCreate</code> expression, you can for example set the 'timestamp' property of a newly created entity to the current time and date, and trigger a geocoding. This example illustrates the syntax of the template expression used in callbacks:<br />
<pre>
(
    set(this, 'timestamp', now),
    set(this, geocode(this.street, this.city, this.country))
)
</pre>
</p>

<p>
The next example illustrates the use of the <code>error()</code> method to prevent the creation of invalid entities in the database:<br />
<pre>
(
    if(empty(this.name),
        error('User', 'name', 'must_not_be_empty'),
        null,
    )
)
</pre>
</p>
<p class="info">Please note that you must use parentheses around a comma-separated list of expressions like in the first example in order for all the expressions to be evaluated.</p>
