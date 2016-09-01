package org.paninij.taskgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.paninij.analysis.Block;
import org.paninij.analysis.BlockGraph;
import org.paninij.taskgraph.Task.Type;

import com.sun.tools.javac.tree.JCTree;

public class SharedObject extends Node {
	public final String instance;
	public final String type;
	public final Map<String, BlockGraph> blockGraphs;
	public Map<String, Node> connections;
	public final String capsuleId;
	public final TaskGraph taskgraph;
	public List<Node> successors;
	public List<Node> predecessors;
	public Map<Integer, List<String>> successorLabels;
	
	public SharedObject(String instance, String type, Map<String, BlockGraph> graphs,
			Map<String, Node> connections, TaskGraph taskgraph) {
		super();
		this.instance = instance;
		this.type = type;
		this.capsuleId = this.instance + "!!" + this.type;
		this.blockGraphs = graphs;
		this.connections = connections;
		this.taskgraph = taskgraph;
		this.successors = new ArrayList<Node>();
		this.predecessors = new ArrayList<Node>();
		this.successorLabels = new HashMap<Integer, List<String>>();
	}
	
	public SharedObject(String instance, String type, Map<String, BlockGraph> graphs,
			TaskGraph taskgraph) {
		this.instance = instance;
		this.type = type;
		this.capsuleId = this.instance + "!!" + this.type;
		this.blockGraphs = graphs;
		this.connections = null;
		this.taskgraph = taskgraph;
		this.successors = new ArrayList<Node>();
		this.predecessors = new ArrayList<Node>();
		this.successorLabels = new HashMap<Integer, List<String>>();
	}

	@Override
	public void setConnections(Map<String, Node> connections) {
		this.connections = connections;
	}
	
	public int getToken() {
		return token;
	}
	
	@Override
	public String toString() {
		String str;
		str = instance;// + "!!" + type;
		//str = String.valueOf(this.token);
		return str;
	
	}
	
	public String toDotGraph() {
		String tmp = "\"" + token + "\"" + "[label=" + "\"" + toString() + "\"" +  
						",shape=note];"; 
		return tmp;
	}
	
	@Override
	public List<Node> getSuccessors() {
		return successors;
	}

	@Override
	public List<Node> getPredecessors() {
		return predecessors;
	}
	
	@Override
	public List<String> getSuccessorsLabels(int succ) {
		return successorLabels.get(succ);
	}

	@Override
	public void addSuccessor(Node node, String label) {
		if (successorLabels.containsKey(node.getToken())) {
			successorLabels.get(node.getToken()).add(label);
		} else {
			List<String> labels = new ArrayList<String>();
			labels.add(label);
			successorLabels.put(node.getToken(), labels);
		}
		
		successors.add(node);
	}

	@Override
	public void addPredecessor(Node node) {
		predecessors.add(node);
	}
	
	private String threadId = "";
	public List<Task> work(String item, String threadId) {
		this.threadId= threadId; 
		List<Task> tasks = new ArrayList<Task>();
		if (this.blockGraphs.containsKey(item)) {
			// get the blockgraph of the method and start creating new tasks
			BlockGraph graph = this.blockGraphs.get(item);

			List<Block> visited = new ArrayList<Block>();
			Map<Block, Task> createdTasks = new HashMap<Block, Task>();
			java.util.Queue<Block> toVisit = new LinkedList<Block>();

			// add the head blocks and start
			for (Block head : graph.getHeads()) {
				toVisit.add(head);
			}

			do {
				// peek the top and create new task
				Block block = toVisit.remove();
				Task blockTask = null;
				if (createdTasks.containsKey(block)) {
					blockTask = createdTasks.get(block);
				} else {
					blockTask = new Task(threadId, capsuleId, item,
						Type.BLK, block);
					taskgraph.tasks.add(blockTask);
					tasks.add(blockTask);
					createdTasks.put(block, blockTask);
				}
				// add the block to visited
				visited.add(block);

				List<Block> succs = block.getSuccs();
				for (Iterator<Block> succsIter = succs.iterator(); succsIter
						.hasNext();) {
					Block succ = succsIter.next();
					Task succTask = null;
					if (!visited.contains(succ)) {
						// create new task
						succTask = new Task(threadId, capsuleId, item,
								Type.BLK, succ);
						taskgraph.tasks.add(succTask);
						tasks.add(succTask);
						createdTasks.put(succ, succTask);
						// push the successor for visiting
						toVisit.add(succ);
					} else {
						succTask = createdTasks.get(succ);
					}
					// add connecting edges (block, succ)
					taskgraph.addEdge(new Edge(blockTask, succTask));
				}
				// visit the units in this block
				BlockVisitor bv = new BlockVisitor();
				for (Iterator<JCTree> unitsIter = block.getBody()
						.iterator(); unitsIter.hasNext();) {
					JCTree unit = unitsIter.next();
					unit.accept(bv);
				}
				// get list of inter-capsular calls
				List<Invoked> calls = bv.calls;
				handleInvokedCalls(blockTask, calls);
			} while (!toVisit.isEmpty());
		}
		
		return tasks;
	}
	
