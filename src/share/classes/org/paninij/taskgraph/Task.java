package org.paninij.taskgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.paninij.analysis.Block;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;

public class Task extends Node {
	private static int ids = -1;
	public final int id;
	public final String threadId;
	public final String capsuleId;
	public final String messageId;
	public final int type;
	public List<Node> successors;
	public List<Node> predecessors;
	public Map<Integer, List<String>> successorLabels;
	public List<String> privateMethods;

	public final Block block;
	public TaskGraph subgraph;

	interface Type {
		static int BLK = 1 << 0;
		static int STATE_R = 1 << 1;
		static int STATE_W = 1 << 2;
		static int BRANCH = 1 << 3;
		static int LOOP = 1 << 4;
		static int SEND = 1 << 5;
		static int PROC = 1 << 6;
	}

	public Task(String threadId, String capsuleId, String messageId, int type,
			Block block) {
		super();
		this.id = ++ids;
		this.threadId = threadId;
		this.capsuleId = capsuleId;
		this.messageId = messageId;
		// this.type = type;
		this.block = block;
		this.privateMethods = new ArrayList<String>();
		this.type = visit();
		this.successors = new ArrayList<Node>();
		this.predecessors = new ArrayList<Node>();
		this.successorLabels = new HashMap<Integer, List<String>>();
	}

	private int visit() {
		BlockVisitor blkVisitor = new BlockVisitor();
		for (JCTree tree : this.block.getBody()) {
			tree.accept(blkVisitor);
		}
		if (blkVisitor.methodCalls.size() > 0)
			this.privateMethods = blkVisitor.methodCalls;
		if (this.block.isLoop() && !this.block.loop().isCapsuleLoop())
			blkVisitor.type |= Type.LOOP;
		if (isBranch())
			blkVisitor.type |= Type.BRANCH;
		return blkVisitor.type;
	}

	@Override
	public void setConnections(Map<String, Node> connections) {
		// TODO Auto-generated method stub

	}

	public int getToken() {
		return token;
	}

	public String toDotGraph() {
		String tmp = "\"" + token + "\"" + "[label=" + "\"" + toString() + "\""
				+ ",style=bold];";
		return tmp;
	}

	@Override
	public String toString() {
		/*
		 * String str = threadId + delim + capsuleId + delim + trim(messageId) +
		 * delim + "type:" + type + ", id:" + id;
		 */
		String str = "";
		/*String str = "id:" + this.token + delim + capsuleId + delim + trim(messageId)
				+ delim + threadId + delim + "type:" + type;*/
		//str = String.valueOf(this.token);
		str = typeString();
		return str;
	}
	
	private String typeString() {
		String str = "";
		if ((type & (1 << 1)) != 0)
			str += "R,";
		if ((type & (1 << 2)) != 0)
			str += "W,";
		if ((type & (1 << 3)) != 0)
			str += "BR,";
		if ((type & (1 << 4)) != 0)
			str += "L,";
		if ((type & (1 << 5)) != 0)
			str += "S,";
		if ((type & (1 << 6)) != 0)
			str += "P,";
		if ((type & (1 << 7)) != 0)
			str += "C,";
		if (str.equals(""))
			str += "B";
		return str;
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

	private boolean isBranch() {
		if (this.block.getSuccs().size() > 1)
			return true;
		return false;
	}

	class BlockVisitor extends TreeScanner {
		int type = Type.BLK;
		List<String> methodCalls = new ArrayList<String>();

		public int getType() {
			return type;
		}

		@Override
		public void visitApply(JCMethodInvocation tree) {
			Symbol sym = null;
			// capture the symbol
			if (tree.meth instanceof JCFieldAccess) {
				JCFieldAccess meth = (JCFieldAccess) tree.meth;
				sym = meth.sym;
			} else if (tree.meth instanceof JCIdent) {
				JCIdent m = (JCIdent) tree.meth;
				sym = m.sym;
			}
			if (sym != null) {
				if (sym.owner.isCapsule()) {
					MethodSymbol msym = (MethodSymbol) sym;
					// if ((msym.tree.mods.flags & Flags.PUBLIC) != 0)

					// add this methodCall to the list
					if (msym.tree != null
							&& (msym.tree.mods.flags & Flags.PRIVATE) != 0) {
						type |= Type.PROC;
						String methodSignature = getMethodSignature(
								msym.tree.name.toString(), msym.tree.params);
						methodCalls.add(methodSignature);
					} else {
						type |= Type.SEND;
					}
				} else {

				}
			}
			super.visitApply(tree);
		}

		private String getMethodSignature(String method,
				List<JCVariableDecl> params) {
			StringBuilder sb = new StringBuilder();
			sb.append(method);
			sb.append("(");
			for (JCVariableDecl jcVariableDecl : params) {
				sb.append(jcVariableDecl.vartype);
				sb.append(",");
			}
			if (sb.toString().contains(","))
				sb.deleteCharAt(sb.length() - 1); // remove the last comma
			sb.append(")");
			return sb.toString();
		}

		@Override
		public void visitSelect(JCFieldAccess tree) {
			if (tree.selected instanceof JCIdent) {
				JCIdent selected = (JCIdent) tree.selected;
				if (selected.sym != null) {
					if (selected.sym.owner.isCapsule()
							&& selected.name.toString().equals("this")) {
						type |= Type.STATE_R;
						state_r = true;
					}
				}
			}
			super.visitSelect(tree);
		}

		boolean state_r = false;

		@Override
		public void visitAssign(JCAssign tree) {
			// check if the lhs expression does the state read
			// super.visitAssign(tree);
			scan(tree.lhs);
			if (state_r)
				type |= Type.STATE_W;
			state_r = false;
			scan(tree.rhs);
		}

		@Override
		public void visitDoLoop(JCDoWhileLoop tree) {
			type |= Type.LOOP;
			super.visitDoLoop(tree);
		}

		@Override
		public void visitWhileLoop(JCWhileLoop tree) {
			type |= Type.LOOP;
			super.visitWhileLoop(tree);
		}

		@Override
		public void visitForLoop(JCForLoop tree) {
			type |= Type.LOOP;
			super.visitForLoop(tree);
		}

		@Override
		public void visitForeachLoop(JCEnhancedForLoop tree) {
			type |= Type.LOOP;
			super.visitForeachLoop(tree);
		}

		@Override
		public void visitIf(JCIf tree) {
			type |= Type.BRANCH;
			super.visitIf(tree);
		}

		@Override
		public void visitSwitch(JCSwitch tree) {
			type |= Type.BRANCH;
			super.visitSwitch(tree);
		}

		@Override
		public void visitConditional(JCConditional tree) {
			type |= Type.BRANCH;
			super.visitConditional(tree);
		}
	}
}
