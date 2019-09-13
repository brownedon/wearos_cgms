package com.dbrowne.cgms;

/**
 * Created by brown on 10/4/2017.
 */

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Readings extends Fragment {
    public Readings_Arr[] readings_arr;
    public SharedPreferences sharedPref;

    public Readings() {
        //Log.d("cgms", "Readings constructor 1");
        //readings_arr = new Readings_Arr[3];
        //initReadings();
    }

    public Readings(SharedPreferences sharedPref) {
        Log.d("cgms", "Readings constructor 2");
        this.sharedPref=sharedPref;
        readings_arr = new Readings_Arr[3];
        for (int i = 0; i < 3; i++) {
            readings_arr[i] = new Readings_Arr();
        }
        //initReadings();
        retrieveReadings();
    }

    void initReadings() {
        Log.d("cgms", "initReadings");

        for (int i = 0; i < 3; i++) {
           // readings_arr[i] = new Readings_Arr();
            readings_arr[i].minutes = 0;
            readings_arr[i].glucose = 0;
            readings_arr[i].isig = 0;
        }
    }

    void dumpReadings(String source){
        Log.d("cgms",source+": "+readings_arr[0].glucose+":"+readings_arr[0].minutes+":"+readings_arr[0].isig);
        Log.d("cgms",source+": "+readings_arr[1].glucose+":"+readings_arr[1].minutes+":"+readings_arr[1].isig);
        Log.d("cgms",source+": "+readings_arr[2].glucose+":"+readings_arr[2].minutes+":"+readings_arr[2].isig);
    }

    public void persistReadings() {
        Log.d("cgms", "persistReadings");

        SharedPreferences.Editor editor = sharedPref.edit();
        //dumpReadings("persistReadings");
        if (readings_arr[0].glucose != 0) {
            editor.putInt("readings_glucosekey1", readings_arr[0].glucose);
            editor.putLong("readings_minuteskey1", readings_arr[0].minutes);
            editor.putLong("readings_isigkey1", readings_arr[0].isig);
        }

        if (readings_arr[1].glucose != 0) {
            editor.putInt("readings_glucosekey2", readings_arr[1].glucose);
            editor.putLong("readings_minuteskey2", readings_arr[1].minutes);
            editor.putLong("readings_isigkey2", readings_arr[1].isig);
        }

        if (readings_arr[2].glucose != 0) {
            editor.putInt("readings_glucosekey3", readings_arr[2].glucose);
            editor.putLong("readings_minuteskey3", readings_arr[2].minutes);
            editor.putLong("readings_isigkey3", readings_arr[2].isig);
        }

       editor.apply();
       // editor.commit();
    }

    public void addReading(int glucose, long rawcounts) {
        Log.d("cgms", "addReading " + glucose + ":" + rawcounts);
        //retrieveReadings();
        //move everything in the array over 1 place
        // then add the new values
        if (rawcounts !=readings_arr[0].isig ) {
            if (glucose > 30) {
                for (int i = 2; i > 0; i--) {
                    // Log.d("cgms", "index:" + i);
                    readings_arr[i].glucose = readings_arr[i - 1].glucose;
                    readings_arr[i].minutes = readings_arr[i - 1].minutes;
                    readings_arr[i].isig = readings_arr[i - 1].isig;
                }
                readings_arr[0].minutes = (System.currentTimeMillis() / 1000) / 60;
                readings_arr[0].glucose = glucose;
                readings_arr[0].isig = rawcounts;

                dumpReadings("addReading");
                persistReadings();
            }
        }
        Log.d("cgms", "done addReading");
    }

   public void retrieveReadings() {
        Log.d("cgms", "retrieveReadings");
        //dumpReadings("retrieveReadings");

        readings_arr[0].glucose = sharedPref.getInt("readings_glucosekey1", 0);
       Log.d("cgms","First stored reading is "+ readings_arr[0].glucose);
        readings_arr[0].minutes = sharedPref.getLong("readings_minuteskey1", 0);
        readings_arr[0].isig = sharedPref.getLong("readings_isigkey1", 0);

        //readings_arr[1] = new Readings_Arr();
        readings_arr[1].glucose = sharedPref.getInt("readings_glucosekey2", 0);
        readings_arr[1].minutes = sharedPref.getLong("readings_minuteskey2", 0);
        readings_arr[1].isig = sharedPref.getLong("readings_isigkey2", 0);

        //readings_arr[2] = new Readings_Arr();
        readings_arr[2].glucose = sharedPref.getInt("readings_glucosekey3", 0);
        readings_arr[2].minutes = sharedPref.getLong("readings_minuteskey3", 0);
        readings_arr[2].isig = sharedPref.getLong("readings_isigkey3", 0);

        //dumpReadings("after retrieveReadings");
        //persistReadings();
        Log.d("cgms", "done retrieveReadings");
    }

    //using the actual minutes since the epoch value underreports the slope
    //so adjust everything based on the first value
    //should end up being 0,5 and 10 minutes
    public double getSlopeGlucose() {
        Log.d("cgms", "getSlopeGlucose");
        dumpReadings("getSlopeGlucose");
        int count = 0;
        double sumx = 0.0, sumy = 0.0, sum1 = 0.0, sum2 = 0.0;

        for (int i = 0; i < 3; i++) {
            if (readings_arr[i].glucose > 20) {
                count++;
                sumx = sumx + (readings_arr[i].minutes);
                sumy = sumy + readings_arr[i].glucose;
            }
        }

        double xmean = sumx / count;
        double ymean = sumy / count;

        //maybe this should be count -1 ???
        for (int i = 0; i < (count); i++) {
            if (readings_arr[i].glucose > 20) {
                sum1 = sum1 + (((readings_arr[i].minutes) - xmean) * (readings_arr[i].glucose - ymean));
                sum2 = sum2 + Math.pow((readings_arr[i].minutes) - xmean, 2);
            }
        }

        // derive the least squares equation
        if (sum2 == 0 || sum1 == 0) {
            return 0;
        }
        double slope = sum1 / sum2;
        Log.d("cgms", "slope:"+slope);
        return slope;
    }

    public String getTimeToCritical(){
        double timeToLimit=100;
        double readingSlope=getSlopeGlucose();
        timeToLimit=getTimeToCriticalInt();

        if((readings_arr[0].glucose<80)||(readings_arr[0].glucose>180)){
            return " ";
        }
        if (Math.abs(timeToLimit) >99){
            return " ";
        }

        if (timeToLimit <= 0) {
            timeToLimit = 1;
        }

        if (readingSlope<0){
            return "-"+String.valueOf((int)(Math.round(timeToLimit)));
        }

        return "+"+String.valueOf((int)(Math.round(timeToLimit)));

    }
    public int getTimeToCriticalInt(){
        double timeToLimit=100;
        double readingSlope=getSlopeGlucose();
        if((readings_arr[0].glucose<80)||(readings_arr[0].glucose>180)){
            return 100;
        }

        if (readingSlope > 0 && readings_arr[0].glucose < 180) {
            //how long until 180
            timeToLimit = Math.abs((180 - readings_arr[0].glucose) / readingSlope);
            //since the dex is ~15 minutes behind reality
            timeToLimit = timeToLimit - 15;
        }
        if (readingSlope < 0 && readings_arr[0].glucose > 80) {
            timeToLimit = Math.abs((readings_arr[0].glucose - 80) / readingSlope);
            //since the dex is ~15 minutes behind reality
            timeToLimit = timeToLimit - 15;
        }
        if (timeToLimit <= 0) {
            timeToLimit = 1;
        }

        return (int)timeToLimit;

    }
}