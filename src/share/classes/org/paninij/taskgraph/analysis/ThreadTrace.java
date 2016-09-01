package org.paninij.taskgraph.analysis;

import java.util.ArrayList;
import java.util.List;

import org.paninij.taskgraph.Edge;
import org.paninij.taskgraph.Queue;
import org.paninij.taskgraph.SharedObject;
import org.paninij.taskgraph.Task;
import org.paninij.taskgraph.TaskGraph;
import org.paninij.taskgraph.Thread;

public class ThreadTrace extends Analysis {

	public ThreadTrace(TaskGraph graph) {
		super(graph);
	}

	/**
	 * print dotgraph for each thread consisting of its trace
	 */
	public void trace() {
		for (org.paninij.taskgraph.Thread thread : this.graph.threads.values()) {
			System.out.println("============ Thread : " + thread.toString()
					+ "============");
			System.out.println(getThreadDot(thread));
		}
	}

	private String getThreadDot(Thread thread) {
		List<Queue> tQueues = new ArrayList<Queue>();
		List<SharedObject> tObjects = new ArrayList<SharedObject>();
		List<Task> tTasks = new ArrayList<Task>();

		StringBuilder sb = new StringBuilder();
		String tmp = "";

		tmp = "digraph " + thread.id + " {\n";
		sb.append(tmp);

		for (Queue queue : thread.queues) {
			tQueues.add(queue);
			sb.append(queue.toDotGraph() + "\n");
		}
		// sb.append("\n");

		for (SharedObject object : this.graph.objects.values()) {
			if (thread.capsules.contains(object.instance)) {
				tObjects.add(object);
				sb.append(object.toDotGraph() + "\n");
			}
		}
		// sb.append("\n");

		for (Task task : this.graph.tasks) {
			if (task.threadId.equals(thread.id)) {
				tTasks.add(task);
				sb.append(task.toDotGraph() + "\n");
			}/*
			 * else { System.out.println(task.threadId); }
			 */
		}
		// sb.append("\n");

		for (Edge edge : this.graph.edges) {
			// skip the dotted edges from queue to task
			if (edge.style == 1)
				continue;
			if (edge.fromNode instanceof Queue) {
				Queue fromNode = (Queue) edge.fromNode;
				if (fromNode.threadId.equals(thread.id)) {
					// make sure the node of the edge are created
					if (!tQueues.contains(fromNode)) {
						tQueues.add(fromNode);
						sb.append(fromNode.toDotGraph() + "\n");
					}
					Task toNode = (Task) edge.toNode;
					if (!tTasks.contains(toNode)) {
						tTasks.add(toNode);
						sb.append(toNode.toDotGraph() + "\n");
					}
					sb.append(edge.toDotGraph() + "\n");
				} else {
					if (edge.toNode instanceof Task) {
						Task toNode = (Task) edge.toNode;
						if (toNode.threadId.equals(thread.id))
							sb.append(edge.toDotGraph() + "\n");
					}
				}
			} else if (edge.fromNode instanceof SharedObject) {
				if (edge.toNode instanceof Task) {
					Task toNode = (Task) edge.toNode;
					if (toNode.threadId.equals(thread.id)) {
						// create nodes if not created
						if (!tTasks.contains(toNode)) {
							tTasks.add(toNode);
							sb.append(toNode.toDotGraph() + "\n");
						}
						SharedObject fromNode = (SharedObject) edge.fromNode;
						if (!tObjects.contains(fromNode)) {
							tObjects.add(fromNode);
							sb.append(fromNode.toDotGraph() + "\n");
						}
						sb.append(edge.toDotGraph() + "\n");
					}
				}
			} else {
				Task fromNode = (Task) edge.fromNode;
				if (fromNode.threadId.equals(thread.id)) {
					if (!tTasks.contains(fromNode)) {
						if (!tTasks.contains(fromNode)) {
							tTasks.add(fromNode);
							sb.append(fromNode.toDotGraph() + "\n");
						}
					}

					if (edge.toNode instanceof Queue) {
						Queue toNode = (Queue) edge.toNode;
						if (!tQueues.contains(toNode)) {
							tQueues.add(toNode);
							sb.append(toNode.toDotGraph() + "\n");
						}
					} else if (edge.toNode instanceof SharedObject) {
						SharedObject toNode = (SharedObject) edge.toNode;
						if (!tObjects.contains(toNode)) {
							tObjects.add(toNode);
							sb.append(toNode.toDotGraph() + "\n");
						}
					} else {
						Task toNode = (Task) edge.toNode;
						if (!tTasks.contains(toNode)) {
							tTasks.add(toNode);
							sb.append(toNode.toDotGraph() + "\n");
						}
					}
					sb.append(edge.toDotGraph() + "\n");
				}
			}
			// sb.append(edge.toDotGraph() + "\n");
		}
		// sb.append("\n");

		tmp = "}";
		sb.append(tmp);
		return sb.toString();
	}

}
