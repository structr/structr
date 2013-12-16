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
 * Interval for typing simulation in ms
 */
var typeInterval = 20;

/**
 * Interval for moving mouse cursor in ms (lower = faster)
 */
var mouseInterval = 10;

/**
 * Number of steps for mouse movement (fewer steps = faster)
 */
var mouseSteps = 10;

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
    },
    onError: function() {
        console.log('############### onError ################');
        this.exit(1);
    },
    onWaitTimeout: function() {
        console.log('############### onWaitTimeout ################');
        this.exit(1);
    },
    onTimeout: function() {
        console.log('############### onTimeout ################');
        this.exit(1);
    },
    onStepTimeout: function() {
        console.log('############### onStepTimeout ################');
        this.exit(1);
    }
}

/**
 * Base URL for Structr UI tests
 */
exports.url = 'http://localhost:8875/structr/';

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
exports.debug = true;

/**
 * The input field with the given selector will be filled
 * with the given text animated as if it was typed in.
 *
 * If inIframe is true, the selector will be applied
 * in the first iframe
 */
exports.animatedType = function(casper, selector, inIframe, text, blur, len) {

    var t = '';

    if (len === undefined) {
        len = 0;
    }

    if (len === text.length + 1) {

        if (blur) {
            casper.evaluate(function(s, f) {
                var el;
                if (f) {
                    el = $('iframe:first-child').contents().find(s);
                } else {
                    el = $(s);
                }

                el.blur();

            }, selector, inIframe);
        }

        return;
    }

    t = text.substr(0, len);

    if (exports.debug)
        console.log('Typing in', selector, ', in iframe?', inIframe);

    casper.thenEvaluate(function(s, t, f) {

        var el;
        if (f) {
            el = $('iframe:first-child').contents().find(s);
        } else {
            el = $(s);
        }

        if (el && el.is('input')) {
            el.val(t);
        } else {
            el.text(t);
        }

    }, selector, t, inIframe);

    window.setTimeout(function() {
        exports.animatedType(casper, selector, inIframe, text, blur, len + 1);
    }, typeInterval);

}

/**
 * Clicks the node which matches the selector in the first iframe
 */
exports.clickInIframe = function(casper, selector) {

    if (exports.debug)
        console.log('Clicking in iframe on', selector);

    casper.evaluate(function(s) {

        $('iframe:first-child').contents().find(s).click();

    }, selector);

}

/**
 * Display a mouse pointer at the given position
 */
exports.mousePointer = function(casper, pos) {

    if (exports.debug)
        console.log('displaying mouse cursor at', pos.left, pos.top);

    //casper.then(function() {

    var currentPos = casper.evaluate(function(p) {

        var c = $('#testCursor');
        if (!c.length) {
            $('body').append('<img id="testCursor" src="icon/cursor2.png" style="position: absolute; top: ' + p.top + 'px; left: ' + p.left + 'px; z-index: 999999">')
            c = $('#testCursor');
        } else {
            c.offset({
                top: p.top,
                left: p.left
            });
        }
        return c.offset();
    }, pos);

    if (exports.debug)
        console.log('current cursor position', currentPos.left, currentPos.top);

    //});

}

/**
 * Move mouse pointer
 */
exports.moveMousePointer = function(casper, startPos, endPos, step) {

    var pos;

    // Stop after a certain number of steps
    if (step === mouseSteps) {
        exports.mousePointer(casper, endPos);
        return;
    }

    if (step === undefined) {
        step = 0;
    }

    pos = {
        left: startPos.left + (endPos.left - startPos.left) / mouseSteps * step,
        top: startPos.top + (endPos.top - startPos.top) / mouseSteps * step
    };

    exports.mousePointer(casper, pos);

    casper.then(function() {
        exports.moveMousePointer(casper, startPos, endPos, step + 1);
    });

}

/**
 * Move mouse pointer to element with given selector
 */
