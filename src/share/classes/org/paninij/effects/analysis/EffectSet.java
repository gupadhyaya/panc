package org.paninij.effects.analysis;

import java.util.*;

import org.paninij.path.*;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;

public class EffectSet {
	// detect whether the method always return newly created object.
	public boolean returnNewObject;

	public boolean commute;
	public HashSet<EffectEntry> read;
	public HashSet<EffectEntry> write;
	public HashSet<CallEffect> calls;
	public boolean isBottom;

	public HashSet<Symbol> writtenLocals;
	public HashSet<Symbol> writtenFields;
	public boolean isWriteBottom;

	boolean isInit = false;

	public EffectSet() {
		read = new HashSet<EffectEntry>();
		write = new HashSet<EffectEntry>();
		calls = new HashSet<CallEffect>();
		writtenLocals = new HashSet<Symbol>();
		writtenFields = new HashSet<Symbol>();
		isWriteBottom = false;
		isBottom = false;
	}

	public EffectSet(boolean b) {
		this();
		isInit = b;
	}

	public EffectSet(EffectSet x) {
		read = new HashSet<EffectEntry>(x.read);
		write = new HashSet<EffectEntry>(x.write);
		calls = new HashSet<CallEffect>(x.calls);
		writtenLocals = new HashSet<Symbol>(x.writtenLocals);
		writtenFields = new HashSet<Symbol>(x.writtenFields);
		isWriteBottom = x.isWriteBottom;
		isBottom = x.isBottom;
		if (x.isInit) { isInit = true; } 
	}

	public void init(EffectSet x) {
		read = new HashSet<EffectEntry>(x.read);
		write = new HashSet<EffectEntry>(x.write);
		calls = new HashSet<CallEffect>(x.calls);
		writtenLocals = new HashSet<Symbol>(x.writtenLocals);
		writtenFields = new HashSet<Symbol>(x.writtenFields);
		isWriteBottom = x.isWriteBottom;
		isBottom = x.isBottom;
		if (x.isInit) { isInit = true; } 
	}

	public int hashCode() {
		return read.hashCode() + write.hashCode() + calls.hashCode() +
		    writtenFields.hashCode() + writtenLocals.hashCode();
	}

	public boolean equals(Object o) {
		if (o instanceof EffectSet){ 
			EffectSet g = (EffectSet)o;
			if (!g.isInit && (!isInit)) { return true; }
			if (g.isInit){
				if (isBottom && g.isBottom) { return true; }
				return isBottom == g.isBottom && write.equals(g.write) &&
				read.equals(g.read) && calls.equals(g.calls) &&
				writtenLocals.equals(g.writtenLocals) &&
				writtenFields.equals(g.writtenFields) && isBottom == g.isBottom
				&& returnNewObject == g.returnNewObject;
			}
		}
		return false;
	}

	public void union(EffectSet x) {
		if (x.isInit) {
			if (isInit) {
				if (!isBottom) {
					if (x.isBottom) {
						makeButtom();
					} else {
						calls.addAll(x.calls);
						read.addAll(x.read);
						write.addAll(x.write);
						writtenLocals.addAll(x.writtenLocals);
						writtenFields.addAll(x.writtenFields);
						isWriteBottom |= x.isWriteBottom;
						
						for (Symbol s : writtenLocals) {
							assignVar(s); }
						for (Symbol s : writtenFields) {
							assignField(s); }
						if (isWriteBottom) {
							removedAffectedByUnanalyzable(read);
							removedAffectedByUnanalyzable(write);
						}
					}
				}
			} else {
				isInit = true;
				if (x.isBottom) {
					makeButtom();
				} else {
					read = new HashSet<EffectEntry>(x.read);
					write = new HashSet<EffectEntry>(x.write);
					calls = new HashSet<CallEffect>(x.calls);
					writtenLocals = new HashSet<Symbol>(x.writtenLocals);
					writtenFields = new HashSet<Symbol>(x.writtenFields);
				}
			}
		}
	}

