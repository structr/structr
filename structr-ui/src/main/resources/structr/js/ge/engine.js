/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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

var clickedMass = null;
var springs = new Array();
var masses = new Array();
var xCenters = new Array();
var yCenters = new Array();
var images = new Array();
var ctx = null;
var nodeImage = new Image(); nodeImage.src = "icon/node.png";
var nodeImageMouseover = new Image(); nodeImageMouseover.src = "icon/node_mouseover.png";
var nodeImagePressed = new Image(); nodeImagePressed.src = "icon/node_pressed.png";
var selectionImage = new Image(); selectionImage.src = "icon/selection.png";
var draggingImage = new Image(); draggingImage.src = "icon/dragging.png";
var colors = new Array();
var types = new Array();
var inc = 32;
var r = 0;
var g = 0;
var b = 0;
var count=0;
var running = null;
var width = 1000;
var height = 600;
var dt = 0.05;
var drag = 50;
var initialSpringLength = 20;
var springForce = 100;
var springDamping = 100;
var centerForce = 0;
var maxMagneticDistance = 1000000;
var magneticForce = 50000;
var magneticFactor = 0.5;
var numMasses = 100;
var keys = new Array();
var centerX = 0;
var centerY = 0;
var mouseX = 0;
var mouseY = 0;
var mouseBtn = 0;
var viewportX = 0;
var viewportY = 0;
var viewportX0 = 0;
var viewportY0 = 0;
var scale = 1;
var canvasOffsetX = 0;
var canvasOffsetY = 0;
var selectX = 0;
var selectY = 0;
var lastX = 0;
var lastY = 0;
var backgroundColor = "rgb(255, 255, 255)";
var headerWidth = 5;
var headerHeight = 70;
var canvas;
    
