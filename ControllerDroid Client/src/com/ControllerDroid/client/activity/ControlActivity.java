package com.ControllerDroid.client.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.ControllerDroid.client.R;
import com.ControllerDroid.client.activity.connection.ConnectionListActivity;
import com.ControllerDroid.client.app.ControllerDroid;
import com.ControllerDroid.client.view.ControlView;
import com.ControllerDroid.protocol.ControllerDroidActionReceiver;
import com.ControllerDroid.protocol.action.ControllerDroidAction;
import com.ControllerDroid.protocol.action.KeyboardAction;
import com.ControllerDroid.protocol.action.MouseClickAction;
import com.ControllerDroid.protocol.action.MouseMoveAction;
import com.ControllerDroid.protocol.action.MouseWheelAction;
import com.ControllerDroid.protocol.action.ScreenCaptureResponseAction;

public class ControlActivity extends Activity implements ControllerDroidActionReceiver
{
	/*
	 * private static final int FILE_EXPLORER_MENU_ITEM_ID = 0; private static
	 * final int KEYBOARD_MENU_ITEM_ID = 1; private static final int
	 * CONNECTIONS_MENU_ITEM_ID = 2; private static final int
	 * SETTINGS_MENU_ITEM_ID = 3; private static final int HELP_MENU_ITEM_ID =
	 * 4;
	 */
	
	private ControllerDroid application;
	private SharedPreferences preferences;
	
	private ControlView controlView;
	private boolean debugging;
	
	private MediaPlayer mpClickOn;
	private MediaPlayer mpClickOff;
	
	private boolean feedbackSound;
	
	protected void onCreate(Bundle savedInstanceState)
	{
		
		super.onCreate(savedInstanceState);
		
		this.application = (ControllerDroid) this.getApplication();
		this.preferences = this.application.getPreferences();
		
		this.checkFullscreen();
		
		this.setContentView(R.layout.control);
		
		this.setButtonsSize();
		
		this.controlView = (ControlView) this.findViewById(R.id.controlView);
		
		this.mpClickOn = MediaPlayer.create(this, R.raw.clickon);
		this.mpClickOff = MediaPlayer.create(this, R.raw.clickoff);
		
		this.debugging = this.preferences.getBoolean("debug_enabled", false);
		
		this.checkOnCreate();
	}
	
