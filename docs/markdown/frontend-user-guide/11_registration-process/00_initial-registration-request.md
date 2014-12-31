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
