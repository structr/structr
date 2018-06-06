'use strict';

export class FlowSockets {

    constructor() {
        this._sockets = {};

        let prevSocket = new D3NE.Socket('prev', 'Previous node', 'Connect a node\'s next port to this.');
        let nextSocket = new D3NE.Socket('next', 'Next node', 'Connect to a node\'s prev port.');
        nextSocket.combineWith(prevSocket);
        this._sockets['prev'] = prevSocket;
        this._sockets['next'] = nextSocket;

        let dataSource = new D3NE.Socket('dataSource', 'Data Source Node', 'The connected node will provide data for this node.');
        let dataTarget = new D3NE.Socket('dataTarget', 'Data Target Node', 'Connect to a node\'s prev port.');
        dataTarget.combineWith(dataSource);
        this._sockets['dataSource'] = dataSource;
        this._sockets['dataTarget'] = dataTarget;

        let parameters = new D3NE.Socket('parameters', 'Parameter Data Nodes', 'Multiple parameter nodes can be connected to this input.');
        let call = new D3NE.Socket('call', 'Call Node', 'Connects to a FlowCall and provides it with arguments.');
        call.combineWith(parameters);
        this._sockets['parameters'] = parameters;
        this._sockets['call'] = call;

        let condition_dataSources = new D3NE.Socket('dataSources', 'Data Source Nodes', 'The connected nodes will provide data for this node.');
        this._sockets['dataTarget'].combineWith(condition_dataSources);
        this._sockets['condition_dataSources'] = condition_dataSources;

        let condition_Result = new D3NE.Socket('result', 'Decision Node or Logic Node', 'Connects to FlowDecision or FlowLogicNode and provides it with arguments.');
        let condition_Condition = new D3NE.Socket('condition', 'Condition Node', 'Connected node provides arguments for this node.');
        let condition_Conditions = new D3NE.Socket('conditions', 'Condition Nodes', 'Connected nodes will provide arguments for this node.');
        condition_Result.combineWith(condition_Conditions);
        condition_Result.combineWith(condition_Condition);
        this._sockets['condition_Result'] = condition_Result;
        this._sockets['condition_Condition'] = condition_Condition;
        this._sockets['condition_Conditions'] = condition_Conditions;

        let nextIfTrue = new D3NE.Socket('trueElement', 'Next node, if condition is true', 'Connect to a node\'s prev port.');
        let nextIfFalse = new D3NE.Socket('falseElement', 'Next node, if condition is false', 'Connect to a node\'s prev port.');
        nextIfTrue.combineWith(this._sockets['prev']);
        nextIfFalse.combineWith(this._sockets['prev']);
        this._sockets['nextIfTrue'] = nextIfTrue;
        this._sockets['nextIfFalse'] = nextIfFalse;

        let keyValueSources = new D3NE.Socket('keyValueSources', 'KeyValue nodes', 'Connected nodes provide KeyValue pairs of data for the object created by this data source.');
        let objectDataTarget = new D3NE.Socket('objectDataTarget', 'ObjectDataSource', 'Connects to a FlowObjectDataSource and provides it with a KeyValue pair of information.');
        objectDataTarget.combineWith(keyValueSources);
        this._sockets['keyValueSources'] = keyValueSources;
        this._sockets['objectDataTarget'] = objectDataTarget;

        let loopBody = new D3NE.Socket('loopBody', 'Start node', 'Connects to the first node of the loop that will get executed for each element of input data collection.');
        loopBody.combineWith(this._sockets['prev']);
        this._sockets['loopBody'] = loopBody;

        let startValue = new D3NE.Socket('startValue', 'Start value', 'Connected data source provides a start value for the element to work with.');
        this._sockets['dataTarget'].combineWith(startValue);
        this._sockets['startValue'] = startValue;

        let scriptSource = new D3NE.Socket('scriptSource', 'Script Source', 'Connected data source provides this node with a script to evaluate.');
        this._sockets['dataTarget'].combineWith(scriptSource);
        this._sockets['scriptSource'] = scriptSource;

    }

    static getInst() {
        if (window._flowSocketInst === undefined) {
            window._flowSocketInst = new FlowSockets();
        }
        return _flowSocketInst;
    }

    getSocket(key) {
        return this._sockets[key];
    }

}