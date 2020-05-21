package com.example.arcore_measure;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.shapes.Shape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
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
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.PlaneDiscoveryController;
import com.google.ar.sceneform.ux.TransformableNode;

import java.text.DecimalFormat;
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

    private float distance = 0;
    private float roomPerimeter = 0;
    private float roomHeight = 0;

    public static Dialog dialog;
    public static Dialog dialogSave;
    private MeasurementViewModel measurementViewModel;
    float surfaceArea;



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
        arFragment.getView().performClick();
        arFragment.setOnTapArPlaneListener(this::addPoint);
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


    //metoda dodająca punkt do powierzchni
    private void addPoint(HitResult hitResult, Plane plane, MotionEvent motionEvent) {

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

        showDistance();
    }

    private void initModel() {
        // wygląd punktu na powierzchni
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
                clearAnchor();
            }
            if(distance != 0 && roomPerimeter != 0 && roomHeight == 0) {
                Toast.makeText(this, "Dodano wysokość ściany", Toast.LENGTH_LONG).show();
                roomHeight = distance;
                distance = 0;
                tvDistance.setText("---");
                clearAnchor();
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
            surfaceArea = (float) (Math.round( roomHeight * roomPerimeter * 100)/ 100.0);
           // Toast.makeText(this, "Powierzchnia ścian = "+ surfaceArea + " m", Toast.LENGTH_LONG).show();
            showAlertDialog(MainActivity.this);
        }
        else
            Toast.makeText(this, "Nie wykonano wszystkich pomiarów", Toast.LENGTH_SHORT).show();
    }

    // metoda, która imituje kliknięcie środka ekranu
    public void addFromCenter(View view) {

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
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogStyle);

        builder.setMessage("This surface is " + surfaceArea +" m\u00B2");
        builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showSaveDialog(MainActivity.this);
            }
        });
        builder.show();

    }

    /* Okno zapisywania powierzchni*/
    public void showSaveDialog (Activity activity){
        dialogSave = new Dialog(activity);
        dialogSave.setCancelable(false);
        dialogSave.setContentView(R.layout.dialog_save);


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



