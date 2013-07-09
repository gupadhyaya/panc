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
 * Contributor(s): Hridesh Rajan, Eric Lin, Sean L. Mooney
 */

package org.paninij.comp;


import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.TypeTags.*;
import static com.sun.tools.javac.tree.JCTree.Tag.APPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.ASSIGN;
import static com.sun.tools.javac.tree.JCTree.Tag.CAPSULEARRAY;
import static com.sun.tools.javac.tree.JCTree.Tag.EXEC;
import static com.sun.tools.javac.tree.JCTree.Tag.FOREACHLOOP;
import static com.sun.tools.javac.tree.JCTree.Tag.LT;
import static com.sun.tools.javac.tree.JCTree.Tag.MAAPPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.PREINC;
import static com.sun.tools.javac.tree.JCTree.Tag.TYPEIDENT;
import static com.sun.tools.javac.tree.JCTree.Tag.VARDEF;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.paninij.analysis.ASTCFGBuilder;
import org.paninij.consistency.*;
import static org.paninij.code.Type.*;
import static org.paninij.consistency.ConsistencyUtil.SEQ_CONST_ALG;

import com.sun.tools.javac.code.CapsuleProcedure;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.CapsuleExtras;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.PaniniConstants;
import org.paninij.system.*;
import org.paninij.systemgraph.*;
import org.paninij.systemgraph.SystemGraph.Node;

import com.sun.source.tree.Tree.Kind;
import java.util.ArrayList;

/***
 * Panini-specific context-dependent analysis. All public functions in 
 * this class, are called from com.sun.tools.javac.comp.Attr to separate 
 * out Panini code. So, visitX in this class is called from method visitX 
 * in the class com.sun.tools.javac.comp.Attr.
 * 
 * @author hridesh
 *
 */
public final class Attr extends CapsuleInternal {
	Log log;
	Annotate annotate;
	AnnotationProcessor annotationProcessor;
	SystemGraphBuilder systemGraphBuilder;
	Types types;

    public Attr(TreeMaker make, Names names, Types types, Enter enter,
            MemberEnter memberEnter, Symtab syms, Log log,
            Annotate annotate) {
        super(make, names, enter, memberEnter, syms);
        this.types = types;
        this.log = log;
        this.annotate = annotate;
        this.annotationProcessor = new AnnotationProcessor(names, make, log);
        this.systemGraphBuilder = new SystemGraphBuilder(syms, names, log);
    }

    void attribSystemDecl(JCSystemDecl tree, Resolve rs, Env<AttrContext> env ) {
        //This is where the systemDecl attribution will go, when
        //pulled in from sun.tools.javac.comp.Attr.visitSystemDecl
    }

	public void visitTopLevel(JCCompilationUnit tree) { /* SKIPPED */ }
	public void visitImport(JCImport tree) { /* SKIPPED */ }
	public void visitLetExpr(LetExpr tree) { /* SKIPPED */ }
	public void visitAnnotation(JCAnnotation tree) { /* SKIPPED */ }
	public void visitModifiers(JCModifiers tree) { /* SKIPPED */ }
	public void visitErroneous(JCErroneous tree) { /* SKIPPED */ }
	public void visitTypeIdent(JCPrimitiveTypeTree tree) { /* SKIPPED */ }
	public void visitTypeApply(JCTypeApply tree) { /* SKIPPED */ }
	public void visitTypeUnion(JCTypeUnion tree) { /* SKIPPED */ }
	public void visitTypeParameter(JCTypeParameter tree) { /* SKIPPED */ }
	public void visitWildcard(JCWildcard tree) { /* SKIPPED */ }
	public void visitTypeBoundKind(TypeBoundKind tree) { /* SKIPPED */ }
	public void visitIdent(JCIdent tree) {  /* SKIPPED */ }
	public void visitLiteral(JCLiteral tree) {  /* SKIPPED */ }
	public void visitTypeArray(JCArrayTypeTree tree) {  /* SKIPPED */ }
	public void visitSkip(JCSkip tree) {  /* SKIPPED */ }
	public final void visitClassDef(JCClassDecl tree) {  /* SKIPPED */ }
	public final void visitLabelled(JCLabeledStatement tree) {  /* SKIPPED */ }
	public final void visitAssert(JCAssert tree) {  /* SKIPPED */ }

