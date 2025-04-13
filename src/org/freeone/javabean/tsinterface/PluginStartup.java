//package org.freeone.javabean.tsinterface;
//
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.project.ProjectManager;
//import com.intellij.openapi.startup.StartupActivity;
//import org.freeone.javabean.tsinterface.setting.JavaBeanToTypescriptInterfaceProjectSettings;
//import org.jetbrains.annotations.NotNull;
//
///**
// * 插件啟動時執行的初始化活動
// * 確保所有項目的設定被正確初始化
// */
//public class PluginStartup implements StartupActivity {
//    @Override
//    public void runActivity(@NotNull Project project) {
//        System.out.println("============================");
//        System.out.println("Java Bean To TS Interface 插件啟動");
//        System.out.println("============================");
//
//        // 初始化項目設定
//        JavaBeanToTypescriptInterfaceProjectSettings.initializeOnStartup(project);
//
//        // 遍歷所有已打開的項目確保初始化
//        try {
//            Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
//            if (openProjects != null && openProjects.length > 1) {
//                System.out.println("檢測到多個開放項目，確保所有項目設定已初始化");
//                for (Project p : openProjects) {
//                    if (p != null && !p.equals(project)) {
//                        JavaBeanToTypescriptInterfaceProjectSettings.initializeOnStartup(p);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            System.out.println("初始化所有項目設定時發生異常: " + e.getMessage());
//        }
//
//        System.out.println("插件啟動初始化完成");
//    }
//}