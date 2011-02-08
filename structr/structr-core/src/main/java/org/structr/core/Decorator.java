/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core;

/**
 * 
 *
 * @author Christian Morgner
 */
public interface Decorator<T>
{
	public void decorate(T t);
}
