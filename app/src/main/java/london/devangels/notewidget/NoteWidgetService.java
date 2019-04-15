package london.devangels.notewidget;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import android.app.PendingIntent;
import android.content.Intent;
import android.widget.RemoteViews;

import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.graphics.Typeface;
import android.text.Selection;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.content.res.Configuration;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.content.Context;
import android.util.SparseArray;
import android.view.View;


public class NoteWidgetService extends AppWidgetService
{
	private static final String TAG = NoteWidgetService.class.getSimpleName();
	private static final String NOTE_WIDGET_DATA_FILE = "notewidgetdata.serial";
	
	private File mNoteWidgetData;
	private Context mViewContext;
	
	private final SparseArray<NoteWidget> mWidgets = new SparseArray<NoteWidget>();
	
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.e(TAG, "onCreate");
		mNoteWidgetData = new File(getFilesDir(), NOTE_WIDGET_DATA_FILE);
		mViewContext    = new ContextThemeWrapper(this, getApplicationTheme(this));
		loadWidgetsFromDisk();
	}
	
	public void onUpdate(int[] appWidgetIds)
	{
		Log.w(TAG, "onUpdate: " + Arrays.toString(appWidgetIds));
		for(int appWidgetId : appWidgetIds){
			updateWidget(appWidgetId);
		}
	}
	
	public void onDeleted(int[] appWidgetIds)
	{
		Log.w(TAG, "onDeleted: " + Arrays.toString(appWidgetIds));
		for(int appWidgetId : appWidgetIds){
			mWidgets.remove(appWidgetId);
		}
	}
	
	public void onOptionsChanged(int appWidgetId, Bundle newOptions)
	{
		newOptions.size();
		Log.w(TAG, "onOptionsChanged: " + appWidgetId + " " + newOptions.toString());
		updateNoteWidgetSize(appWidgetId);
		updateWidget(appWidgetId);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		Log.w(TAG, "onConfigurationChanged: " + newConfig.orientation);
		mViewContext = new ContextThemeWrapper(this, getApplicationTheme(this));
		synchronized (mWidgets){
			for(int i = 0; i < mWidgets.size(); i++){
				NoteWidget noteWidget = mWidgets.valueAt(i);
				noteWidget.view = createNoteView();
				updateNoteWidgetSize(noteWidget.id);
				updateWidget(noteWidget.id);
			}
		}
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.e(TAG, "onDestroy");
		saveWidgetsToDisk();
	}
	
	public static void forceWidgetUpdate(Context context, int appWidgetId)
	{
		context.sendBroadcast(new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
			.setPackage(context.getPackageName())
			.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {
				appWidgetId
			}));
	}
	
	@SuppressLint("NewApi")
	private void updateWidget(int appWidgetId)
	{
		NoteWidget noteWidget = getNoteWidgetForId(appWidgetId);
		if(noteWidget.width == 0 || noteWidget.height == 0){
			updateNoteWidgetSize(appWidgetId);
		}
		
		LayoutParams lp = noteWidget.view.getLayoutParams();
		lp.width  = noteWidget.width;
		lp.height = noteWidget.height;
		noteWidget.view.setLayoutParams(lp);
		
		String text = NoteStorage.getNote(this, appWidgetId);
		applyNoteViewAttrs(this, noteWidget.view, text);
		noteWidget.view.setTextColor(Color.WHITE);
		
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.appwidget_note);
		Bitmap viewImage = getViewImage(noteWidget.view);
		views.setImageViewBitmap(R.id.appwidget_note, viewImage);
		views.setContentDescription(R.id.appwidget_note, text);
		
		Intent configIntent =  new Intent(this, ConfigActivity.class)
			.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		views.setOnClickPendingIntent(R.id.appwidget_note, 
			PendingIntent.getActivity(this, appWidgetId, configIntent, 0));
		
		getAppWidgetManager().updateAppWidget(appWidgetId, views);
		
		viewImage.recycle();
	}
	
	@SuppressLint("NewApi")
	private void updateNoteWidgetSize(int appWidgetId)
	{
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		int width, height;
		Bundle options = getAppWidgetManager().getAppWidgetOptions(appWidgetId);
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
			width  = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
			height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
		}else{
			width  = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
			height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
		}
		NoteWidget noteWidget = getNoteWidgetForId(appWidgetId);
		noteWidget.width = (int)TypedValue
			.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, metrics);
		noteWidget.height = (int)TypedValue
			.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, metrics);
	}
	
	@SuppressLint("InflateParams")
	private EditText createNoteView()
	{
		EditText noteView = (EditText)LayoutInflater.from(mViewContext)
			.inflate(R.layout.note_view, null, false);
		if(noteView.getLayoutParams() == null){
			noteView.setLayoutParams(new LayoutParams(0, 0));
		}
		return noteView;
	}
	
	public static void applyNoteViewAttrs(Context context, EditText noteView, CharSequence text)
	{
		TextPaint paint = noteView.getPaint();
		paint.setFlags(TextPaint.ANTI_ALIAS_FLAG);
		noteView.setTypeface(Typeface.createFromAsset(context.getAssets(), "comic.ttf"));
		noteView.setText(text);
		Selection.removeSelection(noteView.getText());
	}
	
	public Bitmap getViewImage(View view)
	{
		LayoutParams lp = view.getLayoutParams();
		view.measure(
			MeasureSpec.makeMeasureSpec(lp.width,  MeasureSpec.EXACTLY),
			MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY));
		final int width  = view.getMeasuredWidth();
		final int height = view.getMeasuredHeight();
		view.layout(0, 0, width, height);
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		view.draw(canvas);
		return bitmap;
	}
	
	private NoteWidget getNoteWidgetForId(int appWidgetId)
	{
		NoteWidget noteWidget;
		synchronized(mWidgets){
			noteWidget = mWidgets.get(appWidgetId);
			if(noteWidget == null){
				noteWidget = new NoteWidget(appWidgetId);
				noteWidget.view = createNoteView();
				mWidgets.put(appWidgetId, noteWidget);
			}
		}
		return noteWidget;
	}
	
	private static int getApplicationTheme(Context context)
	{
		try{
			ApplicationInfo appInfo = context.getPackageManager()
				.getApplicationInfo(context.getPackageName(), 0);
			return appInfo.theme;
		}
		catch(NameNotFoundException e){
			return android.R.style.Theme_Light;
		}
	}
	
	private void loadWidgetsFromDisk()
	{
		if(!mNoteWidgetData.exists()){
			return;
		}
		try{
			ObjectInputStream in = null;
			try{
				in = new ObjectInputStream(new FileInputStream(mNoteWidgetData));
				NoteWidget[] noteWidgets = (NoteWidget[])in.readObject();
				synchronized (mWidgets){
					mWidgets.clear();
					for(NoteWidget noteWidget : noteWidgets){
						mWidgets.put(noteWidget.id, noteWidget);
					}
				}
			}
			finally{
				if(in != null){
					in.close();
				}
			}
		}
		catch(ClassNotFoundException e){
			mNoteWidgetData.delete();
		}
		catch(IOException e){
			mNoteWidgetData.delete();
		}
	}
	
	private void saveWidgetsToDisk()
	{
		if(mWidgets.size() == 0){
			mNoteWidgetData.delete();
			return;
		}
		try{
			ObjectOutputStream out = null;
			try{
				out = new ObjectOutputStream(new FileOutputStream(mNoteWidgetData));
				synchronized(mWidgets){
					final int count = mWidgets.size();
					NoteWidget[] noteWidgets = new NoteWidget[count];
					for(int i = 0; i < count; i++){
						noteWidgets[i] = mWidgets.valueAt(i);
					}
					out.writeObject(noteWidgets);
				}
			}
			finally{
				if(out != null){
					out.close();
				}
			}
		}
		catch(IOException e){
			mNoteWidgetData.delete();
		}
	}
	
	private class NoteWidget implements Serializable
	{
		private static final long serialVersionUID = 1L;
		
		public transient EditText view;
		
		public final int id;
		public int       width;
		public int       height;
		
		
		public NoteWidget(int id)
		{
			this.id = id;
		}
	}
	
	public static class Provider extends AppWidgetProvider
	{
		@Override
		public Class<?> getAppWidgetService()
		{
			return NoteWidgetService.class;
		}
	}
}
