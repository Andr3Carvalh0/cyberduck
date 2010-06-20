package ch.cyberduck.core;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * A cache for remote directory listings
 *
 * @version $Id$
 */
public class Cache<E extends AbstractPath> {
    protected static Logger log = Logger.getLogger(Cache.class);

    /**
     *
     */
    private Map<PathReference, AttributedList<E>> _impl = Collections.<PathReference, AttributedList<E>>synchronizedMap(new LRUMap(
            Preferences.instance().getInteger("browser.cache.size")
    ) {
        @Override
        protected boolean removeLRU(LinkEntry entry) {
            log.debug("Removing from cache:" + entry);
            return true;
        }
    });

    /**
     * Lookup a path by reference in the cache. Expensive as its parent directory must be
     * evaluated first.
     *
     * @param reference A child object of a cached directory listing in the cache
     * @return Null if the path is no more cached.
     * @see ch.cyberduck.core.AttributedList#get(PathReference)
     */
    public E lookup(PathReference reference) {
        for(AttributedList list : _impl.values()) {
            final AbstractPath path = list.get(reference);
            if(null == path) {
                continue;
            }
            return (E) path;
        }
        log.warn("Lookup failed for " + reference + " in cache");
        return null;
    }

    /**
     * @param reference Absolute path
     * @return True if the directory listing of this path is cached
     */
    public boolean containsKey(PathReference reference) {
        return _impl.containsKey(reference);
    }

    /**
     * Remove the cached directory listing for this path
     *
     * @param reference Reference to the path in cache.
     * @return The previuosly cached directory listing
     */
    public AttributedList<E> remove(PathReference reference) {
        return _impl.remove(reference);
    }

    /**
     * Get the childs of this path using the last sorting and filter used
     *
     * @param reference Reference to the path in cache.
     * @return An empty list if no cached file listing is available
     */
    public AttributedList<E> get(PathReference reference) {
        final AttributedList<E> childs = _impl.get(reference);
        if(null == childs) {
            log.warn("No cache for " + reference);
            return AttributedList.emptyList();
        }
        return childs;
    }

    /**
     * @param path       Absolute path
     * @param comparator Sorting comparator to apply the the file listing
     * @param filter     Path filter to apply. All files that don't match are moved to the
     *                   hidden attribute of the attributed list.
     * @return An empty list if no cached file listing is available
     * @throws ConcurrentModificationException
     *          If the caller is iterating of the cache himself
     *          and requests a new filter here.
     */
    public AttributedList<E> get(PathReference path, Comparator<E> comparator, PathFilter<E> filter) {
        AttributedList<E> childs = _impl.get(path);
        if(null == childs) {
            log.warn("No cache for " + path);
            return AttributedList.emptyList();
        }
        boolean needsSorting = !childs.attributes().get(AttributedList.COMPARATOR).equals(comparator);
        boolean needsFiltering = !childs.attributes().get(AttributedList.FILTER).equals(filter);
        if(needsSorting) {
            // Do not sort when the list has not been filtered yet
            if(!needsFiltering) {
                this.sort(childs, comparator);
            }
            // Saving last sorting comparator
            childs.attributes().put(AttributedList.COMPARATOR, comparator);
        }
        if(needsFiltering) {
            // Add previously hidden files to childs
            final List<E> hidden = childs.attributes().getHidden();
            childs.addAll(hidden);
            // Clear the previously set of hidden files
            hidden.clear();
            for(E child : childs) {
                if(!filter.accept(child)) {
                    //child not accepted by filter; add to cached hidden files
                    childs.attributes().addHidden(child);
                    //remove hidden file from current file listing
                    childs.remove(child);
                }
            }
            // Saving last filter
            childs.attributes().put(AttributedList.FILTER, filter);
            // Sort again because the list has changed
            this.sort(childs, comparator);
        }
        return childs;
    }

    /**
     * The CopyOnWriteArrayList iterator does not support remove but the sort implementation
     * makes use of it. Provide our own implementation here to circumvent.
     *
     * @param childs
     * @param comparator
     * @see java.util.Collections#sort(java.util.List, java.util.Comparator)
     * @see java.util.concurrent.CopyOnWriteArrayList#iterator()
     */
    private void sort(AttributedList<E> childs, Comparator comparator) {
        // Because AttributedList is a CopyOnWriteArrayList we cannot use Collections#sort
        AbstractPath[] sorted = childs.toArray(new AbstractPath[childs.size()]);
        Arrays.sort(sorted, (Comparator<AbstractPath>) comparator);
        for(int j = 0; j < sorted.length; j++) {
            childs.set(j, (E) sorted[j]);
        }
    }

    /**
     * @param reference Reference to the path in cache.
     * @param childs
     * @return
     */
    public AttributedList<E> put(PathReference reference, AttributedList<E> childs) {
        return _impl.put(reference, childs);
    }

    /**
     * Clear all cached directory listings
     */
    public void clear() {
        log.info("Clearing cache " + this.toString());
        _impl.clear();
    }
}
