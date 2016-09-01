package org.paninij.taskgraph;

public class Edge {
	public final int id;
	public static int idC = -1;
	private final String token;
	private final String connector = " --> ";
	public final Node fromNode, toNode;
	
	public final int style;
	public final String edgeLabel;
	
	public Edge(Node fromNode, Node toNode) {
		this.id = ++idC;
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.style = 0;
		this.edgeLabel = "";
		this.token = fromNode.getToken() + "->" + toNode.getToken() + ":" + this.id;
		addSuccessor(fromNode, toNode);
		addPredecessor(toNode, fromNode);
	}
	
	public Edge(Node fromNode, Node toNode, String label) {
		this.id = ++idC;
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.style = 0;
		this.edgeLabel = trim(label);
		this.token = fromNode.getToken() + "->" + toNode.getToken() + ":" + this.id;
		addSuccessor(fromNode, toNode);
		addPredecessor(toNode, fromNode);
	}
	
	public Edge(Node fromNode, Node toNode, int style) {
		this.id = ++idC;
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.style = style;
		this.edgeLabel = "";
		this.token = fromNode.getToken() + "->" + toNode.getToken() + ":" + this.id;
		addSuccessor(fromNode, toNode);
		addPredecessor(toNode, fromNode);
	}
	
	public Edge(Node fromNode, Node toNode, int style, String label) {
		this.id = ++idC;
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.style = style;
		this.edgeLabel = label;
		this.token = fromNode.getToken() + "->" + toNode.getToken() + ":" + this.id;
		addSuccessor(fromNode, toNode);
		addPredecessor(toNode, fromNode);
	}
	
	@Override
	public String toString() {
		String str;
		str = fromNode.toString() + connector + toNode.toString();
		return str;
	}
	
	public String toDotGraph() {
		String tmp;
		tmp = "\"" + fromNode.getToken() + "\"" +
				" -> " + 
				"\"" + toNode.getToken() + "\"" ;
		if (style == 1 || this.edgeLabel != "") {
			tmp += "[";
			if (style == 1) {
				tmp += "style=dotted,";
			}
			if (this.edgeLabel != "") {
				tmp += "label=" + "\"" + this.edgeLabel + "\"";
			}
			tmp += "]";
		}
		
		tmp += ";"; 
		return tmp;
	}
	
	private void addSuccessor(Node node, Node succ) {
		node.addSuccessor(succ, ""+this.id);
	}
	
	private void addPredecessor(Node node, Node pred) {
		node.addPredecessor(pred);
	}
	
	@Override
	public boolean equals(Object obj) {
		Edge edge = (Edge) obj;
		
		if (this.fromNode.equals(edge.fromNode) &&
				this.toNode.equals(edge.toNode) /*&&
				 this.edgeLabel.equals(edge.edgeLabel)*/)
			return true;
		
		return false;
	}
	
	public String getLabel() {
		return edgeLabel;
	}
	
	public String getToken() {
		return token;
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
}
