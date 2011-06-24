package org.structr.ui.page.admin;

import java.util.Collection;
import org.apache.click.ActionResult;
import org.structr.core.Services;
import org.structr.core.notification.GetNotificationsCommand;
import org.structr.core.notification.Notification;
import org.structr.ui.page.StructrPage;

/**
 *
 * @author Christian Morgner
 */
public class Ajax extends StructrPage {

	public ActionResult onSubmitConsoleCommand() {

		StringBuilder ret = new StringBuilder(200);



		// Return an action result containing the message
		return new ActionResult(ret.toString(), ActionResult.HTML);
	}

	public ActionResult onUpdateNotificationContent() {

		StringBuilder ret = new StringBuilder(200);
		int zIndex = 999;

		Collection<Notification> notifications = (Collection<Notification>)Services.command(GetNotificationsCommand.class).execute(getContext().getSession().getId());

		ret.append("<div id=\"notificationContent\">");

		if(notifications != null) {

			for(Notification notification : notifications) {
				String notificationTitle = notification.getTitle();
				String notificationText = notification.getText();
				String containerCss = notification.getContainerCss();
				String titleCss = notification.getTitleCss();
				String textCss = notification.getTextCss();

				ret.append("<div class='");
				if(containerCss != null) {
					ret.append(containerCss);
				}
				ret.append("' style='z-index:");
				ret.append(zIndex--);
				ret.append(";'>");

				if(notificationTitle != null && notificationTitle.length() > 0) {

					ret.append("<h3 class='");
					if(titleCss != null) {
						ret.append(titleCss);
					}
					ret.append("'>");
					ret.append(notificationTitle);
					ret.append("</h3>");
				}

				if(notificationText != null && notificationText.length() > 0) {

					ret.append("<p class='");
					if(textCss != null) {
						ret.append(textCss);
					}
					ret.append("'>");
					ret.append(notification.getText());
					ret.append("</p>");
				}

				ret.append("</div>");
			}

		}

		ret.append("</div>");

		// Return an action result containing the message
		return new ActionResult(ret.toString(), ActionResult.HTML);
	}
}
