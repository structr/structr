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
package org.structr.cron;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.structr.agent.Task;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;

/**
 * A task for testing the CRON service.
 * 
 * @author Christian Morgner
 */
public class CronTestTask implements Task {

	@Override
	public Principal getUser() {
		return null;
	}

	@Override
	public Set<AbstractNode> getNodes() {
		return Collections.emptySet();
	}

	@Override
	public int priority() {
		return 0;
	}

	@Override
	public Date getScheduledTime() {
		return new Date();
	}

	@Override
	public Date getCreationTime() {
		return new Date();
	}

	@Override
	public String getType() {
		return "CronTestTask";
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return 0;
	}

	@Override
	public int compareTo(Delayed o) {
		return 0;
	}

	@Override
	public Object getStatusProperty(String key) {
		return null;
	}
}
