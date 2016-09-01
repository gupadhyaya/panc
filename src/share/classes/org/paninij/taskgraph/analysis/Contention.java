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
 * 
 * Looks for incoming edges from different capsules
 * 
 */
public class Contention extends Analysis {

	public Contention(TaskGraph graph) {
		super(graph);
		// TODO Auto-generated constructor stub
	}

	public void print() {
		System.out.println("============ Contention Analysis ============");
		for (Thread thread : this.graph.threads.values()) {
			System.out.println("Thread: " + thread.id);
			for (Queue queue : thread.queues) {
				// get all incoming edges and see if the
				List<Node> preds = queue.predecessors;
				int syncs = 0;
				List<Node> visitedPreds = new ArrayList<Node>();
				for (Node pred : preds) {
					if (visitedPreds.contains(pred))
						continue;
					List<Edge> edges = getEdges(queue, pred);
					for (Edge edge : edges) {
						if (edge.style == 0
								&& !((Task) edge.fromNode).threadId
										.equals(thread.id)) {
							syncs++;
						}
					}
					visitedPreds.add(pred);
				}
				System.out.println("Queue: " + queue.capsuleId
						+ ", #contentions = " + syncs);
				System.out.println("------------------------------------------");
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
