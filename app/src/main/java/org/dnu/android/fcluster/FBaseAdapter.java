package org.dnu.android.fcluster;

import java.util.Collection;

/**
 */
public interface FBaseAdapter<T> {
    void addItem(T item);
    void addItems(Collection<T> items);
    void remove(T item);
    void clear();
}
