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

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public interface Panini$Duck {
	
    public void paninifinish(Object t);
    
   	public static class DuckBarrier extends AbstractQueuedSynchronizer {
     public final void get() {
     	try {
    			acquireSharedInterruptibly(1);
    		} catch (InterruptedException e) {
    			// TODO: What should be the semantics here?
    			e.printStackTrace();
    		}          	
     }
   		public final void set() {
   			releaseShared(1);
   		}
   		protected boolean isSignalled() { return getState() != 0; }

     protected int tryAcquireShared(int ignore) {
       return isSignalled()? 1 : -1;
     }
       
     protected boolean tryReleaseShared(int ignore) {
       setState(1);
       return true;
     }
   		private static final long serialVersionUID = 221340458078375076L;
    }

}