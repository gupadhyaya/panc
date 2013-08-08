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
 * http://www.paninij.org
 *
 * Contributor(s): Rex Fernando
 */

package org.paninij.comp;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.tree.JCTree.*;
import org.paninij.util.PaniniConstants;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.TypeTags.INT;
import static com.sun.tools.javac.tree.JCTree.Tag.LT;
import static com.sun.tools.javac.tree.JCTree.Tag.PREINC;

public class CapsuleInternal extends Internal {
	protected Symtab syms;
	protected Enter enter;
	protected MemberEnter memberEnter;
	protected Types types;
	protected Resolve rs;

	public CapsuleInternal(TreeMaker make, Names names, Types types, Enter enter,
			MemberEnter memberEnter, Symtab syms, Resolve rs) {
		super(make, names);
		this.types = types;
		this.enter = enter;
		this.syms = syms;
		this.memberEnter = memberEnter;
		this.rs = rs;
		specCounter = 0;
	}

	protected JCBlock generateThreadCapsuleComputeMethodBody(JCCapsuleDecl tree) {
		JCModifiers noMods = mods(0);
		ListBuffer<JCStatement> messageLoopBody = new ListBuffer<JCStatement>();
		messageLoopBody.append(var(noMods, PaniniConstants.PANINI_DUCK_TYPE,
				PaniniConstants.DUCK_INTERFACE_NAME,
				apply(PaniniConstants.PANINI_GET_NEXT_DUCK)));

		ListBuffer<JCCase> cases = new ListBuffer<JCCase>();
		int varIndex = 0;

		TreeCopier<Void> tc = new TreeCopier<Void>(make);
		for (JCMethodDecl method : tree.publicMethods) {
			ListBuffer<JCStatement> caseStatements = new ListBuffer<JCStatement>();
			ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
			for (List<JCVariableDecl> l = method.params; l.nonEmpty(); l = l.tail) {
				JCVariableDecl v = l.head;
				JCExpression varType = tc.copy(v.vartype);
				if(method.restype.type.tag == TypeTags.ARRAY)
					caseStatements.append(var(
							noMods,
							"var" + varIndex,
							varType,
							cast(varType,
									select(cast(PaniniConstants.DUCK_INTERFACE_NAME
											+ "$" + PaniniConstants.ARRAY_DUCKS + "$"
											+ tree.name,
											id(PaniniConstants.PANINI_DUCK_TYPE)),
											createFieldString(
													method.name.toString(),
													varType.toString(),
													v.name.toString(),
													method.params)))));
				else
					caseStatements.append(var(
							noMods,
							"var" + varIndex,
							varType,
							cast(varType,
									select(cast(PaniniConstants.DUCK_INTERFACE_NAME
											+ "$" + method.restype + "$"
											+ tree.name,
											id(PaniniConstants.PANINI_DUCK_TYPE)),
											createFieldString(
													method.name.toString(),
													varType.toString(),
													v.name.toString(),
													method.params)))));
				args.append(id("var" + varIndex++));
			}

			Type returnType = ((MethodType) method.sym.type).restype;
			if (returnType.tag == TypeTags.VOID) {
				caseStatements.append(es(createOriginalCall(method, args)));
				caseStatements.append(es(apply(
						PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_FINISH, args(nullv()))));
			} else {
				caseStatements.append(es(apply(
						PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_FINISH,
						args(createOriginalCall(method, args)))));
			} 
			caseStatements.append(break_());
			String constantName = PaniniConstants.PANINI_METHOD_CONST
					+ method.name;
			if (method.params.nonEmpty())
				for (JCVariableDecl param : method.params) {
					constantName = constantName + "$"
							+ param.vartype;
				}
			cases.append(case_(id(constantName), caseStatements));
		}

		ListBuffer<JCStatement> shutDownBody = createShutdownLogic();
		cases.append(case_(intlit(-1), shutDownBody));

		ListBuffer<JCStatement> exitBody = createTerminationLogic();
		cases.append(case_(intlit(-2), exitBody));

		messageLoopBody.append(swtch(
				apply(PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_MESSAGE_ID), cases));

		ListBuffer<JCStatement> blockStats = new ListBuffer<JCStatement>();
		blockStats = createCapsuleMemberDisconnects(
		        tree.sym.capsule_info.connectedCapsules);
		
		JCBlock b = body(
				make.Try(body(
						//Call capsule Wiring
						make.Exec(createSimpleMethodCall(names.panini.InternalCapsuleWiring)),
						//Call capsule Initialize
						make.Exec(createSimpleMethodCall(names.panini.PaniniCapsuleInit)),
						var(mods(0), PaniniConstants.PANINI_TERMINATE,
								make.TypeIdent(TypeTags.BOOLEAN), falsev()),
						whilel(nott(id(PaniniConstants.PANINI_TERMINATE)),
								body(messageLoopBody))), List.<JCCatch> nil(), body(blockStats)));
		return b;
	}
	