	public void makeButtom() {
		isBottom = true;
		read.clear();
		write.clear();
		calls.clear();
		writtenLocals.clear();
		writtenFields.clear();
		isWriteBottom = true;
	}

	public void makePathButtom() {
		isWriteBottom = true;
		removedAffectedByUnanalyzableBottom();
	}

	public EffectSet finalCompress() {
		compress();
		// compressOpenFieldEffects(calls);
		EffectSet epg = new EffectSet(true);
		epg.read = new HashSet<EffectEntry>(read);
		epg.write = new HashSet<EffectEntry>(write);
		epg.calls = new HashSet<CallEffect>(calls);
		epg.isBottom = isBottom;
		return epg;
	}

	

	/* this method is called to removed redundant effects.
	 * e.g. C.f contains o.f if the type of o is C or subtype of C.
	 * and write contains read. */
	public void compress() {
		if (isBottom) {
			read.clear();
			write.clear();
			calls.clear();
			writtenLocals.clear();
			writtenFields.clear();
			isWriteBottom = true;
			return;
		}
		removeFinalFieldRead(read);
		removeFinalFieldRead(write);

		putStaticFinalFromConcreteToType(read);
		putStaticFinalFromConcreteToType(write);

		HashSet<EffectEntry> toberemoved = new HashSet<EffectEntry>();
		for (EffectEntry rwe1: read) {
			for (EffectEntry rwe2: write) {
				if (rwe1 != rwe2) {
					if (rwe2.equals(rwe1)) {
						toberemoved.add(rwe1);
					}
				} else { toberemoved.add(rwe1); }
			}
		}
		read.removeAll(toberemoved);
	}

	private static void putStaticFinalFromConcreteToType(
			HashSet<EffectEntry> set) {
		HashSet<EffectEntry> toberemoved = new HashSet<EffectEntry>();
		HashSet<EffectEntry> tobeadded = new HashSet<EffectEntry>();
		for (EffectEntry rwe : set) {
			if (rwe instanceof FieldEffect) {
				Symbol f = ((FieldEffect) rwe).f;
				if ((f.flags_field & Flags.STATIC) != 0) {
					toberemoved.add(rwe);
					tobeadded.add(
							new FieldEffect(
									new Path_Class(f.enclClass()), f));
				}
			}
		}
		set.removeAll(toberemoved);
		set.addAll(tobeadded);
	}

	private static void removeFinalFieldRead(HashSet<EffectEntry> set) {
		HashSet<EffectEntry> toberemoved = new HashSet<EffectEntry>();
		for (EffectEntry rwe : set) {
			if (rwe instanceof FieldEffect) {
				Symbol f = ((FieldEffect) rwe).f;
				if ((f.flags_field & Flags.FINAL) != 0) {
					toberemoved.add(rwe);
				}
			}
		}
		set.removeAll(toberemoved);
	}

	public void printEffect() {
		System.out.println("\tisInit = " + isInit);
		if (isBottom) {
			System.out.println("\tbuttom effect");
			return;
		}

		if (read.isEmpty() && write.isEmpty() && calls.isEmpty()) {
			System.out.println("\tpure");
			return;
		}

		if (read.isEmpty()) {
			System.out.println("\treadset empty");
		} else {
			System.out.println("\tread:");
			for (EffectEntry rwe : read) {
				System.out.print("\t\t");
				rwe.printEffect();
			}
		}

		if (write.isEmpty()) {
			System.out.println("\twriteset empty");
		} else {
			System.out.println("\twrite:");
			for (EffectEntry rwe : write) {
				System.out.print("\t\t");
				rwe.printEffect();
			}
		}

		if (calls.isEmpty()) {
			System.out.println("\topen empty");
		} else {
			System.out.println("\topen:");
			for (CallEffect rwe : calls) {
				System.out.print("\t\t");
				rwe.printEffect();
			}
		}
	}

	// f = ...
	public void assignField(Symbol f) {
		if (!isBottom) {
			writtenFields.add(f);
			removedAffectedField(f, read);
			removedAffectedField(f, write);
		}
	}

