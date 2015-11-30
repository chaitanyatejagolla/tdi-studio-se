// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.component.core.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.component.core.constants.IComponentConstants;
import org.talend.component.core.constants.IElementParameterEventProperties;
import org.talend.component.core.constants.IGenericConstants;
import org.talend.components.api.NamedThing;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.api.properties.PresentationItem;
import org.talend.components.api.properties.presentation.Form;
import org.talend.components.api.properties.presentation.Widget;
import org.talend.components.api.schema.SchemaElement;
import org.talend.components.api.service.ComponentService;
import org.talend.core.model.process.IElement;
import org.talend.designer.core.model.components.ElementParameter;

/**
 * created by ycbai on 2015年9月24日 Detailled comment
 *
 */
public class GenericElementParameter extends ElementParameter {

    private ComponentProperties componentProperties;

    private Widget widget;

    private ComponentService componentService;

    private List<?> possibleValues;

    private boolean supportContext;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private boolean isFirstCall;

    public GenericElementParameter(IElement element, ComponentProperties componentProperties, Widget widget,
            ComponentService componentService) {
        super(element);
        this.componentProperties = componentProperties;
        this.widget = widget;
        this.componentService = componentService;
        isFirstCall = true;

        NamedThing[] widgetProperties = widget.getProperties();
        NamedThing widgetProperty = widgetProperties[0];
        setName(widgetProperty.getName());
        setDisplayName(widgetProperty.getDisplayName());
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    @Override
    public void setValue(Object o) {
        super.setValue(o);
        if (!isFirstCall) {
            updateProperty(o);
            boolean calledValidate = callValidate();
            if (calledValidate) {
                fireValidateStatusEvent();
            }
            boolean calledAfter = callAfter();
            if (calledAfter) {
                fireValueChangedEvent();
            }
        }
        isFirstCall = false;
    }

    private void updateProperty(Object newValue) {
        if (componentProperties == null) {
            return;
        }
        NamedThing[] widgetProperties = widget.getProperties();
        NamedThing widgetProperty = widgetProperties[0];
        if (widgetProperty instanceof SchemaElement) {
            SchemaElement se = (SchemaElement) widgetProperty;
            SchemaElement originalElement = componentProperties.getProperty(se.getName());
            if (originalElement != null) {
                se = originalElement;
            }
            Object oldValue = componentProperties.getValue(se);
            if (newValue != null && !newValue.equals(oldValue)) {
                componentProperties.setValue(se, newValue);
                fireConnectionPropertyChangedEvent(newValue);
            }
        } else if (widgetProperty instanceof PresentationItem) {
            PresentationItem pi = (PresentationItem) widgetProperty;
            Form formtoShow = pi.getFormtoShow();
            if (formtoShow != null) {
                fireShowDialogEvent(componentProperties.getForm(formtoShow.getName()));
            }
        }
    }

    private boolean hasPropertyChangeListener() {
        return pcs.getPropertyChangeListeners().length != 0;
    }

    private void fireValidateStatusEvent() {
        if (hasPropertyChangeListener()) {
            this.pcs.firePropertyChange(IElementParameterEventProperties.EVENT_VALIDATE_RESULT_UPDATE, null,
                    componentProperties.getValidationResult());
        }
    }

    private void fireValueChangedEvent() {
        if (hasPropertyChangeListener()) {
            List<Form> forms = componentProperties.getForms();
            for (Form form : forms) {
                if (form.isRefreshUI()) {
                    this.pcs.firePropertyChange(IElementParameterEventProperties.EVENT_PROPERTY_VALUE_CHANGED, null, null);
                    return;
                }
            }
        }
    }

    private void fireConnectionPropertyChangedEvent(Object newPropertyName) {
        if (IGenericConstants.NAME_PROPERTY.equalsIgnoreCase(getName()) && hasPropertyChangeListener()) {
            this.pcs.firePropertyChange(IElementParameterEventProperties.EVENT_PROPERTY_NAME_CHANGED, null, newPropertyName);
        }
    }

    private void fireShowDialogEvent(Form formToDisplay) {
        if (hasPropertyChangeListener()) {
            this.pcs.firePropertyChange(IElementParameterEventProperties.EVENT_SHOW_DIALOG, null, formToDisplay);
        }
    }

    public boolean callBeforePresent() {
        if (widget.isCallBeforePresent()) {
            try {
                componentProperties = componentService.beforePropertyPresent(getName(), componentProperties);
                return true;
            } catch (Throwable e) {
                ExceptionHandler.process(e);
            }
        }
        return false;
    }

    public boolean callBeforeActivate() {
        if (widget.isCallBeforeActivate()) {
            try {
                componentProperties = componentService.beforePropertyActivate(getName(), componentProperties);
                return true;
            } catch (Throwable e) {
                ExceptionHandler.process(e);
            }
        }
        return false;
    }

    private boolean callValidate() {
        if (widget.isCallValidate()) {
            if (widget.isLongRunning()) {
                return new RunWithProgress(widget.getProperties()[0].getDisplayName()) {

                    @Override
                    protected void toDo() throws Throwable {
                        componentProperties = componentService.validateProperty(getParameterName(), componentProperties);
                    };

                }.run();
            } else {
                try {
                    componentProperties = componentService.validateProperty(getParameterName(), componentProperties);
                    return true;
                } catch (Throwable e) {
                    ExceptionHandler.process(e);
                }
            }
        }
        return false;
    }

    private boolean callAfter() {
        if (widget.isCallAfter()) {
            try {
                componentProperties = componentService.afterProperty(getParameterName(), componentProperties);
                return true;
            } catch (Throwable e) {
                ExceptionHandler.process(e);
            }
        }
        return false;
    }

    private String getParameterName() {
        String paramName = getName();
        if (paramName.indexOf(IComponentConstants.UNDERLINE_SEPARATOR) != -1) {
            paramName = paramName.substring(paramName.lastIndexOf(IComponentConstants.UNDERLINE_SEPARATOR) + 1);
        }
        return paramName;
    }

    abstract class RunWithProgress {

        private String taskName;

        public RunWithProgress(String taskName) {
            this.taskName = taskName;
        }

        public boolean run() {
            final AtomicBoolean result = new AtomicBoolean();
            IRunnableWithProgress runnableWithProgress = new IRunnableWithProgress() {

                @Override
                public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(taskName, IProgressMonitor.UNKNOWN);
                    try {
                        toDo();
                    } catch (Throwable e) {
                        result.set(false);
                        throw new InvocationTargetException(e);
                    }
                    result.set(true);
                }
            };
            ProgressMonitorDialog dialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell());
            try {
                dialog.run(true, true, runnableWithProgress);
            } catch (Exception e) {
                result.set(false);
                ExceptionHandler.process(e);
            }
            return result.get();
        }

        protected abstract void toDo() throws Throwable;

    }

    public List<?> getPossibleValues() {
        return this.possibleValues;
    }

    public void setPossibleValues(List<?> possibleValues) {
        this.possibleValues = possibleValues;
    }

    public Widget getWidget() {
        return this.widget;
    }

    public ComponentProperties getComponentProperties() {
        return this.componentProperties;
    }

    public void setComponentProperties(ComponentProperties componentProperties) {
        this.componentProperties = componentProperties;
    }

    public boolean isSupportContext() {
        return this.supportContext;
    }

    public void setSupportContext(boolean supportContext) {
        this.supportContext = supportContext;
    }

    public ComponentService getComponentService() {
        return this.componentService;
    }

    public void setComponentService(ComponentService componentService) {
        this.componentService = componentService;
    }

}