package org.paninij.taskgraph.analysis;

import java.util.ArrayList;
import java.util.List;

import org.paninij.taskgraph.Edge;
import org.paninij.taskgraph.Node;
import org.paninij.taskgraph.Queue;
import org.paninij.taskgraph.Task;
import org.paninij.taskgraph.TaskGraph;
import org.paninij.taskgraph.Thread;

/**
 * Look for tasks that sends message part of a loop
 * 
 * 
 */
public class Communication extends Analysis {

	public Communication(TaskGraph graph) {
		super(graph);
		// TODO Auto-generated constructor stub
	}

	public void print() {
		System.out.println("============ Communication Analysis ============");
		for (Thread thread : this.graph.threads.values()) {
			System.out.println("Thread: " + thread.id);
			for (Queue queue : thread.queues) {
				// get all incoming edges and see if the
				List<Node> preds = queue.predecessors;
				int incomings = 0;
				List<Node> visitedPreds = new ArrayList<Node>();
				System.out.println("Queue: " + queue.capsuleId);
				for (Node pred : preds) {
					if (visitedPreds.contains(pred))
						continue;
					List<Edge> edges = getEdges(queue, pred);
					for (Edge edge : edges) {
						if (edge.style == 0
								&& !((Task) edge.fromNode).threadId
										.equals(thread.id)) {
							incomings++;
							String type = ((((Task) edge.fromNode).type & 16) != 0) ? "many"
									: "one";
							System.out.println("From : "
									+ ((Task) edge.fromNode).threadId + "("
									+ ((Task) edge.fromNode).capsuleId + ")"
									+ ", msg: " + edge.edgeLabel + ", type: "
									+ type);
						}
					}
					visitedPreds.add(pred);
				}
				System.out.println("Total incoming : " + incomings);
				System.out
						.println("------------------------------------------");
			}
		}
	}

	private List<Edge> getEdges(Node node, Node from) {
		List<Edge> edges = new ArrayList<Edge>();
		List<String> succLabels = from.getSuccessorsLabels(node.getToken());
		if (succLabels != null)
			for (String label : succLabels) {
				String edgeToken = from.getToken() + "->" + node.getToken()
						+ ":" + label;
				Edge edge = getEdge(from, node, label);
				if (edge != null) // && edge.style == style
					edges.add(edge);
			}
		return edges;
	}
}
