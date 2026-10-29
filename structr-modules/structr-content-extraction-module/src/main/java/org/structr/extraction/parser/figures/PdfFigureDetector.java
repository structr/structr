/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.extraction.parser.figures;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.structr.extraction.api.document.ExtractedFigureData;
import org.structr.extraction.api.document.FigureSource;
import org.structr.extraction.api.options.FigureOptions;
import org.structr.extraction.parser.figures.FigureCaptions.Caption;
import org.structr.extraction.parser.figures.PdfPageGraphics.Element;
import org.structr.extraction.parser.figures.PdfTextLines.Line;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the figures of a PDF and renders them.
 *
 * A figure is a region of a page where something is drawn: an image, or many paths, as a chart drawn
 * as vector graphics consists of. Elements that are close to each other form one region, and the text
 * in and right next to it (axis labels, legends, the cells of a table between its rules) is added to it.
 * A caption ("Figure 3: ...") below or (for tables) above the region gives it its label. A region without
 * a caption is kept if it is large enough and contains an image or many paths; a caption without a region
 * (e.g. a table without rules) stands for its whole page.
 *
 * What is drawn at the same place on several pages (the header bar of slides, a logo, separator lines) is
 * the template of the pages and is left out before, so it is not joined with the figures next to it.
 *
 * Regions are rendered from the page, so a figure looks as it does in the document, whatever it consists of.
 */
final class PdfFigureDetector {

	/** Drawn elements this close (in points) belong to the same figure. */
	private static final double ELEMENT_GAP = 6;

	/** Text this close to a figure belongs to it, e.g. axis labels. */
	private static final double TEXT_MARGIN = 8;

	/** Maximum distance between a figure and its caption; a caption without punctuation must be closer. */
	private static final double CAPTION_DISTANCE      = 72;
	private static final double WEAK_CAPTION_DISTANCE = 24;

	/** Rules of a table this far apart are joined, so the table is one figure. */
	private static final double RULE_GAP = 40;

	/** Space around a figure when it is rendered. */
	private static final double CROP_MARGIN = 4;

	/** A figure without a caption needs an image or at least this many drawn elements. */
	private static final int MIN_ELEMENTS = 6;

	/** A figure without a caption at the same place on this many pages is a logo or a page decoration. */
	private static final int REPEATED_PAGES = 3;

	/** A page with more captions than this and no figures to them is a list of figures. */
	private static final int MAX_CAPTIONS_WITHOUT_FIGURES = 3;

	private PdfFigureDetector() {
	}

	static List<ExtractedFigureData> detect(final Path path, final FigureOptions options) throws IOException {

		try (final PDDocument document = Loader.loadPDF(path.toFile())) {

			final int pages                         = Math.min(document.getNumberOfPages(), options.getMaxPages());
			final Map<Integer, List<Line>> lines    = PdfTextLines.read(document, pages);
			final Map<Integer, List<Element>> drawn = new HashMap<>();
			final List<Candidate> candidates        = new ArrayList<>();
			final Set<Line> figureLines             = new HashSet<>();

			for (int number = 1; number <= pages; number++) {

				drawn.put(number, PdfPageGraphics.collect(document.getPage(number - 1)));
			}

			removeTemplate(drawn);

			for (int number = 1; number <= pages; number++) {

				final List<Line> pageLines = lines.getOrDefault(number, List.of());

				candidates.addAll(detectOnPage(number, document.getPage(number - 1), drawn.get(number), pageLines, options, figureLines));
			}

			removeRepeated(candidates);

			candidates.sort(Comparator.comparingInt(Candidate::page).thenComparingDouble(c -> c.box().y0()).thenComparingDouble(c -> c.box().x0()));

			final List<Candidate> kept = candidates.subList(0, Math.min(candidates.size(), options.getMaxFigures()));
			final String text          = bodyText(lines, figureLines);

			return render(document, kept, text, options);
		}
	}

