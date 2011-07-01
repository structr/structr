package org.structr.core.notification;

/**
 * The default success notification with success icon.
 *
 * @author Christian Morgner
 */
public class SuccessNotification extends DefaultNotification {

    public SuccessNotification(String text)
    {
	this(text, 3000);
    }

    public SuccessNotification(String text, long lifespan)
    {
	super(null, text, lifespan);
    }

    @Override
    public String getContainerCss()
    {
	return("notification success");
    }

    @Override
    public String getTextCss()
    {
	return("notificationText success");
    }
}
