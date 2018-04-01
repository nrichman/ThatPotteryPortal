package com.pottery.christy.potteryportal;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class PlaceOrder extends AppCompatActivity {
    TextView itemCount;
    ImageView testImageView;
    RelativeLayout innerLayout;
    int itemsAddedId = 0;
    int textAddedId = 300;

    List<EditText> allEds = new ArrayList<EditText>();
    List<Bitmap> allPics = new ArrayList<Bitmap>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);
        itemCount = (TextView) findViewById(R.id.itemCount);
        innerLayout = (RelativeLayout) findViewById(R.id.InnerLayout);

        testImageView = (ImageView) findViewById(R.id.imageView);

//// WORKING PART
//        RequestQueue queue = Volley.newRequestQueue(this);
//        Map<String, String> postParam= new HashMap<String, String>();
//        postParam.put("word", "christytest");
//        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
//                "http://that-pottery-portal.herokuapp.com/test_write", new JSONObject(postParam),
//                new Response.Listener<JSONObject>() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        Log.d("RESPONSE:", response.toString());
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Log.d("ERROR RESPONSE:", "Error: " + error.getMessage());
//            }
//        }) {
//            /**
//             * Passing some request headers
//             * */
//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                HashMap<String, String> headers = new HashMap<String, String>();
//                headers.put("Accept", "application/json");
//                return headers;
//            }
//        };
//        queue.add(jsonObjReq);
//// END WORKING PART


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
        allEds.add(tv1);
        allPics.add(imageBitmap);
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

        String ID = Integer.toString(textAddedId);

        Log.d("ADDED ID",ID);
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
        Log.d("Test","IS HEREE");

        //// WORKING PART
//        RequestQueue queue = Volley.newRequestQueue(this);
//        Map<String, String> postParam= new HashMap<String, String>();
//        postParam.put("word", "christytest");
//        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
//                "http://that-pottery-portal.herokuapp.com/test_write", new JSONObject(postParam),
//                new Response.Listener<JSONObject>() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        Log.d("RESPONSE:", response.toString());
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Log.d("ERROR RESPONSE:", "Error: " + error.getMessage());
//            }
//        }) {
//            /**
//             * Passing some request headers
//             * */
//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                HashMap<String, String> headers = new HashMap<String, String>();
//                headers.put("Accept", "application/json");
//                return headers;
//            }
//        };
//        queue.add(jsonObjReq);
//// END WORKING PART

        // CUSTOMER NAME
        EditText mEdit;
        mEdit   = (EditText)findViewById(R.id.editText);
        Log.d("CUSTOMER NAME", mEdit.getText().toString());

        // CUSTOMER PHONE
        EditText mEdit2;
        mEdit2   = (EditText)findViewById(R.id.editText2);
        Log.d("CUSTOMER NAME", mEdit2.getText().toString());

        String ItemCount = Integer.toString(allEds.size());
        Log.d("ITEM COUNT", ItemCount);


        for(int i=0; i < allEds.size(); i++){
            Log.d("ITEMS",allEds.get(i).getText().toString());
//            Log.d("IMAGE:", getBytes(allPics.get(i)).toString());
        }


//        Bitmap TestBitMap = null;
//        testImageView.setImageBitmap(getImage(getBytes(allPics.get(last))));

//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);


    }

    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    public static byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

}
