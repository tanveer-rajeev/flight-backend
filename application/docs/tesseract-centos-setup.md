# Tesseract OCR — CentOS Server Setup

Install and configure **Tesseract** on a CentOS/RHEL-family Linux server for the passport extraction service (`PassportExtractionService`).

The Java app uses **Tess4J 5.19.0** with **Lept4J 1.24.0**.

**Important:** Lept4J does **not** bundle Linux `.so` files (only Windows DLLs). On EL9 you must install **Leptonica 1.87+** and **Tesseract 5.x** — the RPM versions (4.1 / 1.80) are too old.

Use the install script (builds from source to `/usr/local`):

```bash
chmod +x install-tesseract-centos.sh start.sh
sudo ./install-tesseract-centos.sh   # ~10–15 min compile
./start.sh restart
tail -f app.log
```

---

## What the app needs

| Requirement | Value |
|-------------|-------|
| Language | `eng` (English trained data) |
| Tessdata path | Directory containing `eng.traineddata` |
| Config property | `tesseract.datapath=/path/to/tessdata` |
| Optional tuning | `passport.ocr-max-dimension=2200` |

The service runs two OCR passes:

1. **Full page** — page segmentation mode 6  
2. **MRZ region** (bottom ~22% of image) — mode 7, charset `A–Z 0–9 <`

Only **English** language data is required for passport/MRZ extraction.

---

## 1. Install Tesseract

### CentOS Stream 8 / 9, Rocky Linux, AlmaLinux

Enable EPEL, then install Tesseract, **Leptonica** (required native libs for Tess4J), and English language pack:

```bash
sudo dnf install -y epel-release
sudo dnf install -y tesseract tesseract-langpack-eng leptonica
sudo ldconfig
```

Tess4J loads **`libleptonica.so`** and **`libtesseract.so`** from the OS at runtime. Having `eng.traineddata` alone is not enough — you must install the Leptonica package.

On some images you may also need common image/font libraries used by Java AWT when processing images:

```bash
sudo dnf install -y fontconfig dejavu-sans-fonts
```

### CentOS 7 (legacy)

```bash
sudo yum install -y epel-release
sudo yum install -y tesseract tesseract-langpack-eng leptonica
sudo ldconfig
sudo yum install -y fontconfig dejavu-sans-fonts
```

### Verify installation

```bash
tesseract --version
```

Expected output (version may differ):

```
tesseract 5.x.x
 leptonica-1.x.x
  ...
```

Confirm English trained data exists:

```bash
# Common locations — one of these should work:
ls /usr/share/tesseract/tessdata/eng.traineddata
ls /usr/share/tessdata/eng.traineddata
```

If unsure, ask the package where files were installed:

```bash
rpm -ql tesseract-langpack-eng | grep eng.traineddata
```

Quick OCR smoke test (optional — use any small PNG/JPEG):

```bash
tesseract /path/to/test.png stdout -l eng
```

Confirm native libraries are visible to the JVM:

```bash
ls /usr/lib64/libtesseract.so*
rpm -ql leptonica | grep '\.so'
ldconfig -p | grep -E 'leptonica|lept|tesseract'
ldd /usr/bin/tesseract | grep -E 'leptonica|lept|tesseract'
```

**EL9 / RHEL 9 — Tess4J 5.x:** Lept4J has **no bundled Linux libraries**. Run `install-tesseract-centos.sh` to build **Leptonica 1.87 + Tesseract 5.5** under `/usr/local`. Do **not** symlink EL9's `liblept.so.5` (1.80) — it causes `undefined symbol: returnErrorFloat1`.

| Component | Source |
|-----------|--------|
| tess4j 5.19 + lept4j 1.24 | app.jar |
| leptonica 1.87+ | `/usr/local/lib` (built by install script) |
| tesseract 5.x | `/usr/local/bin/tesseract` |
| eng.traineddata | `/usr/local/share/tessdata` |

---

## 2. Find the tessdata path

`tesseract.datapath` must point to the **folder that directly contains** `eng.traineddata`, not the Tesseract install root.

| OS / package | Typical path |
|--------------|--------------|
| CentOS / RHEL (EPEL) | `/usr/share/tesseract/tessdata` |
| Alternative layout | `/usr/share/tessdata` |

Set the path you verified in step 1:

