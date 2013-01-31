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

package com.sun.tools.javac.comp;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.PaniniConstants;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.tree.JCTree.Tag.GE;
import static com.sun.tools.javac.tree.JCTree.Tag.POSTINC;

public class CapsuleInternal extends Internal {
	ListBuffer<JCTree> contractDefs;
	Symtab syms;
	Enter enter;
	MemberEnter memberEnter;

	public CapsuleInternal(TreeMaker make, Names names, Enter enter,
			MemberEnter memberEnter, Symtab syms) {
		super(make, names);
		this.enter = enter;
		this.syms = syms;
		this.memberEnter = memberEnter;
		specCounter = 0;
		contractDefs = new ListBuffer<JCTree>();
	}

	public JCBlock generateThreadCapsuleComputeMethodBody(JCCapsuleDecl tree) {
		JCModifiers noMods = mods(0);
		ListBuffer<JCStatement> messageLoopBody = new ListBuffer<JCStatement>();
		messageLoopBody.append(var(noMods, PaniniConstants.PANINI_DUCK_TYPE, PaniniConstants.DUCK_INTERFACE_NAME,
				apply(PaniniConstants.PANINI_GET_NEXT_DUCK)));

		ListBuffer<JCCase> cases = new ListBuffer<JCCase>();
		int varIndex = 0;

		for (JCMethodDecl method : tree.publicMethods) {
			ListBuffer<JCStatement> caseStatements = new ListBuffer<JCStatement>();
			ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
			for (int i = 0; i < method.params.size(); i++) {
				JCExpression varType = method.params.get(i).vartype;
				caseStatements.append(var(
						noMods,
						"var" + varIndex,
						varType,
						cast(varType,
								select(
										cast(
												PaniniConstants.DUCK_INTERFACE_NAME + "$"
														+ method.restype.toString() + "$" + tree.name.toString(),
														id(PaniniConstants.PANINI_DUCK_TYPE)), createFieldString(method.name.toString(), varType.toString(), method.params.get(i).name.toString(), method.params)))));
				args.append(id("var" + varIndex++));
			}

			Type returnType = ((MethodType) method.sym.type).restype;
			if (returnType.tag == TypeTags.VOID) {
				caseStatements.append(es(createOriginalCall(method, args)));
				caseStatements.append(es(apply(PaniniConstants.PANINI_DUCK_TYPE, "panini$finish", args(nullv()))));
			} else if (returnType.tag == TypeTags.CLASS) {
				caseStatements.append(es(apply(PaniniConstants.PANINI_DUCK_TYPE, "panini$finish",
						args(createOriginalCall(method, args)))));
			} else {
				System.out.println("Unsupported return type in a public capsule method. Can only be void or non-primitive.");
				System.exit(5555);
			}
			caseStatements.append(break_());
			String constantName = PaniniConstants.PANINI_METHOD_CONST + method.name.toString();
			if(method.params.nonEmpty())
			for(JCVariableDecl param: method.params){
				constantName = constantName + "$" + param.vartype.toString();
			}
			cases.append(case_(
					id(constantName),
					caseStatements));
		}

		ListBuffer<JCStatement> shutDownBody = createShutdownLogic();
		cases.append(case_(intlit(-1), shutDownBody));

		ListBuffer<JCStatement> exitBody = createTerminationLogic();
		cases.append(case_(intlit(-2), exitBody));

		messageLoopBody.append(swtch(apply(PaniniConstants.PANINI_DUCK_TYPE, PaniniConstants.PANINI_MESSAGE_ID), cases));
		
		JCBlock b = body(
				var(mods(0), PaniniConstants.PANINI_TERMINATE,
						make.TypeIdent(TypeTags.BOOLEAN), falsev()),
				whilel(nott(id(PaniniConstants.PANINI_TERMINATE)), body(messageLoopBody)));
		return b;
	}

