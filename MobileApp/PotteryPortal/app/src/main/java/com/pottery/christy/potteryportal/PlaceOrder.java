package com.pottery.christy.potteryportal;

import com.cloudinary.android.MediaManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PlaceOrder extends AppCompatActivity {

    private static final int INPUT_SIZE = 299;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128;
    private static final String INPUT_NAME = "Mul";
    private static final String OUTPUT_NAME = "final_result";

    private static final String MODEL_FILE = "file:///android_asset/optimized_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/retrained_labels.txt";

    TextView itemCount;
    RelativeLayout innerLayout;
    int itemsAddedId = 0;
    int textAddedId = 300;
    int suggestionsAddedId = 500;
    List<EditText> allEds = new ArrayList<EditText>();
    List<Bitmap> allPics = new ArrayList<Bitmap>();
    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    public Handler mHandler;
    static final int REQUEST_IMAGE_CAPTURE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MediaManager.init(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);
        itemCount = (TextView) findViewById(R.id.itemCount);
        innerLayout = (RelativeLayout) findViewById(R.id.InnerLayout);
        mHandler = new Handler();

        initTensorFlowAndLoadModel();
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    private void runThread(Classifier classifier,Bitmap imageBitmap,TextView suggestion) {
        new Thread() {
            public void run() {
                final List<Classifier.Recognition> tensorFlowResults = classifier.recognizeImage(imageBitmap);
                for (int i =0; i < tensorFlowResults.size(); i++ ) {
                    Log.d("Result:",tensorFlowResults.get(i).getTitle());
                }
                if (tensorFlowResults.size() > 0) {
                    String suggested = tensorFlowResults.get(0).getTitle();
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                PlaceOrder order = new PlaceOrder();
                                suggestion.setText(suggested);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public void addNewElement(Bitmap imageBitmap) {
        itemsAddedId++;
        textAddedId++;
        suggestionsAddedId++;

        // ADD IMAGE
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

        // ADD ENTER POTTERY PIECE NAME TEXT BOX
        RelativeLayout.LayoutParams lprams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        EditText tv1 = new EditText(this);
        allEds.add(tv1);
        allPics.add(imageBitmap);
        tv1.setHint("Enter Here");

        lprams.addRule(RelativeLayout.RIGHT_OF, itemsAddedId);
        if (itemsAddedId > 1){
            int aboveID2 = textAddedId - 1;
            lprams.addRule(RelativeLayout.BELOW, aboveID2);
        }

        lprams.addRule(RelativeLayout.ALIGN_BOTTOM, itemsAddedId);
        lprams.height = 200;
        lprams.width = 250;
        tv1.setLayoutParams(lprams);
        tv1.setId(textAddedId);
        tv1.setGravity(Gravity.BOTTOM);

        String ID = Integer.toString(textAddedId);
        Log.d("ADDED ID",ID);
        tv1.setTextSize(16);

        innerLayout.addView(tv1);

        // ????
        ImageView potteryPic = (ImageView) findViewById(itemsAddedId);
        potteryPic.setImageBitmap(imageBitmap);

        String totalCount = String.valueOf(itemsAddedId);
        itemCount.setText(totalCount);
        String suggestedText = "SUGGESTED";

        RelativeLayout.LayoutParams lprams2 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lprams2.addRule(RelativeLayout.RIGHT_OF, textAddedId);
        lprams2.addRule(RelativeLayout.ALIGN_BOTTOM, textAddedId);
        TextView suggestion = new TextView(this);
        suggestion.setText(suggestedText);
        suggestion.setId(suggestionsAddedId);
        suggestion.setTextSize(16);
        suggestion.setWidth(150);
        suggestion.setHeight(100);
        suggestion.setGravity(Gravity.CENTER);
        suggestion.setClickable(true);
        suggestion.setOnClickListener(btnclick);
        suggestion.setBackgroundColor(Color.parseColor("#ccffe6"));
        suggestion.setLayoutParams(lprams2);
        innerLayout.addView(suggestion);

        runThread(classifier, imageBitmap,suggestion);
    }

        View.OnClickListener btnclick = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            Log.d("click", Integer.toString(view.getId()) );
            int idToChange = view.getId();
            idToChange = idToChange - 200;

            EditText et = findViewById(idToChange);

            TextView tv = findViewById(view.getId());
            String changeTo = tv.getText().toString() + " ";
            Log.d("CHECK VALUE", changeTo);
            et.setText(changeTo);
            et.setSelection(et.getText().length());
            et.requestFocus();
        }
    };

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
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }

    public void gotoMainActivity(View v) {

        // CALL FOR ORDER NUMBER
        String url = "http://that-pottery-portal.herokuapp.com/get_order_num";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("RESPONSE ORDERNUM", response);
                        processOrder(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.d("RESPONSE ORDERNUM", error.toString());
                Toast.makeText(getApplicationContext(),"ERROR PROCESSING PLEASE TRY AGAIN",Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(stringRequest);

        // END - DISPLAY MESSAGE TO USER
        Toast.makeText(getApplicationContext(),"FINISHED UPLOAD",Toast.LENGTH_SHORT).show();

//        // CUSTOMER NAME
//        EditText mEdit;
//        mEdit   = (EditText)findViewById(R.id.editText);
//        Log.d("CUSTOMER NAME", mEdit.getText().toString());
//        // CUSTOMER PHONE
//        EditText mEdit2;
//        mEdit2   = (EditText)findViewById(R.id.editText2);
//        Log.d("CUSTOMER NAME", mEdit2.getText().toString());
//
//        String ItemCount = Integer.toString(allEds.size());
//        Log.d("ITEM COUNT", ItemCount);
//
//        for(int i=0; i < allEds.size(); i++){
//            Log.d("ITEMS",allEds.get(i).getText().toString());
//        }

    }
    public void processOrder(String number) {
        String orderNumber = number;

        // UPLOAD IMAGES
        int numberOfItems = allEds.size();
                for (int i = 0; i< numberOfItems; i++) {
            // UPLOAD IMAGE AND CREATE IMAGEURL?
            Bitmap testBitmap = allPics.get(i);
            Uri tempUri = getImageUri(getApplicationContext(), testBitmap);
            File finalFile = new File(getRealPathFromURI(tempUri));
            String createdUrl = "order_" + orderNumber + "_" + Integer.toString(i+1);
            String requestId = MediaManager.get().upload(finalFile.getPath()).option("public_id", createdUrl).dispatch();
            Log.d("URL CREATED:", requestId);
            // END UPLOAD
        }


        // SEND URLS TO PYTHON SCRIPT
        Log.d("SEND URLS", "SENDING URLS TO PYTHON SCRIPT");
        //        RequestQueue queue = Volley.newRequestQueue(this);
//        Map<String, String> postParam= new HashMap<String, String>();
//        String allItemNames = "";
//        for (int i = 0; i < allEds.size(); i++) {
//            allItemNames = allItemNames + Integer.toString(i) + " : " + allEds.get(i).getText().toString() + "\n";
//        }
//        postParam.put("orderNum", orderNumber);
//        postParam.put("items", allItemNames);
//        postParam.put("word", "christytest2");
////        postParam.put("image", testBitmap);
//        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
//                "http://that-pottery-portal.herokuapp.com/????", new JSONObject(postParam),
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

        // SEND ORDER DATA TO PYTHON SCRIPT

        // CUSTOMER NAME
        EditText mEdit;
        mEdit   = (EditText)findViewById(R.id.editText);
//        Log.d("CUSTOMER NAME", mEdit.getText().toString());
        // CUSTOMER PHONE
        EditText mEdit2;
        mEdit2   = (EditText)findViewById(R.id.editText2);
//        Log.d("CUSTOMER NAME", mEdit2.getText().toString());

        Log.d("SEND ORDER", "SENDING ORDER DATA TO PYTHON SCRIPT");

        RequestQueue queue = Volley.newRequestQueue(this);
        Map<String, String> postParam= new HashMap<String, String>();

        postParam.put("order_num", orderNumber);
        postParam.put("name", mEdit.getText().toString());
        postParam.put("phone", mEdit2.getText().toString());
        postParam.put("email", "christytest@test.com");
        postParam.put("notes", "Test Notes here");
        postParam.put("num_items", Integer.toString(numberOfItems));

//        postParam.put("image", testBitmap);
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                "http://that-pottery-portal.herokuapp.com/insert_order", new JSONObject(postParam),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("ORDER GOOD RESPONSE:", response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ORDER ERROR RESPONSE:", "Error: " + error.getMessage());
            }
        }) {
            /**
             * Passing some request headers
             * */
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        queue.add(jsonObjReq);
    }
}
