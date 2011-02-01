/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
