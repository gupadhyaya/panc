package org.paninij.effects.analysis;

import org.paninij.path.Path;

import com.sun.tools.javac.code.Symbol;

public class FieldEffect implements EffectEntry {
	public final Path path; 
	public final Symbol f;

	public FieldEffect(Path path, Symbol f) {
		this.path = path;
		this.f = f;
	}

	public int hashCode() {
		return path.hashCode() + f.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof FieldEffect) {
			FieldEffect rwe = (FieldEffect) obj;
			return f.equals(rwe.f) && path.equals(rwe.path);
		}
		return false;
	}

	public void printEffect(){
	    System.out.println("FieldEffect = " + path.printPath() + "\tf = " + f);
	}
}