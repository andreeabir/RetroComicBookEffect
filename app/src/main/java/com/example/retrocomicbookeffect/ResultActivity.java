package com.example.retrocomicbookeffect;

import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.retrocomicbookeffect.transformations.ColorHalftoneTransformation;
import com.squareup.picasso.Picasso;

import jp.wasabeef.picasso.transformations.CropTransformation;
import jp.wasabeef.picasso.transformations.gpu.VignetteFilterTransformation;

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

    private void loadImage(int halftone) {
        float v = 1f - (VIGNETTE / 100f);
        Picasso.with(getApplicationContext())
                .load(imgUri)
                .transform(new CropTransformation(1, CropTransformation.GravityHorizontal.CENTER,
                        CropTransformation.GravityVertical.CENTER))
                .transform(new ColorHalftoneTransformation(halftone))
                .transform(new VignetteFilterTransformation(getApplicationContext(), new PointF(0.5f, 0.5f),
                        new float[]{0.0f, 0.0f, 0.0f}, 0f, v))
                .placeholder(R.color.placeholder)
                .error(R.color.error)
                .into(image);

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
