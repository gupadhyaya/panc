package org.paninij.analysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import com.sun.tools.javac.tree.JCTree.JCAssert;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCErroneous;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCSkip;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCTypeUnion;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.JCTree.LetExpr;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.tree.TreeScanner;

public class MethodBodyVisitor extends TreeScanner {
	public Set<JCTree> leaders = new HashSet<JCTree>();

	public void visitTopLevel(JCCompilationUnit tree) { /* do nothing */
	}

	public void visitImport(JCImport tree) { /* do nothing */
	}

	public void visitMethodDef(JCMethodDecl tree) {
		scan(tree.body);
	}

	public void visitLetExpr(LetExpr tree) { /* do nothing */
	}

	public void visitAnnotation(JCAnnotation tree) { /* do nothing */
	}

	public void visitModifiers(JCModifiers tree) { /* do nothing */
	}

	public void visitErroneous(JCErroneous tree) { /* do nothing */
	}

	public void visitTypeIdent(JCPrimitiveTypeTree tree) { /* do nothing */
	}

	public void visitTypeArray(JCArrayTypeTree tree) { /* do nothing */
	}

	public void visitTypeApply(JCTypeApply tree) { /* do nothing */
	}

	public void visitTypeUnion(JCTypeUnion tree) { /* do nothing */
	}

	public void visitTypeParameter(JCTypeParameter tree) { /* do nothing */
	}

	public void visitWildcard(JCWildcard tree) { /* do nothing */
	}

	public void visitTypeBoundKind(TypeBoundKind tree) { /* do nothing */
	}

	public void visitSkip(JCSkip tree) { /* do nothing */
	}

	public void visitClassDef(JCClassDecl tree) {
		printCurrent(tree);
	}

	public void visitIdent(JCIdent tree) {
		printCurrent(tree);
	}

	public void visitLiteral(JCLiteral tree) {
		printCurrent(tree);
	}

	public void visitAssert(JCAssert tree) {
		printCurrent(tree);
		super.visitAssert(tree);
	}

	public void visitLabelled(JCLabeledStatement tree) {
		printCurrent(tree);
		super.visitLabelled(tree);
	}

	public void visitVarDef(JCVariableDecl tree) {
		printCurrent(tree);

		super.visitVarDef(tree);
	}

	public void visitBlock(JCBlock tree) {
		// printCurrent(tree);

		super.visitBlock(tree);
	}

	public void visitDoLoop(JCDoWhileLoop tree) {
		printCurrent(tree);

		super.visitDoLoop(tree);
	}

	public void visitWhileLoop(JCWhileLoop tree) {
		printCurrent(tree);

		super.visitWhileLoop(tree);
	}

	public void visitForLoop(JCForLoop tree) {
		printCurrent(tree);

		super.visitForLoop(tree);
	}

	public void visitForeachLoop(JCEnhancedForLoop tree) {
		printCurrent(tree);

		super.visitForeachLoop(tree);
	}

	public void visitSwitch(JCSwitch tree) {
		printCurrent(tree);

		super.visitSwitch(tree);
	}

	public void visitCase(JCCase tree) {
		printCurrent(tree);

		super.visitCase(tree);
	}

	public void visitSynchronized(JCSynchronized tree) {
		printCurrent(tree);

		super.visitSynchronized(tree);
	}

	public void visitTry(JCTry tree) {
		printCurrent(tree);

		super.visitTry(tree);
	}

	public void visitCatch(JCCatch tree) {
		printCurrent(tree);

		super.visitCatch(tree);
	}

	public void visitConditional(JCConditional tree) {
		printCurrent(tree);

		super.visitConditional(tree);
	}

	public void visitIf(JCIf tree) {
		printCurrent(tree);

		super.visitIf(tree);
	}

	public void visitExec(JCExpressionStatement tree) {
		printCurrent(tree);

		super.visitExec(tree);
	}

	public void visitBreak(JCBreak tree) {
		printCurrent(tree);

		super.visitBreak(tree);
	}

	public void visitContinue(JCContinue tree) {
		printCurrent(tree);

		super.visitContinue(tree);
	}

	public void visitReturn(JCReturn tree) {
		printCurrent(tree);

		super.visitReturn(tree);
	}

	public void visitThrow(JCThrow tree) {
		printCurrent(tree);

		super.visitThrow(tree);
	}

	public void visitApply(JCMethodInvocation tree) {
		printCurrent(tree);

		super.visitApply(tree);
	}

	public void visitNewClass(JCNewClass tree) {
		printCurrent(tree);

		// super.visitNewClass(tree);
	}

	public void visitNewArray(JCNewArray tree) {
		printCurrent(tree);

		super.visitNewArray(tree);
	}

	public void visitParens(JCParens tree) {
		printCurrent(tree);

		super.visitParens(tree);
	}

	public void visitAssign(JCAssign tree) {
		printCurrent(tree);

		super.visitAssign(tree);
	}

	public void visitAssignop(JCAssignOp tree) {
		printCurrent(tree);

		super.visitAssignop(tree);
	}

	public void visitUnary(JCUnary tree) {
		printCurrent(tree);

		super.visitUnary(tree);
	}

	public void visitBinary(JCBinary tree) {
		printCurrent(tree);

		super.visitBinary(tree);
	}

	public void visitTypeCast(JCTypeCast tree) {
		printCurrent(tree);

		super.visitTypeCast(tree);
	}

	public void visitTypeTest(JCInstanceOf tree) {
		printCurrent(tree);

		super.visitTypeTest(tree);
	}

	public void visitIndexed(JCArrayAccess tree) {
		printCurrent(tree);

		super.visitIndexed(tree);
	}

	public void visitSelect(JCFieldAccess tree) {
		printCurrent(tree);

		super.visitSelect(tree);
	}

	private void printCurrent(JCTree tree) {
		List<JCTree> predecessors = tree.predecessors;
		int predCount = (predecessors != null) ? predecessors.size() : 0;
		List<JCTree> successors = tree.successors;
		int succCount = (successors != null) ? successors.size() : 0;

		if (predCount != 1) { // If predCount == 1 but the predecessor
			leaders.add(tree); // is a branch, u will get added by that
		} // branch's successor test.
		if (succCount > 1) { // || (u.branches())
			for (Iterator<JCTree> it = successors.iterator(); it.hasNext();) {
				leaders.add(it.next()); // The cast is an
			} // assertion check.
		}
		/*
		 * if (tree.predecessors != null) { for (JCTree next :
		 * tree.predecessors) { System.out.println(nodeText(next) + " -> " +
		 * nodeText(tree)); } }
		 */
	}

	private static String nodeText(JCTree tree) {
		return "\"" + tree.id + " " + tree.toString().replace("\"", "\\\"")
				+ "\"";
	}
}
