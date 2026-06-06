plugins {
	java
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.dinhuan"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation ("org.flywaydb:flyway-core")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.1")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
	implementation(files("libs/uid-generator-1.0.0-SNAPSHOT.jar"))
	implementation ("org.springframework.boot:spring-boot-starter-security")
	implementation ("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly ("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly ("io.jsonwebtoken:jjwt-jackson:0.12.6")
	implementation ("org.sqids:sqids:0.1.0")
	implementation("commons-lang:commons-lang:2.6")
	implementation ("org.springframework.boot:spring-boot-starter-cache")
	implementation ("com.github.ben-manes.caffeine:caffeine")
	implementation ("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.apache.commons:commons-pool2")
	implementation("com.github.xiaolyuh:layering-cache-starter")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
