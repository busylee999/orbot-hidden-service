package ru.dtlbox;

import org.torproject.android.service.TorService;
import org.torproject.android.service.TorServiceConstants;

import android.content.Intent;
import android.util.Log;

public class CustomTorService extends TorService {
    
    private final static String LOG_TAG = "CustomTorService";
    private boolean isStarted = false;
    
    @Override
    public void onCreate(){
        super.onCreate();
        Log.i(LOG_TAG, LOG_TAG + " create");
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.i(LOG_TAG, LOG_TAG + " start");
        setTorProfile(TorServiceConstants.PROFILE_ON);
        isStarted = true;
    }
    
    //check whether the service is running
    public boolean isStarted(){
        return isStarted;
    }
    
    @Override
    public void logMessage(String msg)
    {
        Log.i(LOG_TAG,msg);
        super.logMessage(msg);
    }
    
    @Override
    public void logException(String msg, Exception e)
    {
        Log.e(LOG_TAG,msg,e);
        super.logException(msg, e);
    }
    
}
