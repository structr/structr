/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.agent;

import org.structr.core.agent.Task;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

//~--- classes ----------------------------------------------------------------

/**
 * Use this interface for tasks that convert a given node into another
 * type.
 *
 * @author amorgner
 */
public class UpdateImageMetadataTask implements Task {

	public UpdateImageMetadataTask() {}

	//~--- methods --------------------------------------------------------

	@Override
	public int priority() {

		return 0;

	}

	@Override
	public int compareTo(Delayed o) {

		Long d1 = Long.valueOf(this.getDelay(TimeUnit.MILLISECONDS));
		Long d2 = Long.valueOf(o.getDelay(TimeUnit.MILLISECONDS));

		return (d1.compareTo(d2));

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Set<AbstractNode> getNodes() {

		return Collections.emptySet();

	}

	@Override
	public Date getScheduledTime() {

		return null;

	}

	@Override
	public Date getCreationTime() {

		return null;

	}

	@Override
	public long getDelay(TimeUnit unit) {

		return 0;

	}

	// ----- interface StatusInfo -----
	@Override
	public Object getStatusProperty(String key) {

		// TODO..
		return (null);
	}

	@Override
	public Principal getUser() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public String getType() {

		return getClass().getSimpleName();

	}

}
