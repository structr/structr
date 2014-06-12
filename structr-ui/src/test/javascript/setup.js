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



var fs = require('fs');


/**
 * Screenshot/animation dimensions (pixel)
 */
var w = 1280, h = 720;

/**
 * Interval for typing simulation in ms
 */
var typeInterval = 10;

/**
 * Interval for moving mouse cursor in ms (lower = faster)
 */
var mouseInterval = 10;

/**
 * Number of steps for mouse movement (fewer steps = faster)
 */
var mouseSteps = 20;

/**
 * Recording interval in ms
 */
var recordingInterval = 100;

/**
 * Length of animation images filenames (f.e. 0023.png)
 */
var filenameLength = 4;

/**
 * Base URL for Structr UI tests
 */
exports.url = 'http://localhost:8875/structr/#pages';

/**
 * Base URL for structr
 */
exports.baseUrl = 'http://localhost:8875/';

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
 *
 * If inIframe is true, the selector will be applied
 * in the first iframe
 */
exports.animatedType = function(casper, selector, inIframe, text, blur, len) {

    var t = '';

    if (len === undefined) {
        len = 1;
    }

    if (len === text.length + 1) {

        if (blur) {
            casper.thenEvaluate(function(s, f) {
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

    //t = text.substr(0, len);
    t = text.substring(len - 1, len);

    if (exports.debug)
        console.log('Typing text ', t, ' in', selector, 'in iframe?', inIframe);

    casper.then(function() {

        if (inIframe) {
            casper.withFrame(0, function() {
                casper.sendKeys('body div:nth-child(2) span', t, {keepFocus: true});
            });
        } else {
            casper.sendKeys(selector, t, {keepFocus: true});
            //casper.sendKeys(selector, t);
        }

    });

    casper.wait(inIframe ? 1 : typeInterval, function() {
        exports.animatedType(casper, selector, inIframe, text, blur, len + 1);
    });


}

/**
 * Clicks the node which matches the selector in the first iframe
 */
exports.clickInIframe = function(casper, selector) {

    if (exports.debug)
        console.log('Clicking in iframe on', selector);

    casper.evaluate(function(s) {
        var el = $('iframe:first-child').contents().find(s);
        el.click();
        el.focus();
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
 * Move mouse pointer to element with given selector.
 * 
 * Set offset if you want to place the pointer off the center
 * of the target element.
 */
exports.moveMousePointerTo = function(casper, selector, offset) {

    if (exports.debug)
        console.log('move mouse pointer to', selector);

    casper.then(function() {

        var positions = this.evaluate(function(s, o) {
            var el = $(s);
            var c = $('#testCursor');

            if (el.length && c.length) {

                var startPos = {
                    left: c.offset().left + c.width() / 2,
                    top: c.offset().top + c.height() / 2
                };

                var endPos = {
                    left: (el.offset().left + el.width() / 2) + (o ? o.x : 0),
                    top: (el.offset().top + el.height() / 2) + (o ? o.y : 0)
                };

                return {
                    start: startPos,
                    end: endPos
                };

            }


        }, selector, offset);

        if (exports.debug)
            console.log('start position:', positions.start.left, positions.start.top, ', end position:', positions.end.left, positions.end.top);

        exports.moveMousePointer(casper, positions.start, positions.end);

        this.mouseEvent('mouseover', selector);

    });

}

/**
 * Drag element with given source selector onto element with target selector
 */
exports.dragDropElement = function(casper, sourceSelector, targetSelector, offset) {

    if (exports.debug)
        console.log('drag element', sourceSelector, targetSelector, offset);

    var positions;

    casper.then(function() {

        //  exports.moveMousePointerTo(casper, sourceSelector);

        positions = this.evaluate(function(s, t, o) {
            var sourceEl = $(s);
            var targetEl = $(t);

            if (sourceEl.length && targetEl.length) {


                var sourcePos = {
                    left: sourceEl.offset().left + sourceEl.width() / 2,
                    top: sourceEl.offset().top + sourceEl.height() / 2
                };
                var targetPos = {
                    left: (targetEl.offset().left + targetEl.width() / 2) + (o ? o.x : 0),
                    top: (targetEl.offset().top + targetEl.height() / 2) + (o ? o.y : 0)
                };

                return {sourcePos: sourcePos, targetPos: targetPos};

            }

        }, sourceSelector, targetSelector, offset);

        if (exports.debug)
            console.log('move element from ... to ...', positions.sourcePos.left, positions.sourcePos.top, positions.targetPos.left, positions.targetPos.top);

        exports.moveMousePointerTo(casper, sourceSelector);

        var steps = 10;

        for (var i = 1; i <= steps; i++) {

            //window.setTimeout(function() {

            this.thenEvaluate(function(source, sourcePos, targetPos, st, j) {

                var sourceEl = $(source);

                var dx = targetPos.left - sourcePos.left;
                var dy = targetPos.top - sourcePos.top;

                var newX = sourcePos.left + dx / st * j;
                var newY = sourcePos.top + dy / st * j;

                var movedEl = $('#moved-element-clone');
                if (!movedEl.length) {
                    movedEl = sourceEl.clone();
                    movedEl.attr('id', 'moved-element-clone').appendTo('body');
                }

                var c = $('#testCursor');
                c.appendTo(movedEl);

                movedEl.offset({left: newX, top: newY});
                c.offset({left: newX, top: newY});

            }, sourceSelector, positions.sourcePos, positions.targetPos, steps, i);
            //exports.mousePointer(this, positions.targetPos);
        }
    });

    casper.thenEvaluate(function(s, t, o) {

        //exports.moveMousePointerTo(casper, sourceSelector);

        var movedEl = $('#moved-element-clone');
        if (movedEl.length) {
            movedEl.remove();
        }

//            var positions = this.evaluate(function(s, t, o) {
        var sourceEl = $(s);
        var targetEl = $(t);

        if (sourceEl.length && targetEl.length) {


            var sourcePos = {
                left: sourceEl.offset().left + sourceEl.width() / 2,
                top: sourceEl.offset().top + sourceEl.height() / 2
            };
            var targetPos = {
                left: (targetEl.offset().left + targetEl.width() / 2) + (o ? o.x : 0),
                top: (targetEl.offset().top + targetEl.height() / 2) + (o ? o.y : 0)
            };

        }

//            }, sourceSelector, targetSelector, offset);



        var d = {
            x: targetPos.left - sourcePos.left,
            y: targetPos.top - sourcePos.top
        }

//            this.evaluate(function(s) {

        var sourceEl = $(s);
        sourceEl.simulate('drag-n-drop', {
            dx: d.x,
            dy: d.y
//            dx: -1350,
//            dy: 190
        });

        this.mouseEvent('mouseover', targetSelector);

    }, sourceSelector, targetSelector, offset);

    //console.log('d[x,y]: ' + d.x + ', ' + d.y);

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
exports.animateHtml = function(testName, heading, sections) {

    var html = '<!DOCTYPE html><html><head><title>structr UI Test: ' + heading + '</title><link rel="stylesheet" type="text/css" media="screen" href="test.css"></head><body><div id="main"><h1>' + heading + '</h1>'

    sections.forEach(function(s) {
        html += '<section>' + s + '</section>';
    });

    html += '<img width="' + w + '" height="' + h + '" id="anim"><script>'

    html += '\n'
            + 'var t; var anim = document.getElementById("anim");\n'
            + 'play(0,50);\n';

    var files = fs.list(exports.docsDir + '/screenshots/' + testName + '/');

    html += 'function setImg(i) { anim.src = "../screenshots/' + testName + '/" + ("000000000" + i).substr(-' + filenameLength + ') + ".' + exports.imageType + '"; }\n'
            + 'function play(i, v) { setImg(i);\n'
            + ' if (i<' + (files.length - 3) + ') { t = window.setTimeout(function() { play(i+1,v); }, v?v:100); }; }\n';

    html += '</script><div><button onclick="play(0,50)">Play</button><button onclick="window.clearTimeout(t)">Stop</button><button onclick="play(0,250)">Slow Motion</button></div></div></body></html>';

    fs.write(exports.docsDir + '/html/' + testName + '_test.html', html);

    exports.createNavigation();

}

/**
 * Start recording of screenshots
 */
exports.startRecording = function(window, casper, testName) {

    /**
     * Set casper options, see http://docs.casperjs.org/en/latest/modules/casper.html#index-1
     */
    casper.options.viewportSize = {
        width: w,
        height: h
    };

    var i = 0;
    
    // Remove old screenshots
    fs.removeTree(exports.docsDir + '/screenshots/' + testName);

    window.setInterval(function() {
        casper.capture(exports.docsDir + '/screenshots/' + testName + '/' + exports.pad(i++) + '.' + exports.imageType);
        if (exports.debug)
            console.log('screenshot ' + i + ' created');
    }, recordingInterval);

}

/**
 * Create navigation page
 */
exports.createNavigation = function() {

    var fs = require('fs');

    var files = fs.list(exports.docsDir + '/html/');

    var html = '<!DOCTYPE html><html><head><title>Structr Documentation Menu</title><link rel="stylesheet" type="text/css" media="screen" href="test.css"></head><body><ul id="menu">'

    files.forEach(function(file) {
        if (file.indexOf('_test') > -1) {
            var name = file.substring(4, file.indexOf('_test')).split('_').map(function(n) {
                return n.charAt(0).toUpperCase() + n.slice(1);
            }).join(' ');
            html += '<li><a href="' + file + '" target="cont">' + name + '</a></li>\n';
        }

    });

    html += '</ul></body></html>';

    fs.write(exports.docsDir + '/html/nav.html', html);

}
