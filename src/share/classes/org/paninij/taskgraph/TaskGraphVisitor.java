package org.paninij.taskgraph;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import com.sun.tools.javac.tree.JCTree.JCAssert;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCAssociate;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArray;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArrayCall;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCCapsuleLambda;
import com.sun.tools.javac.tree.JCTree.JCCapsuleWiring;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDesignBlock;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCErroneous;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForAllLoop;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCForeach;
import com.sun.tools.javac.tree.JCTree.JCFree;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCInitDecl;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveCapsuleLambda;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCProcDecl;
import com.sun.tools.javac.tree.JCTree.JCProcInvocation;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCRing;
import com.sun.tools.javac.tree.JCTree.JCSkip;
import com.sun.tools.javac.tree.JCTree.JCStar;
import com.sun.tools.javac.tree.JCTree.JCStateDecl;
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
import com.sun.tools.javac.tree.JCTree.JCWhen;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.JCTree.JCWireall;
import com.sun.tools.javac.tree.JCTree.LetExpr;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;

public class TaskGraphVisitor extends TreeScanner {
	@Override
	public void visitAnnotation(JCAnnotation tree) {
		// TODO Auto-generated method stub
		super.visitAnnotation(tree);
	}
	
	@Override
	public void visitApply(JCMethodInvocation tree) {
		// TODO Auto-generated method stub
		super.visitApply(tree);
	}
	
	@Override
	public void visitAssert(JCAssert tree) {
		// TODO Auto-generated method stub
		super.visitAssert(tree);
	}
	
	@Override
	public void visitAssign(JCAssign tree) {
		// TODO Auto-generated method stub
		super.visitAssign(tree);
	}
	
	@Override
	public void visitAssignop(JCAssignOp tree) {
		// TODO Auto-generated method stub
		super.visitAssignop(tree);
	}
	
	@Override
	public void visitAssociate(JCAssociate tree) {
		// TODO Auto-generated method stub
		super.visitAssociate(tree);
	}
	
	@Override
	public void visitBinary(JCBinary tree) {
		// TODO Auto-generated method stub
		super.visitBinary(tree);
	}
	
	@Override
	public void visitBlock(JCBlock tree) {
		// TODO Auto-generated method stub
		super.visitBlock(tree);
	}
	
	@Override
	public void visitBreak(JCBreak tree) {
		// TODO Auto-generated method stub
		super.visitBreak(tree);
	}
	
	@Override
	public void visitCapsuleArray(JCCapsuleArray tree) {
		// TODO Auto-generated method stub
		super.visitCapsuleArray(tree);
	}
	
	@Override
	public void visitCapsuleDef(JCCapsuleDecl tree) {
		// TODO Auto-generated method stub
		super.visitCapsuleDef(tree);
	}
	
	@Override
	public void visitCapsuleLambda(JCCapsuleLambda tree) {
		// TODO Auto-generated method stub
		super.visitCapsuleLambda(tree);
	}
	
	@Override
	public void visitCapsuleWiring(JCCapsuleWiring tree) {
		// TODO Auto-generated method stub
		super.visitCapsuleWiring(tree);
	}
	
	@Override
	public void visitCase(JCCase tree) {
		// TODO Auto-generated method stub
		super.visitCase(tree);
	}
	
	@Override
	public void visitCatch(JCCatch tree) {
		// TODO Auto-generated method stub
		super.visitCatch(tree);
	}
	
	@Override
	public void visitClassDef(JCClassDecl tree) {
		// TODO Auto-generated method stub
		super.visitClassDef(tree);
	}
	
	@Override
	public void visitConditional(JCConditional tree) {
		// TODO Auto-generated method stub
		super.visitConditional(tree);
	}
	
	@Override
	public void visitContinue(JCContinue tree) {
		// TODO Auto-generated method stub
		super.visitContinue(tree);
	}
	
	@Override
	public void visitDesignBlock(JCDesignBlock that) {
		// TODO Auto-generated method stub
		super.visitDesignBlock(that);
	}
	
