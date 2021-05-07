package com.rikucherry.backgroundcountdowntimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import static com.rikucherry.backgroundcountdowntimer.Constants.ACTION_DELETE_NOTIFICATION;
import static com.rikucherry.backgroundcountdowntimer.Constants.TIMER_REMAIN_TIME;
import static com.rikucherry.backgroundcountdowntimer.Constants.TIMER_SET_TIME;

public class MainActivity extends AppCompatActivity {

    private TextView displayTime;
    private Button startButton;
    private Button resetButton;
    private NotificationReceiver mReceiver = new NotificationReceiver();
    private CountDownTimer timer;

    private final long initialTimeMilli = 60_000; //初设时长1分钟
    private long remainTimeMilli = initialTimeMilli; //剩余时间

    private enum STATE {
        STARTED, STOPPED
    }

    private STATE state;
    private AlarmManager alarmManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayTime = findViewById(R.id.text_time);
        startButton = findViewById(R.id.button_start);
        resetButton = findViewById(R.id.button_reset);

        state = STATE.STOPPED;

        // 注册接收器，用于接收用户删除通知后的行为
        registerReceiver(mReceiver, new IntentFilter(ACTION_DELETE_NOTIFICATION));

        // 初始化显示时间
        updateText(initialTimeMilli);
        updateButton();

        startButton.setOnClickListener(v -> {
            state = STATE.STARTED;
            startTimer();
            updateButton();
        });
        resetButton.setOnClickListener(v -> {
            resetTimer();
            updateButton();
        });
    }


    private void startTimer() {
        timer = new CountDownTimer(remainTimeMilli, 1000) {
            @Override
            public void onTick(long millisUntilFinish) {
                remainTimeMilli = millisUntilFinish;
                updateText(remainTimeMilli);
            }

            @Override
            public void onFinish() {
                resetTimer();
                Toast.makeText(MainActivity.this,"Timer expired!!!", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 获取上次离开时的剩余时间，设置提醒的时间以及当前时间
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        long lastRemainTimeMilli = preference.getLong(TIMER_REMAIN_TIME,0);
        long lastSetTimeMilli = preference.getLong(TIMER_SET_TIME, 0);
        long pastTimeMilli = System.currentTimeMillis() - lastSetTimeMilli;

        if (lastSetTimeMilli <= 0) {// 上一次离开时计时器未启动
            // Do nothing
        } else if (pastTimeMilli < lastRemainTimeMilli){ // 计时器已启动且计时未完成
            remainTimeMilli = lastRemainTimeMilli - pastTimeMilli;
            state = STATE.STARTED;
            startTimer();
        } else { // 计时器已启动且计时已完成
            resetTimer();
        }

        // todo : 取消alarm和通知
        if (alarmManager != null) {
            removeAlarm();
        }
        preference.edit().clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 计时途中离开时储存剩余时间到和当前时间到sp
        // 下一次回到应用时，须通过比较离开经过的时间和计时器剩余时间来判断计时是否失效
        if (state == state.STARTED) {
            timer.cancel();
            SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
            preference.edit().putLong(TIMER_REMAIN_TIME, remainTimeMilli).apply();
            preference.edit().putLong(TIMER_SET_TIME,System.currentTimeMillis()).apply();

            //设置计时提醒
            setAlarm();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void resetTimer() {
        if (state == STATE.STARTED) {
            timer.cancel();
        }
        state = STATE.STOPPED;
        remainTimeMilli = initialTimeMilli;
        updateText(remainTimeMilli);
        updateButton();
    }


    private void updateText(long remainTimeMilli) {
        long minutes = remainTimeMilli / (1000 * 60);
        long seconds = remainTimeMilli / 1000 % 60;
        displayTime.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void setAlarm(){
        long alarmTime = System.currentTimeMillis() + remainTimeMilli;
        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, TimerExpiredReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(this,123,intent,0);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pending);
    }

    private void removeAlarm(){
        Intent intent = new Intent(this, TimerExpiredReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(this,123,intent,0);
        alarmManager.cancel(pending);
    }

    private void updateButton(){
        switch (state) {
            case STARTED:
                startButton.setEnabled(false);
                resetButton.setEnabled(true);
            break;
            case STOPPED:
                startButton.setEnabled(true);
                resetButton.setEnabled(false);
            break;
        }

    }

    public class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == ACTION_DELETE_NOTIFICATION) {
                resetTimer();
                updateButton();
            }
        }
    }

}