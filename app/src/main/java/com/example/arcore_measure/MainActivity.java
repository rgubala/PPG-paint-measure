package com.example.arcore_measure;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
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
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
    private ArrayList<AnchorNode> labelArray = new ArrayList<>();
    private TextView tvDistance;
    private AnchorNode anchorNodeTemp;
    private AnchorNode anchorNodeHeight = null;
    private ArrayList<Anchor> currentAnchor = new ArrayList<>();

    private float distance = 0;
    private float roomPerimeter = 0;
    private float roomHeight = 0;
    ModelRenderable pointRender, aimRender, widthLineRender, heightLineRender;

    public static Dialog dialog;
    public static Dialog dialogSave;
    private MeasurementViewModel measurementViewModel;
    public static Dialog dialogSurfValue;
    float surfaceArea;
    private Button btnSave;
    private Button btnRoom;
    private Button btnUp;
    private Vector3 difference;
    private Vector3 camPos;
    private Vector3 camFor;
    private Vector3 newPos;
    ModelRenderable renderableA;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        }
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        tvDistance = findViewById(R.id.tvDistance);

        btnSave = (Button) findViewById(R.id.buttonAdd);
        btnSave.setEnabled(false);

        btnRoom = (Button) findViewById(R.id.buttonRoom);
        btnRoom.setEnabled(false);

        initModel();
        heightMeasurement();
        arFragment.setOnTapArPlaneListener(this::refreshAim);

        Toast.makeText(this, "Zmierz obwód pokoju", Toast.LENGTH_LONG).show();

        measurementViewModel = new ViewModelProvider(this).get(MeasurementViewModel.class);

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
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(WHITE))
                .thenAccept(material -> {
                    pointRender = ShapeFactory.makeCylinder(0.018f, 0.0001f, zero(), material);
                    pointRender.setShadowCaster(false);
                    pointRender.setShadowReceiver(false);
                });

        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(WHITE))
                .thenAccept(material -> {
                    heightLineRender = ShapeFactory.makeCylinder(0.01f, 0.01f, zero(), material);
                    heightLineRender.setShadowCaster(false);
                    heightLineRender.setShadowReceiver(false);
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

        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(WHITE))
                .thenAccept(material -> {
                    widthLineRender = ShapeFactory.makeCube(new Vector3(.015f, 0, 1f), zero(), material);
                    widthLineRender.setShadowCaster(false);
                    widthLineRender.setShadowReceiver(false);

                });

    }

    // renderowanie etykiety z odelgłością
    void initTextBox(float meters, TransformableNode tN) {

        ViewRenderable.builder()
                .setView(this, R.layout.distance)
                .build()
                .thenAccept(renderable -> {
                    renderable.setShadowCaster(false);
                    renderable.setShadowReceiver(false);
                    renderable.setVerticalAlignment(ViewRenderable.VerticalAlignment.BOTTOM);
                    TextView distanceInMeters = (TextView) renderable.getView();
                    String metersString;
                    if(meters < 1f)
                        metersString = String.format(Locale.ENGLISH, "%.0f", meters*100) + " cm";
                    else
                        metersString = String.format(Locale.ENGLISH, "%.2f", meters) + " m";
                    distanceInMeters.setText(metersString);
                    tN.setRenderable(renderable);
                });

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
        labelsRotation();
        touchScreenCenterConstantly();
    }

    public void clearAnchors(View view) {
        List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
        for (Node node : children) {
            if (node instanceof AnchorNode) {
                if (((AnchorNode) node).getAnchor() != null) {
                    ((AnchorNode) node).getAnchor().detach();
                    node.setParent(null);
                    node.setRenderable(null);
                }
            }
            if (!(node instanceof Camera) && !(node instanceof Sun)) {
                node.setParent(null);
            }
        }
        currentAnchorNode.clear();
        currentAnchor.clear();
        labelArray.clear();
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

    // obracanie etykiet zgodnie z ruchami kamery
    private void labelsRotation() {
        Vector3 cameraPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();

        if(labelArray != null) {
            for(AnchorNode labelNode : labelArray) {
                Vector3 labelPosition = labelNode.getWorldPosition();
                Vector3 direction = Vector3.subtract(cameraPosition, labelPosition);
                Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
                labelNode.setWorldRotation(lookRotation);
            }
        }
    }

    //wywoływane z przycisku "dodaj wymiar"
    public void addDimension(View view) {
        if(difference.length() != 0 && (roomPerimeter == 0 || roomHeight == 0)) {
            if(difference.length() != 0 && roomPerimeter == 0) {
                Toast.makeText(this, "Dodano szerokość ściany.\nTeraz zmierz wysokość ściany", Toast.LENGTH_LONG).show();
                roomPerimeter = difference.length();
                distance = 0;
                difference = Vector3.zero();

                btnSave.setEnabled(false);
            }
            if(difference.length() != 0 && roomPerimeter != 0 && roomHeight == 0) {
                Toast.makeText(this, "Dodano wysokość ściany", Toast.LENGTH_LONG).show();
                //roomHeight = difference.length();
                distance = 0;
                btnSave.setEnabled(false);
                difference = Vector3.zero();
            }
            return;
        }
        if (difference.length() == 0 && roomPerimeter == 0 && roomHeight == 0) {
            Toast.makeText(this, "Jeszcze nie wykonano żadnych pomiarów", Toast.LENGTH_SHORT).show();
            return;
        }
        if (roomPerimeter != 0 && roomHeight != 0) {
            Toast.makeText(this, "Wykonano już wszystkie potrzebne pomiary", Toast.LENGTH_SHORT).show();
            calculateSurfaceArea(view);
            return;
        }
    }

    //wywoływane z przycisku "powierzchnia ściany"
    public void calculateSurfaceArea (View view) {
        if(roomPerimeter != 0 && roomHeight != 0) {
            surfaceArea = (float) (Math.round( roomHeight * roomPerimeter * 100)/ 100.0);
            showAlertDialog(MainActivity.this);
            btnRoom.setEnabled(false);
        }
        else
            Toast.makeText(this, "Nie wykonano wszystkich pomiarów", Toast.LENGTH_SHORT).show();
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
    // dodawanie etykiet
    public void addFromAim(View view) throws InterruptedException {

        if(anchorNodeTemp != null) {
            Vector3 worldPosition = anchorNodeTemp.getWorldPosition();
            Quaternion worldRotation = anchorNodeTemp.getWorldRotation();

            // dodawanie punktu
            worldPosition.x += 0.0000001f;
            AnchorNode confirmedAnchorNode = new AnchorNode();
            confirmedAnchorNode.setWorldPosition(worldPosition);
            confirmedAnchorNode.setWorldRotation(worldRotation);
            Anchor anchor = confirmedAnchorNode.getAnchor();

            confirmedAnchorNode.setParent(arFragment.getArSceneView().getScene());
            TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
            transformableNode.setRenderable(pointRender);
            transformableNode.setParent(confirmedAnchorNode);
            arFragment.getArSceneView().getScene().addOnUpdateListener(this);
            arFragment.getArSceneView().getScene().addChild(confirmedAnchorNode);

            currentAnchor.add(anchor);
            currentAnchorNode.add(confirmedAnchorNode);

            if(currentAnchorNode.size() >= 2) {

                Vector3 node1Pos = currentAnchorNode.get(currentAnchorNode.size() - 2).getWorldPosition();
                Vector3 node2Pos = currentAnchorNode.get(currentAnchorNode.size() - 1).getWorldPosition();
                difference = Vector3.subtract(node1Pos, node2Pos);

                final Quaternion rotationFromAToB =
                        Quaternion.lookRotation(difference.normalized(), Vector3.up());

                // ustawianie linii między punktami
                AnchorNode lineBetween = new AnchorNode();
                lineBetween.setParent(arFragment.getArSceneView().getScene());
                lineBetween.setWorldPosition(Vector3.add(node1Pos, node2Pos).scaled(.5f));
                lineBetween.setWorldRotation(rotationFromAToB);
                lineBetween.setLocalScale(new Vector3(1f, 1f, difference.length()));
                TransformableNode lineNode = new TransformableNode(arFragment.getTransformationSystem());
                lineNode.setParent(lineBetween);
                lineNode.setRenderable(widthLineRender);

                // ustawianie etykiet z odległościami
                AnchorNode lengthLabel = new AnchorNode();
                lengthLabel.setParent(arFragment.getArSceneView().getScene());
                lengthLabel.setWorldPosition(Vector3.add(node1Pos, node2Pos).scaled(.5f));
                TransformableNode distanceNode = new TransformableNode(arFragment.getTransformationSystem());
                distanceNode.setParent(lengthLabel);
                initTextBox(difference.length(), distanceNode);
                labelArray.add(lengthLabel);

                btnSave.setEnabled(true);
            }
        }
    }

    // pomiar wysokości
    @SuppressLint("ClickableViewAccessibility")
    private void heightMeasurement() {
        btnUp = findViewById(R.id.buttonUp);
        btnUp.setOnTouchListener(new View.OnTouchListener() {
            private Handler mHandler;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(anchorNodeHeight == null)
                    anchorNodeHeight = anchorNodeTemp;
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        roomHeight = 0;
                        mHandler.postDelayed(mAction, 20);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        anchorNodeHeight = null;
                        break;
                }
                return false;
            }

            Runnable mAction = new Runnable() {
                @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
                @Override public void run() {
                    if(anchorNodeHeight!=null) {

                        Vector3 worldPosition = anchorNodeHeight.getWorldPosition();
                        Quaternion worldRotation = anchorNodeHeight.getWorldRotation();
                        if(roomHeight == 0) {
                            AnchorNode stand = new AnchorNode();
                            worldPosition.x += 0.0000001f;
                            stand.setWorldPosition(worldPosition);
                            stand.setWorldRotation(worldRotation);
                            stand.setParent(arFragment.getArSceneView().getScene());
                            TransformableNode tN = new TransformableNode(arFragment.getTransformationSystem());
                            tN.setRenderable(aimRender);
                            tN.setParent(stand);

                            arFragment.getArSceneView().getScene().addOnUpdateListener(MainActivity.this);
                            arFragment.getArSceneView().getScene().addChild(stand);
                        }

                        worldPosition.y += 0.01f;
                        AnchorNode anchorNode = new AnchorNode();
                        anchorNode.setWorldPosition(worldPosition);
                        anchorNode.setWorldRotation(worldRotation);

                        anchorNode.setParent(arFragment.getArSceneView().getScene());
                        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                        transformableNode.setRenderable(heightLineRender);
                        transformableNode.setParent(anchorNode);

                        roomHeight += 0.01f;
                        if(roomHeight < 1f)
                            tvDistance.setText(String.format(Locale.ENGLISH, "%.0f", roomHeight*100) + " cm");
                        else
                            tvDistance.setText(String.format(Locale.ENGLISH, "%.2f", roomHeight) + " m");
                        anchorNodeHeight = anchorNode;
                        arFragment.getArSceneView().getScene().addOnUpdateListener(MainActivity.this);
                        arFragment.getArSceneView().getScene().addChild(anchorNode);
                        mHandler.postDelayed(this, 20);

                    }
                }
            };
        });
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

    /* Okno pokazujące obliczoną powierzchnię*/
    public void  showAlertDialog (Activity activity){

        dialogSurfValue = new Dialog(activity);
        dialogSurfValue.setCancelable(false);
        dialogSurfValue.setContentView(R.layout.dialog_surface);
        dialogSurfValue.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        TextView textViewSurfaceValue = (TextView) dialogSurfValue.findViewById(R.id.textViewSurfaceValue);
        textViewSurfaceValue.setText("This surface is " + surfaceArea +" m\u00B2");
        Button btnOk = (Button) dialogSurfValue.findViewById(R.id.btnSurfOK);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogSurfValue.dismiss();
            }
        });

        Button btnSave = (Button) dialogSurfValue.findViewById(R.id.btnSurfSave);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogSurfValue.dismiss();
                showSaveDialog(MainActivity.this);


            }
        });

        dialogSurfValue.show();

    }

    /* Okno zapisywania powierzchni*/
    public void showSaveDialog (Activity activity){

        dialogSave = new Dialog(activity);
        dialogSave.setCancelable(false);
        dialogSave.setContentView(R.layout.dialog_save);
        dialogSave.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnExit = (Button) dialogSave.findViewById(R.id.btndialogExit);
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogSave.dismiss();
            }
        });

        Button btnSave = (Button) dialogSave.findViewById(R.id.btndialogSave);
        EditText nameSurf = (EditText) dialogSave.findViewById(R.id.editTextNameSufr);
        TextView textViewSurfValue = (TextView) dialogSave.findViewById(R.id.surfValueTextView);
        textViewSurfValue.setText(surfaceArea + "m\u00B2");

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Measurements measur = new Measurements(nameSurf.getText().toString(), Float.valueOf(surfaceArea));
                measurementViewModel.insert(measur);
                showDialog(MainActivity.this);

                dialogSave.dismiss();

            }
        });

        dialogSave.show();

    }

    /* Okno z zapisanymi powierzchniami*/
    public void showDialog(Activity activity){

        dialog = new Dialog(activity);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_recycler);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btndialog = (Button) dialog.findViewById(R.id.btndialogOk);
        btndialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
            }
        });
        RecyclerView recyclerView = dialog.findViewById(R.id.recycler);
        final MeasurementListAdapter adapter = new MeasurementListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        measurementViewModel.getAllMeasurements().observe(this, new Observer<List<Measurements>>() {



            @Override
            public void onChanged(List<Measurements> measurements) {
                adapter.setMeasurements(measurements);
            }
        });

        recyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        dialog.show();

    }
}
