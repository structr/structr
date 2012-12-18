
function lastPart(id, separator) {
    if (!separator) {
        separator = '_';
    }
    if (id) {
        return id.substring(id.lastIndexOf(separator)+1);
    }
    return '';
}

function sortArray(arrayIn, sortBy) {
    var arrayOut = arrayIn.sort(function(a,b) {
        return sortBy.indexOf(a.id) > sortBy.indexOf(b.id);
    });
    return arrayOut;
}

function isIn(id, ids) {
    return ($.inArray(id, ids) > -1);
}

function escapeTags(str) {
    if (!str) return str;
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function unescapeTags(str) {
    if (!str) return str;
    return str.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>');
}

$.fn.reverse = [].reverse;

if (typeof String.prototype.endsWith != 'function') {
    String.prototype.endsWith = function(pattern) {
        var d = this.length - pattern.length;
        return d >= 0 && this.lastIndexOf(pattern) === d;
    };
}

if (typeof String.prototype.startsWith != 'function') {
    String.prototype.startsWith = function (str){
        return this.indexOf(str) == 0;
    };
}

if (typeof String.prototype.capitalize != 'function') {
    String.prototype.capitalize = function() {
        return this.charAt(0).toUpperCase() + this.slice(1);
    };
}

if (typeof String.prototype.escapeForJSON != 'function') {
    String.prototype.escapeForJSON = function() {
        return this.replace(/"/g, '\'');
    };
}

if (typeof String.prototype.lpad != 'function') {
    String.prototype.lpad = function(padString, length) {
        var str = this;
        while (str.length < length)
            str = padString + str;
        return str;
    };
}

if (typeof String.prototype.contains != 'function') {
    String.prototype.contains = function(pattern) {
        return this.indexOf(pattern) > 0;
    };
}

if (typeof String.prototype.splitAndTitleize != 'function') {
    String.prototype.splitAndTitleize = function(sep) {
        
        var res = new Array();
        var parts = this.split(sep);
        parts.forEach(function(part) {
            res.push(part.capitalize());
        })
        return res.join(" ");
    }
}

/**
 * Clean text from contenteditable
 * 
 * This function will remove any HTML markup and convert
 * any <br> tag into a line feed ('\n').
 */
function cleanText(input) {
    if (debug)console.log(input);
    var output = '';
    $(input).each(function(i, line) {
        var cleaned = $(line).text();
        if (debug) console.log(cleaned.length);
        if (cleaned.length) {
            output += cleaned;
        } else {
            output += '\n';
        }
    });
    if (debug) console.log(output);
    return output;
}

function shorten(uuid) {
    return uuid.substring(0,8);
}

function urlParam(name) {
    name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
    var regexS = "[\\?&]"+name+"=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.href);
    return (results&&results.length?results[1]:'');
}

function nvl(value, defaultValue) {
    var returnValue;
    if (value === undefined) {
        returnValue = defaultValue;
    } else if (value === false) {
        returnValue = 'false';
    } else if (!value) {
        returnValue = '';
    } else {
        returnValue = value;
    }
    return returnValue;
}

String.prototype.toCamel = function(){
    return this.replace(/(\-[a-z])/g, function($1){
        return $1.toUpperCase().replace('-','');
    });
};   

String.prototype.toUnderscore = function(){
    return this.replace(/([A-Z])/g, function($1){
        return "_"+$1.toLowerCase();
    });
};

/**
 * Gratefully taken from https://gist.github.com/24261/7fdb113f1e26111bd78c0c6fe515f6c0bf418af5
 * 
 * The method trims the given string 'str' to fit nicely within a box of 'len' px width
 * without line break.
 */
function fitStringToSize(str,len) {
    var result = str;
    var span = document.createElement("span");
    span.style.visibility = 'hidden';
    span.style.padding = '0px';
    document.body.appendChild(span);

    // on first run, check if string fits into the length already.
    span.innerHTML = result;
    if(span.offsetWidth > len) {
        var posStart = 0, posMid, posEnd = str.length;
        while (true) {
            // Calculate the middle position
            posMid = posStart + Math.ceil((posEnd - posStart) / 2);
            // Break the loop if this is the last round
            if (posMid==posEnd || posMid==posStart) break;

            span.innerHTML = str.substring(0,posMid) + '&hellip;';

            // Test if the width at the middle position is
            // too wide (set new end) or too narrow (set new start).
            if ( span.offsetWidth > len ) posEnd = posMid; else posStart=posMid;
        }
        
        //Escape < and >, eliminate trailing space and a widow character if one is present.
        result = str.substring(0,posStart).replace("<","&lt;").replace(">","&gt;").replace(/(\s.)?\s*$/,'') + '&hellip;';
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
