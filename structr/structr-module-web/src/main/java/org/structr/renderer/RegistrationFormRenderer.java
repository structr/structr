package org.structr.renderer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.web.RegistrationForm;
import org.structr.core.entity.web.WebNode;

/**
 *
 * @author Christian Morgner
 */
public class RegistrationFormRenderer extends FormRenderer implements NodeRenderer<RegistrationForm>
{
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

	@Override
	public void renderNode(StructrOutputStream out, RegistrationForm currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		HttpServletRequest request = out.getRequest();

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
			//out.append("<div class=\"okMsg\">").append("Your are logged in as ").append(usernameFromSession).append(".</div>");
			return;
		}

		Boolean sessionBlocked = (Boolean)session.getAttribute(WebNode.Key.sessionBlocked.name());

		if(Boolean.TRUE.equals(sessionBlocked))
		{
			out.append("<div class=\"errorMsg\">").append("Too many login attempts, session is blocked for login").append("</div>");
			return;
		}

		String confirmationKeyFieldName = currentNode.getConfirmationKeyFieldName() != null ? currentNode.getConfirmationKeyFieldName() : defaultConfirmationKeyFieldName;
		String confirmationKey = StringUtils.trimToEmpty(request.getParameter(confirmationKeyFieldName));

		if(StringUtils.isNotEmpty(confirmationKey))
		{
			// Don't show form if confirmation key is set
			return;
		}

		// Get values from config page, or defaults
		String action = currentNode.getAction() != null ? currentNode.getAction() : defaultAction;
		String submitButtonName = currentNode.getSubmitButtonName() != null ? currentNode.getSubmitButtonName() : defaultSubmitButtonName;
		String antiRobotFieldName = currentNode.getAntiRobotFieldName() != null ? currentNode.getAntiRobotFieldName() : defaultAntiRobotFieldName;
		String usernameFieldName = currentNode.getUsernameFieldName() != null ? currentNode.getUsernameFieldName() : defaultUsernameFieldName;
		String passwordFieldName = currentNode.getPasswordFieldName() != null ? currentNode.getPasswordFieldName() : defaultPasswordFieldName;
		String firstNameFieldName = currentNode.getFirstNameFieldName() != null ? currentNode.getFirstNameFieldName() : defaultFirstNameFieldName;
		String lastNameFieldName = currentNode.getLastNameFieldName() != null ? currentNode.getLastNameFieldName() : defaultLastNameFieldName;
		String emailFieldName = currentNode.getEmailFieldName() != null ? currentNode.getEmailFieldName() : defaultEmailFieldName;
		String streetFieldName = currentNode.getStreetFieldName() != null ? currentNode.getStreetFieldName() : defaultStreetFieldName;
		String zipCodeFieldName = currentNode.getZipCodeFieldName() != null ? currentNode.getZipCodeFieldName() : defaultZipCodeFieldName;
		String cityFieldName = currentNode.getCityFieldName() != null ? currentNode.getCityFieldName() : defaultCityFieldName;
		String stateFieldName = currentNode.getStateFieldName() != null ? currentNode.getStateFieldName() : defaultStateFieldName;
		String countryFieldName = currentNode.getCountryFieldName() != null ? currentNode.getCountryFieldName() : defaultCountryFieldName;
		String birthdayFieldName = currentNode.getBirthdayFieldName() != null ? currentNode.getBirthdayFieldName() : defaultBirthdayFieldName;
		String genderFieldName = currentNode.getGenderFieldName() != null ? currentNode.getGenderFieldName() : defaultGenderFieldName;
		String confirmEmailFieldName = currentNode.getConfirmEmailFieldName() != null ? currentNode.getConfirmEmailFieldName() : defaultConfirmEmailFieldName;
		String confirmPasswordFieldName = currentNode.getConfirmPasswordFieldName() != null ? currentNode.getConfirmPasswordFieldName() : defaultConfirmPasswordFieldName;
		String cssClass = currentNode.getCssClass() != null ? currentNode.getCssClass() : defaultCssClass;
		String label = currentNode.getLabel() != null ? currentNode.getLabel() : defaultLabel;
		String agreedToTermsOfUseFieldName = currentNode.getAgreedToTermsOfUseFieldName() != null ? currentNode.getAgreedToTermsOfUseFieldName() : defaultAgreedToTermsOfUseFieldName;
		String newsletterFieldName = currentNode.getNewsletterFieldName() != null ? currentNode.getNewsletterFieldName() : defaultNewsletterFieldName;

		String username = StringUtils.trimToEmpty(request.getParameter(usernameFieldName));
		String password = StringUtils.trimToEmpty(request.getParameter(passwordFieldName));
		String confirmPassword = StringUtils.trimToEmpty(request.getParameter(confirmPasswordFieldName));
		String firstName = StringUtils.trimToEmpty(request.getParameter(firstNameFieldName));
		String lastName = StringUtils.trimToEmpty(request.getParameter(lastNameFieldName));
		String email = StringUtils.trimToEmpty(request.getParameter(emailFieldName));
		String confirmEmail = StringUtils.trimToEmpty(request.getParameter(confirmEmailFieldName));
		String zipCode = StringUtils.trimToEmpty(request.getParameter(zipCodeFieldName));
		String street = StringUtils.trimToEmpty(request.getParameter(streetFieldName));
		String city = StringUtils.trimToEmpty(request.getParameter(cityFieldName));
		String state = StringUtils.trimToEmpty(request.getParameter(stateFieldName));
		String country = StringUtils.trimToEmpty(request.getParameter(countryFieldName));
		String birthday = StringUtils.trimToEmpty(request.getParameter(birthdayFieldName));
		String gender = StringUtils.trimToEmpty(request.getParameter(genderFieldName));
		String agreedToTermsOfUse = StringUtils.isNotEmpty(request.getParameter(agreedToTermsOfUseFieldName)) ? "checked=\"checked\"" : "";
		String newsletter = StringUtils.isNotEmpty(request.getParameter(newsletterFieldName)) ? "checked=\"checked\"" : "";


