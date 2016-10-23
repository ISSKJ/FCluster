package org.dnu.android.fcluster;

import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Cluster & Marker adapter
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

    private final ArrayList<FClusterItem> mItems = new ArrayList<>();
    private final ArrayList<FCluster> mClusters = new ArrayList<>();

    private SparseArray<MarkerOptions> mCacheClusterMarkerOption = new SparseArray<>();

    private ClusterListener mClusterListener;

    private GoogleMap mMap;

    private ArrayBlockingQueue<Runnable> mExecutionTasks = new ArrayBlockingQueue<>(1);

    private Handler mUIHandler;

    private int mClusterDensity;

    private boolean mMarkerClicked;

    private float mZoomLevel;

    public boolean isRendering() {
        return mExecutionTasks.remainingCapacity() == 0;
    }

    public FClusterAdapter(Handler uiHandler, GoogleMap map, int density) {
        mMap = map;
        mMap.setOnCameraIdleListener(this);
        mMap.setIndoorEnabled(false);
        mUIHandler = uiHandler;
        mClusterDensity = density;
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                mMarkerClicked = true;
                mExecutionTasks.clear();
                if (marker.getTag() instanceof FCluster) {
                    if (mClusterListener != null) {
                        mClusterListener.onClickClister((FCluster)marker.getTag());
                    }
                    return false;
                }
                if (marker.getTag() instanceof FClusterItem) {
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
                    Runnable runnable = mExecutionTasks.poll();
                    if (runnable != null) {
                        runnable.run();
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
    }

    @Override
    public void clear() {
        mItems.clear();
        mMap.clear();
        mClusters.clear();
    }

    public abstract MarkerOptions newClusterOption(FCluster cluster);

    public abstract MarkerOptions newMarkerOption(FClusterItem item);

    public abstract CircleOptions newCircleOption(FClusterItem item);

    public abstract PolylineOptions newPolylineOption(FClusterItem item);

    public abstract PolygonOptions newPolygonOption(FClusterItem item);

    public abstract boolean shouldCluster();

    public void onRendered(boolean rendering) {
    }

    @Override
    public void onCameraIdle() {
        if (mMarkerClicked) {
            mMarkerClicked = false;
            return;
        }
        if (!isRendering()) {
            final float zoom = mMap.getCameraPosition().zoom;
            final boolean zoomed = zoom != mZoomLevel;
            if (zoom != mZoomLevel) {
                mZoomLevel = zoom;
            }
            computeCluster(zoomed);
        }
    }

    private void computeCluster(final boolean zoomed) {
        final LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        final Projection projection = mMap.getProjection();
        mExecutionTasks.add(new Runnable() {
            @Override
            public void run() {
                if (mMap == null) {
                    return;
                }
                postUI(new Runnable() {
                    @Override
                    public void run() {
                        onRendered(true);
                    }
                });

                final ArrayList<FCluster> clusters;
                if (zoomed) {
                    clusters = new ArrayList<>();
                } else {
                    clusters = new ArrayList<>(mClusters);
                }

                final ArrayList<FClusterItem> visibleItems = new ArrayList<>();
                for (FClusterItem item : mItems) {
                    if (bounds.contains(item.getPosition())) {
                        visibleItems.add(item);
                    }
                    item.setMarker(null);
                }

                ArrayList<FClusterItem> modifiedItems = visibleItems;
                Collections.shuffle(modifiedItems);
                for (int i = 0; i < modifiedItems.size(); i++) {
                    final FClusterItem item = modifiedItems.get(i);
                    final LatLngBounds bounds = getVisibleLatLngBounds(projection, item);

                    final FCluster cluster = new FCluster(item.getPosition());
                    cluster.setBounds(bounds);

                    final ArrayList<FClusterItem> tmp = new ArrayList<>(modifiedItems);
                    for (int j = 0; j < modifiedItems.size(); j++) {
                        final FClusterItem item2 = modifiedItems.get(j);
                        if (bounds.contains(item2.getPosition())) {
                            cluster.addItem(item2);
                            tmp.remove(item2);
                        }
                    }
                    if (cluster.getSize() > MIN_CLUSTER_SIZE && shouldCluster()) {
                        clusters.add(cluster);
                        modifiedItems = tmp;
                        i = 0;
                    }
                }

                ArrayList<FCluster> modified = clusters;
                for (int i = 0; i < modified.size(); i++) {
                    final FCluster cluster = modified.get(i);
                    final LatLngBounds bounds = getVisibleLatLngBounds(projection, cluster);

                    for (int j = 0; j < modified.size(); j++) {
                        final FCluster cluster2 = modified.get(j);
                        if (cluster == cluster2) {
                            continue;
                        }
                        if (bounds.contains(cluster2.getPosition())) {
                            cluster.merge(cluster2);
                            modified.remove(cluster2);
                            i = 0;
                        }
                    }
                }

                for (FCluster cluster : clusters) {
                    for (FClusterItem item : cluster.getClusterItems()) {
                        visibleItems.remove(item);
                    }
                    cluster.setMarker(null);
                }

                mClusters.clear();
                mClusters.addAll(clusters);

                boolean shouldRender = false;
                for (FClusterItem item : visibleItems) {
                    final Marker marker = item.getMarker();
                    if (marker == null) {
                        shouldRender = true;
                        break;
                    }
                }
                for (FCluster cluster : clusters) {
                    final Marker marker = cluster.getMarker();
                    if (marker == null) {
                        shouldRender = true;
                        break;
                    }
                }
                if (!shouldRender) {
                    postUI(new Runnable() {
                        @Override
                        public void run() {
                            onRendered(false);
                        }
                    });
                    return;
                }

                // update cluster
                postUI(new Runnable() {
                    @Override
                    public void run() {
                        if (mMap == null) {
                            return;
                        }
                        mMap.clear();

                        for (FClusterItem item : visibleItems) {
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

                        for (FCluster cluster : clusters) {
                            addCluster(cluster);
                        }
                        onRendered(false);
                    }
                });
            }
        });
    }

    private void postUI(Runnable runnable) {
        if (mUIHandler != null) {
            mUIHandler.post(runnable);
        }
    }

    private void addCluster(FCluster cluster) {
        MarkerOptions options = mCacheClusterMarkerOption.get(cluster.getSize());
        if (options == null) {
            options = newClusterOption(cluster);
        }
        if (options != null) {
            cluster.setMarker(mMap.addMarker(options));
            cluster.getMarker().setTag(cluster);
        } else {
            cluster.setMarker(null);
        }
    }

    private void addMarker(FClusterItem item) {
        MarkerOptions options = newMarkerOption(item);
        if (options != null) {
            item.setMarker(mMap.addMarker(options));
            item.getMarker().setTag(item);
        } else {
            item.setMarker(null);
        }
    }

    private void addCircle(FClusterItem item) {
        final CircleOptions circleOptions = newCircleOption(item);
        if (circleOptions != null) {
            item.setCircle(mMap.addCircle(circleOptions));
        } else {
            item.setCircle(null);
        }
    }

    private void addPolyline(FClusterItem item) {
        final PolylineOptions polylineOptions = newPolylineOption(item);
        if (polylineOptions != null) {
            item.setPolyline(mMap.addPolyline(polylineOptions));
        } else {
            item.setPolyline(null);
        }
    }

    private void addPolygon(FClusterItem item) {
        final PolygonOptions polygonOptions = newPolygonOption(item);
        if (polygonOptions != null) {
            item.setPolygon(mMap.addPolygon(polygonOptions));
        } else {
            item.setPolygon(null);
        }
    }

    private LatLngBounds getVisibleLatLngBounds(Projection projection, FClusterItem item) {
        final Point pItem = projection.toScreenLocation(item.getPosition());
        final Point southWest = new Point(pItem.x - mClusterDensity, pItem.y + mClusterDensity);
        final Point northEast = new Point(pItem.x + mClusterDensity, pItem.y - mClusterDensity);
        return new LatLngBounds(projection.fromScreenLocation(southWest), projection.fromScreenLocation(northEast));
    }

    private void debug(Object obj) {
        Log.d(TAG, obj.toString());
    }
}

