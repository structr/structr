package org.structr.agent;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;

/**
 *
 * @author Christian Morgner
 */
public class AbstractTask<T extends AbstractNode> implements Task<T> {

	private final Map<String, Object> statusProperties = new LinkedHashMap<>();
	private final List<T> nodes                        = new LinkedList<>();
	private Principal user                             = null;
	private int priority                               = 0;
	private Date scheduledTime                         = null;
	private Date creationTime                          = null;
	private String type                                = null;
	private long delay                                 = 0L;

	public AbstractTask(final String type, final Principal user, final T node) {

		this.type = type;
		this.user = user;

		this.nodes.add(node);
	}

	@Override
	public Principal getUser() {
		return user;
	}

	@Override
	public List<T> getNodes() {
		return nodes;
	}

	@Override
	public int priority() {
		return priority;
	}

	@Override
	public Date getScheduledTime() {
		return scheduledTime;
	}

	@Override
	public Date getCreationTime() {
		return creationTime;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public long getDelay(final TimeUnit unit) {

		if (unit != null) {
			return unit.convert(delay, TimeUnit.MILLISECONDS);
		}

		return delay;
	}

	@Override
	public Object getStatusProperty(final String key) {
		return statusProperties.get(key);
	}

	public void setStatusProperty(final String key, final Object value) {
		statusProperties.put(key, value);
	}

	public void addNode(final T node) {
		this.nodes.add(node);
	}

	public void setUser(final Principal user) {
		this.user = user;
	}

	public void setPriority(final int priority) {
		this.priority = priority;
	}

	public void setScheduledTime(final Date scheduledTime) {
		this.scheduledTime = scheduledTime;
	}

	public void setCreationTime(final Date creationTime) {
		this.creationTime = creationTime;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public void setDelay(final long delay) {
		this.delay = delay;
	}

	@Override
	public int compareTo(final Delayed other) {

		final long otherDelay = other.getDelay(TimeUnit.MILLISECONDS);

		return delay > otherDelay ? 1 : delay < otherDelay ? -1 : 0;
	}
}