	// ----- private methods -----
	/**
	 * The figures of a page. The lines of text that belong to them (captions, labels, table cells) are
	 * added to {@code figureLines}.
	 */
	private static List<Candidate> detectOnPage(final int number, final PDPage page, final List<Element> elements, final List<Line> lines, final FigureOptions options, final Set<Line> figureLines) {

		final PDRectangle crop         = page.getCropBox();
		final boolean turned           = Math.floorMod(page.getRotation(), 180) == 90;
		final Box pageBox              = turned ? new Box(0, 0, crop.getHeight(), crop.getWidth()) : new Box(0, 0, crop.getWidth(), crop.getHeight());
		final List<CaptionBlock> captions = captions(lines, options.isIncludeTables());
		final Set<Line> inCaptions     = new HashSet<>();

		for (final CaptionBlock caption : captions) {

			inCaptions.addAll(caption.lines());
		}

		final List<Cluster> clusters = cluster(elements);

		joinRules(clusters, lines, inCaptions);
		addText(clusters, lines, inCaptions);

		// text added to a region can bring it close to another one, e.g. the last row of a table to its bottom rule
		mergeOverlapping(clusters, ELEMENT_GAP);

		final Map<Cluster, CaptionBlock> assigned = assignCaptions(clusters, captions);
		final List<Candidate> result              = new ArrayList<>();

		for (final Cluster cluster : clusters) {

			final CaptionBlock caption = assigned.get(cluster);
			final Box box              = cluster.box.clip(pageBox.x1(), pageBox.y1());

			if (caption != null) {

				result.add(new Candidate(number, box, FigureSource.REGION, caption, List.copyOf(cluster.lines)));
				figureLines.addAll(caption.lines());
				figureLines.addAll(cluster.lines);

			} else if (box.width() >= options.getMinFigureSize() && box.height() >= options.getMinFigureSize() && (cluster.images > 0 || cluster.elements >= MIN_ELEMENTS)) {

				result.add(new Candidate(number, box, FigureSource.REGION, null, List.copyOf(cluster.lines)));
				figureLines.addAll(cluster.lines);
			}
		}

		final List<CaptionBlock> withoutFigure = captions.stream().filter(c -> c.caption().strong() && !assigned.containsValue(c)).toList();
		if (withoutFigure.size() <= MAX_CAPTIONS_WITHOUT_FIGURES) {

			for (final CaptionBlock caption : withoutFigure) {

				result.add(new Candidate(number, pageBox, FigureSource.PAGE, caption, List.of()));
				figureLines.addAll(caption.lines());
			}
		}

		return result;
	}

	/**
	 * The captions on a page: a line that starts with a label, and the lines that continue it.
	 */
	private static List<CaptionBlock> captions(final List<Line> lines, final boolean includeTables) {

		final List<CaptionBlock> captions = new ArrayList<>();

		for (int i = 0; i < lines.size(); i++) {

			final Caption caption = FigureCaptions.parse(lines.get(i).text());
			if (caption == null || (caption.table() && !includeTables)) {

				continue;
			}

			final List<Line> block = new ArrayList<>(List.of(lines.get(i)));
			Box box                = lines.get(i).box();

			for (int j = i + 1; j < lines.size() && block.size() < 12; j++) {

				final Line previous = block.getLast();
				final Line next     = lines.get(j);
				final double gap    = next.box().y0() - previous.box().y1();

				if (gap > previous.height() * 1.2 + 2 || gap < -previous.height() || next.box().horizontalOverlap(box) < 0.5 || FigureCaptions.parse(next.text()) != null) {

					break;
				}

				block.add(next);
				box = box.union(next.box());
			}

			captions.add(new CaptionBlock(caption, block, box, joinLines(block)));

			i += block.size() - 1;
		}

		return captions;
	}

	/**
	 * Groups drawn elements that touch or are close to each other.
	 */
	private static List<Cluster> cluster(final List<Element> elements) {

		final List<Cluster> clusters = new ArrayList<>();

		for (final Element element : elements) {

			final Box grown = element.box().expand(ELEMENT_GAP);
			Cluster target  = null;

			for (int i = 0; i < clusters.size(); i++) {

				final Cluster cluster = clusters.get(i);
				if (cluster.box.intersects(grown)) {

					if (target == null) {

						target = cluster;
						target.add(element);

					} else {

						target.merge(cluster);
						clusters.remove(i--);
					}
				}
			}

			if (target == null) {

				clusters.add(new Cluster(element));
			}
		}

		mergeOverlapping(clusters, 0);

		return clusters;
	}

