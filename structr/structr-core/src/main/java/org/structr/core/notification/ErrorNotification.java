package org.structr.core.notification;

import org.structr.common.SecurityContext;

/**
 * The default error notification with error icon.
 *
 * @author Christian Morgner
 */
public class ErrorNotification extends DefaultNotification {

    public ErrorNotification(SecurityContext securityContext, String text)
    {
	this(securityContext, text, 3000);
    }

    public ErrorNotification(SecurityContext securityContext, String text, long lifespan)
    {
	super(securityContext, null, text, lifespan);
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
