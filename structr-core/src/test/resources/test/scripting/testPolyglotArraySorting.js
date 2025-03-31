for (let i = 0; i < 10; i++) {
    $.create('TestOne', 'name', 'TestOne' + i);
}

let nodes = $.find('TestOne');

nodes.sort((a,b) => {
    if (a.name > b.name) {
        return -1;
    } else if (b.name > a.name) {
        return 1;
    } else {
        return 0;
    }
});

nodes;