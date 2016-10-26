package org.dnu.android.fcluster;

import android.graphics.Point;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Cluster & Marker adapter
 */
public abstract class FClusterAdapter implements FBaseAdapter<FClusterItem>, GoogleMap.OnCameraIdleListener {

    public interface ClusterListener {
        boolean onClickCluster(FCluster cluster);
        boolean onClickClusterItem(FClusterItem item);
    }

    public interface InfoWindowListener {
        void onClickInfoWindow(FClusterItem item);
        void onClickInfoWindow(FCluster cluster);
    }

    public interface RenderListener {
        void onFinishRendered();
    }

    private static final String TAG = FClusterAdapter.class.getSimpleName();

    private static final int MIN_CLUSTER_SIZE = 5;

    protected final ArrayList<FClusterItem> mItems = new ArrayList<>();
    private final ArrayList<FCluster> mClusters = new ArrayList<>();
    private final ArrayList<FClusterItem> mDrawedItems = new ArrayList<>();

    private ClusterListener mClusterListener;

    private RenderListener mRenderListener;

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
                        return mClusterListener.onClickCluster((FCluster)marker.getTag());
                    }
                    return false;
                }
                if (marker.getTag() instanceof FClusterItem) {
                    if (mClusterListener != null) {
                        return mClusterListener.onClickClusterItem((FClusterItem)marker.getTag());
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

    public void setRenderListener(RenderListener listener) {
        mRenderListener = listener;
    }

    public void render() {
        onCameraIdle();
    }

    public float getZoomLevel() {
        return mZoomLevel;
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

    public abstract boolean shouldCluster(FClusterItem item);

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
                }

                ArrayList<FClusterItem> modifiedItems = visibleItems;
                for (int i = 0; i < modifiedItems.size(); i++) {
                    final FClusterItem item = modifiedItems.get(i);
                    final LatLngBounds bounds = getVisibleLatLngBounds(projection, item);

                    final FCluster cluster = new FCluster(item.getPosition());
                    cluster.setBounds(bounds);

                    final ArrayList<FClusterItem> tmp = new ArrayList<>(modifiedItems);
                    for (int j = 0; j < modifiedItems.size(); j++) {
                        if (i == j) {
                            continue;
                        }
                        final FClusterItem item2 = modifiedItems.get(j);
                        if (bounds.contains(item2.getPosition())) {
                            cluster.addItem(item2);
                            tmp.remove(item2);
                        }
                    }
                    if (cluster.getSize() > MIN_CLUSTER_SIZE && shouldCluster(item)) {
                        cluster.addItem(item);
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
                    visibleItems.removeAll(cluster.getClusterItems());
                    mDrawedItems.addAll(cluster.getClusterItems());
                }

                uniqueClusterItem(visibleItems);
                uniqueCluster(clusters);
                uniqueClusterItem(mDrawedItems);

                mClusters.clear();
                mClusters.addAll(clusters);

                debug("visible item:"+visibleItems.size());
                debug("visible cluster:"+clusters.size());
                debug("remove items:"+mDrawedItems.size());

                // update cluster
                postUI(new Runnable() {
                    @Override
                    public void run() {
                        if (mMap == null) {
                            return;
                        }

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
                        clearMarker();
                        mDrawedItems.addAll(visibleItems);
                        mDrawedItems.addAll(clusters);
                        onRendered(false);

                        if (mRenderListener != null) {
                            mRenderListener.onFinishRendered();
                        }
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

    private void clearMarker() {
        for (FClusterItem item : mDrawedItems) {
            final Marker marker = item.getMarker();
            final Circle circle = item.getCircle();
            final Polyline polyline = item.getPolyline();
            final Polygon polygon = item.getPolygon();
            if (marker != null) {
                marker.remove();
                item.setMarker(null);
            }
            if (circle != null) {
                circle.remove();
                item.setCircle(null);
            }
            if (polyline != null) {
                polyline.remove();
                item.setPolyline(null);
            }
            if (polygon != null) {
                polygon.remove();
                item.setPolygon(null);
            }
        }
        mDrawedItems.clear();
    }

    private void addCluster(FCluster cluster) {
        final Marker marker = cluster.getMarker();
        MarkerOptions options = newClusterOption(cluster);
        if (options == null) {
            cluster.setMarker(null);
            return;
        }
        if (marker != null) {
            marker.setPosition(options.getPosition());
            marker.setTag(cluster);
            marker.setTitle(options.getTitle());
            marker.setSnippet(options.getSnippet());
            marker.setIcon(options.getIcon());
            mDrawedItems.remove(cluster);
            return;
        }
        cluster.setMarker(mMap.addMarker(options));
        cluster.getMarker().setTag(cluster);
    }

    private void addMarker(FClusterItem item) {
        final Marker marker = item.getMarker();
        MarkerOptions options = newMarkerOption(item);
        if (options == null) {
            item.setMarker(null);
            return;
        }
        if (marker != null) {
            marker.setPosition(options.getPosition());
            marker.setTitle(options.getTitle());
            marker.setSnippet(options.getSnippet());
            marker.setIcon(options.getIcon());
            marker.setTag(item);
            mDrawedItems.remove(item);
            return;
        }
        item.setMarker(mMap.addMarker(options));
        item.getMarker().setTag(item);
    }

    private void addCircle(FClusterItem item) {
        final Circle circle = item.getCircle();
        final CircleOptions options = newCircleOption(item);
        if (options == null) {
            item.setCircle(null);
            return;
        }
        if (circle != null) {
            circle.setCenter(options.getCenter());
            circle.setRadius(options.getRadius());
            circle.setFillColor(options.getFillColor());
            circle.setStrokeColor(options.getStrokeColor());
            circle.setStrokeWidth(options.getStrokeWidth());
            return;
        }
        item.setCircle(mMap.addCircle(options));
    }

    private void addPolyline(FClusterItem item) {
        final Polyline polyline = item.getPolyline();
        final PolylineOptions options = newPolylineOption(item);
        if (options == null) {
            item.setPolyline(null);
            return;
        }
        if (polyline != null) {
            polyline.setColor(options.getColor());
            polyline.setGeodesic(options.isGeodesic());
            polyline.setPoints(options.getPoints());
            polyline.setWidth(options.getWidth());
            return;
        }
        item.setPolyline(mMap.addPolyline(options));
    }

    private void addPolygon(FClusterItem item) {
        final PolygonOptions polygonOptions = newPolygonOption(item);
        if (polygonOptions != null) {
            item.setPolygon(mMap.addPolygon(polygonOptions));
        } else {
            item.setPolygon(null);
        }
    }

    private void uniqueClusterItem(ArrayList<FClusterItem> list) {
        Set<FClusterItem> set = new HashSet<>(list);
        list.clear();
        list.addAll(set);
    }
    private void uniqueCluster(ArrayList<FCluster> list) {
        Set<FCluster> set = new HashSet<>(list);
        list.clear();
        list.addAll(set);
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

    public VisibleRegion getVisibleRegion() {
        return mMap.getProjection().getVisibleRegion();
    }

    public GoogleMap getMap() {
        return mMap;
    }

    public void zoomToFurthestMarker() {
        boolean shouldZoom = false;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (FClusterItem item : mItems) {
            builder.include(item.getPosition());
            shouldZoom = true;
        }
        if (shouldZoom) {
            LatLngBounds bounds = builder.build();
            int padding = 100;
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }
    }

    public void moveToLocation(LatLng latLng, boolean animate, float defaultZoom) {
        if (animate) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, defaultZoom));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, defaultZoom));
        }
    }

    public ArrayList<FClusterItem> getItems() {
        return mItems;
    }


    public void animateCamera(CameraUpdate cameraUpdate) {
        getMap().animateCamera(cameraUpdate, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                onCameraIdle();
            }

            @Override
            public void onCancel() {
            }
        });
    }
}

