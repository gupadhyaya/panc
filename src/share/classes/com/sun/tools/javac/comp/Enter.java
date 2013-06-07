/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.javac.comp;

import java.util.*;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileManager;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Scope.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.main.Option.PkgInfo;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.TypeTags.PACKAGE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.DOT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.STAR;
import static com.sun.tools.javac.tree.JCTree.Tag.*;
// Panini code
import com.sun.tools.javac.parser.ParserFactory;
import org.paninij.comp.AnnotationProcessor;
// end Panini code

/** This class enters symbols for all encountered definitions into
 *  the symbol table. The pass consists of two phases, organized as
 *  follows:
 *
 *  <p>In the first phase, all class symbols are intered into their
 *  enclosing scope, descending recursively down the tree for classes
 *  which are members of other classes. The class symbols are given a
 *  MemberEnter object as completer.
 *
 *  <p>In the second phase classes are completed using
 *  MemberEnter.complete().  Completion might occur on demand, but
 *  any classes that are not completed that way will be eventually
 *  completed by processing the `uncompleted' queue.  Completion
 *  entails (1) determination of a class's parameters, supertype and
 *  interfaces, as well as (2) entering all symbols defined in the
 *  class into its scope, with the exception of class symbols which
 *  have been entered in phase 1.  (2) depends on (1) having been
 *  completed for a class and all its superclasses and enclosing
 *  classes. That's why, after doing (1), we put classes in a
 *  `halfcompleted' queue. Only when we have performed (1) for a class
 *  and all it's superclasses and enclosing classes, we proceed to
 *  (2).
 *
 *  <p>Whereas the first phase is organized as a sweep through all
 *  compiled syntax trees, the second phase is demand. Members of a
 *  class are entered when the contents of a class are first
 *  accessed. This is accomplished by installing completer objects in
 *  class symbols for compiled classes which invoke the member-enter
 *  phase for the corresponding class tree.
 *
 *  <p>Classes migrate from one phase to the next via queues:
 *
 *  <pre>
 *  class enter -> (Enter.uncompleted)         --> member enter (1)
 *              -> (MemberEnter.halfcompleted) --> member enter (2)
 *              -> (Todo)                      --> attribute
 *                                              (only for toplevel classes)
 *  </pre>
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Enter extends JCTree.Visitor {
    protected static final Context.Key<Enter> enterKey =
        new Context.Key<Enter>();

    Log log;
    Symtab syms;
    Check chk;
    TreeMaker make;
    ClassReader reader;
    Annotate annotate;
    MemberEnter memberEnter;
    Types types;
    Lint lint;
    Names names;
    JavaFileManager fileManager;
    PkgInfo pkginfoOpt;
    // Panini code
    AnnotationProcessor annotationProcessor;
    // end Panini code

    private final Todo todo;

    public static Enter instance(Context context) {
        Enter instance = context.get(enterKey);
        if (instance == null)
            instance = new Enter(context);
        return instance;
    }

    protected Enter(Context context) {
        context.put(enterKey, this);

        log = Log.instance(context);
        reader = ClassReader.instance(context);
        make = TreeMaker.instance(context);
        syms = Symtab.instance(context);
        chk = Check.instance(context);
        memberEnter = MemberEnter.instance(context);
        types = Types.instance(context);
        annotate = Annotate.instance(context);
        lint = Lint.instance(context);
        names = Names.instance(context);

        predefClassDef = make.ClassDef(
            make.Modifiers(PUBLIC),
            syms.predefClass.name, null, null, null, null);
        predefClassDef.sym = syms.predefClass;
        todo = Todo.instance(context);
        fileManager = context.get(JavaFileManager.class);

        Options options = Options.instance(context);
        pkginfoOpt = PkgInfo.get(options);

        // Panini code
        annotationProcessor = new AnnotationProcessor(names, make, ParserFactory.instance(context), log);
        // end Panini code
    }

    /** A hashtable mapping classes and packages to the environments current
     *  at the points of their definitions.
     */
    Map<TypeSymbol,Env<AttrContext>> typeEnvs =
            new HashMap<TypeSymbol,Env<AttrContext>>();

    /** Accessor for typeEnvs
     */
    public Env<AttrContext> getEnv(TypeSymbol sym) {
        return typeEnvs.get(sym);
    }

    public Env<AttrContext> getClassEnv(TypeSymbol sym) {
        Env<AttrContext> localEnv = getEnv(sym);
        Env<AttrContext> lintEnv = localEnv;
        while (lintEnv.info.lint == null)
            lintEnv = lintEnv.next;
        localEnv.info.lint = lintEnv.info.lint.augment(sym.attributes_field, sym.flags());
        return localEnv;
    }

    /** The queue of all classes that might still need to be completed;
     *  saved and initialized by main().
     */
    ListBuffer<ClassSymbol> uncompleted;

    /** A dummy class to serve as enclClass for toplevel environments.
     */
    private JCClassDecl predefClassDef;

/* ************************************************************************
 * environment construction
 *************************************************************************/


    /** Create a fresh environment for class bodies.
     *  This will create a fresh scope for local symbols of a class, referred
     *  to by the environments info.scope field.
     *  This scope will contain
     *    - symbols for this and super
     *    - symbols for any type parameters
     *  In addition, it serves as an anchor for scopes of methods and initializers
     *  which are nested in this scope via Scope.dup().
     *  This scope should not be confused with the members scope of a class.
     *
     *  @param tree     The class definition.
     *  @param env      The environment current outside of the class definition.
     */
    public Env<AttrContext> classEnv(JCClassDecl tree, Env<AttrContext> env) {
        Env<AttrContext> localEnv =
            env.dup(tree, env.info.dup(new Scope(tree.sym)));
        localEnv.enclClass = tree;
        localEnv.outer = env;
        localEnv.info.isSelfCall = false;
        localEnv.info.lint = null; // leave this to be filled in by Attr,
                                   // when annotations have been processed
        return localEnv;
    }

    /** Create a fresh environment for toplevels.
     *  @param tree     The toplevel tree.
     */
    Env<AttrContext> topLevelEnv(JCCompilationUnit tree) {
        Env<AttrContext> localEnv = new Env<AttrContext>(tree, new AttrContext());
        localEnv.toplevel = tree;
        localEnv.enclClass = predefClassDef;
        tree.namedImportScope = new ImportScope(tree.packge);
        tree.starImportScope = new StarImportScope(tree.packge);
        localEnv.info.scope = tree.namedImportScope;
        localEnv.info.lint = lint;
        return localEnv;
    }

    public Env<AttrContext> getTopLevelEnv(JCCompilationUnit tree) {
        Env<AttrContext> localEnv = new Env<AttrContext>(tree, new AttrContext());
        localEnv.toplevel = tree;
        localEnv.enclClass = predefClassDef;
        localEnv.info.scope = tree.namedImportScope;
        localEnv.info.lint = lint;
        return localEnv;
    }

    /** The scope in which a member definition in environment env is to be entered
     *  This is usually the environment's scope, except for class environments,
     *  where the local scope is for type variables, and the this and super symbol
     *  only, and members go into the class member scope.
     */
    Scope enterScope(Env<AttrContext> env) {
        return (env.tree.hasTag(JCTree.Tag.CLASSDEF))
            ? ((JCClassDecl) env.tree).sym.members_field
            : env.info.scope;
    }

