let tasks    = [];
let projects = [];

for (var i=0; i<6; i++) {
	tasks.push($.create('Task', { name: 'Task' + i }));
}

for (var i=0; i<4; i++) {

	let project = $.create('Project', { name: 'Project' + i });

	projects.push(project);

	project.tasks.push(tasks[i+0]);
	project.tasks.push(tasks[i+1]);
	project.tasks.push(tasks[i+2]);
}

let equals = $['find.equals'];
let search = [];

search.push(equals('tasks', [ tasks[0] ]));
search.push(equals('tasks', [ tasks[1] ]));
search.push(equals('tasks', [ tasks[5] ]));

$.find('Project', $.predicate.or(search), $.predicate.sort('name'));

