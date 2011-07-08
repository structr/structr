package org.structr.core.cloud;

import java.io.Serializable;

/**
 *
 * @author Christian Morgner
 */


public class AuthenticationContainer implements Serializable {
	
	private String userName = null;
	
	public AuthenticationContainer() {
	}
	
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
}
