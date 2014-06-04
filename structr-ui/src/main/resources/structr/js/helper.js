/*
 *  Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
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
        return;
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
    //console.log('is', s, 'in', array, '?', ($.inArray(s, array) > -1));
    return ($.inArray(s, array) > -1);
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
            .replace(/&gt;/g, '>');
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

if (typeof String.prototype.escapeForJSON !== 'function') {
    String.prototype.escapeForJSON = function() {
        return this
                .replace(/"/g, '\\"');
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
        return this.indexOf(pattern) > 0;
    };
}

if (typeof String.prototype.splitAndTitleize !== 'function') {
    String.prototype.splitAndTitleize = function(sep) {

        var res = new Array();
        var parts = this.split(sep);
        parts.forEach(function(part) {
            res.push(part.capitalize());
        })
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
    //console.log(input);
    var output = input
            .replace(/<br><\/div>/ig, '\n')
            .replace(/<div>/ig, '\n')
            .replace(/<br(\s*)\/*>/ig, '\n')
            .replace(/(<([^>]+)>)/ig, "");

    //console.log(output);
    return output;

//    if (debug) console.log(input);
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
//
//function getShortLink(str,len,url)
//{
//    return '<a title="' + str.replace("\"","&#34;") + '" href="'+ url +'">' + fitStringToSize(str,len) + '<\/a>';
//}
//
//function getShortAbbr(str,len)
//{
//    return '<abbr title="' + str.replace("\"","&#34;") + '">' + fitStringToSize(str,len) + '<\/abbr>';
//}

function showAjaxLoader(el) {

    //    if (el) {
    //        el.after($('#ajaxLoader'));
    //    }

    $('#ajaxLoader').show();
}

function hideAjaxLoader() {
    $('#ajaxLoader').hide();
}

function formatValue(value) {

    //console.log('formatValue: ', value);

    if (value === undefined || value === null) {
        return '';
    }

    //console.log('is String? ', value.constructor === String);
    //console.log('is Object? ', value.constructor === Object);
    //console.log('is Array? ', value.constructor === Array);

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

    $(element).animate({
        color: '#6db813',
        backgroundColor: '#81ce25'
    }, 50, function() {
        $(this).animate({
            color: oldFg,
            backgroundColor: oldBg
        }, 1000);
    });
}

function blinkRed(element) {

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
        color: '#a00',
        backgroundColor: '#faa'
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
                f = f.nextSibling;
                if (f && f.nodeType === 3) {
                    var comment = {};
                    comment.id = id;
                    comment.textNode = f;
                    comment.rawContent = raw;
                    comments.push(comment);
                }
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
