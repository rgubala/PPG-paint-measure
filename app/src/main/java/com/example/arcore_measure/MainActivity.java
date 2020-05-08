package com.example.arcore_measure;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.shapes.Shape;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.graphics.Color.*;


public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener {
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String TAG = MainActivity.class.getSimpleName();

    // Set to true ensures requestInstall() triggers installation if necessary.
    private boolean mUserRequestedInstall = true;
    private Session mSession;
    private Config mConfig;
    private ArFragment arFragment;
    private ArrayList<AnchorNode> currentAnchorNode = new ArrayList<>();
    private ArrayList<AnchorNode> currentAnchorNodeMid = new ArrayList<>();
    private ArrayList<Node> currentNode = new ArrayList<>();
    private AnchorNode anchorNodeMid;
    private Anchor anchorMid;
    private TextView txt_name;
    ModelRenderable cubeRenderable;
    ModelRenderable renderableA;
    ViewRenderable distance;
    private ArrayList<Anchor> currentAnchor = new ArrayList<>();
    private Vector3 camPos;
    private float distanceMeters;
    private float width;
    private float height;
    private Node nameView;
    private Pose pose;
    private Vector3 node1;
    private Vector3 node0;
    private int i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        }

        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        initModel();

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (cubeRenderable == null){
                return;
            }
            //creating the anchor
            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            currentAnchor.add(anchor);
            currentAnchorNode.add(anchorNode);


            //creating the node
            TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
            node.setRenderable(cubeRenderable);
            node.setParent(anchorNode);
            arFragment.getArSceneView().getScene().addOnUpdateListener(this);
            arFragment.getArSceneView().getScene().addChild(anchorNode);
            node.select();
            if (currentAnchorNode.size() >= 2)
            {
                i = currentAnchorNode.size()-1;
                node1 = currentAnchorNode.get(i).getWorldPosition();
                node0 = currentAnchorNode.get(i-1).getWorldPosition();

                float dx = node0.x - node1.x;
                float dy = node0.y - node1.y;
                float dz = node0.z - node1.z;

                distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                DecimalFormat dM = new DecimalFormat("#.##");
                distanceMeters = Float.valueOf(dM.format(distanceMeters));

                //create mid anchor to show distance on screen
                float[] midPos = {
                        (node0.x + node1.x) / 2,
                        (node0.y + node1.y) / 2,
                        (node0.z + node1.z) / 2};
                float[] quaternion = {0.0f, 0.0f, 0.0f, 0.0f};

                pose = new Pose(midPos, quaternion);
                anchorMid = arFragment.getArSceneView().getSession().createAnchor(pose);
                anchorNodeMid = new AnchorNode(anchorMid);
                anchorNodeMid.setParent(arFragment.getArSceneView().getScene());
                currentAnchorNodeMid.add(anchorNodeMid);
                addName(currentAnchorNodeMid.get(i-1), "" + distanceMeters);
            }
        });
    }

    public boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        String openGlVersionString = ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE)))
                .getDeviceConfigurationInfo()
                .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL 3.0 or later");
            Toast.makeText(activity, "Sceneform requires OpenGL 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
    private void initTextView(String name) {
        ViewRenderable.builder()
                .setView(this, R.layout.distance)
                .build()
                .thenAccept(renderable -> {
                    distance = renderable;
                    distance.setShadowCaster(false);
                    distance.setShadowReceiver(false);
                    txt_name = (TextView) distance.getView();
                    txt_name.setText(name);
                });
    }
    private void initModel() {
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(RED))
                .thenAccept(material -> {
                    Vector3 vector3 = new Vector3(0.05f, 0.0f, 0.01f);
                    cubeRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
                    cubeRenderable.setShadowCaster(false);
                    cubeRenderable.setShadowReceiver(false);
                });
        //aim
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(BLUE))
                .thenAccept(material -> {
                    Vector3 vector3 = new Vector3(0.05f, 0.0f, 0.01f);
                    renderableA = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
                    renderableA.setShadowCaster(false);
                    renderableA.setShadowReceiver(false);

                    Node nodeCam = new Node();
                    nodeCam.setParent(arFragment.getArSceneView().getScene());
                    nodeCam.setRenderable(renderableA);

                    arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                        Camera camera = arFragment.getArSceneView().getScene().getCamera();
                        Ray ray = camera.screenPointToRay(width/2f,height/2f);
                        Vector3 newPos = ray.getPoint(1f);
                        camPos = camera.getWorldPosition();
                        nodeCam.setLocalPosition(newPos);
                    });
                });
    }

    private void clearAnchor() {
        currentAnchor.clear();
        for(AnchorNode anchorNode : currentAnchorNode) {
            if (currentAnchorNode != null) {
                arFragment.getArSceneView().getScene().removeChild(anchorNode);
                anchorNode.getAnchor().detach();
                anchorNode.setParent(null);
            }
        }
        currentAnchorNode.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permission to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
            return;
        }

        try {
            if (mSession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    case INSTALLED:
                        // Success, create the AR session.
                        mSession = new Session(this);
                        mConfig = new Config(mSession);
                        mConfig.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
                        mConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                        break;
                    case INSTALL_REQUESTED:
                        // Ensures next invocation of requestInstall() will either return
                        // INSTALLED or throw an exception.
                        mUserRequestedInstall = false;
                        return;
                }
            }
        } catch (UnavailableUserDeclinedInstallationException | UnavailableDeviceNotCompatibleException e) {
            // Display an appropriate message to the user and return gracefully.
            Toast.makeText(this, "TODO: handle exception " + e, Toast.LENGTH_LONG)
                    .show();
            return;
        } catch (UnavailableArcoreNotInstalledException e) {
            e.printStackTrace();
        } catch (UnavailableSdkTooOldException e) {
            e.printStackTrace();
        } catch (UnavailableApkTooOldException e) {
            e.printStackTrace();
        }
        return;  // mSession is still null.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    public void addName (AnchorNode anchorNode, String name) {
        initTextView(name);

        nameView = new Node();
        nameView.setParent(anchorNode);
        nameView.setRenderable(distance);
        nameView.setLocalPosition(camPos);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        Log.d("API123", "onUpdateFrame...current anchor node " + (currentAnchorNode == null));
    }
}

