package org.structr.renderer;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.structr.common.MailHelper;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.web.MailSender;

/**
 *
 * @author Christian Morgner
 */
public class MailSenderRenderer extends FormRenderer implements NodeRenderer<MailSender>
{
	private static final Logger logger = Logger.getLogger(MailSenderRenderer.class.getName());

	@Override
	public void renderNode(StructrOutputStream out, MailSender currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		readParameters(out.getRequest(), currentNode);
		if(!validateParameters(currentNode))
		{
			return;
		}

		String to = replaceInContent(currentNode.getToAddressTemplate(), parameterMap);
		String toName = replaceInContent(currentNode.getToNameTemplate(), parameterMap);
		String from = replaceInContent(currentNode.getFromAddressTemplate(), parameterMap);
		String fromName = replaceInContent(currentNode.getFromNameTemplate(), parameterMap);
		String cc = replaceInContent(currentNode.getCcAddressTemplate(), parameterMap);
		String bcc = replaceInContent(currentNode.getBccAddressTemplate(), parameterMap);

		String subject = replaceInContent(currentNode.getMailSubjectTemplate(), parameterMap);
		String htmlContent = replaceInContent(currentNode.getHtmlBodyTemplate(), parameterMap);
		String textContent = null;
		if(StringUtils.isNotEmpty(htmlContent))
		{
			textContent = Jsoup.parse(htmlContent).text();
		}

		// If no errors so far, try sending e-mail

		if(errorMsg.length() == 0)
		{

			// Send e-mail
			try
			{

				MailHelper.sendHtmlMail(from, fromName, to, toName, cc, bcc, from, subject, htmlContent, textContent);

				out.append("<div class=\"okMsg\">").append("An e-mail with your invitation was send to ").append(to).append("</div>");
				out.append("<div class=\"htmlMessage\">").append(htmlContent).append("</div>");

			} catch(Exception e)
			{
				logger.log(Level.SEVERE, "Error while sending e-mail", e);
				errorMsg.append("<div class=\"errorMsg\">").append("Error while sending e-mail: ").append(e.getMessage()).append("</div>");
			}
		}

		if(errorMsg.length() > 0)
		{
			out.append(errorMsg).append(errorStyle);
			return;
		}

	}

	@Override
	public String getContentType(MailSender currentNode)
	{
		return ("text/html");


	}
}
