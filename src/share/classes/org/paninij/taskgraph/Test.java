package org.paninij.taskgraph;

public class Test {

	static void process(String str) {
		String methodName = "";
		String[] parts = str.split("\\(");
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
		System.out.println(methodName);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String str = "process(String)";
		process(str);
	}

}
