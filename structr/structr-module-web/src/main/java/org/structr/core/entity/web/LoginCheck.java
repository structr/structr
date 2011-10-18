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

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;
import org.structr.renderer.LoginCheckRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * Checks login credentials.
 *
 * If a user with given username and password exists, and is not blocked,
 * a redirect to the given target node is initiated.
 *
 * @author axel
 */
public class LoginCheck extends WebNode {

	static {

		EntityContext.registerPropertySet(LoginCheck.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey {

		submitButtonName, antiRobotFieldName, usernameFieldName, passwordFieldName, maxErrors, delayThreshold,
		delayTime;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new LoginCheckRenderer());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/door_in.png";
	}

	/**
	 * Return name of anti robot field
	 *
	 * @return
	 */
	public String getAntiRobotFieldName() {
		return getStringProperty(Key.antiRobotFieldName.name());
	}

	/**
	 * Return name of submit button
	 *
	 * @return
	 */
	public String getSubmitButtonName() {
		return getStringProperty(Key.submitButtonName.name());
	}

	/**
	 * Return name of username field
	 *
	 * @return
	 */
	public String getUsernameFieldName() {
		return getStringProperty(Key.usernameFieldName.name());
	}

	/**
	 * Return name of password field
	 *
	 * @return
	 */
	public String getPasswordFieldName() {
		return getStringProperty(Key.passwordFieldName.name());
	}

	/**
	 * Return number of unsuccessful login attempts for a session
	 *
	 * If number is exceeded, login is blocked for this session.
	 */
	public int getMaxErrors() {
		return getIntProperty(Key.maxErrors.name());
	}

	/**
	 * Return number of unsuccessful login attempts before retry delay becomes active
	 */
	public int getDelayThreshold() {
		return getIntProperty(Key.delayThreshold.name());
	}

	/**
	 * Return time (in seconds) to wait for retry after an unsuccessful login attempt
	 */
	public int getDelayTime() {
		return getIntProperty(Key.delayTime.name());
	}

	//~--- set methods ----------------------------------------------------

	/**
	 * Set name of anti robot field
	 *
	 * @param value
	 */
	public void setAntiRobotFieldName(final String value) {

		setProperty(Key.antiRobotFieldName.name(),
			    value);
	}

	/**
	 * Set name of submit button
	 *
	 * @param value
	 */
	public void setSubmitButtonName(final String value) {

		setProperty(Key.submitButtonName.name(),
			    value);
	}

	/**
	 * Set name of username field
	 *
	 * @param value
	 */
	public void setUsernameFieldName(final String value) {

		setProperty(Key.usernameFieldName.name(),
			    value);
	}

	/**
	 * Set name of password field
	 *
	 * @param value
	 */
	public void setPasswordFieldName(final String value) {

		setProperty(Key.passwordFieldName.name(),
			    value);
	}

	/**
	 * Set number of unsuccessful login attempts for a session.
	 *
	 * @param value
	 */
	public void setMaxErrors(final int value) {

		setProperty(Key.maxErrors.name(),
			    value);
	}

	/**
	 * Set number of unsuccessful login attempts before retry delay becomes active
	 *
	 * @param value
	 */
	public void setDelayThreshold(final int value) {

		setProperty(Key.delayThreshold.name(),
			    value);
	}

	/**
	 * Set time (in seconds) to wait for retry after an unsuccessful login attempt
	 *
	 * @param value
	 */
	public void setDelayTime(final int value) {

		setProperty(Key.delayTime.name(),
			    value);
	}
}