	private ListBuffer<JCStatement> createCapsuleMemberDisconnects(
			List<JCVariableDecl> params) {
		ListBuffer<JCStatement> blockStats = new ListBuffer<JCStatement>();
		for (JCVariableDecl vdecl : params) {
			if (vdecl.vartype.type.tsym.isCapsule()) {
				JCStatement stmt = make
						.Exec(make.Apply(
								List.<JCExpression> nil(),
								make.Select(
										make.TypeCast(
												make.Ident(names
														.fromString(PaniniConstants.PANINI_QUEUE)),
												make.Ident(vdecl.name)),
										names.fromString(PaniniConstants.PANINI_DISCONNECT)),
								List.<JCExpression> nil()));

				blockStats.append(stmt);
			} else if (vdecl.vartype.type.tsym.name.toString().equalsIgnoreCase("Array")) {
				if (((ArrayType)vdecl.vartype.type).elemtype.tsym.isCapsule()) {
					ListBuffer<JCStatement> loopBody = new ListBuffer<JCStatement>();
			        JCVariableDecl arraycache = make.VarDef(make.Modifiers(0),
			                names.fromString("index$"),
			                make.TypeIdent(INT),
			                make.Literal(0));
			        JCBinary cond = make.Binary(LT, make.Ident(names.fromString("index$")),
			                make.Select(make.Ident(vdecl.name),
			                        names.fromString("length")));
			        JCUnary unary = make.Unary(PREINC, make.Ident(names.fromString("index$")));
			        JCExpressionStatement step =
			                make.Exec(unary);
			        loopBody.add(make
							.Exec(make.Apply(
									List.<JCExpression> nil(),
									make.Select(
											make.TypeCast(
													make.Ident(names
															.fromString(PaniniConstants.PANINI_QUEUE)),
															make.Indexed(make.Ident(vdecl.name), 
																	make.Ident(names.fromString("index$")))),
											names.fromString(PaniniConstants.PANINI_DISCONNECT)),
									List.<JCExpression> nil())));
			        JCForLoop floop =
			                make.ForLoop(List.<JCStatement>of(arraycache),
			                        cond,
			                        List.of(step),
			                        make.Block(0, loopBody.toList()));
			        blockStats.append(floop);
				}
			}
		}
		return blockStats;
	}

	protected JCBlock generateTaskCapsuleComputeMethodBody(JCCapsuleDecl tree) {
		JCModifiers noMods = mods(0);
		ListBuffer<JCStatement> messageLoopBody = new ListBuffer<JCStatement>();

		ListBuffer<JCCase> cases = new ListBuffer<JCCase>();
		int varIndex = 0;

		for (JCMethodDecl method : tree.publicMethods) {
			ListBuffer<JCStatement> caseStatements = new ListBuffer<JCStatement>();
			ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
			for (List<JCVariableDecl> l = method.params; l.nonEmpty(); l = l.tail) {
				JCVariableDecl vdecl = l.head;
				JCExpression varType = vdecl.vartype;
				if(method.restype.type.tag == TypeTags.ARRAY)
					caseStatements.append(var(
							noMods,
							"var" + varIndex,
							varType,
							cast(varType,
									select(cast(PaniniConstants.DUCK_INTERFACE_NAME
											+ "$" + PaniniConstants.ARRAY_DUCKS + "$"
											+ tree.name,
											id(PaniniConstants.PANINI_DUCK_TYPE)),
											createFieldString(
													method.name.toString(),
													varType.toString(),
													vdecl.name.toString(),
													method.params)))));
				else
					caseStatements.append(var(
							noMods,
							"var" + varIndex,
							varType,
							cast(varType,
									select(cast(PaniniConstants.DUCK_INTERFACE_NAME
											+ "$" + method.restype + "$"
											+ tree.name,
											id(PaniniConstants.PANINI_DUCK_TYPE)),
											createFieldString(
													method.name.toString(),
													varType.toString(),
													vdecl.name.toString(),
													method.params)))));
				args.append(id("var" + varIndex++));
			}

			Type returnType = ((MethodType) method.sym.type).restype;
			if (returnType.tag == TypeTags.VOID) {
				caseStatements.append(es(createOriginalCall(method, args)));
				caseStatements.append(es(apply(
						PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_FINISH, args(nullv()))));
			} else {
				caseStatements.append(make.Exec(make.Apply(List
						.<JCExpression> nil(), make.Select(make.Select(
						make.Ident(names.fromString("System")),
						names.fromString("out")), names.fromString("println")),
						List.<JCExpression> of(stringc("got it")))));
				caseStatements.append(es(apply(
						PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_FINISH,
						args(createOriginalCall(method, args)))));
			}
			caseStatements.append(returnt(falsev()));
			String constantName = PaniniConstants.PANINI_METHOD_CONST
					+ method.name;
			if (method.params.nonEmpty())
				for (List<JCVariableDecl> l = method.params; l.nonEmpty(); l = l.tail){
					JCVariableDecl param = l.head;
					constantName = constantName + "$"
							+ param.vartype;
				}
			cases.append(case_(id(constantName), caseStatements));
		}

		cases.append(case_(
				intlit(-1),
				ifs(gt(select(thist(), PaniniConstants.PANINI_CAPSULE_SIZE),
						intlit(0)),
						body(es(make
								.Apply(List.<JCExpression> nil(),
										id(PaniniConstants.PANINI_PUSH),
										List.<JCExpression> of(id(PaniniConstants.PANINI_DUCK_TYPE)))),
								returnt(falsev())))));

		ListBuffer<JCStatement> blockStats = new ListBuffer<JCStatement>();
		blockStats = createCapsuleMemberDisconnects(
		            tree.sym.capsule_info.connectedCapsules);

		blockStats.append(es(assign(PaniniConstants.PANINI_TERMINATE, truev())));
		blockStats.append(returnt(truev()));
		cases.append(case_(intlit(-2), blockStats));

		messageLoopBody.append(swtch(
				apply(PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.PANINI_MESSAGE_ID), cases));

		JCBlock b = body(
				var(mods(0), PaniniConstants.PANINI_TERMINATE,
						make.TypeIdent(TypeTags.BOOLEAN), falsev()),
				var(noMods, PaniniConstants.PANINI_DUCK_TYPE,
						PaniniConstants.DUCK_INTERFACE_NAME,
						apply(PaniniConstants.PANINI_GET_NEXT_DUCK)));
		b.stats = b.stats.appendList(messageLoopBody);
		b.stats = b.stats.append(returnt(falsev()));
		return b;
	}

	private JCMethodInvocation createOriginalCall(final JCMethodDecl method,
			final ListBuffer<JCExpression> args) {
		TreeCopier<Void> tc = new TreeCopier<Void>(make);
		return apply(thist(), method.name + "$Original", tc.copy(args.toList()));
	}

	/** Create a simple method invocation for the method name.
	 * @param methodName
	 */
	protected JCMethodInvocation createSimpleMethodCall(final Name methodName) {
	    return make.Apply( List.<JCExpression> nil(),
							make.Ident(methodName),
							List.<JCExpression> nil());
	}

