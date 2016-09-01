package org.paninij.taskgraph.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.paninij.analysis.Block;
import org.paninij.taskgraph.Edge;
import org.paninij.taskgraph.Node;
import org.paninij.taskgraph.Queue;
import org.paninij.taskgraph.Task;
import org.paninij.taskgraph.TaskGraph;

public class Parallelism extends Analysis {

	public Parallelism(TaskGraph graph) {
		super(graph);
		// TODO Auto-generated constructor stub
	}

	// prints potential parallelism of each thread
	public void print() {
		System.out
				.println("============ Potential Parallelism Analysis ============");
		for (Edge edge : this.graph.edges) {
			if (edge.fromNode instanceof Task && edge.toNode instanceof Queue
					&& edge.style == 0) {
				Task from = (Task) edge.fromNode;
				Queue to = (Queue) edge.toNode;
				String currentMessageId = trim(from.messageId);
				String qId = from.capsuleId.substring(0,
						from.capsuleId.indexOf("!"));
				Queue senderQ = this.graph.queues.get(qId);
				if (senderQ == null)
					continue;
				// System.out.println("Something wrong! queue cannot be null");
				int senderOtherEdges = 0;
				for (Node succ : senderQ.getSuccessors()) {
					List<Edge> edges = getEdges(succ, senderQ);
					for (Edge otherEdge : edges) {
						if (otherEdge.style == 0
								&& !otherEdge.edgeLabel
										.equals(currentMessageId))
							senderOtherEdges++;
					}
				}

				int receiverOtherEdges = 0;
				for (Node succ : to.getSuccessors()) {
					List<Edge> edges = getEdges(succ, to);
					for (Edge otherEdge : edges) {
						if (otherEdge.style == 0)
							receiverOtherEdges++;
					}
				}
				
				String senderPar = "serial";
				String receiverPar = "serial";
				if (((((Task) edge.fromNode).type & 16) != 0)
						|| (senderOtherEdges != 0))
					// if both threadIds are same ignore
					if (!from.threadId.equals(to.threadId))
						senderPar = "parallel";
				if (receiverOtherEdges > 1)
					receiverPar = "parallel";
				else if (receiverOtherEdges == 1)
					receiverPar = hasLoopInPath(to) ? "parallel" : "serial";
				System.out.println(from.threadId + " --- (" + edge.getLabel()
						+ ") ---> " + to.threadId + ", sender type: "
						+ senderPar + ", receiver type: " + receiverPar);
			}
		}
	}
	
	private boolean hasLoopInPath(Queue to) {
		for (Node succ : to.getSuccessors()) {
			List<Edge> edges = getEdges(succ, to);
			for (Edge otherEdge : edges) {
				if (otherEdge.style == 0)
					return hasLoopInPath(succ);
			}
		}
		return false;
	}
	
	private boolean hasLoopInPath(Node node) {
		boolean loop = false;
		List<Node> visited = new ArrayList<Node>();
		java.util.Queue<Node> toVisit = new LinkedList<Node>();
		toVisit.add(node);
		do {
			Node block = toVisit.remove();
			visited.add(block);
			// visit node and check if it is a loop
			if (block instanceof Task) {
				Task t = (Task) block;
				if ((t.type & 16) != 0)
					return true;
			}
			List<Node> succs = block.getSuccessors();
			for (Iterator<Node> succsIter = succs.iterator(); succsIter
					.hasNext();) {
				Node succ = succsIter.next();
				if (!visited.contains(succ) && (succ instanceof Task)) {
					toVisit.add(succ);
				} 
			}
		} while (!toVisit.isEmpty());
		return loop;
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
