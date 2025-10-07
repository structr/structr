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
        let valueSource = new D3NE.Socket('valueSource', 'Value Data Source Node', 'The connected node will provide value data for this node.');
        let dataSources = new D3NE.Socket('dataSources', 'Data Source Nodes', 'The connected nodes will provide data for this node.');
        let dataTarget = new D3NE.Socket('dataTarget', 'Data Target Node', 'Connect to a node\'s DataSource port.');
        dataTarget.combineWith(dataSource);
        dataTarget.combineWith(dataSources);
        dataTarget.combineWith(valueSource);
        this._sockets['dataSource'] = dataSource;
        this._sockets['valueSource'] = valueSource;
        this._sockets['dataSources'] = dataSources;
        this._sockets['dataTarget'] = dataTarget;

        let parameters = new D3NE.Socket('parameters', 'Parameter Data Nodes', 'Multiple parameter nodes can be connected to this input.');
        let call = new D3NE.Socket('call', 'Call Node', 'Connects to a FlowCall and provides it with arguments.');
        call.combineWith(parameters);
        this._sockets['parameters'] = parameters;
        this._sockets['call'] = call;

        let condition_dataSources = new D3NE.Socket('dataSources', 'Data Source Nodes', 'The connected nodes will provide data for this node.');
        this._sockets['dataTarget'].combineWith(condition_dataSources);
        this._sockets['condition_dataSources'] = condition_dataSources;

        let condition_dataSource = new D3NE.Socket('dataSource', 'Data Source Node', 'The connected node will provide data for this node.');
        this._sockets['dataTarget'].combineWith(condition_dataSource);
        this._sockets['condition_dataSource'] = condition_dataSource;

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

        let startValue = new D3NE.Socket('startValue', 'Initial Data', 'Connected data source provides a start value for the element to work with.');
        this._sockets['dataTarget'].combineWith(startValue);
        this._sockets['startValue'] = startValue;

        let scriptSource = new D3NE.Socket('scriptSource', 'Script Source', 'Connected data source provides this node with a script to evaluate.');
        this._sockets['dataTarget'].combineWith(scriptSource);
        this._sockets['scriptSource'] = scriptSource;

        let nodeSource = new D3NE.Socket('nodeSource', 'Node source', 'Connected data source provides a node entity to extract a property from.');
        let propertyNameSource = new D3NE.Socket('propertyNameSource', 'Property name source', 'Connected node provides a name for the property to get from the node');
        this._sockets['dataTarget'].combineWith(nodeSource);
        this._sockets['dataTarget'].combineWith(propertyNameSource);
        this._sockets['nodeSource'] = nodeSource;
        this._sockets['propertyNameSource'] = propertyNameSource;

        let exceptionHandler = new D3NE.Socket('exceptionHandler', 'Exception Handler', 'Connects to an exception handler node.');
        let handledNodes = new D3NE.Socket('handledNodes', 'Handled Nodes', 'The exceptions of connected nodes will be handled by this element.');
        exceptionHandler.combineWith(handledNodes);
        this._sockets['exceptionHandler'] = exceptionHandler;
        this._sockets['handledNodes'] = handledNodes;

        let condition_baseNode = new D3NE.Socket('condition_baseNode', 'Condition Node', 'Connected node provides arguments for this node.');
        condition_Result.combineWith(condition_baseNode);
        this._sockets['condition_BaseNode'] = condition_baseNode;
        this._sockets['condition_Result'] = condition_Result;

        let forkBody = new D3NE.Socket('forkBody', 'ForkBody', 'Connected nodes will be executed in a new forked thread.');
        forkBody.combineWith(this._sockets['prev']);
        this._sockets['forkBody'] = forkBody;

        let switch_switch = new D3NE.Socket('switch', 'Switch element', 'Connected to a switch\'s cases port.');
        let switch_cases = new D3NE.Socket('cases', 'Case elements', 'Connects to a cases\'s switch port.');
        switch_cases.combineWith(switch_switch);
        this._sockets['switch'] = switch_switch;
        this._sockets['cases'] = switch_cases;

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