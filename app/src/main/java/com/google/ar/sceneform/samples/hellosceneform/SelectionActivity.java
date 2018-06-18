package com.google.ar.sceneform.samples.hellosceneform;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class SelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        ImageButton model1Bu = findViewById(R.id.model1Bu);
        ImageButton model2Bu = findViewById(R.id.model2Bu);

        model1Bu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SelectionActivity.this, HelloSceneformActivity.class);
                i.putExtra("model", 1);
                startActivity(i);
            }
        });

        model2Bu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SelectionActivity.this, HelloSceneformActivity.class);
                i.putExtra("model", 2);
                startActivity(i);
            }
        });
    }
}
