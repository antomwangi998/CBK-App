#!/data/data/com.termux/files/usr/bin/bash
# =============================================================================
# Hela Smart SACCO - Termux Setup & Push Script
# Replaces the existing Python/Kivy CBK-App repo with the Kotlin rewrite
# =============================================================================

set -e  # exit on any error

# ── Colors ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

log()     { echo -e "${GREEN}[✓]${NC} $1"; }
warn()    { echo -e "${YELLOW}[!]${NC} $1"; }
error()   { echo -e "${RED}[✗]${NC} $1"; exit 1; }
section() { echo -e "\n${CYAN}══════════════════════════════════════${NC}"; echo -e "${BLUE}  $1${NC}"; echo -e "${CYAN}══════════════════════════════════════${NC}"; }

# ── Config — edit these ────────────────────────────────────────────────────────
GITHUB_USERNAME="antomwangi998"
REPO_NAME="CBK-App"
PROJECT_ZIP="HelaSacco-Complete.zip"   # path to the zip you downloaded
WORK_DIR="$HOME/hela-sacco-push"
# ─────────────────────────────────────────────────────────────────────────────

section "Step 1: Installing required packages"

pkg update -y 2>/dev/null || warn "pkg update had issues, continuing..."
pkg install -y git unzip openssh 2>/dev/null
log "Packages ready"

section "Step 2: Configure Git identity"

# Check if git is already configured
GIT_NAME=$(git config --global user.name 2>/dev/null || echo "")
GIT_EMAIL=$(git config --global user.email 2>/dev/null || echo "")

if [ -z "$GIT_NAME" ]; then
  echo -n "Enter your full name for git commits: "
  read GIT_NAME
  git config --global user.name "$GIT_NAME"
fi

if [ -z "$GIT_EMAIL" ]; then
  echo -n "Enter your GitHub email: "
  read GIT_EMAIL
  git config --global user.email "$GIT_EMAIL"
fi

git config --global init.defaultBranch main
log "Git configured as: $GIT_NAME <$GIT_EMAIL>"

section "Step 3: GitHub authentication"

echo -e "${YELLOW}You need a GitHub Personal Access Token (PAT) to push.${NC}"
echo "If you don't have one:"
echo "  1. Go to: https://github.com/settings/tokens"
echo "  2. Click 'Generate new token (classic)'"
echo "  3. Select scopes: repo (full), workflow"
echo "  4. Copy the token (starts with ghp_...)"
echo ""
echo -n "Paste your GitHub PAT here (input hidden): "
read -s GITHUB_TOKEN
echo ""

if [ -z "$GITHUB_TOKEN" ]; then
  error "No token provided. Cannot push without authentication."
fi

# Store credentials so we don't need to re-enter
git config --global credential.helper store
echo "https://${GITHUB_USERNAME}:${GITHUB_TOKEN}@github.com" > ~/.git-credentials
chmod 600 ~/.git-credentials
log "GitHub credentials saved"

section "Step 4: Locating project zip"

# Search common locations
ZIP_PATH=""
SEARCH_PATHS=(
  "$HOME/storage/downloads/$PROJECT_ZIP"
  "$HOME/storage/shared/Download/$PROJECT_ZIP"
  "$HOME/Downloads/$PROJECT_ZIP"
  "$HOME/$PROJECT_ZIP"
  "$(pwd)/$PROJECT_ZIP"
)

for path in "${SEARCH_PATHS[@]}"; do
  if [ -f "$path" ]; then
    ZIP_PATH="$path"
    log "Found zip at: $ZIP_PATH"
    break
  fi
done

if [ -z "$ZIP_PATH" ]; then
  echo ""
  warn "Could not auto-find $PROJECT_ZIP"
  echo "Please enter the full path to $PROJECT_ZIP:"
  echo "  (Tip: usually in ~/storage/downloads/)"
  echo -n "Path: "
  read ZIP_PATH
  if [ ! -f "$ZIP_PATH" ]; then
    error "File not found at: $ZIP_PATH"
  fi
fi

section "Step 5: Extracting project"

rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

unzip -q "$ZIP_PATH" -d .
log "Extracted to $WORK_DIR"

