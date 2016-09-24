package org.sana.android.net.test;

import android.test.AndroidTestCase;
import android.util.Log;

import org.apache.http.util.VersionInfo;

public class ApacheTest extends AndroidTestCase {

	
	public void testVersion(){
		VersionInfo vi = VersionInfo.loadVersionInfo("org.apache.http.client",getClass().getClassLoader());  
		String version = vi.getRelease();  
		Log.d("ApacheTest:", "apache http client version" + version);
	}
}
