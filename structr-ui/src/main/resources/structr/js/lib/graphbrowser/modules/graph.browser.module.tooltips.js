var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _tooltip, _page, _onShow, _onError,
		_nodesEnabled = false, _nodeShowOn, _nodeHideOn, _nodeCssClass, _nodePosition, _nodeRenderer,
		_edgesEnabled = false, _edgeShowOn, _edgeHideOn, _edgeCssClass, _edgePosition, _edgeRenderer;

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
			if(settings.node){
				_nodesEnabled = true;
				settings.node.showOn !== undefined ? _nodeShowOn = settings.node.showOn : _nodeShowOn = 'rightClickNode';
				settings.node.hideOn !== undefined ? _nodeHideOn = settings.node.hideOn : _nodeHideOn = 'clickStage';
				settings.node.position !== undefined ? _nodePosition = settings.node.position : _nodePosition = 'bottom';
				settings.node.cssClass !== undefined ? _nodeCssClass = settings.node.cssClass : _nodeCssClass = 'node-tooltip-container';
				(settings.node.renderer !== undefined && typeof settings.node.renderer === 'function') ? _nodeRenderer = settings.node.renderer : _nodeRenderer = undefined;
			}

			if(settings.edge){
				_edgesEnabled = true;
				settings.edge.showOn !== undefined ? _edgeShowOn = settings.edge.showOn : _edgeShowOn = 'rightClickEdge';
				settings.edge.hideOn !== undefined ? _edgeHideOn = settings.edge.hideOn : _edgeHideOn = 'clickStage';
				settings.edge.position !== undefined ? _edgePosition = settings.edge.position : _edgePosition = 'bottom';
				settings.edge.cssClass !== undefined ? _edgeCssClass = settings.edge.cssClass : _edgeCssClass = 'edge-tooltip-container';
				(settings.edge.renderer !== undefined && typeof settings.edge.renderer === 'function') ? _edgeRenderer = settings.edge.renderer : _edgeRenderer = undefined;
			}

			settings.page !== undefined ? _page = settings.page : _page = undefined;
			(settings.onShow !== undefined && typeof settings.onShow === 'function') ? _onShow = settings.onShow : _onShow  = undefined;
			(settings.onError !== undefined && typeof settings.onError === 'function') ? _onError = settings.onError : _onError = undefined;
		}
		else
			throw new Error('"Graph-Browser-Tooltips: No settings found."');

		_callbacks.api.closeTooltip = self.closeTooltip.bind(self);

		var config = {};

		if(_nodesEnabled){
			config.node = {
				show: _nodeShowOn,
				hide: _nodeHideOn,
				position: _nodePosition,
				template: '<div id="node-tooltip-container" class="' + _nodeCssClass + '"></div>',
				renderer: function(node, template) {
					if(_nodeRenderer)
						return _nodeRenderer(node);
					else{
						self.loadStructrPageContent(node);
						return "";
					}
				}
			}
		}

		if(_edgesEnabled){
			config.edge = {
				show: _edgeShowOn,
				hide: _edgeHideOn,
				position: _edgePosition,
				template: '<div id="edge-tooltip-container" class="' + _edgeCssClass + '"></div>',
				renderer: function(edge, template) {
					if(_edgeRenderer)
						return _edgeRenderer(edge);
					else{
						self.loadStructrPageContent(edge);
						return "";
					}
				}
			}
		}

		_tooltip = sigma.plugins.tooltips(_s, _s.renderers[0], config);
	};

	Graphbrowser.Modules.Toolstips.prototype.closeTooltip = function(){
		_tooltip.close();
	};

	Graphbrowser.Modules.Toolstips.prototype.loadStructrPageContent = function(element){
		var self = this;
		if(_page)
			_callbacks.conn.getStructrPage(_page, element.id, self.onContentLoaded, _onError);
		else if(_nodeRenderer)
			self.onContentLoaded(_nodeRenderer(element), node.id);
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