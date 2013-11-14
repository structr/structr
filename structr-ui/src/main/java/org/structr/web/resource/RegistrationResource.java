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

import java.util.Collections;
import org.structr.common.MailHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.property.PropertyKey;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.Resource;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.User;
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
		TARGET_PAGE,
		ERROR_PAGE,
		CONFIRM_REGISTRATION_PAGE,
		CONFIRM_KEY_KEY,
		TARGET_PAGE_KEY,
		ERROR_PAGE_KEY
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
		
		boolean existingUser = false;
		
		if (propertySet.containsKey(User.eMail.jsonName())) {
			
			SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
			final Principal user;
			
			final String emailString  = (String) propertySet.get(User.eMail.jsonName());
			
			if (StringUtils.isEmpty(emailString)) {
				return new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);
			}
			
			localeString = (String) propertySet.get(MailTemplate.locale.jsonName());
			confKey = UUID.randomUUID().toString();
			
			Result result = Services.command(superUserContext, SearchNodeCommand.class).execute(
				Search.andExactTypeAndSubtypes(User.class),
				Search.andExactProperty(superUserContext, User.eMail, emailString));
				
			if (!result.isEmpty()) {
				
				user = (Principal) result.get(0);
				
				// For existing users, update confirmation key
				
				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {
				
						user.setProperty(User.confirmationKey, confKey);
						return null;
					}
				});
				
				existingUser = true;

				
			} else {

				Authenticator auth = securityContext.getAuthenticator();
				user = createUser(securityContext, User.eMail, emailString, propertySet, auth.getUserAutoCreate(), auth.getUserClass());
			}
			
			if (user != null) {

				if (!sendInvitationLink(user, propertySet)) {
					
					// return 400 Bad request
					return new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);
					
				}
				
				// If we have just updated the confirmation key for an existing user,
				// return 200 to distinguish from new users
				if (existingUser) {
					
					// return 200 OK
					return new RestMethodResult(HttpServletResponse.SC_OK);
					
				} else {
				
					// return 201 Created
					return new RestMethodResult(HttpServletResponse.SC_CREATED);
					
				}

			} else {
				
				// return 400 Bad request
				return new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);
				
			}
			

		} else {

			// return 400 Bad request
			return new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);
		
		}

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

	private boolean sendInvitationLink(final Principal user, final Map<String, Object> propertySetFromUserPOST) {

		Map<String, String> replacementMap = new HashMap();
		
		// Populate the replacement map with all POSTed values
		// WARNING! This is unchecked user input!!
		populateReplacementMap(replacementMap, propertySetFromUserPOST);

		String userEmail = user.getProperty(User.eMail);
		
		replacementMap.put(toPlaceholder(User.eMail.jsonName()), userEmail);
		replacementMap.put(toPlaceholder("link"),
			getTemplateText(TemplateKey.BASE_URL, "http://" + Services.getApplicationHost() + ":" + Services.getHttpPort())
			+ getTemplateText(TemplateKey.CONFIRM_REGISTRATION_PAGE, HtmlServlet.CONFIRM_REGISTRATION_PAGE)
			//+ "/" + HtmlServlet.CONFIRM_REGISTRATION_PAGE
			+ getTemplateText(TemplateKey.CONFIRM_KEY_KEY, HtmlServlet.CONFIRM_KEY_KEY)
			+ "=" + confKey
			+ "&" + getTemplateText(TemplateKey.TARGET_PAGE_KEY, HtmlServlet.TARGET_PAGE_KEY)
			+ "=" + getTemplateText(TemplateKey.TARGET_PAGE, "register_thanks")
			+ "&" + getTemplateText(TemplateKey.ERROR_PAGE_KEY, HtmlServlet.ERROR_PAGE_KEY)
			+ "=" + getTemplateText(TemplateKey.ERROR_PAGE, "register_error"));

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
			List<SearchAttribute> attrs = new LinkedList();
			
			attrs.add(Search.andExactType(MailTemplate.class));
			attrs.add(Search.andExactName(key.name()));
			
			if (localeString != null) {
				attrs.add(Search.andExactProperty(securityContext, MailTemplate.locale, localeString));
			}
			
			List<MailTemplate> templates = (List<MailTemplate>) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(attrs).getResults();
			
			if (!templates.isEmpty()) {
				
				Content content = templates.get(0).getProperty(MailTemplate.text);
				return content != null ? content.getProperty(Content.content) : defaultValue;
				
			} else {
				
				return defaultValue;
				
			}
			
		} catch (FrameworkException ex) {
			
			Logger.getLogger(RegistrationResource.class.getName()).log(Level.WARNING, "Could not get mail template for key " + key, ex);
			
		}
		
		return null;
		
	}
	
	private static void populateReplacementMap(final Map<String, String> replacementMap, final Map<String, Object> props) {
		
		for (Entry<String, Object> entry : props.entrySet()) {
			
			replacementMap.put(toPlaceholder(entry.getKey()), entry.getValue().toString());
			
		}
		
	}
	
	private static String toPlaceholder(final String key) {

		return "${".concat(key).concat("}");

	}
	
	/**
	 * Create a new user.
	 * 
	 * If a {@link Person} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 * 
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @return 
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue) {
		
		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP);
		
	}	
	
	/**
	 * Create a new user.
	 * 
	 * If a {@link Person} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 * 
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param propertySet
	 * @return 
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet) {
		
		return createUser(securityContext, credentialKey, credentialValue, propertySet, false);
		
	}	

	/**
	 * Create a new user.
	 * 
	 * If a {@link Person} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 * 
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param autoCreate
	 * @return 
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final boolean autoCreate) {
		
		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP, autoCreate);
		
	}	

	/**
	 * Create a new user.
	 * 
	 * If a {@link Person} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 * 
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param autoCreate
	 * @param userClass
	 * @return 
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final boolean autoCreate, final Class userClass) {
		
		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP, autoCreate, userClass);
		
	}	

	/**
	 * Create a new user.
	 * 
	 * If a {@link Person} is found, convert that object to a {@link User} object.
	 * If autoCreate is true, auto-create a new user, even if no matching person is found.
	 * 
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param propertySet
	 * @param autoCreate
	 * @return 
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet, final boolean autoCreate) {
		
		return createUser(securityContext, credentialKey, credentialValue, propertySet, autoCreate, User.class);
		
	}	
		
	/**
	 * Create a new user.
	 * 
	 * If a {@link Person} is found, convert that object to a {@link User} object.
	 * If autoCreate is true, auto-create a new user, even if no matching person is found.
	 * 
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param propertySet
	 * @param autoCreate
	 * @param userClass
	 * @return 
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet, final boolean autoCreate, final Class userClass) {

		try {
			return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Principal>() {

				@Override
				public Principal execute() throws FrameworkException {

					// First, search for a person with that e-mail address
					Person person = (Person) AuthHelper.getPrincipalForCredential(credentialKey, credentialValue);

					if (person != null) {
						
						// convert to user
						person.unlockReadOnlyPropertiesOnce();
						person.setProperty(AbstractNode.type, User.class.getSimpleName());

						User user = new User();
						user.init(securityContext, person.getNode());

						// index manually, because type is a system property!
						person.updateInIndex();

						user.setProperty(User.confirmationKey, confKey);
						
						return user;

					} else if (autoCreate) {
						
						propertySet.put(AbstractNode.type.jsonName(), userClass != null ? userClass.getSimpleName() : User.class.getSimpleName());

						PropertyMap props = PropertyMap.inputTypeToJavaType(securityContext, propertySet);
						props.put(credentialKey, credentialValue);
						props.put(User.name, credentialValue);
						props.put(User.confirmationKey, confKey);
						
						return (Principal) Services.command(securityContext, CreateNodeCommand.class).execute(props);

					}
					
					logger.log(Level.WARNING, "No user created: No matching person found, and auto-creation is off");
					
					return null;

				}
				
			});
			
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
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