```bash
TESSDATA_DIR=$(dirname "$(rpm -ql tesseract-langpack-eng | grep eng.traineddata | head -1)")
echo "$TESSDATA_DIR"
# Example output: /usr/share/tesseract/tessdata
```

---

## 3. Configure the Spring Boot app

### Auto-detection (default)

If `tesseract.datapath` is **not set**, the app picks the path for the current OS:

| OS | Path tried |
|----|------------|
| Linux (CentOS/RHEL) | `/usr/share/tesseract/tessdata` |
| Linux (alt) | `/usr/share/tessdata` |
| Windows | `C:/Program Files/Tesseract-OCR/tessdata` |
| macOS | `/opt/homebrew/share/tessdata`, `/usr/local/share/tessdata` |

On your CentOS server, with:

```bash
ls /usr/share/tesseract/tessdata/eng.traineddata
```

no extra config is needed — deploy and restart the app. Startup logs will show:

```
Auto-detected Tesseract tessdata path: /usr/share/tesseract/tessdata
```

### Manual override (optional)

Use only when Tesseract is installed in a non-standard location:

```properties
tesseract.datapath=/custom/path/to/tessdata
passport.ocr-max-dimension=2200
```

---

## 4. systemd service example

Create `/etc/systemd/system/flight-backend.service`:

```ini
[Unit]
Description=Flight Backend API
After=network.target postgresql.service redis.service

[Service]
Type=simple
User=app
WorkingDirectory=/opt/flight-backend
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="PASSPORT_OCR_MAX_DIMENSION=2200"
Environment="LD_LIBRARY_PATH=/usr/lib64"
Environment="JNA_LIBRARY_PATH=/usr/lib64"
# TESSERACT_DATAPATH only if auto-detect fails:
# Environment="TESSERACT_DATAPATH=/usr/share/tesseract/tessdata"
ExecStart=/usr/bin/java -Djava.awt.headless=true -jar /opt/flight-backend/app.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl enable flight-backend
sudo systemctl start flight-backend
sudo systemctl status flight-backend
```

---

## 5. Verify from the running app

### Check logs on startup

After deploy, trigger one passport upload. Successful OCR logs look like:

```
Starting passport extraction for file: passport.jpg
Image uploaded to R2: https://...
Tesseract OCR completed - full page: 1234 chars, MRZ region: 89 chars
```

### Test the API

```bash
# Login
TOKEN=$(curl -s -X POST "http://localhost:8091/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"your-password"}' \
  | jq -r '.data.token')

# Extract
curl -X POST "http://localhost:8091/api/passport/extract" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/passport.jpg"
```

If Tesseract is misconfigured, the API returns **502**:

```json
{
  "success": false,
  "status": 502,
  "message": "Passport OCR failed. Ensure Tesseract is installed correctly.",
  "error": { "code": "MICROSERVICE_ERROR" }
}
```

Check application logs for the underlying Tesseract exception.

---

## 6. Troubleshooting

### `eng.traineddata` not found / Error opening data file

**Cause:** Wrong `tesseract.datapath`.

**Fix:**

```bash
rpm -ql tesseract-langpack-eng | grep eng.traineddata
```

Set `tesseract.datapath` to that directory (the parent folder of `eng.traineddata`).

---

### `returnErrorFloat1` / other `undefined symbol` errors

**Symptom:**

```
UnsatisfiedLinkError: Error looking up function 'returnErrorFloat1': /lib64/liblept.so.5: undefined symbol: returnErrorFloat1
```

**Cause:** The `libleptonica.so` → `liblept.so.5` symlink makes JVM load EL9's old Leptonica 1.80, but Tess4J 5.x / Lept4J 1.24 expects newer symbols.

**Fix:**

```bash
sudo rm -f /usr/lib64/libleptonica.so /lib64/libleptonica.so
sudo ldconfig
./start.sh restart
```

Rebuild `app.jar` with `requiresUnpack` (see `pom.xml`) and always start via `./start.sh` (sets `-Djna.nosys=true` for bundled leptonica).

---

### `Leptonica1` / `libleptonica.so: cannot open shared object file`

**Symptom in logs:**

```
NoClassDefFoundError: Could not initialize class net.sourceforge.lept4j.Leptonica1
Caused by: UnsatisfiedLinkError: Unable to load library 'leptonica'
libleptonica.so: cannot open shared object file: No such file or directory
```

