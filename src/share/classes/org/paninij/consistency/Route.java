package org.paninij.consistency;

import java.util.ArrayList;

import org.paninij.systemgraph.SystemGraph.Edge;

// auxiliary class used by the SequentialFIFO
public class Route {
	public final ArrayList<ClassMethod> nodes;
	public final ArrayList<Edge> edges;

	public Route() {
		nodes = new ArrayList<ClassMethod>();
		edges = new ArrayList<Edge>();
	}

	public Route(ArrayList<ClassMethod> nodes, ArrayList<Edge> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}

	// clone the route
	public final Route cloneR() {
		return new Route(new ArrayList<ClassMethod>(nodes),
				new ArrayList<Edge>(edges));
	}

	public final int getIndex(ClassMethod node) {
		int size = nodes.size();
		for (int i = 0; i < size; i++) {
			if (nodes.get(i).equals(node)) {
				return i;
			}
		}
		return -1;
	}

	// clone the subpath that starts from node cm
	public final Route clonePrefixPath(ClassMethod cm) {
		Route loop = new Route();
		int size = nodes.size();
		for (int i = 0; i < size; i++) {
			ClassMethod n = nodes.get(i);
			loop.nodes.add(n);

			if (i < size - 1) {
				Edge e = edges.get(i);
				loop.edges.add(e);
				if (n.equals(cm)) { break; }
			}
		}
		return loop;
	}

	// clone the subpath that starts from node cm
	public final Route cloneSubPath(ClassMethod cm) {
		Route loop = new Route();
		int size = nodes.size();
		for (int i = 0; i < size; i++) {
			ClassMethod n = nodes.get(i);
			if (n.equals(cm)) {
				for (; i < size - 1; i++) {
					n = nodes.get(i);
					Edge e = edges.get(i);
					loop.nodes.add(n);
					loop.edges.add(e);
				}
				loop.nodes.add(nodes.get(size - 1));
			}
		}
		return loop;
	}

	public final int size() { return nodes.size(); }

	public final void printRoute() {
		int size = nodes.size();
		for (int i = 0; i < size - 1; i++) {
			ClassMethod cm = nodes.get(i);
			Edge e = edges.get(i);
			System.out.print(cm.cs + "." + cm.node.name.toString() + "." +
					cm.meth + "-");
			System.out.print(e.line + "->");
		}
		ClassMethod cm = nodes.get(size - 1);
		System.out.println(cm.cs + "." + cm.node.name.toString() + "." +
				cm.meth);
	}

	public final String routeStr() {
		String s = new String();
		int size = nodes.size();
		for (int i = 0; i < size - 1; i++) {
			ClassMethod cm = nodes.get(i);
			Edge e = edges.get(i);
			s += cm.cs + "." + cm.node.name.toString() + "." + cm.meth + "-" +
			e.line + "->";
		}
		ClassMethod cm = nodes.get(size - 1);
		s += cm.cs + "." + cm.node.name.toString() + "." + cm.meth;

		int edgeSize = edges.size();
		if (edgeSize == size) {
		    s += "-" + edges.get(edgeSize - 1).line + "->";
		}
		return s;
	}

	public final int hashCode() {
		return nodes.hashCode() + edges.hashCode();
	}

	public final boolean equals(Object obj) {
        if (obj instanceof Route) {
        	Route other = (Route)obj;
        	return nodes.equals(other.nodes) && edges.equals(other.edges);
        }
        return false;
    }
}