	@Override
	public void visitDoLoop(JCDoWhileLoop tree) {
		// TODO Auto-generated method stub
		super.visitDoLoop(tree);
	}
	
	@Override
	public void visitErroneous(JCErroneous tree) {
		// TODO Auto-generated method stub
		super.visitErroneous(tree);
	}
	
	@Override
	public void visitExec(JCExpressionStatement tree) {
		// TODO Auto-generated method stub
		super.visitExec(tree);
	}
	
	@Override
	public void visitForAllLoop(JCForAllLoop tree) {
		// TODO Auto-generated method stub
		super.visitForAllLoop(tree);
	}
	
	@Override
	public void visitForeach(JCForeach tree) {
		// TODO Auto-generated method stub
		super.visitForeach(tree);
	}
	
	@Override
	public void visitForeachLoop(JCEnhancedForLoop tree) {
		// TODO Auto-generated method stub
		super.visitForeachLoop(tree);
	}
	
	@Override
	public void visitForLoop(JCForLoop tree) {
		// TODO Auto-generated method stub
		super.visitForLoop(tree);
	}
	
	@Override
	public void visitFree(JCFree tree) {
		// TODO Auto-generated method stub
		super.visitFree(tree);
	}
	
	@Override
	public void visitIdent(JCIdent tree) {
		// TODO Auto-generated method stub
		super.visitIdent(tree);
	}
	
	@Override
	public void visitIf(JCIf tree) {
		// TODO Auto-generated method stub
		super.visitIf(tree);
	}
	
	@Override
	public void visitImport(JCImport tree) {
		// TODO Auto-generated method stub
		super.visitImport(tree);
	}
	
	@Override
	public void visitIndexed(JCArrayAccess tree) {
		// TODO Auto-generated method stub
		super.visitIndexed(tree);
	}
	
	@Override
	public void visitIndexedCapsuleWiring(JCCapsuleArrayCall tree) {
		// TODO Auto-generated method stub
		super.visitIndexedCapsuleWiring(tree);
	}
	
	@Override
	public void visitInitDef(JCInitDecl tree) {
		// TODO Auto-generated method stub
		super.visitInitDef(tree);
	}
	
	@Override
	public void visitLabelled(JCLabeledStatement tree) {
		// TODO Auto-generated method stub
		super.visitLabelled(tree);
	}
	
	@Override
	public void visitLambda(JCLambda tree) {
		// TODO Auto-generated method stub
		super.visitLambda(tree);
	}
	
	@Override
	public void visitLetExpr(LetExpr tree) {
		// TODO Auto-generated method stub
		super.visitLetExpr(tree);
	}
	
	@Override
	public void visitLiteral(JCLiteral tree) {
		// TODO Auto-generated method stub
		super.visitLiteral(tree);
	}
	
	@Override
	public void visitMethodDef(JCMethodDecl tree) {
		// TODO Auto-generated method stub
		super.visitMethodDef(tree);
	}
	
	@Override
	public void visitModifiers(JCModifiers tree) {
		// TODO Auto-generated method stub
		super.visitModifiers(tree);
	}
	
	@Override
	public void visitNewArray(JCNewArray tree) {
		// TODO Auto-generated method stub
		super.visitNewArray(tree);
	}
	
	@Override
	public void visitNewClass(JCNewClass tree) {
		// TODO Auto-generated method stub
		super.visitNewClass(tree);
	}
	
	@Override
	public void visitParens(JCParens tree) {
		// TODO Auto-generated method stub
		super.visitParens(tree);
	}
	
	@Override
	public void visitPrimitiveCapsuleLambda(JCPrimitiveCapsuleLambda tree) {
		// TODO Auto-generated method stub
		super.visitPrimitiveCapsuleLambda(tree);
	}
	
	@Override
	public void visitProcApply(JCProcInvocation tree) {
		// TODO Auto-generated method stub
		super.visitProcApply(tree);
	}
	
	@Override
	public void visitProcDef(JCProcDecl tree) {
		// TODO Auto-generated method stub
		super.visitProcDef(tree);
	}
	
