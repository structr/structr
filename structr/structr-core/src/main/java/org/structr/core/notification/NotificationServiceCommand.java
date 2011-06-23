package org.structr.core.notification;

import org.structr.core.Command;

/**
 *
 * @author Christian Morgner
 */
public abstract class NotificationServiceCommand extends Command {

	@Override
	public Class getServiceClass()
	{
		return(NotificationService.class);
	}
}
