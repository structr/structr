package org.structr.core.notification;

/**
 *
 * @author Christian Morgner
 */
public interface Notification {

	public String getTitle();
	public String getText();
	public boolean isExpired();
}
