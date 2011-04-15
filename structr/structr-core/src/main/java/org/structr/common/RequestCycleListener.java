/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.common;

/**
 *
 * @author chrisi
 */
public interface RequestCycleListener
{
	public void onRequestStart();
	public void onRequestEnd();
}
