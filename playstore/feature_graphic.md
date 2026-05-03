# Feature graphic — TODO

Play Store requires a 1024×500 PNG/JPG feature graphic shown at the top of the listing.

## Spec
- 1024×500 px
- PNG or JPG
- No transparency
- Text in the image is OK but Play prefers minimal text since the title is shown beside it

## Design suggestion (matches app aesthetic)

Black background. Six color discs (the same six shown in the score-card share image) arranged left-to-right across the middle. App name "Memory Lights" in the green accent color, big and centered. Optional subtitle "watch · repeat · level up" in gray.

Or: a screenshot of 6-button gameplay (`screenshots/02_six_button_gameplay.png`) cropped to 1024×500 and overlaid with the title.

## Tools
- Figma (free, web-based)
- Canva (templates exist for "Google Play feature graphic")
- ImageMagick if you want to script it from the existing share-card style

## Quick scriptable version (if you want to do it programmatically)

The native-Canvas pattern in `app/src/main/java/com/happypuppy/memorylights/ui/share/ScoreShareHelper.kt` could be adapted to render a 1024×500 graphic with the same look. Worth considering if you want the feature graphic to match the score-card aesthetic exactly.

---

Once created, save as `playstore/feature_graphic.png` and upload to Play Console → Store listing → Graphics → Feature graphic.
