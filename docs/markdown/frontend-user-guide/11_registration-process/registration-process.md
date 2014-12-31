Structr has an integrated user self-registration process, allowing new users to sign-up with their e-mail address. It has three steps:

#### Initial registration request

Your Structr frontend application has to send a POST request to ``/structr/rest/registration`` with a JSON body containing the single parameter ``eMail`` with the e-mail address of the new user. A typical way is to use a JavaScript AJAX call with jQuery's $.ajax() method:

	$.ajax({
		url: '/structr/rest/registration',
		type: 'POST',
		contentType: 'application/json',
		data: JSON.stringify({
		    eMail: $('#e-mail').val()
		}),
		statusCode: {
		    201: function() {
		      console.log('Registration successful!');
		    }
		}
	});


#### Confirmation e-mail

When receiving the request, Structr creates a new User object and sends out an e-mail to the given address, containing a confirmation link.

The following defaults for the confirmation e-mail are builtin to Structr, but can all be overridden using the Mail Template system.

<table>
<thead>
<tr>
<th>Description</th>
<th>Key (set as 'name' attribute)</th>
<th>Default Value</th>
</tr>
</thead>
<tbody>
<tr>
<td>Sender's e-mail address (from)</td>
<td>SENDER_ADDRESS</td>
<td>structr-mail-daemon@localhost</td>
</tr>
<tr>
<td>Sender's name (from)</td>
<td>SENDER_NAME</td>
<td>Structr Mail Daemon</td>
</tr>
<tr>
<td>Plain text e-mail body</td>
<td>TEXT_BODY</td>
<td>Go to ${link} to finalize registration.</td>
</tr>
<tr>
<td>HTML e-mail body</td>
<td>HTML_BODY</td>
<td>&lt;div&gt;Click &lt;a href='${link}'&gt;here&lt;/a&gt; to finalize registration.&lt;/div&gt;</td>
</tr>
<tr>
<td>E-mail subject</td>
<td>SUBJECT</td>
<td>Welcome to Structr, please finalize registration</td>
</tr>
<tr>
<td>Confirmation link base URL</td>
<td>BASE_URL</td>
<td>http://" + appHost + ":" + httpPort</td>
</tr>
<tr>
<td>Confirmation link target page</td>
<td>CONFIRM_REGISTRATION_PAGE</td>
<td>/confirm_registration</td>
</tr>
<tr>
<td>Key for confirmation parameter</td>
<td>CONFIRM_KEY_KEY</td>
<td>key</td>
</tr>
<tr>
<td>Key for success page</td>
<td>TARGET_PAGE_KEY</td>
<td>target</td>
</tr>
<tr>
<td>Key for error page</td>
<td>ERROR_PAGE_KEY</td>
<td>onerror</td>
</tr>
<tr>
<td>Success page</td>
<td>TARGET_PAGE</td>
<td>register_thanks</td>
</tr>
<tr>
<td>Error page</td>
<td>ERROR_PAGE</td>
<td>register_error</td>
</tr>
</tbody>
</table>


The values for 'appHost' and 'httpPort' are taken from structr.conf.

The placeholder ${link} is replaced by a string composed as described in the following pseudo-code (confirmationKey is a random token, generated on the server):

	BASE_URL + CONFIRM_REGISTRATION_PAGE
		+ "?" + CONFIRM_KEY_KEY + "=" + confirmationKey
		+ "&" + TARGET_PAGE_KEY + "=" + TARGET_PAGE
		+ "&" + ERROR_PAGE_KEY  + "=" + ERROR_PAGE

A typical confirmation link looks like this:

	http://0.0.0.0:8082/confirm_registration?key=33781115-fd5d-4a36-a302-bc822bf131b4&target=register_thanks&onerror=register_error

To change it to the following example

	https://structr.com/confirm_registration?key=33781115-fd5d-4a36-a302-bc822bf131b4&target=success-page&onerror=error-page

you need to create MailTemplate objects with the following 'name' and 'text' attributes:

	BASE_URL                  https://structr.com
	TARGET_PAGE               success-page
	ERROR_PAGE                error-page

To change the subject, from address and name, create MailTemplate objects with the appropriate name and text accordingly.

#### User confirmation

The receipient of the confirmation e-mail then has to click on the link or enter the URL into the browser's address field. If the confirmation key is correct, the user will be automatically confirmed and logged in. You can redirect users to their profile page where they can set a password.

<p class="warning">The configuration parameters JsonRestServlet.user.autologin and HtmlServlet.user.autologin have to be set to true to make the auto-login feature work.</p> 
