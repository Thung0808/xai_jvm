# Pre-Maven Central Deployment Checklist

## ✅ Checklist for v1.1.0-alpha Release

This checklist ensures the XAI Core library is ready for Maven Central deployment.

### 1. License Compliance ✅

**Status**: Must verify

- [ ] LICENSE file exists in project root
  - Format: Apache License 2.0 or MIT
  - Location: `/d/xai-jvm/LICENSE`
  - Content: Full license text (minimum 80 lines for Apache 2.0)

**Command to verify**:
```bash
cat d:/xai-jvm/LICENSE | head -20
```

**Fix if missing**:
```bash
# Apache 2.0 License
curl -o d:/xai-jvm/LICENSE https://www.apache.org/licenses/LICENSE-2.0.txt
```

### 2. Javadoc Generation & Validation ✅

**Status**: Need to run

- [ ] Javadoc compiles without errors
  ```bash
  mvn javadoc:javadoc -DskipTests
  ```

- [ ] No rendering errors in LaTeX math formulas
  - Check: `/target/site/apidocs/`
  - Look for broken `$...$` or `$$...$$` blocks
  
- [ ] All public API classes have `@since` tags
  - Example: `@since 0.1.0` or `@since 1.1.0-alpha`

- [ ] Package-level documentation exists
  - Every package should have `package-info.java`
  - Must include usage examples

**Expected files**:
- `src/main/java/.../package-info.java` (✅ confirmed for all 9 packages)
- `xai-core-spring-boot-starter/src/main/java/.../package-info.java` (✅ created)

### 3. JAR Size Validation ✅

**Status**: Need to verify

**Target**: < 20 MB (lightweight principle)

**Command**:
```bash
mvn clean package -DskipTests
ls -lh target/xai-core-*.jar
ls -lh xai-core-spring-boot-starter/target/xai-core-spring-boot-starter-*.jar
```

