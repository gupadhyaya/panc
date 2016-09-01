package org.paninij.taskgraph.analysis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.paninij.taskgraph.Edge;
import org.paninij.taskgraph.Node;
import org.paninij.taskgraph.Queue;
import org.paninij.taskgraph.Task;
import org.paninij.taskgraph.TaskGraph;

/**
 * lists batching opportunities
 * 
 * 
 */
public class Batching extends Analysis {

	public Batching(TaskGraph graph) {
		super(graph);
		// TODO Auto-generated constructor stub
	}

	public void print() {
		System.out.println("============ Batching Opportunities ============");
		for (Edge edge : this.graph.edges) {
			if (edge.fromNode instanceof Task && edge.toNode instanceof Queue
					&& edge.style == 0) {
				Task from = (Task) edge.fromNode;
				Queue to = (Queue) edge.toNode;
				String currentMessageId = trim(from.messageId);
				boolean isLoop = ((((Task) edge.fromNode).type & 16) != 0);
				if (isLoop) {
					// first check if there is only one path from queue along
					// currentMessageId
					int otherEdges = 0;
					for (Node succ : to.getSuccessors()) {
						List<Edge> edges = getEdges(succ, to);
						for (Edge otherEdge : edges) {
							if (otherEdge.style == 0
									&& !otherEdge.edgeLabel
											.equals(currentMessageId))
								otherEdges++;
						}
					}
					boolean isCurrentPathStateW = false;
					if (otherEdges == 0) {
						// only current path from queue along currentMessageId
						// visit the path from queue along currentMessageId
						// check if all the tasks are nil or state_r only
						// if yes, print the batching opportunity
						isCurrentPathStateW = checkPathFromQueueAlong(to,
								currentMessageId);
						if (!isCurrentPathStateW)
							System.out
									.println(from.threadId + " --- ("
											+ edge.getLabel() + ") ---> "
											+ to.threadId);
					} else {
						// there exists more paths to explore
						// all paths read only is fine
						// even a single write then reject
						for (Node succ : to.getSuccessors()) {
							String edgeLabel = to.getSuccessorsLabels(
									succ.getToken()).get(0);
							isCurrentPathStateW = isCurrentPathStateW
									|| checkPathFromQueueAlong(to, edgeLabel);
						}
						if (!isCurrentPathStateW)
							System.out
									.println(from.threadId + " --- ("
											+ edge.getLabel() + ") ---> "
											+ to.threadId);
					}
				}
			}
		}
	}

	// visit all the task nodes to check their state accesses
	private boolean checkPathFromQueueAlong(Queue queue, String messageId) {
		boolean stateW = false;

		List<Node> visited = new ArrayList<Node>();
		java.util.Queue<Node> toVisit = new LinkedList<Node>();
		toVisit.add(queue);
		do {
			Node node = toVisit.remove();
			List<Node> succs = node.getSuccessors();
			for (Node succ : succs) {
				if (!visited.contains(succ) && (succ instanceof Task)) {
					toVisit.add(succ);
				}
			}
			// check the state access pattern of the node
			if (!(node instanceof Task))
				continue;
			Task n = (Task) node;
			stateW = ((n.type & 4) != 0);
			if (stateW)
				break;
			visited.add(node);
		} while (!toVisit.isEmpty());

		return stateW;
	}

	private String trim(String label) {
		if (label.contains("Original")) {
			String parts[] = label.split("Original");
			String str = "";
			str += parts[0].substring(0, parts[0].indexOf("$"));
			str += parts[1];
			return str;
		}

		return label;
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
