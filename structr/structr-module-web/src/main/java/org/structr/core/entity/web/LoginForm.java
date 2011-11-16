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
import org.structr.renderer.LoginFormRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * Render a login form.
 *
 * Username and password field names have to match with a corresponding
 * LoginCheck node to make login work.
 *
 * @author axel
 */
public class LoginForm extends Form {

	public final static String defaultAntiRobotFieldName = "loginForm_antiRobot";
	public final static String defaultPasswordFieldName  = "loginForm_password";
	public final static String defaultSubmitButtonName   = "loginForm_submit";
	public final static String defaultUsernameFieldName  = "loginForm_username";

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(LoginForm.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	/** Name of username field */
	public enum Key implements PropertyKey{ usernameFieldName, passwordFieldName; }

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new LoginFormRenderer());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/form.png";
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

	//~--- set methods ----------------------------------------------------

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
}
