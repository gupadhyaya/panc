package org.paninij.systemgraphs;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.code.Type.*;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Collection;
import java.util.HashSet;

// encapsulates two graphs, one with system-specified module connections as
// edges, and one with inter-procedure calls as edges. Both graphs use
// system-specified module instances as nodes.
public class SystemGraphs {
    public static class Node { // a module instance
        public ClassSymbol sym; public String name;
        public Node(ClassSymbol sym, String name) { this.sym = sym; this.name = name; }
        public String toString() { return sym + " " + name; }
    }
    public static class ProcEdge {
        public String varName;
        public MethodSymbol caller, called;
        public Node from, to;
        public ProcEdge(Node from, Node to, 
                        MethodSymbol caller,
                        MethodSymbol called,
                        String varName) {
            this.from = from; this.to = to; 
            this.caller = caller; this.called = called;
            this.varName = varName;
        }
        public String toString() {
            return from + " -> " + to + " [label=" + called + "]";
        }
    }
    public static class ConnectionEdge {
        public String varName;
        public Node from, to;
        public ConnectionEdge(Node from, Node to, String varName) { this.from = from; this.to = to; this.varName = varName; }
        public String toString() { return from + " -> " + to; }
    }

    public HashMap<Node, HashSet<ProcEdge>> forwardProcEdges = new HashMap<Node, HashSet<ProcEdge>>();
    public HashMap<Node, HashSet<ConnectionEdge>> forwardConnectionEdges = new HashMap<Node, HashSet<ConnectionEdge>>();

    public Node addModule(ClassSymbol sym, String name) {
        Node n = new Node(sym, name);
        forwardProcEdges.put(n, new HashSet<ProcEdge>());
        forwardConnectionEdges.put(n, new HashSet<ConnectionEdge>());
        return n;
    }

    public void addConnectionEdge(Node from, Node to, String name) {
        forwardConnectionEdges.get(from).add(new ConnectionEdge(from, to, name));
    }

    public void addProcEdge(Node from, Node to, MethodSymbol caller, 
                            MethodSymbol called, String varName) {
        forwardProcEdges.get(from).add(new ProcEdge(from, to, caller, called, 
                                                    varName));
    }

    public String toString() {
        String returnValue = "digraph G {\n";
        for (Collection<ProcEdge> edges : forwardProcEdges.values()) {
            for (ProcEdge edge : edges) {
                returnValue += edge + "\n";
            }
        }
        returnValue += "}";
        return returnValue;
    }
}