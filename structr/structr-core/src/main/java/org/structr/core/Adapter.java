/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core;

/**
 *
 * @author Christian Morgner
 */
public interface Adapter<S, T>
{
	public T adapt(S s);
}
