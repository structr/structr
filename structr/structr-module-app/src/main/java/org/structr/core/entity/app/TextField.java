/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.app;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.structr.common.Callback;
import org.structr.common.StructrContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class TextField extends FormField implements InteractiveNode, Callback
{
	private static final Logger logger = Logger.getLogger(TextField.class.getName());

	protected SessionValue<String> errorMessage = null;
	protected SessionValue<String> sessionValue = null;
	private String mappedName = null;

	public TextField()
	{
		// register callback for resetting session values after request
//		StructrContext.registerCallback(this);

		// callback doesnt work, values should be reset after the request
		// that follows the redirect..
		// maybe we need a second variable level
	}

	@Override
	public String getIconSrc()
	{
		return "/images/textfield.png";
	}

	@Override
	public void renderView(final StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
	{
		// if this page is requested to be edited, render edit frame
		if(editNodeId != null && getId() == editNodeId.longValue())
		{

			renderEditFrame(out, editUrl);

			// otherwise, render subnodes in edit mode
		} else
		{

			if(hasTemplate(user))
			{
				template.setCallingNode(this);
				template.renderView(out, startNode, editUrl, editNodeId, user);

			} else
			{
				logger.log(Level.WARNING, "Encountered TextField without template: {0}", this);

				// TODO: default template for TextField?
			}
		}
	}

	// ----- interface InteractiveNode -----
	@Override
	public String getValue()
	{
		HttpServletRequest request = StructrContext.getRequest();
		String name = getName();
		String ret = null;

		String valueFromLastRequest = getLastValue().get();
		logger.log(Level.INFO, "Got " + "last_" + "{0}: {1}", new Object[]
		{
			name, valueFromLastRequest
		});

		if(request == null)
		{
			return valueFromLastRequest;
		}

		if(request != null)
		{

			ret = request.getParameter(name);

			if(ret != null)
			{

				// Parameter is there

				if(ret.length() == 0)
				{

					// Empty value

					return null;

				} else
				{
					getLastValue().set(ret);

					return ret;
				}

			} else
			{
				// Parameter is not in request
				return valueFromLastRequest;
			}

		}

		return null;
	}

	@Override
	public String getStringValue()
	{
		Object value = getValue();
		return (value != null ? value.toString() : null);
	}

	@Override
	public Class getParameterType()
	{
		return (String.class);
	}

	@Override
	public void setMappedName(String mappedName)
	{
		this.mappedName = mappedName;
	}

	@Override
	public String getMappedName()
	{
		if(StringUtils.isNotBlank(mappedName))
		{
			return (mappedName);
		}

		return (getName());
	}

	@Override
	public void setErrorValue(Object errorValue)
	{
		getErrorMessageValue().set(errorValue.toString());
	}

	// ----- interface Callback -----
	@Override
	public void callback()
	{
		// clear session value
		getLastValue().set(null);

		// reset error message
		getErrorMessageValue().set(null);
	}

	// ----- private methods -----
	private SessionValue<String> getErrorMessageValue()
	{
		if(errorMessage == null)
		{
			errorMessage = new SessionValue<String>(createUniqueIdentifier("errorMessage"));
		}

		return(errorMessage);
	}

	private SessionValue<String> getLastValue()
	{
		if(sessionValue == null)
		{
			sessionValue = new SessionValue<String>(createUniqueIdentifier("lastValue"));
		}

		return(sessionValue);
	}
}
