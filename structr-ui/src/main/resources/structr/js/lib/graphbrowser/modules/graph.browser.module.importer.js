var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _mode, _callbackOnSuccess, _callbackOnError;
	var _loadedNodes = [];
	var _loadedEdges = [];
	

	Graphbrowser.Modules.Importer = function (sigmaInstance, callbacks){
		var self = this;
		self.name = "importer";
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.Importer.prototype.types = ["graphImporter"];

	Graphbrowser.Modules.Importer.prototype.init = function(settings){
		var self = this;

		if(settings){
			(settings.onSuccess !== undefined && typeof settings.onSuccess === 'function') ? _callbackOnSuccess = settings.onSuccess : _callbackOnSuccess = undefined;
			(settings.onError !== undefined && typeof settings.onError === 'function') ? _callbackOnError = settings.onError : _callbackOnError = undefined;
		}
		else
			throw new Error("Graph-Browser-Importer: No settings found!");

		
		$(document).on('click', '#importer-controlElement', function(){			
			self.loadGraphviewsForCurrent();
		});

		$(document).on('click', '#importer-loadGraph-submit', function(){		
			_mode = "all";	
			self.loadSelectedView();
		});
	};

	Graphbrowser.Modules.Importer.prototype.loadGraphviewsForCurrent = function(){		
		var self = this;
		_callbacks.conn.getGraphViews(currentViewId, "graphBrowserInfo", self.pickGraphView.bind(self));
	};

	Graphbrowser.Modules.Importer.prototype.pickGraphView = function(resultData){		
		var self = this;
		var radioButtons = '';
		
		if(resultData.length <= 0){
			radioButtons += '<div><span>Keine Ansichten verfügbar</span></div>'
		}
		
		$.each(resultData, function(i, r){
			radioButtons = radioButtons + '<div class="radiobutton importer-loadGraph-radiobuttons"><label><input type="radio"  name="viewButtons" value="' + r.id +  '">' + r.name + '</label><div>';
		});

		$('#importer-loadGraph-graphNames').append(radioButtons);
		radioButtons = ""; 
	};

	Graphbrowser.Modules.Importer.prototype.loadSelectedView = function(){		
		var self = this;
		var selectedView = $("#importer-loadGraph-graphNames input[type='radio']:checked").val();
		if(selectedView !== undefined){
			_callbacks.conn.getGraphViewData(selectedView, "graphBrowser", self.restoreGraph.bind(self));			
		}
	};	

	Graphbrowser.Modules.Importer.prototype.restoreGraph = function(result){		
		var graph = JSON.parse(result.graph);
		var data = JSON.parse(result.data);
		_s.graph.clear();
		_s.graph.read(graph);
		_callbacks.graphRestored(data);
		_callbacks.dataChanged();
		_callbacks.refreshSigma(false, true);
		_callbackOnSuccess();
	};

}).call(window);