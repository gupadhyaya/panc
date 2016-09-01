package org.paninij.analysis;

import java.util.*;

import com.sun.tools.javac.tree.JCTree;

public class Block {
	private JCTree mHead, mTail;
	private final List<JCTree> mBody;
	private List<Block> mPreds, mSuccessors;
	private int mBlockLength = 0, mIndexInMethod = 0;
	private boolean isLoop = false;
	private Loop loop = null;

	public Block(JCTree aHead, JCTree aTail, List<JCTree> aBody,
			int aIndexInMethod, int aBlockLength, BlockGraph aBlockGraph) {
		mHead = aHead;
		mTail = aTail;
		mBody = aBody;
		mIndexInMethod = aIndexInMethod;
		mBlockLength = aBlockLength;
	}

	public List<JCTree> getBody() {
		return mBody;
	}

	public Iterator<JCTree> iterator() {
		if (mBody != null)
			return mBody.iterator();
		else
			return null;
	}

	public JCTree getSuccOf(JCTree aItem) {
		if (aItem != mTail)
			return aItem.successors.get(0);
		else
			return null;
	}

	public JCTree getPredOf(JCTree aItem) {
		if (aItem != mHead)
			return aItem.predecessors.get(0);
		else
			return null;
	}

	public void setIndexInMethod(int aIndexInMethod) {
		mIndexInMethod = aIndexInMethod;
	}

	public int getIndexInMethod() {
		return mIndexInMethod;
	}

	public JCTree getHead() {
		return mHead;
	}

	public JCTree getTail() {
		return mTail;
	}

	public void setPreds(List<Block> preds) {
		mPreds = preds;
		return;
	}

	public List<Block> getPreds() {
		return mPreds;
	}

	public void setSuccs(List<Block> succs) {
		mSuccessors = succs;
	}

	public List<Block> getSuccs() {
		return mSuccessors;
	}

	public void setLoop() {
		this.isLoop = true;
	}

	public boolean isLoop() {
		return this.isLoop;
	}

	public void addLoop(Loop l) {
		this.loop = l;
	}

	public Loop loop() {
		return this.loop;
	}
}