	@Override
	public void visitReference(JCMemberReference tree) {
		// TODO Auto-generated method stub
		super.visitReference(tree);
	}
	
	@Override
	public void visitReturn(JCReturn tree) {
		// TODO Auto-generated method stub
		super.visitReturn(tree);
	}
	
	@Override
	public void visitRing(JCRing tree) {
		// TODO Auto-generated method stub
		super.visitRing(tree);
	}
	
	@Override
	public void visitSelect(JCFieldAccess tree) {
		// TODO Auto-generated method stub
		super.visitSelect(tree);
	}
	
	@Override
	public void visitSkip(JCSkip tree) {
		// TODO Auto-generated method stub
		super.visitSkip(tree);
	}
	
	@Override
	public void visitStar(JCStar tree) {
		// TODO Auto-generated method stub
		super.visitStar(tree);
	}
	
	@Override
	public void visitStateDef(JCStateDecl tree) {
		// TODO Auto-generated method stub
		super.visitStateDef(tree);
	}
	
	@Override
	public void visitSwitch(JCSwitch tree) {
		// TODO Auto-generated method stub
		super.visitSwitch(tree);
	}
	
	@Override
	public void visitSynchronized(JCSynchronized tree) {
		// TODO Auto-generated method stub
		super.visitSynchronized(tree);
	}
	
	@Override
	public void visitThrow(JCThrow tree) {
		// TODO Auto-generated method stub
		super.visitThrow(tree);
	}
	
	@Override
	public void visitTopLevel(JCCompilationUnit tree) {
		// TODO Auto-generated method stub
		super.visitTopLevel(tree);
	}
	
	@Override
	public void visitTree(JCTree tree) {
		// TODO Auto-generated method stub
		super.visitTree(tree);
	}
	
	@Override
	public void visitTry(JCTry tree) {
		// TODO Auto-generated method stub
		super.visitTry(tree);
	}
	
	@Override
	public void visitTypeApply(JCTypeApply tree) {
		// TODO Auto-generated method stub
		super.visitTypeApply(tree);
	}
	
	@Override
	public void visitTypeArray(JCArrayTypeTree tree) {
		// TODO Auto-generated method stub
		super.visitTypeArray(tree);
	}
	
	@Override
	public void visitTypeBoundKind(TypeBoundKind that) {
		// TODO Auto-generated method stub
		super.visitTypeBoundKind(that);
	}
	
	@Override
	public void visitTypeCast(JCTypeCast tree) {
		// TODO Auto-generated method stub
		super.visitTypeCast(tree);
	}
	
	@Override
	public void visitTypeIdent(JCPrimitiveTypeTree tree) {
		// TODO Auto-generated method stub
		super.visitTypeIdent(tree);
	}
	
	@Override
	public void visitTypeParameter(JCTypeParameter tree) {
		// TODO Auto-generated method stub
		super.visitTypeParameter(tree);
	}
	
	@Override
	public void visitTypeTest(JCInstanceOf tree) {
		// TODO Auto-generated method stub
		super.visitTypeTest(tree);
	}
	
	@Override
	public void visitTypeUnion(JCTypeUnion tree) {
		// TODO Auto-generated method stub
		super.visitTypeUnion(tree);
	}
	
	@Override
	public void visitUnary(JCUnary tree) {
		// TODO Auto-generated method stub
		super.visitUnary(tree);
	}
	
	@Override
	public void visitVarDef(JCVariableDecl tree) {
		// TODO Auto-generated method stub
		super.visitVarDef(tree);
	}
	
	@Override
	public void visitWhen(JCWhen tree) {
		// TODO Auto-generated method stub
		super.visitWhen(tree);
	}
	
	@Override
	public void visitWhileLoop(JCWhileLoop tree) {
		// TODO Auto-generated method stub
		super.visitWhileLoop(tree);
	}
	
	@Override
	public void visitWildcard(JCWildcard tree) {
		// TODO Auto-generated method stub
		super.visitWildcard(tree);
	}
	
	@Override
	public void visitWireall(JCWireall tree) {
		// TODO Auto-generated method stub
		super.visitWireall(tree);
	}
}
