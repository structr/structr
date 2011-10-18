package org.structr.renderer;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Folder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jsoup.Jsoup;
import org.structr.common.CurrentRequest;
import org.structr.common.MailHelper;
import org.structr.common.RelType;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.Command;
import org.structr.core.NodeRenderer;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Person;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.entity.web.RegistrationCheck;
import org.structr.core.entity.web.WebNode;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindUserCommand;
import org.structr.core.node.IndexNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class RegistrationCheckRenderer implements NodeRenderer<RegistrationCheck>
{
	private static final Logger logger = Logger.getLogger(RegistrationCheckRenderer.class.getName());

	private final static String HTML_HEADER = "<html>";
	private final static String HTML_FOOTER = "</html>";
	private final static String NUMBER_OF_REGISTRATION_ATTEMPTS = "numberOfRegistrationAttempts";
	private final static String REGISTRATION_FAILURE_USER_EXISTS = "A user with this username already exists, please choose another username!";
	private final static String REGISTRATION_FAILURE_PASSWORDS_DONT_MATCH = "The passwords don't match!";
	private final static String REGISTRATION_FAILURE_EMAILS_DONT_MATCH = "The email adresses don't match!";
	private final static String REGISTRATION_FAILURE_MANDATORY_FIELD_EMPTY = "Please fill out all mandatory fields!";
	private final static String REGISTRATION_FAILURE_NOT_AGREED_TO_TERMS_OF_USE = "To register, you have to agree to the terms of use!";
	private final static String REGISTRATION_FAILURE_WEAK_PASSWORD = "Your password is too weak, it must have at least 6 characters, contain at least one digit, one upper case and one lower case character.";
	private final static String REGISTRATION_FAILURE_INVALID_DATE = "Invalid date format";
	
	public final static String defaultSubmitButtonName = "regForm_submit";
	public final static String defaultAntiRobotFieldName = "regForm_antiRobot";
	public final static String defaultUsernameFieldName = "regForm_username";
	public final static String defaultPasswordFieldName = "regForm_password";
	public final static String defaultAssignedUsername = "admin";
	public final static String defaultInlineCss = "<style type=\"text/css\">body { font-family: sans-serif; font-size: 12px; }</style>";
	public final static String defaultPublicUserDirectoryName = "Public Users";
	public final static String defaultSenderAddress = "registration@structr.org";
	public final static String defaultSenderName = "structr Registration Robot";
	public final static String defaultConfirmationKeyFieldName = "confirmationKey";
	public final static String defaultConfirmPasswordFieldName = "regForm_confirmPassword";
	public final static String defaultFirstNameFieldName = "regForm_firstName";
	public final static String defaultLastNameFieldName = "regForm_lastName";
	public final static String defaultEmailFieldName = "regForm_email";
	public final static String defaultConfirmEmailFieldName = "regForm_confirmEmail";
	public final static String defaultZipCodeFieldName = "regForm_zipCode";
	public final static String defaultCityFieldName = "regForm_city";
	public final static String defaultStreetFieldName = "regForm_street";
	public final static String defaultStateFieldName = "regForm_state";
	public final static String defaultCountryFieldName = "regForm_country";
	public final static String defaultBirthdayFieldName = "regForm_birthday";
	public final static String defaultGenderFieldName = "regForm_gender";
	public final static String defaultAgreedToTermsOfUseFieldName = "regForm_agreedToTermsOfUse";
	public final static String defaultNewsletterFieldName = "regForm_newsletter";

	private List<String> errors = new LinkedList<String>();

	@Override
	public void renderNode(StructrOutputStream out, RegistrationCheck currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		HttpServletRequest request = CurrentRequest.getRequest();

		if(request == null)
		{
			return;
		}

		HttpSession session = request.getSession();

		if(session == null)
		{
			return;
		}

		String usernameFromSession = (String)session.getAttribute(WebNode.Key.username.name());
//            String usernameFromSession = CurrentSession.getGlobalUsername();
		Boolean alreadyLoggedIn = usernameFromSession != null;

		if(alreadyLoggedIn)
		{
			return;
		}

		Boolean sessionBlocked = (Boolean)session.getAttribute(WebNode.Key.sessionBlocked.name());

		if(Boolean.TRUE.equals(sessionBlocked))
		{
			out.append("<div class=\"errorMsg\">").append("Unable to register. Session is blocked.").append("</div>");
			return;
		}

		// Get values from config page, or defaults

		String usernameFieldName = currentNode.getUsernameFieldName() != null ? currentNode.getUsernameFieldName() : defaultUsernameFieldName;
		String passwordFieldName = currentNode.getPasswordFieldName() != null ? currentNode.getPasswordFieldName() : defaultPasswordFieldName;
		String confirmPasswordFieldName = currentNode.getConfirmPasswordFieldName() != null ? currentNode.getConfirmPasswordFieldName() : defaultConfirmPasswordFieldName;
		String firstNameFieldName = currentNode.getFirstNameFieldName() != null ? currentNode.getFirstNameFieldName() : defaultFirstNameFieldName;
		String lastNameFieldName = currentNode.getLastNameFieldName() != null ? currentNode.getLastNameFieldName() : defaultLastNameFieldName;
		String emailFieldName = currentNode.getEmailFieldName() != null ? currentNode.getEmailFieldName() : defaultEmailFieldName;
		String confirmEmailFieldName = currentNode.getConfirmEmailFieldName() != null ? currentNode.getConfirmEmailFieldName() : defaultConfirmEmailFieldName;
		String streetFieldName = currentNode.getStreetFieldName() != null ? currentNode.getStreetFieldName() : defaultStreetFieldName;
		String zipCodeFieldName = currentNode.getZipCodeFieldName() != null ? currentNode.getZipCodeFieldName() : defaultZipCodeFieldName;
		String cityFieldName = currentNode.getCityFieldName() != null ? currentNode.getCityFieldName() : defaultCityFieldName;
		String countryFieldName = currentNode.getCountryFieldName() != null ? currentNode.getCountryFieldName() : defaultCountryFieldName;
		String stateFieldName = currentNode.getStateFieldName() != null ? currentNode.getStateFieldName() : defaultStateFieldName;
		String birthdayFieldName = currentNode.getBirthdayFieldName() != null ? currentNode.getBirthdayFieldName() : defaultBirthdayFieldName;
		String genderFieldName = currentNode.getGenderFieldName() != null ? currentNode.getGenderFieldName() : defaultGenderFieldName;

		String agreedToTermsOfUseFieldName = currentNode.getAgreedToTermsOfUseFieldName() != null ? currentNode.getAgreedToTermsOfUseFieldName() : defaultAgreedToTermsOfUseFieldName;
		String newsletterFieldName = currentNode.getNewsletterFieldName() != null ? currentNode.getNewsletterFieldName() : defaultNewsletterFieldName;
		String confirmationKeyFieldName = currentNode.getConfirmationKeyFieldName() != null ? currentNode.getConfirmationKeyFieldName() : defaultConfirmationKeyFieldName;
		String submitButtonName = currentNode.getSubmitButtonName() != null ? currentNode.getSubmitButtonName() : defaultSubmitButtonName;
		String antiRobotFieldName = currentNode.getAntiRobotFieldName() != null ? currentNode.getAntiRobotFieldName() : defaultAntiRobotFieldName;

		final String publicUserDirectoryName = currentNode.getPublicUserDirectoryName() != null ? currentNode.getPublicUserDirectoryName() : defaultPublicUserDirectoryName;
		String senderAddress = currentNode.getSenderAddress() != null ? currentNode.getSenderAddress() : defaultSenderAddress;
		String senderName = currentNode.getSenderName() != null ? currentNode.getSenderName() : defaultSenderName;
		String inlineCss = currentNode.getInlineCss() != null ? currentNode.getInlineCss() : defaultInlineCss;
		final String assignedUsername = currentNode.getAssignedUsername() != null ? currentNode.getAssignedUsername() : defaultAssignedUsername;

		final String username = request.getParameter(usernameFieldName);
		final String password = request.getParameter(passwordFieldName);
		String confirmPassword = request.getParameter(confirmPasswordFieldName);
		final String firstName = request.getParameter(firstNameFieldName);
		final String lastName = request.getParameter(lastNameFieldName);
		final String email = request.getParameter(emailFieldName);
		String confirmEmail = request.getParameter(confirmEmailFieldName);
		final String street = request.getParameter(streetFieldName);
		final String zipCode = request.getParameter(zipCodeFieldName);
		final String city = request.getParameter(cityFieldName);
		final String state = request.getParameter(stateFieldName);
		final String country = request.getParameter(countryFieldName);
		final String birthday = request.getParameter(birthdayFieldName);
		final String gender = request.getParameter(genderFieldName);
		String agreedToTermsOfUse = request.getParameter(agreedToTermsOfUseFieldName);
		final String newsletter = request.getParameter(newsletterFieldName);
		String confirmationKey = request.getParameter(confirmationKeyFieldName);
		String submitButton = request.getParameter(submitButtonName);
		String antiRobot = request.getParameter(antiRobotFieldName);

		int maxRetries = currentNode.getMaxErrors() > 0 ? currentNode.getMaxErrors() : LoginCheckRenderer.defaultMaxErrors;
		int delayThreshold = currentNode.getDelayThreshold() > 0 ? currentNode.getDelayThreshold() : LoginCheckRenderer.defaultDelayThreshold;
		int delayTime = currentNode.getDelayTime() > 0 ? currentNode.getDelayTime() : LoginCheckRenderer.defaultDelayTime;

		if(StringUtils.isNotEmpty(antiRobot))
		{
			// Don't process form if someone has filled the anti-robot field
			return;
		}

		// Check if we have a user with this name
		User loginUser = null;

		// Check confirmation key
		if(StringUtils.isNotEmpty(confirmationKey) && StringUtils.isNotEmpty(username))
		{

			loginUser = (User)Services.command(FindUserCommand.class).execute(username);

			if(loginUser == null)
			{
				String message = "<div class=\"errorMsg\">Missing username or user not found!</div>";
				registerFailure(out, message, session, maxRetries, delayThreshold, delayTime);
			}

			if(hasFailures(out))
			{
				return;
			}

			String confirmationKeyFromUser = loginUser.getConfirmationKey();
			if(StringUtils.isNotBlank(confirmationKeyFromUser) && confirmationKeyFromUser.equals(confirmationKey))
			{

				loginUser.setBlocked(false);
				loginUser.setConfirmationKey(null);
				out.append("<div class=\"okMsg\">").append("Registration complete, you may now login.").append("</div>");
				return;
			}


		}

		if(StringUtils.isEmpty(submitButton))
		{
			// Don't process form if submit button was not pressed
			return;
		}

//            if (StringUtils.isEmpty(username)) {
//                String message = "<div class=\"errorMsg\">Plesae choose a username!</div>";
//                registerFailure(out, message, session, maxRetries, delayThreshold, delayTime);
//            } else {
//                loginUser = (User) Services.command(FindUserCommand.class).execute(username);
//            }

		if(StringUtils.isNotEmpty(username))
		{
			loginUser = (User)Services.command(FindUserCommand.class).execute(username);
		}

		if(loginUser != null)
		{
			logger.log(Level.INFO, "User with name {0} already exists", loginUser);
			String message = "<div class=\"errorMsg\">" + REGISTRATION_FAILURE_USER_EXISTS + "</div>";
			registerFailure(out, message, session, maxRetries, delayThreshold, delayTime);
		}

		if(hasFailures(out))
		{
			return;
		}

		// Check mandatory fields

		boolean usernameEmpty = StringUtils.isEmpty(username);
		boolean passwordEmpty = StringUtils.isEmpty(password);
		boolean confirmPasswordEmpty = StringUtils.isEmpty(confirmPassword);
		boolean firstNameEmpty = StringUtils.isEmpty(firstName);
		boolean lastNameEmpty = StringUtils.isEmpty(lastName);
		boolean emailEmpty = StringUtils.isEmpty(email);
		boolean confirmEmailEmpty = StringUtils.isEmpty(confirmEmail);

		if(firstNameEmpty || lastNameEmpty || emailEmpty || confirmEmailEmpty)
		{

			StringBuilder errorsOnMandatoryFields = new StringBuilder();

			errorsOnMandatoryFields.append("<div class=\"errorMsg\">").append(REGISTRATION_FAILURE_MANDATORY_FIELD_EMPTY).append("</div>");
			errorsOnMandatoryFields.append("<style type=\"text/css\">");

			if(usernameEmpty)
			{
				errorsOnMandatoryFields.append("input[name=").append(usernameFieldName).append("] { background-color: #ffc }");
			} else
			{
				loginUser = (User)Services.command(FindUserCommand.class).execute(username);

				if(loginUser != null)
				{
					logger.log(Level.FINE, "User with name {0} already exists", loginUser);
					String message = "<div class=\"errorMsg\">" + REGISTRATION_FAILURE_USER_EXISTS + "</div>";
					registerFailure(out, message, session, maxRetries, delayThreshold, delayTime);
				}

			}
			if(passwordEmpty)
			{
				errorsOnMandatoryFields.append("input[name=").append(passwordFieldName).append("] { background-color: #ffc }");
			}
			if(confirmPasswordEmpty)
			{
				errorsOnMandatoryFields.append("input[name=").append(confirmPasswordFieldName).append("] { background-color: #ffc }");
			}
			if(firstNameEmpty)
			{
				errorsOnMandatoryFields.append("input[name=").append(firstNameFieldName).append("] { background-color: #ffc }");
			}
			if(firstNameEmpty)
			{
				errorsOnMandatoryFields.append("input[name=").append(firstNameFieldName).append("] { background-color: #ffc }");
			}
			if(lastNameEmpty)
			{
				errorsOnMandatoryFields.append("input[name=").append(lastNameFieldName).append("] { background-color: #ffc }");
			}
			if(emailEmpty)
			{
				errorsOnMandatoryFields.append("input[name=").append(emailFieldName).append("] { background-color: #ffc }");
			}
			if(confirmEmailEmpty)
			{
				errorsOnMandatoryFields.append("input[name=").append(confirmEmailFieldName).append("] { background-color: #ffc }");
			}

			errorsOnMandatoryFields.append("</style>");

			registerFailure(out, errorsOnMandatoryFields.toString(), session, maxRetries, delayThreshold, delayTime);
		}

		if(hasFailures(out))
		{
			return;
		}

		if(!(password.equals(confirmPassword)))
		{
			String message = "<div class=\"errorMsg\">" + REGISTRATION_FAILURE_PASSWORDS_DONT_MATCH + "</div>";
			registerFailure(out, message, session, maxRetries, delayThreshold, delayTime);
		}

		Date parsedDate = null;
		String parseErrorMsg = null;
		try
		{
			parsedDate = DateUtils.parseDate(birthday, new String[]
				{
					"MM/dd/yyyy"
				});
		} catch(Exception e)
		{
			parseErrorMsg = e.getLocalizedMessage();
		}

		if(parsedDate == null)
		{
			String message = "<div class=\"errorMsg\">" + REGISTRATION_FAILURE_INVALID_DATE + ": " + parseErrorMsg + "</div>";
			registerFailure(out, message, session, maxRetries, delayThreshold, delayTime);
		}

		// Check that password is strong enough:
		// Must contain at least one digit, one upper and one lower case character.
		// Length must be greater than 6.

		String pattern = "((?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{6,})";

		if(!(password.matches(pattern)))
		{
			String message = "<div class=\"errorMsg\">" + REGISTRATION_FAILURE_WEAK_PASSWORD + "</div>";
			registerFailure(out, message, session, maxRetries, delayThreshold, delayTime);
		}

		if(!(email.equals(confirmEmail)))
		{
			String message = "<div class=\"errorMsg\">" + REGISTRATION_FAILURE_EMAILS_DONT_MATCH + "</div>";
			registerFailure(out, message, session, maxRetries, delayThreshold, delayTime);
		}

		// Here we have all mandatory fields

		// Check that user has agreed to the terms of use
		if(StringUtils.isEmpty(agreedToTermsOfUse))
		{
			String message = "<div class=\"errorMsg\">" + REGISTRATION_FAILURE_NOT_AGREED_TO_TERMS_OF_USE + "</div>";
			registerFailure(out, message, session, maxRetries, delayThreshold, delayTime);
		}


		// Check e-mail by sending a confirmation key to the given e-mail address

//            if (user.length() > 0 && password.length() > 0) {
//                email.setAuthentication(user, password);
//            }
		StringBuilder content = new StringBuilder(HTML_HEADER);

//            content.append("<head>").append(inlineCss).append("</head><body>");
		content.append(inlineCss);

		content.append("<p>An account was requested with the following personal data:</p>");

		content.append("<table>");

		if(StringUtils.isNotEmpty(username))
		{
			content.append("<tr><td>Username:</td><td>").append(username).append("</td></tr>");
		}
		if(StringUtils.isNotEmpty(firstName))
		{
			content.append("<tr><td>First Name:</td><td>").append(firstName).append("</td></tr>");
		}
		if(StringUtils.isNotEmpty(lastName))
		{
			content.append("<tr><td>Last Name:</td><td>").append(lastName).append("</td></tr>");
		}
		if(StringUtils.isNotEmpty(email))
		{
			content.append("<tr><td>E-Mail Address:</td><td>").append(email).append("</td></tr>");
		}
		if(StringUtils.isNotEmpty(street))
		{
			content.append("<tr><td>Street:</td><td>").append(street).append("</td></tr>");
		}
		if(StringUtils.isNotEmpty(zipCode))
		{
			content.append("<tr><td>ZIP Code:</td><td>").append(zipCode).append("</td></tr>");
		}
		if(StringUtils.isNotEmpty(city))
		{
			content.append("<tr><td>City:</td><td>").append(city).append("</td></tr>");
		}
		if(StringUtils.isNotEmpty(country))
		{
			content.append("<tr><td>Country:</td><td>").append(country).append("</td></tr>");
		}
		if(StringUtils.isNotEmpty(newsletter))
		{
			content.append("<tr><td>Newsletter:</td><td>yes").append("</td></tr>");
		}
		content.append("</table>");

		if(hasFailures(out))
		{
			return;
		}

		// Create hash digest from user name as confirmation key
		final String confirmationKeyForMail = DigestUtils.sha256Hex(username);

//            content.append("<p>Your confirmation key is ").append(confirmationKeyForMail).append("</p>");
//            content.append("<p>Please click on the link below to complete your registration.</p>");
		content.append("<p><a href=\"");

		content.append(request.getScheme()).append("://").append(request.getServerName()).append(":").append(request.getServerPort());

		String pageUrl = request.getContextPath().concat(
			"/view".concat(startNode.getNodePath().replace("&", "%26"))).concat("?").concat(confirmationKeyFieldName).concat("=").concat(confirmationKeyForMail).concat("&").concat(usernameFieldName).concat("=").concat(username);

		content.append(pageUrl);
		content.append("\">");
		content.append("Please click on this link to complete your registration.");
		content.append("</a></p>");

		content.append("<p>If you think that this registration request was not legitimate, please forward this message to ").append(senderAddress).append(".</p>");
		content.append("<p>If something is not working as expected, please forward this message to ").append(senderAddress).append(", together with a short description of the issue you had. Thank you.</p>");

//            content.append("</body>");

		content.append(HTML_FOOTER);

		String to = email;
		String toName = firstName + " " + lastName;
		String from = senderAddress;
		String fromName = senderName;
		String subject = "Account requested, please confirm your e-mail address";
		String htmlContent = content.toString();
		String textContent = Jsoup.parse(htmlContent).text();

		try
		{

			MailHelper.sendHtmlMail(from, fromName, to, toName, null, null, senderAddress, subject, htmlContent, textContent);

		} catch(Exception e)
		{
			logger.log(Level.SEVERE, "Error while sending registration e-mail", e);
			String message = "<div class=\"errorMsg\">Error while sending registration e-mail: " + e.getLocalizedMessage() + "</div>";
			registerFailure(out, message, session, maxRetries, delayThreshold, delayTime);
		}

		if(hasFailures(out))
		{
			return;
		}

		final RegistrationCheck registrationCheckNode = currentNode;
		final Date birthdayDate = parsedDate;

		// Create new user (will reserve username)
		User newUser = (User)Services.command(TransactionCommand.class).execute(new StructrTransaction()
		{
			@Override
			public Object execute()
			{

				Command create = Services.command(CreateNodeCommand.class);
				Command link = Services.command(CreateRelationshipCommand.class);
				Command search = Services.command(SearchNodeCommand.class);

				List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

				// Get user to assign the nodes to be created to
				User assignedUser = null;
				searchAttrs.add(Search.andExactName(assignedUsername));
				searchAttrs.add(Search.andExactType(User.class.getSimpleName()));

				List<User> userList = (List<User>)search.execute(new SuperUser(), null, false, false, searchAttrs);
				if(!(userList.isEmpty()))
				{
					assignedUser = userList.get(0);
				}

				// Clear search attributes to be reusable
				searchAttrs.clear();

				User newUser = (User)create.execute(assignedUser,
					new NodeAttribute(AbstractNode.Key.name.name(), username),
					new NodeAttribute(AbstractNode.Key.type.name(), User.class.getSimpleName()),
					new NodeAttribute(Person.Key.firstName.name(), firstName),
					new NodeAttribute(Person.Key.firstName.name(), lastName),
					new NodeAttribute(User.Key.realName.name(), (firstName + " " + lastName)),
					new NodeAttribute(Person.Key.street.name(), street),
					new NodeAttribute(Person.Key.zipCode.name(), zipCode),
					new NodeAttribute(Person.Key.city.name(), city),
					new NodeAttribute(Person.Key.state.name(), state),
					new NodeAttribute(Person.Key.country.name(), country),
					new NodeAttribute(Person.Key.gender.name(), gender),
					new NodeAttribute(Person.Key.birthday.name(), birthdayDate),
					new NodeAttribute(Person.Key.email1.name(), email),
					new NodeAttribute(Person.Key.newsletter.name(), StringUtils.isNotEmpty(newsletter)),
					new NodeAttribute(User.Key.frontendUser.name(), true),
					new NodeAttribute(User.Key.confirmationKey.name(), confirmationKeyForMail));

				// Use method for password to be hashed
				newUser.setPassword(password);

				searchAttrs.add(Search.andExactName(publicUserDirectoryName));
				searchAttrs.add(Search.andExactType(Folder.class.getSimpleName()));

				// Look for existing public user directory
				List<Folder> folders = (List<Folder>)search.execute(
					null, registrationCheckNode, false, false, searchAttrs);

				Folder publicUserDirectory = null;

				if(folders.size() > 0)
				{
					publicUserDirectory = folders.get(0);
				}

				if(assignedUser == null)
				{
					logger.log(Level.WARNING, "No assignable user, all objects created will only be visible to the super user!");
				}

				// If no public user directory exists, create one and link to this node
				if(publicUserDirectory == null)
				{
					publicUserDirectory = (Folder)create.execute(assignedUser,
						new NodeAttribute(AbstractNode.Key.name.name(), publicUserDirectoryName),
						new NodeAttribute(AbstractNode.Key.type.name(), Folder.class.getSimpleName()));

					// Link to registration node
					link.execute(registrationCheckNode, publicUserDirectory, RelType.HAS_CHILD);

				}

				link.execute(publicUserDirectory, newUser, RelType.HAS_CHILD);

				// Index user to be findable
				Services.command(IndexNodeCommand.class).execute(newUser);

				return newUser;
			}
		});

		logger.log(Level.INFO, "A new public user {0}[{1}] has been created.", new Object[]
			{
				newUser.getName(), newUser.getId()
			});

		// Clear all blocking stuff
		session.removeAttribute(WebNode.Key.sessionBlocked.name());
		session.removeAttribute(NUMBER_OF_REGISTRATION_ATTEMPTS);

		out.append("<div class=\"okMsg\">").append("An e-mail has been sent to you to validate the given e-mail address. Please click on the link in the e-mail to complete registration.").append("</div>");
	}

	private boolean hasFailures(final StructrOutputStream out)
	{
		if(!(errors.isEmpty()))
		{
			for(String error : errors)
			{
				out.append(error);
			}
			return true;
		}
		return false;
	}

	private void registerFailure(final StructrOutputStream out, final String errorMsg, final HttpSession session, final int maxRetries, final int delayThreshold, final int delayTime)
	{

		errors.add(errorMsg);

		Integer retries = (Integer)session.getAttribute(NUMBER_OF_REGISTRATION_ATTEMPTS);

		if(retries != null && retries > maxRetries)
		{

			logger.log(Level.SEVERE, "More than {0} registration failures, session {1} is blocked", new Object[]
				{
					maxRetries, session.getId()
				});
			session.setAttribute(WebNode.Key.sessionBlocked.name(), true);
			String message = "<div class=\"errorMsg\">Too many unsuccessful registration attempts, your session is blocked for registration!</div>";
			errors.add(message);

		} else if(retries != null && retries > delayThreshold)
		{

			logger.log(Level.INFO, "More than {0} registration failures, execution delayed by {1} seconds", new Object[]
				{
					delayThreshold, delayTime
				});

			try
			{
				Thread.sleep(delayTime * 1000);
			} catch(InterruptedException ex)
			{
				logger.log(Level.SEVERE, null, ex);
			}

			String message = "<div class=\"errorMsg\">You had more than " + delayThreshold + " unsuccessful registration attempts, execution was delayed by " + delayTime + " seconds.</div>";
			errors.add(message);

		} else if(retries != null)
		{

			session.setAttribute(NUMBER_OF_REGISTRATION_ATTEMPTS, retries + 1);

		} else
		{

			session.setAttribute(NUMBER_OF_REGISTRATION_ATTEMPTS, 1);

		}

	}

	@Override
	public String getContentType(RegistrationCheck currentNode)
	{
		return ("text/html");
	}
}