	// var = ...
	public void assignVar(Symbol var) {
		if (!isBottom) {
			writtenLocals.add(var);
			removedAffectedLocal(var, read);
			removedAffectedLocal(var, write);
		}
	}
	
	private static void removedAffectedLocal(Symbol var,
			HashSet<EffectEntry> processingSet) {
		HashSet<EffectEntry> tobeAdded = new HashSet<EffectEntry>();
		HashSet<EffectEntry> tobeRemoved = new HashSet<EffectEntry>();
		for (EffectEntry rwpe : processingSet) {
			if (rwpe instanceof FieldEffect) {
				FieldEffect rwepf = (FieldEffect)rwpe;
				if (rwepf.path.isAffected_Local(var)) {
					Symbol f = rwepf.f;
					tobeRemoved.add(rwpe);
					tobeAdded.add(
							new FieldEffect(Path_Unknown.pfcUnknow, f));
				}
			} else if (rwpe instanceof ArrayEffect) {
				ArrayEffect rwepf = (ArrayEffect)rwpe;
				if (rwepf.path.isAffected_Local(var)) {
					tobeRemoved.add(rwpe);
					tobeAdded.add(new ArrayEffect(Path_Unknown.pfcUnknow,
									rwepf.type));
				}

			} else throw new Error("should not be something else");
		}
		processingSet.removeAll(tobeRemoved);
		processingSet.addAll(tobeAdded);
	}

	private static void removedAffectedField(Symbol field,
			HashSet<EffectEntry> processingSet) {
		HashSet<EffectEntry> tobeAdded = new HashSet<EffectEntry>();
		HashSet<EffectEntry> tobeRemoved = new HashSet<EffectEntry>();
		for (EffectEntry rwpe : processingSet) {
			if (rwpe instanceof FieldEffect) {
				FieldEffect rwepf = (FieldEffect)rwpe;
				if (rwepf.path.isAffected_Path(field)) {
					Symbol f = rwepf.f;
					tobeRemoved.add(rwpe);
					tobeAdded.add(
							new FieldEffect(Path_Unknown.pfcUnknow, f));
				}
			} else if (rwpe instanceof ArrayEffect) {
				ArrayEffect rwepf = (ArrayEffect)rwpe;
				if (rwepf.path.isAffected_Path(field)) {
					tobeRemoved.add(rwpe);
					tobeAdded.add(new ArrayEffect(Path_Unknown.pfcUnknow,
									rwepf.type));
				}
			} else throw new Error("should not be something else");
		}
		processingSet.removeAll(tobeRemoved);
		processingSet.addAll(tobeAdded);
	}

	private static void removedAffectedByUnanalyzable(
			HashSet<EffectEntry> processingSet) {
		HashSet<EffectEntry> tobeAdded = new HashSet<EffectEntry>();
		HashSet<EffectEntry> tobeRemoved = new HashSet<EffectEntry>();
		for (EffectEntry rwpe : processingSet) {
			if (rwpe instanceof FieldEffect) {
				FieldEffect rwepf = (FieldEffect)rwpe;
				if (rwepf.path.isAffected_byUnanalyzablePath()) {
					Symbol f = rwepf.f;
					tobeRemoved.add(rwpe);
					tobeAdded.add(new FieldEffect(Path_Unknown.pfcUnknow, f));
				}
			} else if (rwpe instanceof ArrayEffect) {
				ArrayEffect rwepf = (ArrayEffect)rwpe;
				if (rwepf.path.isAffected_byUnanalyzablePath()) {
					tobeRemoved.add(rwpe);
					tobeAdded.add(new ArrayEffect(Path_Unknown.pfcUnknow,
									rwepf.type));
				}
			} else throw new Error("should not be something else");
		}
		processingSet.removeAll(tobeRemoved);
		processingSet.addAll(tobeAdded);
	}

	public void removedAffectedByUnanalyzableBottom() {
		removedAffectedByUnanalyzable(read);
		removedAffectedByUnanalyzable(write);
	}

	public void removedAffectedFields(HashSet<Symbol> fields) {
		for (Symbol field:fields) {
			removedAffectedField(field, read);
			removedAffectedField(field, write);
		}
	}
}