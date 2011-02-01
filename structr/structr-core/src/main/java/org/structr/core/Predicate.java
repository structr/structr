/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core;

/**
 *
 * @author Christian Morgner
 */
public interface Predicate<T>
{
	public boolean evaluate(T obj);
}
