/*
 * This file is part of the Panini project at Iowa State University.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 * 
 * For more details and the latest version of this code please see
 * http://paninij.org
 * 
 * Contributor(s): Eric Lin, Yuheng Long
 */
package org.paninij.systemgraph;

import java.util.HashMap;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.util.*;

import org.paninij.effects.analysis.*;
import org.paninij.systemgraph.SystemGraph.Node;

public class SystemGraphBuilder {
	Symtab syms;
	Names names;
	Log log;
	
	public SystemGraphBuilder(Symtab syms, Names names, Log log){
		this.syms = syms;
		this.names = names;
		this.log = log;
	}
	
	/** Returns an empty systemGraph
	 */
	public SystemGraph createSystemGraph(){
		return new SystemGraph();
	}
	
	/** Adds a single Node with the name and capsuleSymbol to graph
	 */
	public void addSingleNode(SystemGraph graph, Name name, final CapsuleSymbol c){
		graph.addNode(name, c);
	}

	/** Adds an array of capsules as multiple nodes.
	 * Name of these nodes are represented as "capsuleName[index]" in the graph
	 */
	public void addMultipleNodes(SystemGraph graph, Name name, int amount, final CapsuleSymbol c) {
		for(int i=0;i<amount;i++){
			graph.addNode(names.fromString(name+"["+i+"]"), c);
		}
		graph.capsuleArrays.put(name, amount);
	}
	
	public void addConnection(SystemGraph graph, Name fromNode, Name arg, Name toNode){
		graph.setConnection(fromNode, arg, toNode);
	}
	
	/** Connects a single capsule to a capsule Array
	 */
	public void addConnectionsOneToMany(SystemGraph graph, Name fromNode, Name arg, Name toNode){
		int amount = graph.capsuleArrays.get(toNode);
		for(int i=0;i<amount;i++){
			addConnection(graph, fromNode, names.fromString(arg+"["+i+"]"), names.fromString(toNode+"["+i+"]"));
		}
	}
	
	/** Connects from every capsule of a capsule array to a single capsule. 
	 *  Used for foreach loops in systems
	 */
	public void addConnectionsManyToOne(SystemGraph graph, Name fromNode, Name arg, Name toNode){
		int amount = graph.capsuleArrays.get(fromNode);
		for(int i=0;i<amount;i++){
			addConnection(graph, names.fromString(fromNode+"["+i+"]"), arg, toNode);
		}
	}
	
	/** Connects from every capsule of a capsule array to a capsule array. 
	 *  Used for foreach loops in systems
	 */
	public void addConnectionsManyToMany(SystemGraph graph, Name fromNode, Name arg, Name toNode){
		int fromAmount = graph.capsuleArrays.get(fromNode);
		int toAmount = graph.capsuleArrays.get(fromNode);
		for(int i=0;i<fromAmount;i++){
			for(int j=0;j<toAmount;j++)
				addConnection(graph, names.fromString(fromNode+"["+i+"]"), names.fromString(arg+"["+i+"]"), names.fromString(toNode+"["+j+"]"));
		}
	}

	private void translateCallEffects(SystemGraph.Node node,
			MethodSymbol fromProc, SystemGraph graph, EffectSet ars){
		for (CallEffect call : ars.calls) {
			if (call instanceof CapsuleEffect) {
				CapsuleEffect ce = (CapsuleEffect) call;
				Node n = node.connections.get(ce.callee.name);

				graph.setEdge(node, fromProc, n, ((CapsuleEffect) call).meth);
			} else if (call instanceof ForeachEffect) {
				ForeachEffect fe = (ForeachEffect)call;
				String calleeName = fe.callee.toString();
				int size = calleeName.length();
				HashMap<Name, Node> connections = node.connections;
				for (Name key : connections.keySet()) {
					String keyS = key.toString();
					if (keyS.startsWith(calleeName.toString()) &&
							keyS.charAt(size) == '[') {
						graph.setEdge(node, fromProc, connections.get(key),
								fe.meth);
					}
				}
			}
		}
	}
	
	public void completeEdges(SystemGraph graph){
		for(SystemGraph.Node n : graph.nodes.values()){
			for(MethodSymbol ms : n.procedures){
				if(ms.ars!=null){
//					System.out.println(ms);
//					ms.ars.printEffect();
					translateCallEffects(n, ms, graph, ms.ars);
				}
			}
		}
	}

	public void addEdge(SystemGraph graph, Name fromNode, MethodSymbol fromProc, Name toNode, MethodSymbol toProc){
		graph.setEdge(graph.nodes.get(fromNode), fromProc, graph.nodes.get(toNode), toProc);
	}
}