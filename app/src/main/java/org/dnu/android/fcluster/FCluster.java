package org.dnu.android.fcluster;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by isskj on 16/10/17.
 */

public class FCluster extends FClusterItem {

    private final Collection<FClusterItem> mItems = new ArrayList<>();

    private LatLngBounds mBounds;

    public FCluster(LatLng pos) {
        super(pos);
    }

    public void addItem(FClusterItem item) {
        mItems.add(item);
    }

    public boolean contains(FClusterItem item) {
        return mItems.contains(item);
    }

    public void setBounds(LatLngBounds bounds) {
        mBounds = bounds;
    }
    public float getRadius() {
        Location locA = new Location("A");
        Location locB = new Location("B");
        locA.setLatitude(mBounds.northeast.latitude);
        locA.setLongitude(mBounds.northeast.longitude);
        locB.setLatitude(mBounds.southwest.latitude);
        locB.setLongitude(mBounds.northeast.longitude);
        return locA.distanceTo(locB) / 2;
    }

    public void remove(FClusterItem item) {
        mItems.remove(item);
    }

    public int getSize() {
        return mItems.size();
    }

    public Collection<FClusterItem> getClusterItems() {
        return mItems;
    }
}