	/**
	 * The horizontal rules of a table are separated by rows of text, so they do not touch: rules (and
	 * regions) above each other with about the same width are joined if they are close, or if only rows
	 * of the table are between them, i.e. text that stays within their width and no caption.
	 */
	private static void joinRules(final List<Cluster> clusters, final List<Line> lines, final Set<Line> captionLines) {

		boolean changed = true;

		while (changed) {

			changed = false;

			outer:

			for (int i = 0; i < clusters.size(); i++) {

				for (int j = i + 1; j < clusters.size(); j++) {

					final Cluster a = clusters.get(i);
					final Cluster b = clusters.get(j);

					if ((a.isRule() || b.isRule()) && a.box.horizontalOverlap(b.box) >= 0.8 && Math.abs(a.box.width() - b.box.width()) <= 0.2 * Math.max(a.box.width(), b.box.width())
						&& (verticalGap(a.box, b.box) <= RULE_GAP || onlyRowsBetween(a.box, b.box, lines, captionLines))) {

						a.merge(b);
						clusters.remove(j);
						changed = true;

						break outer;
					}
				}
			}
		}
	}

	private static boolean onlyRowsBetween(final Box a, final Box b, final List<Line> lines, final Set<Line> captionLines) {

		final Box upper   = a.y0() <= b.y0() ? a : b;
		final Box lower   = upper == a ? b : a;
		final Box between = new Box(Math.min(a.x0(), b.x0()), upper.y1(), Math.max(a.x1(), b.x1()), lower.y0());
		int rows          = 0;

		if (between.height() <= 0) {

			return true;
		}

		for (final Line line : lines) {

			if (line.box().y1() <= between.y0() || line.box().y0() >= between.y1()) {

				continue;
			}

			if (line.box().x1() < between.x0() || line.box().x0() > between.x1()) {

				// text in another column
				continue;
			}

			if (captionLines.contains(line) || line.box().x0() < between.x0() - TEXT_MARGIN || line.box().x1() > between.x1() + TEXT_MARGIN) {

				return false;
			}

			rows++;
		}

		return rows > 0;
	}

	/**
	 * Adds the text in and right next to each region to it, e.g. axis labels and legends of a chart, or
	 * the cells of a table. Text next to a region is added if it does not reach out of it sideways, or if
	 * it is short like a label, so a paragraph of the running text next to a figure stays out. Captions are
	 * never added.
	 */
	private static void addText(final List<Cluster> clusters, final List<Line> lines, final Set<Line> captionLines) {

		final Set<Line> used = new HashSet<>(captionLines);
		boolean changed      = true;

		while (changed) {

			changed = false;

			for (final Cluster cluster : clusters) {

				final Box zone = cluster.box.expand(TEXT_MARGIN);

				for (final Line line : lines) {

					if (used.contains(line)) {

						continue;
					}

					final boolean inside = line.box().fractionInside(cluster.box) >= 0.5;
					final boolean beside = line.box().intersects(zone) && line.box().x0() >= zone.x0() && line.box().x1() <= zone.x1();
					final boolean label  = line.box().intersects(zone) && line.box().width() <= Math.max(60, 0.3 * cluster.box.width());

					if (inside || beside || label) {

						cluster.box = cluster.box.union(line.box());
						cluster.lines.add(line);
						used.add(line);
						changed = true;
					}
				}
			}
		}
	}

	private static void mergeOverlapping(final List<Cluster> clusters, final double gap) {

		boolean changed = true;

		while (changed) {

			changed = false;

			outer:

			for (int i = 0; i < clusters.size(); i++) {

				for (int j = i + 1; j < clusters.size(); j++) {

					if (clusters.get(i).box.expand(gap).intersects(clusters.get(j).box)) {

						clusters.get(i).merge(clusters.get(j));
						clusters.remove(j);
						changed = true;

						break outer;
					}
				}
			}
		}
	}

