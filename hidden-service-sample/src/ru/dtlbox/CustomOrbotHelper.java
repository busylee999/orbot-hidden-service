package ru.dtlbox;

import info.guardianproject.onionkit.ui.OrbotHelper;

import org.torproject.android.service.ITorService;
import org.torproject.android.service.TorServiceConstants;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class CustomOrbotHelper extends OrbotHelper {

	protected final static String LOG_TAG = "CustomOrbotHelper";

	public final static String INTENT_TOR_SERVICE = "ru.dtlbox.TOR_SERVICE";
	public final static String TOR_CUSTOM_SERVICE_NAME = "CustomOrbotHelper";

	private ITorService mService = null;
	private static Context mContext = null;
	private static SharedPreferences mPrefs = null;

	// get instance of CustomOrbotHelper
	public static CustomOrbotHelper getInstance() {
		if (mContext != null)
			return SingletonHolder.instance;
		else
			return null;
	}

	private static class SingletonHolder {
		public static CustomOrbotHelper instance = new CustomOrbotHelper(
				mContext);
	}

	// for nobody can use constructor directly
	private CustomOrbotHelper(Context context) {
		super(context);
	}

	/**
	 * set static context for orbot helper creation this function provide to
	 * pass context into super class in super class it was used for detecting
	 * orbot application
	 */
	public static void setContext(Context context) {
		mContext = context;
		mPrefs = CustomTorService.getSharedPrefs(context.getApplicationContext());
	}

	public void torServiceStartAsync(final Activity activity) {
		(new Thread(new Runnable() {

			@Override
			public void run() {
				torServiceStart(activity);
			}
		})).start();
	}

	private String enableHiddenServicePort(int hsPort) {
		Editor pEdit = mPrefs.edit();

		String hsPortString = mPrefs.getString("pref_hs_ports", "");

		if (hsPortString.length() > 0
				&& hsPortString.indexOf(hsPort + "") == -1)
			hsPortString += ',' + hsPort;
		else
			hsPortString = hsPort + "";

		pEdit.putString("pref_hs_ports", hsPortString);
		pEdit.putBoolean("pref_hs_enable", true);

		pEdit.commit();

		String onionHostname = mPrefs.getString("pref_hs_hostname", "");

		while (onionHostname.length() == 0) {
			// we need to stop and start Tor
			try {
				stopTor();

				Thread.sleep(3000); // wait three seconds

				startTor();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			onionHostname = mPrefs.getString("pref_hs_hostname", "");
		}


		return onionHostname;
	}

	// try to start tor service
	public void torServiceStart(final Activity activity) {
		Log.d(LOG_TAG, "tor service start");
		try {
			if (isOrbotInstalled()) {
				Log.i(LOG_TAG,
						"orbotinstalled.Trying to start custom tor service");
				if (!isOrbotRunning()) {
					Intent intent = new Intent(URI_ORBOT);
					intent.setAction(ACTION_START_TOR);
					activity.startActivityForResult(intent, 1);
				} else
					Log.i(LOG_TAG, "orbot already running");

			} else {
				Log.i(LOG_TAG,
						"orbot is not installed.Trying to start custom tor service");
				// if (!isServiceRunning(TOR_CUSTOM_SERVICE_NAME))
				// activity.startService(new Intent(INTENT_TOR_SERVICE));
				// else
				// Log.i(LOG_TAG, "custom tor service already running");
				if (mService == null)
					bindService();
				else
					try {
						startTor();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

			}

		} catch (Exception e) {
			Log.e(LOG_TAG, "error during start orbot");
			e.printStackTrace();
		}

	}

	public void torServiceStart() {
		Log.d(LOG_TAG, "tor service start");
		try {

			Log.i(LOG_TAG,
					"orbot is not installed.Trying to start custom tor service");
			// if (!isServiceRunning(TOR_CUSTOM_SERVICE_NAME))
			// activity.startService(new Intent(INTENT_TOR_SERVICE));
			// else
			// Log.i(LOG_TAG, "custom tor service already running");
			if (mService == null)
				bindService();
			else
				try {
					startTor();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

		} catch (Exception e) {
			Log.e(LOG_TAG, "error during start orbot");
			e.printStackTrace();
		}

	}

	// check is any tor service running
	public boolean isTorServiceRunning() {
		if (isOrbotInstalled())
			return super.isOrbotRunning();
		else
			return isServiceRunning(TOR_CUSTOM_SERVICE_NAME);
	}

	// check is custom service running
	private Boolean isServiceRunning(String serviceName) {
		ActivityManager activityManager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo runningServiceInfo : activityManager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceName.equals(runningServiceInfo.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Class for interacting with the main interface of the service.
	 */
	// this is the connection that gets called back when a successfull bind
	// occurs
	// we should use this to activity monitor unbind so that we don't have to
	// call
	// bindService() a million times
	public ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(LOG_TAG, "onServiceConnected");
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = ITorService.Stub.asInterface(service);

			Log.i(LOG_TAG, "on service connected");

			// if(!((CustomTorService) mService).isStarted())
			// mContext.startService(new Intent(INTENT_TOR_SERVICE));

			try {

				startTor();

			} catch (RemoteException e) {
				// In this case the service has crashed before we could even
				// do anything with it; we can count on soon being
				// disconnected (and then reconnected if it can be restarted)
				// so there is no need to do anything here.
				Log.d(LOG_TAG, "error registering callback to service", e);
			}

		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;

		}
	};

	public void startTor() throws RemoteException {

		// this is a bit of a strange/old/borrowed code/design i used to change
		// the service state
		// not sure it really makes sense when what we want to say is just
		// "startTor"
		mService.setProfile(TorServiceConstants.PROFILE_ON); // this means turn
																// on

	}
	
	 //now we stop Tor! amazing!
    private void stopTor () throws RemoteException
    {
    	if (mService != null)
    	{
    		mService.setProfile(TorServiceConstants.PROFILE_OFF);
			// Message msg =
			// mHandler.obtainMessage(TorServiceConstants.DISABLE_TOR_MSG);
			// mHandler.sendMessage(msg);
            //trafficRow.setVisibility(RelativeLayout.GONE);

            
    	}
    	
     
    }

	// this is where we bind!
	private void bindService() {
		Log.i(LOG_TAG, "bind service");
		// since its auto create, we prob don't ever need to call startService
		// also we should again be consistent with using either
		// iTorService.class.getName()
		// or the variable constant
		mContext.bindService(new Intent(INTENT_TOR_SERVICE), mConnection,
				Context.BIND_AUTO_CREATE);

	}

}
