let tasks    = [];
let projects = [];

for (var i=0; i < 12; i++) {
	tasks.push($.getOrCreate('Task', {
		name: 'Task' + i,
		beginDate: new Date(2022, i, 1),
		endDate: new Date(2022, i, 20),
	}));
}

for (var i=0; i < 4; i++) {

	let project = $.getOrCreate('Project', { name: 'Project' + i });

	projects.push(project);

	project.tasks = [tasks[3*i + 0], tasks[3*i + 1], tasks[3*i + 2]];
}

let getSearchPredicates = (begin, end, project, notTask) => {
	return $.predicate.and(
		$.predicate.equals('project', project),
		$.predicate.not($.predicate.equals('id', notTask.id)),

		$.predicate.or(

			// begin <= task.beginDate < end   (task.beginDate inside search interval or exactly at beginning of search interval)
			$.predicate.equals('beginDate', $.predicate.range(begin, end, true, false)),

			// begin < task.endDate <= end   (task.endDate inside search interval or exactly at end of search interval)
			$.predicate.equals('endDate', $.predicate.range(begin, end, false, true)),

			// complete search interval is inside task interval
			$.predicate.and(
				// task.beginDate < begin
				$.predicate.equals('beginDate', $.predicate.range(null, begin)),

				// task.enDate > end
				$.predicate.equals('endDate', $.predicate.range(end, null))
			)
		)
	);
};

{
	let project = projects[0];
	let notTask = tasks[2];
	let begin   = new Date(2021, 0, 1);
	let end     = new Date(2023, 0, 1);

	let foundTasks = $.find('Task', getSearchPredicates(begin, end, project, notTask));

	let expected = 'Task0,Task1';
	let actual = foundTasks.map(t => t.name).sort().join(',');
	$.assert(expected === actual, 422, `Found '${actual}', but when searching tasks for Project ${project.name} excluding task ${notTask.name} and with a very wide time range (${begin} - ${end}), we should get 2 tasks (${expected}).`);
}

{
	let project = projects[0];
	let notTask = tasks[1];
	let begin   = new Date(2022, 0, 1);
	let end     = new Date(2022, 0, 10);

	let foundTasks = $.find('Task', getSearchPredicates(begin, end, project, notTask));

	let expected = 'Task0';
	let actual = foundTasks.map(t => t.name).sort().join(',');
	$.assert(expected === actual, 422, `Found '${actual}', but when searching tasks for Project ${project.name} excluding task ${notTask.name} and with a narrow time range (${begin} - ${end}), we should get 1 task (${expected}).`);
}

"Success";