	public JCBlock generateTaskCapsuleComputeMethodBody(JCCapsuleDecl tree) {
		JCModifiers noMods = mods(0);
		ListBuffer<JCStatement> messageLoopBody = new ListBuffer<JCStatement>();
		

		ListBuffer<JCCase> cases = new ListBuffer<JCCase>();
		int varIndex = 0;

		for (JCMethodDecl method : tree.publicMethods) {
			ListBuffer<JCStatement> caseStatements = new ListBuffer<JCStatement>();
			ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
			for (int i = 0; i < method.params.size(); i++) {
				JCExpression varType = method.params.get(i).vartype;
				caseStatements.append(var(
						noMods,
						"var" + varIndex,
						varType,
						cast(varType,
								select(
										cast(
												PaniniConstants.DUCK_INTERFACE_NAME + "$"
														+ method.restype.toString() + "$" + tree.name.toString(),
														id(PaniniConstants.PANINI_DUCK_TYPE)), createFieldString(method.name.toString(), varType.toString(), method.params.get(i).name.toString(), method.params)))));
				args.append(id("var" + varIndex++));
			}

			Type returnType = ((MethodType) method.sym.type).restype;
			if (returnType.tag == TypeTags.VOID) {
				caseStatements.append(es(createOriginalCall(method, args)));
				caseStatements.append(es(apply(PaniniConstants.PANINI_DUCK_TYPE, "panini$finish", args(nullv()))));
			} else if (returnType.tag == TypeTags.CLASS) {
				caseStatements.append(es(apply(PaniniConstants.PANINI_DUCK_TYPE, "panini$finish",
						args(createOriginalCall(method, args)))));
			} else {
				System.out.println("Unsupported return type in a public capsule method. Can only be void or non-primitive.");
				System.exit(5555);
			}
			caseStatements.append(returnt(falsev()));
			String constantName = PaniniConstants.PANINI_METHOD_CONST + method.name.toString();
			if(method.params.nonEmpty())
			for(JCVariableDecl param: method.params){
				constantName = constantName + "$" + param.vartype.toString();
			}
			cases.append(case_(
					id(constantName),
					caseStatements));
		}

		cases.append(case_(intlit(-1), ifs(
				gt(select(thist(), "size"), intlit(0)),
				body(
						es(make.Apply(List.<JCExpression> nil(), id("push"),
								List.<JCExpression> of(id(PaniniConstants.PANINI_DUCK_TYPE)))), returnt(falsev())))));

		cases.append(case_(intlit(-2), es(assign(PaniniConstants.PANINI_TERMINATE, truev())),
		returnt(truev())));

		messageLoopBody.append(swtch(apply(PaniniConstants.PANINI_DUCK_TYPE, PaniniConstants.PANINI_MESSAGE_ID), cases));
		
		JCBlock b = body(
				var(mods(0), PaniniConstants.PANINI_TERMINATE,
						make.TypeIdent(TypeTags.BOOLEAN), falsev()),
						var(noMods, PaniniConstants.PANINI_DUCK_TYPE, PaniniConstants.DUCK_INTERFACE_NAME,
								apply(PaniniConstants.PANINI_GET_NEXT_DUCK))
				);
		b.stats = b.stats.appendList(messageLoopBody);
		b.stats = b.stats.append(returnt(falsev()));
		return b;
	}
	
	private JCMethodInvocation createOriginalCall (final JCMethodDecl method, final ListBuffer<JCExpression> args) {
		return apply(thist(), method.name.toString()	+ "$Original", args); 
	}
	
	private ListBuffer<JCStatement> createShutdownLogic() {
		ListBuffer<JCStatement> shutDownBody = new ListBuffer<JCStatement>();
		shutDownBody.append(ifs(
				gt(select(thist(), "size"), intlit(0)),
				body(
						es(make.Apply(List.<JCExpression> nil(), id("push"),
								List.<JCExpression> of(id(PaniniConstants.PANINI_DUCK_TYPE)))), break_())));
		return shutDownBody;
	}

	private ListBuffer<JCStatement> createTerminationLogic() {
		ListBuffer<JCStatement> exitBody = new ListBuffer<JCStatement>();
		exitBody.append(es(assign(PaniniConstants.PANINI_TERMINATE, truev())));
		exitBody.append(break_());
		return exitBody;
	}

