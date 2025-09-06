#!/bin/bash

# Script zum Rebasen aller release/* Branches auf master und Erstellen von GitHub Releases
# Autor: MinecraftServerAPI Team
# Datum: 2025-09-06

set -e  # Bei Fehler beenden

# Farben für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging-Funktionen
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Prüfe ob wir im richtigen Verzeichnis sind
if [ ! -f "pom.xml" ] || [ ! -d ".git" ]; then
    log_error "Dieses Skript muss im Root-Verzeichnis des MinecraftServerAPI-Projekts ausgeführt werden!"
    exit 1
fi

# Prüfe ob gh (GitHub CLI) installiert ist
if ! command -v gh &> /dev/null; then
    log_error "GitHub CLI (gh) ist nicht installiert. Bitte installiere es mit: brew install gh"
    exit 1
fi

# Prüfe ob wir bei GitHub authentifiziert sind
if ! gh auth status &> /dev/null; then
    log_error "Nicht bei GitHub authentifiziert. Bitte führe 'gh auth login' aus."
    exit 1
fi

# Speichere den aktuellen Branch
CURRENT_BRANCH=$(git branch --show-current)
log_info "Aktueller Branch: $CURRENT_BRANCH"

# Stelle sicher, dass das Working Directory sauber ist
if [ -n "$(git status --porcelain)" ]; then
    log_error "Working Directory ist nicht sauber. Bitte committe oder stashe deine Änderungen."
    exit 1
fi

# Hole die neuesten Änderungen
log_info "Hole die neuesten Änderungen vom Remote..."
git fetch --all --prune

# Wechsle zu master und aktualisiere
log_info "Wechsle zu master Branch..."
git checkout master
git pull origin master

# Hole alle release/* Branches
RELEASE_BRANCHES=$(git branch -r | grep 'origin/release/' | sed 's/origin\///' | sort -V)

if [ -z "$RELEASE_BRANCHES" ]; then
    log_warning "Keine release/* Branches gefunden!"
    git checkout "$CURRENT_BRANCH"
    exit 0
fi

log_info "Gefundene Release-Branches:"
echo "$RELEASE_BRANCHES" | while read -r branch; do
    echo "  - $branch"
done

# Arrays für Erfolg und Fehler
SUCCESSFUL_REBASES=()
FAILED_REBASES=()
SUCCESSFUL_RELEASES=()
FAILED_RELEASES=()

# Rebase alle Release-Branches
echo ""
log_info "=== PHASE 1: Rebase alle Release-Branches auf master ==="
echo ""

for branch in $RELEASE_BRANCHES; do
    log_info "Verarbeite Branch: $branch"
    
    # Checkout des Branches
    if git checkout "$branch" 2>/dev/null || git checkout -b "$branch" "origin/$branch" 2>/dev/null; then
        
        # Versuche Rebase
        log_info "Rebase $branch auf master..."
        if git rebase master; then
            log_success "Rebase von $branch erfolgreich!"
            
            # Force Push (da wir rebased haben)
            log_info "Pushe $branch zum Remote..."
            if git push --force-with-lease origin "$branch"; then
                log_success "Push von $branch erfolgreich!"
                SUCCESSFUL_REBASES+=("$branch")
            else
                log_error "Push von $branch fehlgeschlagen!"
                FAILED_REBASES+=("$branch")
                git rebase --abort 2>/dev/null || true
            fi
        else
            log_error "Rebase von $branch fehlgeschlagen! Überspringe..."
            FAILED_REBASES+=("$branch")
            git rebase --abort 2>/dev/null || true
        fi
    else
        log_error "Konnte Branch $branch nicht auschecken!"
        FAILED_REBASES+=("$branch")
    fi
    
    echo ""
done

# Wechsle zurück zu master für Release-Erstellung
git checkout master

echo ""
log_info "=== PHASE 2: Erstelle GitHub Releases ==="
echo ""

# Erstelle Releases für erfolgreich gerebaste Branches
for branch in "${SUCCESSFUL_REBASES[@]}"; do
    # Extrahiere Version aus Branch-Namen (z.B. release/1.19.4 -> 1.19.4)
    VERSION=$(echo "$branch" | sed 's/release\///')
    TAG_NAME="v$VERSION"
    
    log_info "Erstelle Release für $branch (Tag: $TAG_NAME)..."
    
    # Checkout des Branches für Release
    git checkout "$branch"
    
    # Hole die neueste Commit-Message für Release Notes
    LAST_COMMIT=$(git log -1 --pretty=format:"%s")
    
    # Generiere Release Notes
    RELEASE_NOTES="## MinecraftServerAPI v$VERSION

### 🎮 Minecraft Version
Supports Minecraft $VERSION

