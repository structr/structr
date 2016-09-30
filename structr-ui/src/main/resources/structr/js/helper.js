/*
 * Copyright (C) 2010-2016 Structr GmbH
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
function lastPart(id, separator) {
	if (!separator) {
		separator = '_';
	}
	if (id) {
		return id.substring(id.lastIndexOf(separator) + 1);
	}
	return '';
}

function sortArray(arrayIn, sortBy) {
	var arrayOut = arrayIn.sort(function(a, b) {
		return sortBy.indexOf(a.id) > sortBy.indexOf(b.id);
	});
	return arrayOut;
}

function without(s, array) {
	if (!isIn(s, array)) {
		return array;
	}

	var res = [];
	$.each(array, function(i, el) {
		if (!(el === s)) {
			res.push(el);
		}
	});

	return res;

}

function isIn(s, array) {
	//console.log('is', s, 'in', array, '?', (array.indexOf(s) !== -1));
	return (s && array && array.indexOf(s) !== -1);
}

function escapeForHtmlAttributes(str, escapeWhitespace) {
	if (!(typeof str === 'string'))
		return str;
	var escapedStr = str
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;');

	return escapeWhitespace ? escapedStr.replace(/ /g, '&nbsp;') : escapedStr;
}

function escapeTags(str) {
	if (!str)
		return str;
	return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function unescapeTags(str) {
	if (!str)
		return str;
	return str
			.replace(/&nbsp;/g, ' ')
			.replace(/&amp;/g, '&')
			.replace(/&lt;/g, '<')
			.replace(/&gt;/g, '>')
			.replace(/&quot;/g, '"')
			.replace(/&#39;/g, '\'');
}

$.fn.reverse = [].reverse;

if (typeof String.prototype.endsWith !== 'function') {
	String.prototype.endsWith = function(pattern) {
		var d = this.length - pattern.length;
		return d >= 0 && this.lastIndexOf(pattern) === d;
	};
}

if (typeof String.prototype.startsWith !== 'function') {
	String.prototype.startsWith = function(str) {
		return this.indexOf(str) === 0;
	};
}

if (typeof String.prototype.capitalize !== 'function') {
	String.prototype.capitalize = function() {
		return this.charAt(0).toUpperCase() + this.slice(1);
	};
}

if (typeof String.prototype.lpad !== 'function') {
	String.prototype.lpad = function(padString, length) {
		var str = this;
		while (str.length < length)
			str = padString + str;
		return str;
	};
}

if (typeof String.prototype.contains !== 'function') {
	String.prototype.contains = function(pattern) {
		return this.indexOf(pattern) > -1;
	};
}

if (typeof String.prototype.splitAndTitleize !== 'function') {
	String.prototype.splitAndTitleize = function(sep) {

		var res = new Array();
		var parts = this.split(sep);
		parts.forEach(function(part) {
			res.push(part.capitalize());
		});
		return res.join(" ");
	};
}

if (typeof String.prototype.extractVal !== 'function') {
	String.prototype.extractVal = function(key) {
		var pattern = '(' + key + '=")(.*?)"';
		var re = new RegExp(pattern);
		var value = this.match(re);
		return value && value[2] ? value[2] : undefined;
	};
}
/**
 * Clean text from contenteditable
 *
 * This function will remove any HTML markup and convert
 * any <br> tag into a line feed ('\n').
 */