	public List<JCClassDecl> generateClassWrappers(JCCapsuleDecl tree,
			Env<AttrContext> env, Resolve rs) {
		ListBuffer<JCClassDecl> classes = new ListBuffer<JCClassDecl>();
		Map<String, JCClassDecl> alreadedAddedDuckClasses = new HashMap<String, JCClassDecl>();

		for (JCMethodDecl method : tree.publicMethods) {
			ListBuffer<JCTree> constructors = new ListBuffer<JCTree>();

			Type restype = ((MethodType) method.sym.type).restype;
			ClassSymbol c = checkAndResolveReturnType(env, rs, restype);

			Iterator<Symbol> iter = c.members().getElements().iterator();
			if (restype.tag == TypeTags.CLASS) {
				if (!alreadedAddedDuckClasses.containsKey(restype.toString())) {
					JCClassDecl duckClass = generateNewDuckClass(tree.name.toString(), method, constructors, restype, iter);
					classes.add(duckClass);
					alreadedAddedDuckClasses.put(restype.toString(), duckClass);
				}
				else {
					if (!method.params.isEmpty()) {
						JCClassDecl duckClass = alreadedAddedDuckClasses.get(restype.toString());
						adaptDuckClass(method, iter, duckClass);
					}
				}
			} else if (restype.toString().equals("void")) {
				if (!alreadedAddedDuckClasses.containsKey(restype.toString())) {
					List<JCExpression> implement;
					ListBuffer<JCTree> wrappedMethods = new ListBuffer<JCTree>();

					JCVariableDecl fieldMessageId = var(mods(PRIVATE | FINAL), "messageId", make.TypeIdent(TypeTags.INT));
					JCVariableDecl fieldRedeemed = var(mods(PRIVATE), PaniniConstants.REDEEMED, make.TypeIdent(TypeTags.BOOLEAN), make.Literal(TypeTags.BOOLEAN, 0));

					wrappedMethods.add(constructor(
							mods(PUBLIC),
							params(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT))),
							body(es(supert()),
									es(assign(select(thist(), "messageId"), id("messageId"))))));

					// Code to be generated for the flag logic.
					// public final Void value() {
					// try {
					// synchronized (this) {
					// while (redeemed == false) wait(); // --- New
					// }
					// } catch (InterruptedException e) {
					// return value();
					// }
					// return null;
					// }
					List<JCCatch> catchers = List.<JCCatch> of(make.Catch(
							make.VarDef(make.Modifiers(0), names.fromString("e"),
									make.Ident(names.fromString("InterruptedException")), null),
							make.Block(
									0,
									List.<JCStatement> of(make.Return(make.Apply(null,
											make.Ident(names.fromString(PaniniConstants.VALUE)),
											List.<JCExpression> nil()))))));
					wrappedMethods.add(method(
							mods(PUBLIC | FINAL),
							"value",
							id("Void"),
							params(),
							body(make.Try(body(sync(make.This(Type.noType), body(whilel(
							// Test whether the duck is ready.
									isFalse(PaniniConstants.REDEEMED), es(apply("wait")))))), catchers,
									null), returnt(nullv()))));

					// Code to be generated for the flag logic.
					// public final void panini$finish(Void t) {
					// synchronized (this) {
					// redeemed = true;
					// notifyAll();
					// }
					// }
					wrappedMethods.add(method(mods(PUBLIC | FINAL),
							PaniniConstants.PANINI_FINISH, voidt(),
							params(var(mods(0), "t", id("Void"))), body(sync(thist(),
							// The duck is ready.
									body(es(assign("redeemed", truev())), es(apply("notifyAll")))))));

					wrappedMethods.add(this.createPaniniMessageID());
					wrappedMethods.add(createVoidFutureGetMethod());
					implement = implementing(
							ta(id(PaniniConstants.DUCK_INTERFACE_NAME), args(id("Void")))).toList();

					ListBuffer<JCTree> variableFields = new ListBuffer<JCTree>();

					if (!method.params.isEmpty()) {
						ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
						ListBuffer<JCVariableDecl> consParams = new ListBuffer<JCVariableDecl>();
						consParams.add(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT)));
						consBody.add(es(assign(select(thist(), "messageId"), id("messageId"))));

						for (JCVariableDecl par : method.params) {
							consParams.add(var(mods(0), par.name, par.vartype));
							consBody.add(es(assign(
									select(thist(), createFieldString(method.name, par, method.params)), 
									id(par.name))));
							variableFields.add(var(mods(PUBLIC),
									names.fromString(createFieldString(method.name, par, method.params)),
									par.vartype));
						}
						constructors.add(constructor(mods(PUBLIC), consParams, body(consBody)));
					}
					
