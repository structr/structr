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

package org.structr.modules.agent.thumbnail;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.structr.core.agent.Task;
import org.structr.core.entity.StructrNode;

/**
 *
 * @author cmorgner
 */
public class ThumbnailTask implements Task
{
	private Set<StructrNode> nodes = null;

	public ThumbnailTask(StructrNode node)
	{
		this.nodes = new LinkedHashSet<StructrNode>();
		this.nodes.add(node);
	}

	public Set<StructrNode> getNodes()
	{
		return(nodes);
	}

	public int priority()
	{
		return(0);
	}

	public Date getScheduledTime()
	{
		return(null);
	}

	public Date getCreationTime()
	{
		return(null);
	}

	public long getDelay(TimeUnit unit)
	{
		return(0);
	}

	public int compareTo(Delayed o)
	{
		Long d1 = new Long(this.getDelay(TimeUnit.MILLISECONDS));
		Long d2 = new Long(   o.getDelay(TimeUnit.MILLISECONDS));

		return(d1.compareTo(d2));
	}

}
