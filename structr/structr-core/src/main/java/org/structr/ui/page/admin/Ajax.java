package org.structr.ui.page.admin;

import org.apache.click.ActionResult;

import org.structr.core.Services;
import org.structr.core.notification.GetNotificationsCommand;
import org.structr.core.notification.Notification;
import org.structr.ui.page.StructrPage;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import org.structr.common.AccessMode;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class Ajax extends StructrPage {

	public Ajax() {
		super();

		securityContext.setAccessMode(AccessMode.Backend);
	}

	//~--- methods --------------------------------------------------------

	public ActionResult onUpdateNotificationContent() {

		StringBuilder output                   = new StringBuilder(200);
		int zIndex                             = 999;
		Collection<Notification> notifications = (Collection<Notification>) Services.command(
							     GetNotificationsCommand.class).execute(
							     getContext().getSession().getId());

		output.append("<div id=\"notificationContent\">");

		if (notifications != null) {

			for (Notification notification : notifications) {

				String notificationTitle = notification.getTitle();
				String notificationText  = notification.getText();
				String containerCss      = notification.getContainerCss();
				String titleCss          = notification.getTitleCss();
				String textCss           = notification.getTextCss();

				output.append("<div class='");

				if (containerCss != null) {
					output.append(containerCss);
				}

				output.append("' style='z-index:");
				output.append(zIndex--);
				output.append(";'>");

				if ((notificationTitle != null) && (notificationTitle.length() > 0)) {

					output.append("<h3 class='");

					if (titleCss != null) {
						output.append(titleCss);
					}

					output.append("'>");
					output.append(notificationTitle);
					output.append("</h3>");
				}

				if ((notificationText != null) && (notificationText.length() > 0)) {

					output.append("<p class='");

					if (textCss != null) {
						output.append(textCss);
					}

					output.append("'>");
					output.append(notification.getText());
					output.append("</p>");
				}

				output.append("</div>");
			}
		}

		output.append("</div>");

		// Return an action result containing the message
		return new ActionResult(output.toString(), ActionResult.HTML);
	}
}