		out.append("<form name='").append(currentNode.getName()).append("' action='").append(action).append("' method='post'>");
		out.append("<input type='hidden' name='").append(antiRobotFieldName).append("' value=''>");
		out.append("<table class='").append(cssClass).append("'>");
		//out.append("<tr><th><span class='heading'>").append(label).append("</span></th><th></th></tr>");
		out.append("<tr><td class='label'>Username*</td><td></td></tr>");
		out.append("<tr><td class='field'><input type='text' name='").append(usernameFieldName).append("' value='").append(username).append("' size='30'></td></tr>");
		out.append("<tr><td class='label'>Password*</td><td class='label'>Confirm Password*</td></tr>");
		out.append("<tr>");
		out.append("<td class='field'><input type='password' name='").append(passwordFieldName).append("' value='").append(password).append("' size='30'></td>");
		out.append("<td class='field'><input type='password' name='").append(confirmPasswordFieldName).append("' value='").append(confirmPassword).append("' size='30'></td>");
		out.append("</tr>");
		out.append("<tr><td class='label'>First Name*</td><td class='label'>Last Name*</td></tr>");
		out.append("<tr>");
		out.append("<td class='field'><input type='text' name='").append(firstNameFieldName).append("' value='").append(firstName).append("' size='30'></td>");
		out.append("<td class='field'><input type='text' name='").append(lastNameFieldName).append("' value='").append(lastName).append("' size='30'></td>");
		out.append("</tr>");
		out.append("<tr><td class='label' colspan='2'>Email*</td></tr>");
		out.append("<tr><td class='field twoColumns' colspan='2'><input type='text' name='").append(emailFieldName).append("' value='").append(email).append("' size='60'></td></tr>");
		out.append("<tr><td class='label' colspan='2'>Confirm Email*</td></tr>");
		out.append("<tr><td class='field twoColumns' colspan='2'><input type='text' name='").append(confirmEmailFieldName).append("' value='").append(confirmEmail).append("' size='60'></td></tr>");
		out.append("<tr><td class='label' colspan='2'>Street</td></tr>");
		out.append("<tr><td class='field twoColumns' colspan='2'><input type='text' name='").append(streetFieldName).append("' value='").append(street).append("' size='60'></td></tr>");
		out.append("<tr><td class='label'>ZIP / Postal Code</td><td class='label'>City</td></tr>");
		out.append("<tr>");
		out.append("<td class='field'><input type='text' name='").append(zipCodeFieldName).append("' value='").append(zipCode).append("' size='30'></td>");
		out.append("<td class='field'><input type='text' name='").append(cityFieldName).append("' value='").append(city).append("' size='30'></td>");
		out.append("</tr>");
		out.append("<tr><td class='label'>State</td><td class='label'>Country</td></tr>");
		out.append("<tr>");
		out.append("<td class='field'><input type='text' name='").append(stateFieldName).append("' value='").append(state).append("' size='30'></td>");
		out.append("<td class='field'><input type='text' name='").append(countryFieldName).append("' value='").append(country).append("' size='30'></td>");
		out.append("</tr>");
		out.append("<tr><td class='label'><table><tr><td style='text-align: left; width: 50%;'>Birthday</td><td style='width: 50%; text-align: right'>MM/DD/YYYY</td></tr></table></td><td class='label'>Gender</td></tr>");
		out.append("<tr>");
		out.append("<td class='field'><input type='text' name='").append(birthdayFieldName).append("' value='").append(birthday).append("' size='30'></td>");
		out.append("<td class='field'><select name='").append(genderFieldName).append("'>");
		out.append("<option value=''").append(">-- Please select --</option>");
		out.append("<option value='w'").append("w".equals(gender) ? "selected" : "").append(">Female</option>");
		out.append("<option value='m'").append("m".equals(gender) ? "selected" : "").append(">Male</option>");
		out.append("</select></td>");
		out.append("</tr>");
		out.append("<tr><td class='button' colspan='2'><input type='submit' name='").append(submitButtonName).append("' value='Submit'></td></tr>");
		out.append("<tr><td class='field' colspan='2'><input type='checkbox' name='").append(newsletterFieldName).append("' ").append(newsletter).append(">I would like to receive a newsletter</td></tr>");
		out.append("<tr><td class='field' colspan='2'><input type='checkbox' name='").append(agreedToTermsOfUseFieldName).append("' ").append(agreedToTermsOfUse).append(">I agree to the terms of use</td></tr>");
		out.append("</table>");
		out.append("<p>* must be filled out.</p>");
		out.append("</form>");
	}

	@Override
	public String getContentType(RegistrationForm currentNode)
	{
		return ("text/html");
	}
}
