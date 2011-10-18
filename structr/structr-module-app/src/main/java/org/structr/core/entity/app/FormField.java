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
package org.structr.core.entity.app;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author axel
 */
public abstract class FormField extends AbstractNode {

	public static final String LABEL_KEY = "label";
	public static final String DESCRIPTION_KEY = "description";
	public static final String HINT_KEY = "hint";
	public static final String HELP_TEXT_KEY = "helpText";

	public abstract String getErrorMessage(HttpServletRequest request);

	public abstract Object getErrorValue(HttpServletRequest request);

	public abstract void setErrorValue(HttpServletRequest request, Object errorValue);

	public String getLabel() {
		return getStringProperty(LABEL_KEY);
	}

	public void setLabel(final String value) {
		setProperty(LABEL_KEY, value);
	}

	public String getDescription() {
		return getStringProperty(DESCRIPTION_KEY);
	}

	public void setDescription(final String value) {
		setProperty(DESCRIPTION_KEY, value);
	}

	public String getHint() {
		return getStringProperty(HINT_KEY);
	}

	public void setHint(final String value) {
		setProperty(HINT_KEY, value);
	}

	public String getHelpText() {
		return getStringProperty(HELP_TEXT_KEY);
	}

	public void setHelpText(final String value) {
		setProperty(HELP_TEXT_KEY, value);
	}
}
