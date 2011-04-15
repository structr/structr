/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.app;

import org.structr.common.SessionValue;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.structr.common.SessionContext;
import org.structr.context.SessionMonitor;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.entity.app.slots.NullSlot;
import org.structr.core.entity.app.slots.StringSlot;
import org.structr.core.node.FindUserCommand;

/**
 *
 * @author Christian Morgner
 */
public class AppLogin extends ActiveNode
{
	private static final Logger logger = Logger.getLogger(AppLogin.class.getName());

	protected final static int DefaultMaxErrors =		1000;
	protected final static int DefaultDelayThreshold =	100;
	protected final static int DefaultDelayTime =		3;

	public final static String USERNAME_FIELD_NAME =	"username";
	public final static String PASSWORD_FIELD_NAME =	"password";
	private final static String ANTI_ROBOT_FIELD_NAME =	"antiRobot";

	private final static String MAX_ERRORS_KEY =		"maxRetries";
	private final static String DELAY_THRESHOLD_KEY =	"delayThreshold";
	private final static String DELAY_TIME_KEY =		"delayTime";

	private final static String LOGIN_FAILURE_TEXT = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";

	// non-final variables
	private SessionValue<Boolean> sessionBlockedValue = null;
	private SessionValue<Integer> loginAttemptsValue = null;
	private SessionValue<String> errorMessageValue = null;
	private SessionValue<String> okMessageValue = null;
	private SessionValue<String> userNameValue = null;

	@Override
	public boolean execute(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		HttpSession session = SessionContext.getSession();
		String usernameFromSession = getUserNameValue().get();
		Boolean alreadyLoggedIn = usernameFromSession != null;

		if(alreadyLoggedIn)
		{
			return(true);
		}

		Boolean sessionBlocked = getSessionBlockedValue().get();

		if(Boolean.TRUE.equals(sessionBlocked))
		{
			getErrorMessageValue().set("Too many login attempts, session is blocked for login");

			return(false);
		}

		String username = (String)getValue(USERNAME_FIELD_NAME);
		String password = (String)getValue(PASSWORD_FIELD_NAME);
		String antiRobot = (String)getValue(ANTI_ROBOT_FIELD_NAME);

		int maxRetries = getMaxErrors() == 0 ? DefaultMaxErrors : getMaxErrors();
		int delayThreshold = getDelayThreshold() == 0 ? DefaultDelayThreshold : getDelayThreshold();
		int delayTime = getDelayTime() == 0 ? DefaultDelayTime : getDelayTime();

		if(StringUtils.isNotEmpty(antiRobot))
		{
			// Don't process form if someone has filled the anti-robot field
			return(false);
		}

		if(StringUtils.isEmpty(username))
		{
			setErrorValue(USERNAME_FIELD_NAME, "You must enter a username");
			countLoginFailure(maxRetries, delayThreshold, delayTime);

			return(false);
		}

		if(StringUtils.isEmpty(password))
		{
			setErrorValue(PASSWORD_FIELD_NAME, "You must enter a password");
			countLoginFailure(maxRetries, delayThreshold, delayTime);

			return(false);
		}

		// Session is not blocked, and we have a username and a password

		// First, check if we have a user with this name
		User loginUser = (User)Services.command(FindUserCommand.class).execute(username);

		// No matter what reason to deny login, always show the same error message to
		// avoid giving hints
		getErrorMessageValue().set(LOGIN_FAILURE_TEXT);

		if(loginUser == null)
		{
			logger.log(Level.INFO, "No user found for name {0}", loginUser);
			countLoginFailure(maxRetries, delayThreshold, delayTime);

			return(false);
		}

		// From here, we have a valid user

		if(loginUser.isBlocked())
		{
			logger.log(Level.INFO, "User {0} is blocked", loginUser);
			countLoginFailure(maxRetries, delayThreshold, delayTime);

			return(false);
		}

		// Check password
		String encryptedPasswordValue = DigestUtils.sha512Hex(password);
		if(!encryptedPasswordValue.equals(loginUser.getPassword()))
		{
			logger.log(Level.INFO, "Wrong password for user {0}", loginUser);
			countLoginFailure(maxRetries, delayThreshold, delayTime);

			return(false);
		}

		// Username and password are both valid
		getUserNameValue().set(loginUser.getName());

		// Register user with internal session management
		long sessionId = SessionMonitor.registerUserSession(user, session);
		SessionMonitor.logActivity(user, sessionId, "Login");

		// Mark this session with the internal session id
		session.setAttribute(SessionMonitor.SESSION_ID, sessionId);

		// Clear all blocking stuff
		getSessionBlockedValue().set(false);
		getLoginAttemptsValue().set(0);
		getErrorMessageValue().set(null);

		// set success message
		getOkMessageValue().set("Login successful.");

		// finally, return success
		return (true);
	}

