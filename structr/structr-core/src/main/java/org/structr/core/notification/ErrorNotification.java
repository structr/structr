package org.structr.core.notification;

/**
 * The default error notification with error icon.
 *
 * @author Christian Morgner
 */
public class ErrorNotification extends DefaultNotification {

    public ErrorNotification(String text)
    {
	this(text, 3000);
    }

    public ErrorNotification(String text, long lifespan)
    {
	super(null, text, lifespan);
    }

    @Override
    public String getContainerCss()
    {
	return("notification error");
    }

    @Override
    public String getTextCss()
    {
	return("notificationText error");
    }
}
