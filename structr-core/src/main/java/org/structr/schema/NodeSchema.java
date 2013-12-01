/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.schema;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public interface NodeSchema {

	public String getNodeSource(final ErrorBuffer errorBuffer) throws FrameworkException;
	public String getClassName();
}
