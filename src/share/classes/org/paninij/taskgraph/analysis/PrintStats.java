package org.paninij.taskgraph.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.paninij.taskgraph.Edge;
import org.paninij.taskgraph.Node;
import org.paninij.taskgraph.Queue;
import org.paninij.taskgraph.SharedObject;
import org.paninij.taskgraph.TaskGraph;

public class PrintStats extends Analysis {

	public PrintStats(TaskGraph graph) {
		super(graph);
	}
	
	public static int mean(int[] m) {
		int sum = 0;
	    for (int i = 0; i < m.length; i++) {
	        sum += m[i];
	    }
	    return sum / m.length;
	}
	
	public static int median(int[] m) {
	    int middle = m.length/2;
	    if (m.length%2 == 1) {
	        return m[middle];
	    } else {
	        return (m[middle-1] + m[middle]) / 2;
	    }
	}



	public void print() {
		int qs = this.graph.queues.size();
		int objs = this.graph.objects.size();
		int tasks = this.graph.tasks.size();
		int threads = this.graph.threads.size();
		int n = (qs+objs+tasks);
		int edges = getValidEdges();
		
		System.out.println("=============== Taskgraph stats ===============");
		//System.out.println("threads : " + threads);
		//System.out.println("queues : " + qs);
		//System.out.println("shared objects : " + objs);
		//System.out.println("tasks : " + tasks);
		System.out.println("Number of MPC Processes: " + qs);
		System.out.println("Number of nodes: " + n);
		System.out.println("Number of edges: "+ edges);
		
		int[] ins = new int[n];
		int[] outs = new int[n];
		
		//Arrays.sort(ins);
		int i = 0, o = 0;
		for (Node node : this.graph.queues.values()) {
			int in = getInDegree(node);
			int out = getOutDegree(node);
			
			ins[i++] = in; 
			outs[o++] = out;
		}
		for (Node node : this.graph.objects.values()) {
			int in = getInDegree(node);
			int out = getOutDegree(node);
			
			ins[i++] = in; 
			outs[o++] = out;
		}
		for (Node node : this.graph.tasks) {
			int in = getInDegree(node);
			int out = getOutDegree(node);
			
			ins[i++] = in; 
			outs[o++] = out;
		}
		
		for (int j = 0; j < ins.length; j++) {
			System.out.print(ins[j] + ",");
		}
		
		System.out.println();
		
		for (int j = 0; j < outs.length; j++) {
			System.out.print(outs[j] + ",");
		}
		
		System.out.println();
		//Arrays.sort(ins);
		//Arrays.sort(outs);
		
		/*int insMin = ins[0], insMax = ins[n-1];
		int outMin = outs[0], outMax = outs[n-1];
		
		int insMedian = 0, outsMedian = 0;
		int insFirstHalfMedian = 0, outsFirstHalfMedian = 0;
		int insSecondHalfMedian = 0, outsSecondHalfMedian = 0;
		
		*/ 
		
		// for each queue print its stats
		/*for (Queue queue : this.graph.queues.values()) {
			System.out.println("============ Queue : " + queue.toString()
					+ " ============");
			List<Node> preds = queue.predecessors;
			int syncs = 0;
			int orderProblems = 0;
			List<Node> visitedPreds = new ArrayList<Node>();
			for (Node pred : preds) {
				if (visitedPreds.contains(pred))
					continue;
				List<Edge> edges = getEdges(queue, pred);
				for (Edge edge : edges) {
					if (edge.style == 0) {
						syncs++;
					} else {
						orderProblems++;
					}
				}
				visitedPreds.add(pred);
			}
			System.out.println("Number of wait-notify synchronizations : "
					+ syncs);
			System.out
					.println("Number of ordering problems : " + orderProblems);
		}

		// for each shared capsule print its stats
		for (SharedObject obj : this.graph.objects.values()) {
			System.out.println("============ SharedObject : " + obj.toString()
					+ " ============");
			List<Node> preds = obj.predecessors;
			int syncs = 0;
			List<Node> visitedPreds = new ArrayList<Node>();
			for (Node pred : preds) {
				if (visitedPreds.contains(pred))
					continue;
				List<Edge> edges = getEdges(obj, pred);
				for (Edge edge : edges) {
					if (edge.style == 0) {
						syncs++;
					}
				}
				visitedPreds.add(pred);
			}
			System.out.println("Number of monitor synchronizations : " + syncs);
		}*/
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
	
	private int getValidEdges() {
		int count = 0;
		for (Edge edge : this.graph.edges) {
			if (edge.style == 0) {
				count++;
			}
		}
		return count;
	}

	private int getInDegree(Node node) {
		List<Node> preds = node.getPredecessors();
		int ins = 0;
		List<Node> visitedPreds = new ArrayList<Node>();
		for (Node pred : preds) {
			if (visitedPreds.contains(pred))
				continue;
			List<Edge> edges = getEdges(node, pred);
			for (Edge edge : edges) {
				if (edge.style == 0)
						ins++;
			}
			visitedPreds.add(pred);
		}
		return ins;
	}
	
	private int getOutDegree(Node node) {
		List<Node> succs = node.getSuccessors();
		int outs = 0;
		List<Node> visitedSuccs = new ArrayList<Node>();
		for (Node succ : succs) {
			if (visitedSuccs.contains(succ))
				continue;
			List<Edge> edges = getEdges(succ, node);
			for (Edge edge : edges) {
				if (edge.style == 0)
					outs++;
			}
			visitedSuccs.add(succ);
		}
		return outs;
	}
}
