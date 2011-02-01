/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core;

import java.io.Serializable;
import java.util.Set;

/**
 *
 *
 * @author axel
 */
public interface Module extends Serializable
{
	public String getModulePath();

	public Set<String> getClasses();
	public Set<String> getProperties();
	public Set<String> getResources();
}
