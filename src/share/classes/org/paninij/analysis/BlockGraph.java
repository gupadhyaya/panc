package org.paninij.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

public class BlockGraph {
	private JCMethodDecl tree;
	private List<JCTree> mBody;
	private List<JCTree> mUnits;
	private final ASTCFGBuilder cfg;
	private List<Block> mBlocks;
	private List<Block> mHeads = new ArrayList<Block>();
	private List<Block> mTails = new ArrayList<Block>();
	private List<Loop> loops = new ArrayList<Loop>();

	public BlockGraph(JCMethodDecl tree, ASTCFGBuilder cfg) {
		this.tree = tree;
		this.cfg = cfg;
		// if (cfg.order == null || cfg.order.size() == 0) {
		mBody = clean(cfg.order);
		// clean();
		mUnits = mBody;
		Set<JCTree> leaders = computeLeaders();
		buildBlocks(leaders, tree);
		loopify();
		// }
	}

	public List<Block> getBlocks() {
		return mBlocks;
	}

	public List<Block> getHeads() {
		return mHeads;
	}

	public List<Block> getTails() {
		return mTails;
	}

	public List<Block> getPredsOf(Block b) {
		return b.getPreds();
	}

	public List<Block> getSuccsOf(Block b) {
		return b.getSuccs();
	}

	public int size() {
		return mBlocks.size();
	}

	public Iterator<Block> iterator() {
		return mBlocks.iterator();
	}

	private ArrayList<JCTree> clean(ArrayList<JCTree> cfgNodes) {
		ArrayList<JCTree> newNodes = new ArrayList<JCTree>();
		for (JCTree node : cfgNodes) {
			if (node.predecessors.size() == 0 && node.successors.size() == 0)
				continue;
			newNodes.add(node);
		}
		return newNodes;
	}

	private Set<JCTree> computeLeaders() {
		Set<JCTree> leaders = new HashSet<JCTree>();

		for (Iterator<JCTree> unitIt = this.mUnits.iterator(); unitIt.hasNext();) {
			JCTree u = unitIt.next();

			List<JCTree> predecessors = u.predecessors;
			int predCount = (predecessors != null) ? predecessors.size() : 0;
			List<JCTree> successors = u.successors;
			int succCount = (successors != null) ? successors.size() : 0;

			if (predCount == 0 && succCount == 0)
				continue;
			if (predCount != 1) { // If predCount == 1 but the predecessor
				leaders.add(u); // is a branch, u will get added by that
			} // branch's successor test.
			if (succCount > 1) { // || (u.branches())
				for (Iterator<JCTree> it = successors.iterator(); it.hasNext();) {
					leaders.add(it.next()); // The cast is an
				} // assertion check.
			}
			// add tree with inter-capsular calls to be leaders
			/*
			 * if (u instanceof JCMethodInvocation) { JCMethodInvocation mi =
			 * (JCMethodInvocation) u; if (visitApply(mi) &&
			 * !leaders.contains(u)) leaders.add(u); }
			 */
		}

		return leaders;
	}

