#!/usr/bin/env bash
# Install Tesseract 5 + Leptonica 1.87 from source for tess4j 5.x on EL9
# Usage: sudo ./install-tesseract-centos.sh
set -euo pipefail

PREFIX="${PREFIX:-/usr/local}"
LEPTONICA_VERSION="${LEPTONICA_VERSION:-1.87.0}"
TESSERACT_VERSION="${TESSERACT_VERSION:-5.5.0}"
BUILD_DIR="${BUILD_DIR:-/tmp/tesseract-build}"
TESSDATA_DIR="${PREFIX}/share/tessdata"

log() { echo "==> $*"; }
fail() { echo "error: $*" >&2; exit 1; }

leptonica_installed() {
  compgen -G "${PREFIX}/lib/libleptonica.so*" >/dev/null 2>&1 \
    || compgen -G "${PREFIX}/lib64/libleptonica.so*" >/dev/null 2>&1
}

if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
  fail "Run as root: sudo ./install-tesseract-centos.sh"
fi

if command -v dnf >/dev/null 2>&1; then PKG=dnf; else PKG=yum; fi

log "Removing old libleptonica symlink (if any) ..."
rm -f /usr/lib64/libleptonica.so /lib64/libleptonica.so

log "Installing build dependencies ($PKG) ..."
$PKG install -y epel-release
$PKG groupinstall -y "Development Tools" \
  || $PKG install -y gcc gcc-c++ make autoconf automake libtool pkgconfig
$PKG install -y \
  wget curl git \
  libpng-devel libjpeg-turbo-devel libtiff-devel zlib-devel \
  libwebp-devel libarchive-devel libcurl-devel \
  fontconfig dejavu-sans-fonts

# openjpeg optional on EL9 — package name varies; leptonica builds without it
for _oj in openjpeg2-devel libopenjp2-devel openjpeg-devel; do
  $PKG install -y "$_oj" 2>/dev/null && break || true
done

mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

if ! leptonica_installed; then
  log "Building Leptonica ${LEPTONICA_VERSION} ..."
  rm -rf "leptonica-${LEPTONICA_VERSION}"
  wget -O "leptonica-${LEPTONICA_VERSION}.tar.gz" \
    "https://github.com/DanBloomberg/leptonica/releases/download/${LEPTONICA_VERSION}/leptonica-${LEPTONICA_VERSION}.tar.gz"
  tar xzf "leptonica-${LEPTONICA_VERSION}.tar.gz"
  cd "leptonica-${LEPTONICA_VERSION}"
  ./configure --prefix="$PREFIX"
  make -j"$(nproc)"
  make install
  cd "$BUILD_DIR"
else
  log "Leptonica already under $PREFIX — skip"
fi

cat >/etc/ld.so.conf.d/usrlocal.conf <<EOF
/usr/local/lib
/usr/local/lib64
EOF
ldconfig

if [[ ! -x "${PREFIX}/bin/tesseract" ]]; then
  log "Building Tesseract ${TESSERACT_VERSION} ..."
  rm -rf "tesseract-${TESSERACT_VERSION}"
  wget -O "tesseract-${TESSERACT_VERSION}.tar.gz" \
    "https://github.com/tesseract-ocr/tesseract/archive/${TESSERACT_VERSION}.tar.gz"
  tar xzf "tesseract-${TESSERACT_VERSION}.tar.gz"
  cd "tesseract-${TESSERACT_VERSION}"
  ./autogen.sh
  PKG_CONFIG_PATH="${PREFIX}/lib/pkgconfig:${PREFIX}/lib64/pkgconfig:${PKG_CONFIG_PATH:-}" \
    LDFLAGS="-L${PREFIX}/lib -L${PREFIX}/lib64 -Wl,-rpath,${PREFIX}/lib" \
    CPPFLAGS="-I${PREFIX}/include" \
    ./configure --prefix="$PREFIX"
  make -j"$(nproc)"
  make install
  ldconfig
  cd "$BUILD_DIR"
else
  log "Tesseract already under $PREFIX — skip"
fi

[[ -x "${PREFIX}/bin/tesseract" ]] || fail "Build finished but ${PREFIX}/bin/tesseract missing"

log "Installing eng.traineddata ..."
mkdir -p "$TESSDATA_DIR"
if [[ ! -f "${TESSDATA_DIR}/eng.traineddata" ]]; then
  curl -fsSL -o "${TESSDATA_DIR}/eng.traineddata" \
    "https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata"
fi

log "Verify ..."
"${PREFIX}/bin/tesseract" --version | head -3
ls -la "${TESSDATA_DIR}/eng.traineddata"
ls -la "${PREFIX}/lib"/libleptonica* "${PREFIX}/lib64"/libleptonica* 2>/dev/null || true

echo
echo "=============================================="
echo " Done"
echo "=============================================="
echo "Tesseract: ${PREFIX}/bin/tesseract"
echo "Tessdata:  ${TESSDATA_DIR}"
echo
echo "Next: ./start.sh restart && tail -f app.log"
