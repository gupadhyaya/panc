package org.paninij.taskgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.paninij.analysis.Block;
import org.paninij.analysis.BlockGraph;
import org.paninij.systemgraph.SystemGraph.Node;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCForeach;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.util.Name;

public class TaskGraph {
	public org.paninij.taskgraph.Node root;
	public Set<Task> tasks = new HashSet<Task>();
	public Map<String, Queue> queues = new HashMap<String, Queue>();
	public Map<String, Thread> threads = new HashMap<String, Thread>();
	public Map<String, SharedObject> objects = new HashMap<String, SharedObject>();
	public Set<Edge> edges = new HashSet<Edge>();
	public Map<String, Edge> hashedEdges = new HashMap<String, Edge>();
	public final SchedulingInfo schedInfo;
	public final Map<String, Map<String, BlockGraph>> capsuleGraphs;
	
	private int MAX = 100;
	private int iterations = 0;
	
	public TaskGraph(SchedulingInfo schedInfo, Map<String, Map<String, BlockGraph>> capsuleGraphs) {
		this.schedInfo = schedInfo;
		this.capsuleGraphs = capsuleGraphs;
	}
	
	/**
	 * Fix-point taskgraph building algorithm
	 */
	public void build(List<Name> cycleNames) {
		initThreads();
		List<String> strNames = toStringNames(cycleNames);
		// proceed until all the queues are empty.
		while (!isAllEmpty() && (iterations < MAX)) {
			/*for (Queue queue : queues.values()) {
				queue.process();
			}*/
			for (String tId : this.threads.keySet()) {
				Thread t = this.threads.get(tId);
				t.start(strNames);
			}
			
			iterations++;
		}
	}
	
	private List<String> toStringNames(List<Name> cycleNames) {
		List<String> strNames = new ArrayList<String>();
		for (Name name : cycleNames) {
			if (!strNames.contains(name.toString()))
				strNames.add(name.toString());
		}
		return strNames;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(" ====== Queues ====== \n");
		for (Queue queue : this.queues.values()) {
			sb.append(queue.toString() + "\n");
		}
		sb.append("\n");
		
		sb.append(" ====== SharedCapsules ====== \n");
		for (SharedObject object : this.objects.values()) {
			sb.append(object.toString() + "\n");
		}
		sb.append("\n");
		
		sb.append(" ====== Tasks ====== \n");
		for (Task task : this.tasks) {
			sb.append(task.toString() + "\n");
		}
		sb.append("\n");
		
		sb.append(" ====== Edges ====== \n");
		for (Edge edge : this.edges) {
			sb.append(edge.toString() + "\n");
		}
		sb.append("\n");
		
		return sb.toString();
	}
	
	public String toDotGraph() {
		StringBuilder sb = new StringBuilder();
		String tmp = "";
		
		tmp = "digraph {\n"; 
		sb.append(tmp);
		
		for (Queue queue : this.queues.values()) {
			sb.append(queue.toDotGraph() + "\n");
		}
		//sb.append("\n");
		
		for (SharedObject object : this.objects.values()) {
			sb.append(object.toDotGraph() + "\n");
		}
		//sb.append("\n");
		
		for (Task task : this.tasks) {
			sb.append(task.toDotGraph() + "\n");
		}
		//sb.append("\n");
		
		for (Edge edge : this.edges) {
			if (edge.style == 0)
				sb.append(edge.toDotGraph() + "\n");
		}
		//sb.append("\n");
		
		tmp = "}";
		sb.append(tmp);
		return sb.toString();
	}
	
	private void initThreads() {
		for (String threadId : this.schedInfo.threadCapsules.keySet()) {
			List<String> capsules = this.schedInfo.threadCapsules.get(threadId);
			List<Queue> queues = getCapsuleQueues(capsules);
			Thread t = new Thread(threadId, capsules, queues, this);
			this.threads.put(threadId, t);
		}
	}
	
	private List<Queue> getCapsuleQueues(List<String> capsules) {
		List<Queue> queues = new ArrayList<Queue>();
		for (String capsuleId : capsules) {
			Queue queue = this.queues.get(capsuleId);
			if (queue != null)
				queues.add(queue);
		}
		return queues;
	}
	
	private boolean isAllEmpty() {
		for (Queue queue : queues.values()) {
			if (!queue.isEmpty())
				return false;
		}
		return true;
	}
	
	/*private Map<String, Node> getConnections(Node sysNode) {
		Map<String, Node> connections = new HashMap<String, Node>();
		for (Name key : sysNode.connections.keySet()) {
			connections.put(key.toString(), sysNode.connections.get(key));
		}
		return connections;
	}*/

	public void addNode(org.paninij.systemgraph.SystemGraph.Node node, Name name) {
		// if thread or task add a Queue 
		// otherwise add a SharedObject
		String instance = name.toString();
		String type = node.capsule.name.toString();
		Map<String, BlockGraph> graphs = this.capsuleGraphs.get(type);
		
		if (graphs == null) {
			graphs = fetch(type);
		}
		
		if (node.capsule.name.toString().contains("$thread") ||
				node.capsule.name.toString().contains("$task")) {
			Queue queue = new Queue(instance, type, graphs, schedInfo, this);
			if (instance.equals("this")) {
				// to begin building graphs add run() as default method to start
				queue.add("run()"); 
				// set root
				root = queue;
			}
			queues.put(instance, queue);
		} else {
			SharedObject object = new SharedObject(instance, type, graphs, this);
			objects.put(instance, object);
		}	
	}
	