	private ListBuffer<JCStatement> createShutdownLogic() {
		ListBuffer<JCStatement> shutDownBody = new ListBuffer<JCStatement>();
		shutDownBody
				.append(ifs(
						gt(select(thist(), PaniniConstants.PANINI_CAPSULE_SIZE),
								intlit(0)),
						body(es(make
								.Apply(List.<JCExpression> nil(),
										id(PaniniConstants.PANINI_PUSH),
										List.<JCExpression> of(id(PaniniConstants.PANINI_DUCK_TYPE)))),
								break_())));
		return shutDownBody;
	}

	private ListBuffer<JCStatement> createTerminationLogic() {
		ListBuffer<JCStatement> exitBody = new ListBuffer<JCStatement>();
		exitBody.append(es(assign(PaniniConstants.PANINI_TERMINATE, truev())));
		exitBody.append(break_());
		return exitBody;
	}

	public List<JCClassDecl> generateClassWrappers(JCCapsuleDecl tree,
			Env<AttrContext> env) {
		ListBuffer<JCClassDecl> classes = new ListBuffer<JCClassDecl>();
		Map<String, JCClassDecl> alreadedAddedDuckClasses = new HashMap<String, JCClassDecl>();

		for (List<JCMethodDecl> l = tree.publicMethods; l.nonEmpty(); l = l.tail){
			JCMethodDecl method = l.head;
			ListBuffer<JCTree> constructors = new ListBuffer<JCTree>();

			Type restype = ((MethodType) method.sym.type).restype;
			ClassSymbol c = checkAndResolveReturnType(env, rs, restype);

			if(c!=null && !c.toString().equals("java.lang.String")){
				Iterator<Symbol> iter = c.members().getElements().iterator();
				if (restype.tag == TypeTags.CLASS) {
					if (!alreadedAddedDuckClasses.containsKey(restype.toString())) {
						JCClassDecl duckClass = generateNewDuckClass(
								tree.name.toString(), method, constructors,
								restype, iter, c, env);
						classes.add(duckClass);
						alreadedAddedDuckClasses.put(restype.toString(), duckClass);
					} else {
						if (!method.params.isEmpty()) {
							JCClassDecl duckClass = alreadedAddedDuckClasses
									.get(restype.toString());
							adaptDuckClass(method, iter, duckClass, false);
						}
					}
				} else if (restype.toString().equals("void")) {
					if (!alreadedAddedDuckClasses.containsKey(restype.toString())) {
						List<JCExpression> implement;
						ListBuffer<JCTree> wrappedMethods = new ListBuffer<JCTree>();
	
						JCVariableDecl fieldMessageId = var(mods(PRIVATE | FINAL),
								PaniniConstants.PANINI_MESSAGE_ID,
								make.TypeIdent(TypeTags.INT));
						JCVariableDecl fieldRedeemed = var(mods(PRIVATE),
								PaniniConstants.REDEEMED,
								make.TypeIdent(TypeTags.BOOLEAN),
								make.Literal(TypeTags.BOOLEAN, 0));
	
						wrappedMethods
								.add(constructor(
										mods(PUBLIC),
										params(var(mods(0),
												PaniniConstants.PANINI_MESSAGE_ID,
												make.TypeIdent(TypeTags.INT))),
										body(es(supert()),
												es(assign(
														select(thist(),
																PaniniConstants.PANINI_MESSAGE_ID),
														id(PaniniConstants.PANINI_MESSAGE_ID))))));
	
						wrappedMethods.add(createValueMethod());
	
						// Code to be generated for the flag logic.
						// public final void panini$finish(Void t) {
						// synchronized (this) {
						// redeemed = true;
						// notifyAll();
						// }
						// }
						JCMethodDecl paniniFinish;
						paniniFinish = method(
								mods(PUBLIC | FINAL),
								PaniniConstants.PANINI_FINISH,
								voidt(),
								params(var(mods(0), "t", id("Void"))),
								body(sync(
										thist(),
										// The duck is ready.
										body(es(assign(PaniniConstants.REDEEMED,
												truev())), es(apply("notifyAll"))))));
	
						wrappedMethods.add(this.createPaniniMessageID());
						wrappedMethods.add(createVoidFutureGetMethod());
						implement = implementing(
								ta(id(PaniniConstants.DUCK_INTERFACE_NAME),
										args(id("Void")))).toList();
	
						ListBuffer<JCTree> variableFields = new ListBuffer<JCTree>();
	
						if (!method.params.isEmpty()) {
							ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
							ListBuffer<JCVariableDecl> consParams = new ListBuffer<JCVariableDecl>();
							consParams.add(var(mods(0),
									PaniniConstants.PANINI_MESSAGE_ID,
									make.TypeIdent(TypeTags.INT)));
							consBody.add(es(assign(
									select(thist(),
											PaniniConstants.PANINI_MESSAGE_ID),
									id(PaniniConstants.PANINI_MESSAGE_ID))));

							for (List<JCVariableDecl> vl = method.params; vl
									.nonEmpty(); vl = vl.tail) {
								JCVariableDecl par = vl.head;
								consParams.add(var(mods(0), par.name,
										par.vartype));
								consBody.add(es(assign(
										select(thist(),
												createFieldString(method.name,
														par, method.params)),
										id(par.name))));
								variableFields
										.add(var(mods(PUBLIC), names
												.fromString(createFieldString(
														method.name, par,
														method.params)),
												par.vartype));
								if (!par.vartype.type.isPrimitive())
									paniniFinish.body.stats = paniniFinish.body.stats
											.append(es(assign(
													select(thist(),
															createFieldString(
																	method.name,
																	par,
																	method.params)),
													nullv())));
							}
							constructors.add(constructor(mods(PUBLIC),
									consParams, body(consBody)));
						}
						wrappedMethods.add(paniniFinish);

						JCClassDecl wrappedClass = make
								.ClassDef(
										mods(0),
										names.fromString(PaniniConstants.DUCK_INTERFACE_NAME
												+ "$"
												+ restype
												+ "$"
												+ tree.name), List
												.<JCTypeParameter> nil(), null,
										implement,
										defs(fieldMessageId, fieldRedeemed)
												.appendList(variableFields)
												.appendList(wrappedMethods)
												.appendList(constructors)
												.toList());

						classes.add(wrappedClass);
						alreadedAddedDuckClasses.put(restype.toString(),
								wrappedClass);
					} else {
						if (!method.params.isEmpty()) {
							JCClassDecl duckClass = alreadedAddedDuckClasses
									.get(restype.toString());
							adaptDuckClass(method, iter, duckClass, false);
						}
					}
				} else if (restype.tag == TypeTags.ARRAY){
					if (!alreadedAddedDuckClasses.containsKey(PaniniConstants.ARRAY_DUCKS)) {
						JCClassDecl duckClass = generateNewDuckClass(
								tree.name.toString(), method, constructors,
								restype, iter, c, env);
						classes.add(duckClass);
						alreadedAddedDuckClasses.put(PaniniConstants.ARRAY_DUCKS, duckClass);
					} else {
						if (!method.params.isEmpty()) {
							JCClassDecl duckClass = alreadedAddedDuckClasses
									.get(restype.toString());
							adaptDuckClass(method, iter, duckClass, false);
						}
					}
				}
			}else{
				if(restype.isPrimitive()||c.toString().equals("java.lang.String")){
					if (!alreadedAddedDuckClasses.containsKey(restype.toString())) {
						ListBuffer<JCTree> wrappedMethods = new ListBuffer<JCTree>();
	
						JCVariableDecl fieldMessageId = var(mods(PRIVATE | FINAL),
								PaniniConstants.PANINI_MESSAGE_ID,
								make.TypeIdent(TypeTags.INT));
						JCVariableDecl fieldRedeemed = var(mods(PRIVATE),
								PaniniConstants.REDEEMED,
								make.TypeIdent(TypeTags.BOOLEAN),
								make.Literal(TypeTags.BOOLEAN, 0));
						
						JCMethodDecl paniniFinish;
						paniniFinish = method(
								mods(PUBLIC | FINAL),
								PaniniConstants.PANINI_FINISH,
								voidt(),
								params(var(mods(0), "t", id("Void"))),
								body(sync(
										thist(),
										// The duck is ready.
										body(es(assign(PaniniConstants.REDEEMED,
												truev())), es(apply("notifyAll"))))));
	
						wrappedMethods.add(this.createPaniniMessageID());
	
						ListBuffer<JCTree> variableFields = new ListBuffer<JCTree>();
	
						
						ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
						ListBuffer<JCVariableDecl> consParams = new ListBuffer<JCVariableDecl>();
						consParams.add(var(mods(0),
								PaniniConstants.PANINI_MESSAGE_ID,
								make.TypeIdent(TypeTags.INT)));
						consBody.add(es(supert(args(id(PaniniConstants.PANINI_MESSAGE_ID)))));
						consBody.add(es(assign(
								select(thist(),
										PaniniConstants.PANINI_MESSAGE_ID),
								id(PaniniConstants.PANINI_MESSAGE_ID))));
						if (!method.params.isEmpty()) {
							for (List<JCVariableDecl> vl = method.params; vl
									.nonEmpty(); vl = vl.tail) {
								JCVariableDecl par = vl.head;
								consParams.add(var(mods(0), par.name,
										par.vartype));
								consBody.add(es(assign(
										select(thist(),
												createFieldString(method.name,
														par, method.params)),
										id(par.name))));
								variableFields
										.add(var(mods(PUBLIC), names
												.fromString(createFieldString(
														method.name, par,
														method.params)),
												par.vartype));
								if (!par.vartype.type.isPrimitive())
									paniniFinish.body.stats = paniniFinish.body.stats
											.append(es(assign(
													select(thist(),
															createFieldString(
																	method.name,
																	par,
																	method.params)),
													nullv())));
							}
						}
							constructors.add(constructor(mods(PUBLIC), consParams,
									body(consBody)));
						wrappedMethods.add(paniniFinish);
						String restypeString = restype.toString().equals(
								"java.lang.String") ? "String" : restype
								.toString();

						JCClassDecl wrappedClass = make
								.ClassDef(
										mods(0),
										names.fromString(PaniniConstants.DUCK_INTERFACE_NAME
												+ "$"
												+ restypeString
												+ "$"
												+ tree.name), List
												.<JCTypeParameter> nil(), select(PrimitiveClass.get(restypeString).toString()),
												List
												.<JCExpression> nil(),
										defs(fieldMessageId, fieldRedeemed)
												.appendList(variableFields)
												.appendList(wrappedMethods)
												.appendList(constructors).toList());
	
						classes.add(wrappedClass);
						alreadedAddedDuckClasses.put(restype.toString(),
								wrappedClass);
					} else {
						JCClassDecl duckClass = alreadedAddedDuckClasses
								.get(restype.toString());
							adaptDuckClass(method, null, duckClass, true);
					}
				}
			}
		}
		return classes.toList();
	}
	
