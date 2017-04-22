package buw.koenig.seltmann.mis_2017_exercise_2_maps_polygons;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ToggleButton toggleButton;
    private Button button;
    private EditText editText;
    private boolean drawPolygon = false;
    private Polygon polygon;
    private Marker centroid;

    private ArrayList<Marker> markers = new ArrayList<>();

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initialize();
    }

    private void initialize() {
        sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        editText     = (EditText)     findViewById(R.id.editText);
        button       = (Button)       findViewById(R.id.clearAllButton);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                drawPolygon = b;

                if (!b) {
                    if (polygon != null) {
                        polygon.remove();
                    }
                    if (centroid != null) {
                        centroid.remove();
                    }

                    for (Marker marker : markers) {
                        marker.remove();
                    }

                    markers.clear();
                }
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.clear();
                markers.clear();

                if (polygon != null) {
                    polygon.remove();
                }
                if (centroid != null) {
                    centroid.remove();
                }
                toggleButton.setChecked(false);
                deleteSharedPrefences();
            }
        });
    }

    private void hideSoftKeyboard(Activity activity) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e) {
            Toast.makeText(activity, "Cannot hide keyboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void readSharedPreferences() {
        for (String posString : sharedPreferences.getAll().keySet()) {
            String[] posArray = posString.split(",");
            LatLng pos = new LatLng(Double.parseDouble(posArray[0]),
                                    Double.parseDouble(posArray[1]));

            String message = sharedPreferences.getString(posString, "");

            mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(message));
        }
    }

    private void writeSharedPreferences(LatLng pos, String msg) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String posString = pos.latitude + "," + pos.longitude;
        editor.putString(posString, msg);
        editor.apply();
    }

    private void deleteSharedPrefences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission not granted!", Toast.LENGTH_LONG).show();
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        readSharedPreferences();

        // Add a marker in Sydney and move the camera
        LatLng weimar = new LatLng(50.979492, 11.323544);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(weimar));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                hideSoftKeyboard(MapsActivity.this);

                if (polygon != null) {
                    polygon.remove();
                }

                if (centroid != null) {
                    centroid.remove();
                }

                String message = editText.getText().toString();

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(message);

                if (drawPolygon) {
                    markers.add(mMap.addMarker(markerOptions));
                    drawPolygon();

                } else {
                    writeSharedPreferences(latLng, message);
                    mMap.addMarker(markerOptions);
                }

                editText.setText("");
            }
        });
    }

    private void drawPolygon() {
        PolygonOptions polygonOptions = new PolygonOptions()
                .strokeColor(Color.BLACK)
                .strokeWidth(5)
                .fillColor(Color.argb(50, 76, 187, 23));

        for (Marker marker : markers) {
            polygonOptions.add(marker.getPosition());
        }

        polygon = mMap.addPolygon(polygonOptions);

        if (polygonOptions.getPoints().size() >= 3) {
            double area = SphericalUtil.computeArea(polygon.getPoints());

            DecimalFormat decimalFormat = new DecimalFormat("##.##");

            String areaMessage = area >= 100000
                     ? "Area: " + decimalFormat.format(area/1000000.0) + " km^2"
                     : "Area: " + decimalFormat.format(area) + " m^2";

            LatLng centroidPos = getCentroid(polygonOptions.getPoints());

            centroid = mMap.addMarker(new MarkerOptions()
                    .position(centroidPos)
                    .title(areaMessage)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
            centroid.showInfoWindow();
        }
    }

    /**
     * https://en.wikipedia.org/wiki/Centroid#Of_a_finite_set_of_points
     * @param points
     * @return
     */
    private LatLng getCentroid(List<LatLng> points) {
        double x = 0.0;
        double y = 0.0;

        for (LatLng point : points) {
            x += point.latitude;
            y += point.longitude;
        }

        int size = points.size();
        return new LatLng(x / size, y / size);
    }
}
