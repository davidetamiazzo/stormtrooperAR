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
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Plane.Type;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity {
    private static final String TAG = "hellosceneform";
    private static final int COLOR = 1;
    private String detectedColor = "";
    private int model = 1;

    private ArFragment arFragment;
    //private ModelRenderable andyRenderable;
    private ModelRenderable myRenderable;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
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

        colorDetectionBu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HelloSceneformActivity.this, ColorDetectionActivity.class);
                startActivityForResult(i, COLOR);
            }
        });

        //get model selected from first activity
        Intent intent = getIntent();
        model = intent.getIntExtra("model", 1);

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().

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
        else //model == 2
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

        arFragment.setOnTapArPlaneListener(
              (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                  if (myRenderable == null) {
                      return;
                  }

                  if (plane.getType() != Type.HORIZONTAL_UPWARD_FACING) {
                      return;
                  }

                  //sets the hitPose higher for stormtrooper and bb8 since they're not imported in proper position
                  Anchor anchor;
                  if (model == 1) {
                      anchor = hitResult.getTrackable().createAnchor(
                              hitResult.getHitPose().compose(Pose.makeTranslation(0, 0.5f, 0)));
                  }

                  else //model == 2
                      anchor = anchor = hitResult.getTrackable().createAnchor(
                              hitResult.getHitPose().compose(Pose.makeTranslation(0, -0.13f, 0f)));


                  // Create the Anchor.
                  //Anchor anchor = hitResult.createAnchor();
                  AnchorNode anchorNode = new AnchorNode(anchor);
                  anchorNode.setAnchor(anchor);
                  anchorNode.setParent(arFragment.getArSceneView().getScene());

                  // Create the transformable node and add it to the anchor.
                  TransformableNode tn = new TransformableNode(arFragment.getTransformationSystem());
                  tn.setParent(anchorNode);
                  tn.setRenderable(myRenderable);
                  tn.select();

                  /*
                  //We need to create an additional Node to tn to rotate the 3d model without
                  // rotating the transformation system
                  Node bb8 = new Node();
                  bb8.setParent(tn);
                  //rotate the model since it's facing downwards by default
                  float[] upAxis = plane.getCenterPose().getYAxis();
                  Vector3 upDirection = new Vector3(-upAxis[0], -upAxis[1], -upAxis[2]);
                  bb8.setLookDirection(upDirection);
                    */

              });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //always use in portrait mode
        Configuration newConfig = getResources().getConfiguration();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        arFragment.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        arFragment.onDestroy();
    }

    private String generateFilename() {
        String date =
                new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/" + date + "_screenshot.jpg";
    }

    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {

        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    private void takePhoto() {
        final String filename = generateFilename();
        ArSceneView view = arFragment.getArSceneView();

        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(HelloSceneformActivity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Photo saved", Snackbar.LENGTH_LONG);
                snackbar.setAction("Open in Photos", v -> {
                    File photoFile = new File(filename);

                    Uri photoURI = FileProvider.getUriForFile(HelloSceneformActivity.this,
                            HelloSceneformActivity.this.getPackageName() + ".FileProvider",
                            photoFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                    intent.setDataAndType(photoURI, "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);

                });
                snackbar.show();
            } else {
                Toast toast = Toast.makeText(HelloSceneformActivity.this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

    /*
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ImageButton colorDetectionBu = findViewById(R.id.colorDetection);
            ImageButton cameraButton = findViewById(R.id.cameraClick);

            cameraButton.setVisibility(View.GONE);
            colorDetectionBu.setVisibility(View.VISIBLE);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            ImageButton colorDetectionBu = findViewById(R.id.colorDetection);
            ImageButton cameraButton = (ImageButton) findViewById(R.id.cameraClick);

            colorDetectionBu.setVisibility(View.GONE);
            cameraButton.setVisibility(View.VISIBLE);
        }
    }
    */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == COLOR) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                detectedColor = data.getStringExtra("COLOR");
                Toast toast = Toast.makeText(HelloSceneformActivity.this,
                        "The detected color was " + detectedColor, Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }
}
