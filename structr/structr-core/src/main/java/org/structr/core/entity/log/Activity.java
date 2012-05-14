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



package org.structr.core.entity.log;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.converter.LongDateConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 *
 * Activity is the base class for any logged activity. An activity has start
 * and end time, so we can log long-running activities as well.
 *
 * Subclass this class to log more specific activities.
 *
 * @author axel
 */
public class Activity extends AbstractNode {

	static {

		EntityContext.registerPropertySet(Activity.class, PropertyView.All, Key.values());
		EntityContext.registerPropertyConverter(Activity.class, Key.startTimestamp, LongDateConverter.class);
		EntityContext.registerPropertyConverter(Activity.class, Key.endTimestamp, LongDateConverter.class);

	}

	//~--- fields ---------------------------------------------------------

	private Principal user;

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ sessionId, startTimestamp, endTimestamp, activityText; }

	//~--- constructors ---------------------------------------------------

	public Activity() {

		super();

	}

	public Activity(final Map<String, Object> properties) {

		super(properties);

	}

	//~--- get methods ----------------------------------------------------

	public Date getStartTimestamp() {

		return getDateProperty(Key.startTimestamp.name());

	}

	public Date getEndTimestamp() {

		return getDateProperty(Key.endTimestamp.name());

	}

	public String getActivityText() {

		return getStringProperty(Key.activityText.name());

	}

	public long getSessionId() {

		return getLongProperty(Key.sessionId.name());

	}

	/**
	 * Principal property for logging purposes only
	 *
	 * @return
	 */
	public Principal getUser() {

		return user;

	}

	//~--- set methods ----------------------------------------------------

	public void setStartTimestamp(final Date timestamp) throws FrameworkException {

		setProperty(Key.startTimestamp.name(), timestamp);

	}

	public void setEndTimestamp(final Date timestamp) throws FrameworkException {

		setProperty(Key.endTimestamp.name(), timestamp);

	}

	public void setActivityText(final String text) throws FrameworkException {

		setProperty(Key.activityText.name(), text);

	}

	public void setSessionId(final long id) throws FrameworkException {

		setProperty(Key.sessionId.name(), id);

	}

	/**
	 * Principal property for logging purposes only
	 */
	public void setUser(final Principal user) {

		this.user = user;

	}

}
