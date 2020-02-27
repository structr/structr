/**
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.javaparser.test;

public abstract class SimpleTestClass {
        
    public int     anInt;
    public long    aLong;
    public boolean aBoolean;
	public String  aString;
        
    public SimpleTestClass(final int anInt, long aLong, boolean aBoolean, String aString) {
        this.anInt    = anInt;
		this.aLong    = aLong;
		this.aBoolean = aBoolean;
		this.aString  = aString;
    }
        
    public void setAnInt(int newValue) {
        anInt = newValue;
    }
        
    public void setALong(long newValue) {
        aLong = newValue;
    }
        
    public void setABoolean(boolean newValue) {
        aBoolean = newValue;
    }

    public void setAString(final String newValue) {
        aString = newValue;
    }
	
}