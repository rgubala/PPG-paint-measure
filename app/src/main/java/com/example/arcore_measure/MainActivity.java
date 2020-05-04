package com.example.arcore_measure;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.shapes.Shape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.PlaneDiscoveryController;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import static android.graphics.Color.*;
import static com.google.ar.sceneform.math.Vector3.zero;


public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener {
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String TAG = MainActivity.class.getSimpleName();

    // Set to true ensures requestInstall() triggers installation if necessary.
    private boolean mUserRequestedInstall = true;
    private Session mSession;
    private ArFragment arFragment;
    private ArrayList<AnchorNode> currentAnchorNode = new ArrayList<>();
    private TextView tvDistance;
    ModelRenderable cubeRenderable;
    private ArrayList<Anchor> currentAnchor = new ArrayList<>();


    private SensorManager sensorManager;
    private Sensor sensor;
    private float xAngle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        }

        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        tvDistance = findViewById(R.id.tvDistance);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        initModel();
        // to ma obliczać na bierząco odległość kamery od ziemi
        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {

        });
        // aim in the center
        MaterialFactory
                .makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(YELLOW))
                .thenAccept(material -> {
                    ModelRenderable renderable = ShapeFactory.makeCube(new Vector3(.05f,0,.05f),zero(),material);
                    Node node = new Node();
                    node.setParent(arFragment.getArSceneView().getScene());
                    node.setRenderable(renderable);

                    arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                        Camera camera = arFragment.getArSceneView().getScene().getCamera();
                        Ray ray = camera.screenPointToRay(1080/2f,1920/2f);
                        Vector3 newPosition = ray.getPoint(1f);
                        node.setLocalPosition(newPosition);
                    });
                });

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (cubeRenderable == null){
                return;
            }
            //creating the anchor
            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            //clearAnchor();

            currentAnchor.add(anchor);
            currentAnchorNode.add(anchorNode);

            //creating the node
            TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
            node.setRenderable(cubeRenderable);
            node.setParent(anchorNode);
            arFragment.getArSceneView().getScene().addOnUpdateListener(this);
            arFragment.getArSceneView().getScene().addChild(anchorNode);
            node.select();

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

    private void initModel() {
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(RED))
                .thenAccept(material -> {
                    Vector3 vector3 = new Vector3(.02f, .02f, .02f);
                    cubeRenderable = ShapeFactory.makeCube(vector3, zero(), material);
                    cubeRenderable.setShadowCaster(false);
                    cubeRenderable.setShadowReceiver(false);
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

    public void start() {
        // enable our sensor when the activity is resumed, ask for
        // 10 ms updates.
        sensorManager.registerListener((SensorEventListener) this, sensor, 10000);
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            xAngle = event.values[0];
        }
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

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        Log.d("API123", "onUpdateFrame...current anchor node " + (currentAnchorNode == null));

        if (currentAnchorNode.size() == 2) {
            Vector3 node1 = currentAnchorNode.get(1).getWorldPosition();
            Vector3 node0 = currentAnchorNode.get(0).getWorldPosition();

            float distance = Vector3.subtract(node0, node1).length();
            //tvDistance.setText("Distance : " + distance + " metres");

        }
    }
}

