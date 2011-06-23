package org.structr.core.notification;

/**
 *
 * @author chrisi
 */
public class DefaultNotification implements Notification {

	protected long creationTime = 0L;
	protected long lifespan = 0L;
	protected String title = null;
	protected String text = null;

	public DefaultNotification(String title, String text)
	{
		this.creationTime = System.currentTimeMillis();
		this.lifespan = 3000;

		this.title = title;
		this.text = text;
	}

	@Override
	public String getTitle()
	{
		return(title);
	}

	@Override
	public String getText()
	{
		return(text);
	}

	@Override
	public boolean isExpired()
	{
		return(System.currentTimeMillis() > this.creationTime + lifespan);
	}
}