	public enum PrimitiveClass implements Serializable {
	    LONG,BOOLEAN,BYTE,CHAR,DOUBLE,FLOAT,INT,SHORT,STRING;

	    public String toString(){
	        switch(this){
	        case LONG :
	            return "org.paninij.lang.Long";
	        case BOOLEAN :
	            return "org.paninij.lang.Boolean";
	        case CHAR :
	            return "org.paninij.lang.Character";
	        case DOUBLE :
	            return "org.paninij.lang.Double";
	        case FLOAT :
	            return "org.paninij.lang.Float";
	        case INT :
	            return "org.paninij.lang.Integer";
	        case SHORT :
	            return "org.paninij.lang.Short";
	        case STRING :
	            return "org.paninij.lang.String";
	        case BYTE :
	            return "org.paninij.lang.Byte";
	        }
	        return null;
	    }
	    
	    public static PrimitiveClass get(String value){
	        if(value.equalsIgnoreCase("long"))
	            return PrimitiveClass.LONG;
	        else if(value.equals("boolean"))
	            return PrimitiveClass.BOOLEAN;
	        else if(value.equals("char"))
	            return PrimitiveClass.CHAR;
	        else if(value.equals("double"))
	            return PrimitiveClass.DOUBLE;
	        else if(value.equals("float"))
	            return PrimitiveClass.FLOAT;
	        else if(value.equals("int"))
	            return PrimitiveClass.INT;
	        else if(value.equals("short"))
	            return PrimitiveClass.SHORT;
	        else if(value.equals("String"))
	            return PrimitiveClass.STRING;
	        else if(value.equals("byte"))
	            return PrimitiveClass.BYTE;
	        else
	            return null;
	    }
	}
	
