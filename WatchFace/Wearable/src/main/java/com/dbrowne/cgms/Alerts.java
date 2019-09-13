package com.dbrowne.cgms;

/**
 * Created by brown on 10/4/2017.
 */

import android.app.Fragment;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import com.dbrowne.cgms.Singleton;

import static android.content.Context.VIBRATOR_SERVICE;

public class Alerts extends Fragment {

    long segments[] = {400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400, 400};

    long segments_short[] = {400, 400};

    Vibrator mVibrator;


    public int alertCount = 0;

    public  Alerts() {
    }


    public  Alerts(Vibrator mVibrator) {
        this.mVibrator = mVibrator;
    }

    public void doNoSignal(){
        final int indexInPatternToRepeat = -1;
        mVibrator.vibrate(segments, indexInPatternToRepeat);
    }

    public void doAlerts(int currentGlucose, int timeToLimit) {
        //-1 - don't repeat
        final int indexInPatternToRepeat = -1;
        Log.d("cgms","doAlerts "+alertCount);
        Log.d("cgms", "alerts ISIG:" + Singleton.getInstance().getLastISIG() +":"+ Singleton.getInstance().getISIG());
      // if ( Singleton.getInstance().getLastISIG() != Singleton.getInstance().getISIG()){
        if (currentGlucose > 80 && currentGlucose < 180) {
            alertCount = 0;
        }

        if (currentGlucose < 80 && alertCount == 0 && currentGlucose > 60) {
            mVibrator.vibrate(segments, indexInPatternToRepeat);
        }

        if (currentGlucose < 80 && currentGlucose > 60) {
            //APP_LOG(APP_LOG_LEVEL_DEBUG, "between 60 and 80, increment");
            alertCount++;
            if (alertCount >= 2) {
                alertCount = 0;
            }
        }

        if (currentGlucose < 60 && alertCount == 0) {
            mVibrator.vibrate(segments, indexInPatternToRepeat);
        }

        if (currentGlucose < 60) {
            //APP_LOG(APP_LOG_LEVEL_DEBUG, "below 60, increment");
            alertCount++;
            if (alertCount >= 1) {
                alertCount = 0;
            }
        }

        if (currentGlucose > 180 && alertCount == 0) {
            mVibrator.vibrate(segments, indexInPatternToRepeat);
        }

        if (currentGlucose > 180) {
            alertCount++;
            if (alertCount >= 24) {
                alertCount = 0;
            }
        }

        //quick alert whenever predicted to be at high or low limit
        if (timeToLimit == 1) {
            mVibrator.vibrate(segments_short, indexInPatternToRepeat);
        }


    }
 //   }
}