	/**
	 * Gives each caption the region it belongs to: the nearest one above it (a table's caption: below it),
	 * or on the other side if there is none. The closest pairs are matched first, each region gets at most
	 * one caption.
	 */
	private static Map<Cluster, CaptionBlock> assignCaptions(final List<Cluster> clusters, final List<CaptionBlock> captions) {

		final List<Pair> pairs = new ArrayList<>();

		for (final CaptionBlock caption : captions) {

			final double limit = caption.caption().strong() ? CAPTION_DISTANCE : WEAK_CAPTION_DISTANCE;

			for (final Cluster cluster : clusters) {

				if (cluster.box.horizontalOverlap(caption.box()) < 0.3) {

					continue;
				}

				final double captionBelow = caption.box().y0() - cluster.box.y1();
				final double captionAbove = cluster.box.y0() - caption.box().y1();
				final double preferred    = caption.caption().table() ? captionAbove : captionBelow;
				final double other        = caption.caption().table() ? captionBelow : captionAbove;

				if (preferred >= -TEXT_MARGIN && preferred <= limit) {

					pairs.add(new Pair(cluster, caption, Math.max(0, preferred)));

				} else if (other >= -TEXT_MARGIN && other <= limit) {

					pairs.add(new Pair(cluster, caption, Math.max(0, other) + limit));
				}
			}
		}

		pairs.sort(Comparator.comparingDouble(Pair::distance));

		final Map<Cluster, CaptionBlock> assigned = new LinkedHashMap<>();
		final Set<CaptionBlock> taken             = new HashSet<>();

		for (final Pair pair : pairs) {

			if (!assigned.containsKey(pair.cluster()) && !taken.contains(pair.caption())) {

				assigned.put(pair.cluster(), pair.caption());
				taken.add(pair.caption());
			}
		}

		return assigned;
	}

	/**
	 * Removes what is drawn at the same place on several pages: the template of the pages, e.g. the header
	 * bar and the separator lines of slides, or a logo. Otherwise it would be joined with the figures
	 * next to it, which differ from page to page.
	 */
	private static void removeTemplate(final Map<Integer, List<Element>> drawn) {

		final Map<String, Set<Integer>> pagesByPlace = new HashMap<>();

		for (final Map.Entry<Integer, List<Element>> page : drawn.entrySet()) {

			for (final Element element : page.getValue()) {

				pagesByPlace.computeIfAbsent(place(element.box(), 2), k -> new HashSet<>()).add(page.getKey());
			}
		}

		for (final List<Element> elements : drawn.values()) {

			elements.removeIf(element -> pagesByPlace.get(place(element.box(), 2)).size() >= REPEATED_PAGES);
		}
	}

	/**
	 * Figures without a caption at the same place on several pages are logos, headers or footers.
	 */
	private static void removeRepeated(final List<Candidate> candidates) {

		final Map<String, Set<Integer>> pagesByPlace = new HashMap<>();

		for (final Candidate candidate : candidates) {

			if (candidate.caption() == null) {

				pagesByPlace.computeIfAbsent(place(candidate.box(), 5), k -> new HashSet<>()).add(candidate.page());
			}
		}

		candidates.removeIf(c -> c.caption() == null && pagesByPlace.get(place(c.box(), 5)).size() >= REPEATED_PAGES);
	}

	private static List<ExtractedFigureData> render(final PDDocument document, final List<Candidate> candidates, final String text, final FigureOptions options) throws IOException {

		final PDFRenderer renderer             = new PDFRenderer(document);
		final List<ExtractedFigureData> result = new ArrayList<>();
		final double scale                     = options.getDpi() / 72.0;
		BufferedImage pageImage                = null;
		int renderedPage                       = -1;

		for (final Candidate candidate : candidates) {

			if (candidate.page() != renderedPage) {

				pageImage    = renderer.renderImageWithDPI(candidate.page() - 1, options.getDpi(), ImageType.RGB);
				renderedPage = candidate.page();
			}

			final BufferedImage image = candidate.source() == FigureSource.PAGE ? pageImage : crop(pageImage, candidate.box().expand(CROP_MARGIN), scale);
			if (image == null) {

				continue;
			}

			final byte[] png       = FigureImages.png(image);
			final Caption caption  = candidate.caption() != null ? candidate.caption().caption() : null;
			final List<String> mentions = caption != null ? FigureMentions.find(text, caption) : List.of();

			final String captionText    = candidate.caption() != null ? candidate.caption().text() : null;

			result.add(new ExtractedFigureData(candidate.page(), result.size(), caption != null ? caption.label() : null, captionText,
				candidate.source(), candidate.box().toArray(), image.getWidth(), image.getHeight(), FigureImages.MEDIA_TYPE, png, FigureImages.sha256(png), mentions, figureText(captionText, candidate.lines())));
		}

		return result;
	}

