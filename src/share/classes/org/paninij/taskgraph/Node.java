package org.paninij.taskgraph;

import java.util.List;
import java.util.Map;

public abstract class Node {
	protected int token;
	protected static int TOKENS = -1;
	
	public Node() {
		token = ++TOKENS;
	}
	
	protected String delim = "\\n"; //"!!";
	public abstract void setConnections(Map<String, Node> connections);
	public abstract int getToken();
	public abstract List<Node> getSuccessors();
	public abstract List<Node> getPredecessors();
	public abstract List<String> getSuccessorsLabels(int succ);
	public abstract void addSuccessor(Node node, String label);
	public abstract void addPredecessor(Node node);
}