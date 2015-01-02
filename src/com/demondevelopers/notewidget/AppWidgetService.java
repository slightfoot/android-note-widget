package com.demondevelopers.notewidget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;


public abstract class AppWidgetService extends Service
{
	private static final String ACTION_UPDATE          = AppWidgetService.class.getCanonicalName() + ".UPDATE";
	private static final String ACTION_DELETED         = AppWidgetService.class.getCanonicalName() + ".DELETED";
	private static final String ACTION_OPTIONS_CHANGED = AppWidgetService.class.getCanonicalName() + ".OPTIONS_CHANGED";
	
	private AppWidgetManager  mAppWidgetManager;
	
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		mAppWidgetManager = AppWidgetManager.getInstance(this);
		// Service might have been killed and restarted, go get details of widgets from manager and populate locals
	}
	
	protected abstract void onUpdate(int[] appWidgetIds);
	protected abstract void onDeleted(int[] appWidgetIds);
	protected abstract void onOptionsChanged(int appWidgetId, Bundle newOptions);
	
	protected AppWidgetManager getAppWidgetManager()
	{
		return mAppWidgetManager;
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		mAppWidgetManager = null;
	}
	
	@SuppressLint("InlinedApi")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if(intent != null){
			final String action = intent.getAction();
			if(ACTION_UPDATE.equals(action)){
				onUpdate(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
			}
			else if(ACTION_DELETED.equals(action)){
				onDeleted(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
			}
			else if(ACTION_OPTIONS_CHANGED.equals(action)){
				onOptionsChanged(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0), 
					intent.getBundleExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS));
			}
		}
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	
	protected static abstract class Provider extends AppWidgetProvider
	{
		public abstract Class<?> getAppWidgetService();
		
		protected Intent createIntent(Context context)
		{
			return new Intent(context, getAppWidgetService());
		}
		
		@Override
		public void onEnabled(Context context)
		{
			context.startService(createIntent(context));
		}
		
		@Override
		public void onDisabled(Context context)
		{
			context.stopService(createIntent(context));
		}
		
		@Override
		public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
		{
			context.startService(createIntent(context)
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
				.setAction(ACTION_UPDATE));
		}
		
		@Override
		public void onDeleted(Context context, int[] appWidgetIds)
		{
			context.startService(createIntent(context)
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
				.setAction(ACTION_DELETED));
		}
		
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		@Override
		public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions)
		{
			context.startService(createIntent(context)
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, newOptions)
				.setAction(ACTION_OPTIONS_CHANGED));
		}
	}
}
