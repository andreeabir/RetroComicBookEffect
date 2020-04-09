package com.example.retrocomicbookeffect;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SecondActivity extends AppCompatActivity {

    ImageView imageView2;
    Bitmap bmp = null;
    byte step0_bytes[] = null;
    String step1_url = null;
    String step2_url = null;
    String step3_url = null;
    String step4_url = null;

    Button button0;
    Button button1;
    Button button2;
    Button button3;
    Button button4;

    TextView textView;
    private final OkHttpClient client = new OkHttpClient();

    boolean new_img = false;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        imageView2 = findViewById(R.id.myImageView2);

        button0 = findViewById(R.id.button0);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        button4 = findViewById(R.id.button4);

        textView = (TextView) findViewById(R.id.textView);


        button0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                bmp = BitmapFactory.decodeByteArray(step0_bytes, 0, step0_bytes.length, options);
                imageView2.setImageBitmap(bmp);
                textView.setText(R.string.normal);
            }
        });

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                if (step1_url != null){
                    Glide.with(SecondActivity.this).load(step1_url).into(imageView2);
                    textView.setText(R.string.step1_txt);
                }else{
                    bmp = BitmapFactory.decodeByteArray(step0_bytes, 0, step0_bytes.length, options);
                    imageView2.setImageBitmap(bmp);
                }
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                if (step2_url != null){
                    Glide.with(SecondActivity.this).load(step2_url).into(imageView2);
                    textView.setText(R.string.step2_txt);
                }else{
                    bmp = BitmapFactory.decodeByteArray(step0_bytes, 0, step0_bytes.length, options);
                    imageView2.setImageBitmap(bmp);
                }
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                if (step3_url != null){
                    Glide.with(SecondActivity.this).load(step3_url).into(imageView2);
                    textView.setText(R.string.step3_txt);
                }else{
                    bmp = BitmapFactory.decodeByteArray(step0_bytes, 0, step0_bytes.length, options);
                    imageView2.setImageBitmap(bmp);
                }
            }
        });

        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                if (step4_url != null){
                    Glide.with(SecondActivity.this).load(step4_url).into(imageView2);
                    textView.setText(R.string.step4_txt);
                }else{
                    bmp = BitmapFactory.decodeByteArray(step0_bytes, 0, step0_bytes.length, options);
                    imageView2.setImageBitmap(bmp);
                }
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null){
            String uri_str = bundle.getString("uri_str");
            Uri uri = Uri.parse(uri_str);
            imageView2.setImageURI(uri);
            try {
                InputStream in = getContentResolver().openInputStream(uri);
                ExifInterface exif = new ExifInterface(in);
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotationInDegrees = exifToDegrees(rotation);
                Matrix matrix = new Matrix();
                if (rotation != 0) {matrix.preRotate(rotationInDegrees);}
                final Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                final Bitmap adjustedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new Thread(new Runnable() {

                    public void run() {
                        Bitmap bitmap_scaled = scale(adjustedBitmap, 500, 500);
                        bitmap_scaled.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] byteImage = baos.toByteArray();
                        step0_bytes = byteImage;
                        process_on_server(byteImage);
                    }
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

    // Scale a bitmap preserving the aspect ratio.
    private Bitmap scale(Bitmap bitmap, int maxWidth, int maxHeight) {
        // Determine the constrained dimension, which determines both dimensions.
        int width;
        int height;
        float widthRatio = (float)bitmap.getWidth() / maxWidth;
        float heightRatio = (float)bitmap.getHeight() / maxHeight;
        // Width constrained.
        if (widthRatio >= heightRatio) {
            width = maxWidth;
            height = (int)(((float)width / bitmap.getWidth()) * bitmap.getHeight());
        }
        // Height constrained.
        else {
            height = maxHeight;
            width = (int)(((float)height / bitmap.getHeight()) * bitmap.getWidth());
        }
        Bitmap scaledBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        float ratioX = (float)width / bitmap.getWidth();
        float ratioY = (float)height / bitmap.getHeight();
        float middleX = width / 2.0f;
        float middleY = height / 2.0f;
        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
        return scaledBitmap;
    }

    public void process_on_server(byte[] byteImage){
        String encodedImage = Base64.encodeToString(byteImage, Base64.DEFAULT);
        String result_data = sendData(encodedImage);
    }

    private byte[] getKey(final JSONArray array) {
        final byte[] key = new byte[array.length()];
        for (int i = 0; i < array.length(); i++) {
            try {
                key[i]=(byte)(((int)array.get(i)) & 0xFF);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return key;
    }

    public String sendData(String image) {
        String urlString = "http://192.168.0.183:65432";
        OutputStream out = null;
        String str = "abc";

        try {

            RequestBody formBody = new FormBody.Builder()
                    .add("image", image)
                    .build();

            Request request = new Request.Builder()
                    .url(getResources().getString(R.string.server_url))
                    .post(formBody)
                    .build();

            client.newCall(request)
                    .enqueue(new Callback() {
                        @Override
                        public void onFailure(final Call call, IOException e) {
                            // Error

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // For the example, you can show an error dialog or a toast
                                    // on the main UI thread
                                }
                            });
                        }

                        @Override
                        public void onResponse(Call call, final Response response) throws IOException {
                            String result_data = response.body().string();

                            Log.d("TAG", "Response is " + result_data);
                            // Do something with the response
                            JSONArray arr = null;
                            try {
                                arr = new JSONArray(result_data);
                                JSONObject jObj = arr.getJSONObject(0);
                                step1_url = jObj.getString("step1");
                                step2_url = jObj.getString("step2");
                                step3_url = jObj.getString("step3");
                                step4_url = jObj.getString("step4");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Log.d("TAG", "Response parsed"  );
                        }
                    });
            return "";

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return "0";
    }
}