function Engine(parent) {
	
    this.parent = parent;
    canvas = $("<canvas>")[0];
    $(parent).append(canvas);
        
    this.initialize = function() {
	    
	colors[ 0] = "#aa0000";
	colors[ 1] = "#00aa00";
	colors[ 2] = "#0000ff";
	colors[ 3] = "#ff8800";
	colors[ 4] = "#888800";
	colors[ 4] = "#880088";
	colors[ 4] = "#008888";
	colors[ 4] = "#888800";

	this.resizeCanvas();
	
        ctx = canvas.getContext("2d");
    
        canvas.onmousemove = function(e) {
            
            mouseX = e.x;
            mouseY = e.y;
            
            var mX = ((mouseX - width/2 - canvasOffsetX - headerWidth) / scale);
            var mY = ((mouseY - height/2 - canvasOffsetY - headerHeight) / scale);
            
            if(clickedMass !== null) {
                clickedMass.x = mX;
            	clickedMass.y = mY;
            }
        }

        canvas.onmousedown = function(e) {
		mouseBtn = e.button+1;
	}
	
        canvas.onmouseup = function(e) {
	    mouseBtn = 0;
	    clickedMass = null;
        }
        
        canvas.onmousewheel = function(e) {
            var s = 1 + (e.wheelDelta / 300);
            if((scale * s) > 0.1 && (scale * s) < 10) {
                scale *= s;
    	        ctx.scale(s, s);	
	    }
    	}
        
        canvas.onblur = function() {
            clickedMass = null;
	    mouseBtn = 0;
        }
        
        document.onkeydown = function(e) {
		keys[e.keyCode] = 1;
	}
	
        document.onkeyup = function(e) {
		keys[e.keyCode] = 0;
	}
        
        // center canvas on 0:0
        ctx.translate(width/2, height/2);
        
        // initialize centers
        this.setCenters(10, 10);
    }

    this.update = function() {

	var springNum = springs.length;
	var massNum = masses.length;
	var i = 0;
	var j = 0;
	
	ctx.save();
	ctx.setTransform(1, 0, 0, 1, 0, 0);
	ctx.fillStyle = backgroundColor;
	ctx.fillRect(0, 0, canvas.width, canvas.height);
	ctx.restore();

	// smooth scrolling
	var dvx = (viewportX - viewportX0);
	var dvy = (viewportY - viewportY0);
	viewportX -= dvx * 0.2;
	viewportY -= dvy * 0.2;
	
	canvasOffsetX += viewportX * scale;
	canvasOffsetY += viewportY * scale;
	
	ctx.translate(viewportX, viewportY);
	
	// apply magnetic force
	for(i=0;i<massNum;i++) {
		
		for(j=i+1;j<massNum;j++) {

			var m1 = masses[i];
			var m2 = masses[j];
		
			var f = magneticForce;
		
			if(m1.type === m2.type) {
				f *= magneticFactor;
			}

			if(m1.selected === true && m2.selected === true) {
				f *= magneticFactor * magneticFactor;
			}
			
			var dx = (m1.x - m2.x);
			var dy = (m1.y - m2.y);		
			
			var l2 = (dx*dx + dy*dy);
			
			if(l2 < maxMagneticDistance) {
			
				var l =  Math.sqrt(l2);

				// magnetic force
				if(l > dt) {
			
					var dFx = dx * (1 / (l * l)) * f;
					var dFy = dy * (1 / (l * l)) * f;
	       
					m1.Fx += dFx;
					m1.Fy += dFy;
				
					m2.Fx -= dFx;
					m2.Fy -= dFy;
				}
			}
		}
	}

	// update springs
	for(i=0;i<springNum;i++) {
		var obj = springs[i];
		obj.update();
		obj.draw();
	}

	// update position & reset forces
	for(i=0;i<massNum;i++) {
		var obj = masses[i];
		obj.update();
		obj.draw();
		obj.Fx = 0;
		obj.Fy = 0;
	}
	
	this.handleKeys();
	this.handleMouse();
	
	window.setTimeout("engine.update()", dt);
    }
    
    this.addNode = function(node) {
	    
        var startX = (Math.random() * 100) + (width/2) - 50;
        var startY = (Math.random() * 100) + (height/2) - 50;

	if (!isIn(node.type, types)) {
	    types.push(node.type);
	}
	
	new Mass(node, startX, startY);
	
    }
    
    this.addRelationship = function(type, startNodeId, endNodeId) {
            new Spring(type, startNodeId, endNodeId);
    }
    
    this.handleKeys = function() {

	var s = 0;

	if(keys[37] === 1) { viewportX += 5; }	// left
	if(keys[38] === 1) { viewportY += 5; }	// up
	if(keys[39] === 1) { viewportX -= 5; }	// right
	if(keys[40] === 1) { viewportY -= 5; }	// down

	if(keys[33] === 1) {
	
		s = 1 + (1 / 10);
		if((scale * s) > 0.25 && (scale * s) < 6) {
			scale *= s;
			ctx.scale(s, s);	
		}
	}	

	if(keys[34] === 1) {
	
		s = 1 + (-1 / 10);
		if((scale * s) > 0.25 && (scale * s) < 6) {
			scale *= s;
			ctx.scale(s, s);	
		}
	}	
    }

    this.handleMouse = function() {

        var mX = ((mouseX - width/2 - canvasOffsetX - headerWidth) / scale);
        var mY = ((mouseY - height/2 - canvasOffsetY - headerHeight) / scale);
        		
	// left button
	if(mouseBtn === 1 && clickedMass === null) {
	
		if(selectX === 0 && selectY === 0) {
			selectX = mX;
			selectY = mY;
		}
		
		ctx.globalAlpha = 0.1;
		ctx.fillStyle = "rgb(64, 64, 64)";
		ctx.fillRect(selectX, selectY, mX-selectX, mY-selectY);
		ctx.globalAlpha = 1;
		
	} else {
	
		/*
		16 shift
		17 ctrl
		18 alt
		*/
	
		if(selectX !== 0 && selectY !== 0) {

			var i=0;
			for(i=0;i<masses.length;i++) {
				var mass = masses[i];
				if(mass.isInside(selectX, selectY, mX-selectX, mY-selectY)) {
					mass.selected = true;
				} else {
				
					if(mass.selected) {
						if(keys[16] === 0) {
							mass.selected = false;
						}
					}
				}
			}
	
			selectX = 0;
			selectY = 0;
		}
	}
    }

    this.toggleCenterForce = function() {
    	if(centerForce > 0) {
    		centerForce = 0;
    	} else {
    		centerForce = 20;
    	}
    }
    
    this.setCenter = function(type, x, y) {
    	xCenters[type] = x;
    	yCenters[type] = y;
    }
    
    this.setCenters = function(rx, ry) {
    
    	var i=0;
    	var rad = (2*Math.PI) / colors.length;
    	for(i=0; i<colors.length; i++) {
        
    		var cx = Math.cos(i * rad) * rx;
    		var cy = Math.sin(i * rad) * ry;
      	
    		this.setCenter(colors[i], cx, cy);
    	}
    }
    
    this.resizeCanvas = function resizeCanvas() {
        canvas.width = $(window).width() - 48;
        canvas.height = $(window).height() - $('#header').height() - 72;
    }
}