	public void setConnections(Collection<Node> nodes) {
		for (Node node : nodes) {
			Map<String, org.paninij.taskgraph.Node> connections = new HashMap<String, org.paninij.taskgraph.Node>();
			String tNodeName = node.name.toString();
			org.paninij.taskgraph.Node tNode = null;
			
			if (queues.containsKey(tNodeName)) {
				tNode = queues.get(tNodeName);
			} else if (objects.containsKey(tNodeName)) {
				tNode = objects.get(tNodeName);
			}
			
			for (Entry<Name, Node> connection : node.connections.entrySet()) {
				String localName = connection.getKey().toString();
				String globalName = connection.getValue().name.toString();
				org.paninij.taskgraph.Node globalTaskNode = null;
				if (queues.containsKey(globalName)) {
					globalTaskNode = queues.get(globalName);
				} else if (objects.containsKey(globalName)) {
					globalTaskNode = objects.get(globalName);
				}
				connections.put(localName, globalTaskNode);
			}
			
			tNode.setConnections(connections);
		}
	}

	public void setEdge(Node fromNode, Node toNode, MethodSymbol fromProcedure,
		MethodSymbol toProcedure) {
		//toString(fromNode, toNode, fromProcedure, toProcedure);
		//MethodDeclVisitor mVisitor = new MethodDeclVisitor(this.schedInfo);
		String threadId, capsuleId, messageId;
		Task t = null;
		// create task for fromProcedure
		threadId = schedInfo.threadId(fromNode.name.toString());
		capsuleId = fromNode.getQualifiedName();
		messageId = fromProcedure.getQualifiedName().toString();
		//t = new Task(threadId, capsuleId, messageId, Type.PROC);
		/*MethodSymbol node = getOriginal(fromNode, fromProcedure.name.toString());
		if (node == null)
			node = fromProcedure;
		node.tree.accept(mVisitor);*/
		//fromProcedure.tree.accept(mVisitor);
		//t.subgraph = mVisitor.subgraph;
		tasks.add(t);
		// create task for toProcedure
		threadId = schedInfo.threadId(toNode.name.toString());
		capsuleId = toNode.getQualifiedName();
		messageId = toProcedure.getQualifiedName().toString();
		//t = new Task(threadId, capsuleId, messageId, Type.PROC);
		/*node = getOriginal(toNode, toProcedure.name.toString());
		if (node == null)
			node = toProcedure;
		node.tree.accept(mVisitor);*/
		//toProcedure.tree.accept(mVisitor);
		//t.subgraph = mVisitor.subgraph;
		tasks.add(t);
		// TODO: create edge between these tasks.
	}
	
	private Map<String, BlockGraph> fetch(String type) {
		Map<String, BlockGraph> graphs = null;
		if (type.contains("$task") ||
				type.contains("$monitor") ||
				type.contains("$sequential")) {
			String newtype = type.substring(0, type.indexOf('$')) + "$thread";
			graphs = this.capsuleGraphs.get(newtype);
		}
		
		return graphs;
	}
	
	private void addEdge(Node fromNode, Node toNode, MethodSymbol fromProcedure,
			MethodSymbol toProcedure) {
		// fromNode = thread, toNode = thread
		// fromNode = thread, toNode = task
		// fromNode = 
	}
	
	public void addEdge(Edge edge) {
		this.edges.add(edge);
		this.hashedEdges.put(edge.getToken(), edge);
	}
	
	/*class MethodDeclVisitor extends TreeScanner {
		public TaskGraph subgraph;
		
		public MethodDeclVisitor(SchedulingInfo schedInfo) {
			this.subgraph = new TaskGraph(schedInfo);
		}
		
		@Override
		public void visitTree(JCTree tree) {
			System.out.println(tree.toString());
			super.visitTree(tree);
		}
	}*/
	
	private MethodSymbol getOriginal(Node node, String name) {
		for (MethodSymbol proc : node.procedures) {
			if (proc.name.toString().equals(name+"$Original"))
				return proc;
		}
		return null;
	}
	
	public void toString(Node fromNode, Node toNode, MethodSymbol fromProcedure,
			MethodSymbol toProcedure) {
		StringBuilder sb = new StringBuilder();
	    sb.append(fromNode.name); sb.append(".");sb.append(fromProcedure);
	    sb.append(" --> ");
	    sb.append(toNode.name); sb.append("."); sb.append(toProcedure);
	    sb.append("\n");
	    System.out.println(sb.toString());
	}
	
	public Edge getEdge(org.paninij.taskgraph.Node from, org.paninij.taskgraph.Node to, String label) {
		/*for (String edgekey : hashedEdges.keySet()) {
			System.out.print(edgekey + ", ");
		}*/
		String edgeToken = from.getToken() + "->" + to.getToken() + ":" + label;
		if (hashedEdges.containsKey(edgeToken)) {
			return hashedEdges.get(edgeToken);
		}
		return null;
	}
}
