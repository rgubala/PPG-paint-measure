package com.example.arcore_measure;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
    private ArrayList<Anchor> currentAnchor = new ArrayList<>();
    private AnchorNode anchorNodeTemp;
    private TextView tvDistance;
    ModelRenderable pointRender, aimRender;
    private ViewRenderable textBox;
    private float distance = 0;
    private float roomPerimeter = 0;
    private float roomHeight = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        }
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        tvDistance = findViewById(R.id.tvDistance);
        initModel();

        arFragment.setOnTapArPlaneListener(this::refreshAim);
        Toast.makeText(this, "Zmierz obwód pokoju", Toast.LENGTH_LONG).show();

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

    // renderowanie modeli punktu i celownika
    private void initModel() {

        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(WHITE))
                .thenAccept(material -> {
                    pointRender = ShapeFactory.makeCylinder(0.018f, 0.0001f, zero(), material);
                    pointRender.setShadowCaster(false);
                    pointRender.setShadowReceiver(false);
                });

        Texture.builder()
                .setSource(getApplicationContext(), R.drawable.aim)
                .build().
                thenAccept(texture -> {
                    MaterialFactory.makeTransparentWithTexture(getApplicationContext(), texture)
                            .thenAccept(material -> {
                                aimRender = ShapeFactory.makeCylinder(0.08f, 0f, zero(), material);
                                aimRender.setShadowCaster(false);
                                aimRender.setShadowReceiver(false);
                            });
                });
    }

    // renderowanie etykiety z odelgłością
    void initTextBox(float meters) {

        ViewRenderable.builder()
                .setView(this, R.layout.distance)
                .build()
                .thenAccept(renderable -> {
                    textBox = renderable;
                    textBox.setShadowCaster(false);
                    textBox.setShadowReceiver(false);
                    textBox.setVerticalAlignment(ViewRenderable.VerticalAlignment.BOTTOM);
                    TextView distanceInMeters = (TextView) textBox.getView();
                    String metersString = String.format(Locale.ENGLISH, "%.2f", meters) + " m";
                    distanceInMeters.setText(metersString);

                });
    }

    // metoda zwraca linię o zadanej długości (umieszczana między punktami)
    ModelRenderable initLine(float length) throws InterruptedException {
        final ModelRenderable[] lineRender = new ModelRenderable[1];
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(WHITE))
                .thenAccept(material -> {
                            lineRender[0] = ShapeFactory.makeCube(new Vector3(.015f, 0, length), zero(), material);
                            lineRender[0].setShadowCaster(false);
                            lineRender[0].setShadowReceiver(false);
                        });
        TimeUnit.MILLISECONDS.sleep(20);
        return lineRender[0];
    }

    // celownik jeżdżący po powierzchni
    private void refreshAim(HitResult hitResult, Plane plane, MotionEvent motionEvent) {

        if (aimRender == null)
            return;

        if(motionEvent.getMetaState() == 0) {
            if (anchorNodeTemp != null)
                anchorNodeTemp.getAnchor().detach();

            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
            TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
            transformableNode.setRenderable(aimRender);
            transformableNode.setParent(anchorNode);
            arFragment.getArSceneView().getScene().addOnUpdateListener(this);
            arFragment.getArSceneView().getScene().addChild(anchorNode);

            anchorNodeTemp = anchorNode;
        }
    }

    // dodawnie punktów do powierzchni na podstawie pozycji celownika
    // dodawanie linii między punktami
    public void addFromAim(View view) throws InterruptedException {

        if(anchorNodeTemp != null) {
            Vector3 worldPosition = anchorNodeTemp.getWorldPosition();
            Quaternion worldRotation = anchorNodeTemp.getWorldRotation();

            worldPosition.x += 0.0000001f;
            AnchorNode confirmedAnchorNode = new AnchorNode();
            confirmedAnchorNode.setWorldPosition(worldPosition);
            confirmedAnchorNode.setWorldRotation(worldRotation);
            Anchor anchor = confirmedAnchorNode.getAnchor();
            currentAnchor.add(anchor);
            currentAnchorNode.add(confirmedAnchorNode);

            confirmedAnchorNode.setParent(arFragment.getArSceneView().getScene());
            TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
            transformableNode.setRenderable(pointRender);
            transformableNode.setParent(confirmedAnchorNode);
            arFragment.getArSceneView().getScene().addOnUpdateListener(this);
            arFragment.getArSceneView().getScene().addChild(confirmedAnchorNode);

            if(currentAnchorNode.size() >= 2) {

                Vector3 node1Pos = currentAnchorNode.get(currentAnchorNode.size() - 2).getWorldPosition();
                Vector3 node2Pos = currentAnchorNode.get(currentAnchorNode.size() - 1).getWorldPosition();
                Vector3 difference = Vector3.subtract(node1Pos, node2Pos);

                final Vector3 directionFromTopToBottom = difference.normalized();
                final Quaternion rotationFromAToB =
                        Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());

                AnchorNode lineBetween = new AnchorNode();
                TransformableNode lineNode = new TransformableNode(arFragment.getTransformationSystem());
                TransformableNode distanceNode = new TransformableNode(arFragment.getTransformationSystem());
                lineNode.setParent(lineBetween);
                lineNode.setLocalPosition(Vector3.add(node1Pos, node2Pos).scaled(.5f));
                lineNode.setLocalRotation(rotationFromAToB);
                lineNode.setRenderable(initLine(difference.length()));

                distanceNode.setParent(lineBetween);
                distanceNode.setLocalPosition(Vector3.add(node1Pos, node2Pos).scaled(.5f));
                distanceNode.setLocalRotation(rotationFromAToB);

                initTextBox(difference.length());
                distanceNode.setRenderable(textBox);
                arFragment.getArSceneView().getScene().addChild(lineBetween);

            }
        }
    }

    // imitowanie kliknięć na środek ekranu (do celownika)
    private void touchScreenCenterConstantly() {

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 10;
        float x = (float)(this.getResources().getDisplayMetrics().widthPixels) / 2;
        float y = (float)(this.getResources().getDisplayMetrics().heightPixels) / 2;
        MotionEvent motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                0
        );
        arFragment.getArSceneView().dispatchTouchEvent(motionEvent);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        touchScreenCenterConstantly();
    }

    private void clearAll() {
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

    // suma odległości pomiędzy wszystkimi zaznaczonymi punktami
    private void showDistance() {
        if (currentAnchorNode.size() >= 2) {
            int i = currentAnchorNode.size();
            Vector3 node1 = currentAnchorNode.get(i-1).getWorldPosition();
            Vector3 node0 = currentAnchorNode.get(i-2).getWorldPosition();

            distance += Vector3.subtract(node0, node1).length();
            tvDistance.setText("Distance : " + distance + " metres");
        }
    }

    //wywoływane z przycisku "dodaj wymiar"
    public void addDimension(View view) {

        if(distance != 0 && (roomPerimeter == 0 || roomHeight == 0)) {
            if(distance != 0 && roomPerimeter == 0) {
                Toast.makeText(this, "Dodano szerokość ściany.\nTeraz zmierz wysokość ściany", Toast.LENGTH_LONG).show();
                roomPerimeter = distance;
                distance = 0;
                tvDistance.setText("---");
                clearAll();
            }
            if(distance != 0 && roomPerimeter != 0 && roomHeight == 0) {
                Toast.makeText(this, "Dodano wysokość ściany", Toast.LENGTH_LONG).show();
                roomHeight = distance;
                distance = 0;
                tvDistance.setText("---");
                clearAll();
            }
            return;
        }
        if (distance == 0 && roomPerimeter == 0 && roomHeight == 0) {
            Toast.makeText(this, "Jeszcze nie wykonano żadnych pomiarów", Toast.LENGTH_SHORT).show();
            return;
        }
        if (distance == 0 && roomPerimeter != 0 && roomHeight != 0) {
            Toast.makeText(this, "Wykonano już wszystkie potrzebne pomiary", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    //wywoływane z przycisku "powierzchnia ściany"
    public void calculateSurfaceArea (View view) {
        if(roomPerimeter != 0 && roomHeight != 0) {
            float surfaceArea = roomHeight * roomPerimeter;
            Toast.makeText(this, "Powierzchnia ścian = "+ surfaceArea + " m", Toast.LENGTH_LONG).show();
        }
        else
            Toast.makeText(this, "Nie wykonano wszystkich pomiarów", Toast.LENGTH_SHORT).show();
    }

}