**Expected Results**:
- Core JAR: ~ 2-5 MB
- Spring Boot Starter JAR: ~ 500 KB (slim, doesn't include dependencies)
- Total with dependencies: < 20 MB when unpacked

**Analysis**:
```bash
# List JAR contents
unzip -l target/xai-core-1.1.0-alpha.jar | head -50

# Check dependency sizes
mvn dependency:tree
mvn dependency:analyze
```

### 4. POM Configuration ✅

**Core Module (`pom.xml`)**:
- [x] GroupId: `io.github.Thung0808`
- [x] ArtifactId: `xai-core`
- [x] Version: `1.1.0-alpha`
- [x] Name: Clear and descriptive
- [x] Description: Detailed description
- [x] URL: Points to GitHub/documentation
- [x] License: Apache 2.0
- [x] Developers: Contact information
- [x] SCM: Git repository details
- [x] Properties: Source/target Java version (21)

**Spring Boot Starter Module**:
- [x] Separate POM with Spring Boot dependencies
- [x] Depends on core module (`xai-core:1.1.0-alpha`)
- [x] Auto-configuration in spring.factories

### 5. Source & Documentation Files ✅

**Core Library**:
- [x] 76 source files (67 Phase 5 + 9 Phase 6)
- [x] 120/120 tests passing
- [x] README.md with examples
- [x] RELEASE-NOTES-1.1.0-alpha.md documenting changes
- [x] All public classes have Javadoc
- [x] No `@deprecated` methods without replacement

**Spring Boot Starter**:
- [x] Auto-configuration class: `XaiAutoConfiguration.java`
- [x] Properties class: `XaiProperties.java`
- [x] Metrics integration: `ExplainerMetrics.java`
- [x] REST controller: `ExplanationController.java`
- [x] Example configuration: `application.yml`
- [x] Grafana dashboard: `grafana-dashboard-xai-core.json`
- [x] Documentation: `package-info.java`

### 6. Dependency Analysis ✅

**Core Library Dependencies**:
```
io.github.Thung0808:smile-ml-adapter:1.1.0-alpha (transitive)
  → smile:smile-core:2.6.0
  → smile:smile-graph:2.6.0
  → org.slf4j:slf4j-api:2.0.9
```

**Spring Boot Starter Dependencies**:
```
io.github.Thung0808:xai-core:1.1.0-alpha (direct)
org.springframework.boot:spring-boot-starter-web:3.2.1
io.micrometer:micrometer-registry-prometheus:1.12.0
```

**Check for issues**:
- [ ] No circular dependencies
- [ ] No transitive version conflicts
- [ ] All dependencies have Maven Central availability
- [ ] No SNAPSHOT dependencies in release build

**Command**:
```bash
mvn clean install -DskipTests
mvn dependency:tree
mvn dependency:analyze
```

### 7. Test Coverage ✅

**Current Status**:
- ✅ 120/120 tests passing
- ✅ Phase 5 tests: 96 tests (all stable explainers)
- ✅ Phase 6 tests: 24 tests (compliance, security)

**Required Coverage**:
- [ ] All public classes have unit tests
- [ ] Critical paths have integration tests
- [ ] No failing tests in any build configuration

**Command**:
```bash
mvn clean test
mvn jacoco:report  # If jacoco plugin added
```

### 8. Maven Central Requirements ✅

**GPG Signing** (for deployment):
- [ ] GPG key generated: `gpg --version`
- [ ] Private key available: `gpg --list-secret-keys`
- [ ] Public key uploaded to keyserver
- [ ] Maven configured for signing: `~/.m2/settings.xml`

**Sonatype Account**:
- [ ] Account created at https://oss.sonatype.org
- [ ] Namespace claim verified (io.github.Thung0808)
- [ ] API token generated for Maven

**Example settings.xml**:
```xml
<servers>
    <server>
        <id>ossrh</id>
        <username>your-sonatype-username</username>
        <password>encrypted-password</password>
    </server>
</servers>

<profiles>
    <profile>
        <id>ossrh</id>
        <properties>
            <gpg.executable>gpg2</gpg.executable>
            <gpg.passphrase>your-passphrase</gpg.passphrase>
        </properties>
    </profile>
</profiles>
```

### 9. Version Management ✅

**Current Version**: `1.1.0-alpha`

**Release Plan**:
1. **v1.1.0-beta** (Next)
   - Merge Phase 6 final testing
   - Release from branch: `release/1.1.0-beta`
   
2. **v1.1.0** (Production)
   - Remove `-alpha` suffix
   - Tag: `v1.1.0`
   
3. **v1.2.0** (Future)
   - Spring Boot Starter stable
   - Kafka/Flink stream processors
   - Benchmarking reports

### 10. Release Process Checklist ✅

**Pre-Release** (Current):
- [x] Verify all tests pass: 120/120 ✅
- [x] Update version in all pom.xml files
- [x] Update RELEASE-NOTES
- [x] Create Git tag: `git tag v1.1.0-alpha`
- [x] Build locally: `mvn clean package`
- [ ] Run full test suite one more time

**Release** (When ready for Maven Central):
```bash
# 1. Ensure clean working directory
git status

# 2. Build with GPG signing
mvn clean deploy -P ossrh,gpg

# 3. Check staging repository
# Visit: https://oss.sonatype.org/service/local/staging/browse
# Status should be: "closeRepositoryByStagingProfileURI"

# 4. Close staging repository
curl -X POST "https://oss.sonatype.org/service/local/staging/bulk/close" ...

# 5. Promote to Maven Central
# After ~2 hours synchronization, verify at:
# https://mvnrepository.com/artifact/io.github.Thung0808/xai-core/1.1.0
```

### 11. Final Verification ✅

**Before Maven Central Deployment**:

```bash
# 1. Verify JAR integrity
jar tf target/xai-core-1.1.0-alpha.jar | head -20

# 2. Test with fresh Maven repository
mvn clean install -U

# 3. Verify Javadoc
mvn javadoc:jar

# 4. Check for forbidden dependencies
mvn dependency:analyze

# 5. Sign all artifacts
mvn gpg:sign -Dgpg.passphrase=your-pass

# 6. Final build
mvn clean package -P release
```

**Expected Output**:
```
[INFO] ==================== BUILD SUCCESS ====================
[INFO] Total time: X.XXXs
[INFO] Finished at: 2026-01-XX ...
[INFO] Final Memory: XXM/XXM
```

### 12. Documentation for Users ✅

**README.md** should include:
- [x] Quick start (30 seconds)
- [x] Installation instructions
- [x] Usage examples
- [x] API reference
- [x] Performance metrics
- [x] Contributing guidelines

**Example Installation**:
```xml
<!-- Core Library -->
<dependency>
    <groupId>io.github.Thung0808</groupId>
    <artifactId>xai-core</artifactId>
    <version>1.1.0-alpha</version>
</dependency>

<!-- Spring Boot Starter (optional) -->
<dependency>
    <groupId>io.github.Thung0808</groupId>
    <artifactId>xai-core-spring-boot-starter</artifactId>
    <version>1.1.0-alpha</version>
</dependency>
```

---

## Summary Status

| Component | Status | Notes |
|-----------|--------|-------|
| **License** | ⏳ TODO | Need to verify LICENSE file exists |
| **Javadoc** | ⏳ TODO | Need to run `mvn javadoc:javadoc` |
| **JAR Size** | ⏳ TODO | Need to verify < 20MB |
| **Tests** | ✅ PASS | 120/120 tests passing |
| **Docs** | ✅ OK | All package-info.java in place |
| **POM Config** | ✅ OK | All metadata complete |
| **Spring Boot** | ✅ OK | Auto-config + metrics ready |
| **Grafana** | ✅ OK | Dashboard JSON provided |

## Next Steps

1. **Verify License File** (2 min)
2. **Run Javadoc Check** (5 min)
3. **Verify JAR Sizes** (3 min)
4. **Create Maven Central Account** (if needed)
5. **Configure GPG Signing** (10 min)
6. **Deploy to Maven Central** (automated)

**Estimated Total Time**: 30 minutes to Maven Central availability
