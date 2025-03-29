package org.freeone.javabean.tsinterface.setting;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 主要配置文件
 * Provides controller functionality for application settings.
 */
final class JavaBeanToTypescriptInterfaceSettingsConfigurable implements Configurable {

    private JavaBeanToTypescriptInterfaceComponent mySettingsComponent;
    private JPanel mainPanel;
    private JavaBeanToTypescriptInterfaceComponent component;

    // A default constructor with no arguments is required because this
    // implementation
    // is registered in an applicationConfigurable EP

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Java DTO to TypeScript Interface";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        component = new JavaBeanToTypescriptInterfaceComponent();

        // 添加標題和說明面板
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel titleLabel = new JLabel("Java Bean 轉 TypeScript 介面設定");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        JTextArea descriptionArea = new JTextArea("設定Java類別轉換為TypeScript介面的各項參數。適當配置可以改善生成的結果。");
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setEditable(false);
        descriptionArea.setBackground(new Color(0, 0, 0, 0));
        descriptionArea.setForeground(new Color(100, 100, 100));
        descriptionArea.setFont(new Font(descriptionArea.getFont().getName(), Font.PLAIN, 12));
        descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        headerPanel.add(titleLabel);
        headerPanel.add(descriptionArea);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(component.getMainPanel(), BorderLayout.CENTER);
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        return component.isDateToString() != settings.isEnableDataToString()
                || component.isUseAnnotationJsonProperty() != settings.isUseAnnotationJsonProperty()
                || component.isAllowFindClassInAllScope() != settings.isAllowFindClassInAllScope()
                || component.isIgnoreParentField() != settings.isIgnoreParentField()
                || component.isAddOptionalMarkToAllFields() != settings.isAddOptionalMarkToAllFields()
                || component.isIgnoreSerialVersionUID() != settings.isIgnoreSerialVersionUID()
                || component.isOnlyProcessGenericDto() != settings.isOnlyProcessGenericDto()
                || component.isUseTransactionCodePrefix() != settings.isUseTransactionCodePrefix()
                || !component.getRequestSuffix().equals(settings.getRequestSuffix())
                || !component.getResponseSuffix().equals(settings.getResponseSuffix())
                || !compare(component.getRequestDtoSuffixes(), settings.getRequestDtoSuffixes())
                || !compare(component.getResponseDtoSuffixes(), settings.getResponseDtoSuffixes());
    }

    @Override
    public void apply() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        settings.setEnableDataToString(component.isDateToString());
        settings.setUseAnnotationJsonProperty(component.isUseAnnotationJsonProperty());
        settings.setAllowFindClassInAllScope(component.isAllowFindClassInAllScope());
        settings.setIgnoreParentField(component.isIgnoreParentField());
        settings.setAddOptionalMarkToAllFields(component.isAddOptionalMarkToAllFields());
        settings.setIgnoreSerialVersionUID(component.isIgnoreSerialVersionUID());
        settings.setOnlyProcessGenericDto(component.isOnlyProcessGenericDto());
        settings.setUseTransactionCodePrefix(component.isUseTransactionCodePrefix());
        settings.setRequestSuffix(component.getRequestSuffix());
        settings.setResponseSuffix(component.getResponseSuffix());

        settings.getRequestDtoSuffixes().clear();
        List<String> requestDtoSuffixes = component.getRequestDtoSuffixes();
        for (String suffix : requestDtoSuffixes) {
            settings.getRequestDtoSuffixes().add(suffix);
        }

        settings.getResponseDtoSuffixes().clear();
        List<String> responseDtoSuffixes = component.getResponseDtoSuffixes();
        for (String suffix : responseDtoSuffixes) {
            settings.getResponseDtoSuffixes().add(suffix);
        }
    }

    @Override
    public void reset() {
        JavaBeanToTypescriptInterfaceSettingsState settings = JavaBeanToTypescriptInterfaceSettingsState.getInstance();
        component.setDateToString(settings.isEnableDataToString());
        component.setUseAnnotationJsonProperty(settings.isUseAnnotationJsonProperty());
        component.setAllowFindClassInAllScope(settings.isAllowFindClassInAllScope());
        component.setIgnoreParentField(settings.isIgnoreParentField());
        component.setAddOptionalMarkToAllFields(settings.isAddOptionalMarkToAllFields());
        component.setIgnoreSerialVersionUID(settings.isIgnoreSerialVersionUID());
        component.setOnlyProcessGenericDto(settings.isOnlyProcessGenericDto());
        component.setUseTransactionCodePrefix(settings.isUseTransactionCodePrefix());
        component.setRequestSuffix(settings.getRequestSuffix());
        component.setResponseSuffix(settings.getResponseSuffix());

        component.getRequestDtoSuffixes().clear();
        for (String suffix : settings.getRequestDtoSuffixes()) {
            component.getRequestDtoSuffixes().add(suffix);
        }

        component.getResponseDtoSuffixes().clear();
        for (String suffix : settings.getResponseDtoSuffixes()) {
            component.getResponseDtoSuffixes().add(suffix);
        }
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }

    private boolean compare(List<String> list1, List<String> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        for (String suffix : list1) {
            if (!list2.contains(suffix)) {
                return false;
            }
        }
        return true;
    }

}