package com.dbrowne.cgms;

/**
 * Created by brown on 10/4/2017.
 */
import android.util.Log;

public class cgms {
    int SLOPE_OVERRIDE=100;

    public Readings_Arr[] readings_arr = new Readings_Arr[3];

    int slope;
    long intercept;

    int slopecount=0;
    int sleepCount=0;

    long last_reading = 0;
    long this_reading = 0;
    int miss_count = 0;
    int sensor_miss_count = 0;
    int currentGlucose = 0;
    int lastGlucose = 0;
    long calTime=0;
    boolean newCal=false;
    long isig = 0;
    int secondCount=0;
    int watchCallbackCount = 0;
    int lastWatchCallbackCount = 0;

    int slopeDirection = 0;

   //public calibration cal = new calibration();
    Readings read = new Readings();

    boolean readingAdded = false;


    public void getSlopeAndInt(){
        Log.d("cgms", "getSlopeAndInt");
        int calCount=0;
        Calibrations cal = new Calibrations();
        cal.retrieveCal();

        for (int i = 4; i > 0; i-- ) {
            if (cal.calibrations_arr[i].isig>0){
                calCount++;
            }
        }

        if (calCount>0){
            cal.calcSlopeandInt();
        }else{
            //APP_LOG(APP_LOG_LEVEL_DEBUG,"Use defaults");
            slope=700;
            intercept=30000;
        }
    }


    public void resetCal() {
        Log.d("cgms", "resetCal");
        Calibrations cal = new Calibrations();
        cal.initCalibrations();

        cal.addCalibration(30000, 0);
        slope = 700;
        intercept = 30000;

        cal.persistCalibration();
    }

