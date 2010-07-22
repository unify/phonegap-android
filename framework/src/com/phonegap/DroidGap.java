package com.phonegap;
/* License (MIT)
 * Copyright (c) 2008 Nitobi
 * website: http://phonegap.com
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * Software), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.widget.LinearLayout;
import android.os.Build.*;
import android.provider.MediaStore;

public class DroidGap extends Activity {
		
	private static final String LOG_TAG = "DroidGap";
	protected WebView appView;
	private LinearLayout root;	
	
	private Device gap;
	private GeoBroker geo;
	private AccelBroker accel;
	private CameraLauncher launcher;
	private ContactManager mContacts;
	private FileUtils fs;
	private NetworkManager netMan;
	private CompassListener mCompass;
	private Storage	cupcakeStorage;
	private CryptoHandler crypto;
	private BrowserKey mKey;
	private AudioHandler audio;

	private Uri imageUri;
	
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE); 
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); 
        // This builds the view.  We could probably get away with NOT having a LinearLayout, but I like having a bucket!
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 
        		ViewGroup.LayoutParams.FILL_PARENT, 0.0F);
         
        LinearLayout.LayoutParams webviewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
        		ViewGroup.LayoutParams.FILL_PARENT, 1.0F);
        
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setLayoutParams(containerParams);
                
        appView = new WebView(this);
        appView.setLayoutParams(webviewParams);
        
        WebViewReflect.checkCompatibility();
                
        if (android.os.Build.VERSION.RELEASE.startsWith("2."))
        	appView.setWebChromeClient(new EclairClient(this));        	
        else
        {        
        	appView.setWebChromeClient(new GapClient(this));
        }
           
        appView.setInitialScale(100);
        appView.setVerticalScrollBarEnabled(false);
        
        WebSettings settings = appView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);

    	Package pack = this.getClass().getPackage();
    	String appPackage = pack.getName();
    	
        WebViewReflect.setStorage(settings, true, "/data/data/" + appPackage + "/app_database/");
        
        // Turn on DOM storage!
        WebViewReflect.setDomStorage(settings);
        // Turn off native geolocation object in browser - we use our own :)
        WebViewReflect.setGeolocationEnabled(settings, true);
        /* Bind the appView object to the gap class methods */
        bindBrowser(appView);
        if(cupcakeStorage != null)
        	cupcakeStorage.setStorage(appPackage);
                
        root.addView(appView);   
        
        setContentView(root);                        
    }
	
	public void invoke(String origin, boolean allow, boolean remember) {

	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
      //don't reload the current page when the orientation is changed
      super.onConfigurationChanged(newConfig);
    } 
    
    private void bindBrowser(WebView appView)
    {
    	gap = new Device(appView, this);
    	accel = new AccelBroker(appView, this);
    	launcher = new CameraLauncher(appView, this);
    	mContacts = new ContactManager(appView, this);
    	fs = new FileUtils(appView);
    	netMan = new NetworkManager(appView, this);
    	mCompass = new CompassListener(appView, this);  
    	crypto = new CryptoHandler(appView);
    	mKey = new BrowserKey(appView, this);
    	audio = new AudioHandler(appView, this);
    	
    	// This creates the new javascript interfaces for PhoneGap
    	appView.addJavascriptInterface(gap, "DroidGap");
    	appView.addJavascriptInterface(accel, "Accel");
    	appView.addJavascriptInterface(launcher, "GapCam");
    	appView.addJavascriptInterface(mContacts, "ContactHook");
    	appView.addJavascriptInterface(fs, "FileUtil");
    	appView.addJavascriptInterface(netMan, "NetworkManager");
    	appView.addJavascriptInterface(mCompass, "CompassHook");
    	appView.addJavascriptInterface(crypto, "GapCrypto");
    	appView.addJavascriptInterface(mKey, "BackButton");
    	appView.addJavascriptInterface(audio, "GapAudio");
    	
    	
    	if (android.os.Build.VERSION.RELEASE.startsWith("1."))
    	{
    		cupcakeStorage = new Storage(appView);
        	geo = new GeoBroker(appView, this);
    		appView.addJavascriptInterface(cupcakeStorage, "droidStorage");
        	appView.addJavascriptInterface(geo, "Geo");
    	}
    }
           
 
	public void loadUrl(String url)
	{
		appView.loadUrl(url);
	}
	
	
  /**
    * Provides a hook for calling "alert" from javascript. Useful for
    * debugging your javascript.
  */
	public class GapClient extends WebChromeClient {				
		
		Context mCtx;
		public GapClient(Context ctx)
		{
			mCtx = ctx;
		}
		
		@Override
	    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
	        Log.d(LOG_TAG, message);
	        // This shows the dialog box.  This can be commented out for dev
	        AlertDialog.Builder alertBldr = new AlertDialog.Builder(mCtx);
	        GapOKDialog okHook = new GapOKDialog();
	        GapCancelDialog cancelHook = new GapCancelDialog();
	        alertBldr.setMessage(message);
	        alertBldr.setTitle("Alert");
	        alertBldr.setCancelable(true);
	        alertBldr.setPositiveButton("OK", okHook);
	        alertBldr.setNegativeButton("Cancel", cancelHook);
	        alertBldr.show();
	        result.confirm();
	        return true;
	    }
		
		/*
		 * This is the Code for the OK Button
		 */
		
		public class GapOKDialog implements DialogInterface.OnClickListener {

			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.dismiss();
			}			
		
		}
		
		public class GapCancelDialog implements DialogInterface.OnClickListener {

			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.dismiss();
			}			
		
		}
	  

	}
	
	public final class EclairClient extends GapClient
	{		
		private String TAG = "PhoneGapLog";
		private long MAX_QUOTA = 100 * 1024 * 1024;
		
		public EclairClient(Context ctx) {
			super(ctx);
			// TODO Auto-generated constructor stub
		}
		
		public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota, long estimatedSize,
		    	     long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater)
		{
		  Log.d(TAG, "event raised onExceededDatabaseQuota estimatedSize: " + Long.toString(estimatedSize) + " currentQuota: " + Long.toString(currentQuota) + " totalUsedQuota: " + Long.toString(totalUsedQuota));  	
		  
			if( estimatedSize < MAX_QUOTA)
		    	{	                                        
		    	  //increase for 1Mb        		    	  		    	  
		    		long newQuota = currentQuota + 1024*1024;		    		
		    		Log.d(TAG, "calling quotaUpdater.updateQuota newQuota: " + Long.toString(newQuota) );  	
		    		quotaUpdater.updateQuota(newQuota);
		    	}
		    else
		    	{
		    		// Set the quota to whatever it is and force an error
		    		// TODO: get docs on how to handle this properly
		    		quotaUpdater.updateQuota(currentQuota);
		    	}		    	
		}		
		                    
		// console.log in api level 7: http://developer.android.com/guide/developing/debug-tasks.html
		public void onConsoleMessage(String message, int lineNumber, String sourceID)
		{       
			// This is a kludgy hack!!!!
			Log.d(TAG, sourceID + ": Line " + Integer.toString(lineNumber) + " : " + message);              
		}
		
		@Override
		public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
			// TODO Auto-generated method stub
			super.onGeolocationPermissionsShowPrompt(origin, callback);
			callback.invoke(origin, true, false);
		}
		
	}
	
  
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
    	
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	if (mKey.isBound())
        	{
        		//We fire an event here!
        		appView.loadUrl("javascript:document.keyEvent.backTrigger()");
        	}
        	else
        	{
        		String testUrl = appView.getUrl();
        		appView.goBack();
        		if(appView.getUrl().equals(testUrl))
        		{
        			return super.onKeyDown(keyCode, event);
        		}
        	}
        }
        
        if (keyCode == KeyEvent.KEYCODE_MENU) 
        {
        	// This is where we launch the menu
        	appView.loadUrl("javascript:keyEvent.menuTrigger()");
        }
                
        return false;
    }
	
    // This is required to start the camera activity!  It has to come from the previous activity
    public void startCamera()
    {
    	Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        File photo = new File(Environment.getExternalStorageDirectory(),  "Pic.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(photo));
        imageUri = Uri.fromFile(photo);
        startActivityForResult(intent, 0);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
    	   super.onActivityResult(requestCode, resultCode, intent);
    	   
    	   if (resultCode == Activity.RESULT_OK) {
    		   Uri selectedImage = imageUri;
    	       getContentResolver().notifyChange(selectedImage, null);
    	       ContentResolver cr = getContentResolver();
    	       Bitmap bitmap;
    	       try {
    	            bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, selectedImage);
    	            launcher.processPicture(bitmap);
    	       } catch (Exception e) {
    	    	   launcher.failPicture("Did not complete!");
    	       }
    	    }
    	   else
    	   {
    		   launcher.failPicture("Did not complete!");
    	   }
    }

    public WebView getView()
    {
      return this.appView;
    }
      
}
