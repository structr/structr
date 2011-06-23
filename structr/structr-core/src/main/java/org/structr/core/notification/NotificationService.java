package org.structr.core.notification;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.structr.core.Command;
import org.structr.core.SingletonService;

/**
 *
 * @author Christian Morgner
 */
public class NotificationService implements SingletonService {

	public static final Map<String, Queue<Notification>> globalNotificationMap = Collections.synchronizedMap(new HashMap<String, Queue<Notification>>());
	public static final Queue<Notification> globalNotifications = new ConcurrentLinkedQueue<Notification>();

	public List<Notification> getNotifications(String sessionId)
	{
		List<Notification> ret = new LinkedList<Notification>();

		ret.addAll(getNotificationsInternal(sessionId));
		ret.addAll(getGlobalNotifications());

		return(ret);
	}

	public boolean addNotification(Notification notification)
	{
		return(addNotification(null, notification));
	}

	public boolean addNotification(String sessionId, Notification notification)
	{
		if(sessionId != null) {

			Queue<Notification> notifications = getNotificationsInternal(sessionId);
			boolean ret = false;

			if(notifications == null) {

				notifications = new ConcurrentLinkedQueue<Notification>();
				synchronized(globalNotificationMap) {

					globalNotificationMap.put(sessionId, notifications);
				}
			}

			synchronized(notifications) {

				// store in list
				ret = notifications.add(notification);
			}

			return(ret);

		} else {

			globalNotifications.add(notification);

			return(true);
		}
	}

	private Queue<Notification> getNotificationsInternal(String sessionId)
	{
		Queue<Notification> ret = null;

		synchronized(globalNotificationMap) {

			ret = globalNotificationMap.get(sessionId);

			if(ret != null) {

				Notification head = ret.peek();
				if(head != null && head.isExpired()) {

					ret.remove();
				}
			}
		}

		return(ret);
	}

	private Queue<Notification> getGlobalNotifications()
	{
		Notification head = globalNotifications.peek();
		if(head != null && head.isExpired()) {

			globalNotifications.remove();
		}

		return(globalNotifications);
	}

	// ----- interface SingletonService -----
	@Override
	public void injectArguments(Command command)
	{
		command.setArgument("service", this);
	}

	@Override
	public void initialize(Map<String, Object> context)
	{
	}

	@Override
	public void shutdown()
	{
	}

	@Override
	public boolean isRunning()
	{
		return(true);
	}

	@Override
	public String getName()
	{
		return("structr notification service");
	}
}
