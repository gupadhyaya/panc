package org.paninij.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

public class Loop {
	public Block header;
	public Block backJump;
	public List<Block> loopBlocks = new ArrayList<Block>();
	public List<Block> loopExists = new ArrayList<Block>();
	public final BlockGraph g;

	public Loop(BlockGraph graph) {
		this.g = graph;
	}

	public void addBlock(Block block) {
		if (!this.loopBlocks.contains(block))
			this.loopBlocks.add(block);
	}

	public void addExit(Block block) {
		if (!this.loopExists.contains(block))
			this.loopExists.add(block);
	}

	// identify loop that provides index to capsule arrays
	public boolean isCapsuleLoop() {
		boolean capsuleLoop = false;
		String lindex = getLoopIndex();
		String cindex = visit(header);
		if (lindex.equals(cindex))
			capsuleLoop = true;
		return capsuleLoop;
	}

	private String visit(Block b) {
		boolean isCapsuleLoop = false;
		List<Block> visited = new ArrayList<Block>();
		java.util.Queue<Block> toVisit = new LinkedList<Block>();
		toVisit.add(b);
		LoopStmtVisitor lv = new LoopStmtVisitor();
		do {
			Block block = toVisit.remove();
			visited.add(block);
			for (JCTree tree : block.getBody()) {
				tree.accept(lv);
			}
			List<Block> succs = block.getSuccs();
			for (Iterator<Block> succsIter = succs.iterator(); succsIter
					.hasNext();) {
				Block succ = succsIter.next();
				if (!visited.contains(succ)) {
					toVisit.add(succ);
				}
			}
		} while (!toVisit.isEmpty());

		return lv.cindex;
	}

	private String getLoopIndex() {
		String li = "";
		LoopStmtVisitor lv = new LoopStmtVisitor();
		for (JCTree tree : this.header.getBody()) {
			tree.accept(lv);
		}
		li = lv.index;
		return li;
	}

	class LoopStmtVisitor extends TreeScanner {
		public String index = "";
		public String cindex = "";

		@Override
		public void visitApply(JCMethodInvocation tree) {
			// TODO Auto-generated method stub
			super.visitApply(tree);
			if (tree.meth instanceof JCFieldAccess) {
				JCFieldAccess meth = (JCFieldAccess) tree.meth;
				// if (meth.name.toString().equalsIgnoreCase("start")) {
				String cname = "";
				if (meth.selected instanceof JCArrayAccess) {
					JCArrayAccess ms = (JCArrayAccess) meth.selected;

					if (ms.index instanceof JCIdent) {
						cname = ms.index.toString();
					}
				} /*
				 * else { cname = ((JCIdent) meth.selected).name.toString(); }
				 */
				if (cname != "")
					this.cindex = cname;
			}

		}

		@Override
		public void visitBinary(JCBinary tree) {
			// TODO Auto-generated method stub
			if (tree.lhs instanceof JCIdent) {
				JCIdent i = (JCIdent) tree.lhs;
				this.index = i.name.toString();
			}
			super.visitBinary(tree);
		}
	}
}