	protected void onResume()
	{
		super.onResume();
		
		this.application.registerActionReceiver(this);
		
		this.feedbackSound = this.preferences.getBoolean("feedback_sound", false);
		
		// This probably won't get called since the editText is set to MultiLine
		((android.widget.EditText) findViewById(R.id.textline)).setOnEditorActionListener(new OnEditorActionListener()
		{
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				boolean handled = false;
				if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND)
				{
					sendMessage(v.getText().toString());
					v.setText("");
					handled = true;
				}
				return handled;
			}
			
		});
		((android.widget.Button) findViewById(R.id.inputSend)).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				// Do something in response to button click
				sendMessage(((android.widget.EditText) findViewById(R.id.textline)).getText().toString());
				((android.widget.EditText) findViewById(R.id.textline)).setText("");
			}
		});
		((android.widget.Button) findViewById(R.id.inputBackspace)).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				// Do something in response to button click
				application.sendAction(new KeyboardAction(-1));
			}
		});
	}
	
	protected void sendMessage(String s)
	{
		
		if (debugging)
			android.util.Log.d("Note", "Sending string: " + s);
		for (int i = 0; i < s.length(); i++)
			this.application.sendAction(new KeyboardAction(s.charAt(i)));
	}
	
	protected void onPause()
	{
		super.onPause();
		
		this.application.unregisterActionReceiver(this);
	}
	
	public boolean onKeyUp(int KeyCode, KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)
		{
			return true;
		}
		if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)
		{
			return true;
		}
		return false;
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		int unicode = event.getUnicodeChar();
		boolean consumeEvent = false;
		
		if (debugging)
			android.util.Log.d("Note", "Key Captured with keycode [" + keyCode + "] and unicode [" + unicode + "]");
		if (unicode == 0)
		{
			boolean volumeAsPaging = this.preferences.getBoolean("send_volume_as_pageupdn", false);
			switch (event.getKeyCode())
			{
				case KeyEvent.KEYCODE_DEL:
					unicode = KeyboardAction.UNICODE_BACKSPACE;
					break;
				case KeyEvent.KEYCODE_VOLUME_UP:
					unicode = volumeAsPaging ? KeyboardAction.UNICODE_PAGEUP : KeyboardAction.UNICODE_VOL_UP;
					consumeEvent = true;
					break;
				case KeyEvent.KEYCODE_VOLUME_DOWN:
					unicode = volumeAsPaging ? KeyboardAction.UNICODE_PAGEDN : KeyboardAction.UNICODE_VOL_DN;
					consumeEvent = true;
					break;
				case KeyEvent.KEYCODE_TAB:
					unicode = KeyboardAction.UNICODE_TAB;
					break;
				case KeyEvent.KEYCODE_DPAD_UP:
					unicode = KeyboardAction.UNICODE_ARROW_UP;
					break;
				case KeyEvent.KEYCODE_DPAD_DOWN:
					unicode = KeyboardAction.UNICODE_ARROW_DOWN;
					break;
				case KeyEvent.KEYCODE_DPAD_LEFT:
					unicode = KeyboardAction.UNICODE_ARROW_LEFT;
					break;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					unicode = KeyboardAction.UNICODE_ARROW_RIGHT;
					break;
			
			}
		}
		if (unicode != 0)
		{
			this.application.sendAction(new KeyboardAction(unicode));
		}
		if (consumeEvent)
		{
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
		/*
		 * Use Menu layout XML menu.add(Menu.NONE, FILE_EXPLORER_MENU_ITEM_ID,
		 * Menu.NONE,
		 * this.getResources().getString(R.string.text_file_explorer));
		 * menu.add(Menu.NONE, KEYBOARD_MENU_ITEM_ID, Menu.NONE,
		 * this.getResources().getString(R.string.text_keyboard));
		 * menu.add(Menu.NONE, CONNECTIONS_MENU_ITEM_ID, Menu.NONE,
		 * this.getResources().getString(R.string.text_connections));
		 * menu.add(Menu.NONE, SETTINGS_MENU_ITEM_ID, Menu.NONE,
		 * this.getResources().getString(R.string.text_settings));
		 * menu.add(Menu.NONE, HELP_MENU_ITEM_ID, Menu.NONE,
		 * this.getResources().getString(R.string.text_help));
		 */
		// Inflate the menu items for use in the action bar
		android.view.MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.control_menu, menu);
		return super.onCreateOptionsMenu(menu);
		
		// return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		// Use Menu layout XML
		// case FILE_EXPLORER_MENU_ITEM_ID:
			case R.id.action_file_explorer:
				this.startActivity(new Intent(this, FileExplorerActivity.class));
				break;
			// case KEYBOARD_MENU_ITEM_ID:
			case R.id.action_keyboard:
				this.toggleKeyboard();
				break;
			// case CONNECTIONS_MENU_ITEM_ID:
			case R.id.action_connections:
				this.startActivity(new Intent(this, ConnectionListActivity.class));
				break;
			// case SETTINGS_MENU_ITEM_ID:
			case R.id.action_settings:
				this.startActivity(new Intent(this, SettingsActivity.class));
				break;
			// case HELP_MENU_ITEM_ID:
			case R.id.action_help:
				this.startActivity(new Intent(this, HelpActivity.class));
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		
		return true;
	}
	
	public void receiveAction(ControllerDroidAction action)
	{
		if (action instanceof ScreenCaptureResponseAction)
		{
			this.controlView.receiveAction((ScreenCaptureResponseAction) action);
		}
	}
	
	public void mouseClick(byte button, boolean state)
	{
		this.application.sendAction(new MouseClickAction(button, state));
		
		if (this.feedbackSound)
		{
			if (state)
			{
				this.playSound(this.mpClickOn);
			}
			else
			{
				this.playSound(this.mpClickOff);
			}
		}
	}
	
	public void mouseMove(int moveX, int moveY)
	{
		this.application.sendAction(new MouseMoveAction((short) moveX, (short) moveY));
	}
	
	public void mouseWheel(int amount)
	{
		this.application.sendAction(new MouseWheelAction((byte) amount));
	}
	
	private void playSound(MediaPlayer mp)
	{
		if (mp != null)
		{
			mp.seekTo(0);
			mp.start();
		}
	}
	
	private void toggleKeyboard()
	{
		InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(0, 0);
		
	}
	
	private void checkFullscreen()
	{
		if (this.preferences.getBoolean("fullscreen", false))
		{
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
			this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}
	
	private void setButtonsSize()
	{
		LinearLayout clickLayout = (LinearLayout) this.findViewById(R.id.clickLayout);
		LinearLayout inputLayout = (LinearLayout) this.findViewById(R.id.inputLayout);
		
		int orientation = this.getResources().getConfiguration().orientation;
		
		int size = (int) (Float.parseFloat(this.preferences.getString("buttons_size", null)) * this.getResources().getDisplayMetrics().density);
		
		if (orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			clickLayout.getLayoutParams().height = (int) size;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
				inputLayout.setTranslationY((int) size);
			else
			{
				// Fix for devices pre-3.0
				inputLayout.setPadding(0, (int) size, 0, 0);
				inputLayout.getLayoutParams().height += (int) size;
				
			}
		}
		else if (orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			clickLayout.getLayoutParams().width = (int) size;
			inputLayout.setPadding((int) size, 0, 0, 0);
		}
	}
	
	private void checkOnCreate()
	{
		if (this.checkFirstRun())
		{
			this.firstRunDialog();
		}
		else if (this.checkNewVersion())
		{
			this.newVersionDialog();
		}
	}
	
	private boolean checkFirstRun()
	{
		return this.preferences.getBoolean("debug_firstRun", true);
	}
	
	private void firstRunDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);
		builder.setMessage(R.string.text_first_run_dialog);
		builder.setPositiveButton(R.string.text_yes, new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				ControlActivity.this.startActivity(new Intent(ControlActivity.this, HelpActivity.class));
				ControlActivity.this.disableFirstRun();
			}
		});
		builder.setNegativeButton(R.string.text_no, new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
				ControlActivity.this.disableFirstRun();
			}
		});
		builder.create().show();
	}
	
	private void disableFirstRun()
	{
		Editor editor = this.preferences.edit();
		editor.putBoolean("debug_firstRun", false);
		editor.commit();
		
		this.updateVersionCode();
	}
	
	private boolean checkNewVersion()
	{
		try
		{
			if (this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_META_DATA).versionCode != this.preferences.getInt("app_versionCode", 0))
			{
				return true;
			}
		}
		catch (NameNotFoundException e)
		{
			this.application.debug(e);
		}
		
		return false;
	}
	
	private void newVersionDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);
		builder.setMessage(R.string.text_new_version_dialog);
		builder.setNeutralButton(R.string.text_ok, new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
				ControlActivity.this.updateVersionCode();
			}
		});
		builder.create().show();
	}
	
	private void updateVersionCode()
	{
		try
		{
			Editor editor = this.preferences.edit();
			editor.putInt("app_versionCode", this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_META_DATA).versionCode);
			editor.commit();
		}
		catch (NameNotFoundException e)
		{
			this.application.debug(e);
		}
	}
	
}