exports.moveMousePointerTo = function(casper, selector) {

    if (exports.debug)
        console.log('move mouse pointer to', selector);

    casper.then(function() {

        var positions = this.evaluate(function(s) {
            var el = $(s);
            var c = $('#testCursor');

            if (el.length && c.length) {

                var startPos = {left: c.offset().left + c.width() / 2, top: c.offset().top + c.height() / 2};
                var endPos = {left: el.offset().left + el.width() / 2, top: el.offset().top + el.height() / 2};

                return {
                    start: startPos,
                    end: endPos
                };

            }
        }, selector);

        if (exports.debug)
            console.log('start position:', positions.start.left, positions.start.top, ', end position:', positions.end.left, positions.end.top);

        exports.moveMousePointer(casper, positions.start, positions.end);
    });

}

/**
 * Drag element with given source selector onto element with target selector
 */
exports.dragDropElement = function(casper, sourceSelector, targetSelector) {

    if (exports.debug)
        console.log('drag element', sourceSelector, targetSelector);

    casper.then(function() {

        //exports.moveMousePointerTo(casper, targetSelector);

        var positions = this.evaluate(function(s, t) {
            var sourceEl = $(s);
            var targetEl = $(t);

            var c = $('#testCursor');

            if (sourceEl.length && targetEl.length) {

                c.appendTo(sourceEl);

                var sourcePos = {'left': sourceEl.offset().left + sourceEl.width() / 2, 'top': sourceEl.offset().top + sourceEl.height() / 2};
                var targetPos = {'left': targetEl.offset().left + targetEl.width() / 2, 'top': targetEl.offset().top + targetEl.height() / 2};

                sourceEl.simulate('drag', {
                    dx: (targetPos.left - sourcePos.left),
                    dy: (targetPos.top - sourcePos.top),
                    moves: 10
                });

                return {'sourcePos': sourcePos, 'targetPos': targetPos};

            }

        }, sourceSelector, targetSelector);

        exports.mousePointer(this, positions.targetPos);

        if (exports.debug)
            console.log(positions.sourcePos.left, positions.sourcePos.top, positions.targetPos.left, positions.targetPos.top);

    });

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
exports.animateHtml = function(testName, heading, desc) {

    var fs = require('fs');

    var html = '<!DOCTYPE html><html><head><title>structr UI Test: ' + heading + '</title><link rel="stylesheet" type="text/css" media="screen" href="test.css"></head><body><h1>' + heading + '</h1><div id="desc">' + desc + '</div><img width="' + w + '" height="' + h + '" id="anim"><script type="text/javascript">'

    html += '\n'
            + 'var anim = document.getElementById("anim");\n'
            + 'play(0);\n';

    var files = fs.list(exports.docsDir + '/screenshots/' + testName + '/');

    html += 'function setImg(i) { anim.src = "../screenshots/' + testName + '/" + ("000000000" + i).substr(-' + filenameLength + ') + ".' + exports.imageType + '"; }\n'
            + 'function play(i, v) { setImg(i);\n'
            + ' if (i<' + (files.length - 3) + ') { window.setTimeout(function() { play(i+1,v); }, v?v:100); }; }\n';

    html += '</script><div><button onclick="play(0)">Play</button><button onclick="play(0,250)">Slow Motion</button></div></body></html>';

    fs.write(exports.docsDir + '/html/' + testName + '.html', html);

}

/**
 * Start recording of screenshots
 */
exports.startRecording = function(window, casper, testName) {

    casper.options.viewportSize = {
        width: w,
        height: h
    };

    var i = 0;

    var fs = require('fs');

    // Remove old screenshots
    fs.removeTree(exports.docsDir + '/screenshots/' + testName);

    window.setInterval(function() {
        casper.capture(exports.docsDir + '/screenshots/' + testName + '/' + exports.pad(i++) + '.' + exports.imageType);
        if (exports.debug)
            console.log('screenshot ' + i + ' created');
    }, recordingInterval);
}

casper.test.setUp(function() {

    casper.start(exports.url);

});
