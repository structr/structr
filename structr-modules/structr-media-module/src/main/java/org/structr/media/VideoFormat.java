/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.media;

/**
 * An enumeration of pre-defined video formats to be used with a VideoConverter.
 *
 *
 */
public enum VideoFormat {

	qqvga("160x120"),
	qvga("320x240"),
	vga("640x480"),
	svga("800x600"),
	xga("1024x768"),
	uxga("1600x1200"),
	qxga("2048x1536"),
	sxga("1280x1024"),
	qsxga("2560x2048"),
	hsxga("5120x4096"),
	wvga("852x480"),
	wxga("1366x768"),
	wsxga("1600x1024"),
	wuxga("1920x1200"),
	woxga("2560x1600"),
	wqsxga("3200x2048"),
	wquxga("3840x2400"),
	whsxga("6400x4096"),
	whuxga("7680x4800"),
	cga("320x200"),
	ega("640x350"),
	hd480("852x480"),
	hd720("1280x720"),
	hd1080("1920x1080");

	private String resolution = null;

	private VideoFormat(final String resolution) {
		this.resolution = resolution;
	}

	public String getResolution() {
		return resolution;
	}
}