/* ************************************************************************
 * Visitor methods for phase 1: class enter
 *************************************************************************/

    /** Visitor argument: the current environment.
     */
    protected Env<AttrContext> env;

    /** Visitor result: the computed type.
     */
    Type result;

    /** Visitor method: enter all classes in given tree, catching any
     *  completion failure exceptions. Return the tree's type.
     *
     *  @param tree    The tree to be visited.
     *  @param env     The environment visitor argument.
     */
    Type classEnter(JCTree tree, Env<AttrContext> env) {
        Env<AttrContext> prevEnv = this.env;
        try {
            this.env = env;
            tree.accept(this);
            return result;
        }  catch (CompletionFailure ex) {
            return chk.completionError(tree.pos(), ex);
        } finally {
            this.env = prevEnv;
        }
    }

    /** Visitor method: enter classes of a list of trees, returning a list of types.
     */
    public <T extends JCTree> List<Type> classEnter(List<T> trees, Env<AttrContext> env) {
        ListBuffer<Type> ts = new ListBuffer<Type>();
        for (List<T> l = trees; l.nonEmpty(); l = l.tail) {
            Type t = classEnter(l.head, env);
            if (t != null)
                ts.append(t);
        }
        return ts.toList();
    }

    @Override
    public void visitTopLevel(JCCompilationUnit tree) {
        JavaFileObject prev = log.useSource(tree.sourcefile);
        boolean addEnv = false;
        boolean isPkgInfo = tree.sourcefile.isNameCompatible("package-info",
                                                             JavaFileObject.Kind.SOURCE);
        if (tree.pid != null) {
            tree.packge = reader.enterPackage(TreeInfo.fullName(tree.pid));
            if (tree.packageAnnotations.nonEmpty() || pkginfoOpt == PkgInfo.ALWAYS) {
                if (isPkgInfo) {
                    addEnv = true;
                } else {
                    log.error(tree.packageAnnotations.head.pos(),
                              "pkg.annotations.sb.in.package-info.java");
                }
            }
        } else {
            tree.packge = syms.unnamedPackage;
        }
        tree.packge.complete(); // Find all classes in package.
        Env<AttrContext> topEnv = topLevelEnv(tree);
        
        setRuntimeImports(topEnv);

        // Save environment of package-info.java file.
        if (isPkgInfo) {
            Env<AttrContext> env0 = typeEnvs.get(tree.packge);
            if (env0 == null) {
                typeEnvs.put(tree.packge, topEnv);
            } else {
                JCCompilationUnit tree0 = env0.toplevel;
                if (!fileManager.isSameFile(tree.sourcefile, tree0.sourcefile)) {
                    log.warning(tree.pid != null ? tree.pid.pos()
                                                 : null,
                                "pkg-info.already.seen",
                                tree.packge);
                    if (addEnv || (tree0.packageAnnotations.isEmpty() &&
                                   tree.docComments != null &&
                                   tree.docComments.get(tree) != null)) {
                        typeEnvs.put(tree.packge, topEnv);
                    }
                }
            }

            for (Symbol q = tree.packge; q != null && q.kind == PCK; q = q.owner)
                q.flags_field |= EXISTS;

            Name name = names.package_info;
            ClassSymbol c = reader.enterClass(name, tree.packge);
            c.flatname = names.fromString(tree.packge + "." + name);
            c.sourcefile = tree.sourcefile;
            c.completer = null;
            c.members_field = new Scope(c);
            tree.packge.package_info = c;
        }
    // Panini code
        tree.defs = capsuleSplitter(tree.defs);
    // end Panini code
        classEnter(tree.defs, topEnv);
        if (addEnv) {
            todo.append(topEnv);
        }
        log.useSource(prev);
        result = null;
    }
    
    // Panini Code
    private List<JCTree> capsuleSplitter(List<JCTree> defs){
    	ListBuffer<JCTree> copiedDefs = new ListBuffer<JCTree>();
    	TreeCopier<Void> tc = new TreeCopier<Void>(make);
    	for(JCTree def : defs){
    		if(def.getTag() == CAPSULEDEF && (((JCCapsuleDecl)def).mods.flags & INTERFACE)==0 ){
    			JCCapsuleDecl capsule = (JCCapsuleDecl)def;
    			ListBuffer<JCTree> interfaceBody = new ListBuffer<JCTree>();
    			boolean hasRun = false;
    			for(JCTree capsuleDefs : capsule.defs){
    				if(capsuleDefs.getTag() == METHODDEF){
    					if(((JCMethodDecl)capsuleDefs).name.toString().equals("run")&&((JCMethodDecl)capsuleDefs).params.isEmpty())
    						hasRun = true;
    					if((((JCMethodDecl)capsuleDefs).mods.flags & PRIVATE)==0 && !((JCMethodDecl)capsuleDefs).name.toString().equals(PaniniConstants.PANINI_CAPSULE_INIT)){
	    					interfaceBody.add(make.MethodDef(tc.copy(((JCMethodDecl)capsuleDefs).mods), 
	    							((JCMethodDecl)capsuleDefs).name, 
	    							tc.copy(((JCMethodDecl)capsuleDefs).restype), 
	    							tc.copy(((JCMethodDecl)capsuleDefs).typarams), 
	    							tc.copy(((JCMethodDecl)capsuleDefs).params), 
	    							tc.copy(((JCMethodDecl)capsuleDefs).thrown), null, tc.copy(((JCMethodDecl)capsuleDefs).defaultValue)));
    					}
    				}else if(capsuleDefs.getTag() == VARDEF){
    					interfaceBody.add(make.VarDef(tc.copy(((JCVariableDecl)capsuleDefs).mods), ((JCVariableDecl)capsuleDefs).name, 
    							tc.copy(((JCVariableDecl)capsuleDefs).vartype), null));
    				}else
    					interfaceBody.add(tc.copy(capsuleDefs));
    			}
    			JCExpression excp = make.Ident(names.fromString("java"));
    			excp = make.Select(excp, names.fromString("lang"));
    			excp = make.Select(excp, names.fromString("InterruptedException"));
    			interfaceBody.add(make.MethodDef(make.Modifiers(PUBLIC), names.fromString(PaniniConstants.PANINI_START), make.TypeIdent(TypeTags.VOID), 
    					List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(), List.<JCExpression>nil(), null, null));
    			interfaceBody.add(make.MethodDef(make.Modifiers(PUBLIC), names.fromString(PaniniConstants.PANINI_SHUTDOWN), make.TypeIdent(TypeTags.VOID), 
    					List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(), List.<JCExpression>nil(), null, null));
    			interfaceBody.add(make.MethodDef(make.Modifiers(PUBLIC), names.fromString(PaniniConstants.PANINI_JOIN), make.TypeIdent(TypeTags.VOID), 
    					List.<JCTypeParameter>nil(), List.<JCVariableDecl>nil(), List.<JCExpression>of(excp), null, null));
    			JCCapsuleDecl copyActive =
    					make.CapsuleDef(make.Modifiers(FINAL, annotationProcessor.createCapsuleAnnotation(Flags.ACTIVE, capsule)), names.fromString(capsule.name + "$thread"), 
    							tc.copy(capsule.params), List.<JCExpression>of(make.Ident(capsule.name)), tc.copy(capsule.defs));
    			JCCapsuleDecl copyCapsule = 
    					make.CapsuleDef(make.Modifiers(INTERFACE, annotationProcessor.createCapsuleAnnotation(Flags.INTERFACE, capsule)), 
    							capsule.name, tc.copy(capsule.params), tc.copy(capsule.implementing), interfaceBody.toList());
    			copiedDefs.add(copyCapsule);
    			copiedDefs.add(copyActive);
    			copyActive.parentCapsule = copyCapsule;
    			if(!hasRun){
    				JCCapsuleDecl copyTask =
        					make.CapsuleDef(make.Modifiers(FINAL, annotationProcessor.createCapsuleAnnotation(Flags.TASK, capsule)), names.fromString(capsule.name + "$task"), 
        							tc.copy(capsule.params), List.<JCExpression>of(make.Ident(capsule.name)), tc.copy(capsule.defs));
        			JCCapsuleDecl copySerial =
        					make.CapsuleDef(make.Modifiers(FINAL, annotationProcessor.createCapsuleAnnotation(Flags.SERIAL, capsule)), names.fromString(capsule.name + "$serial"), 
        							tc.copy(capsule.params), List.<JCExpression>of(make.Ident(capsule.name)), tc.copy(capsule.defs));
        			JCCapsuleDecl copyMonitor =
        					make.CapsuleDef(make.Modifiers(FINAL, annotationProcessor.createCapsuleAnnotation(Flags.MONITOR, capsule)), names.fromString(capsule.name + "$monitor"), 
        							tc.copy(capsule.params), List.<JCExpression>of(make.Ident(capsule.name)), tc.copy(capsule.defs));
	    			copiedDefs.add(copyTask);
	    			copyTask.parentCapsule = copyCapsule;
	    			copiedDefs.add(copySerial);
	    			copySerial.parentCapsule = copyCapsule;
	    			copiedDefs.add(copyMonitor);
	    			copyMonitor.parentCapsule = copyCapsule;
    			}
    		}
    		else
    			copiedDefs.add(tc.copy(def));
    	}
    	return copiedDefs.toList();
    }
    // end Panini code

    @Override
    public void visitClassDef(JCClassDecl tree) {
        Symbol owner = env.info.scope.owner;
        Scope enclScope = enterScope(env);
        ClassSymbol c;
        if (owner.kind == PCK) {
            // We are seeing a toplevel class.
            PackageSymbol packge = (PackageSymbol)owner;
            for (Symbol q = packge; q != null && q.kind == PCK; q = q.owner)
                q.flags_field |= EXISTS;
            c = reader.enterClass(tree.name, packge);
            packge.members().enterIfAbsent(c);
            if ((tree.mods.flags & PUBLIC) != 0 && !classNameMatchesFileName(c, env)) {
                log.error(tree.pos(),
                          "class.public.should.be.in.file", tree.name);
            }
        } else {
            if (!tree.name.isEmpty() &&
                !chk.checkUniqueClassName(tree.pos(), tree.name, enclScope)) {
                result = null;
                return;
            }
            if (owner.kind == TYP) {
                // We are seeing a member class.
                c = reader.enterClass(tree.name, (TypeSymbol)owner);
                if ((owner.flags_field & INTERFACE) != 0) {
                    tree.mods.flags |= PUBLIC | STATIC;
                }
            } else {
                // We are seeing a local class.
                c = reader.defineClass(tree.name, owner);
                c.flatname = chk.localClassName(c);
                if (!c.name.isEmpty())
                    chk.checkTransparentClass(tree.pos(), c, env.info.scope);
            }
        }
        tree.sym = c;

        // Enter class into `compiled' table and enclosing scope.
        if (chk.compiled.get(c.flatname) != null) {
            duplicateClass(tree.pos(), c);
            result = types.createErrorType(tree.name, (TypeSymbol)owner, Type.noType);
            tree.sym = (ClassSymbol)result.tsym;
            return;
        }
        chk.compiled.put(c.flatname, c);
        enclScope.enter(c);

        // Set up an environment for class block and store in `typeEnvs'
        // table, to be retrieved later in memberEnter and attribution.
        Env<AttrContext> localEnv = classEnv(tree, env);
        typeEnvs.put(c, localEnv);

        // Fill out class fields.
        c.completer = memberEnter;
        c.flags_field = chk.checkFlags(tree.pos(), tree.mods.flags, c, tree);
        c.sourcefile = env.toplevel.sourcefile;
        c.members_field = new Scope(c);

        ClassType ct = (ClassType)c.type;
        if (owner.kind != PCK && (c.flags_field & STATIC) == 0) {
            // We are seeing a local or inner class.
            // Set outer_field of this class to closest enclosing class
            // which contains this class in a non-static context
            // (its "enclosing instance class"), provided such a class exists.
            Symbol owner1 = owner;
            while ((owner1.kind & (VAR | MTH)) != 0 &&
                   (owner1.flags_field & STATIC) == 0) {
                owner1 = owner1.owner;
            }
            if (owner1.kind == TYP) {
                ct.setEnclosingType(owner1.type);
            }
        }

        // Enter type parameters.
        ct.typarams_field = classEnter(tree.typarams, localEnv);

        // Add non-local class to uncompleted, to make sure it will be
        // completed later.
        if (!c.isLocal() && uncompleted != null) uncompleted.append(c);
//      System.err.println("entering " + c.fullname + " in " + c.owner);//DEBUG

        // Recursively enter all member classes.
        classEnter(tree.defs, localEnv);

        result = c.type;
    }
    //where
        /** Does class have the same name as the file it appears in?
         */
        private static boolean classNameMatchesFileName(ClassSymbol c,
                                                        Env<AttrContext> env) {
            return env.toplevel.sourcefile.isNameCompatible(c.name.toString(),
                                                            JavaFileObject.Kind.SOURCE);
        }

    /** Complain about a duplicate class. */
    protected void duplicateClass(DiagnosticPosition pos, ClassSymbol c) {
        log.error(pos, "duplicate.class", c.fullname);
    }

    /** Class enter visitor method for type parameters.
     *  Enter a symbol for type parameter in local scope, after checking that it
     *  is unique.
     */
    @Override
    public void visitTypeParameter(JCTypeParameter tree) {
        TypeVar a = (tree.type != null)
            ? (TypeVar)tree.type
            : new TypeVar(tree.name, env.info.scope.owner, syms.botType);
        tree.type = a;
        if (chk.checkUnique(tree.pos(), a.tsym, env.info.scope)) {
            env.info.scope.enter(a.tsym);
        }
        result = a;
    }

    /** Default class enter visitor method: do nothing.
     */
    @Override
    public void visitTree(JCTree tree) {
        result = null;
    }
    // Panini code
    public void visitSystemDef(JCSystemDecl tree){
    	Symbol owner = env.info.scope.owner;
        Scope enclScope = enterScope(env);
        SystemSymbol c;
        if (owner.kind == PCK) {
            // We are seeing a toplevel class.
            PackageSymbol packge = (PackageSymbol)owner;
            for (Symbol q = packge; q != null && q.kind == PCK; q = q.owner)
                q.flags_field |= EXISTS;
            c = reader.enterSystem(tree.name, packge);
            packge.members().enterIfAbsent(c);
            if ((tree.mods.flags & PUBLIC) != 0 && !classNameMatchesFileName(c, env)) {
                log.error(tree.pos(),
                          "class.public.should.be.in.file", tree.name);
            }
        } else {
            if (!tree.name.isEmpty() &&
                !chk.checkUniqueClassName(tree.pos(), tree.name, enclScope)) {
                result = null;
                return;
            }
            if (owner.kind == TYP) {
                // We are seeing a member class.
                c = reader.enterSystem(tree.name, (TypeSymbol)owner);
                if ((owner.flags_field & INTERFACE) != 0) {
                    tree.mods.flags |= PUBLIC | STATIC;
                }
            } else {
                // We are seeing a local class.
                c = reader.defineSystem(tree.name, owner);
                c.flatname = chk.localClassName(c);
                if (!c.name.isEmpty())
                    chk.checkTransparentClass(tree.pos(), c, env.info.scope);
            }
        }
        tree.sym = c;

        // Enter class into `compiled' table and enclosing scope.
        if (chk.compiled.get(c.flatname) != null) {
            duplicateClass(tree.pos(), c);
            result = types.createErrorType(tree.name, (TypeSymbol)owner, Type.noType);
            tree.sym = (SystemSymbol)result.tsym;
            return;
        }
        chk.compiled.put(c.flatname, c);
        enclScope.enter(c);

        // Set up an environment for class block and store in `typeEnvs'
        // table, to be retrieved later in memberEnter and attribution.
        Env<AttrContext> localEnv = classEnv(tree, env);
        typeEnvs.put(c, localEnv);

        // Fill out class fields.
        c.completer = memberEnter;
        c.flags_field = chk.checkFlags(tree.pos(), tree.mods.flags, c, tree);
        c.sourcefile = env.toplevel.sourcefile;
        c.members_field = new Scope(c);

        ClassType ct = (ClassType)c.type;
        if (owner.kind != PCK && (c.flags_field & STATIC) == 0) {
            // We are seeing a local or inner class.
            // Set outer_field of this class to closest enclosing class
            // which contains this class in a non-static context
            // (its "enclosing instance class"), provided such a class exists.
            Symbol owner1 = owner;
            while ((owner1.kind & (VAR | MTH)) != 0 &&
                   (owner1.flags_field & STATIC) == 0) {
                owner1 = owner1.owner;
            }
            if (owner1.kind == TYP) {
                ct.setEnclosingType(owner1.type);
            }
        }

        // Enter type parameters.
        ct.typarams_field = classEnter(tree.typarams, localEnv);

        // Add non-local class to uncompleted, to make sure it will be
        // completed later.
        if (!c.isLocal() && uncompleted != null) uncompleted.append(c);
//      System.err.println("entering " + c.fullname + " in " + c.owner);//DEBUG

        
        // Recursively enter all member classes.
//        classEnter(tree.defs, localEnv);

        tree.sym = c;
        result = c.type;
        tree.switchToClass();
    }
        
    private void setRuntimeImports(Env<AttrContext> env){
    	JCExpression pid = make.Ident(names.fromString("java"));
    	pid = make.Select(pid, names.fromString("util"));
    	pid = make.Select(pid, names.fromString("concurrent"));
    	pid = make.Select(pid, names.fromString("locks"));
    	pid = make.Select(pid, names.fromString("ReentrantLock"));
    	env.toplevel.defs = env.toplevel.defs.prepend(make.Import(pid, false));
    	pid = make.Ident(names.fromString("org"));
    	pid = make.Select(pid, names.fromString("paninij"));
    	pid = make.Select(pid, names.fromString("runtime"));
    	pid = make.Select(pid, names.asterisk);
    	env.toplevel.defs = env.toplevel.defs.prepend(make.Import(pid, false));
    	pid = make.Ident(names.fromString("org"));
    	pid = make.Select(pid, names.fromString("paninij"));
    	pid = make.Select(pid, names.fromString("runtime"));
    	pid = make.Select(pid, names.fromString("types"));
    	pid = make.Select(pid, names.fromString("Panini$Duck"));
    	env.toplevel.defs = env.toplevel.defs.prepend(make.Import(pid, false));
    	pid = make.Ident(names.fromString("org"));
    	pid = make.Select(pid, names.fromString("paninij"));
    	pid = make.Select(pid, names.fromString("runtime"));
    	pid = make.Select(pid, names.fromString("types"));
    	pid = make.Select(pid, names.fromString("Panini$Duck$Void"));
    	env.toplevel.defs = env.toplevel.defs.prepend(make.Import(pid, false));
    }
    
    public void visitCapsuleDef(JCCapsuleDecl tree){
    	if((tree.mods.flags & Flags.INTERFACE) !=0){
    		tree.needsDefaultRun= false;
    	}
    	Symbol owner = env.info.scope.owner;
        Scope enclScope = enterScope(env);
        CapsuleSymbol c;
        if (owner.kind == PCK) {
            // We are seeing a toplevel class.
            PackageSymbol packge = (PackageSymbol)owner;
            for (Symbol q = packge; q != null && q.kind == PCK; q = q.owner)
                q.flags_field |= EXISTS;
            c = reader.enterCapsule(tree.name, packge);
            packge.members().enterIfAbsent(c);
            if ((tree.mods.flags & PUBLIC) != 0 && !classNameMatchesFileName(c, env)) {
                log.error(tree.pos(),
                          "class.public.should.be.in.file", tree.name);
            }
        } else {
            if (!tree.name.isEmpty() &&
                !chk.checkUniqueClassName(tree.pos(), tree.name, enclScope)) {
                result = null;
                return;
            }
            if (owner.kind == TYP) {
                // We are seeing a member class.
                c = reader.enterCapsule(tree.name, (TypeSymbol)owner);
                if ((owner.flags_field & INTERFACE) != 0) {
                    tree.mods.flags |= PUBLIC | STATIC;
                }
            } else {
                // We are seeing a local class.
                c = reader.defineCapsule(tree.name, owner);
                c.flatname = chk.localClassName(c);
                if (!c.name.isEmpty())
                    chk.checkTransparentClass(tree.pos(), c, env.info.scope);
            }
        }
        tree.sym = c;
        tree.sym.tree = tree;

        // Enter class into `compiled' table and enclosing scope.
        if (chk.compiled.get(c.flatname) != null) {
            duplicateClass(tree.pos(), c);
            result = types.createErrorType(tree.name, (TypeSymbol)owner, Type.noType);
            tree.sym = (ClassSymbol)result.tsym;
            return;
        }
        chk.compiled.put(c.flatname, c);
        enclScope.enter(c);

        // Set up an environment for class block and store in `typeEnvs'
        // table, to be retrieved later in memberEnter and attribution.
        Env<AttrContext> localEnv = classEnv(tree, env);
        typeEnvs.put(c, localEnv);

        // Fill out class fields.
        c.completer = memberEnter;
        c.flags_field = chk.checkFlags(tree.pos(), tree.mods.flags, c, tree);
        c.sourcefile = env.toplevel.sourcefile;
        c.members_field = new Scope(c);

        ClassType ct = (ClassType)c.type;
        if (owner.kind != PCK && (c.flags_field & STATIC) == 0) {
            // We are seeing a local or inner class.
            // Set outer_field of this class to closest enclosing class
            // which contains this class in a non-static context
            // (its "enclosing instance class"), provided such a class exists.
            Symbol owner1 = owner;
            while ((owner1.kind & (VAR | MTH)) != 0 &&
                   (owner1.flags_field & STATIC) == 0) {
                owner1 = owner1.owner;
            }
            if (owner1.kind == TYP) {
                ct.setEnclosingType(owner1.type);
            }
        }

        // Enter type parameters.
        ct.typarams_field = classEnter(tree.typarams, localEnv);

        // Add non-local class to uncompleted, to make sure it will be
        // completed later.
        if (!c.isLocal() && uncompleted != null) uncompleted.append(c);
//      System.err.println("entering " + c.fullname + " in " + c.owner);//DEBUG
        
        if((tree.mods.flags & Flags.INTERFACE) ==0){
	        c.flags_field = processCapsuleAnnotations(tree, c);
	        ListBuffer<JCTree> definitions = new ListBuffer<JCTree>();
	        if((c.flags_field & SERIAL)!=0){
	        	definitions = translateSerialCapsule(tree, c, localEnv);
	        	((CapsuleSymbol)tree.parentCapsule.sym).translated_serial = c;
	        }else if((c.flags_field & ACTIVE)!=0){
	        	definitions = translateActiveCapsule(tree, c, localEnv);
	        	((CapsuleSymbol)tree.parentCapsule.sym).translated_thread = c;
	        }else if((c.flags_field & TASK)!=0){
	        	definitions = translateTaskCapsule(tree, c, localEnv);
	        	((CapsuleSymbol)tree.parentCapsule.sym).translated_task = c;
	        }else if((c.flags_field & MONITOR)!=0){
	        	definitions = translateMonitorCapsule(tree, c, localEnv);
	        	((CapsuleSymbol)tree.parentCapsule.sym).translated_monitor = c;
	        }else{ //default action
	        	definitions = translateActiveCapsule(tree, c, localEnv);
	        	((CapsuleSymbol)tree.parentCapsule.sym).translated_thread = c;
	        }
	        c.parentCapsule = (CapsuleSymbol)tree.parentCapsule.sym;
	    	List<JCVariableDecl> fields = tree.getParameters();
	    	while(fields.nonEmpty()){
	    		definitions.prepend(make.VarDef(make.Modifiers(PUBLIC),
	    				fields.head.name,
	    				fields.head.vartype,
	    				null));
	    		fields = fields.tail;
	    	}
	    	tree.defs = definitions.toList();
        }else {
        	List<JCVariableDecl> fields = tree.getParameters();
	    	while(fields.nonEmpty()){
	    		tree.defs = tree.defs.prepend(make.VarDef(make.Modifiers(PUBLIC),
	    				fields.head.name,
	    				fields.head.vartype,
	    				null));
	    		fields = fields.tail;
	    	}
        	c.definedRun = true;
        }
        ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
        params.appendList(tree.params);
        c.capsuleParameters = params.toList();
        tree.sym = c;
        syms.capsules.put(c.name, c);
        classEnter(tree.defs, localEnv);
        result = c.type;
        annotationProcessor.setDefinedRun(tree, c.definedRun);
//        c.fillIn();//fill in fields?
        tree.switchToClass();
    }
    
    public List<JCStatement> push(Name n){
    	ListBuffer<JCStatement> stats = new ListBuffer<JCStatement>();
    	stats.add(make.Exec(make.Assign(make.Indexed(make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_OBJECTS)), 
    			make.Unary(POSTINC, 
    					make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_TAIL)))), 
    					make.Ident(n))));
    	stats.add(make.If(make.Binary(GE, make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_TAIL)), 
    			make.Select(make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_OBJECTS)), 
    					names.fromString("length"))), 
    			make.Exec(make.Assign(
    					make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_TAIL)), 
    					make.Literal(0))), 
    			null));
    	return stats.toList();
    }
    
    public ListBuffer<JCTree> translateSerialCapsule(JCCapsuleDecl tree, CapsuleSymbol c, Env<AttrContext> localEnv){
    	tree.extending = make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_SEQUENTIAL));
        ListBuffer<JCTree> definitions = new ListBuffer<JCTree>();
        for(int i=0;i<tree.defs.length();i++){
        	if(tree.defs.get(i).getTag() == Tag.METHODDEF){
        		JCMethodDecl mdecl = (JCMethodDecl)tree.defs.get(i);
        		if(mdecl.name.toString().equals("run")&&mdecl.params.isEmpty()){
        			log.error(tree.pos(), "serialize.active.capsules");
        		}else if((mdecl.mods.flags & PRIVATE) ==0
        					&&(mdecl.mods.flags & PROTECTED) ==0){
                    JCProcDecl p = make.ProcDef(make.Modifiers(PUBLIC|FINAL),
                    		mdecl.name,
                    		mdecl.restype,
                    		mdecl.typarams,
                    		mdecl.params,
                    		mdecl.thrown,
                    		mdecl.body,
                    		null
                    		);
                    p.switchToMethod();
                    tree.publicMethods = tree.publicMethods.append(p);
        		}else
        			definitions.add(mdecl);
        	} else if(tree.defs.get(i).getTag() == VARDEF){
    			JCVariableDecl mdecl = (JCVariableDecl)tree.defs.get(i);
    			if(mdecl.mods.flags!=0)
    				log.error(mdecl.pos(), "illegal.state.modifiers");
    			mdecl.mods.flags |=PRIVATE;
    			JCStateDecl state = make.at(mdecl.pos).StateDef(make.Modifiers(PRIVATE), mdecl.name, mdecl.vartype, mdecl.init);
    			state.switchToVar();
    			definitions.add(state);
    		}else definitions.add(tree.defs.get(i));
        }
        c.definedRun = false;
        for(JCMethodDecl d : tree.publicMethods){
    		definitions.add(d);
    	}
        tree.needsDefaultRun = false;
        return definitions;
    }
    
    public ListBuffer<JCTree> translateMonitorCapsule(JCCapsuleDecl tree, CapsuleSymbol c, Env<AttrContext> localEnv){
    	tree.extending = make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_SEQUENTIAL));
        ListBuffer<JCTree> definitions = new ListBuffer<JCTree>();
        for(int i=0;i<tree.defs.length();i++){
        	if(tree.defs.get(i).getTag() == Tag.METHODDEF){
        		JCMethodDecl mdecl = (JCMethodDecl)tree.defs.get(i);
        		if(mdecl.name.toString().equals("run")&&mdecl.params.isEmpty()){
        			log.error(tree.pos(), "serialize.active.capsules");
        		}else if((mdecl.mods.flags & PRIVATE) ==0
        					&&(mdecl.mods.flags & PROTECTED) ==0){
                    JCProcDecl p = make.ProcDef(make.Modifiers(PUBLIC|Flags.SYNCHRONIZED|FINAL),
                    		mdecl.name,
                    		mdecl.restype,
                    		mdecl.typarams,
                    		mdecl.params,
                    		mdecl.thrown,
                    		mdecl.body,
                    		null
                    		);
                    p.switchToMethod();
                    tree.publicMethods = tree.publicMethods.append(p);
        		}else
        			definitions.add(mdecl);
        	} else if(tree.defs.get(i).getTag() == VARDEF){
    			JCVariableDecl mdecl = (JCVariableDecl)tree.defs.get(i);
    			if(mdecl.mods.flags!=0)
    				log.error(mdecl.pos(), "illegal.state.modifiers");
    			mdecl.mods.flags |=PRIVATE;
    			JCStateDecl state = make.at(mdecl.pos).StateDef(make.Modifiers(PRIVATE), mdecl.name, mdecl.vartype, mdecl.init);
    			state.switchToVar();
    			definitions.add(state);
    		}else definitions.add(tree.defs.get(i));
        }
        c.definedRun = false;
        for(JCMethodDecl d : tree.publicMethods){
    		definitions.add(d);
    	}
        tree.needsDefaultRun = false;
        return definitions;
    }
    
    public ListBuffer<JCTree> translateActiveCapsule(JCCapsuleDecl tree, CapsuleSymbol c, Env<AttrContext> localEnv){
    	tree.extending = make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_THREAD));
    	int indexer = 0;
        boolean hasRun = false;
        ListBuffer<JCTree> definitions = new ListBuffer<JCTree>();
        TreeCopier<Void> tc = new TreeCopier<Void>(make);
        for(int i=0;i<tree.defs.length();i++){
        	if(tree.defs.get(i).getTag() == Tag.METHODDEF){
        		JCMethodDecl mdecl = (JCMethodDecl)tree.defs.get(i);
        		if(mdecl.name.toString().equals("run")&&mdecl.params.isEmpty()){
            		MethodSymbol msym = new MethodSymbol(
            				PUBLIC|FINAL,
            				names.fromString("run"),
            				new MethodType(
            						List.<Type>nil(),
            						syms.voidType,
            						List.<Type>nil(),
            						syms.methodClass
            						),
            						tree.sym
            				);
            		JCMethodDecl computeDecl = make.MethodDef(msym,
            				mdecl.body);
            		computeDecl.params = List.<JCVariableDecl>nil();
            		memberEnter.memberEnter(computeDecl, localEnv);
            		definitions.add(computeDecl);
                    tree.computeMethod = computeDecl;
            		hasRun=true;
        		}
        		else if((mdecl.mods.flags & PRIVATE) ==0
        					&&(mdecl.mods.flags & PROTECTED) ==0){
        			String constantName = PaniniConstants.PANINI_METHOD_CONST + mdecl.name.toString();
            		if(mdecl.params.nonEmpty())
            			for(JCVariableDecl param: mdecl.params){
            				constantName = constantName + "$" + param.vartype.toString();
            			}
    				mdecl.mods.flags |= PUBLIC;
    				JCVariableDecl v = make.VarDef(make.Modifiers(PUBLIC | STATIC | FINAL),
    						names.fromString(constantName),
    						make.TypeIdent(TypeTags.INT),
    					    make.Literal(indexer++));
                    JCProcDecl p = make.ProcDef(make.Modifiers(PUBLIC),
                    		mdecl.name,
                    		tc.copy(mdecl.restype),
                    		tc.copy(mdecl.typarams),
                    		tc.copy(mdecl.params),
                    		tc.copy(mdecl.thrown),
                    		tc.copy(mdecl.body),
                    		null
                    		);
                    p.mods.annotations = tc.copy(mdecl.mods.annotations);
                    p.switchToMethod();
                    definitions.prepend(v);
                    tree.publicMethods = tree.publicMethods.append(p);
        		}else 
        			definitions.add(tc.copy(mdecl));
        	} else if(tree.defs.get(i).getTag() == VARDEF){
    			JCVariableDecl mdecl = (JCVariableDecl)tree.defs.get(i);
    			if(mdecl.mods.flags!=0)
    				log.error(mdecl.pos(), "illegal.state.modifiers");
    			if(mdecl.init ==null)
    				log.warning(mdecl.pos(), "state.not.initialized");
    			mdecl.mods.flags |=PRIVATE;
    			JCStateDecl state = make.at(mdecl.pos).StateDef(make.Modifiers(PRIVATE), mdecl.name, tc.copy(mdecl.vartype), tc.copy(mdecl.init));
    			state.switchToVar();
    			definitions.add(state);
    		}else definitions.add(tc.copy(tree.defs.get(i)));
        }
        if(!hasRun){
        	for(JCMethodDecl mdecl : tree.publicMethods){
        		String constantName = PaniniConstants.PANINI_METHOD_CONST + mdecl.name.toString();
        		if(mdecl.params.nonEmpty())
        			for(JCVariableDecl param: mdecl.params){
        				constantName = constantName + "$" + param.vartype.toString();
        			}
        		c.definedRun = false;
	        	ListBuffer<JCStatement> copyBody = new ListBuffer<JCStatement>();
	        	copyBody.append(make.Exec(make.Apply(List.<JCExpression>nil(), make.Ident(names.fromString(PaniniConstants.PANINI_PUSH)), List.<JCExpression>of(make.Ident(names.fromString(PaniniConstants.PANINI_DUCK_TYPE))))));
	        	ListBuffer<JCVariableDecl> vars = new ListBuffer<JCVariableDecl>();
	        	ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
	        	args.add(make.Ident(names.fromString(constantName)));
	            for(JCVariableDecl v : mdecl.params){
	            	vars.add(make.VarDef(tc.copy(v.mods), v.name, tc.copy(v.vartype), null));
	            	args.append(make.Ident(v.name));
	            }
	            JCExpression duckType = getDuckType(tree, mdecl);
				copyBody.prepend(make.Try(
						make.Block(
								0,
								List.<JCStatement> of(make.Exec(make.Assign(
										make.Ident(names
												.fromString(PaniniConstants.PANINI_DUCK_TYPE)),
										make.NewClass(null, List
												.<JCExpression> nil(), duckType,
												args.toList(), null))))),
						List.<JCCatch> of(make.Catch(
								make.VarDef(make.Modifiers(0), names
										.fromString("e"), make.Ident(names
										.fromString("Exception")), null),
								make.Block(
										0,
										List.<JCStatement> of(make.Throw(make.NewClass(
												null,
												List.<JCExpression> nil(),
												make.Ident(names
														.fromString("DuckException")),
												List.<JCExpression> of(make.Ident(names
														.fromString("e"))),
												null))))// this is catch clause
														// body
						)), null));
				copyBody.prepend(make.VarDef(make.Modifiers(0),
						names.fromString(PaniniConstants.PANINI_DUCK_TYPE),
						duckType,
						make.Literal(TypeTags.BOT, null)));
	            if(!mdecl.restype.toString().equals("void"))
	            	copyBody.append(procedureReturnStatement(mdecl));
	            
	            JCMethodDecl methodCopy = make.MethodDef(
	            		make.Modifiers(PRIVATE|FINAL), 
	            		mdecl.name.append(names.fromString("$Original")), 
	            		tc.copy(mdecl.restype), 
	            		tc.copy(mdecl.typarams), 
	            		vars.toList(),
	            		tc.copy(mdecl.thrown), 
	            		tc.copy(mdecl.body), 
	            		null);
	            methodCopy.sym = new MethodSymbol(PRIVATE, methodCopy.name, mdecl.restype.type, tree.sym);
	            mdecl.mods.flags |= FINAL;
	            mdecl.body = make.Block(0, copyBody.toList());
	            definitions.add(methodCopy);
	            definitions.add(mdecl);
        	}
            MethodSymbol msym = new MethodSymbol(
                PUBLIC|FINAL,
                names.fromString("run"),
                new MethodType(
                    List.<Type>nil(),
                    syms.voidType,
                    List.<Type>nil(),
                    syms.methodClass
                    ),
                tree.sym
                );
			JCMethodDecl m = make.MethodDef(msym,
                                            make.Block(0, List.<JCStatement>nil()));
			m.mods = make.Modifiers(PUBLIC|FINAL, List.<JCAnnotation>of(make.Annotation(make.Ident(names.fromString("SuppressWarnings")), 
        			List.<JCExpression>of(make.Literal("unchecked")))));
            m.params = List.<JCVariableDecl>nil();
            m.sym = msym;
            memberEnter.memberEnter(m, localEnv);
            definitions.add(m);
    		hasRun=true;
            tree.needsDefaultRun = true;
            tree.computeMethod = m;
    	}
        else{
        	c.definedRun = true;
        	for(JCMethodDecl d : tree.publicMethods){
        		definitions.add(d);
        	}

        	for(JCMethodDecl mdecl : tree.publicMethods){
                ListBuffer<JCVariableDecl> vars = new ListBuffer<JCVariableDecl>();
	        	ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
	        	args.add(make.Ident(names.fromString(PaniniConstants.PANINI_METHOD_CONST + mdecl.name.toString())));
	            for(JCVariableDecl v : mdecl.params){
	            	vars.add(make.VarDef(v.mods, v.name, v.vartype, null));
	            	args.append(make.Ident(v.name));
	            }

	            JCMethodDecl methodCopy = make.MethodDef(
	            		make.Modifiers(PRIVATE|FINAL), 
	            		mdecl.name.append(names.fromString("$Original")), 
	            		tc.copy(mdecl.restype),
	            		tc.copy(mdecl.typarams), 
	            		tc.copy(vars.toList()),
	            		tc.copy(mdecl.thrown), 
	            		tc.copy(mdecl.body)
,
	            		null);
	            methodCopy.sym = new MethodSymbol(PRIVATE, methodCopy.name, mdecl.restype.type, tree.sym);
	            definitions.add(methodCopy);
        	}
        	//add from public methods
        }
        return definitions;
    }
    
    public ListBuffer<JCTree> translateTaskCapsule(JCCapsuleDecl tree, CapsuleSymbol c, Env<AttrContext> localEnv){
    	tree.extending = make.Ident(names.fromString(PaniniConstants.PANINI_CAPSULE_TASK));
    	int indexer = 0;
        boolean hasRun = false;
        ListBuffer<JCTree> definitions = new ListBuffer<JCTree>();
        TreeCopier<Void> tc = new TreeCopier<Void>(make);
        for(int i=0;i<tree.defs.length();i++){
        	if(tree.defs.get(i).getTag() == Tag.METHODDEF){
        		JCMethodDecl mdecl = (JCMethodDecl)tree.defs.get(i);
        		if(mdecl.name.toString().equals("run")&&mdecl.params.isEmpty()){
        			log.error(tree.pos(), "serialize.active.capsules");
        		}
        		else if((mdecl.mods.flags & PRIVATE) ==0
        					&&(mdecl.mods.flags & PROTECTED) ==0){
        			String constantName = PaniniConstants.PANINI_METHOD_CONST + mdecl.name.toString();
            		if(mdecl.params.nonEmpty())
            			for(JCVariableDecl param: mdecl.params){
            				constantName = constantName + "$" + param.vartype.toString();
            			}
    				mdecl.mods.flags |= PUBLIC;
    				JCVariableDecl v = make.VarDef(make.Modifiers(PUBLIC | STATIC | FINAL),
    						names.fromString(constantName),
    						make.TypeIdent(TypeTags.INT),
    					    make.Literal(indexer++));
                    JCProcDecl p = make.ProcDef(make.Modifiers(PUBLIC|FINAL),
                    		mdecl.name,
                    		tc.copy(mdecl.restype),
                    		tc.copy(mdecl.typarams),
                    		tc.copy(mdecl.params),
                    		tc.copy(mdecl.thrown),
                    		tc.copy(mdecl.body),
                    		null
                    		);
                    p.mods.annotations = tc.copy(mdecl.mods.annotations);
                    p.switchToMethod();
                    definitions.prepend(v);
                    tree.publicMethods = tree.publicMethods.append(p);
        		}else 
        			definitions.add(mdecl);
        	} else if(tree.defs.get(i).getTag() == VARDEF){
    			JCVariableDecl mdecl = (JCVariableDecl)tree.defs.get(i);
    			if(mdecl.mods.flags!=0)
    				log.error(mdecl.pos(), "illegal.state.modifiers");
    			mdecl.mods.flags |=PRIVATE;
    			JCStateDecl state = make.at(mdecl.pos).StateDef(make.Modifiers(PRIVATE), mdecl.name, mdecl.vartype, mdecl.init);
    			state.switchToVar();
    			definitions.add(state);
    		}else definitions.add(tree.defs.get(i));
        }
        if(!hasRun){
        	for(JCMethodDecl mdecl : tree.publicMethods){
        		String constantName = PaniniConstants.PANINI_METHOD_CONST + mdecl.name.toString();
        		if(mdecl.params.nonEmpty())
        			for(JCVariableDecl param: mdecl.params){
        				constantName = constantName + "$" + param.vartype.toString();
        			}
        		c.definedRun = false;
	        	ListBuffer<JCStatement> copyBody = new ListBuffer<JCStatement>();
	        	copyBody.append(make.Exec(make.Apply(List.<JCExpression>nil(), make.Ident(names.fromString(PaniniConstants.PANINI_PUSH)), List.<JCExpression>of(make.Ident(names.fromString(PaniniConstants.PANINI_DUCK_TYPE))))));
	        	ListBuffer<JCVariableDecl> vars = new ListBuffer<JCVariableDecl>();
	        	ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
	        	args.add(make.Ident(names.fromString(constantName)));
	            for(JCVariableDecl v : mdecl.params){
	            	vars.add(make.VarDef(v.mods, v.name, v.vartype, null));
	            	args.append(make.Ident(v.name));
	            }
	            JCExpression duckType = getDuckType(tree, mdecl);
				copyBody.prepend(make.Try(
						make.Block(
								0,
								List.<JCStatement> of(make.Exec(make.Assign(
										make.Ident(names
												.fromString(PaniniConstants.PANINI_DUCK_TYPE)),
										make.NewClass(null, List
												.<JCExpression> nil(), duckType,
												args.toList(), null))))),
						List.<JCCatch> of(make.Catch(
								make.VarDef(make.Modifiers(0), names
										.fromString("e"), make.Ident(names
										.fromString("Exception")), null),
								make.Block(
										0,
										List.<JCStatement> of(make.Throw(make.NewClass(
												null,
												List.<JCExpression> nil(),
												make.Ident(names
														.fromString("DuckException")),
												List.<JCExpression> of(make.Ident(names
														.fromString("e"))),
												null))))// this is catch clause
														// body
						)), null));
				copyBody.prepend(make.VarDef(make.Modifiers(0),
						names.fromString(PaniniConstants.PANINI_DUCK_TYPE),
						duckType,
						make.Literal(TypeTags.BOT, null)));
				if(!mdecl.restype.toString().equals("void")){
	            	copyBody.append(procedureReturnStatement(mdecl));
	            }
	            
	            JCMethodDecl methodCopy = make.MethodDef(
	            		make.Modifiers(PRIVATE|FINAL), 
	            		mdecl.name.append(names.fromString("$Original")), 
	            		tc.copy(mdecl.restype), 
	            		tc.copy(mdecl.typarams), 
	            		vars.toList(),
	            		tc.copy(mdecl.thrown), 
	            		tc.copy(mdecl.body), 
	            		null);
	            methodCopy.sym = new MethodSymbol(PRIVATE, methodCopy.name, mdecl.restype.type, tree.sym);
	            mdecl.mods.flags |= FINAL;
	            mdecl.body = make.Block(0, copyBody.toList());
	            definitions.add(methodCopy);
	            definitions.add(mdecl);
        	}
            MethodSymbol msym = new MethodSymbol(
                PUBLIC|FINAL,
                names.fromString("run"),
                new MethodType(
                    List.<Type>nil(),
                    syms.booleanType,
                    List.<Type>nil(),
                    syms.methodClass
                    ),
                tree.sym
                );
			JCMethodDecl m = make.MethodDef(msym,
                                            make.Block(0, List.<JCStatement>nil()));
			m.mods = make.Modifiers(PUBLIC|FINAL, List.<JCAnnotation>of(make.Annotation(make.Ident(names.fromString("SuppressWarnings")), 
        			List.<JCExpression>of(make.Literal("unchecked")))));
            m.params = List.<JCVariableDecl>nil();
            m.sym = msym;
            memberEnter.memberEnter(m, localEnv);
            definitions.add(m);
    		hasRun=true;
            tree.needsDefaultRun = true;
            tree.computeMethod = m;
    	}
        else{
        	c.definedRun = true;
        	for(JCMethodDecl d : tree.publicMethods){
        		definitions.add(d);
        	}

        	for(JCMethodDecl mdecl : tree.publicMethods){
                ListBuffer<JCVariableDecl> vars = new ListBuffer<JCVariableDecl>();
	        	ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
	        	args.add(make.Ident(names.fromString(PaniniConstants.PANINI_METHOD_CONST + mdecl.name.toString())));
	            for(JCVariableDecl v : mdecl.params){
	            	vars.add(make.VarDef(v.mods, v.name, v.vartype, null));
	            	args.append(make.Ident(v.name));
	            }

	            JCMethodDecl methodCopy = make.MethodDef(
	            		make.Modifiers(PRIVATE|FINAL), 
	            		mdecl.name.append(names.fromString("$Original")), 
	            		tc.copy(mdecl.restype),
	            		tc.copy(mdecl.typarams), 
	            		tc.copy(vars.toList()),
	            		tc.copy(mdecl.thrown), 
	            		tc.copy(mdecl.body)
,
	            		null);
	            methodCopy.sym = new MethodSymbol(PRIVATE, methodCopy.name, mdecl.restype.type, tree.sym);
	            definitions.add(methodCopy);
        	}

        	//add from public methods
        }
        return definitions;
    }
    
    public long processCapsuleAnnotations(JCCapsuleDecl tree, ClassSymbol c){
    	for(JCAnnotation annotation : tree.mods.annotations){
    		if(annotation.annotationType.toString().equals("CapsuleKind")){
    			Object arg = "";
    			if(annotation.args.isEmpty())
    				log.error(tree.pos(), "annotation.missing.default.value", annotation, "value");
    			else if (annotation.args.size()!=1||annotation.args.head.getTag()!=ASSIGN){
    				if(annotation.args.head.getTag()==LITERAL)
    					arg = ((JCLiteral)annotation.args.head).value;
    				else
    					log.error(tree.pos(), "annotation.value.must.be.name.value");
    			}
    			return getCapsuleKind(arg, c, annotation);
    		}
    	}
    	return c.flags_field;
    }
    
    public long getCapsuleKind(Object kind, ClassSymbol c, JCAnnotation annotation){
		if (kind.equals("SERIAL"))
			return c.flags_field |= SERIAL;
		else if (kind.equals("ACTIVE"))
			return c.flags_field |= ACTIVE;
		else if (kind.equals("TASK"))
			return c.flags_field |= TASK;
		else if (kind.equals("MONITOR"))
			return c.flags_field |= MONITOR;
		else
			log.error(annotation.pos(), "annotation.value.not.allowable.type");
		return c.flags_field;
    }
    
    private JCStatement procedureReturnStatement(final JCMethodDecl mdecl){
		String returnType = mdecl.restype.toString();
		JCStatement returnStat;
		if (returnType.equals("long")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(names
							.fromString(PaniniConstants.PANINI_DUCK_TYPE)),
							names.fromString("longValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("boolean")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(names
							.fromString(PaniniConstants.PANINI_DUCK_TYPE)),
							names.fromString("booleanValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("byte")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(names
							.fromString(PaniniConstants.PANINI_DUCK_TYPE)),
							names.fromString("byteValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("char")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(names
							.fromString(PaniniConstants.PANINI_DUCK_TYPE)),
							names.fromString("charValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("double")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(names
							.fromString(PaniniConstants.PANINI_DUCK_TYPE)),
							names.fromString("doubleValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("float")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(names
							.fromString(PaniniConstants.PANINI_DUCK_TYPE)),
							names.fromString("floatValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("int")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(names
							.fromString(PaniniConstants.PANINI_DUCK_TYPE)),
							names.fromString("intValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("short")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(names
							.fromString(PaniniConstants.PANINI_DUCK_TYPE)),
							names.fromString("shortValue")), List
					.<JCExpression> nil()));
		} else if (returnType.equals("String")) {
			returnStat = make.Return(make.Apply(List.<JCExpression> nil(), make
					.Select(make.Ident(names
							.fromString(PaniniConstants.PANINI_DUCK_TYPE)),
							names.fromString("toString")), List
					.<JCExpression> nil()));
		} else {
			returnStat = make.Return(make.Ident(names
					.fromString(PaniniConstants.PANINI_DUCK_TYPE)));
		}
		return returnStat;
    }
    
    private JCExpression getDuckType(final JCCapsuleDecl tree, final JCMethodDecl mdecl){
    	String returnType = mdecl.restype.toString();
		JCExpression duck = null;
		if (returnType.equals("long")) {
			JCExpression longExp = make.Ident(names.fromString("org"));
			longExp = make.Select(longExp, names.fromString("paninij"));
			longExp = make.Select(longExp, names.fromString("lang"));
			longExp = make.Select(longExp, names.fromString("Long"));
			duck = longExp;
		} else if (returnType.equals("boolean")) {
			JCExpression booleanExp = make.Ident(names.fromString("org"));
			booleanExp = make.Select(booleanExp, names.fromString("paninij"));
			booleanExp = make.Select(booleanExp, names.fromString("lang"));
			booleanExp = make.Select(booleanExp, names.fromString("Boolean"));
			duck = booleanExp;
		} else if (returnType.equals("byte")) {
			JCExpression byteExp = make.Ident(names.fromString("org"));
			byteExp = make.Select(byteExp, names.fromString("paninij"));
			byteExp = make.Select(byteExp, names.fromString("lang"));
			byteExp = make.Select(byteExp, names.fromString("Byte"));
			duck = byteExp;
		} else if (returnType.equals("char")) {
			JCExpression charExp = make.Ident(names.fromString("org"));
			charExp = make.Select(charExp, names.fromString("paninij"));
			charExp = make.Select(charExp, names.fromString("lang"));
			charExp = make.Select(charExp, names.fromString("Character"));
			duck = charExp;
		} else if (returnType.equals("double")) {
			JCExpression doubleExp = make.Ident(names.fromString("org"));
			doubleExp = make.Select(doubleExp, names.fromString("paninij"));
			doubleExp = make.Select(doubleExp, names.fromString("lang"));
			doubleExp = make.Select(doubleExp, names.fromString("Double"));
			duck = doubleExp;
		} else if (returnType.equals("float")) {
			JCExpression floatExp = make.Ident(names.fromString("org"));
			floatExp = make.Select(floatExp, names.fromString("paninij"));
			floatExp = make.Select(floatExp, names.fromString("lang"));
			floatExp = make.Select(floatExp, names.fromString("Float"));
			duck = floatExp;
		} else if (returnType.equals("int")) {
			JCExpression intExp = make.Ident(names.fromString("org"));
			intExp = make.Select(intExp, names.fromString("paninij"));
			intExp = make.Select(intExp, names.fromString("lang"));
			intExp = make.Select(intExp, names.fromString("Integer"));
			duck = intExp;
		} else if (returnType.equals("short")) {
			JCExpression shortExp = make.Ident(names.fromString("org"));
			shortExp = make.Select(shortExp, names.fromString("paninij"));
			shortExp = make.Select(shortExp, names.fromString("lang"));
			shortExp = make.Select(shortExp, names.fromString("Short"));
			duck = shortExp;
		} else if (returnType.equals("String")) {
			JCExpression stringExp = make.Ident(names.fromString("org"));
			stringExp = make.Select(stringExp, names.fromString("paninij"));
			stringExp = make.Select(stringExp, names.fromString("lang"));
			stringExp = make.Select(stringExp, names.fromString("String"));
			duck = stringExp;
		} else
			duck = make.Ident(names
					.fromString(PaniniConstants.DUCK_INTERFACE_NAME + "$"
							+ mdecl.restype.toString() + "$"
							+ tree.name.toString()));
		return duck;
    }
    
    private void translateCapsuleAnnotations(){
    	Set<Map.Entry<Name, ClassSymbol>> classSymbols = new HashSet<Map.Entry<Name, ClassSymbol>>(syms.classes.entrySet());
    	
    	for(Map.Entry<Name, ClassSymbol> entry : classSymbols){
    		ClassSymbol classSymbol = entry.getValue();
    		if(classSymbol.classfile!=null)
    			if(classSymbol.classfile.getKind()== JavaFileObject.Kind.CLASS){
    				classSymbol.complete();
    			}
    		if(classSymbol.attributes_field.size()!=0){
    			for(Attribute.Compound compound : classSymbol.attributes_field){
    				if(compound.type.tsym.getQualifiedName().toString().contains("PaniniCapsuleDecl")){
    					CapsuleSymbol capsuleSymbol;
    					if(classSymbol instanceof CapsuleSymbol)
    						capsuleSymbol = (CapsuleSymbol)classSymbol;
    					else
    						capsuleSymbol = CapsuleSymbol.fromClassSymbol(classSymbol);
    					annotationProcessor.translateCapsuleAnnotations(capsuleSymbol, compound);
	    				syms.capsules.put(capsuleSymbol.name, capsuleSymbol);
    				}
    			}
    		}
    	}
    }
    
    private void fillInCapsuleSymbolRest(){
    	for(CapsuleSymbol capsule : syms.capsules.values()){
    		CapsuleSymbol c = syms.capsules.get(names.fromString(capsule+"$thread"));
    		if(c!=null){
    			capsule.translated_thread = c;
    			c.parentCapsule = capsule;
    		}
    		c = syms.capsules.get(names.fromString(capsule+"$task"));
    		if(c!=null){
    			capsule.translated_task = c;
    			c.parentCapsule = capsule;
    		}
    		c = syms.capsules.get(names.fromString(capsule+"$monitor"));
    		if(c!=null){
    			capsule.translated_monitor = c;
    			c.parentCapsule = capsule;
    		}
    		c = syms.capsules.get(names.fromString(capsule+"$serial"));
    		if(c!=null){
    			capsule.translated_serial = c;
    			c.parentCapsule = capsule;
    		}
    	}
    }
    
    // end Panini code
    /** Main method: enter all classes in a list of toplevel trees.
     *  @param trees      The list of trees to be processed.
     */
    public void main(List<JCCompilationUnit> trees) {
        complete(trees, null);
        
        // Panini code
        translateCapsuleAnnotations();
        fillInCapsuleSymbolRest();
        // end Panini code
    }

    /** Main method: enter one class from a list of toplevel trees and
     *  place the rest on uncompleted for later processing.
     *  @param trees      The list of trees to be processed.
     *  @param c          The class symbol to be processed.
     */
    public void complete(List<JCCompilationUnit> trees, ClassSymbol c) {
        annotate.enterStart();
        ListBuffer<ClassSymbol> prevUncompleted = uncompleted;
        if (memberEnter.completionEnabled) uncompleted = new ListBuffer<ClassSymbol>();

        try {
            // enter all classes, and construct uncompleted list
            classEnter(trees, null);

            // complete all uncompleted classes in memberEnter
            if  (memberEnter.completionEnabled) {
                while (uncompleted.nonEmpty()) {
                    ClassSymbol clazz = uncompleted.next();
                    if (c == null || c == clazz || prevUncompleted == null)
                        clazz.complete();
                    else
                        // defer
                        prevUncompleted.append(clazz);
                }

                // if there remain any unimported toplevels (these must have
                // no classes at all), process their import statements as well.
                for (JCCompilationUnit tree : trees) {
                    if (tree.starImportScope.elems == null) {
                        JavaFileObject prev = log.useSource(tree.sourcefile);
                        Env<AttrContext> topEnv = topLevelEnv(tree);
                        memberEnter.memberEnter(tree, topEnv);
                        log.useSource(prev);
                    }
                }
            }
        } finally {
            uncompleted = prevUncompleted;
            annotate.enterDone();
        }
    }
}
