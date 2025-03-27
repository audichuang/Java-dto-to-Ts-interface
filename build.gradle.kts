plugins {
    id("org.jetbrains.intellij") version "1.17.4"
    id("io.freefair.lombok") version "8.4"
    java
}

group = "org.freeone.javabean.tsinterface"
version = "0.2.9"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
maven { url = uri("https://repo.jetbrains.team/intellij-repository/releases") }
    maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
    
    // maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

// 配置源碼目錄
sourceSets {
    main {
        java {
            srcDirs("src")
        }
        resources {
            srcDirs("resources")
        }
    }
}

intellij {
    version.set("2024.1")
    type.set("IC")
    plugins.set(listOf("com.intellij.java"))
    updateSinceUntilBuild.set(false)
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks {
    // 配置 Java 編譯選項
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.isFailOnError = false
        options.compilerArgs.add("-Xlint:deprecation")
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("243.*")
        pluginDescription.set("將 Java DTO 轉換為 TypeScript 接口定義")
        changeNotes.set("""
            <ul>
                <li>0.2.8: 改進 Alt+Enter 意圖操作，提供保存、複製和編輯選項</li>
            </ul>
        """)
        pluginId.set("org.freeone.javadto.tsinterface")
    }
} 