	private JCMethodDecl createDuckConstructor(Iterator<Symbol> iter, final boolean isVoid, final boolean isPrimitive){
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
		MethodSymbol constructor = null;
		List<Type> thrownTypes = List.<Type>nil();
		if(!isVoid){
			while (iter.hasNext()) {
				Symbol s = iter.next();
				if (s.getKind() == ElementKind.CONSTRUCTOR) {
					MethodSymbol m = (MethodSymbol) s;
					if (m.isStatic() || ((m.flags() & PRIVATE) != 0))
						continue; // Is this correct?
					if (constructor == null){
						constructor = m;
						thrownTypes = m.getThrownTypes();
					}
					else if (m.params().length() < constructor.params().length()){
						constructor = m;
						thrownTypes = m.getThrownTypes();
					}
				}
			}
			if (constructor != null)
				for (List<VarSymbol> l = constructor.params(); l.nonEmpty(); l = l.tail){
					VarSymbol v = l.head;
					if (v.type.toString().equals("boolean"))
						args.add(falsev());
					else if (v.type.isPrimitive())
						args.add(intlit(0));
					else{
						args.add(make.TypeCast(make.Type(v.type), nullv()));
					}
				}
		}

		List<JCExpression> thrown = make.Types(thrownTypes);
		ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
		if(isPrimitive){
			args.add(id(PaniniConstants.PANINI_MESSAGE_ID));
			consBody.add(es(make.Apply(List.<JCExpression> nil(),
					id(names._super), args.toList())));
		}else
			consBody.add(es(make.Apply(List.<JCExpression> nil(),
					id(names._super), args.toList())));
		consBody.add(es(assign(
				select(thist(), PaniniConstants.PANINI_MESSAGE_ID),
				id(PaniniConstants.PANINI_MESSAGE_ID))));
		return make.MethodDef(mods(PUBLIC), 
				names.init,
				null,
				List.<JCTypeParameter>nil(),
				params(var(mods(0), PaniniConstants.PANINI_MESSAGE_ID,
				make.TypeIdent(TypeTags.INT))).toList(),
				thrown,
				body(consBody),
				null
				);
	}
	
	private JCMethodDecl createDuckConstructor(Iterator<Symbol> iter, boolean isVoid, JCMethodDecl method, JCMethodDecl paniniFinish, final boolean isPrimitive){
		JCMethodDecl constructor = createDuckConstructor(iter, isVoid, isPrimitive);
		for (List<JCVariableDecl> l = method.params; l.nonEmpty(); l = l.tail){
			JCVariableDecl par = l.head;
			constructor.params = constructor.params.append(var(mods(0), par.name, par.vartype));
			constructor.body.stats = constructor.body.stats.append(es(assign(
					select(thist(),
							createFieldString(method.name, par,
									method.params)), id(par.name))));
			if (!par.vartype.type.isPrimitive())
				paniniFinish.body.stats = paniniFinish.body.stats
						.append(es(assign(
								select(thist(),
										createFieldString(method.name, par,
												method.params)), nullv())));
		}
		return constructor;
	}

	private ListBuffer<JCExpression> superArgs(Iterator<Symbol> iter) {
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
		MethodSymbol constructor = null;
		while (iter.hasNext()) {
			Symbol s = iter.next();
			if (s.getKind() == ElementKind.CONSTRUCTOR) {
				MethodSymbol m = (MethodSymbol) s;
				if (m.isStatic() || ((m.flags() & PRIVATE) != 0))
					continue; // Is this correct?
				if (constructor == null)
					constructor = m;
				else if (m.params().length() < constructor.params().length())
					constructor = m;
			}
		}
		if (constructor != null)
			for (List<VarSymbol> l = constructor.params(); l.nonEmpty(); l = l.tail){
				VarSymbol v = l.head;
				if (v.type.toString().equals("boolean"))
					args.add(falsev());
				else if (v.type.isPrimitive())
					args.add(intlit(0));
				else
					args.add(make.TypeCast(make.Ident(v.type.tsym), nullv()));
			}
		return args;
	}

	private void adaptDuckClass(JCMethodDecl method, Iterator<Symbol> iter,
			JCClassDecl duckClass, final boolean isPrimitive) {
		ListBuffer<JCTree> newFields = new ListBuffer<JCTree>();
		JCMethodDecl paniniFinish = null;
		for (List<JCTree> l = duckClass.defs; l.nonEmpty(); l=l.tail){
			JCTree def = l.head;
			if (def.hasTag(Tag.METHODDEF))
				if (((JCMethodDecl) def).name.equals(names.panini.PaniniFinish))
					paniniFinish = (JCMethodDecl) def;
		}
		if (!hasDuplicate(duckClass, method.params, method.name)) {
			if (paniniFinish == null)
				Assert.error();// ///shouldn't happen
			if (iter == null)
				duckClass.defs = duckClass.defs.append(createDuckConstructor(
						iter, true, method, paniniFinish, isPrimitive));
			else
				duckClass.defs = duckClass.defs.append(createDuckConstructor(
						iter, method.restype.toString().equals("void"), method,
						paniniFinish, isPrimitive));
		}
		for (List<JCVariableDecl> l = method.params; l.nonEmpty(); l=l.tail){
			JCVariableDecl par = l.head;
			newFields.add(var(mods(PUBLIC), names.fromString(createFieldString(
					method.name, par, method.params)), par.vartype));
		}
		duckClass.defs = duckClass.defs.appendList(newFields);
	}