	private static BufferedImage crop(final BufferedImage page, final Box box, final double scale) {

		final int x0 = Math.max(0, (int) Math.floor(box.x0() * scale));
		final int y0 = Math.max(0, (int) Math.floor(box.y0() * scale));
		final int x1 = Math.min(page.getWidth(), (int) Math.ceil(box.x1() * scale));
		final int y1 = Math.min(page.getHeight(), (int) Math.ceil(box.y1() * scale));

		if (x1 - x0 < 1 || y1 - y0 < 1) {

			return null;
		}

		// a copy, so the page image can be released while the figure is kept
		final BufferedImage copy  = new BufferedImage(x1 - x0, y1 - y0, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = copy.createGraphics();

		try {

			graphics.drawImage(page.getSubimage(x0, y0, x1 - x0, y1 - y0), 0, 0, null);

		} finally {

			graphics.dispose();
		}

		return copy;
	}

	/**
	 * The text of the pages without the text of the figures and their captions, to find the sentences
	 * that refer to a figure.
	 */
	private static String bodyText(final Map<Integer, List<Line>> lines, final Set<Line> figureLines) {

		final StringBuilder text = new StringBuilder();

		for (final Integer page : lines.keySet().stream().sorted().toList()) {

			final List<Line> body = lines.get(page).stream().filter(line -> !figureLines.contains(line)).toList();

			text.append(joinLines(body)).append("\n\n");
		}

		return text.toString();
	}

	/**
	 * What a figure says in words: its caption, then the lines inside it (axis labels, legends, table
	 * cells) from top to bottom and left to right, one line each.
	 */
	private static String figureText(final String caption, final List<Line> lines) {

		final List<String> parts = new ArrayList<>();

		if (caption != null && !caption.isBlank()) {

			parts.add(caption);
		}

		lines.stream()
			.sorted(Comparator.comparingDouble((Line line) -> line.box().y0()).thenComparingDouble(line -> line.box().x0()))
			.map(Line::text)
			.filter(line -> !line.isBlank())
			.forEach(parts::add);

		return String.join("\n", parts);
	}

	/**
	 * Lines joined into one text, with words hyphenated at the end of a line put together again.
	 */
	private static String joinLines(final List<Line> lines) {

		final StringBuilder text = new StringBuilder();

		for (final Line line : lines) {

			final String value = line.text();
			if (text.length() > 1 && text.charAt(text.length() - 1) == '-' && Character.isLetter(text.charAt(text.length() - 2)) && !value.isEmpty() && Character.isLowerCase(value.charAt(0))) {

				text.setLength(text.length() - 1);

			} else if (!text.isEmpty()) {

				text.append(' ');
			}

			text.append(value);
		}

		return text.toString();
	}

	private static double verticalGap(final Box a, final Box b) {

		return Math.max(0, Math.max(a.y0(), b.y0()) - Math.min(a.y1(), b.y1()));
	}

	/**
	 * Where a box is, rounded to the given number of points.
	 */
	private static String place(final Box box, final double precision) {

		return Math.round(box.x0() / precision) + ":" + Math.round(box.y0() / precision) + ":" + Math.round(box.x1() / precision) + ":" + Math.round(box.y1() / precision);
	}

	/**
	 * A caption: the recognized label, the lines it consists of, their box and their text.
	 */
	private record CaptionBlock(Caption caption, List<Line> lines, Box box, String text) {
	}

	/**
	 * A figure found on a page, with its caption (if any) and the lines of text inside it.
	 */
	private record Candidate(int page, Box box, FigureSource source, CaptionBlock caption, List<Line> lines) {
	}

	private record Pair(Cluster cluster, CaptionBlock caption, double distance) {
	}

	/**
	 * Elements that belong together, with their box.
	 */
	private static final class Cluster {

		private final List<Line> lines = new ArrayList<>();
		private Box box;
		private int elements;
		private int images;

		Cluster(final Element element) {

			this.box = element.box();

			count(element);
		}

		void add(final Element element) {

			box = box.union(element.box());

			count(element);
		}

		void merge(final Cluster other) {

			lines.addAll(other.lines);

			box       = box.union(other.box);
			elements += other.elements;
			images   += other.images;
		}

		/**
		 * A thin horizontal line, as tables have above and below their rows.
		 */
		boolean isRule() {

			return box.height() <= 2.5 && box.width() >= 40;
		}

		private void count(final Element element) {

			elements++;

			if (element.image()) {

				images++;
			}
		}
	}
}
