/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cron;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.structr.agent.Task;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;

/**
 * A task for testing the CRON service.
 *
 *
 */
public class CronTestTask<T extends AbstractNode> implements Task<T> {

	@Override
	public Principal getUser() {
		return null;
	}

	@Override
	public List<T> getNodes() {
		return Collections.emptyList();
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
