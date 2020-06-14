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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

    private float roomPerimeter = 0;
    private float roomHeight = 0;
    private float roomHeightConfirm = 0;
    ModelRenderable pointRender, aimRender, widthLineRender, heightLineRender;

    public static Dialog dialog;
    public static Dialog dialogSave;
    private MeasurementViewModel measurementViewModel;
    public static Dialog dialogSurfValue;
    private float surfaceArea;
    private float totalLength;
    private Button btnSave;
    private Button btnRoom;
    private Button btnUp;
    private Button btnAdd;
    private Button btnCalc;
    private Vector3 difference;

    public static Dialog dialogDoors;
    public static Dialog dialogCalculator;
    Button buttonSub, buttonAdd,buttonCalculator;
    TextView calcTextViewLayers, respond;
    Spinner calcSpinner;
    EditText editTextPaintEff, editTextGivenSurface;
    private int mCounter=1;
    double a,paintEfficiency, surfaceToPaint;
    double number1;
    String number2;

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

        btnUp = findViewById(R.id.buttonUp);

        btnAdd = findViewById(R.id.button);

        btnCalc = findViewById(R.id.button3);
        btnCalc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCalculatorDialog(MainActivity.this);
            }
        });

        initModel();
        heightMeasurement();
        Toast.makeText(this, "Zmierz wysokosc pokoju", Toast.LENGTH_LONG).show();
        arFragment.setOnTapArPlaneListener(this::refreshAim);

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
        totalLength = 0;
        roomHeight = 0;
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
        if (roomPerimeter == 0 || roomHeightConfirm == 0) {
            if (roomHeight != 0 && roomHeightConfirm == 0) {
                Toast.makeText(this, "Dodano wysokość ściany.\nTeraz zmierz szerokość ściany", Toast.LENGTH_LONG).show();
                roomHeightConfirm = roomHeight;
                btnSave.setEnabled(false);
            }
            if (roomHeight == 0 && roomHeightConfirm != 0 && roomPerimeter == 0) {
                Toast.makeText(this, "Dodano szerokość ściany", Toast.LENGTH_LONG).show();
                roomPerimeter = totalLength;
                difference = Vector3.zero();
                btnSave.setEnabled(false);
                btnRoom.setEnabled(true);
            }
            clearAnchors(view);
            return;
        }
        if (roomPerimeter != 0 && roomHeightConfirm != 0) {
            showDialog(MainActivity.this);
            return;
        }
    }

    //wywoływane z przycisku "powierzchnia ściany"
    public void calculateSurfaceArea (View view) {
        if(roomPerimeter != 0 && roomHeightConfirm != 0) {
            showDoorsDialog(MainActivity.this);
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
                totalLength += difference.length();

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
        btnUp.setOnTouchListener(new View.OnTouchListener() {
        private Handler mHandler;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (anchorNodeHeight == null) {
                    btnSave.setEnabled(true);
                    anchorNodeHeight = currentAnchorNode.get(0);
                }

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
                Measurements measure = new Measurements(nameSurf.getText().toString(), Float.valueOf(surfaceArea));
                measurementViewModel.insert(measure);
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

    public void showDoorsDialog (Activity activity) {
        dialogDoors = new Dialog(activity);
        dialogDoors.setCancelable(false);
        dialogDoors.setContentView(R.layout.dialog_subtractsurface);
        dialogDoors.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnNo = dialogDoors.findViewById(R.id.btnSubtractNo);
        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogDoors.dismiss();
                surfaceArea = (float) (Math.round(roomHeightConfirm * roomPerimeter * 100) / 100.0);
                showAlertDialog(MainActivity.this);
                btnSave.setEnabled(true);
                roomHeightConfirm = 0;
            }
        });

        Button btnYes = dialogDoors.findViewById(R.id.btnSurfSave);
        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Config arConfig = mSession.getConfig();
                arConfig.setPlaneFindingMode(Config.PlaneFindingMode.VERTICAL);
                mSession.configure(arConfig);
                arFragment.getArSceneView().setupSession(mSession);
            }
        });
        dialogDoors.show();
    }

    public void showCalculatorDialog (Activity activity) {
        dialogCalculator = new Dialog(activity);
        dialogCalculator.setCancelable(false);
        dialogCalculator.setContentView(R.layout.dialog_calculator);
        dialogCalculator.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        buttonSub = (Button) dialogCalculator.findViewById(R.id.buttonSub);
        buttonAdd = (Button) dialogCalculator.findViewById(R.id.buttonAdd);
        buttonCalculator = (Button) dialogCalculator.findViewById(R.id.buttonCalculator);
        editTextGivenSurface = (EditText) dialogCalculator.findViewById(R.id.editTextGivenSurface);
        calcTextViewLayers = (TextView) dialogCalculator.findViewById(R.id.calcTextViewLayers);
        calcSpinner = (Spinner) dialogCalculator.findViewById(R.id.calcSpinner);
        editTextPaintEff = (EditText) dialogCalculator.findViewById(R.id.editTextPaintEff);
        respond = (TextView) dialogCalculator.findViewById(R.id.respond);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.opcje, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        calcSpinner.setAdapter(adapter);
        calcSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String text = parent.getItemAtPosition(position).toString();
                Toast.makeText(parent.getContext(),text,Toast.LENGTH_SHORT).show();
                switch (position){
                    case 0:
                        a=1;
                        break;
                    case 1:
                        a=1.2;
                        break;
                    case 2:
                        a=1.4;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        buttonSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCounter--;
                calcTextViewLayers.setText(Integer.toString(mCounter));
            }
        });

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCounter++;
                calcTextViewLayers.setText(Integer.toString(mCounter));
            }
        });

        buttonCalculator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                surfaceToPaint = Double.parseDouble(editTextGivenSurface.getText().toString());
                paintEfficiency = Double.parseDouble(editTextPaintEff.getText().toString());
                number1=a*surfaceToPaint*mCounter*paintEfficiency;
                number1= (double) (Math.round(number1*100)/100.0);
                number2=String.valueOf(number1);
                respond.setText("You should use "+ number2 + "l of paint");
            }
        });

        Button btnExt = dialogCalculator.findViewById(R.id.buttonCalculatorExit);
        btnExt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogCalculator.dismiss();
            }
        });

        dialogCalculator.show();
    }
}
