package org.paninij.effects;

import java.util.*;

import javax.lang.model.element.ElementKind;

import org.paninij.analysis.CommonMethod;
import org.paninij.path.*;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;

public class EffectIntra {
	// detect whether the method always return newly created object.
	public boolean returnNewObject = true;

	public final ArrayList<JCTree> order;
	public final EffectInter inter;
	public final HashMap<JCTree, AliasingGraph> aliasing;
	public final JCMethodDecl curr_meth;

	public HashMap<JCTree, EffectSet> effectBeforeFlow =
	    new HashMap<JCTree, EffectSet>();
	public HashMap<JCTree, EffectSet> effectAfterFlow =
	    new HashMap<JCTree, EffectSet>();

	// pair of the capsule calls that have no synchronization in between
	public HashSet<BiCall> direct = new HashSet<BiCall>();
	// pair of the capsule calls that have synchronization in between
	public HashSet<BiCall> indirect = new HashSet<BiCall>();

	public EffectIntra(EffectInter inter,
			JCMethodDecl curr_meth, ArrayList<JCTree> order,
			HashMap<JCTree, AliasingGraph> aliasing) {
		this.inter = inter;
		this.order = order;
		this.aliasing = aliasing;
		this.curr_meth = curr_meth;
	}

	private final void flowThrough(JCTree tree, AliasingGraph aliasing,
							   EffectSet inValue, EffectSet out) {
		out.init(inValue);
		if (out.isBottom) { return; }

		if (tree instanceof JCMethodInvocation) { /////////// Calls
			inter.intraProcessMethodCall((JCMethodInvocation)tree, aliasing,
					out, this);
		} else if (tree instanceof JCAssign) { // lhs =
			abstractCommandAssign(tree, ((JCAssign)tree).lhs, aliasing, out);
		} else if (tree instanceof JCAssignOp) { // lhs X=
			abstractCommandAssign(tree, ((JCAssignOp)tree).lhs, aliasing, out);
		} else if (tree instanceof JCUnary) { // sth++
			JCUnary jcu = (JCUnary)tree;
			Tag opcode = jcu.getTag();
			if (opcode == JCTree.Tag.PREINC || opcode == JCTree.Tag.POSTINC || 
				opcode == JCTree.Tag.POSTDEC || opcode == JCTree.Tag.PREDEC) {
				abstractCommandAssign(tree, jcu.arg, aliasing, out);
			} else if (opcode == JCTree.Tag.POS || opcode == JCTree.Tag.NEG ||
					opcode == JCTree.Tag.NOT || opcode == JCTree.Tag.COMPL) {
				// -sth, +sth, !sth or ~sth, ignore
			} else throw new Error("opcode match error = " + opcode);
		} else if(tree instanceof JCFieldAccess) { // field read o.f
			addFieldAccessEffect((JCFieldAccess)tree, aliasing, out, 0);
		} else if (tree instanceof JCIdent) {      // field read f, omitted this
			addIdentEffect((JCIdent)tree, out, 0);
		} else if (tree instanceof JCArrayAccess) { // array read a[i]
			JCArrayAccess arr = (JCArrayAccess) tree;
			addArrayAccessEffect(arr.indexed, arr.index, aliasing, 0, out);
		} else if (tree instanceof JCForeach) {
			inter.intraForeach((JCForeach)tree, aliasing, this, out);
		} else if (tree instanceof JCReturn) {
			JCExpression expr = ((JCReturn) tree).expr;
			if (expr != null &&
					!(expr instanceof JCNewClass) &&
					!(expr instanceof JCNewArray) &&
					!aliasing.isReceiverNew(expr)) {
				returnNewObject = false;
			}
		} else if (tree instanceof JCCatch ||
				tree instanceof JCBinary || tree instanceof JCInstanceOf ||
				tree instanceof JCTypeCast || tree instanceof JCVariableDecl ||
				tree instanceof JCMethodDecl || tree instanceof JCModifiers ||
				tree instanceof JCTypeParameter ||
				tree instanceof TypeBoundKind //the followings are JCExpression 
				|| tree instanceof JCAnnotation ||
				tree instanceof JCArrayTypeTree || tree instanceof JCConditional
				|| tree instanceof JCLiteral || tree instanceof JCNewArray ||
				tree instanceof JCNewClass || tree instanceof JCParens ||
				tree instanceof JCPrimitiveTypeTree ||
				tree instanceof JCTypeApply || tree instanceof JCTypeUnion ||
				tree instanceof JCWildcard || //the followings are JCStatement
				tree instanceof JCAssert || tree instanceof JCBlock ||
				tree instanceof JCBreak || tree instanceof JCCase ||
				tree instanceof JCClassDecl || tree instanceof JCContinue ||
				tree instanceof JCDoWhileLoop || tree instanceof JCForLoop ||
				tree instanceof JCEnhancedForLoop || tree instanceof JCIf ||
				tree instanceof JCExpressionStatement ||
				tree instanceof JCLabeledStatement || tree instanceof JCSkip || 
				tree instanceof JCSwitch || tree instanceof JCSynchronized ||
				tree instanceof JCThrow || tree instanceof JCTry ||
				tree instanceof JCWhileLoop) {
			// ignored do nothing...
		} else if (tree instanceof JCCompilationUnit || tree instanceof JCImport
				|| tree instanceof JCMethodDecl || tree instanceof JCErroneous
				|| tree instanceof LetExpr) {
		} else throw new Error("JCTree match faliure " + tree);
	}

