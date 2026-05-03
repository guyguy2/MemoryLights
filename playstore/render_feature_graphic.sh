#!/usr/bin/env bash
# Renders the 1024x500 Play Store feature graphic via ImageMagick.
# Style matches the in-app Share score card (black bg, six color discs,
# green accent wordmark).
#
# Re-run with: ./playstore/render_feature_graphic.sh
# Output: playstore/feature_graphic.png

set -euo pipefail

cd "$(dirname "$0")"

W=1024
H=500
OUT=feature_graphic.png

FONT_BOLD="/System/Library/Fonts/Supplemental/Arial Bold.ttf"
FONT_REG="/System/Library/Fonts/Supplemental/Arial.ttf"

# Disc colors match the in-app palette (SimonPanel + ScoreShareHelper).
G="#43A047"; R="#E53935"; Y="#FDD835"; B="#1E88E5"; P="#8E24AA"; O="#FB8C00"

# Six discs centered horizontally near the lower third.
DISC_R=28
DISC_Y=320
SPACING=80
COUNT=6
ROW_W=$((SPACING * (COUNT - 1)))
START_X=$((W / 2 - ROW_W / 2))

ACCENT="#4CAF50"

# Build the args as a bash array so paths with spaces stay intact.
args=(
  -size "${W}x${H}" canvas:black
  -fill "$ACCENT"
  -font "$FONT_BOLD" -pointsize 90 -kerning 8
  -gravity north -annotate +0+90 "MEMORY LIGHTS"
)

colors=("$G" "$R" "$Y" "$B" "$P" "$O")
for i in 0 1 2 3 4 5; do
  CX=$((START_X + i * SPACING))
  args+=(-fill "${colors[$i]}" -draw "circle $CX,$DISC_Y $((CX + DISC_R)),$DISC_Y")
done

args+=(
  -fill "#B0B0B0"
  -font "$FONT_REG" -pointsize 28 -kerning 6
  -gravity south -annotate +0+80 "WATCH    REPEAT    LEVEL UP"
  -fill none -stroke "#1F1F1F" -strokewidth 4
  -draw "roundrectangle 4,4 $((W-4)),$((H-4)) 24,24"
  "$OUT"
)

magick "${args[@]}"

echo "Rendered $OUT ($(file "$OUT" | cut -d: -f2-))"
