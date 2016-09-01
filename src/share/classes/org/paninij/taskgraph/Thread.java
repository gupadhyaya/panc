package org.paninij.taskgraph;

import java.util.ArrayList;
import java.util.List;

public class Thread {
	public final String id;
	public final List<String> capsules;
	public final List<Queue> queues;
	public final TaskGraph taskgraph;
	List<String> visitedCapsules = new ArrayList<String>();
	
	public Thread(String id, List<String> capsules, List<Queue> queues, TaskGraph taskgraph) {
		this.id = id;
		this.capsules = capsules;
		this.queues = queues;
		this.taskgraph = taskgraph;
	}
	
	public void start(List<String> cycleNames) {
		for (Queue queue : queues) {
			boolean check = check(cycleNames, queue.instance) ? true: false;
			List<Task> tasks = queue.processOne(check);
			if (tasks.size() == 0)
				continue;
			// connect queue to first task (queue, first)
			// connect last tasks back to queue
			connectTasksToQueue(queue, tasks);
			
			/*Task first = tasks.get(0);
			Task last = tasks.get(tasks.size() - 1);
			Edge queueToFirst = new Edge(queue, first); 
			Edge lastToQueue = new Edge(last, queue, 1);
			this.taskgraph.addEdge(queueToFirst);
			this.taskgraph.addEdge(lastToQueue);*/
			
			// invoke post process
			queue.postProcess(tasks);
			
			// updated visited capsules
			if (!visitedCapsules.contains(queue.instance))
				visitedCapsules.add(queue.instance);
		}
	}
	
	private void connectTasksToQueue(Queue queue, List<Task> tasks) {
		for (Task task : tasks) {
			if (!hasTaskEdges(task.predecessors, tasks)) {
				Edge queueToFirst = new Edge(queue, task, task.messageId); 
				this.taskgraph.addEdge(queueToFirst);
			} 
			if (!hasTaskEdges(task.successors, tasks)) {
				Edge lastToQueue = new Edge(task, queue, 1);
				this.taskgraph.addEdge(lastToQueue);
			}
		}
	}
	
	private boolean hasTaskEdges(List<Node> nodes, List<Task> tasks) {
		for (Node node : nodes) 
			if (node instanceof Task)
				if (tasks.contains((Task)node))
						return true;
		return false;
	}
		
	private boolean check(List<String> cycleNames, String name) {
		if (cycleNames.contains(name) && visitedCapsules.contains(name))
			return true;
		return false;
	}
}
