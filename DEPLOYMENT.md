# Maven Central Deployment Guide

This guide explains how to deploy XAI Core to Maven Central (Sonatype OSSRH).

## Prerequisites

### 1. Sonatype Account

1. Create account at [https://issues.sonatype.org](https://issues.sonatype.org)
2. Create JIRA ticket to claim `io.github.Thung0808` namespace
3. Verify GitHub ownership (add comment to JIRA ticket from GitHub)

**Example JIRA ticket:**
```
Summary: Request for io.github.Thung0808 namespace
Description: I would like to publish artifacts under io.github.Thung0808
Group Id: io.github.Thung0808
Project URL: https://github.com/Thung0808/xai-core
```

### 2. GPG Key Setup

Generate GPG key for artifact signing:

```powershell
# Install GPG (Windows)
winget install GnuPG.GnuPG

# Generate key
gpg --gen-key
# Use your email: your.email@example.com
# Set strong passphrase

# List keys
gpg --list-keys

# Export public key to keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys <YOUR_KEY_ID>

# Verify upload
gpg --keyserver keyserver.ubuntu.com --recv-keys <YOUR_KEY_ID>
```

### 3. Maven Settings

Configure `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>YOUR_SONATYPE_USERNAME</username>
      <password>YOUR_SONATYPE_PASSWORD</password>
    </server>
  </servers>
  
  <profiles>
    <profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.keyname>YOUR_KEY_ID</gpg.keyname>
        <gpg.passphrase>YOUR_GPG_PASSPHRASE</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
```

**Security Note:** Use Maven encryption for passwords:
```bash
mvn --encrypt-master-password <your_password>
mvn --encrypt-password <your_password>
```

## Deployment Process

### 1. Pre-Release Checklist

- [ ] All tests passing (`mvn clean test`)
- [ ] Version updated in `pom.xml`
- [ ] CHANGELOG.md updated
- [ ] README.md reflects current version
- [ ] Javadoc complete (no warnings)
- [ ] Examples compile and run
- [ ] No snapshot dependencies

### 2. Build Artifacts

```powershell
# Clean build with all artifacts
mvn clean package

# Verify artifacts generated:
# - xai-core-0.7.0.jar (main JAR)
# - xai-core-0.7.0-sources.jar (sources)
# - xai-core-0.7.0-javadoc.jar (javadoc)
# - xai-core-0.7.0.pom (POM)
# - *.asc files (GPG signatures)

# Inspect JAR contents
jar tf target/xai-core-0.7.0.jar | Select-String -Pattern "\.class$" | Measure-Object
# Should see ~60+ classes
```

### 3. Deploy to Staging

```powershell
# Deploy to OSSRH staging repository
mvn clean deploy -P ossrh

# If deployment succeeds, you'll see:
# [INFO] BUILD SUCCESS
# [INFO] Uploaded to ossrh: https://oss.sonatype.org/.../xai-core/0.7.0/...
```

### 4. Release to Maven Central

Option A: **Automatic Release** (configured in POM)
```xml
<autoReleaseAfterClose>true</autoReleaseAfterClose>
```
Artifacts automatically released after staging validation.

Option B: **Manual Release**
1. Login to [https://oss.sonatype.org](https://oss.sonatype.org)
2. Navigate to "Staging Repositories"
3. Find your staging repository (`iogihubThung0808-xxxx`)
4. Click "Close" (triggers validation)
5. Wait for validation (5-10 minutes)
6. If successful, click "Release"
7. Artifacts sync to Maven Central (2-4 hours)

### 5. Verify Release

Wait 2-4 hours, then check:

```powershell
# Check Maven Central search
# Visit: https://search.maven.org/artifact/io.github.Thung0808/xai-core/0.7.0/jar

# Test dependency resolution
mvn dependency:get -Dartifact=io.github.Thung0808:xai-core:0.7.0

# Verify in new project
mvn archetype:generate -DgroupId=test -DartifactId=test-xai -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
cd test-xai
# Add dependency to pom.xml
mvn clean compile
```

## Version Management

### Release Candidates (RC)

For testing before stable release:

```xml
<version>1.0.0-RC1</version>
```

Deploy to staging and share with beta testers.

### Stable Releases

```xml
<version>1.0.0</version>
```

Only deploy after thorough testing.

### Patch Releases

```xml
<version>1.0.1</version>
```

Bugfixes and security updates only.

## Troubleshooting

### Issue: GPG Signing Fails

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-gpg-plugin:3.1.0:sign
```

**Solution:**
```powershell
# Check GPG is in PATH
gpg --version

# Test signing manually
gpg --armor --detach-sign target/xai-core-0.7.0.jar

# Check passphrase in settings.xml
```

### Issue: Missing Sources/Javadoc

```
[ERROR] Missing: sources JAR
```

**Solution:**
```powershell
# Verify plugins in pom.xml
# - maven-source-plugin
# - maven-javadoc-plugin

# Build explicitly
mvn clean source:jar javadoc:jar
```

### Issue: POM Validation Fails

```
[ERROR] Invalid POM: Missing required element: <licenses>
```

**Solution:** Ensure pom.xml has all required elements:
- `<name>`
- `<description>`
- `<url>`
- `<licenses>`
- `<developers>`
- `<scm>`

### Issue: Nexus Staging Validation Fails

Common failures:
- **Invalid POM**: Missing metadata → Fix pom.xml
- **Invalid Signature**: GPG issue → Re-sign artifacts
- **Missing Javadoc**: Empty javadoc JAR → Fix javadoc warnings

**Solution:**
1. Drop failed staging repository
2. Fix issues
3. Deploy again

## Post-Release

### 1. Tag Release

```powershell
git tag -a v0.7.0 -m "Release 0.7.0: UX & Adoption features"
git push origin v0.7.0
```

### 2. GitHub Release

1. Go to GitHub Releases
2. Click "Create a new release"
3. Select tag `v0.7.0`
4. Title: `XAI Core 0.7.0`
5. Description: Copy from CHANGELOG.md
6. Attach artifacts (optional):
   - `xai-core-0.7.0.jar`
   - `xai-core-0.7.0-sources.jar`
   - `xai-core-0.7.0-javadoc.jar`

### 3. Update Documentation

- [ ] Update README badges
- [ ] Update documentation website
- [ ] Announce on social media / mailing lists
- [ ] Update examples to use new version

### 4. Prepare Next Version

```xml
<version>0.8.0-SNAPSHOT</version>
```

## Resources

- [Sonatype OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Maven GPG Plugin](https://maven.apache.org/plugins/maven-gpg-plugin/)
- [Maven Central Requirements](https://central.sonatype.org/publish/requirements/)
- [GPG Key Generation](https://central.sonatype.org/publish/requirements/gpg/)

## Security Best Practices

1. **Never commit credentials** to Git
2. **Use Maven password encryption**
3. **Rotate GPG keys** every 2-3 years
4. **Revoke compromised keys** immediately
5. **Backup private keys** securely

## Support

For deployment issues:
- Sonatype JIRA: [https://issues.sonatype.org](https://issues.sonatype.org)
- Maven Central Guide: [https://central.sonatype.org/](https://central.sonatype.org/)