/******************************************************************************
 * A mass object.
 */
function Mass(node, startX, startY) {
    
    masses[masses.length] = this;
    this.nodeId = node.id;
    this.color = colors[types.indexOf(node.type)];
    this.typeString = node.type;
    this.x = startX;
    this.y = startY;
    this.x0 = this.x;
    this.y0 = this.y;
    this.Fx = 0;
    this.Fy = 0;
    this.image = nodeImage;

    this.draw = function() {

        var mX = ((mouseX - width/2 - canvasOffsetX - headerWidth) / scale);
        var mY = ((mouseY - height/2 - canvasOffsetY - headerHeight) / scale);
        		
        var fillStyle = this.color;
	var r = 10;
	var e = 20;
	var r2 = r/2;

	// highlight mass element under cursor
        if(Math.abs(this.x - r2 - mX) < e && Math.abs(this.y - r2 - mY) < e) {
                
             if(mouseBtn > 0 && selectX === 0 && selectY === 0) {
                        
                 clickedMass = this;
	     }
		     
	     fillStyle = "#ff0000";
        }

	ctx.globalAlpha = 0.2;
        ctx.fillStyle = fillStyle;
        ctx.strokeStyle = '#000000';
        ctx.beginPath();
        ctx.arc(this.x-r2, this.y-r2, r, 0, 2*Math.PI);
        ctx.fill();
        ctx.lineWidth =	 1;
        ctx.stroke();        
        ctx.fillStyle = '#000000';
	ctx.fillText(this.typeString, this.x-r2, this.y-r2);
    }

    this.update = function() {
	
        var tmpX = this.x;
        var tmpY = this.y;

	// pull towards the center of the screen
	if(this !== clickedMass) {
	
		this.Fx += (xCenters[this.color] - this.x) * centerForce;
	     	this.Fy += (yCenters[this.color] - this.y) * centerForce;

	        var dt2 = (dt * dt);

		// drag
		this.Fx -= (this.x - this.x0) * drag;
		this.Fy -= (this.y - this.y0) * drag;

		this.x += (this.x - this.x0) + (this.Fx * dt2);
		this.y += (this.y - this.y0) + (this.Fy * dt2);
	}

        this.x0 = tmpX;
        this.y0 = tmpY;
    }

    this.isInside = function(x, y, w, h) {

	var inside = true;
	var d = 5;

	if(w > 0) {
		inside &= (this.x+d >= x && this.x+d <= x+w);
	} else {
		inside &= (this.x+d <= x && this.x+d >= x+w);
	}
	
	if(h > 0) {
		inside &= (this.y+d >= y && this.y+d <= y+h);
	} else {
		inside &= (this.y+d <= y && this.y+d >= y+h);
	}
	
	return inside;
    }
}