	private void handleInvokedCalls(Task block, List<Invoked> calls) {
		for (Invoked invoked : calls) {
			String cname = invoked.capsule;
			String mname = invoked.method;
			Node capsule = null;
			if (connections.containsKey(cname)) {
				capsule = connections.get(cname);
				if (capsule instanceof Queue) {
					Queue capsuleQ = (Queue) capsule;
					taskgraph.addEdge(new Edge(block, capsuleQ));
					taskgraph.addEdge(new Edge(capsuleQ, block, 1));
					capsuleQ.add(mname);
				} else {
					// SharedObject
					SharedObject capsuleObj = (SharedObject) capsule;
					taskgraph.addEdge(new Edge(block, capsuleObj));
					//taskgraph.addEdge(new Edge(capsuleObj, block, 1));
					processNow(capsuleObj, mname, threadId);
				}
			} else {
				// handling things like
				// senders[index].send() calls
				for (String ckey : connections.keySet()) {
					if (ckey.contains(cname + "[")) {
						capsule = connections.get(ckey);
						if (capsule instanceof Queue) {
							Queue capsuleQ = (Queue) capsule;
							taskgraph.addEdge(new Edge(block, capsuleQ));
							taskgraph.addEdge(new Edge(capsuleQ, block, 1));
							capsuleQ.add(mname);
						} else {
							// SharedObject
							SharedObject capsuleObj = (SharedObject) capsule;
							taskgraph.addEdge(new Edge(block, capsuleObj));
							//taskgraph.addEdge(new Edge(capsuleObj, block, 1));
							processNow(capsuleObj, mname, threadId);
						}
					}
				}
			}
		}
	}
	
	private void processNow(SharedObject obj, String item, String threadId) {
		List<Task> tasks = obj.work(item, threadId);
		for (Task task : tasks) {
			if (!hasTaskEdges(task.predecessors, tasks)) {
				Edge queueToFirst = new Edge(obj, task, task.messageId); 
				this.taskgraph.addEdge(queueToFirst);
			} 
			if (!hasTaskEdges(task.successors, tasks)) {
				Edge lastToQueue = new Edge(task, obj, 1);
				this.taskgraph.addEdge(lastToQueue);
			}
		}
		/*if (tasks != null && tasks.size() > 0) {
			Task first = tasks.get(0);
			Task last = tasks.get(tasks.size() - 1);
			Edge queueToFirst = new Edge(obj, first); 
			Edge lastToQueue = new Edge(last, obj, 1);
			this.taskgraph.addEdge(queueToFirst);
			this.taskgraph.addEdge(lastToQueue);
		}*/
	}
	
	private boolean hasTaskEdges(List<Node> nodes, List<Task> tasks) {
		for (Node node : nodes) 
			if (node instanceof Task)
				if (tasks.contains((Task)node))
						return true;
		return false;
	}
}