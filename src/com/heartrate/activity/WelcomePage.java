package com.heartrate.activity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
//欢迎界面
@SuppressLint("HandlerLeak")
public class WelcomePage extends Activity{
    
    private static final String S = "Heart_Rate_Detect";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        setContentView(R.layout.welcomepage);
        //启动线程
        Thread mt = new Thread(mThread);
        mt.start();
    }
    

    private Handler mHandler = new Handler(){
        
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

            if((String)msg.obj == S) {
                //跳转
                Intent intent = new Intent();
                intent.setClass(WelcomePage.this,Heart_Rate_Detect.class);
                WelcomePage.this.startActivity(intent); 
                finish();
            }
        }
    };
    
    Runnable mThread = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            Message msg = mHandler.obtainMessage();
            //延时1.5秒
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            msg.obj = S;
            mHandler.sendMessage(msg);
        }
        
    };

}