	@Override
	public String getIconSrc()
	{
		return ("/images/door_in.png");
	}

	@Override
	public Map<String, Slot> getSlots()
	{
		Map<String, Slot> ret = new LinkedHashMap<String, Slot>();

		ret.put(USERNAME_FIELD_NAME, new StringSlot());
		ret.put(PASSWORD_FIELD_NAME, new StringSlot());
		ret.put(ANTI_ROBOT_FIELD_NAME, new NullSlot(String.class));

		return (ret);
	}

	// ----- getter & setter
	/**
	 * Return number of unsuccessful login attempts for a session
	 *
	 * If number is exceeded, login is blocked for this session.
	 */
	public int getMaxErrors()
	{
		return getIntProperty(MAX_ERRORS_KEY);
	}

	/**
	 * Return number of unsuccessful login attempts before retry delay becomes active
	 */
	public int getDelayThreshold()
	{
		return getIntProperty(DELAY_THRESHOLD_KEY);
	}

	/**
	 * Return time (in seconds) to wait for retry after an unsuccessful login attempt
	 */
	public int getDelayTime()
	{
		return getIntProperty(DELAY_TIME_KEY);
	}

	// ----- private methods -----
	private void countLoginFailure(int maxRetries, int delayThreshold, int delayTime)
	{
		HttpSession session = SessionContext.getSession();
		Integer retries = getLoginAttemptsValue().get();

		if(retries != null && retries > maxRetries)
		{

			logger.log(Level.SEVERE, "More than {0} login failures, session {1} is blocked", new Object[]
				{
					maxRetries, session.getId()
				});

			getSessionBlockedValue().set(true);
			getErrorMessageValue().set("Too many unsuccessful login attempts, your session is blocked for login!");

		} else if(retries != null && retries > delayThreshold)
		{

			logger.log(Level.INFO, "More than {0} login failures, execution delayed by {1} seconds", new Object[]
				{
					maxRetries, delayTime
				});

			try
			{
				Thread.sleep(delayTime * 1000);

			} catch(InterruptedException ex)
			{
				logger.log(Level.SEVERE, null, ex);
			}

			StringBuilder errorBuffer = new StringBuilder(200);
			errorBuffer.append("You had more than ");
			errorBuffer.append(maxRetries);
			errorBuffer.append(" unsuccessful login attempts, execution was delayed by ");
			errorBuffer.append(delayTime);
			errorBuffer.append(" seconds.");

			getErrorMessageValue().set(errorBuffer.toString());


		} else if(retries != null)
		{
			getLoginAttemptsValue().set(getLoginAttemptsValue().get() + 1);

		} else
		{
			getLoginAttemptsValue().set(1);
		}

	}
	
	private SessionValue<String> getUserNameValue()
	{
		if(userNameValue == null)
		{
			userNameValue = new SessionValue<String>(createUniqueIdentifier("userName"));
		}
		
		return(userNameValue);
	}

	private SessionValue<Integer> getLoginAttemptsValue()
	{
		if(loginAttemptsValue == null)
		{
			// create new session value with default 0
			loginAttemptsValue = new SessionValue<Integer>(createUniqueIdentifier("loginAttempts"), 0);
		}

		return (loginAttemptsValue);
	}

	private SessionValue<Boolean> getSessionBlockedValue()
	{
		if(sessionBlockedValue == null)
		{
			// create new session value with default false
			sessionBlockedValue = new SessionValue<Boolean>(createUniqueIdentifier("sessionBlocked"), false);
		}

		return (sessionBlockedValue);
	}

	private SessionValue<String> getErrorMessageValue()
	{
		if(errorMessageValue == null)
		{
			// create new session value with default ""
			errorMessageValue = new SessionValue<String>("errorMessage", "");
		}

		return (errorMessageValue);
	}

	private SessionValue<String> getOkMessageValue()
	{
		if(okMessageValue == null)
		{
			// create new session value with default ""
			okMessageValue = new SessionValue<String>("okMessage", "");
		}

		return (okMessageValue);
	}
}

