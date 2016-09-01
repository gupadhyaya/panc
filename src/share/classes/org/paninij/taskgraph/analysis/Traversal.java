package org.paninij.taskgraph.analysis;

import org.paninij.taskgraph.Node;
import org.paninij.taskgraph.TaskGraph;

public class Traversal extends Analysis {

	public Traversal(TaskGraph graph) {
		super(graph);
	}
	
	@Override
	protected void compute(Node node) {
		//System.out.println(node.toString());
	}

}
