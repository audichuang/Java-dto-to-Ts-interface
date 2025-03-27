plugins {
    id("org.jetbrains.intellij") version "1.17.4"
    id("io.freefair.lombok") version "8.4"
}

group = "org.freeone.javabean.tsinterface"
version = "0.2.7"

repositories {
    mavenCentral()
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
    version.set("2024.3")
    type.set("IC")
    plugins.set(listOf("com.intellij.java"))
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
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("243.*")
        pluginDescription.set("將 Java DTO 轉換為 TypeScript 接口定義")
        changeNotes.set("""
            <ul>
                <li>更名為 Java-dto-to-Ts-interface</li>
            </ul>
        """)
        pluginId.set("org.freeone.javadto.tsinterface")
    }
} 