	public final void preVisitMethodDef(JCMethodDecl tree,
			final com.sun.tools.javac.comp.Attr attr) {
		if (tree.sym.isProcedure) {
			try {
				((JCProcDecl) tree).switchToProc();
				tree.accept(attr);
			} catch (ClassCastException e) {
			}
		}
	}

	public final void postVisitMethodDef(JCMethodDecl tree, Env<AttrContext> env, Resolve rs) {
		if (tree.body != null) {
			tree.accept(new ASTCFGBuilder());
		}

		if (tree.sym.owner.isCapsule()) {
			////
//			EffectSet es = new EffectSet();
//			es.add(es.bottomEffect());
//			es.add(es.methodEffect(tree.sym));
//			annotationProcessor.setEffects(tree, es);
//			annotate.enterAnnotation(tree.mods.annotations.last(), Type.noType, env);
			//// to add effectset: move out of if clause and remove test set; change second argument of setEffects to actual effectSet
			CapsuleProcedure cp = new CapsuleProcedure((ClassSymbol) tree.sym.owner,
					tree.name, tree.sym.params);
			((ClassSymbol) tree.sym.owner).capsule_info.procedures.put(tree.sym, cp);
			if(tree.sym.effect!=null){
				annotationProcessor.setEffects(tree, tree.sym.effect);
				Attribute.Compound buf = annotate.enterAnnotation(tree.mods.annotations.last(), Type.noType, env);
				tree.sym.attributes_field = tree.sym.attributes_field.append(buf); 
			}
		}
	}
	
	public final void visitVarDef(JCVariableDecl tree) {  /* SKIPPED */ }
	public final void visitBlock(JCBlock tree) {  /* SKIPPED */ }
	public final void visitDoLoop(JCDoWhileLoop tree) {  /* SKIPPED */ }
	public final void visitWhileLoop(JCWhileLoop tree){  /* SKIPPED */ }
	public final void visitForLoop(JCForLoop tree){  /* SKIPPED */ }
	public final void visitForeachLoop(JCEnhancedForLoop tree) {  /* SKIPPED */ }
	public final void visitSwitch(JCSwitch tree) {  /* SKIPPED */ }
	public final void visitCase(JCCase tree) {  /* SKIPPED */ }
	public final void visitSynchronized(JCSynchronized tree) {  /* SKIPPED */ }
	public final void visitTry(JCTry tree) {  /* SKIPPED */ }
	public final void visitCatch(JCCatch tree) {  /* SKIPPED */ }
	public final void visitConditional(JCConditional tree) {  /* SKIPPED */ }
	public final void visitIf(JCIf tree) {  /* SKIPPED */ }
	public final void visitExec(JCExpressionStatement tree) {  /* SKIPPED */ }
	public final void visitBreak(JCBreak tree) {  /* SKIPPED */ }
	public final void visitContinue(JCContinue tree) {  /* SKIPPED */ }
	public final void visitReturn(JCReturn tree) {  /* SKIPPED */ }
	public final void visitThrow(JCThrow tree) {  /* SKIPPED */ }
	public final void visitApply(JCMethodInvocation tree) {  /* SKIPPED */ }
	public final void visitNewClass(JCNewClass tree) {  /* SKIPPED */ }
	public final void visitNewArray(JCNewArray tree) {  /* SKIPPED */ }
	public final void visitParens(JCParens tree) {  /* SKIPPED */ }
	public final void visitAssign(JCAssign tree) {  /* SKIPPED */ }
	public final void visitAssignop(JCAssignOp tree) {  /* SKIPPED */ }
	public final void visitUnary(JCUnary tree) {  /* SKIPPED */ }
	public final void visitBinary(JCBinary tree) {  /* SKIPPED */ }
	public void visitTypeCast(JCTypeCast tree) {  /* SKIPPED */ }
	public void visitTypeTest(JCInstanceOf tree) {  /* SKIPPED */ }
	public void visitIndexed(JCArrayAccess tree) {  /* SKIPPED */ }
	public void visitSelect(JCFieldAccess tree) {  /* SKIPPED */ }
	
