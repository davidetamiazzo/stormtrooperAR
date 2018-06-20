package com.google.ar.sceneform.samples.hellosceneform;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Plane.Type;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * This is an activity that uses the Sceneform UX package to place 3D models on a surface.
 */
public class SceneformActivity extends AppCompatActivity {

    private static final String TAG = "sceneform"; //for Log purposes
    private static final int COLOR = 1; //request code necessary in onActivityResult, which handles the detected color
    private int model; //1 for the Stormtrooper, 2 for BB8

    private ArFragment arFragment;
    private ModelRenderable myRenderable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ux);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        ImageButton cameraButton = findViewById(R.id.cameraClick);
        ImageButton colorDetectionBu = findViewById(R.id.colorDetection);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });

        //set the button on the left to start the color detection
        //it will return an event caught in onActivityResult
        colorDetectionBu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SceneformActivity.this, ColorDetectionActivity.class);
                startActivityForResult(i, COLOR);
            }
        });

        //get model selected from first activity
        Intent intent = getIntent();
        model = intent.getIntExtra("model", 1);

    }

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onResume() {
        super.onResume();
        //always use in portrait mode, the buttons are symmetric so can be used in landscape without any difference
        Configuration newConfig = getResources().getConfiguration();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        }

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept() which sets the new "stage" in the CompletableFuture.
        // In this case, the last action to be performed is loading the Renderable. In case of failure, Toast

        //load the Stormtrooper model
        if (model == 1) {
            ModelRenderable.builder()
                    .setSource(this, R.raw.stormtrooper)
                    .build()
                    .thenAccept(renderable -> myRenderable = renderable)
                    .exceptionally(
                            throwable -> {
                                Toast toast =
                                        Toast.makeText(this, "Unable to load bb8 renderable", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                                return null;
                            });
        }
        else //model == 2, BB8
        {
            ModelRenderable.builder()
                    .setSource(this, R.raw.bb8)
                    .build()
                    .thenAccept(renderable -> myRenderable = renderable)
                    .exceptionally(
                            throwable -> {
                                Toast toast =
                                        Toast.makeText(this, "Unable to load bb8 renderable", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                                return null;
                            });
        }

        //set the tap listener directly from the fragment, which handles taps on the flat surfaces detected
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (myRenderable == null) {
                        return;
                    }

                    if (plane.getType() != Type.HORIZONTAL_UPWARD_FACING) {
                        return;
                    }

                    // Set the hitPose higher for stormtrooper (1) and bb8 (2) since they are not imported in proper position
                    // Create the Anchor first
                    Anchor anchor;
                    if (model == 1) {
                        anchor = hitResult.getTrackable().createAnchor(
                                hitResult.getHitPose().compose(Pose.makeTranslation(0, 1.05f, 0)));
                    }

                    else //model == 2
                        anchor  = hitResult.getTrackable().createAnchor(
                                hitResult.getHitPose().compose(Pose.makeTranslation(0, -0.13f, 0f)));


                    //The AnchorNode automatically detects the world space coordinates in the Anchor location
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setAnchor(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    // Create the transformable node and add it to the anchor, and attach the Renderable to it
                    TransformableNode tn = new TransformableNode(arFragment.getTransformationSystem());
                    tn.setParent(anchorNode);
                    tn.setRenderable(myRenderable);
                    tn.select();


                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        arFragment.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        arFragment.onDestroyView();
    }

    //used in takePhoto, generates the file name with extension .jpg
    private String generateFilename() {
        String date =
                new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/" + date + "_screenshot.jpg";
    }

    //used in takePhoto, we write the bitmap created to the selected location
    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {

        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }

        //The writing passes through a ByteArrayOutputStream, which gets the result of the JPEG compression
        //Then the compressed stream is passed to a FileOutputStream, which can write to the specific location
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex); //pass the exception to the father method
        }
    }

    private void takePhoto() {
        final String filename = generateFilename();
        ArSceneView view = arFragment.getArSceneView(); //the actual image being shown at the moment

        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(SceneformActivity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }

                //Show snackbar to inform that the photo was taken, and allows to see it
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Photo saved", Snackbar.LENGTH_LONG);
                snackbar.setAction("Open in Photos", v -> {
                    File photoFile = new File(filename);

                    Uri photoURI = FileProvider.getUriForFile(SceneformActivity.this,
                            SceneformActivity.this.getPackageName() + ".FileProvider",
                            photoFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                    intent.setDataAndType(photoURI, "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);

                });
                snackbar.show();
            } else {
                Toast toast = Toast.makeText(SceneformActivity.this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String detectedColor = "";

        // Check which request we're responding to
        if (requestCode == COLOR) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                detectedColor = data.getStringExtra("COLOR");

                if (detectedColor.equals("RED"))
                {
                    model = 1; //stormtrooper
                    Toast toast = Toast.makeText(SceneformActivity.this,
                            "Switched to Stormtrooper model", Toast.LENGTH_LONG);
                    toast.show();
                }
                else if (detectedColor.equals("GREEN"))
                {
                    model = 2; //bb8
                    Toast toast = Toast.makeText(SceneformActivity.this,
                            "Switched to BB8 model", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }
    }
}