	private static void abstractCommandAssign(JCTree tree, JCExpression leftOp, 
			AliasingGraph aliasing, EffectSet out) {
		leftOp = CommonMethod.essentialExpr(leftOp);

		if (leftOp instanceof JCIdent) {/////////// v=...
			addIdentEffect((JCIdent)leftOp, out, 1);
		} else if (leftOp instanceof JCFieldAccess) {  // X.f = ..., ///////////
			addFieldAccessEffect((JCFieldAccess)leftOp, aliasing, out, 1);
		} else if (leftOp instanceof JCArrayAccess) {  ////////////// v[i] = ...
			JCArrayAccess arr = (JCArrayAccess) leftOp;
			addArrayAccessEffect(arr.indexed, arr.index, aliasing, 1, out);
		} else if (leftOp instanceof JCAnnotation ||
				leftOp instanceof JCArrayTypeTree || leftOp instanceof JCAssign
				|| leftOp instanceof JCAssignOp || leftOp instanceof JCBinary ||
				leftOp instanceof JCConditional || leftOp instanceof JCErroneous
				|| leftOp instanceof JCInstanceOf || leftOp instanceof JCLiteral
				|| leftOp instanceof JCMethodInvocation ||
				leftOp instanceof JCNewArray || leftOp instanceof JCNewClass ||
				leftOp instanceof JCPrimitiveTypeTree ||
				leftOp instanceof JCTypeApply || leftOp instanceof JCTypeUnion
				|| leftOp instanceof JCUnary || leftOp instanceof JCWildcard ||
				leftOp instanceof LetExpr) {
			throw new Error("Array match failure = " + tree + " type = " +
					leftOp.getClass());
		} else throw new Error("JCAssign match failure = " + tree + " type = " +
				leftOp.getClass());
	}

	public static void addArrayAccessEffect(JCExpression indexed,
			JCExpression index, AliasingGraph aliasing, int readOrWrite,
	        EffectSet result) {
		if (!aliasing.isReceiverNew(indexed)) {
			Path p = aliasing.createPathForExp(indexed);

			Type type = indexed.type;
			if (readOrWrite == 0) {
				result.read.add(new ArrayEffect(p, type));
			} else {
				result.write.add(new ArrayEffect(p, type));
			}
		}
	}

	public static void addFieldAccessEffect(JCFieldAccess jcf,
			AliasingGraph aliasing, EffectSet result, int readOrWrite) {
		Symbol sym = jcf.sym;
		if ((sym.flags_field & Flags.STATIC) == 0) {
			if (sym.getKind() == ElementKind.FIELD) {
				JCExpression selected = jcf.selected;
				selected = CommonMethod.essentialExpr(selected);

				if (!aliasing.isReceiverNew(selected)) {
					Path p = aliasing.createPathForExp(selected);
	
					if (readOrWrite == 0) {
						result.read.add(new FieldEffect(p, sym));
					} else {
						result.assignField(sym);
						result.write.add(new FieldEffect(p, sym));
					}
				}
			} else if (sym.getKind() == ElementKind.METHOD ||
					sym.getKind() == ElementKind.PACKAGE ||
					sym.getKind() == ElementKind.CLASS ||
					sym.getKind() == ElementKind.CONSTRUCTOR ||
					sym.getKind() == ElementKind.ENUM ||
					sym.getKind() == ElementKind.INTERFACE) { // ignore
			} else throw new Error("should be a field = " + jcf + "\t" +
					sym.getKind());
		}
	}

