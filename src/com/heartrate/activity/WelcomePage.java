package com.heartrate.activity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
//��ӭ����
@SuppressLint("HandlerLeak")
public class WelcomePage extends Activity{
    
    private static final String S = "Heart_Rate_Detect";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//ȥ��������
        setContentView(R.layout.welcomepage);
        //�����߳�
        Thread mt = new Thread(mThread);
        mt.start();
    }
    

    private Handler mHandler = new Handler(){
        
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

            if((String)msg.obj == S) {
                //��ת
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
            //��ʱ1.5��
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