---
inclusion: fileMatch
fileMatchPattern: "*.xml"
---

# Android XML Guidelines

## Layout Rules
- Use ConstraintLayout atau LinearLayout untuk simple layouts
- Use MaterialCardView untuk card-based items
- Prefer `match_parent` dan `wrap_content` over fixed dimensions
- Use `@dimen/` resources untuk spacing consistency

## Naming Conventions
- Layout files: `activity_xxx.xml`, `fragment_xxx.xml`, `item_xxx.xml`, `bottom_sheet_xxx.xml`
- IDs: camelCase dengan prefix (e.g., `tvAppName`, `btnSubmit`, `ivIcon`, `rvList`)
- Prefixes: `tv` (TextView), `btn` (Button), `iv` (ImageView), `rv` (RecyclerView), `et` (EditText)

## Material 3 Components
- Use `com.google.android.material.card.MaterialCardView`
- Use `com.google.android.material.button.MaterialButton`
- Use `com.google.android.material.chip.Chip`
- Use `style="@style/Widget.Material3.Button.TonalButton"` for tonal buttons

## Accessibility
- ALWAYS add `android:contentDescription` untuk ImageView/ImageButton
- Use `android:importantForAccessibility` when needed
- Provide meaningful descriptions, bukan "image" atau "button"

## Colors
- Use `?attr/colorPrimary`, `?attr/colorSurface`, etc. untuk theme colors
- Use `@color/` resources untuk custom colors
- Support dark mode dengan proper color resources

## Strings
- NEVER hardcode strings, use `@string/` resources
- Use format strings untuk dynamic content: `@string/app_count` → `%d apps`

## Example Card Item
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="12dp"
    android:layout_marginVertical="4dp"
    app:cardElevation="0dp"
    app:cardCornerRadius="12dp"
    app:strokeWidth="1dp"
    app:strokeColor="@color/outline">
    
    <!-- Content here -->
    
</com.google.android.material.card.MaterialCardView>
```
