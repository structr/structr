/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.schema;

import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public interface NodeSchema {

	public String getNodeSource() throws FrameworkException;
	public String getPackageName();
	public String getClassName();
}
