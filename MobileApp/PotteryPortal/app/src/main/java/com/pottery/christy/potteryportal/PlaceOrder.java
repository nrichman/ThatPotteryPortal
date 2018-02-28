package com.pottery.christy.potteryportal;

import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PlaceOrder extends AppCompatActivity {
    TextView itemCount;
    RelativeLayout innerLayout;
    int itemsAddedId = 0;
    int textAddedId = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);
        itemCount = (TextView) findViewById(R.id.itemCount);
        innerLayout = (RelativeLayout) findViewById(R.id.InnerLayout);
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    public void addNewElement(Bitmap imageBitmap) {
        itemsAddedId++;
        textAddedId++;

        ImageView imageviewNew = new ImageView(PlaceOrder.this);
        RelativeLayout innerLayout2 = (RelativeLayout)findViewById(R.id.InnerLayout);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        imageviewNew.setId(itemsAddedId);
        if (itemsAddedId > 1) {
            int aboveID = itemsAddedId - 1;
            params.addRule(RelativeLayout.BELOW, aboveID);
        }

        params.height = 200;
        params.width = 200;
        imageviewNew.setLayoutParams(params);
        innerLayout2.addView(imageviewNew);

        RelativeLayout.LayoutParams lprams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        EditText tv1 = new EditText(this);
        tv1.setText("Enter Name");
        lprams.addRule(RelativeLayout.RIGHT_OF, itemsAddedId);
        if (itemsAddedId > 1){
            int aboveID2 = textAddedId - 1;
            lprams.addRule(RelativeLayout.BELOW, aboveID2);
        }

        lprams.addRule(RelativeLayout.ALIGN_BOTTOM, itemsAddedId);
        lprams.height = 200;
        lprams.width = 800;
        tv1.setLayoutParams(lprams);
        tv1.setId(textAddedId);
        tv1.setTextSize(16);
        tv1.setWidth(200);
        tv1.setHeight(100);
        innerLayout.addView(tv1);
        ImageView potteryPic = (ImageView) findViewById(itemsAddedId);
        potteryPic.setImageBitmap(imageBitmap);

        String totalCount = String.valueOf(itemsAddedId);
        itemCount.setText(totalCount);

    }

    public void dispatchTakePictureIntent(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            addNewElement(imageBitmap);
        }
    }
    public void gotoMainActivity(View v) {

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
