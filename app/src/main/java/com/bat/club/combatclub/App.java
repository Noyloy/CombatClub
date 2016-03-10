package com.bat.club.combatclub;

import android.app.Application;
import android.content.Context;

/**
 * Created by Noyloy on 27-Jan-16.
 */
public class App extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext(){
        return mContext;
    }
}