	public final void visitCapsuleDef(final JCCapsuleDecl tree, final com.sun.tools.javac.comp.Attr attr, Env<AttrContext> env, Resolve rs){
		if (tree.needsDefaultRun){
			List<JCClassDecl> wrapperClasses = generateClassWrappers(tree, env, rs);
			enter.classEnter(wrapperClasses, env.outer);
//			        	System.out.println(wrapperClasses);
			attr.attribClassBody(env, tree.sym);
			if((tree.sym.flags_field & TASK) !=0)
				tree.computeMethod.body = generateTaskCapsuleComputeMethodBody(tree);
			else{
				tree.computeMethod.body = generateThreadCapsuleComputeMethodBody(tree);
				tree.computeMethod.body.stats = tree.computeMethod.body.stats
						.prepend(make.Exec(make.Apply(
								List.<JCExpression> nil(),
								make.Ident(names
										.fromString(PaniniConstants.PANINI_CAPSULE_INIT)),
								List.<JCExpression> nil()))); 
			}
		}
		else {
			attr.attribClassBody(env, tree.sym);
			if(tree.computeMethod!=null)
				tree.computeMethod.body.stats = tree.computeMethod.body.stats
						.prepend(make.Exec(make.Apply(
								List.<JCExpression> nil(),
								make.Ident(names
										.fromString(PaniniConstants.PANINI_CAPSULE_INIT)),
								List.<JCExpression> nil())));
			if ((tree.sym.flags_field & ACTIVE) != 0) {
				// Reference count disconnect()
				ListBuffer<JCStatement> blockStats = new ListBuffer<JCStatement>();
				List<JCVariableDecl> params = tree.params;
				for (JCVariableDecl jcVariableDecl : params) {
					if (jcVariableDecl.vartype.type.tsym.isCapsule()) {
						JCStatement stmt = make
								.Exec(make.Apply(
										List.<JCExpression> nil(),
										make.Select(
												make.TypeCast(
														make.Ident(names
																.fromString(PaniniConstants.PANINI_QUEUE)),
														make.Ident(jcVariableDecl.name)),
												names.fromString(PaniniConstants.PANINI_DISCONNECT)),
										List.<JCExpression> nil()));

						blockStats.append(stmt);
					}
				}

				List<JCCatch> catchers = List
						.<JCCatch> of(make.Catch(
								make.VarDef(make.Modifiers(0), names
										.fromString("e"), make.Ident(names
										.fromString("Exception")), null), make
										.Block(0, List.<JCStatement> nil())));
				List<JCStatement> bodyStats = (make.Try(
						make.Block(0, blockStats.toList()), catchers,
						null)).getBlock().stats;
				
				tree.computeMethod.body.stats = tree.computeMethod.body.stats
						.append(make.Try(
								make.Block(0, blockStats.toList()), catchers,
								null));//List(bodyStats);
			}
			
			if ((tree.sym.flags_field & SERIAL) != 0 || (tree.sym.flags_field & MONITOR) != 0) {
				// For serial capsule version
				ListBuffer<JCStatement> blockStats = new ListBuffer<JCStatement>();
				List<JCVariableDecl> params = tree.params;
				for (JCVariableDecl jcVariableDecl : params) {
					if (jcVariableDecl.vartype.type.tsym.isCapsule()) {
						JCStatement stmt = make.Exec(make.Apply(
								List.<JCExpression> nil(),
								make.Select(
										make.TypeCast(
												make.Ident(names
														.fromString(PaniniConstants.PANINI_QUEUE)),
												make.Ident(jcVariableDecl.name)),
										names.fromString(PaniniConstants.PANINI_DISCONNECT)),
								List.<JCExpression> nil()));

						blockStats.append(stmt);
					}
				}
				
				List<JCCatch> catchers = List.<JCCatch> of(make.Catch(make.VarDef(
						make.Modifiers(0), names.fromString("e"),
						make.Ident(names.fromString("Exception")), null),
						make.Block(0, List.<JCStatement> nil())));
				ListBuffer<JCStatement> methodStats = new ListBuffer<JCStatement>();
				methodStats.append(make.Exec(make.Unary(JCTree.Tag.POSTDEC, make.Ident(names
						.fromString(PaniniConstants.PANINI_REF_COUNT)))));
			
				methodStats.append(make.If(make.Binary(JCTree.Tag.EQ, make.Ident(names
						.fromString(PaniniConstants.PANINI_REF_COUNT)), make.Literal(TypeTags.INT, new Integer(0))), 
						make.Block(0, blockStats.toList()), null));
				
				JCBlock body = make.Block(0, methodStats.toList());
				
				JCMethodDecl disconnectMeth = null;
				MethodSymbol msym = null;
				msym = new MethodSymbol(PUBLIC | FINAL | Flags.SYNCHRONIZED,
						names.fromString(PaniniConstants.PANINI_DISCONNECT),
						new MethodType(List.<Type> nil(), syms.voidType,
								List.<Type> nil(), syms.methodClass), tree.sym);
				disconnectMeth = make.MethodDef(
						make.Modifiers(PUBLIC | FINAL | Flags.SYNCHRONIZED),
						names.fromString(PaniniConstants.PANINI_DISCONNECT),
						make.TypeIdent(TypeTags.VOID),
						List.<JCTypeParameter> nil(),
						List.<JCVariableDecl> nil(), List.<JCExpression> nil(),
						body, null);
				disconnectMeth.sym = msym;
				tree.defs = tree.defs.append(disconnectMeth);
			}
		}
		for(JCTree def : tree.defs){
			if(def instanceof JCMethodDecl){
				for(JCVariableDecl param : ((JCMethodDecl)def).params){
					if(param.type.tsym.isCapsule() &&!((JCMethodDecl)def).name.toString().contains("$Original")){
						log.error("procedure.argument.illegal", param, ((JCMethodDecl)def).name.toString(), tree.sym);
					}
				}
			}else if(def.getTag() == Tag.VARDEF){
				if(((JCVariableDecl)def).type.tsym.isCapsule())
					((JCVariableDecl)def).mods.flags |= FINAL;
			}
		}
	}
	