	private String trim(String fullName) {
		int index = -1;
		int openParamIndex = fullName.indexOf("<");
		String types = "";
		String rawClassName;
		while (fullName.indexOf(".", index + 1) != -1) {
			if (openParamIndex != -1
					&& fullName.indexOf(".", index + 1) > openParamIndex)
				break;
			index = fullName.indexOf(".", index + 1);
		}
		if (openParamIndex != -1) {
			types = trim(fullName.substring(openParamIndex + 1,
					fullName.indexOf(">")));
			rawClassName = fullName.toString().substring(index + 1,
					openParamIndex)
					+ "<" + types + ">";
		} else
			rawClassName = fullName.toString().substring(index + 1);
		return rawClassName;
	}

	private JCExpression signatureType(ListBuffer<JCExpression> typaram,
			ClassSymbol c) {
		if (typaram.isEmpty())
			return make.Ident(c);
		else
			return make.TypeApply(make.Ident(c), typaram.toList());
	}

	private JCClassDecl generateNewDuckClass(String classNameSuffix,
			JCMethodDecl method, ListBuffer<JCTree> constructors, Type restype,
			Iterator<Symbol> iter, ClassSymbol c, Env<AttrContext> env) {
		String rawClassName = trim(restype.toString());
		if (restype.tag == TypeTags.ARRAY)
			rawClassName = PaniniConstants.ARRAY_DUCKS;
		ListBuffer<JCTypeParameter> typeParams = new ListBuffer<JCTypeParameter>();
		for (List<TypeSymbol> l = c.getTypeParameters(); l.nonEmpty(); l=l.tail){
			TypeSymbol ts = l.head;
			typeParams.append(make.TypeParam(ts.name, (TypeVar) ts.type));
		}
		ListBuffer<JCExpression> typeExpressions = new ListBuffer<JCExpression>();
		for (List<JCTypeParameter> l = typeParams.toList(); l.nonEmpty(); l=l.tail){
			JCTypeParameter tp = l.head;
			typeExpressions.add(make.Ident(tp.name));
		}
		JCVariableDecl fieldWrapped;
		if (restype.tag == TypeTags.ARRAY)
			fieldWrapped = var(mods(PRIVATE),
					PaniniConstants.PANINI_WRAPPED,
					id("Object"), nullv());
		else fieldWrapped = var(mods(PRIVATE),
				PaniniConstants.PANINI_WRAPPED,
				signatureType(typeExpressions, c), nullv());
		JCVariableDecl fieldMessageId = var(mods(PRIVATE | FINAL),
				PaniniConstants.PANINI_MESSAGE_ID,
				make.TypeIdent(TypeTags.INT), null);
		JCVariableDecl fieldRedeemed = var(mods(PRIVATE),
				PaniniConstants.REDEEMED, make.TypeIdent(TypeTags.BOOLEAN),
				make.Literal(TypeTags.BOOLEAN, 0));

		ListBuffer<JCTree> wrappedMethods = new ListBuffer<JCTree>();
		ListBuffer<JCExpression> inits = new ListBuffer<JCExpression>();
		boolean providesHashCode = false;
		boolean providesEquals = false;
		if (restype.tag != TypeTags.ARRAY){
			ClassSymbol cs = c;
			Set<String> methods = new HashSet<String>();
			for(Type type = restype; type != Type.noType && !type.toString().equals(names.java_lang_Object.toString()); type = cs.getSuperclass()){
				cs = checkAndResolveReturnType(env, rs, type);
				Iterator<Symbol> iterator = cs.members().getElements().iterator();
				while (iterator.hasNext()) {
					Symbol s = iterator.next();
					if (s.getKind() == ElementKind.METHOD) {
						MethodSymbol m = (MethodSymbol) s;
						JCMethodDecl value;
						if(c.packge()!=env.enclClass.sym.packge())
							if (m.isStatic() || ((m.flags() & PUBLIC) == 0)
									|| ((m.flags() & SYNTHETIC) != 0))
								continue; // Do not wrap static methods.
						if (m.isStatic() || ((m.flags() & PROTECTED) != 0) ||((m.flags() & PRIVATE) != 0)
								|| ((m.flags() & SYNTHETIC) != 0))
							continue; // Do not wrap static methods.
						if (!m.type.getReturnType().toString().equals("void")) {
							value = createFutureValueMethod(m, m.name);
						} else {
							value = createVoidFutureValueMethod(m, m.name);
						}
						
						if(methods.add(m.toString()))
							wrappedMethods.add(value);
						if(!providesHashCode && m.name.contentEquals("hashCode") && m.getParameters().length() == 0)
							providesHashCode = true;
						if(!providesEquals && m.name.contentEquals("equals") && m.getParameters().length() == 1) {
							providesEquals = true;
						}
					}
				}
			}
		}
		iter = c.members().getElements().iterator();
		constructors.add(createDuckConstructor(iter, false, false));

		JCMethodDecl messageIdMethod = createPaniniMessageID();
		JCMethodDecl finishMethod = createPaniniFinishMethod(c);

		JCExpression extending;
		List<JCExpression> implement;
		if (restype.isInterface()) {
			extending = null;
			implement = implementing(
					ta(id(PaniniConstants.DUCK_INTERFACE_NAME),
							args(id(restype.toString()))),
					id(restype.toString())).toList();
		} else if (restype.tag == TypeTags.ARRAY){
			extending = id("Panini$Duck$Array$Types");
			implement = List.<JCExpression>nil();
		} else if ((c.flags() & Flags.FINAL) !=0){
			extending = ta(id("Panini$Duck$Final"),
					args(signatureType(typeExpressions, c)));
			implement = List.<JCExpression>nil();
		} else {
			JCMethodDecl get;
			if (restype.toString().equals("void")) {
				extending = id(PaniniConstants.DUCK_INTERFACE_NAME + "$Void");
				implement = implementing(
						ta(id(PaniniConstants.DUCK_INTERFACE_NAME),
								args(id("Void")))).toList();
				get = createVoidFutureGetMethod();
				wrappedMethods.add(get);
			} else {
				extending = signatureType(typeExpressions, c);
				implement = implementing(
						ta(id(PaniniConstants.DUCK_INTERFACE_NAME),
								args(signatureType(typeExpressions, c))))
						.toList();
				get = createFutureGetMethod(c);
				wrappedMethods.add(get);
			}
		}

		ListBuffer<JCTree> variableFields = new ListBuffer<JCTree>();

		if (!method.params.isEmpty()) {
			for (List<JCVariableDecl> l = method.params; l.nonEmpty(); l=l.tail){
				JCVariableDecl par = l.head;
				variableFields.add(var(mods(PUBLIC), names
						.fromString(createFieldString(method.name, par,
								method.params)), par.vartype));
			}
			iter = c.members().getElements().iterator();
			constructors.add(createDuckConstructor(iter, false, method, finishMethod, false));
		}
		
		ListBuffer<JCTree> wrapperMembers;
		if (restype.tag == TypeTags.ARRAY || (c.flags() & Flags.FINAL) !=0)
			wrapperMembers = defs(fieldWrapped, fieldMessageId, fieldRedeemed); 
		else wrapperMembers = defs(fieldWrapped, fieldMessageId, fieldRedeemed, finishMethod, messageIdMethod); 
		if(!providesHashCode) wrapperMembers.append(createHashCode());
		if(!providesEquals) wrapperMembers.append(createEquals());

		JCClassDecl wrappedClass = make.ClassDef(
				mods(FINAL),
				names.fromString(PaniniConstants.DUCK_INTERFACE_NAME + "$"
						+ rawClassName + "$" + classNameSuffix),
				typeParams.toList(),
				extending,
				implement,
				wrapperMembers.appendList(variableFields).appendList(constructors)
				.appendList(wrappedMethods).toList());

		return wrappedClass;
	}

