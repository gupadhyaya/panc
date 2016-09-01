package org.paninij.taskgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.paninij.systemgraph.SystemGraph;
import org.paninij.systemgraph.SystemGraph.Node;

import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCDesignBlock;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

public class SchedulingInfo {
	private final String POOL = "pool:";
	private final String THREAD = "thread:";
	private static int ids = -1;
	private int psize;
	private int pindex = 0;
	// a map of capsules that will be run by the thread
	public Map<String, List<String>> threadCapsules = new HashMap<String, List<String>>();

	HashMap<String, String> capsuleTypes = new HashMap<String, String>();
	HashMap<String, String> threadIds = new HashMap<String, String>();

	public SchedulingInfo(JCDesignBlock design, SystemGraph graph) {
		String this_id = THREAD + String.valueOf(++ids);
		threadIds.put("this", this_id);
		insertToMap(this_id, "this");
		extractCapsuleInfo(graph);
		extractSchedulingInfo(design);
	}

	public String threadId(String capsule) {
		return threadIds.get(capsule);
	}
	
	public void print() {
		System.out.println("============ Scheduling info ============ ");
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> threadIdEntry : threadIds.entrySet()) {
			String capsuleId = threadIdEntry.getKey();
			String id = threadIdEntry.getValue();
			String str = capsuleId + " : " + id + "\n";
			sb.append(str);
		}
		System.out.println(sb.toString());
	}

	private void addCapsule(String instance, String type) {
		capsuleTypes.put(instance, type);
	}

	private void initTaskPool(int size) {
		psize = size;
	}

	private void add(String capsule) {
		if (capsuleTypes.containsKey(capsule)) {
			String ctype = capsuleTypes.get(capsule);
			if (ctype.contains("$thread")) {
				String id = THREAD + String.valueOf(++ids); //capsule + ":" + ctype;
				threadIds.put(capsule, id);
				insertToMap(id, capsule);
			} else if (ctype.contains("$task")) {
				String id = POOL + (pindex++ % psize);
				threadIds.put(capsule, id);
				insertToMap(id, capsule);
			}
		} else {
			for (String key : capsuleTypes.keySet()) {
				if (key.contains(capsule + "["))
					add(key);
			}
		}
	}
	
	private void insertToMap(String threadId, String capsule) {
		if (threadCapsules.containsKey(threadId)) {
			threadCapsules.get(threadId).add(capsule);
		} else {
			List<String> capsules = new ArrayList<String>();
			capsules.add(capsule);
			threadCapsules.put(threadId, capsules);
		}
	}

	private final void extractSchedulingInfo(JCDesignBlock tree) {
		class ScheduleDeclCollector extends TreeScanner {
			@Override
			public void visitApply(JCMethodInvocation tree) {
				if (tree.meth instanceof JCFieldAccess) {
					JCFieldAccess meth = (JCFieldAccess) tree.meth;
					if (meth.name.toString().equalsIgnoreCase("start")) {
						if (meth.selected instanceof JCArrayAccess) {
							JCArrayAccess ms = (JCArrayAccess) meth.selected;
							String cname = "";
							if (ms.index instanceof JCIdent) {
								cname = ms.indexed.toString();
							} else {
								cname = ms.indexed + "[" + ms.index + "]";
							}
							add(cname);
						} else {
							add(((JCIdent) meth.selected).name.toString());
						}
					} else if (meth.name.toString().equalsIgnoreCase(
							"panini$init")) {
						if (tree.args.head instanceof JCLiteral) {
							JCLiteral l = (JCLiteral) tree.args.head;
							initTaskPool((Integer) l.value);
						}
					}
				}

			}
		}
		ScheduleDeclCollector sdc = new ScheduleDeclCollector();
		tree.accept(sdc);
	}

	private void extractCapsuleInfo(SystemGraph graph) {
		for (Iterator<Node> nodesIter = graph.nodes.values().iterator(); nodesIter
				.hasNext();) {
			Node n = nodesIter.next();
			addCapsule(n.name.toString(), n.capsule.name.toString());
		}
	}
}
