/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity.web;

import java.util.Map;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.renderer.LoginCheckRenderer;

/**
 * Checks login credentials.
 *
 * If a user with given username and password exists, and is not blocked,
 * a redirect to the given target node is initiated.
 * 
 * @author axel
 */
public class LoginCheck extends WebNode {

	private final static String ICON_SRC = "/images/door_in.png";

	@Override
	public String getIconSrc() {
		return ICON_SRC;
	}
	public final static String SUBMIT_BUTTON_NAME_KEY = "submitButtonName";
	public final static String ANTI_ROBOT_FIELD_NAME_KEY = "antiRobotFieldName";
	/** Name of username field */
	public final static String USERNAME_FIELD_NAME_KEY = "usernameFieldName";
	/** Name of password field */
	public final static String PASSWORD_FIELD_NAME_KEY = "passwordFieldName";
	/** Absolute number of login errors (wrong inputs) for a session. Each wrong or missing input field is counted. */
	public final static String MAX_ERRORS_KEY = "maxRetries";
	/** Number of unsuccessful login attempts before retry delay becomes active */
	public final static String DELAY_THRESHOLD_KEY = "delayThreshold";
	/** Time to wait for retry after an unsuccessful login attempt */
	public final static String DELAY_TIME_KEY = "delayTime";

	/**
	 * Return name of anti robot field
	 *
	 * @return
	 */
	public String getAntiRobotFieldName() {
		return getStringProperty(ANTI_ROBOT_FIELD_NAME_KEY);
	}

	/**
	 * Set name of anti robot field
	 *
	 * @param value
	 */
	public void setAntiRobotFieldName(final String value) {
		setProperty(ANTI_ROBOT_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of submit button
	 *
	 * @return
	 */
	public String getSubmitButtonName() {
		return getStringProperty(SUBMIT_BUTTON_NAME_KEY);
	}

	/**
	 * Set name of submit button
	 *
	 * @param value
	 */
	public void setSubmitButtonName(final String value) {
		setProperty(SUBMIT_BUTTON_NAME_KEY, value);
	}

	/**
	 * Return name of username field
	 *
	 * @return
	 */
	public String getUsernameFieldName() {
		return getStringProperty(USERNAME_FIELD_NAME_KEY);
	}

	/**
	 * Return name of password field
	 *
	 * @return
	 */
	public String getPasswordFieldName() {
		return getStringProperty(PASSWORD_FIELD_NAME_KEY);
	}

	/**
	 * Return number of unsuccessful login attempts for a session
	 *
	 * If number is exceeded, login is blocked for this session.
	 */
	public int getMaxErrors() {
		return getIntProperty(MAX_ERRORS_KEY);
	}

	/**
	 * Return number of unsuccessful login attempts before retry delay becomes active
	 */
	public int getDelayThreshold() {
		return getIntProperty(DELAY_THRESHOLD_KEY);
	}

	/**
	 * Return time (in seconds) to wait for retry after an unsuccessful login attempt
	 */
	public int getDelayTime() {
		return getIntProperty(DELAY_TIME_KEY);
	}

	/**
	 * Set name of username field
	 *
	 * @param value
	 */
	public void setUsernameFieldName(final String value) {
		setProperty(USERNAME_FIELD_NAME_KEY, value);
	}

	/**
	 * Set name of password field
	 *
	 * @param value
	 */
	public void setPasswordFieldName(final String value) {
		setProperty(USERNAME_FIELD_NAME_KEY, value);
	}

	/**
	 * Set number of unsuccessful login attempts for a session.
	 *
	 * @param value
	 */
	public void setMaxErrors(final int value) {
		setProperty(MAX_ERRORS_KEY, value);
	}

	/**
	 * Set number of unsuccessful login attempts before retry delay becomes active
	 *
	 * @param value
	 */
	public void setDelayThreshold(final int value) {
		setProperty(DELAY_THRESHOLD_KEY, value);
	}

	/**
	 * Set time (in seconds) to wait for retry after an unsuccessful login attempt
	 *
	 * @param value
	 */
	public void setDelayTime(final int value) {
		setProperty(DELAY_TIME_KEY, value);
	}

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {
		renderers.put(RenderMode.Default, new LoginCheckRenderer());
	}
}