function cleanText(input) {
	if (typeof input !== 'string') {
		return input;
	}
	//console.log(input);
	var output = input
			.replace(/<div><br><\/div>/ig, '\n')
			.replace(/<div><\/div>/g, '\n')
			.replace(/<br(\s*)\/*>/ig, '\n')
			.replace(/(<([^>]+)>)/ig, "")
			.replace(/\u00A0/ig, String.fromCharCode(32))
	;

	//console.log(output);
	return output;

//    _Logger.consoleLog(input);
//    var output = '';
//    $(input).each(function(i, line) {
//        var cleaned = $(line).text();
//        console.log('>'+cleaned+'<');
//        output += cleaned + '\n';
//    });
//    console.log(output);
//    return output;
}

/**
 * Expand literal '\n' to newline,
 * which is encoded as '\\n' in HTML attribute values.
 */
function expandNewline(text) {
	var output = text.replace(/\\\\n/g, '<br>');
	return output;
}

var uuidRegexp = new RegExp('[a-fA-F0-9]{32}');
function isUUID (str) {
	return (str.length === 32 && uuidRegexp.test(str));
}

function shorten(uuid) {
	return uuid.substring(0, 8);
}

function urlParam(name) {
	name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
	var regexS = "[\\?&]" + name + "=([^&#]*)";
	var regex = new RegExp(regexS);
	var res = regex.exec(window.location.href);
	return (res && res.length ? res[1] : '');
}

function nvl(value, defaultValue) {
	var returnValue;
	if (value === undefined) {
		returnValue = defaultValue;
	} else if (value === false) {
		returnValue = 'false';
	} else if (value === 0) {
		returnValue = '0';
	} else if (!value) {
		returnValue = defaultValue;
	} else {
		returnValue = value;
	}
	return returnValue;
}

String.prototype.toCamel = function() {
	return this.replace(/(\-[a-z])/g, function(part) {
		return part.toUpperCase().replace('-', '');
	});
};

String.prototype.toUnderscore = function() {
	return this.replace(/([A-Z])/g, function(m, a, offset) {
		return (offset > 0 ? '_' : '') + m.toLowerCase();
	});
};

/**
 * Gratefully taken from https://gist.github.com/24261/7fdb113f1e26111bd78c0c6fe515f6c0bf418af5
 *
 * The method trims the given string 'str' to fit nicely within a box of 'len' px width
 * without line break.
 */
//function fitStringToSize(str,len) {
//    var result = str;
//    var span = document.createElement("span");
//    span.style.visibility = 'hidden';
//    span.style.padding = '0px';
//    document.body.appendChild(span);
//
//    // on first run, check if string fits into the length already.
//    span.innerHTML = result;
//    if (span.offsetWidth > len) {
//        var posStart = 0, posMid, posEnd = str.length;
//        while (true) {
//            // Calculate the middle position
//            posMid = posStart + Math.ceil((posEnd - posStart) / 2);
//            // Break the loop if this is the last round
//            if (posMid===posEnd || posMid===posStart) break;
//
//            span.innerHTML = str.substring(0,posMid) + '&hellip;';
//
//            // Test if the width at the middle position is
//            // too wide (set new end) or too narrow (set new start).
//            if ( span.offsetWidth > len ) posEnd = posMid; else posStart=posMid;
//        }
//
//        //Escape < and >, eliminate trailing space and a widow character if one is present.
//        result = str.substring(0,posStart).replace("<","&lt;").replace(">","&gt;").replace(/(\s.)?\s*$/,'') + '&hellip;';
//    }
//    document.body.removeChild(span);
//    return result;
//}

function fitStringToWidth(str, width, className) {
	// str    A string where html-entities are allowed but no tags.
	// width  The maximum allowed width in pixels
	// className  A CSS class name with the desired font-name and font-size. (optional)
	// ----
	// _escTag is a helper to escape 'less than' and 'greater than'
	function _escTag(s) {
		return s ? s.replace("<", "&lt;").replace(">", "&gt;"):'';
	}

	//Create a span element that will be used to get the width
	var span = document.createElement("span");
	//Allow a classname to be set to get the right font-size.
	if (className)
		span.className = className;
	span.style.display = 'inline';
	span.style.visibility = 'hidden';
	span.style.padding = '0px';
	document.body.appendChild(span);

	var result = _escTag(str); // default to the whole string
	span.innerHTML = result;
	// Check if the string will fit in the allowed width. NOTE: if the width
	// can't be determinated (offsetWidth==0) the whole string will be returned.
	if (span.offsetWidth > width) {
		var posStart = 0, posMid, posEnd = str.length, posLength;
		// Calculate (posEnd - posStart) integer division by 2 and
		// assign it to posLength. Repeat until posLength is zero.
		while (posLength = (posEnd - posStart) >> 1) {
			posMid = posStart + posLength;
			//Get the string from the begining up to posMid;
			span.innerHTML = _escTag(str.substring(0, posMid)) + '&hellip;';

			// Check if the current width is too wide (set new end)
			// or too narrow (set new start)
			if (span.offsetWidth > width)
				posEnd = posMid;
			else
				posStart = posMid;
		}

		result = '<abbr title="' +
				str.replace("\"", "&quot;") + '">' +
				_escTag(str.substring(0, posStart)) +
				'&hellip;<\/abbr>';
	}
	document.body.removeChild(span);
	return result;
}

function formatValue(value) {

	if (value === undefined || value === null) {
		return '';
	}

	if (value.constructor === Object) {

		var out = '';
		$(Object.keys(value)).each(function(i, k) {
			out += k + ': ' + formatValue(value[k]) + '\n';
		});
		return out;

	} else if (value.constructor === Array) {
		var out = '';
		$(value).each(function(i, v) {
			out += JSON.stringify(v);
		});
		return out;

	} else {
		return value;
	}
}

function getTypeFromResourceSignature(signature) {
	var i = signature.indexOf('/');
	if (i === -1)
		return signature;
	return signature.substring(0, i);
}

function blinkGreen(element) {

	blink(element, '#6db813', '#81ce25');
}

function blinkRed(element) {

	blink(element, '#a00', '#faa');
}

function blink (element, color, bgColor) {

	if (!element || !element.length) {
		return;
	}

	var fg = element.prop('data-fg-color'), oldFg = fg || element.css('color');
	var bg = element.prop('data-bg-color'), oldBg = bg || element.css('backgroundColor');

	if (!fg) {
		element.prop('data-fg-color', oldFg);
	}

	if (!bg) {
		element.prop('data-bg-color', oldBg);
	}

	element.animate({
		color: color,
		backgroundColor: bgColor
	}, 50, function() {
		$(this).animate({
			color: oldFg,
			backgroundColor: oldBg
		}, 1000);
	});
}

function getComments(el) {
	var comments = [];
	var f = el.firstChild;
	while (f) {
		if (f.nodeType === 8) {
			var id = f.nodeValue.extractVal('data-structr-id');
			var raw = f.nodeValue.extractVal('data-structr-raw-value');
			if (id) {
				var comment = {};
				comment.id = id;
				comment.node = f;
				comment.rawContent = raw;
				comments.push(comment);
			}
		}
		f = f ? f.nextSibling : f;
	}
	return comments;
}

function getNonCommentSiblings(el) {
	var siblings = [];
	var s = el.nextSibling;
	while (s) {
		if (s.nodeType === 8) {
			return siblings;
		}
		siblings.push(s);
		s = s.nextSibling;
	}
}

function pluralize(name) {
	return name.endsWith('y') ? name.substring(0, name.length - 1) + 'ies' : (name.endsWith('s') ? name : name + 's');
}

function getDateTimePickerFormat(rawFormat) {
	var dateTimeFormat, obj = {};
	if (rawFormat.indexOf('T') > 0) {
		dateTimeFormat = rawFormat.split('\'T\'');
	} else {
		dateTimeFormat = rawFormat.split(' ');
	}
	var dateFormat = dateTimeFormat[0], timeFormat = dateTimeFormat.length > 1 ? dateTimeFormat[1] : undefined;
	obj.dateFormat = DateFormatConverter.convert(moment().toMomentFormatString(dateFormat), DateFormatConverter.momentJs, DateFormatConverter.datepicker);
	var timeFormat = dateTimeFormat.length > 1 ? dateTimeFormat[1] : undefined;
	if (timeFormat) {
		obj.timeFormat = DateFormatConverter.convert(moment().toMomentFormatString(timeFormat), DateFormatConverter.momentJs, DateFormatConverter.timepicker);
	}
	obj.separator = (rawFormat.indexOf('T') > 0) ? 'T' : ' ';
	return obj;
}

function getElementDisplayName(entity) {
	if (!entity.name) {
		return (entity.tag ? entity.tag : '[' + entity.type + ']');
	}
	if (entity.name && $.isBlank(entity.name)) {
		return '(blank name)';
	}
	return entity.name;
}

jQuery.isBlank = function (obj) {
	if (!obj || $.trim(obj) === "") return true;
	if (obj.length && obj.length > 0) return false;

	for (var prop in obj) if (obj[prop]) return false;
	return true;
};

/**
 * thin wrapper for localStorage with a success-check and error display
 */

var localStorageObject = {};

var LSWrapper = {

	setItem: function(key, value) {
		localStorageObject[key] = value;
	},

	getItem: function (key) {
		return localStorageObject[key] || null;
	},

	removeItem: function (key) {
		delete localStorageObject[key];
	},

	clear: function () {
		localStorageObject = {};
	}

};

function fastRemoveAllChildren(el) {
	if (!el) return;
	var child;
	while ((child = el.firstChild)) {
		el.removeChild(child);
	}
}

/**
 * static list of all logtypes
 */
var _LogType = {
	CONTENTS:        "CONTENTS",
	CRAWLER:         "CRAWLER",
	CRUD:            "CRUD",
	DND:             "DND",
	ELEMENTS:        "ELEMENTS",
	ENTITIES:        "ENTITIES",
	FILES:           "FILES",
	FIND_DUPLICATES: "FIND_DUPLICATES",
	GRAPH:           "GRAPH",
	INIT:            "INIT",
	LOCALIZATION:    "LOCALIZATION",
	MODEL:           "MODEL",
	PAGER:           "PAGER",
	PAGES:           "PAGES",
	SCHEMA:          "SCHEMA",
	SECURTIY:        "SECURTIY",
	WEBSOCKET:       "WS",
	WIDGETS:         "WIDGETS",
	WS: {
		ADD:                        "WS.ADD",
		APPEND_CHILD:               "WS.APPEND_CHILD",
		APPEND_FILE:                "WS.APPEND_FILE",
		APPEND_USER:                "WS.APPEND_USER",
		APPEND_WIDGET:              "WS.APPEND_WIDGET",
		AUTOCOMPLETE:               "WS.AUTOCOMPLETE",
		CHILDREN:                   "WS.CHILDREN",
		CHUNK:                      "WS.CHUNK",
		CLONE_COMPONENT:            "WS.CLONE_COMPONENT",
		CLONE_NODE:                 "WS.CLONE_NODE",
		CLONE_PAGE:                 "WS.CLONE_PAGE",
		CREATE:                     "WS.CREATE",
		CREATE_AND_APPEND_DOM_NODE: "WS.CREATE_AND_APPEND_DOM_NODE",
		CREATE_COMPONENT:           "WS.CREATE_COMPONENT",
		CREATE_DOM_NODE:            "WS.CREATE_DOM_NODE",
		CREATE_LOCAL_WIDGET:        "WS.CREATE_LOCAL_WIDGET",
		CREATE_RELATIONSHIP:        "WS.CREATE_RELATIONSHIP",
		CREATE_SIMPLE_PAGE:         "WS.CREATE_SIMPLE_PAGE",
		DELETE:                     "WS.DELETE",
		DELETE_RELATIONSHIP:        "WS.DELETE_RELATIONSHIP",
		DELETE_UNATTACHED_NODES:    "WS.DELETE_UNATTACHED_NODES",
		DOM_NODE_CHILDREN:          "WS.DOM_NODE_CHILDREN",
		FIND_DUPLICATES:            "WS.FIND_DUPLICATES",
		GET:                        "WS.GET",
		GET_BY_TYPE:                "WS.GET_BY_TYPE",
		GET_LOCAL_STORAGE:          "WS.GET_LOCAL_STORAGE",
		GET_PROPERTY:               "WS.GET_PROPERTY",
		GET_RELATIONSHIP:           "WS.GET_RELATIONSHIP",
		GET_SCHEMA_INFO:            "WS.GET_SCHEMA_INFO",
		GET_TYPE_INFO:              "WS.GET_TYPE_INFO",
		IMPORT:                     "WS.IMPORT",
		INSERT_BEFORE:              "WS.INSERT_BEFORE",
		LINK:                       "WS.LINK",
		LIST:                       "WS.LIST",
		LIST_ACTIVE_ELEMENTS:       "WS.LIST_ACTIVE_ELEMENTS",
		LIST_COMPONENTS:            "WS.LIST_COMPONENTS",
		LIST_SCHEMA_PROPERTIES:     "WS.LIST_SCHEMA_PROPERTIES",
		LIST_SYNCABLES:             "WS.LIST_SYNCABLES",
		LIST_UNATTACHED_NODES:      "WS.LIST_UNATTACHED_NODES",
		LOGIN:                      "WS.LOGIN",
		LOGOUT:                     "WS.LOGOUT",
		PATCH:                      "WS.PATCH",
		PING:                       "WS.PING",
		PULL:                       "WS.PULL",
		PUSH:                       "WS.PUSH",
		PUSH_SCHEMA:                "WS.PUSH_SCHEMA",
		QUERY:                      "WS.QUERY",
		REMOVE:                     "WS.REMOVE",
		REMOVE_FROM_COLLECTION:     "WS.REMOVE_FROM_COLLECTION",
		REPLACE_WIDGET:             "WS.REPLACE_WIDGET",
		SAVE_LOCAL_STORAGE:         "WS.SAVE_LOCAL_STORAGE",
		SAVE_NODE:                  "WS.SAVE_NODE",
		SEARCH:                     "WS.SEARCH",
		SET_PERMISSION:             "WS.SET_PERMISSION",
		SNAPSHOTS:                  "WS.SNAPSHOTS",
		STATUS:                     "WS.STATUS",
		SYNC_MODE:                  "WS.SYNC_MODE",
		UNARCHIVE:                  "WS.UNARCHIVE",
		UPDATE:                     "WS.UPDATE",
		UPLOAD:                     "WS.UPLOAD",
		WRAP_CONTENT:               "WS.WRAP_CONTENT"
	}
};
/**
 * The Logger object
 * After an implementation of the log-method is chosen we dont need to re-evalute the debug parameter.
 * Also we can change the implementation on-the-fly if we need to.
 */
var _Logger = {
	subscriptions: [],
	ignored: ["WS.STATUS", "WS.PING"],

	/**
	 * Initializes the logger
	 * The caller would normally use the URL parameter 'debug' as a parameter.
	 */
	initLogger: function (mode, subscriptions) {
		footer.hide();

		switch (mode) {
			case 'true':
			case '1':
			case 'page':
				_Logger.log = _Logger.htmlLog;
				footer.show();
				break;

			case 'console':
			case '2':
				_Logger.log = _Logger.consoleLog;
				break;

			default:
				_Logger.log = function () {};
		}

		this.subscriptions = [];
		if (subscriptions) {
			if (typeof subscriptions === "string") {
				this.subscriptions = subscriptions.split(',');
			} else if (Array.prototype.isPrototypeOf(subscriptions)) {
				this.subscriptions = subscriptions;
			} else {
				console.log("Unknown subscription initialization! " + subscriptions);
				console.log("Subscribing to every log type.");
				this.subscriptions = this.getAllLogTypes();
			}
		} else {
			this.subscriptions = this.getAllLogTypes();
		}
	},

	/**
	 * The log function (needs to be initialized in order for logging to work)
	 * default implementation does nothing
	 */
	log: function () {},

	/**
	 * Sends all arguments to console.log
	 */
	consoleLog: function () {
		if (this.subscribed(Array.prototype.slice.call(arguments, 0, 1)[0])) {
			console.log(Array.prototype.slice.call(arguments));
		}
	},

	/**
	 * Logs all arguments to the log area in the footer
	 */
	htmlLog: function () {
		if (this.subscribed(Array.prototype.slice.call(arguments, 0, 1)[0])) {
			var msg = Array.prototype.slice.call(arguments).join(' ');
			var div = $('#log', footer);
			div.append(msg + '<br>');
			footer.scrollTop(div.height());
		}
	},


	subscribe: function (type) {
		if (this.subscriptions.indexOf(type) === -1) {
			this.subscriptions.push(type);
		}
	},

	unsubscribe: function (type) {
		var pos = this.subscriptions.indexOf(type);
		if (pos !== -1) {
			this.subscriptions.splice(pos, 1);
		}
	},

	ignore: function (type) {
		if (this.ignored.indexOf(type) === -1) {
			this.ignored.push(type);
		}
	},

	unignore: function (type) {
		var pos = this.ignored.indexOf(type);
		if (pos !== -1) {
			this.ignored.splice(pos, 1);
		}
	},

	/**
	 * decide whether we allow the event to be shown to the user
	 * only if it is not in the ignore liste AND in the subscription list we return true
	 */
	subscribed: function (type) {
		if (type) {
			return (this.ignored.indexOf(type) === -1) && (this.subscriptions.indexOf(type) !== -1 || this.subscriptions.indexOf(type.split('.')[0]) !== -1);
		} else {
			console.error("undefined log type - this should not happen!");
			return true;
		}
	},

	getAllLogTypes: function() {
		var logtypes = [];
		Object.keys(_LogType).forEach(function(key) {
			if (typeof _LogType[key] === "string") {
				logtypes.push(_LogType[key]);
			}
		});

		return logtypes;
	}
};