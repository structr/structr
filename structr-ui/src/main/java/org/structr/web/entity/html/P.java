/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity.html;

import org.structr.web.entity.dom.DOMElement;
import org.neo4j.graphdb.Direction;

import org.structr.web.common.RelType;
import org.structr.core.property.Endpoints;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class P extends DOMElement {

	public static final Endpoints<Content>  contents  = new Endpoints<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<A>        as        = new Endpoints<A>("as", A.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Abbr>     abbrs     = new Endpoints<Abbr>("abbrs", Abbr.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Area>     areas     = new Endpoints<Area>("areas", Area.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Audio>    audios    = new Endpoints<Audio>("audios", Audio.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<B>        bs        = new Endpoints<B>("bs", B.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Bdi>      bdis      = new Endpoints<Bdi>("bdis", Bdi.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Bdo>      bdos      = new Endpoints<Bdo>("bdos", Bdo.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Br>       brs       = new Endpoints<Br>("brs", Br.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Button>   buttons   = new Endpoints<Button>("buttons", Button.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Canvas>   canvases  = new Endpoints<Canvas>("canvases", Canvas.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Cite>     cites     = new Endpoints<Cite>("cites", Cite.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Code>     codes     = new Endpoints<Code>("codes", Code.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Command>  commands  = new Endpoints<Command>("commands", Command.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Datalist> datalists = new Endpoints<Datalist>("datalists", Datalist.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Del>      dels      = new Endpoints<Del>("dels", Del.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Dfn>      dfns      = new Endpoints<Dfn>("dfns", Dfn.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Em>       ems       = new Endpoints<Em>("ems", Em.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Embed>    embeds    = new Endpoints<Embed>("embeds", Embed.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<I>        is        = new Endpoints<I>("is", I.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Iframe>   iframes   = new Endpoints<Iframe>("iframes", Iframe.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Img>      imgs      = new Endpoints<Img>("imgs", Img.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Input>    inputs    = new Endpoints<Input>("inputs", Input.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Ins>      inss      = new Endpoints<Ins>("inss", Ins.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Kbd>      kbds      = new Endpoints<Kbd>("kbds", Kbd.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Keygen>   keygens   = new Endpoints<Keygen>("keygens", Keygen.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Label>    labels    = new Endpoints<Label>("labels", Label.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Map>      maps      = new Endpoints<Map>("maps", Map.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Mark>     marks     = new Endpoints<Mark>("marks", Mark.class, RelType.CONTAINS, Direction.OUTGOING, false);
// ???	public static final CollectionProperty<Math>     maths     = new CollectionProperty<Math>("maths", Math.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Meter>    meters    = new Endpoints<Meter>("meters", Meter.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Noscript> noscripts = new Endpoints<Noscript>("noscripts", Noscript.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Object>   objects   = new Endpoints<Object>("objects", Object.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Output>   outputs   = new Endpoints<Output>("outputs", Output.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Progress> progresss = new Endpoints<Progress>("progresss", Progress.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Q>        qs        = new Endpoints<Q>("qs", Q.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Ruby>     rubys     = new Endpoints<Ruby>("rubys", Ruby.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<S>        ss        = new Endpoints<S>("ss", S.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Samp>     samps     = new Endpoints<Samp>("samps", Samp.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Script>   scripts   = new Endpoints<Script>("scripts", Script.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Select>   selects   = new Endpoints<Select>("selects", Select.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Small>    smalls    = new Endpoints<Small>("smalls", Small.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Span>     spans     = new Endpoints<Span>("spans", Span.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Strong>   strongs   = new Endpoints<Strong>("strongs", Strong.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Sub>      subs      = new Endpoints<Sub>("subs", Sub.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Sup>      sups      = new Endpoints<Sup>("sups", Sup.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Textarea> textareas = new Endpoints<Textarea>("textareas", Textarea.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Time>     times     = new Endpoints<Time>("times", Time.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<U>        us        = new Endpoints<U>("us", U.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Var>      vars      = new Endpoints<Var>("vars", Var.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Video>    videos    = new Endpoints<Video>("videos", Video.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Wbr>      wbrs      = new Endpoints<Wbr>("wbrs", Wbr.class, RelType.CONTAINS, Direction.OUTGOING, false);
}