	private JCMethodDecl createPaniniFinishMethod(ClassSymbol restype) {
		ListBuffer<JCVariableDecl> finishParams = new ListBuffer<JCVariableDecl>();
		if(restype.toString().equals("Array"))
			finishParams.add(var(mods(0), "t", id("Object")));
		else
			finishParams.add(var(mods(0), "t", make.Ident(restype)));
		JCMethodDecl finishMethod = method(
				mods(PUBLIC | FINAL),
				names.panini.PaniniFinish,
				make.Type(syms.voidType),
				finishParams,
				body(sync(
						thist(),
						body(es(assign(PaniniConstants.PANINI_WRAPPED, id("t"))),
								es(assign(PaniniConstants.REDEEMED, truev())),
								es(apply("notifyAll"))))));
		return finishMethod;
	}

	private JCMethodDecl createPaniniMessageID() {
		return method(
				mods(PUBLIC | FINAL),
				names.fromString(PaniniConstants.PANINI_MESSAGE_ID),
				make.TypeIdent(TypeTags.INT),
				body(returnt(select(thist(), PaniniConstants.PANINI_MESSAGE_ID))));
	}

	private JCMethodDecl createHashCode() {
		return method(
				mods(PUBLIC | FINAL),
				names.fromString(PaniniConstants.PANINI_DUCK_HASHCODE),
				make.TypeIdent(TypeTags.INT),
				body(returnt(apply(
						apply(thist(), PaniniConstants.PANINI_DUCK_GET,
								new ListBuffer<JCExpression>()),
						PaniniConstants.PANINI_DUCK_HASHCODE,
						new ListBuffer<JCExpression>()))));
	}

	private JCMethodDecl createEquals() {
		ListBuffer<JCVariableDecl> equalsParams = new ListBuffer<JCVariableDecl>();
		equalsParams.add(var(mods(0), "o", "Object"));
		return method(
				mods(PUBLIC | FINAL),
				names.fromString(PaniniConstants.PANINI_DUCK_EQUALS),
				make.TypeIdent(TypeTags.BOOLEAN),
				equalsParams,
				body(ifs(
						isNull(apply(thist(), PaniniConstants.PANINI_DUCK_GET,
								new ListBuffer<JCExpression>())),
						returnt(isNull("o")),
						returnt(apply(
								select(thist(), PaniniConstants.PANINI_WRAPPED),
								PaniniConstants.PANINI_DUCK_EQUALS,
								args(id("o")))))));
	}

	private JCMethodDecl createDuckConstructor(ListBuffer<JCExpression> inits) {
		return constructor(
				mods(PUBLIC),
				params(var(mods(0), PaniniConstants.PANINI_MESSAGE_ID,
						make.TypeIdent(TypeTags.INT))),
				body(es(make.Apply(List.<JCExpression> nil(),
						make.Ident(names._super), inits.toList())),
						es(assign(
								select(thist(),
										PaniniConstants.PANINI_MESSAGE_ID),
								id(PaniniConstants.PANINI_MESSAGE_ID)))));
	}

	private JCMethodDecl createValueMethod() {
		return method(
				mods(PUBLIC | FINAL),
				"value",
				id("Void"),
				params(),
				body(ifs(isFalse(PaniniConstants.REDEEMED),
						es(apply(thist(), PaniniConstants.PANINI_DUCK_GET))),
						returnt(nullv())));
	}

