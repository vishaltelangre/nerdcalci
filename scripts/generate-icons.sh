#!/bin/bash

# generate-icons.sh
# Generates Android app icons and fastlane metadata icons from a source image
#
# Usage: ./scripts/generate-icons.sh <source-image>
# Example: ./scripts/generate-icons.sh ~/Downloads/app-icon.png

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check if source image is provided
if [ -z "$1" ]; then
    print_error "No source image provided"
    echo ""
    echo "Usage: $0 <source-image>"
    echo ""
    echo "Example:"
    echo "  $0 ~/Downloads/app-icon.png"
    echo ""
    echo "Requirements:"
    echo "  - Source image should be at least 512x512 (recommended: 1024x1024 or higher)"
    echo "  - Image should be square (1:1 aspect ratio)"
    echo "  - PNG format with transparency (for round icons)"
    exit 1
fi

SOURCE_IMAGE="$1"

# Check if source image exists
if [ ! -f "$SOURCE_IMAGE" ]; then
    print_error "Source image not found: $SOURCE_IMAGE"
    exit 1
fi

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

print_info "Source image: $SOURCE_IMAGE"
print_info "Project root: $PROJECT_ROOT"

# Check which image tool is available
if command -v sips &> /dev/null; then
    IMAGE_TOOL="sips"
    print_info "Using sips for image resizing (macOS native)"
elif command -v convert &> /dev/null; then
    IMAGE_TOOL="convert"
    print_info "Using ImageMagick convert for image resizing"
else
    print_error "No image resizing tool found!"
    echo ""
    echo "Please install one of the following:"
    echo "  - macOS: sips (built-in)"
    echo "  - Linux/macOS: ImageMagick (brew install imagemagick)"
    exit 1
fi

# Function to resize image using sips
resize_with_sips() {
    local input="$1"
    local output="$2"
    local size="$3"

    sips -z "$size" "$size" "$input" --out "$output" > /dev/null 2>&1
}

# Function to resize image using ImageMagick
resize_with_convert() {
    local input="$1"
    local output="$2"
    local size="$3"

    convert "$input" -resize "${size}x${size}" "$output"
}

# Function to resize image (auto-detect tool)
resize_image() {
    local input="$1"
    local output="$2"
    local size="$3"

    # Create output directory if it doesn't exist
    mkdir -p "$(dirname "$output")"

    if [ "$IMAGE_TOOL" = "sips" ]; then
        resize_with_sips "$input" "$output" "$size"
    else
        resize_with_convert "$input" "$output" "$size"
    fi
}

# Verify source image dimensions
if [ "$IMAGE_TOOL" = "sips" ]; then
    WIDTH=$(sips -g pixelWidth "$SOURCE_IMAGE" | grep -oE '[0-9]+$')
    HEIGHT=$(sips -g pixelHeight "$SOURCE_IMAGE" | grep -oE '[0-9]+$')
else
    DIMENSIONS=$(identify -format "%wx%h" "$SOURCE_IMAGE")
    WIDTH=$(echo "$DIMENSIONS" | cut -d'x' -f1)
    HEIGHT=$(echo "$DIMENSIONS" | cut -d'x' -f2)
fi

print_info "Source image dimensions: ${WIDTH}x${HEIGHT}"

# Check if image is square
if [ "$WIDTH" -ne "$HEIGHT" ]; then
    print_warning "Source image is not square (${WIDTH}x${HEIGHT})"
    print_warning "Icons may appear distorted"
fi

# Check minimum size
if [ "$WIDTH" -lt 512 ] || [ "$HEIGHT" -lt 512 ]; then
    print_warning "Source image is smaller than recommended (512x512)"
    print_warning "Generated icons may appear pixelated"
fi

echo ""
print_info "Generating Android mipmap icons..."

# Android mipmap icon sizes
# Reference: https://developer.android.com/training/multiscreen/screendensities

# Generate Android mipmap icons
# Format: density:size
for entry in "mdpi:48" "hdpi:72" "xhdpi:96" "xxhdpi:144" "xxxhdpi:192"; do
    density=$(echo "$entry" | cut -d':' -f1)
    size=$(echo "$entry" | cut -d':' -f2)
    mipmap_dir="$PROJECT_ROOT/app/src/main/res/mipmap-$density"

    print_info "Generating $density (${size}x${size})..."

    # Generate regular launcher icon
    resize_image "$SOURCE_IMAGE" "$mipmap_dir/ic_launcher.png" "$size"
    print_success "  Created ic_launcher.png"

    # Generate round launcher icon (same as regular for now)
    resize_image "$SOURCE_IMAGE" "$mipmap_dir/ic_launcher_round.png" "$size"
    print_success "  Created ic_launcher_round.png"
done

echo ""
print_info "Generating fastlane metadata icon..."

# Fastlane icon (512x512 recommended by Google Play)
fastlane_icon_dir="$PROJECT_ROOT/fastlane/metadata/android/en-US/images"
resize_image "$SOURCE_IMAGE" "$fastlane_icon_dir/icon.png" 192
print_success "Created fastlane icon (192x192)"

echo ""
print_info "Removing extended attributes (macOS)..."

# Remove macOS extended attributes that can cause Android build issues
if command -v xattr &> /dev/null; then
    xattr -cr "$PROJECT_ROOT/app/src/main/res/mipmap-"* 2>/dev/null || true
    xattr -cr "$fastlane_icon_dir" 2>/dev/null || true
    print_success "Cleaned extended attributes"
else
    print_warning "xattr not available (not a problem on non-macOS)"
fi

echo ""
print_success "Icon generation complete!"
echo ""

# Summary
echo "Generated icons:"
echo "  • mdpi:    48x48   (app/src/main/res/mipmap-mdpi/)"
echo "  • hdpi:    72x72   (app/src/main/res/mipmap-hdpi/)"
echo "  • xhdpi:   96x96   (app/src/main/res/mipmap-xhdpi/)"
echo "  • xxhdpi:  144x144 (app/src/main/res/mipmap-xxhdpi/)"
echo "  • xxxhdpi: 192x192 (app/src/main/res/mipmap-xxxhdpi/)"
echo "  • fastlane: 192x192 (fastlane/metadata/android/en-US/images/)"
echo ""

# Check file sizes
total_size=$(du -sh "$PROJECT_ROOT/app/src/main/res/mipmap-"* | awk '{sum+=$1} END {print sum}')
print_info "Total size: ~${total_size}KB"

echo ""
print_success "Next steps:"
echo "  1. Review generated icons in Android Studio"
echo "  2. Build the app: ./gradlew assembleDebug"
echo "  3. Test on device/emulator"
echo "  4. Commit the changes"
echo ""

print_warning "Note: If your source icon has transparency, consider creating"
print_warning "a version with a solid background for better display on all launchers."
