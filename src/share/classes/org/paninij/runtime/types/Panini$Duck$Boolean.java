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
 * Contributor(s): Hridesh Rajan
 */

package org.paninij.runtime.types;

public class Panini$Duck$Boolean implements Panini$Duck<Boolean> {

	public Panini$Duck$Boolean(int messageId) {
		super();
		this.messageId = messageId;
	}
		
	public final boolean value() {
		if(!available) get();
		return this.value;
	}

	private boolean value;
	
	public void panini$finish(boolean value) {
		this.value = value;
		this.available = true;
		notifyAll();
	}
	
	public void panini$finish(Boolean value) {
		this.value = value;
		this.available = true;
		notifyAll();
	}

	private boolean available = false;
	private final void get() {
		value_not_yet_set: 
			do {
			try {
				synchronized (this) {
					wait(0);
				}
			} catch (InterruptedException e) {
				continue value_not_yet_set;
			}
		} while (available == false);
	}

	
	
	public final int panini$message$id() {
		return this.messageId;
	}

	private final int messageId; 
	@SuppressWarnings("unused")
	private Panini$Duck$Boolean() { messageId = -1; }
}
