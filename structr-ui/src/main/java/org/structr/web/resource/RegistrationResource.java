/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.resource;

import org.structr.common.MailHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.Resource;
import org.structr.web.entity.User;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.lucene.search.BooleanClause.Occur;
import org.parboiled.common.StringUtils;
import org.structr.core.auth.AuthHelper;
import org.structr.core.entity.Person;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.mail.MailTemplate;
import org.structr.web.servlet.HtmlServlet;

//~--- classes ----------------------------------------------------------------

/**
 * A resource to register new users
 *
 * @author Axel Morgner
 */
public class RegistrationResource extends Resource {

	private static final Logger logger = Logger.getLogger(RegistrationResource.class.getName());
	
	private enum TemplateKey {
		SENDER_NAME,
		SENDER_ADDRESS,
		SUBJECT,
		TEXT_BODY,
		HTML_BODY,
		BASE_URL,
		TARGET_PAGE
	}

	private static String localeString;
	private static String confKey;
	
	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		return (getUriPart().equals(part));

	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		if (propertySet.containsKey(User.email.jsonName()) && propertySet.size() == 1) {
			
			SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
			User user;
			
			final String emailString  = (String) propertySet.get(User.email.jsonName());
			
			if (StringUtils.isEmpty(emailString)) {
				return new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);
			}
			
			localeString = (String) propertySet.get(MailTemplate.locale.jsonName());
			confKey = UUID.randomUUID().toString();
			
			Result result = Services.command(superUserContext, SearchNodeCommand.class).execute(
				Search.andExactType(User.class.getSimpleName()),
				Search.andExactProperty(superUserContext, User.email, emailString));
				
			if (!result.isEmpty()) {
				
				user = (User) result.get(0);
				
				// meh..
				final User finalUser = user;
			
				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {
				
						finalUser.setProperty(User.confirmationKey, confKey);
						return null;
					}
				});
				
			} else {

				user = createUser(securityContext, emailString);
			}
			
			if (user != null) {

				if (!sendInvitationLink(user)) {
					
					return new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);
					
				}
			}

		}

		// return 200 OK
		return new RestMethodResult(HttpServletResponse.SC_OK);

	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		return null;

	}

	private boolean sendInvitationLink(final User user) {

		Map<String, String> replacementMap = new HashMap();

		String userEmail = user.getProperty(User.email);
		
		replacementMap.put(toPlaceholder(User.email.jsonName()), userEmail);
		replacementMap.put(toPlaceholder("link"),
			getTemplateText(TemplateKey.BASE_URL, "//" + Services.getApplicationHost() + ":" + Services.getHttpPort())
			+ "/" + HtmlServlet.CONFIRM_REGISTRATION_PAGE
			+ "?" + HtmlServlet.CONFIRM_KEY_KEY + "=" + confKey
			+ "&" + HtmlServlet.TARGET_PAGE_KEY + "=" + getTemplateText(TemplateKey.TARGET_PAGE, "register_thanks"));

		String textMailTemplate = getTemplateText(TemplateKey.TEXT_BODY, "Go to ${link} to finalize registration.");
		String htmlMailTemplate = getTemplateText(TemplateKey.HTML_BODY, "<div>Click <a href='${link}'>here</a> to finalize registration.</div>");
		String textMailContent  = MailHelper.replacePlaceHoldersInTemplate(textMailTemplate, replacementMap);
		String htmlMailContent  = MailHelper.replacePlaceHoldersInTemplate(htmlMailTemplate, replacementMap);

		try {

			MailHelper.sendHtmlMail(
				getTemplateText(TemplateKey.SENDER_ADDRESS, "structr-mail-daemon@localhost"),
				getTemplateText(TemplateKey.SENDER_NAME, "Structr Mail Daemon"),
				userEmail, "", null, null, null,
				getTemplateText(TemplateKey.SUBJECT, "Welcome to Structr, please finalize registration"),
				htmlMailContent, textMailContent);

		} catch (Exception e) {

			logger.log(Level.SEVERE, "Unable to send registration e-mail", e);
			return false;
		}
		
		return true;

	}

	private String getTemplateText(final TemplateKey key, final String defaultValue) {
		try {
			List<MailTemplate> templates = (List<MailTemplate>) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(
				Search.andExactType(MailTemplate.class.getSimpleName()),
				Search.andExactName(key.name()),
				Search.andMatchExactValues(securityContext, MailTemplate.locale, localeString, Occur.MUST)
			).getResults();
			
			if (!templates.isEmpty()) {
				
				return templates.get(0).getProperty(MailTemplate.text).getProperty(Content.content);
				
			} else {
				
				return defaultValue;
				
			}
			
		} catch (FrameworkException ex) {
			
			Logger.getLogger(RegistrationResource.class.getName()).log(Level.WARNING, "Could not get mail template for key " + key, ex);
			
		}
		
		return null;
		
	}
	
	private static String toPlaceholder(final String key) {

		return "${".concat(key).concat("}");

	}
	
	/**
	 * Create a new user
	 * 
	 * If a {@link Person} is found, convert that object to a {@link User} object
	 * 
	 * @param securityContext
	 * @param emailString
	 * @return 
	 */
	public static User createUser(final SecurityContext securityContext, final String emailString) {

		try {
			return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<User>() {

				@Override
				public User execute() throws FrameworkException {

					User user;
					
					// First, search for a person with that e-mail address
					Person person = (Person) AuthHelper.getPrincipalForEmail(emailString);

					if (person != null) {
						
						// convert to user
						person.unlockReadOnlyPropertiesOnce();
						person.setProperty(AbstractNode.type, User.class.getSimpleName());

						user = new User();
						user.init(securityContext, person.getNode());

						// index manually, because type is a system property!
						person.updateInIndex();

						user.setProperty(User.confirmationKey, confKey);

					} else {

						user = (User) Services.command(securityContext, CreateNodeCommand.class).execute(
							new NodeAttribute(AbstractNode.type, User.class.getSimpleName()),
							new NodeAttribute(User.email, emailString),
							new NodeAttribute(User.name, emailString),
							new NodeAttribute(User.confirmationKey, confKey));

					}

					return user;

				}
				
			});
			
		} catch (FrameworkException ex) {
			Logger.getLogger(RegistrationResource.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return null;
		
	}
	
	
	//~--- get methods ----------------------------------------------------

	@Override
	public Class getEntityClass() {

		return null;

	}

	@Override
	public String getUriPart() {

		return "registration";

	}

	@Override
	public String getResourceSignature() {

		return "_registration";

	}

	@Override
	public boolean isCollectionResource() {

		return false;

	}

}
