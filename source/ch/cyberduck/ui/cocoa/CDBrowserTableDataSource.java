package ch.cyberduck.ui.cocoa;

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

import com.apple.cocoa.application.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import ch.cyberduck.core.Path;

/**
 * @version $Id$
 */
public abstract class CDBrowserTableDataSource {//implements NSTableView.DataSource {

    protected static final NSImage SYMLINK_ICON = NSImage.imageNamed("symlink.tiff");
    protected static final NSImage FOLDER_ICON = NSImage.imageNamed("folder16.tiff");
    protected static final NSImage NOT_FOUND_ICON = NSImage.imageNamed("notfound.tiff");

    protected List childs(Path path) {
        //get cached directory listing
        List l = path.list(controller.getEncoding(), //character encoding
                false, // do not refresh
                this.controller.getFileFilter(),
                false); // do not notify observers (important!)
        return l;
    }

    protected CDBrowserController controller;

    public CDBrowserTableDataSource(CDBrowserController controller) {
        this.controller = controller;
    }

    // ----------------------------------------------------------
    // Sorting
    // ----------------------------------------------------------

    public boolean isSortedAscending() {
        return this.sortAscending;
    }

    public NSTableColumn selectedColumn() {
        return this.selectedColumn;
    }

    private NSTableColumn selectedColumn = null;
    private boolean sortAscending = true;


    public void sort(NSTableColumn tableColumn, final boolean ascending) {
        if (controller.isMounted()) {
            final int higher = ascending ? 1 : -1;
            final int lower = ascending ? -1 : 1;
            List files = controller.workdir().getSession().cache().get(controller.workdir().getAbsolute());
            if (files != null) {
                if (tableColumn.identifier().equals("TYPE")) {
                    Collections.sort(files,
                            new Comparator() {
                                public int compare(Object o1, Object o2) {
                                    Path p1 = (Path) o1;
                                    Path p2 = (Path) o2;
                                    if (p1.attributes.isDirectory() && p2.attributes.isDirectory()) {
                                        return 0;
                                    }
                                    if (p1.attributes.isFile() && p2.attributes.isFile()) {
                                        return 0;
                                    }
                                    if (p1.attributes.isFile()) {
                                        return higher;
                                    }
                                    return lower;
                                }
                            });
                }
                else if (tableColumn.identifier().equals("FILENAME")) {
                    Collections.sort(files,
                            new Comparator() {
                                public int compare(Object o1, Object o2) {
                                    Path p1 = (Path) o1;
                                    Path p2 = (Path) o2;
                                    if (ascending) {
                                        return p1.getName().compareToIgnoreCase(p2.getName());
                                    }
                                    return -p1.getName().compareToIgnoreCase(p2.getName());
                                }
                            });
                }
                else if (tableColumn.identifier().equals("SIZE")) {
                    Collections.sort(files,
                            new Comparator() {
                                public int compare(Object o1, Object o2) {
                                    double p1 = ((Path) o1).attributes.getSize();
                                    double p2 = ((Path) o2).attributes.getSize();
                                    if (p1 > p2) {
                                        return higher;
                                    }
                                    else if (p1 < p2) {
                                        return lower;
                                    }
                                    return 0;
                                }
                            });
                }
                else if (tableColumn.identifier().equals("MODIFIED")) {
                    Collections.sort(files,
                            new Comparator() {
                                public int compare(Object o1, Object o2) {
                                    Path p1 = (Path) o1;
                                    Path p2 = (Path) o2;
                                    if (ascending) {
                                        return p1.attributes.getTimestamp().compareTo(p2.attributes.getTimestamp());
                                    }
                                    return -p1.attributes.getTimestamp().compareTo(p2.attributes.getTimestamp());
                                }
                            });
                }
                else if (tableColumn.identifier().equals("OWNER")) {
                    Collections.sort(files,
                            new Comparator() {
                                public int compare(Object o1, Object o2) {
                                    Path p1 = (Path) o1;
                                    Path p2 = (Path) o2;
                                    if (ascending) {
                                        return p1.attributes.getOwner().compareToIgnoreCase(p2.attributes.getOwner());
                                    }
                                    return -p1.attributes.getOwner().compareToIgnoreCase(p2.attributes.getOwner());
                                }
                            });
                }
                else if (tableColumn.identifier().equals("PERMISSIONS")) {
                    Collections.sort(files,
                            new Comparator() {
                                public int compare(Object o1, Object o2) {
                                    int p1 = Integer.parseInt(((Path) o1).attributes.getPermission().getOctalCode());
                                    int p2 = Integer.parseInt(((Path) o2).attributes.getPermission().getOctalCode());
                                    if (p1 > p2) {
                                        return higher;
                                    }
                                    else if (p1 < p2) {
                                        return lower;
                                    }
                                    return 0;
                                }
                            });
                }
            }
        }
    }

    // ----------------------------------------------------------
    // TableView/OutlineView Delegate methods
    // ----------------------------------------------------------


    public boolean selectionShouldChangeInTableView(NSTableView tableView) {
        return true;
    }

    public boolean selectionShouldChangeInOutlineView(NSTableView tableView) {
        return true;
    }


    public void outlineViewDidClickTableColumn(NSTableView tableView, NSTableColumn tableColumn) {
        this.tableViewDidClickTableColumn(tableView, tableColumn);
    }

    public void tableViewDidClickTableColumn(NSTableView tableView, NSTableColumn tableColumn) {
        List selectedRows = controller.getSelectedPaths();
        if (this.selectedColumn == tableColumn) {
            this.sortAscending = !this.sortAscending;
        }
        else {
            if (selectedColumn != null) {
                tableView.setIndicatorImage(null, selectedColumn);
            }
            this.selectedColumn = tableColumn;
        }
        tableView.setIndicatorImage(this.sortAscending ?
                NSImage.imageNamed("NSAscendingSortIndicator") :
                NSImage.imageNamed("NSDescendingSortIndicator"),
                tableColumn);
        tableView.deselectAll(null);
        this.sort(tableColumn, sortAscending);
        List childs = this.childs(controller.workdir());
        for (Iterator i = selectedRows.iterator(); i.hasNext();) {
            tableView.selectRow(childs.indexOf(i.next()), true);
        }
        tableView.reloadData();
    }

    /**
     * Returns true to permit aTableView to select the row at rowIndex, false to deny permission.
     * The delegate can implement this method to disallow selection of particular rows.
     */
    public boolean tableViewShouldSelectRow(NSTableView aTableView, int rowIndex) {
        return true;
    }

    public boolean outlineViewShouldSelectItem(NSOutlineView outlineView, Object item) {
        return true;
    }


    /**
     * Returns true to permit aTableView to edit the cell at rowIndex in aTableColumn, false to deny permission.
     * The delegate can implemen this method to disallow editing of specific cells.
     */
    public boolean tableViewShouldEditLocation(NSTableView view, NSTableColumn tableColumn, int row) {
        return false;
    }

    // ----------------------------------------------------------
    //	NSDraggingSource
    // ----------------------------------------------------------

    public boolean ignoreModifierKeysWhileDragging() {
        return false;
    }

    public int draggingSourceOperationMaskForLocal(boolean local) {
        return NSDraggingInfo.DragOperationMove | NSDraggingInfo.DragOperationCopy;
    }
}
