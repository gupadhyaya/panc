package org.paninij.taskgraph;

import java.util.ArrayList;
import java.util.List;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

public class BlockVisitor extends TreeScanner {
	public final List<Invoked> calls;

	public BlockVisitor() {
		this.calls = new ArrayList<Invoked>();
	}

	@Override
	public void visitApply(JCMethodInvocation tree) {
		// figure out if the method invocation is inter-capsular call?
		String connectedCapsule = ""; // connected capsule
		String capsuleMethod = ""; // method of the connected capsule being
									// invoked
		List<JCExpression> args = tree.args;
		Symbol sym = null;
		// capture the symbol
		if (tree.meth instanceof JCFieldAccess) {
			JCFieldAccess meth = (JCFieldAccess) tree.meth;
			sym = meth.sym;
		} else if (tree.meth instanceof JCIdent) {
			JCIdent m = (JCIdent) tree.meth;
			sym = m.sym;
		}
		if (sym != null) {
			if (sym.owner.isCapsule()) {
				if (tree.meth instanceof JCFieldAccess) {
					JCFieldAccess meth = (JCFieldAccess) tree.meth;
					if (meth.selected instanceof JCArrayAccess) {
						JCArrayAccess ms = (JCArrayAccess) meth.selected;
						if ((ms.index instanceof JCIdent) ||
								(ms.index instanceof JCArrayAccess)) {
							connectedCapsule = ms.indexed.toString();
						} else {
							connectedCapsule = ms.indexed + "[" + ms.index
									+ "]";
						}
					} else if (meth.selected instanceof JCIdent) {
						connectedCapsule = ((JCIdent) meth.selected).name
								.toString();
					} else {
						System.out.println("[Unknown] type: " + meth.selected.toString());
					}
				}

				// get method signature
				String methodName = sym.toString();
				if (!methodName.equals("run()")) {
					String[] parts = methodName.split("\\(");
					String tmp = parts[1];
					if (tmp.contains(",")) {
						String[] commaparts = tmp.split(",");
						tmp = "";
						for (String string : commaparts) {
							if (string.contains(".")) {
								String[] dotparts = string.split("\\.");
								tmp += dotparts[dotparts.length - 1];
							} else {
								tmp += string;
							}
							tmp += ",";
						}
						if (tmp.contains(","))
							tmp = tmp.substring(0, tmp.length() - 1);
					} else if (tmp.contains(".")) {
						String[] dotparts = tmp.split("\\.");
						tmp = "";
						tmp += dotparts[dotparts.length - 1];
					}

					methodName = parts[0] + "$Original(" + tmp;
				}
				capsuleMethod = methodName;
			}
		}

		if (connectedCapsule != "" && capsuleMethod != "")
			calls.add(new Invoked(connectedCapsule, capsuleMethod));
	}

	private String getMethodSignature(String method, List<JCVariableDecl> params) {
		StringBuilder sb = new StringBuilder();
		sb.append(method);
		sb.append("(");
		for (JCVariableDecl jcVariableDecl : params) {
			sb.append(jcVariableDecl.vartype);
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1); // remove the last comma
		sb.append(")");
		return sb.toString();
	}

}
