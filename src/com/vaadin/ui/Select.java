/*
@VaadinApache2LicenseForJavaFiles@
 */

package com.vaadin.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.data.Container;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.gwt.client.ui.VFilterSelectPaintable;

/**
 * <p>
 * A class representing a selection of items the user has selected in a UI. The
 * set of choices is presented as a set of {@link com.vaadin.data.Item}s in a
 * {@link com.vaadin.data.Container}.
 * </p>
 * 
 * <p>
 * A <code>Select</code> component may be in single- or multiselect mode.
 * Multiselect mode means that more than one item can be selected
 * simultaneously.
 * </p>
 * 
 * @author Vaadin Ltd.
 * @version
 * @VERSION@
 * @since 3.0
 */
@SuppressWarnings("serial")
@ClientWidget(VFilterSelectPaintable.class)
public class Select extends AbstractSelect implements AbstractSelect.Filtering,
        FieldEvents.BlurNotifier, FieldEvents.FocusNotifier {

    /**
     * Holds value of property pageLength. 0 disables paging.
     */
    protected int pageLength = 10;

    private int columns = 0;

    // Current page when the user is 'paging' trough options
    private int currentPage = -1;

    private int filteringMode = FILTERINGMODE_STARTSWITH;

    private String filterstring;
    private String prevfilterstring;

    /**
     * Number of options that pass the filter, excluding the null item if any.
     */
    private int filteredSize;

    /**
     * Cache of filtered options, used only by the in-memory filtering system.
     */
    private List<Object> filteredOptions;

    /**
     * Flag to indicate that request repaint is called by filter request only
     */
    private boolean optionRequest;

    /**
     * True if the container is being filtered temporarily and item set change
     * notifications should be suppressed.
     */
    private boolean filteringContainer;

    /**
     * Flag to indicate whether to scroll the selected item visible (select the
     * page on which it is) when opening the popup or not. Only applies to
     * single select mode.
     * 
     * This requires finding the index of the item, which can be expensive in
     * many large lazy loading containers.
     */
    private boolean scrollToSelectedItem = true;

    /* Constructors */

    /* Component methods */

    public Select() {
        super();
    }

    public Select(String caption, Collection<?> options) {
        super(caption, options);
    }

    public Select(String caption, Container dataSource) {
        super(caption, dataSource);
    }

    public Select(String caption) {
        super(caption);
    }

    /**
     * Paints the content of this component.
     * 
     * @param target
     *            the Paint Event.
     * @throws PaintException
     *             if the paint operation failed.
     */
    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        if (isMultiSelect()) {
            // background compatibility hack. This object shouldn't be used for
            // multiselect lists anymore (ListSelect instead). This fallbacks to
            // a simpler paint method in super class.
            super.paintContent(target);
            // Fix for #4553
            target.addAttribute("type", "legacy-multi");
            return;
        }

        // clear caption change listeners
        getCaptionChangeListener().clear();

        // The tab ordering number
        if (getTabIndex() != 0) {
            target.addAttribute("tabindex", getTabIndex());
        }

        // If the field is modified, but not committed, set modified attribute
        if (isModified()) {
            target.addAttribute("modified", true);
        }

        // Adds the required attribute
        if (!isReadOnly() && isRequired()) {
            target.addAttribute("required", true);
        }

        if (isNewItemsAllowed()) {
            target.addAttribute("allownewitem", true);
        }

        boolean needNullSelectOption = false;
        if (isNullSelectionAllowed()) {
            target.addAttribute("nullselect", true);
            needNullSelectOption = (getNullSelectionItemId() == null);
            if (!needNullSelectOption) {
                target.addAttribute("nullselectitem", true);
            }
        }

        // Constructs selected keys array
        String[] selectedKeys;
        if (isMultiSelect()) {
            selectedKeys = new String[((Set<?>) getValue()).size()];
        } else {
            selectedKeys = new String[(getValue() == null
                    && getNullSelectionItemId() == null ? 0 : 1)];
        }

        target.addAttribute("pagelength", pageLength);

        target.addAttribute("filteringmode", getFilteringMode());

        // Paints the options and create array of selected id keys
        int keyIndex = 0;

        target.startTag("options");

        if (currentPage < 0) {
            optionRequest = false;
            currentPage = 0;
            filterstring = "";
        }

        boolean nullFilteredOut = filterstring != null
                && !"".equals(filterstring)
                && filteringMode != FILTERINGMODE_OFF;
        // null option is needed and not filtered out, even if not on current
        // page
        boolean nullOptionVisible = needNullSelectOption && !nullFilteredOut;

        // first try if using container filters is possible
        List<?> options = getOptionsWithFilter(nullOptionVisible);
        if (null == options) {
            // not able to use container filters, perform explicit in-memory
            // filtering
            options = getFilteredOptions();
            filteredSize = options.size();
            options = sanitetizeList(options, nullOptionVisible);
        }

        final boolean paintNullSelection = needNullSelectOption
                && currentPage == 0 && !nullFilteredOut;

        if (paintNullSelection) {
            target.startTag("so");
            target.addAttribute("caption", "");
            target.addAttribute("key", "");
            target.endTag("so");
        }

        final Iterator<?> i = options.iterator();
        // Paints the available selection options from data source

        while (i.hasNext()) {

            final Object id = i.next();

            if (!isNullSelectionAllowed() && id != null
                    && id.equals(getNullSelectionItemId()) && !isSelected(id)) {
                continue;
            }

            // Gets the option attribute values
            final String key = itemIdMapper.key(id);
            final String caption = getItemCaption(id);
            final Resource icon = getItemIcon(id);
            getCaptionChangeListener().addNotifierForItem(id);

            // Paints the option
            target.startTag("so");
            if (icon != null) {
                target.addAttribute("icon", icon);
            }
            target.addAttribute("caption", caption);
            if (id != null && id.equals(getNullSelectionItemId())) {
                target.addAttribute("nullselection", true);
            }
            target.addAttribute("key", key);
            if (isSelected(id) && keyIndex < selectedKeys.length) {
                target.addAttribute("selected", true);
                selectedKeys[keyIndex++] = key;
            }
            target.endTag("so");
        }
        target.endTag("options");

        target.addAttribute("totalitems", size()
                + (needNullSelectOption ? 1 : 0));
        if (filteredSize > 0 || nullOptionVisible) {
            target.addAttribute("totalMatches", filteredSize
                    + (nullOptionVisible ? 1 : 0));
        }

        // Paint variables
        target.addVariable(this, "selected", selectedKeys);
        if (isNewItemsAllowed()) {
            target.addVariable(this, "newitem", "");
        }

        target.addVariable(this, "filter", filterstring);
        target.addVariable(this, "page", currentPage);

        currentPage = -1; // current page is always set by client

        optionRequest = true;

        // Hide the error indicator if needed
        if (shouldHideErrors()) {
            target.addAttribute("hideErrors", true);
        }
    }

    /**
     * Returns the filtered options for the current page using a container
     * filter.
     * 
     * As a size effect, {@link #filteredSize} is set to the total number of
     * items passing the filter.
     * 
     * The current container must be {@link Filterable} and {@link Indexed}, and
     * the filtering mode must be suitable for container filtering (tested with
     * {@link #canUseContainerFilter()}).
     * 
     * Use {@link #getFilteredOptions()} and
     * {@link #sanitetizeList(List, boolean)} if this is not the case.
     * 
     * @param needNullSelectOption
     * @return filtered list of options (may be empty) or null if cannot use
     *         container filters
     */
    protected List<?> getOptionsWithFilter(boolean needNullSelectOption) {
        Container container = getContainerDataSource();

        if (pageLength == 0) {
            // no paging: return all items
            filteredSize = container.size();
            return new ArrayList<Object>(container.getItemIds());
        }

        if (!(container instanceof Filterable)
                || !(container instanceof Indexed)
                || getItemCaptionMode() != ITEM_CAPTION_MODE_PROPERTY) {
            return null;
        }

        Filterable filterable = (Filterable) container;

        Filter filter = buildFilter(filterstring, filteringMode);

        // adding and removing filters leads to extraneous item set
        // change events from the underlying container, but the ComboBox does
        // not process or propagate them based on the flag filteringContainer
        if (filter != null) {
            filteringContainer = true;
            filterable.addContainerFilter(filter);
        }

        Indexed indexed = (Indexed) container;

        int indexToEnsureInView = -1;

        // if not an option request (item list when user changes page), go
        // to page with the selected item after filtering if accepted by
        // filter
        Object selection = getValue();
        if (isScrollToSelectedItem() && !optionRequest && !isMultiSelect()
                && selection != null) {
            // ensure proper page
            indexToEnsureInView = indexed.indexOfId(selection);
        }

        filteredSize = container.size();
        currentPage = adjustCurrentPage(currentPage, needNullSelectOption,
                indexToEnsureInView, filteredSize);
        int first = getFirstItemIndexOnCurrentPage(needNullSelectOption,
                filteredSize);
        int last = getLastItemIndexOnCurrentPage(needNullSelectOption,
                filteredSize, first);

        List<Object> options = new ArrayList<Object>();
        for (int i = first; i <= last && i < filteredSize; ++i) {
            options.add(indexed.getIdByIndex(i));
        }

        // to the outside, filtering should not be visible
        if (filter != null) {
            filterable.removeContainerFilter(filter);
            filteringContainer = false;
        }

        return options;
    }

    /**
     * Constructs a filter instance to use when using a Filterable container in
     * the <code>ITEM_CAPTION_MODE_PROPERTY</code> mode.
     * 
     * Note that the client side implementation expects the filter string to
     * apply to the item caption string it sees, so changing the behavior of
     * this method can cause problems.
     * 
     * @param filterString
     * @param filteringMode
     * @return
     */
    protected Filter buildFilter(String filterString, int filteringMode) {
        Filter filter = null;

        if (null != filterString && !"".equals(filterString)) {
            switch (filteringMode) {
            case FILTERINGMODE_OFF:
                break;
            case FILTERINGMODE_STARTSWITH:
                filter = new SimpleStringFilter(getItemCaptionPropertyId(),
                        filterString, true, true);
                break;
            case FILTERINGMODE_CONTAINS:
                filter = new SimpleStringFilter(getItemCaptionPropertyId(),
                        filterString, true, false);
                break;
            }
        }
        return filter;
    }

    @Override
    public void containerItemSetChange(Container.ItemSetChangeEvent event) {
        if (!filteringContainer) {
            super.containerItemSetChange(event);
        }
    }

    /**
     * Makes correct sublist of given list of options.
     * 
     * If paint is not an option request (affected by page or filter change),
     * page will be the one where possible selection exists.
     * 
     * Detects proper first and last item in list to return right page of
     * options. Also, if the current page is beyond the end of the list, it will
     * be adjusted.
     * 
     * @param options
     * @param needNullSelectOption
     *            flag to indicate if nullselect option needs to be taken into
     *            consideration
     */
    private List<?> sanitetizeList(List<?> options, boolean needNullSelectOption) {

        if (pageLength != 0 && options.size() > pageLength) {

            int indexToEnsureInView = -1;

            // if not an option request (item list when user changes page), go
            // to page with the selected item after filtering if accepted by
            // filter
            Object selection = getValue();
            if (isScrollToSelectedItem() && !optionRequest && !isMultiSelect()
                    && selection != null) {
                // ensure proper page
                indexToEnsureInView = options.indexOf(selection);
            }

            int size = options.size();
            currentPage = adjustCurrentPage(currentPage, needNullSelectOption,
                    indexToEnsureInView, size);
            int first = getFirstItemIndexOnCurrentPage(needNullSelectOption,
                    size);
            int last = getLastItemIndexOnCurrentPage(needNullSelectOption,
                    size, first);
            return options.subList(first, last + 1);
        } else {
            return options;
        }
    }

    /**
     * Returns the index of the first item on the current page. The index is to
     * the underlying (possibly filtered) contents. The null item, if any, does
     * not have an index but takes up a slot on the first page.
     * 
     * @param needNullSelectOption
     *            true if a null option should be shown before any other options
     *            (takes up the first slot on the first page, not counted in
     *            index)
     * @param size
     *            number of items after filtering (not including the null item,
     *            if any)
     * @return first item to show on the UI (index to the filtered list of
     *         options, not taking the null item into consideration if any)
     */
    private int getFirstItemIndexOnCurrentPage(boolean needNullSelectOption,
            int size) {
        // Not all options are visible, find out which ones are on the
        // current "page".
        int first = currentPage * pageLength;
        if (needNullSelectOption && currentPage > 0) {
            first--;
        }
        return first;
    }

    /**
     * Returns the index of the last item on the current page. The index is to
     * the underlying (possibly filtered) contents. If needNullSelectOption is
     * true, the null item takes up the first slot on the first page,
     * effectively reducing the first page size by one.
     * 
     * @param needNullSelectOption
     *            true if a null option should be shown before any other options
     *            (takes up the first slot on the first page, not counted in
     *            index)
     * @param size
     *            number of items after filtering (not including the null item,
     *            if any)
     * @param first
     *            index in the filtered view of the first item of the page
     * @return index in the filtered view of the last item on the page
     */
    private int getLastItemIndexOnCurrentPage(boolean needNullSelectOption,
            int size, int first) {
        // page length usable for non-null items
        int effectivePageLength = pageLength
                - (needNullSelectOption && (currentPage == 0) ? 1 : 0);
        return Math.min(size - 1, first + effectivePageLength - 1);
    }

    /**
     * Adjusts the index of the current page if necessary: make sure the current
     * page is not after the end of the contents, and optionally go to the page
     * containg a specific item. There are no side effects but the adjusted page
     * index is returned.
     * 
     * @param page
     *            page number to use as the starting point
     * @param needNullSelectOption
     *            true if a null option should be shown before any other options
     *            (takes up the first slot on the first page, not counted in
     *            index)
     * @param indexToEnsureInView
     *            index of an item that should be included on the page (in the
     *            data set, not counting the null item if any), -1 for none
     * @param size
     *            number of items after filtering (not including the null item,
     *            if any)
     */
    private int adjustCurrentPage(int page, boolean needNullSelectOption,
            int indexToEnsureInView, int size) {
        if (indexToEnsureInView != -1) {
            int newPage = (indexToEnsureInView + (needNullSelectOption ? 1 : 0))
                    / pageLength;
            page = newPage;
        }
        // adjust the current page if beyond the end of the list
        if (page * pageLength > size) {
            page = (size + (needNullSelectOption ? 1 : 0)) / pageLength;
        }
        return page;
    }

    /**
     * Filters the options in memory and returns the full filtered list.
     * 
     * This can be less efficient than using container filters, so use
     * {@link #getOptionsWithFilter(boolean)} if possible (filterable container
     * and suitable item caption mode etc.).
     * 
     * @return
     */
    protected List<?> getFilteredOptions() {
        if (null == filterstring || "".equals(filterstring)
                || FILTERINGMODE_OFF == filteringMode) {
            prevfilterstring = null;
            filteredOptions = new LinkedList<Object>(getItemIds());
            return filteredOptions;
        }

        if (filterstring.equals(prevfilterstring)) {
            return filteredOptions;
        }

        Collection<?> items;
        if (prevfilterstring != null
                && filterstring.startsWith(prevfilterstring)) {
            items = filteredOptions;
        } else {
            items = getItemIds();
        }
        prevfilterstring = filterstring;

        filteredOptions = new LinkedList<Object>();
        for (final Iterator<?> it = items.iterator(); it.hasNext();) {
            final Object itemId = it.next();
            String caption = getItemCaption(itemId);
            if (caption == null || caption.equals("")) {
                continue;
            } else {
                caption = caption.toLowerCase();
            }
            switch (filteringMode) {
            case FILTERINGMODE_CONTAINS:
                if (caption.indexOf(filterstring) > -1) {
                    filteredOptions.add(itemId);
                }
                break;
            case FILTERINGMODE_STARTSWITH:
            default:
                if (caption.startsWith(filterstring)) {
                    filteredOptions.add(itemId);
                }
                break;
            }
        }

        return filteredOptions;
    }

    /**
     * Invoked when the value of a variable has changed.
     * 
     * @see com.vaadin.ui.AbstractComponent#changeVariables(java.lang.Object,
     *      java.util.Map)
     */
    @Override
    public void changeVariables(Object source, Map<String, Object> variables) {
        // Not calling super.changeVariables due the history of select
        // component hierarchy

        // Selection change
        if (variables.containsKey("selected")) {
            final String[] ka = (String[]) variables.get("selected");

            if (isMultiSelect()) {
                // Multiselect mode

                // TODO Optimize by adding repaintNotNeeded whan applicaple

                // Converts the key-array to id-set
                final LinkedList<Object> s = new LinkedList<Object>();
                for (int i = 0; i < ka.length; i++) {
                    final Object id = itemIdMapper.get(ka[i]);
                    if (id != null && containsId(id)) {
                        s.add(id);
                    }
                }

                // Limits the deselection to the set of visible items
                // (non-visible items can not be deselected)
                final Collection<?> visible = getVisibleItemIds();
                if (visible != null) {
                    @SuppressWarnings("unchecked")
                    Set<Object> newsel = (Set<Object>) getValue();
                    if (newsel == null) {
                        newsel = new HashSet<Object>();
                    } else {
                        newsel = new HashSet<Object>(newsel);
                    }
                    newsel.removeAll(visible);
                    newsel.addAll(s);
                    setValue(newsel, true);
                }
            } else {
                // Single select mode
                if (ka.length == 0) {

                    // Allows deselection only if the deselected item is visible
                    final Object current = getValue();
                    final Collection<?> visible = getVisibleItemIds();
                    if (visible != null && visible.contains(current)) {
                        setValue(null, true);
                    }
                } else {
                    final Object id = itemIdMapper.get(ka[0]);
                    if (id != null && id.equals(getNullSelectionItemId())) {
                        setValue(null, true);
                    } else {
                        setValue(id, true);
                    }
                }
            }
        }

        String newFilter;
        if ((newFilter = (String) variables.get("filter")) != null) {
            // this is a filter request
            currentPage = ((Integer) variables.get("page")).intValue();
            filterstring = newFilter;
            if (filterstring != null) {
                filterstring = filterstring.toLowerCase();
            }
            optionRepaint();
        } else if (isNewItemsAllowed()) {
            // New option entered (and it is allowed)
            final String newitem = (String) variables.get("newitem");
            if (newitem != null && newitem.length() > 0) {
                getNewItemHandler().addNewItem(newitem);
                // rebuild list
                filterstring = null;
                prevfilterstring = null;
            }
        }

        if (variables.containsKey(FocusEvent.EVENT_ID)) {
            fireEvent(new FocusEvent(this));
        }
        if (variables.containsKey(BlurEvent.EVENT_ID)) {
            fireEvent(new BlurEvent(this));
        }

    }

    @Override
    public void requestRepaint() {
        super.requestRepaint();
        optionRequest = false;
        prevfilterstring = filterstring;
        filterstring = null;
    }

    private void optionRepaint() {
        super.requestRepaint();
    }

    public void setFilteringMode(int filteringMode) {
        this.filteringMode = filteringMode;
    }

    public int getFilteringMode() {
        return filteringMode;
    }

    /**
     * Note, one should use more generic setWidth(String) method instead of
     * this. This now days actually converts columns to width with em css unit.
     * 
     * Sets the number of columns in the editor. If the number of columns is set
     * 0, the actual number of displayed columns is determined implicitly by the
     * adapter.
     * 
     * @deprecated
     * 
     * @param columns
     *            the number of columns to set.
     */
    @Deprecated
    public void setColumns(int columns) {
        if (columns < 0) {
            columns = 0;
        }
        if (this.columns != columns) {
            this.columns = columns;
            setWidth(columns, Select.UNITS_EM);
            requestRepaint();
        }
    }

    /**
     * @deprecated see setter function
     * @return
     */
    @Deprecated
    public int getColumns() {
        return columns;
    }

    public void addListener(BlurListener listener) {
        addListener(BlurEvent.EVENT_ID, BlurEvent.class, listener,
                BlurListener.blurMethod);
    }

    public void removeListener(BlurListener listener) {
        removeListener(BlurEvent.EVENT_ID, BlurEvent.class, listener);
    }

    public void addListener(FocusListener listener) {
        addListener(FocusEvent.EVENT_ID, FocusEvent.class, listener,
                FocusListener.focusMethod);
    }

    public void removeListener(FocusListener listener) {
        removeListener(FocusEvent.EVENT_ID, FocusEvent.class, listener);

    }

    /**
     * @deprecated use {@link ListSelect}, {@link OptionGroup} or
     *             {@link TwinColSelect} instead
     * @see com.vaadin.ui.AbstractSelect#setMultiSelect(boolean)
     */
    @Deprecated
    @Override
    public void setMultiSelect(boolean multiSelect) {
        super.setMultiSelect(multiSelect);
    }

    /**
     * @deprecated use {@link ListSelect}, {@link OptionGroup} or
     *             {@link TwinColSelect} instead
     * 
     * @see com.vaadin.ui.AbstractSelect#isMultiSelect()
     */
    @Deprecated
    @Override
    public boolean isMultiSelect() {
        return super.isMultiSelect();
    }

    /**
     * Sets whether to scroll the selected item visible (directly open the page
     * on which it is) when opening the combo box popup or not. Only applies to
     * single select mode.
     * 
     * This requires finding the index of the item, which can be expensive in
     * many large lazy loading containers.
     * 
     * @param scrollToSelectedItem
     *            true to find the page with the selected item when opening the
     *            selection popup
     */
    public void setScrollToSelectedItem(boolean scrollToSelectedItem) {
        this.scrollToSelectedItem = scrollToSelectedItem;
    }

    /**
     * Returns true if the select should find the page with the selected item
     * when opening the popup (single select combo box only).
     * 
     * @see #setScrollToSelectedItem(boolean)
     * 
     * @return true if the page with the selected item will be shown when
     *         opening the popup
     */
    public boolean isScrollToSelectedItem() {
        return scrollToSelectedItem;
    }

}
