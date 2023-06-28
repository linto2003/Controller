package com.example.bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;


public class BootLoad extends AppCompatActivity {

    ImageView logoImageTop, logoImageBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boot_load);

        /*
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        };
        timer.schedule(task, 2000);
         */

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screen_width = displayMetrics.widthPixels;

        logoImageTop = findViewById(R.id.top);
        logoImageBottom = findViewById(R.id.bottom);

        Animation animateImageTop = new TranslateAnimation(0, 0, -400, -150);
        animateImageTop.setDuration(2000);
        animateImageTop.setFillAfter(true);
        logoImageTop.startAnimation(animateImageTop);

        Animation animateImageBottom = new TranslateAnimation(0, 0, 800, 190);
        animateImageBottom.setDuration(2000);
        animateImageBottom.setFillAfter(true);
        logoImageBottom.startAnimation(animateImageBottom);

        animateImageBottom.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }
}