/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

/**
 *
 *
 * @author Christian Morgner
 */
public interface InteractiveNode
{
	public Class getParameterType();

	public String getName();
	public Object getValue();
}
