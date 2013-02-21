package org.paninij.analysis.staticprofile;

import static com.sun.tools.javac.code.Flags.PRIVATE;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import org.paninij.analysis.CFG;
import org.paninij.analysis.CFGBuilder;
import org.paninij.analysis.CFGPrinter;
import org.paninij.effects.AliasingComp;
import org.paninij.effects.EffectSet;
import org.paninij.effects.SystemEffectsComp;

import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.util.Context;

public class StaticProfilePass {

    protected static final Context.Key<StaticProfilePass> secKey =
            new Context.Key<StaticProfilePass>();
    
	static double epsilon = 0.000001;
	
	private JCCapsuleDecl capsule;
	//callgraph methods in dfs order TODO:
	Vector<JCMethodDecl> methods = new Vector<JCMethodDecl>();
	//get the entrypoint of callgraph somehow
	private JCMethodDecl root;
	
	//more class fields
	Set<JCMethodDecl> functionLoopHeads = new HashSet<JCMethodDecl>();
	
	public StaticProfilePass() {
		// TODO Auto-generated constructor stub
	}
	
	public StaticProfilePass(JCCapsuleDecl capsule) {
		this.capsule = capsule;
		// runOnCapsule();
		processCapsule(this.capsule);
	}
	
    public static StaticProfilePass instance(Context context) {
    	StaticProfilePass instance = context.get(secKey);
        if (instance == null)
            instance = new StaticProfilePass(context);
        return instance;
    }
    
    protected StaticProfilePass(Context context) {
        context.put(secKey, this);
    }
	
	/**
	 * per class staticprofile information
	 */
	private void processCapsule(JCCapsuleDecl capsule) {
		this.capsule = capsule;
		LinkedList<JCMethodDecl> methodsToProcess;
		methodsToProcess = new LinkedList<JCMethodDecl>();
		HashSet<JCMethodDecl> visitedMethods = new HashSet<JCMethodDecl>();
		for (JCTree def : capsule.defs) {
			if (def.getTag() == Tag.METHODDEF) {
				JCMethodDecl method = (JCMethodDecl) def;
				if ((method.sym.flags() & PRIVATE) != 0
						|| (method.sym.name.toString().equals("run") && capsule.sym.hasRun)) {
					methodsToProcess.offer(method);
				}
			}
		}

		while (!methodsToProcess.isEmpty()) {
			JCMethodDecl method = methodsToProcess.poll();
			visitedMethods.add(method);
			new BlockEdgeFrequencyPass(this.capsule, method);
		}
	}
	
	private final boolean runOnCapsule() {
		preprocess();
		ListIterator<JCMethodDecl> methodsIter = methods.listIterator();
		// creating reverse-dfs order of callgraph nodes
		while (methodsIter.hasNext())	methodsIter.next();
		// Search for function loop heads in reverse depth-first order
		while (methodsIter.hasPrevious()) {
			JCMethodDecl method = methodsIter.previous();
			// If function is a loop head, propagate frequencies from it.
			if (functionLoopHeads.contains(method)) {
				// Mark all reachable nodes as not visited.
				markReachable(method);
				// Propagate call frequency starting from this loop head.
				propagateCallFrequency(method, false);
			}
		}
		// Release some unused memory.
		methods.clear();
		// Mark all functions reachable from the main function as not visited.
		markReachable(root);

		// Propagate frequency starting from the main function.
		propagateCallFrequency(root, true);

		// With function frequency calculated, propagate it to block and edge
		// frequencies to achieve global block and edge frequency.
		//calculateGlobalInfo(m);
		return false;
	}
	
	/**
	 * Preprocess - From a call graph: (1) obtain functions in depth-first
	 * order; (2) find back edges; (3) find loop heads; (4) local block and edge
	 * profile information (per function); (5) local function edge frequency;
	 * (6) map of function predecessors.
	 */
	private final void preprocess() {
		
	}
	
	/**
	 * MarkReachable - Mark all blocks reachable from root function as not
	 * reachable
	 */
	private final void markReachable(JCMethodDecl root) {
		
	}
	
	/**
	 * PropagateCallFrequency - Calculate function call and invocation
	 * frequencies.
	 * 
	 * @param root
	 * @param end
	 */

	private final void propagateCallFrequency(JCMethodDecl root, boolean end) {
		
	}
}