**Cause:** Tess4J needs native **Leptonica** and **Tesseract** shared libraries on the server. `eng.traineddata` being present does not fix this.

**Fix on CentOS / Rocky / Alma:**

```bash
sudo dnf install -y leptonica tesseract tesseract-langpack-eng
sudo ldconfig

# EL9: leptonica ships as liblept.so.5 — Tess4J expects libleptonica.so
if [ ! -e /usr/lib64/libleptonica.so ] && [ -e /usr/lib64/liblept.so.5 ]; then
  sudo ln -sf /usr/lib64/liblept.so.5 /usr/lib64/libleptonica.so
  sudo ldconfig
fi

ls /usr/lib64/libleptonica.so /usr/lib64/liblept.so.5 /usr/lib64/libtesseract.so*
```

Restart the Java app, then retry the passport upload.

If the app still cannot load libs from a Spring Boot fat JAR, add to your **systemd** service:

```ini
Environment="LD_LIBRARY_PATH=/usr/lib64"
Environment="JNA_LIBRARY_PATH=/usr/lib64"
```

---

### `libtesseract.so` / other native library errors

**Cause:** Tesseract or Leptonica not installed, or `ldconfig` cache stale.

**Fix:**

```bash
sudo dnf install -y leptonica tesseract tesseract-langpack-eng
sudo ldconfig
```

Ensure the app runs on the same machine where the packages are installed (not a minimal JRE-only container without these libs).

---

### OCR returns empty text / `extracted: false` every time

**Cause:** Image quality, not installation.

**Checks:**

- Use a clear, flat, well-lit passport photo (JPEG/PNG).
- Confirm CLI OCR works on the same image:
  ```bash
  tesseract passport.jpg stdout -l eng
  ```
- Try increasing resolution tuning (only if images are very large):
  ```properties
  passport.ocr-max-dimension=2600
  ```

---

### Permission denied reading tessdata

**Cause:** App user cannot read `/usr/share/tesseract/tessdata`.

**Fix:**

```bash
sudo chmod -R a+rX /usr/share/tesseract/tessdata
# Or run app as a user in a group that can read the files
ls -la /usr/share/tesseract/tessdata/eng.traineddata
```

---

### SELinux blocking (rare)

If tessdata or temp files are in a custom path:

```bash
sudo semanage fcontext -a -t usr_t "/opt/tessdata(/.*)?"
sudo restorecon -Rv /opt/tessdata
```

Default `/usr/share/tesseract/tessdata` is usually fine without changes.

---

### Missing fonts / headless server errors

Java image processing on headless servers sometimes needs fontconfig:

```bash
sudo dnf install -y fontconfig dejavu-sans-fonts
```

Run Java with headless AWT (often default on servers):

```bash
java -Djava.awt.headless=true -jar app.jar
```

---

## 7. Docker note (optional)

If the app runs in Docker, install Tesseract **inside the image**, not only on the host.

Example Dockerfile snippet (Rocky/Alma/CentOS Stream base):

```dockerfile
FROM rockylinux:9

RUN dnf install -y epel-release \
    && dnf install -y tesseract tesseract-langpack-eng leptonica fontconfig dejavu-sans-fonts \
    && ldconfig \
    && dnf clean all

ENV LD_LIBRARY_PATH=/usr/lib64
ENV JNA_LIBRARY_PATH=/usr/lib64

COPY target/app.jar /app.jar
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "/app.jar"]
```

---

## 8. Production checklist

- [ ] `leptonica`, `tesseract`, and `tesseract-langpack-eng` installed
- [ ] `libleptonica.so` and `libtesseract.so` visible (`ldconfig -p | grep leptonica`)
- [ ] `eng.traineddata` exists and path confirmed
- [ ] `tesseract.datapath` override only if auto-detect fails (check startup logs)
- [ ] App restarted after config change
- [ ] `POST /api/passport/extract` returns 200 with `extracted: true` on a test scan
- [ ] R2 credentials configured (upload works independently of OCR)
- [ ] Logs show `Tesseract OCR completed` without errors

---

## Related docs

- [Passport Extraction API](./passport-extraction-api.md) — endpoint, request/response, error codes