function Spring(typeString, startNodeId, endNodeId) {

    springs[springs.length] = this;
    this.typeString = typeString;
    this.startNodeId = startNodeId;
    this.endNodeId = endNodeId;
    this.m1 = null;
    this.m2 = null;
    this.nl = initialSpringLength;
    
    this.update = function() {
	    
        if (this.m1 && this.m2) {
		
		var dx = (this.m1.x - this.m2.x);
		var dy = (this.m1.y - this.m2.y);		

		var l =  (Math.sqrt(dx*dx + dy*dy));

		// spring force
		var dl = this.nl - l;

		var dF1x = dx * dl * springForce * (1 / springDamping) * dt * dt;
		var dF1y = dy * dl * springForce * (1 / springDamping) * dt * dt;

		if (dF1x > 1000 || dF1y > 1000) {
			dF1x *= 0.1;
			dF1y *= 0.1;
		}

                // apply forces
                this.m1.Fx += dF1x;
                this.m1.Fy += dF1y;
                
                this.m2.Fx -= dF1x;
                this.m2.Fy -= dF1y;
		
	} else {

	    for (i=0; i<masses.length; i++) {

		    if (masses[i].nodeId === startNodeId) {

			    this.m1 = masses[i];
			    continue;
		    }

		    if (masses[i].nodeId === endNodeId) {

			    this.m2 = masses[i];
			    continue;
		    }
		}

	}
	    
    }
    
    this.draw = function() {

	if (this.m1 && this.m2) {

            this.drawRel(typeString, this.m1.x - 10, this.m1.y - 10, this.m2.x - 10, this.m2.y - 10, 20, 20, 20, 20);
	}
    }

    this.drawRel = function(type, x1, y1, x2, y2, w1, h1, w2, h2) {

        var ax, ay, bx, by;

        var cx = (h1 / 2) * (x2 - x1) / (y2 - y1);
        var cy = (w1 / 2) * (y2 - y1) / (x2 - x1);
        var dx = (h2 / 2) * (x2 - x1) / (y2 - y1);
        var dy = (w2 / 2) * (y2 - y1) / (x2 - x1);

        if (y2 >= y1) {

            ax = Math.min(Math.max(x1 + cx, x1 - (w1 / 2)), x1 + (w1 / 2));
            bx = Math.min(Math.max(x2 - dx, x2 - (w2 / 2)), x2 + (w2 / 2));

        } else if (y1 > y2) {

            ax = Math.min(Math.max(x1 - cx, x1 - (w1 / 2)), x1 + (w1 / 2));
            bx = Math.min(Math.max(x2 + dx, x2 - (w2 / 2)), x2 + (w2 / 2));

        }

	if (x2 >= x1) {

            ay = Math.min(Math.max(y1 + cy, y1 - (h1 / 2)), y1 + (h1 / 2));
            by = Math.min(Math.max(y2 - dy, y2 - (h2 / 2)), y2 + (h2 / 2));

        } else if (x1 > x2) {

            ay = Math.min(Math.max(y1 - cy, y1 - (h1 / 2)), y1 + (h1 / 2));
            by = Math.min(Math.max(y2 + dy, y2 - (h2 / 2)), y2 + (h2 / 2));

        }

	var style = "#000000";

	ctx.globalAlpha = 0.5;
	ctx.fillStyle = style;
	ctx.strokeStyle = style;
        this.drawArrow(ax, ay, bx, by);
	ctx.fillStyle = "#000000";
	ctx.strokeStyle = "#000000";
        this.drawText(type, bx - (bx - ax) / 2, by - (by - ay) / 2);
    }

    this.drawText = function(text, x, y) {
	
        if (text && x && y) {
            ctx.font = "10px sans-serif";
            ctx.textAlign = "center";
            ctx.textBaseline = "bottom";
            ctx.fillText(text, x, y);
        }
    }

    this.drawArrow = function(x1, y1, x2, y2) {
	    
        var size = 6;
        var angle = Math.atan2(y2 - y1, x2 - x1);
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.lineTo(x2, y2);
        ctx.stroke();
        ctx.closePath();
        ctx.beginPath();
        var x3 = x2 - size * Math.cos(angle - Math.PI / 6);
        var y3 = y2 - size * Math.sin(angle - Math.PI / 6);
        ctx.lineTo(x3, y3);
        ctx.moveTo(x2, y2);
        var x4 = x2 - size * Math.cos(angle + Math.PI / 6);
        var y4 = y2 - size * Math.sin(angle + Math.PI / 6);
        ctx.lineTo(x4, y4);
        ctx.lineTo(x3, y3);
        ctx.lineTo(x2, y2);
        ctx.fill();
        ctx.stroke();
        ctx.closePath();
    }
}