	public static void addIdentEffect(JCIdent left, EffectSet fcg,
			int readOrWrite) {
		Symbol sym = left.sym;
		if ((sym.flags_field & Flags.STATIC) == 0) {
			ElementKind kind = sym.getKind();
			if (kind == ElementKind.FIELD) {
				if (sym.name.toString().compareTo("this") != 0) {
					// ignore this = x
					if (readOrWrite == 0) {
						fcg.read.add(
								new FieldEffect(
										new Path_Parameter(null, 0), sym));
					} else {
						fcg.assignField(sym);
						fcg.write.add(
								new FieldEffect(
										new Path_Parameter(null, 0), sym));
					}
				}
			} else if (kind == ElementKind.LOCAL_VARIABLE ||
					kind == ElementKind.PARAMETER ||
					kind == ElementKind.EXCEPTION_PARAMETER) {
				if (readOrWrite == 1) { fcg.assignVar(sym); }
			} else if (kind == ElementKind.CLASS ||
					kind == ElementKind.INTERFACE || kind == ElementKind.METHOD
					|| kind == ElementKind.CONSTRUCTOR ||
					kind == ElementKind.PACKAGE ||
					kind == ElementKind.ENUM) {
			} else throw new Error("Match failure = " + kind + "\t" + left);
		}
	}

	private EffectSet newInitialFlow() { return new EffectSet(); }
	private EffectSet entryInitialFlow() { return new EffectSet(true); }

	private static void mergeInto(EffectSet inout, EffectSet in) {
		EffectSet tmp = new EffectSet();
		tmp = new EffectSet(inout);
		tmp.union(in);

		inout.init(tmp);
	}

	public EffectSet doAnalysis() {
		JCTree head = order.get(0);
		Collection<JCTree> changedUnits = CommonMethod.constructWorklist(order);

	    // Create the order of the AST for analyzing the effect of the methods.
        // This order is used to make the algorithm converge faster.
		HashSet<JCTree> resultNodes = new HashSet<JCTree>();
		for (JCTree node : order) {
			changedUnits.add(node);
			resultNodes.add(node);
			effectBeforeFlow.put(node, newInitialFlow());
			effectAfterFlow.put(node, newInitialFlow());
		}

		if (head != null) {
			effectBeforeFlow.put(head, entryInitialFlow());
			effectAfterFlow.put(head, newInitialFlow());
		}

		// Perform fixed point flow analysis
		while (!changedUnits.isEmpty()) {
			EffectSet previousAfterFlow = newInitialFlow();

			EffectSet beforeFlow = new EffectSet(true);
			EffectSet afterFlow;
	
			//get the first object
			JCTree s = changedUnits.iterator().next();
			changedUnits.remove(s);
			// previousAfterFlow = new EffectSet(effectAfterFlow.get(s));
			previousAfterFlow.init(effectAfterFlow.get(s));

			// Compute and store beforeFlow
			List<JCTree> preds = s.predecessors;
			beforeFlow = effectBeforeFlow.get(s);

			if (preds.size() > 0) { // copy
				for (JCTree sPred : preds) {
					EffectSet otherBranchFlow = effectAfterFlow.get(sPred);
					mergeInto(beforeFlow, otherBranchFlow);
				}
			}

			// Compute afterFlow and store it.
			afterFlow = effectAfterFlow.get(s);

			flowThrough(s, aliasing.get(s), beforeFlow, afterFlow);

			afterFlow.compress();

			if (!afterFlow.equals(previousAfterFlow)) {
				changedUnits.addAll(s.successors);
			}
		}
		EffectSet resultEffect = new EffectSet();
		for (JCTree astc : resultNodes) {
			EffectSet otherBranchFlow = effectAfterFlow.get(astc);
			mergeInto(resultEffect, otherBranchFlow);
		}
		resultEffect.compress();
		resultEffect.returnNewObject = returnNewObject;
		resultEffect.direct = direct;
		resultEffect.indirect = indirect;

		return resultEffect;
	}
}