					JCClassDecl wrappedClass = make.ClassDef(
							mods(0),
							names.fromString(PaniniConstants.DUCK_INTERFACE_NAME + "$"
									+ restype.toString() + "$" + tree.name.toString()),
							List.<JCTypeParameter> nil(),
							null,
							implement,
							defs(fieldMessageId, fieldRedeemed).appendList(variableFields)
									.appendList(wrappedMethods).appendList(constructors).toList());

					classes.add(wrappedClass);
					alreadedAddedDuckClasses.put(restype.toString(), wrappedClass);
				} else {
					if (!method.params.isEmpty()) {
						JCClassDecl duckClass = alreadedAddedDuckClasses.get(restype.toString());
						adaptDuckClass(method, iter, duckClass);
					}
				}
			}
		}
		return classes.toList();
	}
	
	private void adaptDuckClass(JCMethodDecl method, Iterator<Symbol> iter, JCClassDecl duckClass) {
		ListBuffer<JCTree> newFields = new ListBuffer<JCTree>();
		if (!hasDuplicate(duckClass, method.params, method.name)) {
			ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
			ListBuffer<JCVariableDecl> consParams = new ListBuffer<JCVariableDecl>();

			boolean hasConstructor = false;
			ListBuffer<JCExpression> inits = new ListBuffer<JCExpression>();
			while (iter.hasNext()) {
				Symbol s = iter.next();
				if (s.getKind() == ElementKind.CONSTRUCTOR) {
					MethodSymbol m = (MethodSymbol) s;
					for (VarSymbol v : m.params()) {
						if (v.type.toString().equals("boolean"))
							inits.add(falsev());
						else if (v.type.isPrimitive()) {
							inits.add(intlit(0));
						}
					}
					hasConstructor = true;
					break;
				}
			}
			if (hasConstructor)
				consBody.add(es(make.Apply(List.<JCExpression> nil(), id(names._super), inits.toList())));

			consParams.add(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT)));
			consBody.add(es(assign(select(thist(), "messageId"), id("messageId"))));

			for (JCVariableDecl par : method.params) {
				consParams.add(var(mods(0), par.name, par.vartype));
				consBody.add(es(assign(
						select(thist(),
								createFieldString(method.name, par, method.params)),
						id(par.name))));
			}
			duckClass.defs = duckClass.defs.append(constructor(mods(PUBLIC), consParams, body(consBody)));
		}
		for (JCVariableDecl par : method.params) {
			newFields.add(var(mods(PUBLIC),
					names.fromString(createFieldString(method.name, par, method.params)),
					par.vartype));
		}
		duckClass.defs = duckClass.defs.appendList(newFields);
	}
	
	private String trim(String fullName){
		int index =0;
		while(fullName.indexOf(".", index+1)!=-1){
			index = fullName.indexOf(".", index+1);
		}
		if(index ==0)
			return fullName;
		else{		
			String rawClassName = fullName.toString().substring(index+1);
			return rawClassName;
		}
	}

	private JCClassDecl generateNewDuckClass(String classNameSuffix, JCMethodDecl method, ListBuffer<JCTree> constructors, Type restype, Iterator<Symbol> iter) {
		String rawClassName = trim(restype.toString());
		JCVariableDecl fieldWrapped = var(mods(PRIVATE), "wrapped", rawClassName, nullv());
		JCVariableDecl fieldMessageId = var(mods(PRIVATE | FINAL), "messageId", make.TypeIdent(TypeTags.INT), null);
		JCVariableDecl fieldRedeemed = var(mods(PRIVATE), PaniniConstants.REDEEMED, make.TypeIdent(TypeTags.BOOLEAN), make.Literal(TypeTags.BOOLEAN, 0));

		ListBuffer<JCTree> wrappedMethods = new ListBuffer<JCTree>();
		boolean addedConstructors = false;
		ListBuffer<JCExpression> inits = new ListBuffer<JCExpression>();
		while (iter.hasNext()) {
			Symbol s = iter.next();
			if (s.getKind() == ElementKind.METHOD) {
				MethodSymbol m = (MethodSymbol) s;
				JCMethodDecl value; 
				if (!m.type.getReturnType().toString().equals("void")){
					value = createFutureValueMethod(m, m.name);
				}
				else{ 
					value = createVoidFutureValueMethod(m, m.name);
				}
				wrappedMethods.add(value);
			} else if (s.getKind() == ElementKind.CONSTRUCTOR) {
				MethodSymbol m = (MethodSymbol) s;

				ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
				params.add(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT)));

				
				if (!addedConstructors) {
					for (VarSymbol v : m.params()) {
						if (v.type.toString().equals("boolean"))
							inits.add(falsev());
						else if (v.type.isPrimitive()) {
							inits.add(intlit(0));
						} else
							inits.add(nullv());
					}
					constructors.add(createDuckConstructor(inits));
					addedConstructors = true;
				}
			}
		}

		JCMethodDecl messageIdMethod = createPaniniMessageID();
		JCMethodDecl finishMethod = createPaniniFinishMethod(rawClassName);
		JCMethodDecl hashCode = createHashCode();
		JCMethodDecl equals = createEquals();

		JCExpression extending;
		List<JCExpression> implement;
		if (restype.isInterface()) {
			extending = null;
			implement = implementing(
					ta(id(PaniniConstants.DUCK_INTERFACE_NAME),
							args(id(restype.toString()))), id(restype.toString())).toList();
		} else {
			JCMethodDecl get;
			if (restype.toString().equals("void")) {
				extending = id(PaniniConstants.DUCK_INTERFACE_NAME + "$Void");
				implement = implementing(
						ta(id(PaniniConstants.DUCK_INTERFACE_NAME), args(id("Void"))))
						.toList();
				get = createVoidFutureGetMethod();
				wrappedMethods.add(get);
			} else {
				extending = id(rawClassName);
				implement = implementing(
						ta(id(PaniniConstants.DUCK_INTERFACE_NAME),
								args(id(rawClassName)))).toList();
				get = createFutureGetMethod(restype);
				wrappedMethods.add(get);
			}
		}

		ListBuffer<JCTree> variableFields = new ListBuffer<JCTree>();

		if (!method.params.isEmpty()) {
			ListBuffer<JCStatement> consBody = new ListBuffer<JCStatement>();
			ListBuffer<JCVariableDecl> consParams = new ListBuffer<JCVariableDecl>();
			if (addedConstructors) {
				consBody.add(es(make.Apply(List.<JCExpression> nil(), id(names._super),
						inits.toList())));
			}
			consParams.add(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT)));
			consBody.add(es(assign(select(thist(), "messageId"), id("messageId"))));

			for (JCVariableDecl par : method.params) {
				consParams.add(var(mods(0), par.name, par.vartype));
				consBody.add(es(assign(
						select(thist(),
								createFieldString(method.name, par, method.params)), id(par.name))));
				variableFields.add(var(mods(PUBLIC),
						names.fromString(createFieldString(method.name, par, method.params)),
						par.vartype));
			}
			constructors.add(constructor(mods(PUBLIC), consParams, body(consBody)));
		}

		JCClassDecl wrappedClass = make.ClassDef(
				mods(0),
				names.fromString(PaniniConstants.DUCK_INTERFACE_NAME + "$"
						+ rawClassName + "$" + classNameSuffix),
						List.<JCTypeParameter> nil(), extending, implement,
						defs(fieldWrapped, fieldMessageId, fieldRedeemed, finishMethod, messageIdMethod, hashCode, equals).appendList(variableFields)
						.appendList(constructors).appendList(wrappedMethods).toList());

		return wrappedClass;
	}

	private JCMethodDecl createPaniniFinishMethod(String restype) {
		ListBuffer<JCVariableDecl> finishParams = new ListBuffer<JCVariableDecl>();
		finishParams.add(var(mods(0), "t", restype));
		JCMethodDecl finishMethod = method(
				mods(PUBLIC | FINAL),
				names.fromString(PaniniConstants.PANINI_FINISH),
				make.Type(syms.voidType),
				finishParams,
				body(sync(
						thist(),
						body(es(assign("wrapped", id("t"))), es(assign("redeemed", truev())),
								es(apply("notifyAll"))))));
		return finishMethod;
	}

	private JCMethodDecl createPaniniMessageID() {
		return method(mods(PUBLIC | FINAL),
				names.fromString(PaniniConstants.PANINI_MESSAGE_ID),
				make.TypeIdent(TypeTags.INT),
				body(returnt(select(thist(), "messageId"))));
	}

	private JCMethodDecl createHashCode() {
		return method(mods(PUBLIC | FINAL),
				names.fromString(PaniniConstants.PANINI_DUCK_HASHCODE),
				make.TypeIdent(TypeTags.INT),
				body(returnt(apply(apply(thist(),PaniniConstants.PANINI_DUCK_GET, new ListBuffer<JCExpression>()), PaniniConstants.PANINI_DUCK_HASHCODE, new ListBuffer<JCExpression>()))));
	}

	private JCMethodDecl createEquals() {
		ListBuffer<JCVariableDecl> equalsParams = new ListBuffer<JCVariableDecl>();
		equalsParams.add(var(mods(0), "o", "Object"));		
		return method(mods(PUBLIC | FINAL),
				names.fromString(PaniniConstants.PANINI_DUCK_EQUALS),
				make.TypeIdent(TypeTags.BOOLEAN),
				equalsParams,
    body(ifs(
				isNull(apply(thist(),PaniniConstants.PANINI_DUCK_GET, new ListBuffer<JCExpression>())),
				returnt(isNull("o")),
				returnt(apply(select(thist(),"wrapped"), PaniniConstants.PANINI_DUCK_EQUALS, args(id("o")))))));
	}

	private JCMethodDecl createDuckConstructor(ListBuffer<JCExpression> inits) {
		return constructor(
				mods(PUBLIC),
				params(var(mods(0), "messageId", make.TypeIdent(TypeTags.INT))),
				body(es(make.Apply(List.<JCExpression> nil(),
						make.Ident(names._super), inits.toList())),
						es(assign(select(thist(), "messageId"), id("messageId")))));
	}

	private JCMethodDecl createFutureValueMethod(MethodSymbol m, Name method_name) {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
		JCExpression restype = make.Type(m.type.getReturnType());
		
		// TODO correct type parameter replacements
		if(m.type.getReturnType().getKind()==TypeKind.DECLARED){
			if(m.type.getReturnType().toString().equals("java.lang.Object")){
				restype = id(trim(((ClassType)m.owner.type).supertype_field.getTypeArguments().head.toString()));
			}
		}
		
		// TODO correct type parameter replacements
		if(m.getParameters()!=null){
			for(VarSymbol v:m.getParameters()){
				if(v.type.toString().equals("java.lang.Object")){
					params.add(var(mods(0), v.name, id(trim(((ClassType)m.owner.type).supertype_field.getTypeArguments().head.toString()))));
				}else
					params.add(make.VarDef(v, null));
				args.add(id(v.name));
			}
		}
		List<JCCatch> catchers = List.<JCCatch> of(make.Catch(make.VarDef(
				make.Modifiers(0), names.fromString("e"),
				make.Ident(names.fromString("InterruptedException")), null),
				make.Block(
						0,
						List.<JCStatement> of(returnt(apply("wrapped", method_name.toString(), args))))));
		JCMethodDecl value = method(
				mods(PUBLIC),
				method_name,
				restype,
				params,
				body(make.Try(
						body(sync(
								make.This(Type.noType),
								body(whilel(isFalse(PaniniConstants.REDEEMED),
										es(apply("wait")))))), catchers, null),
						returnt(apply("wrapped", method_name.toString(), args))));
		return value;
	}

	private JCMethodDecl createVoidFutureValueMethod(MethodSymbol m, Name method_name) {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
		ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
		
		// TODO correct type parameter replacements
		if(m.getParameters()!=null){
			for(VarSymbol v:m.getParameters()){
				if(v.type.toString().equals("java.lang.Object")){
					params.add(var(mods(0), v.name, id(trim(((ClassType)m.owner.type).supertype_field.getTypeArguments().head.toString()))));
				}else
					params.add(make.VarDef(v, null));
				args.add(id(v.name));
			}
		}
		List<JCCatch> catchers = List.<JCCatch> of(make.Catch(make.VarDef(
				make.Modifiers(0), names.fromString("e"),
				make.Ident(names.fromString("InterruptedException")), null),
				make.Block(0, List.<JCStatement> of(make.Exec(make.Apply(null,
					make.Ident(method_name),
					args.toList()))))));
		JCMethodDecl delegate = method(
				mods(PUBLIC | FINAL),
				method_name,
				make.Type(syms.voidType),
				params,
				body(make.Try(body(sync(make.This(Type.noType), body(whilel(
				// Test whether the duck is ready.
				// while (redeemed == false) wait();
						isFalse(PaniniConstants.REDEEMED), es(apply("wait")))))), catchers,
						null), es(apply("wrapped", method_name.toString(), args))));
		return delegate;
	}

	private JCMethodDecl createFutureGetMethod(Type restype) {
		ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
		
		// TODO correct type parameter replacements
		List<JCCatch> catchers = List.<JCCatch> of(make.Catch(make.VarDef(
				make.Modifiers(0), names.fromString("e"),
				make.Ident(names.fromString("InterruptedException")), null),
				make.Block(
						0,
						List.<JCStatement> of(returnt("wrapped")))));
		JCMethodDecl value = method(
				mods(PUBLIC),
				"panini$get",
				id(trim(restype.toString())),
				params,
				body(make.Try(
						body(sync(
								make.This(Type.noType),
								body(whilel(isFalse(PaniniConstants.REDEEMED),
										es(apply("wait")))))), catchers, null),
						returnt("wrapped")));
		return value;
	}

	private JCMethodDecl createVoidFutureGetMethod() {
ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
		
		// TODO correct type parameter replacements
		List<JCCatch> catchers = List.<JCCatch> of(make.Catch(make.VarDef(
				make.Modifiers(0), names.fromString("e"),
				make.Ident(names.fromString("InterruptedException")), null),
				make.Block(
						0,
						List.<JCStatement> of(returnt(nullv())))));
		JCMethodDecl value = method(
				mods(PUBLIC),
				"panini$get",
				id("Void"),
				params,
				body(make.Try(
						body(sync(
								make.This(Type.noType),
								body(whilel(isFalse(PaniniConstants.REDEEMED),
										es(apply("wait")))))), catchers, null),
						returnt(nullv())));
		return value;
	}
	
	private ClassSymbol checkAndResolveReturnType(Env<AttrContext> env,	Resolve rs, Type restype) {
		ClassSymbol c;
		if (restype.toString().equals("void"))
			c = (ClassSymbol) rs.findIdent(env, names.fromString(PaniniConstants.DUCK_INTERFACE_NAME + "$Void"), TYP);
		else if (restype.isPrimitive()) {
			System.out.println("\n\nNon-void primitive return types for capsule procedure calls are not yet supported.\n\n");
			System.exit(10);
			c = null;
		} else
			c = (ClassSymbol) restype.tsym;

		if (c.type.isFinal()) {
			System.out.println("\n\nFinal classes as return types for capsule procedure calls are not supported.\n\n");
			System.exit(10);
		}
		return c;
	}

	boolean hasDuplicate(JCClassDecl c, List<JCVariableDecl> v, Name name) {
		boolean result = false;
		for (JCTree def : c.defs) {
			if (def.getTag() == Tag.METHODDEF
					&& ((JCMethodDecl) def).name.equals(names.init)) {
				if (((JCMethodDecl) def).params.length() == v.length() + 1) {
					result = true;
					for (int i = 1; i < ((JCMethodDecl) def).params.length(); i++) {

						if (!((JCMethodDecl) def).params.get(i).vartype.toString().equals(
								v.get(i - 1).vartype.toString())) {
							result = false;
							i = ((JCMethodDecl) def).params.length();
						}
					}
					if (result) {
						for (JCVariableDecl var : v)
							((JCMethodDecl) def).body.stats = ((JCMethodDecl) def).body.stats
									.append(es(assign(
											select(thist(), createFieldString(name, var, v)),
											id(var.name.toString()))));
					}
				}
			}
		}
		return result;
	}
	
	private String createFieldString(Name name, JCVariableDecl param, List<JCVariableDecl> params){
		return createFieldString(name.toString(), param.vartype.toString(), param.name.toString(), params);
	}
	private String createFieldString(String name, String vartype, String paramName, List<JCVariableDecl> params){
		String fieldName;
		fieldName = vartype + "$" + paramName + "$" + name;
		if(params.nonEmpty())
		for(JCVariableDecl v : params){
			fieldName = fieldName + "$" + v.vartype.toString();
		}
		return fieldName;
	}
}
