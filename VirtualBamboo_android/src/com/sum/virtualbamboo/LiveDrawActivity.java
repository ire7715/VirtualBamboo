package com.sum.virtualbamboo;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LiveDrawActivity extends Activity {
	private static final String TAG = "com.ire.LiveDrawActivity";
	
	/*** BEGIN stable variable ***/
		/*** BEGIN UI ***/
			protected ImageView monitor1, monitor2;
			protected ViewGroup viewMask;
			protected ImageButton rotateButton;
			protected TextView tip = null;
		/*** END UI ***/
			
		/*** BEGIN animation ***/
			protected ServerSocket animationServer = null;
			protected boolean monitorSwitch = false;
			protected PowerManager.WakeLock mWakeLock;
			protected Thread animationID = null;
			protected Bitmap bufferedImg = null;
			protected int screenOrientation = 1; 
		/*** END animation ***/
			
		/*** BEGIN cursor ***/
			protected Thread cursorID = null;
			protected ServerSocket cursorServer = null; 
			protected int cursorAction = -1;	// -1 for idle, others are same as MotionEvent
			protected List<cursorAction> actionBuffer = Collections.synchronizedList(new LinkedList<cursorAction>());
		/*** END cursor ***/
	
		protected Handler mHandler = null;
		protected String pcHostAddress = null;
	/*** END stable variable ***/
	
	/**
	 * print stack trace of this exception
	 * @param e all kinds of exception
	 */
	private static void exceptionPrinter(Exception e){
		StackTraceElement[] stack = e.getStackTrace();
		for(StackTraceElement item : stack)
			Log.e(LiveDrawActivity.TAG, item.toString());
	}
	
	class animationPlayer extends Thread{
		public static final int LANDSCAPE_IMAGE = 1;
		public static final int PORTRAIT_IMAGE = 2;
		public static final int META_REQ = 3;
		
		/**
		 * Sends meta-data first, then keeps sending orientation data and receiving images.
		 */
		@Override
		public void run()
		{
			Socket client = null;
			ObjectOutputStream out = null;
			InputStream nis = null;
			int orientationRead = 0;
			screenOrientation = animationPlayer.PORTRAIT_IMAGE;
			Thread thisThread = Thread.currentThread();
			// initialize server socket
			try
			{
				
				//attempt to accept a connection
				client = animationServer.accept();
				handlePost("Connection Established.");

				out = new ObjectOutputStream(client.getOutputStream());
				handlePost("Start transferring meta-data.");
				
				out.write(screenOrientation);
				Point size = new Point();
				getWindowManager().getDefaultDisplay().getSize(size);
				out.writeInt(size.y);
				out.writeInt(size.x);
				out.flush();
				
				out.close();
				out = null;
				client.close();
				client = null;
				handlePost("Meta-data transfer finished.");

				try
				{
					
					while(animationID == thisThread){
						client = animationServer.accept();
						out = new ObjectOutputStream(client.getOutputStream());
						out.write(screenOrientation);
						size = new Point();
						getWindowManager().getDefaultDisplay().getSize(size);
						out.writeInt(size.y);
						out.writeInt(size.x);
						out.flush();
						
						nis = client.getInputStream();
						orientationRead = nis.read();
						if(orientationRead == animationPlayer.LANDSCAPE_IMAGE || orientationRead == animationPlayer.PORTRAIT_IMAGE){
							handleImg(BitmapFactory.decodeStream(nis));
						}
						
						out.close();
						out = null;
						nis.close();
						nis = null;
						client.close();
						client = null;
					}
				}catch (SocketException e)
				{
					LiveDrawActivity.exceptionPrinter(e);
				}catch (IOException ioException)
				{
					LiveDrawActivity.exceptionPrinter(ioException);
				}
				
				if(out != null){
					out.close();
					out = null;
				}
				
				if(nis != null){
					nis.close();
					nis = null;
				}
				
				if(client != null){
					client.close();
					client = null;
				}
			}
			catch (SocketException e)
			{
				LiveDrawActivity.exceptionPrinter(e);
			}
			catch (SocketTimeoutException e)
			{
				handlePost("Connection has timed out! Please try again");
				LiveDrawActivity.exceptionPrinter(e);
			}
			catch (IOException e)
			{
				LiveDrawActivity.exceptionPrinter(e);
			}

			handlePost("Connection ended.");
		}
		
		private void handlePost(String s){
			Log.d(LiveDrawActivity.TAG, s);
		}
		
		private void handleImg(Bitmap img){
			bufferedImg = img;
			mHandler.post(showImage);
		}
	}

	private final Runnable showImage = new Runnable(){

		/**
		 * two drawing board. When one is displaying, draw another one.
		 */
		@Override
		public void run() {
			
			if(monitorSwitch){
				monitor1.setImageBitmap(bufferedImg);
				monitor2.setVisibility(View.GONE);
				monitor1.setVisibility(View.VISIBLE);
			}else{
				monitor2.setImageBitmap(bufferedImg);
				monitor1.setVisibility(View.GONE);
				monitor2.setVisibility(View.VISIBLE);
			}
			monitorSwitch = !monitorSwitch;
		}
		
	};
	
	class cursorThread extends Thread{
		
		/**
		 * Keeps sending mouse action. Stream sealed by -1.
		 */
		@Override
		public void run(){
				
				Socket client = null;
				ObjectOutputStream toPC;
				Thread thisThread = Thread.currentThread();
				while(thisThread == cursorID){
					try {
						
						client = cursorServer.accept();
						toPC = new ObjectOutputStream(client.getOutputStream());
						cursorAction info = null;
						while(actionBuffer.size() != 0){
							info = actionBuffer.remove(0);
							toPC.writeInt(info.action);
							toPC.writeInt(info.x);
							toPC.writeInt(info.y);
						}
						toPC.writeInt(-1);
						toPC.flush();
						
						toPC.close();
						toPC = null;
						client.close();
						client = null;
					} catch (UnknownHostException e) {
						exceptionPrinter(e);
					} catch (IOException e) {
						exceptionPrinter(e);
					}
				}
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_live_draw);
		this.getActionBar().hide();

		this.monitor1 = (ImageView)this.findViewById(R.id.imageView1);
		this.monitor2 = (ImageView)this.findViewById(R.id.imageView2);
		this.viewMask = (RelativeLayout)this.findViewById(R.id.viewMask);
        //this.tip = (TextView)this.findViewById(R.id.textView1);
        this.rotateButton = (ImageButton)this.findViewById(R.id.imageButton1);
        
        this.viewMask.setBackgroundColor(Color.argb(200,0,0,0));
        this.rotateButton.setOnClickListener(new ImageButton.OnClickListener(){

			@Override
			public void onClick(View v) {
				if(screenOrientation == animationPlayer.PORTRAIT_IMAGE)
					screenOrientation = animationPlayer.LANDSCAPE_IMAGE;
				else if(screenOrientation == animationPlayer.LANDSCAPE_IMAGE)
					screenOrientation = animationPlayer.PORTRAIT_IMAGE;
			}
        	
        });
		
		this.mHandler = new Handler();
		
		// prevent device from sleep.
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, LiveDrawActivity.TAG);
        
        try {
        	if(this.animationServer == null || !this.animationServer.isBound())
        		this.animationServer = new ServerSocket(33800);
        	if(this.cursorServer == null || !this.cursorServer.isBound())
	        	this.cursorServer = new ServerSocket(33801);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// Use hardware menu button can enable the control panel.
		
		switch(keyCode){
		case KeyEvent.KEYCODE_MENU:
			if(this.viewMask.getVisibility() == View.GONE)
				this.viewMask.setVisibility(View.VISIBLE);
			else
				this.viewMask.setVisibility(View.GONE);
			return true;
		}
		
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		/**
		 * When the mask enabled, user can control the leaked window from device.
		 * Otherwise, store the touch action. Device will send it to computer later.
		 */
		
		if(this.viewMask.getVisibility() == View.VISIBLE){	// TODO scaling and dragging action
			
			
		}else if(event.getActionIndex() == 0){	// drawing only, that is, read the first pointer only.
			this.actionBuffer.add(new cursorAction(event.getActionMasked(), event.getX(), event.getY()));
		}
		
		return super.onTouchEvent(event);
	}

	/**
	 * starts threads which communicate between device and computer.
	 */
	private void start(){
		this.animationID = new animationPlayer();
		this.animationID.start();
		
		this.cursorID = new cursorThread();
		this.cursorID.start();
	}
	
	/**
	 * stop threads.
	 */
	private void stop(){
		this.animationID = null;
		this.cursorID = null;
	}

	@Override
	protected void onPause() {
		this.mWakeLock.release();
		this.stop();
		
		super.onPause();
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		
		this.mWakeLock.acquire();
		this.start();
	}

	/**
	 * To save device resources, I want to close the ServerSockets when pause and re-launch them when resume.
	 * But it seems like once you close, there is no way to re-bind to the same port again, unless application resources are recycled by system.
	 * So instead of letting the application pause, I would rather kill this activity.
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		System.runFinalizersOnExit(true);
		System.exit(0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.live_draw, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
