package org.dnu.android.fcluster;

import android.graphics.Point;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by isskj on 16/10/17.
 */

public abstract class FClusterAdapter implements FBaseAdapter<FClusterItem>, GoogleMap.OnCameraIdleListener {

    public interface ClusterListener {
        void onClickClister(FCluster cluster);
        void onClickClisterItem(FClusterItem item);
    }

    public interface InfoWindowListener {
        void onClickInfoWindow(FClusterItem item);
        void onClickInfoWindow(FCluster cluster);
    }

    private static final String TAG = FClusterAdapter.class.getSimpleName();

    private static final int MIN_CLUSTER_SIZE = 3;
    private static final int MAX_RENDERING_TASK_QUEUE = 5;

    private final ArrayList<FClusterItem> mItems = new ArrayList<>();
    private final ArrayList<FClusterItem> mVisibleItems = new ArrayList<>();
    private final ArrayList<FCluster> mClusters = new ArrayList<>();

    private ClusterListener mClusterListener;

    private GoogleMap mMap;

    private ArrayBlockingQueue<Runnable> mExecutionTasks = new ArrayBlockingQueue<>(MAX_RENDERING_TASK_QUEUE);

    private Handler mUIHandler;

    private int mClusterDensity;

    private boolean mMarkerClicked;

    public boolean isRendering() {
        return mExecutionTasks.size() > 0;
    }

