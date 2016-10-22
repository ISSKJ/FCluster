package org.dnu.android.testfcluster;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.dnu.android.fcluster.FCluster;
import org.dnu.android.fcluster.FClusterAdapter;
import org.dnu.android.fcluster.FClusterItem;
import org.dnu.android.fcluster.MapIconGenerator;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, FClusterAdapter.ClusterListener, FClusterAdapter.InfoWindowListener {

    private MapIconGenerator mMapIconGenerator;

    private FClusterAdapter mAdapter;

    private LatLng[] dummy;

    private boolean mShouldCluster;

    final Random mRandom = new Random();

    private String[] dummyImage = {
            "http://megaicons.net/static/img/icons_sizes/311/1406/256/mario-icon.png",
            "https://cdn.tutsplus.com/vector/uploads/2013/11/chris-flower-600.png",
            "http://linuxsoid.com/picte/APP/browser/Chromium.png",
            "https://s-media-cache-ak0.pinimg.com/originals/40/8b/e5/408be5c5d1a4e5671e259eb3d40efa5f.jpg",
            "http://icons.iconseeker.com/png/fullsize/doraemon/smile-6.png",
            "https://duckduckgo.com/assets/icons/meta/DDG-icon_256x256.png",
            "https://lh5.ggpht.com/WLSUWqj9TsA2T5gcaWe4zpNJq1i81kRHIGDSm5aJ9wM1QijBqlnVSkV7V_f9Oy3S1Lg=h310",
            "http://s.hswstatic.com/gif/animal-stereotype-orig.jpg",
            "https://pbs.twimg.com/profile_images/378800000831249044/effb57c08b2f5783c686b589d84d2b92.jpeg",
            "http://wallpaperwarrior.com/wp-content/uploads/2016/08/Animal-Wallpaper-16-1024x640.jpg",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SeekBar seekBar = (SeekBar)findViewById(R.id.scale_seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mAdapter.isRendering()) {
                    setRandomData(seekBar.getProgress());
                }
            }
        });
        seekBar.setProgress(100);
        mMapIconGenerator = new MapIconGenerator(getApplicationContext());

        // map fragment
        final SupportMapFragment fragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.map_container, fragment)
                .commit();
        fragment.getMapAsync(this);

        final View.OnClickListener clusterButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShouldCluster = !mShouldCluster;
                ((TextView)view).setText(mShouldCluster ? "Cluster" : "UnCluster");
            }
        };
        findViewById(R.id.cluster_button).setOnClickListener(clusterButtonListener);
        clusterButtonListener.onClick(findViewById(R.id.cluster_button));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
        }
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setIndoorEnabled(false);
        final LatLng latLng = new LatLng(35.686533327621, 139.69192653894);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        int density = getResources().getDimensionPixelSize(R.dimen.cluster_density);
        mAdapter = new MyClusterAdapter(new Handler(Looper.getMainLooper()), googleMap, density);
        mAdapter.setClusterListener(this);
        mAdapter.setInfoWindowAdapter(new MyInfoWindowAdapter(), this);
        setRandomData(100);
    }

    private void setRandomData(int progress) {
        int size = 100000;
        dummy = new LatLng[size];
        for (int i = 0; i < size; i++) {
            double w = progress * 5.0 * Math.sqrt(mRandom.nextDouble());
            double t = 2 * Math.PI * mRandom.nextDouble();
            double x = w * Math.cos(t);
            double y = w * Math.sin(t);
            dummy[i] = new LatLng(x, y);
        }

        mAdapter.clear();
        FClusterItem item;
        for (int i = 0; i < dummy.length; i++) {
            item = new FClusterItem(dummy[i]);
            final Sample sample = new Sample();
            sample.title = String.valueOf(mRandom.nextInt(1000));
            sample.snippet = "ISSKJ";
            sample.imageURL = dummyImage[mRandom.nextInt(dummyImage.length-1)];
            //sample.radius = 1000000;
            //sample.polylineLatLng = dummy[mRandom.nextInt(dummy.length-1)];
            //sample.polygonLatLng = new ArrayList<>();
            //final LatLng latLng1 = new LatLng(dummy[i].latitude+1, dummy[i].longitude+1);
            //final LatLng latLng2 = new LatLng(dummy[i].latitude+1, dummy[i].longitude-1);
            //final LatLng latLng3 = new LatLng(dummy[i].latitude-1, dummy[i].longitude+1);
            //final LatLng latLng4 = new LatLng(dummy[i].latitude-1, dummy[i].longitude-1);
            //sample.polygonLatLng.add(latLng1);
            //sample.polygonLatLng.add(latLng2);
            //sample.polygonLatLng.add(latLng3);
            //sample.polygonLatLng.add(latLng4);

            item.setTag(sample);
            //item.setPolygon();
            //item.setPolyline();
            //item.setCircle();
            mAdapter.addItem(item);
        }
    }

    @Override
    public void onClickClister(FCluster cluster) {
        Log.d("FCluster", "cluster clicked."+cluster.getSize());
    }

    @Override
    public void onClickClisterItem(FClusterItem item) {
        Log.d("FCluster", "cluster item clicked."+item.getMarker().getTitle());
    }

    @Override
    public void onClickInfoWindow(FClusterItem item) {
        Log.d("FCluster", "cluster item info window clicked."+item.getMarker().getTitle());
    }

    @Override
    public void onClickInfoWindow(FCluster cluster) {
        Log.d("FCluster", "cluster info window clicked."+cluster.getSize());
    }

    private class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) {
            return getInfoContents(marker);
        }

        @Override
        public View getInfoContents(Marker marker) {
            final View root = LayoutInflater.from(getApplicationContext()).inflate(R.layout.layout_info_window, null);
            final TextView tvTitle = (TextView)root.findViewById(R.id.title_text);
            final TextView tvSnippet = (TextView)root.findViewById(R.id.snippet_text);

            final Object tag = mAdapter.getMarkerTag(marker);
            if (tag instanceof FCluster) {
                final FCluster cluster = (FCluster)tag;
                final StringBuilder sb = new StringBuilder();
                sb.append("cluster size:"+cluster.getSize());
                sb.append("\n");
                for (FClusterItem item : cluster.getClusterItems()) {
                    final Sample sample = (Sample) item.getTag();
                    sb.append(sample.title);
                    sb.append("\n");
                }
                tvTitle.setText(sb);
            } else if (tag instanceof FClusterItem) {
                FClusterItem item = (FClusterItem)tag;
                if (item.getTag() instanceof Sample) {
                    final Sample sample = (Sample) item.getTag();
                    tvTitle.setText(sample.title);
                    tvSnippet.setText(sample.snippet);
                }
            }
            return root;
        }
    }

    private class MyClusterAdapter extends FClusterAdapter {

        private MyClusterAdapter(Handler uiHandler, GoogleMap map, int density) {
            super(uiHandler, map, density);
        }

        @Override
        public MarkerOptions newClusterOption(FCluster cluster) {
            MarkerOptions options = new MarkerOptions();
            options.position(cluster.getPosition());
            options.icon(mMapIconGenerator.getClusterImageDescriptor(cluster.getSize()));
            return options;
        }

        @Override
        public MarkerOptions newMarkerOption(FClusterItem item) {
            if (item.getTag() instanceof Sample) {
                final Sample sample = (Sample)item.getTag();
                final MarkerOptions options = new MarkerOptions();
                options.position(item.getPosition());
                options.icon(mMapIconGenerator.getDescriptor(sample.imageURL));
                return options;
            }
            return null;
        }

        @Override
        public CircleOptions newCircleOption(FClusterItem item) {
            if (item.getTag() instanceof Sample) {
                final Sample sample = (Sample)item.getTag();
                final CircleOptions options = new CircleOptions();
                options.center(item.getPosition());
                options.radius(sample.radius);
                options.strokeWidth(3f);
                options.strokeColor(Color.DKGRAY);
                return options;
            }
            return null;
        }

        @Override
        public PolylineOptions newPolylineOption(FClusterItem item) {
            if (item.getTag() instanceof Sample) {
                final Sample sample = (Sample)item.getTag();
                final PolylineOptions options = new PolylineOptions();
                options.add(item.getPosition());
                options.add(sample.polylineLatLng);
                options.geodesic(false);
                options.width(10f);
                options.color(Color.MAGENTA);
                return options;
            }
            return null;
        }

        @Override
        public PolygonOptions newPolygonOption(FClusterItem item) {
            if (item.getTag() instanceof Sample) {
                final Sample sample = (Sample)item.getTag();
                final PolygonOptions options = new PolygonOptions();
                options.addAll(sample.polygonLatLng);
                options.geodesic(false);
                options.strokeWidth(10f);
                options.strokeColor(Color.MAGENTA);
                return options;
            }
            return null;
        }

        @Override
        public boolean shouldCluster() {
            return mShouldCluster;
        }
    }
}
