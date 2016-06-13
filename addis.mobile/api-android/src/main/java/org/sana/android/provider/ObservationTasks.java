/**
 * Copyright (c) 2013, Sana
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sana nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL Sana BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.sana.android.provider;

import org.sana.api.task.ObservationTask;

import android.net.Uri;

/**
 * @author Sana Development
 *
 */
public class ObservationTasks {
	/**
     * The MIME type of CONTENT_URI providing a directory of objects of this 
     * type.
     */
	public static final String CONTENT_TYPE = 
		"vnd.android.cursor.dir/org.sana.task.observationTask";
	
	/** The content type of {@link #CONTENT_URI} for a single instance. */
	public static final String CONTENT_ITEM_TYPE = 
		"vnd.android.cursor.item/org.sana.task.observationTask";

	/**
     * The content:// style URI for objects of this type.
     */
	public static final Uri CONTENT_URI = Uri.parse("content://"
			+ Tasks.AUTHORITY + "/observation");
	
	private ObservationTasks(){}
	
	public static interface Contract extends Tasks.Contract<ObservationTask>{
		public static final String ENCOUNTER = "encounter";
		public static final String INSTRUCTION = "instruction";
		public static final String PARENT = "parent";
	}
}
