package org.structr.cloud;

/**
 *
 * @author Christian Morgner
 */


public class AuthenticationContainer implements Message {

	private String userName = null;

	public AuthenticationContainer() {}

	public AuthenticationContainer(String userName) {

		this.userName = userName;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Override
	public Message process(final ServerContext context) {

		if (context.authenticateUser(userName)) {

			return this;
		}

		context.endTransaction();
		context.closeConnection();

		return null;
	}
}
