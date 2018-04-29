package com.pottery.christy.potteryportal;

import com.cloudinary.android.MediaManager;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// PlaceOrder class handles all of the logic for the customer adding in the order
// information to be submitted to the web applcation.
public class PlaceOrder extends AppCompatActivity {

    private static final int INPUT_SIZE = 299;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128;
    private static final String INPUT_NAME = "Mul";
    private static final String OUTPUT_NAME = "final_result";
    private static final String MODEL_FILE = "file:///android_asset/optimized_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/retrained_labels.txt";
    Queue<TensorObject> tensorQueue = new PriorityQueue<TensorObject>();

    TextView itemCount;
    RelativeLayout innerLayout;
    Button submitButton;

    int itemsAddedId = 0;
    int textAddedId = 300;
    int suggestionsAddedId = 500;
    int suggestionsAddedId2 = 600;
    int signaturesAddedId = 700;
    List<EditText> allEds = new ArrayList<EditText>();
    List<Bitmap> allPics = new ArrayList<Bitmap>();
    List<EditText> allsignatures = new ArrayList<EditText>();
    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    public Handler mHandler;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    static class TensorObject implements Comparable
    {
        Bitmap bMap;
        TextView text;
        TextView text2;

        TensorObject(Bitmap bMap, TextView text, TextView text2)
        {
            this.bMap = bMap;
            this.text = text;
            this.text2 = text2;
        }

        @Override
        public int compareTo(@NonNull Object o) {
            return 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ONCREATE", "ONCREATE RUNS");
        try {
            MediaManager.init(this);
        } catch (Exception e) {
            Log.d("Exception",e.toString());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);
        itemCount = (TextView) findViewById(R.id.itemCount);
        innerLayout = (RelativeLayout) findViewById(R.id.InnerLayout);

        submitButton = (Button) findViewById(R.id.button3);
        mHandler = new Handler();

        initTensorFlowAndLoadModel();
        Log.d("Thread Creation", "runThread called next");
        runThread();
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

    // Separate thread for tensorflow to run in the background
    private void runThread() {
        new Thread() {
            public void run () {
                while (true) {

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!tensorQueue.isEmpty())
                    {
                        TensorObject tObject = tensorQueue.remove();
                        Log.d("tObject bMap", tObject.bMap.toString());
                        Log.d("tObject text", tObject.text.getText().toString());
                        Log.d("Classifier", classifier.toString());
                        final List<Classifier.Recognition> tensorFlowResults = classifier.recognizeImage(tObject.bMap);
                        Log.d("Here",tensorFlowResults.get(0).getTitle());
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
                                        if (tensorFlowResults.size() > 1) {
                                            String suggested2 = tensorFlowResults.get(1).getTitle();
                                            tObject.text2.setText(suggested2);
                                            tObject.text2.setBackgroundResource(R.drawable.suggestion_background);
                                        }
                                        tObject.text.setText(suggested);
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }.start();
    }

    // Adding a new Item
    // This will create a thumbnail of the item picture that was just taken
    // and will create the input sections for the new item including the item name
    // item signature, and the buttons for suggestions
    public void addNewElement(Bitmap imageBitmap) {
        itemsAddedId++;
        textAddedId++;
        suggestionsAddedId++;
        suggestionsAddedId2++;
        signaturesAddedId++;

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
        params.width = 150;
        params.setMargins(0,50,0,0);
        imageviewNew.setLayoutParams(params);
        innerLayout2.addView(imageviewNew);

        // ADD ENTER POTTERY PIECE NAME TEXT BOX
        RelativeLayout.LayoutParams lprams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        EditText tv1 = new EditText(this);
        allEds.add(tv1);
        allPics.add(imageBitmap);
        tv1.setHint(R.string.itemHint);
        lprams.addRule(RelativeLayout.RIGHT_OF, itemsAddedId);
        lprams.setMargins(5,0,5,0);
        lprams.addRule(RelativeLayout.ALIGN_BOTTOM, itemsAddedId);
        lprams.width = 170;
        tv1.setLayoutParams(lprams);
        tv1.setId(textAddedId);
        tv1.setGravity(Gravity.CENTER);

        String ID = Integer.toString(textAddedId);
        Log.d("ADDED ID",ID);
        tv1.setTextSize(TypedValue.COMPLEX_UNIT_PX,24);
        tv1.setBackgroundResource(R.drawable.teal_background);

        innerLayout.addView(tv1);

        // ADD SIGNATURE BOX
        RelativeLayout.LayoutParams lpramsSignature = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        EditText signature = new EditText(this);
        allsignatures.add(signature);
        signature.setHint(R.string.signatureHint);
        lpramsSignature.addRule(RelativeLayout.RIGHT_OF, textAddedId);
        lpramsSignature.addRule(RelativeLayout.ALIGN_BOTTOM, textAddedId);
        lpramsSignature.width = 150;
        signature.setLayoutParams(lpramsSignature);
        signature.setId(signaturesAddedId);
        signature.setGravity(Gravity.CENTER);
        signature.setTextSize(TypedValue.COMPLEX_UNIT_PX,24);
        signature.setBackgroundResource(R.drawable.teal_background);
        innerLayout.addView(signature);

        // ADD SUGGESTION
        ImageView potteryPic = (ImageView) findViewById(itemsAddedId);
        potteryPic.setImageBitmap(imageBitmap);

        String totalCount = String.valueOf(itemsAddedId);
        itemCount.setText(totalCount);
        String suggestedText = "loading suggestion...";

        RelativeLayout.LayoutParams lprams2 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lprams2.addRule(RelativeLayout.RIGHT_OF, signaturesAddedId);
        lprams2.addRule(RelativeLayout.ALIGN_BOTTOM, signaturesAddedId);
        lprams2.setMargins(5,0,5,0);
        TextView suggestion = new TextView(this);
        suggestion.setText(suggestedText);
        suggestion.setId(suggestionsAddedId);
        suggestion.setTextSize(40);
        suggestion.setTextSize(TypedValue.COMPLEX_UNIT_PX,18);
        suggestion.setWidth(150);
        suggestion.setHeight(75);
        suggestion.setGravity(Gravity.CENTER);
        suggestion.setClickable(true);
        suggestion.setOnClickListener(btnclick);
        suggestion.setBackgroundResource(R.drawable.suggestion_background);
        suggestion.setTextColor(getResources().getColor(R.color.colorText));
        suggestion.setLayoutParams(lprams2);
        innerLayout.addView(suggestion);

        // ADD SUGGESTION TWO

        RelativeLayout.LayoutParams lprams3 = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lprams3.addRule(RelativeLayout.ABOVE, suggestionsAddedId);
        lprams3.addRule(RelativeLayout.ALIGN_LEFT, suggestionsAddedId);
        lprams3.setMargins(5,0,5,5);
        TextView suggestion2 = new TextView(this);
        suggestion2.setId(suggestionsAddedId2);
        suggestion2.setTextSize(40);
        suggestion2.setTextSize(TypedValue.COMPLEX_UNIT_PX,18);
        suggestion2.setWidth(150);
        suggestion2.setHeight(75);
        suggestion2.setGravity(Gravity.CENTER);
        suggestion2.setClickable(true);
        suggestion2.setOnClickListener(btnclick);
        suggestion2.setBackgroundColor(Color.TRANSPARENT);
        suggestion2.setTextColor(getResources().getColor(R.color.colorText));
        suggestion2.setLayoutParams(lprams3);
        innerLayout.addView(suggestion2);

        // ADD TO TENSORFLOW QUEUE
        tensorQueue.add(new TensorObject(imageBitmap, suggestion, suggestion2));
    }

        View.OnClickListener btnclick = new View.OnClickListener() {
        // Method called when user clicks a suggestion, this will
            // change the item name input to the name suggested
        @Override
        public void onClick(View view) {
            Log.d("click", Integer.toString(view.getId()) );
            int idToChange = view.getId();
            idToChange = idToChange - 200;
            if (idToChange > 400) {
                idToChange = idToChange - 100;
            }

            EditText et = findViewById(idToChange);

            TextView tv = findViewById(view.getId());
            String changeTo = tv.getText().toString() + " ";
            Log.d("CHECK VALUE", changeTo);
            et.setText(changeTo);
            et.setSelection(et.getText().length());
            et.requestFocus();
        }
    };

    // This handles the user taking pictures of items
    public void dispatchTakePictureIntent(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    // This method creates a bitmap of the item image that was just taken
    // by the user, it then calls addNewElement to update the view
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

    // This method is the submit order functionality, it will make a request
    // to the python script to get an available order number, upload the images
    // to cloudinary, then send the final order information to the second python
    // script so it will be available to the web application.
    public void gotoMainActivity(View v) {

        submitButton.setClickable(false);
        submitButton.setBackgroundResource(R.drawable.suggestion_background);

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
        }


        // SEND ORDER DATA TO PYTHON SCRIPT

        // CUSTOMER NAME
        EditText mEdit;
        mEdit   = (EditText)findViewById(R.id.editText);
        // CUSTOMER PHONE
        EditText mEdit2;
        mEdit2   = (EditText)findViewById(R.id.editText2);

        String allItemNames = "";
                for (int i = 0; i < allEds.size(); i++) {
            allItemNames = allItemNames + "+" + allEds.get(i).getText().toString() + "=" + allsignatures.get(i).getText().toString();
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        Map<String, String> postParam= new HashMap<String, String>();

        postParam.put("order_num", orderNumber);
        postParam.put("name", mEdit.getText().toString());
        postParam.put("phone", mEdit2.getText().toString());
        postParam.put("email", "christytest@test.com");
        postParam.put("notes", "Test Notes");
        postParam.put("num_items", Integer.toString(numberOfItems));
        postParam.put("order_items", allItemNames);

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                "http://that-pottery-portal.herokuapp.com/insert_order", new JSONObject(postParam),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(getApplicationContext(),"FINISHED UPLOAD",Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        PlaceOrder.this.finish();
                        startActivity(intent);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),"ERROR UPLOADING",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);

                PlaceOrder.this.finish();
                startActivity(intent);
            }
        }) {

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