	private void initRefCount(Map<Name, JCFieldAccess> refCountStats,
			ListBuffer<JCStatement> assigns, SystemGraph sysGraph) {
		Set<Entry<Name, JCFieldAccess>> entrySet = refCountStats.entrySet();//sysGraph.nodes.entrySet();//variables.entrySet();
		for (Entry<Name, JCFieldAccess> entry : entrySet) {
			// Reference count update
			int refCount = 0;
			Name vdeclName = entry.getKey();
			refCount = sysGraph.nodes.get(vdeclName).indegree;
			JCFieldAccess accessStat = entry.getValue();
			JCAssign refCountAssign = make
					.Assign(accessStat,
							intlit(refCount));
			JCExpressionStatement refCountAssignStmt = make
					.Exec(refCountAssign);
			assigns.append(refCountAssignStmt);
		}
	}

	public final void visitSystemDef(JCSystemDecl tree, Resolve rs, Env<AttrContext> env, boolean doGraphs, SEQ_CONST_ALG seqConstAlg){
	    attribSystemDecl(tree, rs, env);

	    ListBuffer<JCStatement> decls;
        ListBuffer<JCStatement> inits;
        ListBuffer<JCStatement> assigns;
        ListBuffer<JCStatement> submits;
        ListBuffer<JCStatement> starts;
        ListBuffer<JCStatement> joins;

        SystemGraph sysGraph;

        SystemDeclRewriter interp = new SystemDeclRewriter(make, log);
        JCSystemDecl rewritenTree = interp.rewrite(tree);

        SystemMainTransformer mt = new SystemMainTransformer(syms, names, types, log,
                rs, env, make, systemGraphBuilder);
        rewritenTree = mt.translate(rewritenTree);

        //pull data structures back out for reference here.
        decls = mt.decls;
        inits = mt.inits;
        assigns = mt.assigns;
        submits = mt.submits;
        starts = mt.starts;
        joins = mt.joins;
        sysGraph = mt.sysGraph;

		if(rewritenTree.hasTaskCapsule)
			processSystemAnnotation(rewritenTree, inits, env);
		
		initRefCount(mt.refCountStats, assigns, sysGraph);
		// Reference counting based garbage collection

		List<JCStatement> mainStmts;
		mainStmts = decls.appendList(inits).appendList(assigns).appendList(starts).appendList(joins)/*.appendList(submits)*/.toList();
		JCMethodDecl maindecl = createMainMethod(rewritenTree.sym, rewritenTree.body, rewritenTree.params, mainStmts);
		rewritenTree.defs = rewritenTree.defs.append(maindecl);

		systemGraphBuilder.completeEdges(sysGraph, annotationProcessor, env, rs);

		// Sequential consistency detection
		SeqConstCheckAlgorithm sca = 
		    ConsistencyUtil.createChecker(seqConstAlg, sysGraph, log);
		sca.potentialPathCheck();

		rewritenTree.switchToClass();

		memberEnter.memberEnter(maindecl, env);
		if (doGraphs) {
			//ListBuffer<Symbol> capsules = new ListBuffer<Symbol>();
			for (JCStatement v : decls) {
				if (v.getTag() == VARDEF) {
					JCVariableDecl varDecl = (JCVariableDecl)v;
					ClassSymbol c = syms.capsules.get(names.fromString(varDecl.vartype.toString()));
					//System.out.println("indegree: "+ c.indegree + ", outdegree: "+ c.outdegree);
					if (varDecl.vartype.toString().contains("[]")) {
						//                    System.out.println("\n\n\nConsistency checker doesn't yet support capsule arrays. Exiting now.\n\n\n");
						//                    System.exit(5);
						//c = syms.capsules.get(names.fromString(varDecl.vartype.toString().substring(0, varDecl.vartype.toString().indexOf("["))));

					}
					//if (!capsules.contains(c)) capsules.append(c);
				}
			}
			//tree.sym.capsules = capsules.toList();
		}
	}

