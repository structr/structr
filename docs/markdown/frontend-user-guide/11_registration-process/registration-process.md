# Registration (Sign-up) E-Mails

Structr has an integrated user self-registration process, allowing new users to sign-up with their e-mail address. It has three steps:

## Initial registration request

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


## Confirmation e-mail

When receiving the request, Structr creates a new User object and sends out an e-mail to the given address, containing a confirmation link.

The following defaults for the confirmation e-mail are builtin to Structr, but can all be overridden using the Mail Template system.


| Description                    | Key (set as 'name' attribute) | Default Value                                                                                 |
|--------------------------------|-------------------------------|-----------------------------------------------------------------------------------------------|
| Sender's e-mail address (from) | SENDER_ADDRESS                | structr-mail-daemon@localhost                                                                 |
| Sender's name (from)           | SENDER_NAME                   | Structr Mail Daemon                                                                           |
| Plain text e-mail body         | TEXT_BODY                     | Go to ${link} to finalize registration.                                                       |
| HTML e-mail body               | HTML_BODY                     | &lt;div&gt;Click &lt;a href='$\{link\}'&gt;here&lt;/a&gt; to finalize registration.&lt;/div&gt; |
| E-mail subject                 | SUBJECT                       | Welcome to Structr, please finalize registration                                              |
| Confirmation link base URL     | BASE_URL                      | http://" + appHost + ":" + httpPort                                                           |
| Confirmation link target page  | CONFIRM_REGISTRATION_PAGE     | /confirm_registration                                                                         |
| Key for confirmation parameter | CONFIRM_KEY_KEY               | key                                                                                           |
| Key for success page           | TARGET_PAGE_KEY               | target                                                                                        |
| Key for error page             | ERROR_PAGE_KEY                | onerror                                                                                       |
| Success page                   | TARGET_PAGE                   | register_thanks                                                                               |
| Error page                     | ERROR_PAGE                    | register_error                                                                                |

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

## User confirmation

The receipient of the confirmation e-mail then has to click on the link or enter the URL into the browser's address field. If the confirmation key is correct, the user will be automatically confirmed and logged in. You can redirect users to their profile page where they can set a password.

<p class="warning">The configuration parameters JsonRestServlet.user.autologin and HtmlServlet.user.autologin have to be set to true to make the auto-login feature work.</p> 
