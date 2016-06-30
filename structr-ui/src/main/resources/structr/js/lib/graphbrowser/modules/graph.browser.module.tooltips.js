var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _tooltip, _show, _hide, _cssClass, _cssId, _position, _page, _onShow, _onError, _nodeRenderer;

	Graphbrowser.Modules.Toolstips = function (sigmaInstance, callbacks){
		var self = this;
		self.name = "tooltips";
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.Toolstips.prototype.types = [];

	Graphbrowser.Modules.Toolstips.prototype.init = function(settings){
		var self = this;

		//Check the settings
		if(settings !== undefined){
			settings.show !== undefined ? _show = settings.show : _show = 'rightClickNode';
			settings.hide !== undefined ? _hide = settings.hide : _hide = 'clickStage';
			settings.cssClass !== undefined ? _cssClass = settings.cssClass : _cssClass = 'tooltip-container';
			settings.position !== undefined ? _position = settings.position : _position = 'bottom';
			(settings.onShow !== undefined && typeof settings.onShow === 'function') ? _onShow = settings.onShow : _onShow  = undefined;
			(settings.onError !== undefined && typeof settings.onError === 'function') ? _onError = settings.onError : _onError = undefined;
			settings.page !== undefined ? _page = settings.page : _page = undefined;
			(settings.nodeRenderer !== undefined && typeof settings.nodeRenderer === 'function') ? _nodeRenderer = settings.nodeRenderer : _nodeRenderer = undefined;
		}
		else
			throw new Error('"Graph-Browser-Tooltips: No settings found."');

		_callbacks.api.closeTooltip = self.closeTooltip.bind(self);

		var config = {
			node: {
				show: _show,
				hide: _hide,
				position: _position,
				template: '<div id="tooltip-container" class="' + _cssClass + '"></div>',
				renderer: function(node, template) {
					if(_nodeRenderer)
						return _nodeRenderer(node);
					else{
						self.loadStructrPageContent(node);
						return "";
					}
				}
			}
		};

		_tooltip = sigma.plugins.tooltips(_s, _s.renderers[0], config);
	};

	Graphbrowser.Modules.Toolstips.prototype.closeTooltip = function(){
		_tooltip.close();
	};

	Graphbrowser.Modules.Toolstips.prototype.loadStructrPageContent = function(eventNode){
		var self = this;
		if(_page)
			_callbacks.conn.getStructrPage(_page, eventNode.data.node.id, self.onContentLoaded, _onError);
		else if(_nodeRenderer)
			self.onContentLoaded(_nodeRenderer(eventNode.data.node), eventNode.data.node.id);
	};

	Graphbrowser.Modules.Toolstips.prototype.onContentLoaded = function(content, nodeId){
		$('#tooltip-container').append(content);

		$(document).on('click', '#tooltip-btnCloseTooltip-' + nodeId,  function() {
			_tooltip.close();
		});

		if(_onShow !== undefined)
			_onShow(node);
	};
}).call(window);