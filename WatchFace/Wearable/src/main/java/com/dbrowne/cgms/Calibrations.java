package com.dbrowne.cgms;

/**
 * Created by brown on 10/4/2017.
 */

import android.app.Fragment;
import android.content.SharedPreferences;
import android.util.Log;
import static java.lang.Math.pow;

public class Calibrations extends Fragment {
    //persist slope and intercept
//values provided when paired with phone
    double slope = 700;
    long intercept = 30000;
    double tmpSlope;
    long tmpIntercept;

    public Calibrations_Arr[] calibrations_arr ;
    public SharedPreferences sharedPref;

    public  Calibrations(){
        //Log.d("cgms", "Calibrations constructor 1");
        //calibrations_arr = new Calibrations_Arr[5];
       // initCalibrations();
    }

    public Calibrations(SharedPreferences sharedPref) {
        Log.d("cgms", "Calibrations constructor 2");
        this.sharedPref=sharedPref;
        calibrations_arr = new Calibrations_Arr[5];
        for (int i = 0; i < 5; i++) {
            calibrations_arr[i] = new Calibrations_Arr();
        }
        //initCalibrations();
        retrieveCal();

        for (int i = 0; i < 5; i++) {
            if (calibrations_arr[i].isig > 0) {
                Log.d("cgms", "Calibration: " + calibrations_arr[i].glucose + ":" + calibrations_arr[i].isig);
            }
        }
        //calcSlopeandInt();

    }

    public void initCalibrations(){
        Log.d("cgms", "initCalibrations");

        for (int i = 0; i < 5; i++) {
           // calibrations_arr[i] = new Calibrations_Arr();
            calibrations_arr[i].glucose = 0;
            calibrations_arr[i].isig = 0;
            calibrations_arr[i].minutes = 0;
        }
        slope=700;
        intercept=30000;
     }



    public void persistCalibration() {
        Log.d("cgms", "persistCalibration");

        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt("calibrations_glucosekey1", calibrations_arr[0].glucose);
        editor.putLong("calibrations_minuteskey1", calibrations_arr[0].minutes);
        editor.putLong("calibrations_isigkey1", calibrations_arr[0].isig);

        editor.putInt("calibrations_glucosekey2", calibrations_arr[1].glucose);
        editor.putLong("calibrations_minuteskey2", calibrations_arr[1].minutes);
        editor.putLong("calibrations_isigkey2", calibrations_arr[1].isig);

        editor.putInt("calibrations_glucosekey3", calibrations_arr[2].glucose);
        editor.putLong("calibrations_minuteskey3", calibrations_arr[2].minutes);
        editor.putLong("calibrations_isigkey3", calibrations_arr[2].isig);

        editor.putInt("calibrations_glucosekey4", calibrations_arr[3].glucose);
        editor.putLong("calibrations_minuteskey4", calibrations_arr[3].minutes);
        editor.putLong("calibrations_isigkey4", calibrations_arr[3].isig);

        editor.putInt("calibrations_glucosekey5", calibrations_arr[4].glucose);
        editor.putLong("calibrations_minuteskey5", calibrations_arr[4].minutes);
        editor.putLong("calibrations_isigkey5", calibrations_arr[4].isig);

        Double slopedbl =slope;

        Log.d("cgms","persistCalibration "+slope+":"+intercept);
        editor.putInt("slope", slopedbl.intValue());
        editor.putLong("intercept", intercept);

        //editor.commit();
        editor.apply();
    }

    public void retrieveCal() {
        Log.d("cgms", "retrieveCal");

        //calibrations_arr = new Calibrations_Arr[5];

        //calibrations_arr[0] = new Calibrations_Arr();

        calibrations_arr[0].glucose =  sharedPref.getInt("calibrations_glucosekey1", 0);
        Log.d("cgms","First cal point is "+calibrations_arr[0].glucose);
        calibrations_arr[0].minutes =  sharedPref.getLong("calibrations_minuteskey1", 0);
        calibrations_arr[0].isig =  sharedPref.getLong("calibrations_isigkey1", 0);

        //calibrations_arr[1] = new Calibrations_Arr();
        calibrations_arr[1].glucose =  sharedPref.getInt("calibrations_glucosekey2", 0);
        calibrations_arr[1].minutes =  sharedPref.getLong("calibrations_minuteskey2", 0);
        calibrations_arr[1].isig =  sharedPref.getLong("calibrations_isigkey2", 0);

        //calibrations_arr[2] = new Calibrations_Arr();
        calibrations_arr[2].glucose =  sharedPref.getInt("calibrations_glucosekey3", 0);
        calibrations_arr[2].minutes =  sharedPref.getLong("calibrations_minuteskey3", 0);
        calibrations_arr[2].isig =  sharedPref.getLong("calibrations_isigkey3", 0);

        //calibrations_arr[3] = new Calibrations_Arr();
        calibrations_arr[3].glucose =  sharedPref.getInt("calibrations_glucosekey4", 0);
        calibrations_arr[3].minutes =  sharedPref.getLong("calibrations_minuteskey4", 0);
        calibrations_arr[3].isig =  sharedPref.getLong("calibrations_isigkey4", 0);

        //calibrations_arr[4] = new Calibrations_Arr();
        calibrations_arr[4].glucose =  sharedPref.getInt("calibrations_glucosekey5", 0);
        calibrations_arr[4].minutes =  sharedPref.getLong("calibrations_minuteskey5", 0);
        calibrations_arr[4].isig =  sharedPref.getLong("calibrations_isigkey5", 0);

        slope = sharedPref.getInt("slope", 700);
        intercept = sharedPref.getLong("intercept", 30000);

    }

