/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core;

/**
 * Classes implementing this interface can be decorated by one or more
 * {@see org.structr.core.Decorator} instances in order to extend existing
 * functionality without the need to modify the code itself.
 *
 * @author Christian Morgner
 */
public interface Decorable<T>
{
	public void addDecorator(Decorator<T> d);
	public void removeDecorator(Decorator<T> d);
}