    public FClusterAdapter(Handler uiHandler, GoogleMap map, int density) {
        mMap = map;
        mMap.setOnCameraIdleListener(this);
        mUIHandler = uiHandler;
        mClusterDensity = density;
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.d(TAG, "marker click:");
                mMarkerClicked = true;
                mExecutionTasks.clear();
                if (marker.getTag() instanceof FCluster) {
                    Log.d(TAG, "marker cluster hit:");
                    if (mClusterListener != null) {
                        mClusterListener.onClickClister((FCluster)marker.getTag());
                    }
                    return false;
                }
                if (marker.getTag() instanceof FClusterItem) {
                    Log.d(TAG, "marker cluster item hit:");
                    if (mClusterListener != null) {
                        mClusterListener.onClickClisterItem((FClusterItem)marker.getTag());
                    }
                    return false;
                }
                return false;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mMap != null) {
                    try {
                        Log.d(TAG, "task running:");
                        Runnable runnable = mExecutionTasks.take();
                        Log.d(TAG, "task posted:");
                        runnable.run();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "run:"+e.toString());
                    }
                }
            }
        }).start();
    }

    public void setInfoWindowAdapter(GoogleMap.InfoWindowAdapter adapter, final InfoWindowListener listener) {
        mMap.setInfoWindowAdapter(adapter);
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (listener != null) {
                    if (marker.getTag() instanceof FCluster) {
                        listener.onClickInfoWindow((FCluster) marker.getTag());
                    } else if (marker.getTag() instanceof FClusterItem) {
                        listener.onClickInfoWindow((FClusterItem) marker.getTag());
                    }
                }
            }
        });
    }

    public Object getMarkerTag(Marker marker) {
        return marker.getTag();
    }

    public void setClusterListener(ClusterListener listener) {
        mClusterListener = listener;
    }

    @Override
    public void addItem(FClusterItem item) {
        mItems.add(item);
    }

    @Override
    public void addItems(Collection<FClusterItem> items) {
        mItems.addAll(items);
    }

    @Override
    public void remove(FClusterItem item) {
        mItems.remove(item);
        mVisibleItems.remove(item);
    }

    @Override
    public void clear() {
        mItems.clear();
        mVisibleItems.clear();
    }

    public abstract MarkerOptions newClusterOption(FCluster cluster);

    public abstract MarkerOptions newMarkerOption(FClusterItem item);

    public abstract CircleOptions newCircleOption(FClusterItem item);

    public abstract PolylineOptions newPolylineOption(FClusterItem item);

    public abstract PolygonOptions newPolygonOption(FClusterItem item);

    public abstract boolean shouldCluster();

    @Override
    public void onCameraIdle() {
        if (mMarkerClicked) {
            mMarkerClicked = false;
            return;
        }
        if (mExecutionTasks.size() < MAX_RENDERING_TASK_QUEUE) {
            computeCluster();
        }
    }

    private void computeCluster() {
        final LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        final Projection projection = mMap.getProjection();
        mExecutionTasks.add(new Runnable() {
            @Override
            public void run() {
                if (mMap == null) {
                    return;
                }
                mVisibleItems.clear();
                for (FClusterItem item : mItems) {
                    if (bounds.contains(item.getPosition())) {
                        mVisibleItems.add(item);
                    }
                }
                Log.d(TAG, "visible count:"+mVisibleItems.size());

                final ArrayList<FCluster> clusters = new ArrayList<>();
                ArrayList<FClusterItem> modifiedItems = new ArrayList<>(mVisibleItems);
                for (int i = 0; i < modifiedItems.size(); i++) {
                    final FClusterItem item = modifiedItems.get(i);
                    final Point pItem = projection.toScreenLocation(item.getPosition());
                    final Point southWest = new Point(pItem.x - mClusterDensity, pItem.y + mClusterDensity);
                    final Point northEast = new Point(pItem.x + mClusterDensity, pItem.y - mClusterDensity);
                    final LatLngBounds bounds = new LatLngBounds(projection.fromScreenLocation(southWest), projection.fromScreenLocation(northEast));
                    final FCluster cluster = new FCluster(item.getPosition());
                    cluster.setBounds(bounds);

                    ArrayList<FClusterItem> tmp = new ArrayList<>(modifiedItems);
                    tmp.remove(item);
                    for (int j = 0; j < modifiedItems.size(); j++) {
                        final FClusterItem item2 = modifiedItems.get(j);
                        if (bounds.contains(item2.getPosition())) {
                            cluster.addItem(item2);
                            tmp.remove(item2);
                        }
                    }
                    if (cluster.getSize() > MIN_CLUSTER_SIZE && shouldCluster()) {
                        clusters.add(cluster);
                    }
                    modifiedItems = tmp;
                }
                final ArrayList<FClusterItem> shouldAddItems = new ArrayList<>();
                final ArrayList<FClusterItem> shouldRemoveItems = new ArrayList<>();
                final ArrayList<FClusterItem> unclusters = new ArrayList<>(mVisibleItems);
                for (FCluster cluster : clusters) {
                    for (FClusterItem item : cluster.getClusterItems()) {
                        unclusters.remove(item);
                        shouldRemoveItems.add(item);
                    }
                }
                for (FClusterItem item : unclusters) {
                    if (bounds.contains(item.getPosition())) {
                        shouldAddItems.add(item);
                    }
                }
                Log.d(TAG, "uncluster count:"+unclusters.size());
                if (mUIHandler != null) {
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "clusters sum:"+clusters.size());
                            clearCluster();
                            for (FCluster cluster : clusters) {
                                Log.d(TAG, "cluster count:"+cluster.getSize());
                                addCluster(cluster);
                            }
                            for (FClusterItem item : shouldRemoveItems) {
                                removeMarker(item);
                                if (item.isCircle()) {
                                    removeCircle(item);
                                }
                                if (item.isPolyline()) {
                                    removePolyline(item);
                                }
                                if (item.isPolygon()) {
                                    removePolygon(item);
                                }
                            }
                            for (FClusterItem item : shouldAddItems) {
                                addMarker(item);
                                if (item.isCircle()) {
                                    addCircle(item);
                                }
                                if (item.isPolyline()) {
                                    addPolyline(item);
                                }
                                if (item.isPolygon()) {
                                    addPolygon(item);
                                }
                            }
                            Log.d(TAG, "should remove count:"+shouldRemoveItems.size());
                            Log.d(TAG, "should add count:"+shouldAddItems.size());
                        }
                    });
                }
            }
        });
    }

    private void addCluster(FCluster cluster) {
        mClusters.add(cluster);
        MarkerOptions options = newClusterOption(cluster);
        if (options != null) {
            cluster.setMarker(mMap.addMarker(options));
            cluster.getMarker().setTag(cluster);
        } else {
            cluster.setMarker(null);
        }
    }

    private void clearCluster() {
        for (FCluster cluster : mClusters) {
            final Marker marker = cluster.getMarker();
            if (marker != null) {
                marker.remove();
            }
        }
    }

    private void addMarker(FClusterItem item) {
        removeMarker(item);
        MarkerOptions options = newMarkerOption(item);
        if (options != null) {
            item.setMarker(mMap.addMarker(options));
            item.getMarker().setTag(item);
        } else {
            item.setMarker(null);
        }
    }

    private void removeMarker(FClusterItem item) {
        final Marker marker = item.getMarker();
        if (marker != null) {
            marker.remove();
            item.setMarker(null);
        }
    }

    private void addCircle(FClusterItem item) {
        removeCircle(item);
        final CircleOptions circleOptions = newCircleOption(item);
        if (circleOptions != null) {
            item.setCircle(mMap.addCircle(circleOptions));
        } else {
            item.setCircle(null);
        }
    }

    private void removeCircle(FClusterItem item) {
        final Circle circle = item.getCircle();
        if (circle != null) {
            circle.remove();
            item.setCircle(null);
        }
    }

    private void addPolyline(FClusterItem item) {
        removePolyline(item);
        final PolylineOptions polylineOptions = newPolylineOption(item);
        if (polylineOptions != null) {
            item.setPolyline(mMap.addPolyline(polylineOptions));
        } else {
            item.setPolyline(null);
        }
    }

    private void removePolyline(FClusterItem item) {
        final Polyline polyline = item.getPolyline();
        if (polyline != null) {
            polyline.remove();
            item.setPolyline(null);
        }
    }

    private void addPolygon(FClusterItem item) {
        removePolygon(item);
        final PolygonOptions polygonOptions = newPolygonOption(item);
        if (polygonOptions != null) {
            item.setPolygon(mMap.addPolygon(polygonOptions));
        } else {
            item.setPolygon(null);
        }
    }

    private void removePolygon(FClusterItem item) {
        final Polygon polygon = item.getPolygon();
        if (polygon != null) {
            polygon.remove();
            item.setPolygon(null);
        }
    }
}

