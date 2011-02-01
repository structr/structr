/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core;

/**
 * Thrown when a command does not know how to handle a parameter to
 * its {@see Command.execute} method.
 *
 * @author cmorgner
 */
public class UnsupportedArgumentError extends Error
{
	public UnsupportedArgumentError(String message)
	{
		super(message);
	}

	public UnsupportedArgumentError(Throwable cause)
	{
		super(cause);
	}
	public UnsupportedArgumentError(String message, Throwable cause)
	{
		super(message, cause);
	}
}