    void calibrate(int gluc) {
        Log.d("cgms", "calibrate");
        Calibrations cal = new Calibrations();
        if (readings_arr[0].isig > 0) {
            newCal = true;
          //  calTime = read.readings_arr[0].seconds;
         //   cal.addCalibration(read.readings_arr[0].isig, gluc);
         //   cal.calcSlopeandInt();
          //  long rc1 = (read.readings_arr[0].isig) - intercept;
       //     int glucose =   ( (Long) (rc1 / slope)).intValue();
         //   read.readings_arr[0].glucose=glucose;
        }
    }

/*
static void menu_select_callback(int index, void *ctx){
    Log.d("cgms", "menu select callback");
       // addInterceptToMenu();
        //some debug for when paired with phone
        //get a dump of the cal array through cloud pebble
        for (int i = 0; i < 5; i++ ) {
        if (cal.calibrations_arr[i].rawcounts > 0) {
        }
        }

        if( s_first_menu_items[index].subtitle==NULL){
        s_first_menu_items[index].subtitle="Selected";
        }else{
        s_first_menu_items[index].subtitle=NULL;
        }

        if (*s_first_menu_items[index].title=='E'){
        APP_LOG(APP_LOG_LEVEL_DEBUG, "enter was selected");
        for (int i = 1; i < 16; i++) {
            if (s_first_menu_items[i].subtitle != NULL && *s_first_menu_items[i].subtitle == 'S'){
                //
                slopecount = 0;
                if (*s_first_menu_items[i].title == 'R'){
                    resetCal();
                    newCal = false;
                    break;
                }else if (*s_first_menu_items[i].title == 'Z'){
                    sleepCount = 15;
                    break;
                }else{
                    //its a calibration
                    if (i >= 3 && i <= 15) {
                        calibrate(atoi(s_first_menu_items[i].title));
                    }
                    break;
                }
            }
        }
        //reset menu
        for (int i = 0; i < 16; i++) {
            s_first_menu_items[i].subtitle = NULL;
        }
    }

*/

/*
    public    void reCalibrate(

        if (read.readings_arr[0].seconds  - calTime > 20 && newCal) { //16 minutes missed readings, clear it
        newCal = false;
        }

        if (read.readings_arr[0].seconds  - calTime > 11 && newCal) { //11 minutes
        newCal = false;
        updateRawcount(readings_arr[0].rawcounts);
        cal.calcSlopeandInt();
        //addInterceptToMenu();
        }

        }






public void cgms_display(uint32_t isig){
        int timeToLimit=100;
        int tmpSlope=slope;
        APP_LOG(APP_LOG_LEVEL_DEBUG, "cgms_display");
        getSlopeAndInt();

        //slope changed alert user
        //if(slope!=tmpSlope){
        //  vibes_enqueue_custom_pattern(pat);
        //}

        APP_LOG(APP_LOG_LEVEL_DEBUG, "ISIG KEY");
        //  isig = (new_tuple->value->uint32);
        APP_LOG(APP_LOG_LEVEL_DEBUG, "ISIG %d", (int)isig);
        APP_LOG(APP_LOG_LEVEL_DEBUG, "GLUCOSE %d", currentGlucose);
        APP_LOG(APP_LOG_LEVEL_DEBUG, "Intercept %d", intercept);
        APP_LOG(APP_LOG_LEVEL_DEBUG, "Slope %d", slope);

        if(newCal==true){
        reCalibrate();
        }

        if (isig != 0  && slope != 0 && intercept != 0 ) {
        currentGlucose = ((isig - intercept) / slope);
        addReading(currentGlucose, isig);
        double readingSlope = getSlopeGlucose();
        //
        if(readingSlope==0){
        snprintf(glucbuf, sizeof(glucbuf), "%d  -", currentGlucose);
        }else{
        snprintf(glucbuf, sizeof(glucbuf), "%d  ", currentGlucose);
        }
        if (readingSlope < 0) {
        //45 down
        if (abs(readingSlope) >= 1)
        snprintf(glucbuf, sizeof(glucbuf), "%d  \\", currentGlucose);
        //straight down
        if (abs(readingSlope) >= 2)
        snprintf(glucbuf, sizeof(glucbuf), "%d V", currentGlucose);
        if (abs(readingSlope) >= 3) {
        snprintf(glucbuf, sizeof(glucbuf), "%d  VV", currentGlucose);
        vibes_enqueue_custom_pattern(pat);
        }
        }

        if (readingSlope > 0) {
        if (readingSlope >= 1)
        snprintf(glucbuf, sizeof(glucbuf), "%d  /", currentGlucose);
        if (readingSlope >= 2)
        snprintf(glucbuf, sizeof(glucbuf), "%d  ^", currentGlucose);
        if (readingSlope >= 3) {
        snprintf(glucbuf, sizeof(glucbuf), "%d  ^^", currentGlucose);
        vibes_enqueue_custom_pattern(pat);
        }
        }
        timeToLimit=100;
        if (readingSlope > 0 && currentGlucose < 180) {
        //how long until 180
        timeToLimit = abs((180 - currentGlucose) / readingSlope);
        //since the dex is ~15 minutes behind reality
        timeToLimit = timeToLimit - 15;
        }
        if (readingSlope < 0 && currentGlucose > 80) {
        timeToLimit = abs((currentGlucose - 80) / readingSlope);
        //since the dex is ~15 minutes behind reality
        timeToLimit = timeToLimit - 15;
        }
        if (timeToLimit <= 0) {
        timeToLimit = 1;
        }

        #ifdef TESTING
        timeToLimit=10;
        readingSlope=2;
        #endif

        if (timeToLimit < 99) {
        if (readingSlope < 0) {
        snprintf(buf, sizeof(buf), "V %d", timeToLimit);
        }
        if (readingSlope > 0) {
        snprintf(buf, sizeof(buf), "^ %d", timeToLimit);
        }
        } else {
        snprintf(buf, sizeof(buf), "   ");
        }

        if (abs(lastGlucose - currentGlucose ) > 25 && lastGlucose != 0 && currentGlucose != 0) {
        text_layer_set_text(alert_layer, "???");
        }

        readingAdded = false;
        if (currentGlucose > 20) {
        if (sleepCount==0){
        alerts(currentGlucose,timeToLimit);
        }
        } else {
        text_layer_set_text(alert_layer, "  ?");
        }
        lastGlucose = currentGlucose;
        char sign='+';
        if (readingSlope<0){
        sign='-';
        }

        if((abs((int)readingSlope)==0) && (abs((int)(readingSlope*10)%10)==0)){
        snprintf(testbuf, sizeof(testbuf), " ---\n%d", (int)slope);
        }else{
        snprintf(testbuf, sizeof(testbuf), "%c%d.%d\n%d", sign,abs((int)readingSlope),abs((int)(readingSlope*10)%10),(int)slope);
        }

        snprintf(glucbuf, sizeof(glucbuf), "%d",(int)currentGlucose);
        if(timeToLimit<99){
        snprintf(buf, sizeof(buf), "%c%d", sign,timeToLimit);
        } else {
        snprintf(buf, sizeof(buf), "    ");
        }
        text_layer_set_text(timetolimit_layer, buf);

        text_layer_set_text(glucose_layer, glucbuf);
        text_layer_set_text(test_layer, testbuf);
        APP_LOG(APP_LOG_LEVEL_DEBUG, "Calc GLUCOSE %d", (int)currentGlucose);
        }
        }



public void tick_handler(struct tm *tick_time, TimeUnits units_changed) {
        //this also seems to get called when ios connects
        //not just every minute...
        miss_count++;

        if (sleepCount>0){
        sleepCount--;
        }
        APP_LOG(APP_LOG_LEVEL_DEBUG, "In tick handler %i, %i", miss_count, sensor_miss_count);
        #ifdef SMARTSTRAP
        strap_request_data("In tick handler") ;
        #endif
        if (miss_count > 6 && miss_count<=11) {
        APP_LOG(APP_LOG_LEVEL_DEBUG, "One Miss recorded");
        text_layer_set_text(alert_layer, "  !");
        }
        if(miss_count > 13) {
        APP_LOG(APP_LOG_LEVEL_DEBUG, "Miss recorded");
        vibes_enqueue_custom_pattern(pat);
        text_layer_set_text(alert_layer, " !!");
        }

        layer_mark_dirty(window_get_root_layer(s_main_window));

        #ifdef TESTING
        cgms_display(80000); // 71mg/dl
        //cgms_display(70000);  //56 mg/dl
        miss_count=0;
        #endif

        }
*/

}