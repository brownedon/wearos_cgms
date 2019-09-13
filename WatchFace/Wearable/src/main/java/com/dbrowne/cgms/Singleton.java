package com.dbrowne.cgms;

/**
 * Created with IntelliJ IDEA.
 * Date: 13/05/13
 * Time: 10:36
 */
public class Singleton {
    private static Singleton mInstance = null;

    private String mGlucose;
    private Long mActionTime;
    private String mAction;
    private int mActionInt;
    private Long mISIG;
    private Long mISIGTime;
    private String mBluetooth;

    private Long mLastISIG;

    private Singleton(){
        mGlucose = "0";
        mActionTime=0L;
        mAction="none";
        mActionInt=0;
        mISIG=0L;
        mISIGTime=0L;
        mBluetooth="DOWN";
    }

    public static Singleton getInstance(){
        if(mInstance == null)
        {
            mInstance = new Singleton();
        }
        return mInstance;
    }

    public String getGlucose(){
        return this.mGlucose;
    }
    public void setGlucose(String value){
        mGlucose = value;
    }

    public String getAction(){
        return this.mAction;
    }
    public void setAction(String value){
        mAction = value;
    }

    public Long getActionTime(){
        return this.mActionTime;
    }
    public void setActionTime(Long value){
        mActionTime = value;
    }

    public int getActionInt(){
        return this.mActionInt;
    }
    public void setActionInt(int value){
        mActionInt = value;
    }

    public Long getISIGTime(){
        return this.mISIGTime;
    }
    public void setISIGTime(Long value){
        mISIGTime = value;
    }

    public Long getISIG(){
        return this.mISIG;
    }
    public void setISIG(Long value){
        mISIG = value;
    }

    public Long getLastISIG(){
        return this.mLastISIG;
    }
    public void setLastISIG(Long value){
        mLastISIG = value;
    }

    public String getBluetooth(){
        return this.mBluetooth;
    }
    public void setBluetooth(String value){
        mBluetooth = value;
    }


}