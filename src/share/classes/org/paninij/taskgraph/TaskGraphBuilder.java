package org.paninij.taskgraph;

import java.util.ArrayList;
import java.util.Map;

import org.paninij.analysis.BlockGraph;
import org.paninij.systemgraph.SystemGraph;
import org.paninij.systemgraph.SystemGraph.Edge;
import org.paninij.systemgraph.SystemGraph.Node;
import org.paninij.taskgraph.analysis.Batching;
import org.paninij.taskgraph.analysis.Communication;
import org.paninij.taskgraph.analysis.Contention;
import org.paninij.taskgraph.analysis.Parallelism;
import org.paninij.taskgraph.analysis.PrintStats;
import org.paninij.taskgraph.analysis.ThreadTrace;
import org.paninij.taskgraph.analysis.Traversal;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree.JCDesignBlock;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;

public class TaskGraphBuilder {
	public final SystemGraph systemGraph;
	public final SchedulingInfo schedInfo;
	public final Map<String, Map<String, BlockGraph>> capsuleGraphs;
	public final Name _this;
	
	public TaskGraphBuilder(JCDesignBlock design, SystemGraph systemGraph,
								Map<String, Map<String, BlockGraph>> capsuleGraphs, Name _this) {
		this._this = _this;
		this.systemGraph = systemGraph;
		this.schedInfo = new SchedulingInfo(design, systemGraph);
		this.capsuleGraphs = capsuleGraphs;
		this.schedInfo.print();
		
		build();
	}
	
	public TaskGraph build() {
		TaskGraph graph = new TaskGraph(this.schedInfo, this.capsuleGraphs);
		
		for (Node node : systemGraph.nodes.values()){
			graph.addNode(node, node.name);
		}
		
		graph.setConnections(systemGraph.nodes.values());
		
		graph.build(getCycleNames());
		
		System.out.println(" ======= Task Graph ======= ");
		System.out.println(graph.toDotGraph());
		
		// invoke and test analysis
		//Traversal traversal = new Traversal(graph);
		//traversal.BFS();
		
		PrintStats stats = new PrintStats(graph);
		stats.print();
		
		/*Contention contentions = new Contention(graph);
		contentions.print();*/
		
		/*Communication communications = new Communication(graph);
		communications.print();*/
		
		/*Parallelism parallelism = new Parallelism(graph);
		parallelism.print();*/
		
		//Batching batching = new Batching(graph);
		//batching.print();
		
		//ThreadTrace threadTrace = new ThreadTrace(graph);
		//threadTrace.trace();
		
		return graph;
	}
	
	private java.util.List<Name> getCycleNames() {
		List<Pair<Name, Name>> cycleNames = this.systemGraph.detectCyclicReferences(_this);
		java.util.List<Name> cycleNodes = new ArrayList<Name>();
		for (Pair<Name, Name> pair : cycleNames) {
			if (!cycleNodes.contains(pair.fst))
				cycleNodes.add(pair.fst);
			if (!cycleNodes.contains(pair.snd))
				cycleNodes.add(pair.snd);
		}
		return cycleNodes;
	}
	
	private boolean isPublic(MethodSymbol fromProcedure, MethodSymbol toProcedure) {
		if ((fromProcedure.flags_field & Flags.PUBLIC) != 0 &&
				(toProcedure.flags_field & Flags.PUBLIC) != 0)
				return true;
		
		return false;
	}
	
}
