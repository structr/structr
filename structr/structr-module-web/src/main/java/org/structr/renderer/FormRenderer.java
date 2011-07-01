package org.structr.renderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.structr.common.CurrentRequest;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.web.Form;

/**
 *
 * @author Christian Morgner
 */
public abstract class FormRenderer
{
	private static final Logger logger = Logger.getLogger(FormRenderer.class.getName());

	private final static String KEY_PREFIX = "${";
	private final static String KEY_SUFFIX = "}";

	public final static String defaultAction = "";
	public final static String defaultCssClass = "formTable";
	public final static String defaultLabel = "Login";
	public final static String defaultSubmitButtonName = "form_submit";
	public final static String defaultAntiRobotFieldName = "form_antiRobot";

	protected Map parameterMap = new HashMap<String, String>();
	protected StringBuilder errorMsg = new StringBuilder();
	protected StringBuilder errorStyle = new StringBuilder();

	private String submitButtonName;
	private String antiRobotFieldName;

	protected boolean validateParameters(Form node)
	{

		if(StringUtils.isEmpty(param(submitButtonName)))
		{
			// Don't process form at all if submit button was not pressed
			return false;
		}

		if(StringUtils.isNotEmpty(param(antiRobotFieldName)))
		{
			// Don't process form if someone has filled the anti-robot field
			return false;
		}

		// Check mandatory parameters

		errorStyle.append("<style type=\"text/css\">");

		List<String> mandatoryParameterNames = node.getMandatoryParameterNamesAsList();

		if(mandatoryParameterNames != null)
		{

			for(String mandatoryParameterName : mandatoryParameterNames)
			{
				if(StringUtils.isEmpty(param(mandatoryParameterName)))
				{
					errorMsg.append("<div class=\"errorMsg\">").append("Please fill out \"").append("<script type=\"text/javascript\">document.write(getLabel('").append(mandatoryParameterName).append("'));</script>\"").append("</div>");
					errorStyle.append("input[name=").append(mandatoryParameterName).append("] { background-color: #ffc }\n");
				}
			}
		}
		errorStyle.append("</style>");

		return true;
	}

	protected void readParameters(Form node)
	{

		HttpServletRequest request = CurrentRequest.getRequest();

		if(request == null)
		{
			return;
		}

//            HttpSession session = request.getSession();
//
//            if (session == null) {
//                return;
//            }

		List<String> parameterNames = node.getParameterNamesAsList();

		// Get values from config page, or defaults
		submitButtonName = node.getSubmitButtonName() != null ? node.getSubmitButtonName() : defaultSubmitButtonName;
		antiRobotFieldName = node.getAntiRobotFieldName() != null ? node.getAntiRobotFieldName() : defaultAntiRobotFieldName;

		// Static, technical parameters
		parameterMap.put(submitButtonName, request.getParameter(submitButtonName));
		parameterMap.put(antiRobotFieldName, request.getParameter(antiRobotFieldName));

		if(parameterNames != null)
		{
			for(String parameterName : parameterNames)
			{

				String parameterValue = request.getParameter(parameterName);

				// Clean values and add to parameter map
				parameterMap.put(parameterName, clean(node, parameterValue));
			}
		}
	}

	public String clean(Form node, final String input)
	{

		String output = StringUtils.trimToEmpty(input);

		for(String strip : node.getStripFromValuesAsList())
		{
			output = StringUtils.replace(output, strip, "");
		}

		return output;
	}

	public String param(final String key)
	{
		return (parameterMap.containsKey(key) ? (String)parameterMap.get(key) : "");
	}

	protected String replaceInContent(final String template, final Map parameterMap)
	{

		if(template != null)
		{

			StringBuilder content = new StringBuilder(template);

			int start = content.indexOf(KEY_PREFIX);
			while(start > -1)
			{

				int end = content.indexOf(KEY_SUFFIX, start + KEY_PREFIX.length());

				if(end < 0)
				{
					logger.log(Level.WARNING, "Key suffix {0} not found in template", new Object[]
						{
							KEY_SUFFIX
						});
					break;
				}

				String key = content.substring(start + KEY_PREFIX.length(), end);

				StringBuilder replacement = new StringBuilder();

				replacement.append(parameterMap.get(key));


				String replaceBy = replacement.toString();

				content.replace(start, end
					+ KEY_SUFFIX.length(), replaceBy);
				// avoid replacing in the replacement again
				start = content.indexOf(KEY_PREFIX, start + replaceBy.length() + 1);
			}

			return content.toString();

		} else
		{
			return null;
		}
	}
}
