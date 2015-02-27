The following built-in functions can be used for string manipulation:

<table>
<tr><th>Function</th><th>Description</th><th>Example</th></tr>

<tr><td>md5(string)</td><td>Returns the MD5 hash of a string, f.e. to be used with Gravatar</td><td><code>${md5(user.eMail)}</code></td></tr>
<tr><td>upper(string)</td><td>Converts the string to upper case</td><td><code>${upper(page.name)}</code></td></tr>
<tr><td>lower(string)</td><td>Converts the string to lower case</td><td><code>${lower(page.name)}</code></td></tr>
<tr><td>join(collection, separator)</td><td>Joins all the elements of the given collection into a single string using the given separator</td><td><code>${join(this.children, ',')}</code></td></tr>
<tr><td>concat(values...)</td><td>Concatenates all the values together into a single string</td><td><code>${concat('Hello ', user.name)}</code></td></tr>
<tr><td>split(string [, separatorExpression])</td><td>Splits the given string into a collection of strings using either the default separator expression <code>[,;]+</code> or a given expression</td><td><code>${split('Comma,separated,collection')} => ['Comma', 'separated', 'collection']</code></td></tr>
<tr><td>abbr(longString, maxLength)</td><td>Abbreviates the given string at word boundaries and appends ellipsis</td><td><code>${abbr('Long text', 7)} => Long…</code></td></tr>
<tr><td>capitalize(string)</td><td>Capitalizes the given string</td><td><code>${capitalize('abc')} => Abc</code></td></tr>
<tr><td>titleize(string, separator)</td><td>Capitalizes each word of the given string, using the given separator as the word boundary</td><td><code>${titleize('my-property-name', '-')} => My Property Name</code></td></tr>
<tr><td>index_of(string, substring)</td><td>Returns the position of the given substring in the given string, returns -1 if the string does not contain the substring</td><td><code>${index_of('This is a test', 'test')} => 10</code></td></tr>
<tr><td>contains(string, substring)</td><td>Returns true if the given string contains the given substring, false otherwise</td><td><code>${contains('test', 'e')} => true</code></td></tr>
<tr><td>substring(string, start [, length])</td><td>Returns a substring of the given string, starting at <i>start</i>, with an optional length argument</td><td><code>${substring('testest', 2)} => 'stest'</code><br/><code>${substring('testest', 2, 2)} => 'st'</code></td></tr>
<tr><td>length(string)</td><td>Returns the length of the given string</td><td><code>${length('hello world')} => 11</code></td></tr>
<tr><td>replace(templateExpression, sourceEntity)</td><td>Replaces the template expressions in the given string with values from the given source entity</td><td><code>${replace('${this.name}', this)}</code></td></tr>
<tr><td>clean(string)</td><td>Normalizes the given string and removes all special characters, whitespace etc. so it can be used as an article name f.e..</td><td><code>${clean('this is a test äöüß 123 ""//<>#+*foo- -bar')} => this-is-a-test-aou-123---foo-bar</code></td></tr>
<tr><td>urlencode(string)</td><td>URL encodes the given string</td><td><code>${urlencode('[1 TO 2]')} => %5B1%20TO%202%5D</code></td></tr>
<tr><td>escape_javascript(string)</td><td>Escapes the given string so that it can be safely used in JavaScript</td><td><code>${escape_javascript('A"B')} => A\"B</code></td></tr>
<tr><td>date_format(value, pattern)</td><td>Formats the given date value using the given date pattern</td><td><code>	${date_format('Tue Feb 26 10:49:26 CET 2013', 'yyyyMMdd')} => 20130226</code></td></tr>
<tr><td>parse_date(string, pattern)</td><td>Parses the given date string using the given pattern</td><td><code>${parse_date('20140101', 'yyyyMMdd')}</code></td></tr>
<tr><td>number_format(value, ISO639LangCode, pattern)</td><td>Formats the given number value using the given language code and pattern</td><td><code>${number_format(12345.6789, 'en', '#,##0.00')} => 12,350.00</code></td></tr>
<tr><td>template(templateName, locale, sourceEntity)</td><td>Fetches a MailTemplate entity with the given templateName and locale and fills it with the values from the given sourceEntity, returning the resulting text</td><td><code>${template('WELCOME_MAIL_SUBJECT', 'de', user)}</code></td></tr>
<tr><td>random(length)</td><td>Returns a random alphanumeric string with the given length</td><td><code>${random(5)} => 'dag3z'</code></td></tr>

</table>
