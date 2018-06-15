/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.hellosceneform;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Plane.Type;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity {
  private static final String TAG = "hellosceneform";

  private ArFragment arFragment;
  //private ModelRenderable andyRenderable;
  private ModelRenderable bb8Renderable;

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_ux);
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    Button colorDetectionBu = findViewById(R.id.colorDetection);

    //hide the button if the orientation is portrait
    Configuration newConfig = getResources().getConfiguration();
    if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
    {
        colorDetectionBu.setVisibility(View.GONE);
    }
/*
    // When you build a Renderable, Sceneform loads its resources in the background while returning
    // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
    ModelRenderable.builder()
        .setSource(this, R.raw.andy)
        .build()
        .thenAccept(renderable -> andyRenderable = renderable)
        .exceptionally(
            throwable -> {
              Toast toast =
                  Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
              toast.setGravity(Gravity.CENTER, 0, 0);
              toast.show();
              return null;
            });

    arFragment.setOnTapArPlaneListener(
        (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
          if (andyRenderable == null) {
            return;
          }

          if (plane.getType() != Type.HORIZONTAL_UPWARD_FACING) {
            return;
          }

          // Create the Anchor.
          Anchor anchor = hitResult.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());

          // Create the transformable andy and add it to the anchor.
          TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
          andy.setParent(anchorNode);
          andy.setRenderable(andyRenderable);
          andy.select();
        });
        */

      //trying to load bb8 renderable
      ModelRenderable.builder()
              .setSource(this, R.raw.andy)
              .build()
              .thenAccept(renderable -> bb8Renderable = renderable)
              .exceptionally(
                      throwable -> {
                          Toast toast =
                                  Toast.makeText(this, "Unable to load bb8 renderable", Toast.LENGTH_LONG);
                          toast.setGravity(Gravity.CENTER, 0, 0);
                          toast.show();
                          return null;
                      });

      arFragment.setOnTapArPlaneListener(
              (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                  if (bb8Renderable == null) {
                      return;
                  }

                  if (plane.getType() != Type.HORIZONTAL_UPWARD_FACING) {
                      return;
                  }

                  // Create the Anchor.
                  Anchor anchor = hitResult.createAnchor();
                  AnchorNode anchorNode = new AnchorNode(anchor);
                  anchorNode.setAnchor(anchor);
                  anchorNode.setParent(arFragment.getArSceneView().getScene());

                  // Create the transformable node and add it to the anchor.
                  TransformableNode tn = new TransformableNode(arFragment.getTransformationSystem());
                  tn.setParent(anchorNode);

                  //We need to create an additional Node to tn to rotate the 3d model without rotating the transformation system
                  Node bb8 = new Node();
                  bb8.setParent(tn);
                  bb8.setRenderable(bb8Renderable);

                  //rotate the model since it's facing downwards by default
                  float[] upAxis = plane.getCenterPose().getYAxis();
                  //Log.e(TAG, "upAxis" + upAxis[0] + upAxis[1] + upAxis[2]);
                  Vector3 upDirection = new Vector3(-upAxis[0], -upAxis[1], -upAxis[2]);
                  bb8.setLookDirection(upDirection);

                  tn.select();

              });
  }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Button colorDetectionBu = findViewById(R.id.colorDetection);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            colorDetectionBu.setVisibility(View.VISIBLE);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            colorDetectionBu.setVisibility(View.GONE);
        }
    }
}