	private JCMethodDecl createFutureValueMethod(MethodSymbol m,
			Name method_name) {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
		JCExpression restype = make.Type(m.type.getReturnType());

		ListBuffer<JCTypeParameter> tp = new ListBuffer<JCTypeParameter>();
		for (List<TypeSymbol> l = m.getTypeParameters(); l.nonEmpty(); l=l.tail){
			TypeSymbol ts = l.head;
			tp.appendList(make.TypeParams(List.<Type> of(ts.type)));
		}
		if (m.getParameters() != null) {
			for (List<VarSymbol> l = m.getParameters(); l.nonEmpty(); l = l.tail){
				VarSymbol v = l.head;
				params.add(make.VarDef(v, null));
				args.add(id(v.name));
			}
		}
		JCMethodDecl value = make.MethodDef(
				mods(PUBLIC | FINAL),
				method_name,
				restype,
				tp.toList(),
				params.toList(),
				make.Types(m.getThrownTypes()),
				body(ifs(isFalse(PaniniConstants.REDEEMED),
						es(apply(thist(), PaniniConstants.PANINI_DUCK_GET))),
						returnt(apply(PaniniConstants.PANINI_WRAPPED,
								method_name.toString(), args))), null);
		return value;
	}

	private JCMethodDecl createVoidFutureValueMethod(MethodSymbol m,
			Name method_name) {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
		ListBuffer<JCTypeParameter> tp = new ListBuffer<JCTypeParameter>();
		for (List<TypeSymbol> l = m.getTypeParameters(); l.nonEmpty(); l = l.tail){
			TypeSymbol ts = l.head;
			tp.appendList(make.TypeParams(List.<Type> of(ts.type)));
		}

		if (m.getParameters() != null) {
			for (List<VarSymbol> l = m.getParameters(); l.nonEmpty(); l = l.tail){
				VarSymbol v = l.head;
				params.add(make.VarDef(v, null));
				args.add(id(v.name));
			}
		}
		JCMethodDecl delegate = make.MethodDef(
				mods(PUBLIC | FINAL),
				method_name,
				make.Type(syms.voidType),
				tp.toList(),
				params.toList(),
				make.Types(m.getThrownTypes()),
				body(ifs(isFalse(PaniniConstants.REDEEMED),
						es(apply(thist(), PaniniConstants.PANINI_DUCK_GET))),
						es(apply(PaniniConstants.PANINI_WRAPPED,
								method_name.toString(), args))), null);
		return delegate;
	}

	private JCMethodDecl createFutureGetMethod(ClassSymbol restype) {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();

		List<JCCatch> catchers = List.<JCCatch> of(make.Catch(make.VarDef(
				make.Modifiers(0), names.fromString("e"),
				make.Ident(names.fromString("InterruptedException")), null),
				make.Block(0, List.<JCStatement> nil())));
		JCMethodDecl value = method(
				mods(PUBLIC | FINAL),
				PaniniConstants.PANINI_DUCK_GET,
				make.Ident(restype),
				params,
				body(whilel(isFalse(PaniniConstants.REDEEMED), make.Try(
						body(sync(
								make.This(Type.noType),
								body(whilel(isFalse(PaniniConstants.REDEEMED),
										es(apply("wait")))))), catchers, null)),
						returnt(PaniniConstants.PANINI_WRAPPED)));
		return value;
	}

	private JCMethodDecl createVoidFutureGetMethod() {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();

		List<JCCatch> catchers = List.<JCCatch> of(make.Catch(make.VarDef(
				make.Modifiers(0), names.fromString("e"),
				make.Ident(names.fromString("InterruptedException")), null),
				make.Block(0, List.<JCStatement> nil())));
		JCMethodDecl value = method(
				mods(PUBLIC | FINAL),
				PaniniConstants.PANINI_DUCK_GET,
				id("Void"),
				params,
				body(whilel(isFalse(PaniniConstants.REDEEMED), make.Try(
						body(sync(
								make.This(Type.noType),
								body(whilel(isFalse(PaniniConstants.REDEEMED),
										es(apply("wait")))))), catchers, null)),
						returnt(nullv())));
		return value;
	}

	
	
	public ClassSymbol checkAndResolveReturnType(Env<AttrContext> env,
			Resolve rs, Type restype) {
		ClassSymbol c;
		if (restype.toString().equals("void"))
			c = (ClassSymbol) rs.findIdent(
					env,
					names.fromString(PaniniConstants.DUCK_INTERFACE_NAME
							+ "$Void"), TYP);
		else if (restype.isPrimitive()) {
			return null;
		} else
			c = (ClassSymbol) restype.tsym;
		return c;
	}

	private boolean hasDuplicate(JCClassDecl c, List<JCVariableDecl> v,
			Name name) {
		boolean result = false;
		for (List<JCTree> l = c.defs; l.nonEmpty(); l = l.tail){
			JCTree def = l.head;
			if (def.getTag() == Tag.METHODDEF
					&& ((JCMethodDecl) def).name.equals(names.init)) {
				JCMethodDecl mdecl = (JCMethodDecl)def;
				if (mdecl.params.length() == v.length() + 1) {
					result = true;
					for (int i = 1; i < mdecl.params.length(); i++) {
						if (!mdecl.params.get(i).vartype
								.toString().equals(
										v.get(i - 1).vartype.toString())) {
							result = false;
							i = mdecl.params.length();
						}
					}
					if (result) {
						for (int i=0; i<v.length(); i++){
							mdecl.body.stats = mdecl.body.stats
									.append(es(assign(
											select(thist(),
													createFieldString(name,
															v.get(i), v)),
											id(mdecl.params.get(i+1).name.toString()))));
						}
						break;
					}
				}
			}
		}
		return result;
	}

	private String createFieldString(Name name, JCVariableDecl param,
			List<JCVariableDecl> params) {
		return createFieldString(name.toString(), param.vartype.toString(),
				param.name.toString(), params);
	}

	private String createFieldString(String name, String vartype,
			String paramName, List<JCVariableDecl> params) {
		String fieldName;
		fieldName = vartype + "$" + paramName + "$" + name;
		if (params.nonEmpty())
			for(List<JCVariableDecl> l = params; l.nonEmpty(); l=l.tail){
				JCVariableDecl v = l.head;
				fieldName = fieldName + "$" + v.vartype.toString();
			}
		return fieldName;
	}
}
