package org.structr.core.notification;

import org.structr.common.SecurityContext;

/**
 * The default success notification with success icon.
 *
 * @author Christian Morgner
 */
public class SuccessNotification extends DefaultNotification {

    public SuccessNotification(SecurityContext securityContext, String text)
    {
	this(securityContext, text, 3000);
    }

    public SuccessNotification(SecurityContext securityContext, String text, long lifespan)
    {
	super(securityContext, null, text, lifespan);
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
