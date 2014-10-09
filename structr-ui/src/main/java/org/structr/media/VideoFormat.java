package org.structr.media;

/**
 * An enumeration of pre-defined video formats to be used with a VideoConverter.
 *
 * @author Christian Morgner
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
