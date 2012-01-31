/* 
@VaadinApache2LicenseForJavaFiles@
 */

package com.vaadin.terminal.gwt.client;

import java.util.Set;

import com.google.gwt.user.client.ui.Widget;

/**
 * @deprecated To be removed before 7.0.0
 */
@Deprecated
public interface Container {

    /**
     * Replace child of this layout with another component.
     * 
     * Each layout must be able to switch children. To to this, one must just
     * give references to a current and new child.
     * 
     * @param oldComponent
     *            Child to be replaced
     * @param newComponent
     *            Child that replaces the oldComponent
     */
    void replaceChildComponent(Widget oldComponent, Widget newComponent);

    /**
     * Is a given component child of this layout.
     * 
     * @param component
     *            Component to test.
     * @return true iff component is a child of this layout.
     */
    boolean hasChildComponent(Widget component);

    /**
     * Called when a child components size has been updated in the rendering
     * phase.
     * 
     * @param children
     *            Set of child widgets whose size have changed
     * @return true if the size of the Container remains the same, false if the
     *         event need to be propagated to the Containers parent
     */
    boolean requestLayout(Set<Widget> children);

    /**
     * Returns the size currently allocated for the child component.
     * 
     * @param child
     * @return
     */
    RenderSpace getAllocatedSpace(Widget child);

}