    public void addCalibration(long isig, int glucose) {
         Log.d("cgms", "addCalibration " + isig + ":" + glucose);
         //move everything in the array over 1 place
         //then add the new values
         //retrieveCal();
         //don't allow the bootstrap value in more than once in a row
         if ((isig == 30000 && calibrations_arr[0].isig != 30000) || (isig > 30000)) {
             for (int i = 4; i > 0; i--) {
                 calibrations_arr[i].glucose = calibrations_arr[i - 1].glucose;
                 calibrations_arr[i].minutes = calibrations_arr[i - 1].minutes;
                 calibrations_arr[i].isig = calibrations_arr[i - 1].isig;
             }
             calibrations_arr[0].glucose = glucose;
             calibrations_arr[0].minutes = (System.currentTimeMillis() / 1000)/60;
             calibrations_arr[0].isig = isig;
         }
     }

        void getCalSlopeAndIntercept(){
            Log.d("cgms", "getCalSlopeAndIntercept");
            //retrieveCal();
            int count = 0;
            double sumx = 0.0, sumy = 0.0, sum1 = 0.0, sum2 = 0.0;

            for (int i = 0; i < 5; i++) {
                if (calibrations_arr[i].isig > 0) {
                    Log.d("cgms","Calibration: "+calibrations_arr[i].glucose+":"+ calibrations_arr[i].isig);
                    count++;
                    sumx = sumx + calibrations_arr[i].glucose;
                    sumy = sumy + calibrations_arr[i].isig;
                }
            }

            double xmean = sumx / count;
            double ymean = sumy / count;

            for (int i = 0; i < (count-1); i++) {
                if (calibrations_arr[i].isig > 0) {
                    sum1 = sum1 + ((calibrations_arr[i].glucose - xmean) * (calibrations_arr[i].isig - ymean));
                    sum2 = sum2 + pow(calibrations_arr[i].glucose - xmean, 2);
                }
            }
            if (sum2 == 0) {
                tmpSlope = 0;
                tmpIntercept = 0;
            } else {
                // derive the least squares equation
                tmpSlope = sum1 / sum2;
                tmpIntercept = (long) ymean - ((long) tmpSlope * (long) xmean);
            }
            Log.d("cgms","Slope:"+tmpSlope+" Intercept: "+tmpIntercept);
        }

       public void calcSlopeandInt () {
            Log.d("cgms", "calcSlopeandInt");
            tmpSlope = 0;
            tmpIntercept = 0;

            getCalSlopeAndIntercept();
            if ((tmpSlope > 300 && tmpSlope < 2000) && (tmpIntercept < 60000 && tmpIntercept > 10000)) {
                slope = tmpSlope;
                intercept = tmpIntercept;
            } else {
                //fall back to bootstrap value and most recent calibration
                int tmpGlucose = calibrations_arr[0].glucose;
                long tmpRawcount = calibrations_arr[0].isig;
                initCalibrations();
                addCalibration(30000, 0);
                addCalibration(tmpRawcount, tmpGlucose);
                //persistCalibration();
                getCalSlopeAndIntercept();
                //
                //APP_LOG(APP_LOG_LEVEL_DEBUG,"Cal %d:%d",tmpSlope,tmpIntercept);
                if ((tmpSlope > 300 && tmpSlope < 2000) && (tmpIntercept < 60000 && tmpIntercept > 10000)) {
                    slope = tmpSlope;
                    intercept = tmpIntercept;
                } else {
                    //this is a bad sensor error that should be handled
                    //reject calibration and fall back to original cal array
                    Log.d("cgms", "ERROR");
                    retrieveCal();
                }

            }
           persistCalibration();
        }

       public void updateIsig(long isig){
            Log.d("cgms", "updateRawcount");
            calibrations_arr[0].isig = isig;
           // persistCalibration();
        }
    }