### 📦 Änderungen
- Rebased auf den neuesten master Branch
- Alle aktuellen Features und Bugfixes enthalten

### 🔄 Letzte Änderung
$LAST_COMMIT

### 📥 Installation
Lade die JAR-Datei herunter und platziere sie in deinem Minecraft Server's \`plugins\` Ordner.

### 📖 Dokumentation
Siehe [README.md](https://github.com/$(gh repo view --json nameWithOwner -q .nameWithOwner)/blob/$branch/README.md) für die vollständige Dokumentation.

---
*Automatisch generiert am $(date '+%Y-%m-%d %H:%M:%S')*"
    
    # Erstelle Tag falls nicht vorhanden
    if ! git tag | grep -q "^$TAG_NAME$"; then
        log_info "Erstelle Tag $TAG_NAME..."
        git tag "$TAG_NAME"
        git push origin "$TAG_NAME"
    else
        log_warning "Tag $TAG_NAME existiert bereits, aktualisiere..."
        git tag -f "$TAG_NAME"
        git push origin -f "$TAG_NAME"
    fi
    
    # Baue das Projekt
    log_info "Baue JAR-Datei für $VERSION..."
    if mvn clean package -DskipTests > /dev/null 2>&1; then
        JAR_FILE=$(find target -name "MinecraftServerAPI-*.jar" | head -1)
        
        if [ -f "$JAR_FILE" ]; then
            # Erstelle oder aktualisiere Release
            log_info "Erstelle GitHub Release..."
            
            # Prüfe ob Release bereits existiert
            if gh release view "$TAG_NAME" > /dev/null 2>&1; then
                log_warning "Release $TAG_NAME existiert bereits, lösche und erstelle neu..."
                gh release delete "$TAG_NAME" --yes
            fi
            
            # Erstelle neues Release
            if gh release create "$TAG_NAME" \
                --title "MinecraftServerAPI v$VERSION" \
                --notes "$RELEASE_NOTES" \
                --target "$branch" \
                "$JAR_FILE#MinecraftServerAPI-$VERSION.jar"; then
                
                log_success "Release $TAG_NAME erfolgreich erstellt!"
                SUCCESSFUL_RELEASES+=("$VERSION")
            else
                log_error "Fehler beim Erstellen von Release $TAG_NAME!"
                FAILED_RELEASES+=("$VERSION")
            fi
        else
            log_error "JAR-Datei nicht gefunden für $VERSION!"
            FAILED_RELEASES+=("$VERSION")
        fi
    else
        log_error "Build fehlgeschlagen für $VERSION!"
        FAILED_RELEASES+=("$VERSION")
    fi
    
    echo ""
done

# Zurück zum ursprünglichen Branch
log_info "Wechsle zurück zu $CURRENT_BRANCH..."
git checkout "$CURRENT_BRANCH"

# Zusammenfassung
echo ""
echo "========================================"
echo "           ZUSAMMENFASSUNG              "
echo "========================================"
echo ""

if [ ${#SUCCESSFUL_REBASES[@]} -gt 0 ]; then
    log_success "Erfolgreich gerebaste Branches (${#SUCCESSFUL_REBASES[@]}):"
    for branch in "${SUCCESSFUL_REBASES[@]}"; do
        echo "  ✅ $branch"
    done
fi

if [ ${#FAILED_REBASES[@]} -gt 0 ]; then
    echo ""
    log_error "Fehlgeschlagene Rebases (${#FAILED_REBASES[@]}):"
    for branch in "${FAILED_REBASES[@]}"; do
        echo "  ❌ $branch"
    done
fi

if [ ${#SUCCESSFUL_RELEASES[@]} -gt 0 ]; then
    echo ""
    log_success "Erfolgreich erstellte Releases (${#SUCCESSFUL_RELEASES[@]}):"
    for version in "${SUCCESSFUL_RELEASES[@]}"; do
        echo "  ✅ v$version"
    done
fi

if [ ${#FAILED_RELEASES[@]} -gt 0 ]; then
    echo ""
    log_error "Fehlgeschlagene Releases (${#FAILED_RELEASES[@]}):"
    for version in "${FAILED_RELEASES[@]}"; do
        echo "  ❌ v$version"
    done
fi

echo ""
echo "========================================"

# Exit-Code basierend auf Erfolg
if [ ${#FAILED_REBASES[@]} -eq 0 ] && [ ${#FAILED_RELEASES[@]} -eq 0 ]; then
    log_success "Alle Operationen erfolgreich abgeschlossen! 🎉"
    exit 0
else
    log_warning "Einige Operationen sind fehlgeschlagen. Bitte prüfe die Fehler oben."
    exit 1
fi