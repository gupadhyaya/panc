package org.paninij.taskgraph.analysis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.paninij.taskgraph.Edge;
import org.paninij.taskgraph.Node;
import org.paninij.taskgraph.TaskGraph;

public abstract class Analysis {
	protected final TaskGraph graph;

	public Analysis(TaskGraph graph) {
		this.graph = graph;
	}

	/*
	 * client analysis are required to specify what to compute at the visited
	 * node
	 */
	protected void compute(Node node) {
		// System.out.println(node.getToken());
	}

	public void BFS() {
		List<Node> visitedNodes = new ArrayList<Node>();
		List<String> visitedEdgeLabels = new ArrayList<String>();

		java.util.Queue<Node> toVisit = new LinkedList<Node>();

		// add root to the queue to begin traversal
		toVisit.add(graph.root);

		do {
			Node node = toVisit.remove();
			// visit
			compute(node);
			visitedNodes.add(node);
			List<Node> succs = node.getSuccessors();

			for (Node succ : succs) {
				if (!visitedNodes.contains(succ)) {
					toVisit.add(succ);
					Edge toVisitEdge = getNotVisitedEdge(visitedEdgeLabels,
							succ, node);
					if (toVisitEdge != null) {
						// correct edge traversal here
						// System.out.println("Edge : " + toVisitEdge.getToken());
						visitedEdgeLabels.add(toVisitEdge.getToken());
					}
				} else {
					// although we have visited a node,
					// a node may have multi edges
					Edge notVisitedEdge = getNotVisitedEdge(visitedEdgeLabels,
							node, succ);
					if (notVisitedEdge != null) {
						visitedEdgeLabels.add(notVisitedEdge.getToken());
						toVisit.add(succ);
					}
				}
			}

		} while (!toVisit.isEmpty());
	}

	private Edge getNotVisitedEdge(List<String> visitedEdgeLabels, Node node,
			Node succ) {
		try {
			List<String> succLabels = succ.getSuccessorsLabels(node.getToken());
			if (succLabels != null)
				for (String label : succLabels) {
					String edgeToken = succ.getToken() + "->" + node.getToken()
							+ ":" + label;
					if (!visitedEdgeLabels.contains(edgeToken))
						return getEdge(succ, node, label);
				}
		} catch (NullPointerException npe) {
			throw npe;
		}
		return null;
	}

	protected Edge getEdge(Node from, Node to, String label) {
		return this.graph.getEdge(from, to, label);
	}
}
