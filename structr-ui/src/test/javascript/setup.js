/*
 *  Copyright (C) 2010-2013 Axel Morgner
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

/**
 * Screenshot/animation dimensions (pixel)
 */
var w = 960, h = 800;

/**
 * Type interval for typing simulation in ms
 */
var typeInterval = 20;

/**
 * Recording interval in ms
 */
var recordingInterval = 100;

/**
 * Length of animation images filenames (f.e. 0023.png)
 */
var filenameLength = 4;

/**
 * Set casper options, see http://docs.casperjs.org/en/latest/modules/casper.html#index-1
 */
exports.casperOptions = {
    viewportSize: {
        width: w,
        height: h
    }
}

/**
 * Base URL for Structr UI tests
 */
exports.url = 'http://localhost:8875/structr/#pages';

/**
 * Image type extension
 * 
 * Use png, gif, or jpg
 */
exports.imageType = 'png';

/**
 * Base directory for docs
 */ 
exports.docsDir = '../../../../docs';

/**
 * Set to true to enable debug logging
 */
exports.debug = false;

/**
 * The input field with the given selector will be filled
 * with the given text animated as if it was typed in.
*/
exports.animatedType = function(casper, selector, text, len) {
    var t = '';

    if (len === undefined) {
        len = 0;
    }

    if (len === text.length + 1)
        return;

    t = text.substr(0, len);

    casper.thenEvaluate(function(f, t) {
        $(f).val(t);
    }, selector, t);

    window.setTimeout(function() {
        exports.animatedType(casper, selector, text, len + 1);
    }, typeInterval);

}

/**
 * Utility function to left-fill a file name with leading '0's
 */
exports.pad = function(num) {
    return ('000000000' + num).substr(-filenameLength);
}

/**
 * Create an HTML file with JS-animated video
 */
exports.animateHtml = function(name, heading, desc) {
    
    var fs = require('fs');
    
    var html = '<!DOCTYPE html><html><head><title>structr UI Test: ' + heading + '</title><link rel="stylesheet" type="text/css" media="screen" href="test.css"></head><body><h1>' + heading + '</h1><div id="desc">' + desc + '</div><img width="' + w + '" height="' + h + '" id="anim"><script type="text/javascript">'

    html += '\n'
    + 'var anim = document.getElementById("anim");\n'
    + 'play(0);\n';
    
    var files = fs.list(exports.docsDir + '/screenshots/' + name + '/');

    html += 'function setImg(i) { anim.src = "../screenshots/' + name + '/" + ("000000000" + i).substr(-' + filenameLength + ') + ".' + exports.imageType + '"; }\n'
    + 'function play(i, v) { setImg(i);\n'
    + ' if (i<' + (files.length-3) + ') { window.setTimeout(function() { play(i+1,v); }, v?v:100); }; }\n';

    html += '</script><div><button onclick="play(0)">Play</button><button onclick="play(0,250)">Slow Motion</button></div></body></html>';

    fs.write(exports.docsDir + '/html/' + name + '.html', html);
    
}

/**
 * Start recording of screenshots
 */
exports.startRecording = function(window, casper, testName) {
    var i = 0;
    window.setInterval(function() {
        casper.capture(exports.docsDir + '/screenshots/' + testName + '/' + exports.pad(i++) + '.' + exports.imageType);
        if (exports.debug) console.log('screenshot ' + i + ' created');
    }, recordingInterval);
}