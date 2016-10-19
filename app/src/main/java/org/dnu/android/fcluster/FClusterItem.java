package org.dnu.android.fcluster;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;

/**
 * Created by isskj on 16/10/17.
 */

public class FClusterItem {

    private LatLng mPosition;

    private Marker mMarker;

    private Circle mCircle;

    private Polyline mPolyline;

    private Polygon mPolygon;

    private Object mTag;

    private enum MarkerType {
        CIRCLE, POLYLINE, POLYGON
    }

    private MarkerType mMarkerType;

    public FClusterItem(LatLng pos) {
        mPosition = pos;
    }

    public LatLng getPosition() {
        return mPosition;
    }

    void setMarker(Marker marker) {
        mMarker = marker;
    }

    public Marker getMarker() {
        return mMarker;
    }

    public void setTag(Object obj) {
        mTag = obj;
    }

    public Object getTag() {
        return mTag;
    }

    void setCircle(Circle circle) {
        mCircle = circle;
    }

    Circle getCircle() {
        return mCircle;
    }

    void setPolyline(Polyline polyline) {
        mPolyline = polyline;
    }

    public Polyline getPolyline() {
        return mPolyline;
    }

    void setPolygon(Polygon polygon) {
        mPolygon = polygon;
    }

    public Polygon getPolygon() {
        return mPolygon;
    }

    public boolean isCircle() {
        return mMarkerType == MarkerType.CIRCLE;
    }

    public void setCircle() {
        mMarkerType = MarkerType.CIRCLE;
    }

    public boolean isPolyline() {
        return mMarkerType == MarkerType.POLYLINE;
    }

    public void setPolyline() {
        mMarkerType = MarkerType.POLYLINE;
    }

    public boolean isPolygon() {
        return mMarkerType == MarkerType.POLYGON;
    }

    public void setPolygon() {
        mMarkerType = MarkerType.POLYGON;
    }
}

