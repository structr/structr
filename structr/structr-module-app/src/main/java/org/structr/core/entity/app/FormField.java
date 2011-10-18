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

<<<<<<< HEAD
import javax.servlet.http.HttpServletRequest;
=======
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.core.EntityContext;
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
import org.structr.core.entity.AbstractNode;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public abstract class FormField extends AbstractNode {

	static {

		EntityContext.registerPropertySet(DataNode.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ label, description, hint, helpText; }

	//~--- get methods ----------------------------------------------------

	public abstract String getErrorMessage(HttpServletRequest request);

	public abstract Object getErrorValue(HttpServletRequest request);

<<<<<<< HEAD
	public abstract void setErrorValue(HttpServletRequest request, Object errorValue);

=======
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
	public String getLabel() {
		return getStringProperty(Key.label.name());
	}

	public String getDescription() {
		return getStringProperty(Key.description.name());
	}

	public String getHint() {
		return getStringProperty(Key.hint.name());
	}

	public String getHelpText() {
		return getStringProperty(Key.helpText.name());
	}

	//~--- set methods ----------------------------------------------------

	public abstract void setErrorValue(Object errorValue);

	public void setLabel(final String value) {

		setProperty(Key.label.name(),
			    value);
	}

	public void setDescription(final String value) {

		setProperty(Key.description.name(),
			    value);
	}

	public void setHint(final String value) {

		setProperty(Key.hint.name(),
			    value);
	}

	public void setHelpText(final String value) {

		setProperty(Key.helpText.name(),
			    value);
	}
}
