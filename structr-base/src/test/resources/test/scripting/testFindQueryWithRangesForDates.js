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

let getSearchPredicatesWithRangePredicate = (begin, end, project, notTask) => {

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

let getSimplifiedSearchPredicates = (begin, end, project, notTask) => {

	return $.predicate.and(
		$.predicate.equals('project', project),
		$.predicate.not($.predicate.equals('id', notTask.id)),

		$.predicate.and(

			// task.beginDate < end
			$.predicate.lt('beginDate', end),

			// task.endDate > begin
			$.predicate.gt('endDate', begin)
		)
	);
};

let getSearchPredicatesWithOverlapsPredicate = (begin, end, project, notTask) => {

	return $.predicate.and(
		$.predicate.equals('project', project),
		$.predicate.not($.predicate.equals('id', notTask.id)),
		$.predicate.overlaps('beginDate', 'endDate', begin, end)
	);
};

{
	let project = projects[0];
	let notTask = tasks[2];
	let begin   = new Date(2021, 0, 1);
	let end     = new Date(2023, 0, 1);

	let foundTasks1 = $.find('Task', getSearchPredicatesWithRangePredicate(begin, end, project, notTask));
	let foundTasks2 = $.find('Task', getSimplifiedSearchPredicates(begin, end, project, notTask));
	let foundTasks3 = $.find('Task', getSearchPredicatesWithOverlapsPredicate(begin, end, project, notTask));

	let expected = 'Task0,Task1';
	let actual1 = foundTasks1.map(t => t.name).sort().join(',');
	$.assert(expected === actual1, 422, `Found '${actual1}', but when searching tasks for Project ${project.name} excluding task ${notTask.name} and with a very wide time range (${begin} - ${end}), we should get 2 tasks (${expected}).`);

	let actual2 = foundTasks2.map(t => t.name).sort().join(',');
	$.assert(expected === actual2, 422, `Found '${actual2}', but when searching tasks for Project ${project.name} excluding task ${notTask.name} and with a very wide time range (${begin} - ${end}), we should get 2 tasks (${expected}).`);

	let actual3 = foundTasks3.map(t => t.name).sort().join(',');
	$.assert(expected === actual3, 422, `Found '${actual3}', but when searching tasks for Project ${project.name} excluding task ${notTask.name} and with a very wide time range (${begin} - ${end}), we should get 2 tasks (${expected}).`);
}

{
	let project = projects[0];
	let notTask = tasks[1];
	let begin   = new Date(2022, 0, 1);
	let end     = new Date(2022, 0, 10);

	let foundTasks1 = $.find('Task', getSearchPredicatesWithRangePredicate(begin, end, project, notTask));
	let foundTasks2 = $.find('Task', getSimplifiedSearchPredicates(begin, end, project, notTask));
	let foundTasks3 = $.find('Task', getSearchPredicatesWithOverlapsPredicate(begin, end, project, notTask));

	let expected = 'Task0';
	let actual1 = foundTasks1.map(t => t.name).sort().join(',');
	$.assert(expected === actual1, 422, `Found '${actual1}', but when searching tasks for Project ${project.name} excluding task ${notTask.name} and with a narrow time range (${begin} - ${end}), we should get 1 task (${expected}).`);

	let actual2 = foundTasks2.map(t => t.name).sort().join(',');
	$.assert(expected === actual2, 422, `Found '${actual2}', but when searching tasks for Project ${project.name} excluding task ${notTask.name} and with a narrow time range (${begin} - ${end}), we should get 1 task (${expected}).`);

	let actual3 = foundTasks3.map(t => t.name).sort().join(',');
	$.assert(expected === actual3, 422, `Found '${actual3}', but when searching tasks for Project ${project.name} excluding task ${notTask.name} and with a narrow time range (${begin} - ${end}), we should get 1 task (${expected}).`);
}

"Success";