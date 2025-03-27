package org.freeone.javabean.tsinterface.setting;

import javax.swing.*;
import java.awt.*;

public class JavaBeanToTypescriptInterfaceComponent {
    private JPanel jPanel;
    private JCheckBox dateToStringCheckBox;
    private JCheckBox useJsonPropertyCheckBox;
    private JCheckBox allowFindClassInAllScope;
    private JCheckBox ignoreParentField;

    public JavaBeanToTypescriptInterfaceComponent() {
        createUI();
    }

    private void createUI() {
        jPanel = new JPanel(new GridLayout(4, 1));

        dateToStringCheckBox = new JCheckBox("java.util.Date to String");
        useJsonPropertyCheckBox = new JCheckBox("use @JsonProperty");
        allowFindClassInAllScope = new JCheckBox("allow find class in all scope");
        ignoreParentField = new JCheckBox("ignore parent fields");

        jPanel.add(dateToStringCheckBox);
        jPanel.add(useJsonPropertyCheckBox);
        jPanel.add(allowFindClassInAllScope);
        jPanel.add(ignoreParentField);
    }

    public JPanel getJPanel() {
        return jPanel;
    }

    public JCheckBox getDateToStringCheckBox() {
        return dateToStringCheckBox;
    }

    public JCheckBox getUseJsonPropertyCheckBox() {
        return useJsonPropertyCheckBox;
    }

    public JCheckBox getAllowFindClassInAllScope() {
        return allowFindClassInAllScope;
    }

    public JCheckBox getIgnoreParentField() {
        return ignoreParentField;
    }
}