# Find the project root (the folder inside the zip)
PROJECT_ROOT=$(find . -name "settings.gradle.kts" -maxdepth 3 | head -1 | xargs dirname)
if [ -z "$PROJECT_ROOT" ]; then
  error "Could not find settings.gradle.kts — invalid project zip"
fi
log "Project root: $PROJECT_ROOT"
cd "$PROJECT_ROOT"

section "Step 6: Initialising git and connecting to GitHub"

REMOTE_URL="https://github.com/${GITHUB_USERNAME}/${REPO_NAME}.git"

if [ -d ".git" ]; then
  warn "Existing .git found — resetting it"
  rm -rf .git
fi

git init
git remote add origin "$REMOTE_URL"
log "Remote set to: $REMOTE_URL"

section "Step 7: Creating .gitignore"

cat > .gitignore << 'GITIGNORE'
# Android / Gradle
*.iml
.gradle/
local.properties
.idea/
.DS_Store
build/
captures/
.externalNativeBuild/
.cxx/
*.apk
*.aab
*.ap_
*.dex

# Secrets — NEVER commit these
keystore.jks
*.keystore
*.jks
release.properties

# local.properties has API keys — kept out of git
local.properties
GITIGNORE

log ".gitignore created"

section "Step 8: Wiping old Python code from remote and force-pushing"

echo ""
warn "This will REPLACE all files in https://github.com/${GITHUB_USERNAME}/${REPO_NAME}"
warn "The old Python/Kivy code will be gone. Are you sure? (yes/no)"
echo -n "> "
read CONFIRM

if [ "$CONFIRM" != "yes" ]; then
  echo "Aborted. Nothing was pushed."
  exit 0
fi

git add -A
git commit -m "feat: rewrite Hela Smart SACCO in Kotlin/Jetpack Compose

Complete rewrite from Python/Kivy to native Android:
- Kotlin + Jetpack Compose + Material 3
- Room database (mirrors original SQLite schema)
- Hilt dependency injection
- MVVM architecture with StateFlow
- Login with PBKDF2 auth + account lockout
- Dashboard with role-aware stats
- Member registration (4-step wizard)
- Deposit / Withdrawal / Transfer with receipt
- Loan application, schedule, repayment
- KYC approval workflow
- Notifications & Settings
- Reports dashboard
- AI Assistant (Claude API)
- Investment portfolio

Build: GitHub Actions (debug on push, release on tags)"

# Force push — this replaces the Python repo history
git push --force origin main

echo ""
log "══════════════════════════════════════"
log "  Push complete!"
log "══════════════════════════════════════"
echo ""
echo -e "  Repo:    ${CYAN}https://github.com/${GITHUB_USERNAME}/${REPO_NAME}${NC}"
echo -e "  Actions: ${CYAN}https://github.com/${GITHUB_USERNAME}/${REPO_NAME}/actions${NC}"
echo ""
echo "GitHub Actions will now build the APK automatically."
echo "Check the Actions tab — first build takes ~5 minutes."
echo ""

section "Optional: Set GitHub Secrets for the build"

echo "For the build to work fully, add these secrets in GitHub:"
echo "  Settings → Secrets and variables → Actions → New repository secret"
echo ""
echo -e "  ${YELLOW}ANTHROPIC_API_KEY${NC}  → your Claude API key (sk-ant-...)"
echo -e "  ${YELLOW}KEYSTORE_BASE64${NC}    → base64-encoded release keystore (for release builds)"
echo -e "  ${YELLOW}KEY_ALIAS${NC}          → keystore key alias"
echo -e "  ${YELLOW}KEY_PASSWORD${NC}       → key password"
echo -e "  ${YELLOW}STORE_PASSWORD${NC}     → keystore password"
echo ""
echo "To generate a keystore later, run:"
echo -e "  ${CYAN}keytool -genkeypair -v -keystore hela.jks -alias hela -keyalg RSA -keysize 2048 -validity 10000${NC}"
echo "Then encode it:"
echo -e "  ${CYAN}base64 hela.jks | tr -d '\\n'${NC}  (copy the output into KEYSTORE_BASE64)"
