var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

var animating = animating || undefined;

(function() {
	'use strict';
	var _s, _callbacks;

	var _count = 0;
	var _timeout = 0;

	Graphbrowser.Modules.Fruchtermann = function(sigmaInstance, callbacks){
		var self = this;
		self.name = 'fruchtermanReingold';
		_callbacks = callbacks;
		_s = sigmaInstance;
	};

	Graphbrowser.Modules.Fruchtermann.prototype.types = ["layout"];

	Graphbrowser.Modules.Fruchtermann.prototype.init = function(){
		var self = this;
		$(document).on('click', '#fruchterman-controlElement',  function() {
			self.doLayout(1);
		});
	};

	Graphbrowser.Modules.Fruchtermann.prototype.start = function(){
		var self = this;
		self.doLayout(1);
	};

	Graphbrowser.Modules.Fruchtermann.prototype.doLayout = function(num) {
		var self = this;

		if(num){
			self.restartLayout(num);
		}
		else{
			self.restartLayout(20);
		}
	};

	Graphbrowser.Modules.Fruchtermann.prototype.restartLayout = function(num) {
		var self = this;

		var config = {
			autoArea: false,
			area: 1000000000,
			gravity: 0,
			speed: 0.1,
			iterations: 1500,
			easing: 'quadraticInOut',
			duration: 800
		};

		var listener = sigma.layouts.fruchtermanReingold.configure(_s, config);

		listener.bind('stop', function(event) {
			window.clearTimeout(_timeout);
			_timeout = window.setTimeout(function() {
				if (_timeout) {
					_timeout = 0;
					_callbacks.dataChanged();
				}
			}, 50);
		});

		window.setTimeout(function() {
			animating = true;
			sigma.layouts.fruchtermanReingold.start(_s);
			animating = false;
			if (_count++ < num) {
				self.restartLayout(num);
			} else {
				_count = 0;
			}
		}, 40);
	};
}).call(window);