	private boolean visitApply(JCMethodInvocation tree) {
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
				return true;
			}
		}
		return false;
	}

	private Map<JCTree, Block> buildBlocks(Set<JCTree> leaders,
			JCMethodDecl tree) {
		List<Block> blockList = new ArrayList<Block>(leaders.size());
		Map<JCTree, Block> unitToBlock = new HashMap<JCTree, Block>();

		JCTree blockHead = null;
		int blockLength = 0;
		Iterator<JCTree> unitIt = mUnits.iterator();
		if (unitIt.hasNext()) {
			blockHead = unitIt.next();
			if (!leaders.contains(blockHead)) {
				throw new RuntimeException(
						"BlockGraph: first unit not a leader!");
			}
			blockLength++;
		}

		JCTree blockTail = blockHead;
		int indexInMethod = 0;

		while (unitIt.hasNext()) {
			JCTree u = unitIt.next();
			if ((blockTail instanceof JCMethodInvocation)
					&& visitApply((JCMethodInvocation) blockTail)) {
				addBlock(blockHead, blockTail, indexInMethod, blockLength,
						blockList, unitToBlock);
				indexInMethod = indexInMethod + blockLength;// indexInMethod++;
				blockHead = u;
				blockLength = 0;
			}
			if (leaders.contains(u)) {
				addBlock(blockHead, blockTail, indexInMethod, blockLength,
						blockList, unitToBlock);
				indexInMethod = indexInMethod + blockLength;// indexInMethod++;
				blockHead = u;
				blockLength = 0;
			}
			blockTail = u;
			blockLength++;
		}
		if (blockLength > 0) {
			// Add final block.
			addBlock(blockHead, blockTail, indexInMethod, blockLength,
					blockList, unitToBlock);
		}

		// The underlying UnitGraph defines heads and tails.
		if (cfg.currentStartNodes != null)
			for (Iterator<JCTree> it = cfg.currentStartNodes.iterator(); it
					.hasNext();) {
				JCTree headUnit = it.next();
				Block headBlock = unitToBlock.get(headUnit);
				if (headBlock.getHead() == headUnit) {
					mHeads.add(headBlock);
				} else {
					throw new RuntimeException(
							"BlockGraph(): head Unit is not the first unit in the corresponding Block!");
				}
			}
		if (cfg.currentEndNodes != null)
			for (Iterator<JCTree> it = cfg.currentEndNodes.iterator(); it
					.hasNext();) {
				JCTree tailUnit = it.next();
				Block tailBlock = unitToBlock.get(tailUnit);
				if (tailBlock == null)
					System.out.println();
				if (tailBlock.getTail() == tailUnit) {
					mTails.add(tailBlock);
				} else {
					throw new RuntimeException(
							"BlockGraph(): tail Unit is not the last unit in the corresponding Block!");
				}
			}
		if (cfg.currentExitNodes != null)
			for (Iterator<JCTree> it = cfg.currentExitNodes.iterator(); it
					.hasNext();) {
				JCTree tailUnit = it.next();
				Block tailBlock = unitToBlock.get(tailUnit);
				if (tailBlock.getTail() == tailUnit) {
					mTails.add(tailBlock);
				} else {
					throw new RuntimeException(
							"BlockGraph(): tail Unit is not the last unit in the corresponding Block!");
				}
			}

		for (Iterator<Block> blockIt = blockList.iterator(); blockIt.hasNext();) {
			Block block = blockIt.next();

			List<JCTree> predUnits = block.getHead().predecessors;
			List<Block> predBlocks = new ArrayList<Block>(predUnits.size());
			for (Iterator<JCTree> predIt = predUnits.iterator(); predIt
					.hasNext();) {
				JCTree predUnit = predIt.next();
				Block predBlock = unitToBlock.get(predUnit);
				if (predBlock == null) {
					throw new RuntimeException(
							"BlockGraph(): block head mapped to null block!");
				}
				predBlocks.add(predBlock);
			}

			if (predBlocks.size() == 0) {
				block.setPreds(Collections.<Block> emptyList());

				// If the UnreachableCodeEliminator is not eliminating
				// unreachable handlers, then they will have no
				// predecessors, yet not be heads.
				/*
				 * if (! mHeads.contains(block)) { throw new
				 * RuntimeException("Block with no predecessors is not a head!"
				 * ); // Note that a block can be a head even if it has //
				 * predecessors: a handler that might catch an exception //
				 * thrown by the first Unit in the method. }
				 */
			} else {
				block.setPreds(Collections.unmodifiableList(predBlocks));
				if (block.getHead() == mUnits.get(0)) {
					mHeads.add(block); // Make the first block a head
					// even if the Body is one huge loop.
				}
			}

			List<JCTree> succUnits = block.getTail().successors;
			List<Block> succBlocks = new ArrayList<Block>(succUnits.size());
			for (Iterator<JCTree> succIt = succUnits.iterator(); succIt
					.hasNext();) {
				JCTree succUnit = succIt.next();
				Block succBlock = unitToBlock.get(succUnit);
				if (succBlock == null) {
					throw new RuntimeException(
							"BlockGraph(): block tail mapped to null block!");
				}
				succBlocks.add(succBlock);
			}
			if (succBlocks.size() == 0) {
				block.setSuccs(Collections.<Block> emptyList());
				if (!mTails.contains(block)) {
					throw new RuntimeException(
							"Block with no successors is not a tail!: "
									+ block.toString());
					// Note that a block can be a tail even if it has
					// successors: a return that throws a caught exception.
				}
			} else {
				block.setSuccs(Collections.unmodifiableList(succBlocks));
			}
		}
		mBlocks = Collections.unmodifiableList(blockList);
		mHeads = Collections.unmodifiableList(mHeads);
		if (mTails.size() == 0) {
			mTails = Collections.emptyList();
		} else {
			mTails = Collections.unmodifiableList(mTails);
		}

		return unitToBlock;
	}

	/**
	 * recognize loops and annotate blocks that are part of loop
	 */
	public void loopify() {
		// assuming only one head
		if (this.mHeads == null || this.mHeads.size() == 0)
			return;
		Block head = this.mHeads.get(0);
		List<Block> visited = new ArrayList<Block>();
		java.util.Queue<Block> toVisit = new LinkedList<Block>();
		toVisit.add(head);
		do {
			Block block = toVisit.remove();
			visited.add(block);
			List<Block> succs = block.getSuccs();
			for (Iterator<Block> succsIter = succs.iterator(); succsIter
					.hasNext();) {
				Block succ = succsIter.next();
				if (!visited.contains(succ)) {
					toVisit.add(succ);
				} else {
					annotateLoop(succ);
				}
			}

		} while (!toVisit.isEmpty());
	}

	private void annotateLoop(Block b) {
		List<Block> visited = new ArrayList<Block>();
		java.util.Queue<Block> toVisit = new LinkedList<Block>();
		if (b.getSuccs().size() == 0)
			return;
		toVisit.add(b.getSuccs().get(0));
		Loop loop = new Loop(this);
		loop.header = b;
		b.addLoop(loop);
		loop.addExit(b);
		loop.addBlock(b);
		visited.add(b);
		do {
			Block block = toVisit.remove();
			block.setLoop();
			block.addLoop(loop);
			loop.addBlock(block);
			visited.add(block);
			List<Block> succs = block.getSuccs();
			for (Iterator<Block> succsIter = succs.iterator(); succsIter
					.hasNext();) {
				Block succ = succsIter.next();
				if (!visited.contains(succ)) {
					toVisit.add(succ);
				}
				if (succ.equals(b))
					loop.backJump = block;
			}
		} while (!toVisit.isEmpty());
		this.loops.add(loop);
	}

	private void addBlock(JCTree head, JCTree tail, int index, int length,
			List<Block> blockList, Map<JCTree, Block> unitToBlock) {
		Block block = new Block(head, tail,
				mBody.subList(index, index + length), index, length, this);
		blockList.add(block);
		unitToBlock.put(tail, block);
		unitToBlock.put(head, block);
	}

}
