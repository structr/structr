/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import org.structr.core.entity.AbstractNode;

/**
 *
 *
 * @author Christian Morgner
 */
public interface NodeSource
{
	public AbstractNode loadNode();
}