	public final void visitProcDef(JCProcDecl tree){
		Type restype = ((MethodType)tree.sym.type).restype;
		if((restype.tsym.flags_field & Flags.CAPSULE) == 1){
			log.error(tree.pos(), "procedure.restype.illegal.capsule");
		}
		tree.switchToMethod();
	}
	
	// Helper functions
	private void processSystemAnnotation(JCSystemDecl tree, ListBuffer<JCStatement> stats, Env<AttrContext> env){
		int numberOfPools = 1;
		for(JCAnnotation annotation : tree.mods.annotations){
			if(annotation.annotationType.toString().equals("Parallelism")){
				if(annotation.args.isEmpty())
					log.error(tree.pos(), "annotation.missing.default.value", annotation, "value");
				else if (annotation.args.size()==1 && annotation.args.head.getTag()==ASSIGN){
					if (annotate.enterAnnotation(annotation,
							syms.annotationType, env).member(names.value).type == syms.intType)
						numberOfPools = (Integer) annotate
						.enterAnnotation(annotation,
								syms.annotationType, env)
								.member(names.value).getValue();
				}
			}
		}
		stats.prepend(make.Try(make.Block(0, List.<JCStatement> of(make
				.Exec(make.Apply(List.<JCExpression> nil(), make
						.Select(make.Ident(names
								.fromString(PaniniConstants.PANINI_CAPSULE_TASK)),
								names.fromString(PaniniConstants.PANINI_INIT)), List
								.<JCExpression> of(make.Literal(numberOfPools)))))),
								List.<JCCatch> of(make.Catch(make.VarDef(
										make.Modifiers(0), names.fromString("e"),
										make.Ident(names.fromString("Exception")),
										null), make.Block(0,
												List.<JCStatement> nil()))), null));
	}

	private JCMethodDecl createMainMethod(final ClassSymbol containingClass, final JCBlock methodBody, final List<JCVariableDecl> params, final List<JCStatement> mainStmts) {
		Type arrayType = new ArrayType(syms.stringType, syms.arrayClass);
		MethodSymbol msym = new MethodSymbol(
				PUBLIC|STATIC,
				names.fromString("main"),
				new MethodType(
						List.<Type>of(arrayType),
						syms.voidType,
						List.<Type>nil(),
						syms.methodClass
						),
						containingClass
				);
		JCMethodDecl maindecl = make.MethodDef(msym, methodBody);
		JCVariableDecl mainArg = null; 
		if(params.length() == 0) {
			mainArg = make.Param( names.fromString("args"), arrayType, msym);
			maindecl.params = List.<JCVariableDecl>of(mainArg);
		} else maindecl.params = params;

		maindecl.body.stats = mainStmts;
		return maindecl;
	}
}
