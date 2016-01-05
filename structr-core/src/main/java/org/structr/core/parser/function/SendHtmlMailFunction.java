package org.structr.core.parser.function;

import org.apache.commons.mail.EmailException;
import org.structr.common.MailHelper;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SendHtmlMailFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SEND_HTML_MAIL = "Usage: ${send_html_mail(fromAddress, fromName, toAddress, toName, subject, content)}.";

	@Override
	public String getName() {
		return "send_html_mail()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 6)) {

			final String from = sources[0].toString();
			final String fromName = sources[1].toString();
			final String to = sources[2].toString();
			final String toName = sources[3].toString();
			final String subject = sources[4].toString();
			final String htmlContent = sources[5].toString();
			String textContent = "";

			if (sources.length == 7) {
				textContent = sources[6].toString();
			}

			try {
				return MailHelper.sendHtmlMail(from, fromName, to, toName, null, null, from, subject, htmlContent, textContent);

			} catch (EmailException eex) {
				eex.printStackTrace();
			}
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SEND_HTML_MAIL;
	}

	@Override
	public String shortDescription() {
		return "Sends an HTML e-mail";
	}

}
