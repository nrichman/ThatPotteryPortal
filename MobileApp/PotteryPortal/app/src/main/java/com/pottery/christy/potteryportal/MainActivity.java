package com.pottery.christy.potteryportal;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

// MainActivity is the main/starting view of the application
// From here the user can go to enter the order
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    // This method start the PlaceOrder activity
    public void launchActivity(View v) {

        Intent intent = new Intent(this, PlaceOrder.class);
        startActivity(intent);
    }
}
