package com.example.retrocomicbookeffect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.retrocomicbookeffect.transformations.ColorHalftoneTransformation;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Random;

import jp.wasabeef.picasso.transformations.CropTransformation;
import jp.wasabeef.picasso.transformations.gpu.ContrastFilterTransformation;

class MyThread extends Thread {
    Context context;
    Uri imgUri;
    int halftone;
    Bitmap bitmap;

    public MyThread(Context context, Uri imageUri, int halftone){
        this.context = context;
        this.imgUri = imageUri;
        this.halftone = halftone;
    }

    @Override
    public void run() {
        if (halftone > 0) {
            try {
                bitmap = Picasso.with(context)
                        .load(imgUri)
                        .transform(new CropTransformation(1, CropTransformation.GravityHorizontal.CENTER,
                                CropTransformation.GravityVertical.CENTER))
                        .transform(new ColorHalftoneTransformation(halftone))
                        .placeholder(R.color.placeholder)
                        .error(R.color.error)
                        .get();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            try {
                bitmap = Picasso.with(context)
                        .load(imgUri)
                        .transform(new CropTransformation(1, CropTransformation.GravityHorizontal.CENTER,
                                CropTransformation.GravityVertical.CENTER))
                        .transform(new ContrastFilterTransformation(context, 0.5f))
                        .placeholder(R.color.placeholder)
                        .error(R.color.error)
                        .get();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap getValue() {
        return bitmap;
    }
}

public class ResultActivity extends AppCompatActivity {
    private ImageView image;
    private Button changeBtn;
    private boolean state = true;
    private Uri imgUri = null;
    private int VIGNETTE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        image = findViewById(R.id.image);
        changeBtn = findViewById(R.id.btn_change);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String uri_str = bundle.getString("uri_str");
            imgUri = Uri.parse(uri_str);
        }

        changeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state) {
                    changeBtn.setText("Comic");
                    loadImageSimple();
                } else {
                    changeBtn.setText("Normal");
                    loadImage(4);
                }
                state = !state;
            }
        });
        loadImage(4);
    }

    public static Bitmap applyGrain(Bitmap source, int percentNoise) {
        // get source image size
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        // get pixel array from source
        source.getPixels(pixels, 0, width, 0, 0, width, height);
        // create a random object
        Random random = new Random();

        int index = 0;
        // Note: Declare the c and randColor variables outside of the for loops
        int c = 0;
        int randColor = 0;
        // iterate through pixels
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                if (random.nextInt(101) < percentNoise) {
                    // Skip this iteration a certain percentage of the time
                    continue;
                }
                // get current index in 2D-matrix
                index = y * width + x;
                // get random color
                c = random.nextInt(255);
                randColor = Color.rgb(c, c, c);
                pixels[index] |= randColor;
            }
        }
        Bitmap bmOut = Bitmap.createBitmap(width, height, source.getConfig());
        bmOut.setPixels(pixels, 0, width, 0, 0, width, height);
        return bmOut;
    }

        private void loadImage(int halftone) {
        float v = 1f - (VIGNETTE / 100f);

        MyThread thread_halftone = new MyThread(getApplicationContext(), imgUri, halftone);
        thread_halftone.start();
        try {
            thread_halftone.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Bitmap bitmap_halftone = thread_halftone.getValue();

        MyThread thread_contrast = new MyThread(getApplicationContext(), imgUri, -1);
            thread_contrast.start();
        try {
            thread_contrast.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Bitmap bitmap_normal = thread_contrast.getValue();
        bitmap_normal = applyGrain(bitmap_normal, 5);

        Paint paint = new Paint();
        Bitmap bitmap_view = Bitmap.createBitmap(bitmap_halftone);
        Canvas canvas = new Canvas(bitmap_view);
        canvas.drawBitmap(bitmap_normal, 0, 0, paint);
        PorterDuff.Mode mode = PorterDuff.Mode.DARKEN;
        paint.setXfermode(new PorterDuffXfermode(mode));
        canvas.drawBitmap(bitmap_halftone, 0, 0, paint);

        image.setImageBitmap(bitmap_view);

    }

    private void loadImageSimple() {
        Picasso.with(getApplicationContext())
                .load(imgUri)
                .transform(new CropTransformation(1, CropTransformation.GravityHorizontal.CENTER,
                        CropTransformation.GravityVertical.CENTER))
                .placeholder(R.color.placeholder)
                .error(R.color.error)
                .into(image);
    }
}
