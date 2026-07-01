# Liquid Glass Migration Guide

Step-by-step guide for migrating existing iOS/macOS/iPadOS apps to the Liquid Glass design system.

## Table of Contents

1. [Phase 1: Automatic Adoption](#phase-1-automatic-adoption)
2. [Phase 2: Remove Conflicting Customizations](#phase-2-remove-conflicting-customizations)
3. [Phase 3: Enhance with Glass APIs](#phase-3-enhance-with-glass-apis)
4. [Phase 4: Platform-Specific Refinements](#phase-4-platform-specific-refinements)
5. [Phase 5: Accessibility and Testing](#phase-5-accessibility-and-testing)
6. [Backward Compatibility](#backward-compatibility)

---

## Phase 1: Automatic Adoption

**Do this first.** Many system components auto-adopt glass when compiled with Xcode 26.

### What auto-adopts without code changes:
- Navigation bars (become glass)
- Tab bars (become glass with blur overlay)
- Toolbars (items get glass styling)
- Search bars (get glassy background)
- Sheets (partial-height sheets get glass background)
- Alerts and confirmation dialogs
- Context menus and popovers
- System controls (toggles, sliders, steppers)

### Steps:
1. Open project in Xcode 26
2. Set deployment target to iOS 26 / macOS 26 (or use `@available` checks)
3. Build and run - observe which elements already look correct
4. Document what needs manual adjustment

---

## Phase 2: Remove Conflicting Customizations

Custom backgrounds and materials on navigation elements conflict with automatic glass adoption. Remove or conditionally disable them.

### Navigation bar backgrounds

```swift
// REMOVE or conditionally disable:
.toolbarBackground(.visible, for: .navigationBar)
.toolbarBackground(Color.blue, for: .navigationBar)

// If backward compatibility needed:
if #unavailable(iOS 26) {
    view.toolbarBackground(.visible, for: .navigationBar)
}
```

### Tab bar backgrounds

```swift
// REMOVE custom tab bar appearances:
UITabBar.appearance().backgroundColor = .systemBackground  // Remove

// REMOVE:
.toolbarBackground(.visible, for: .tabBar)
```

### Custom toolbar materials

```swift
// REMOVE:
.toolbar { ... }
.toolbarBackground(.ultraThinMaterial, for: .navigationBar)

// Glass is applied automatically
```

### Sheet backgrounds

```swift
// REMOVE on iOS 26:
.presentationBackground(.ultraThinMaterial)

// System applies Liquid Glass automatically for partial-height sheets
// Only specify detents:
.presentationDetents([.medium, .large])
```

### Inline navigation title display mode

Review `.navigationBarTitleDisplayMode(.inline)` usage. With glass, the system may handle title presentation differently.

---

## Phase 3: Enhance with Glass APIs

After removing conflicts, enhance your app with Liquid Glass APIs.

### 3a. Add backgroundExtensionEffect to hero content

Find hero images or featured content that should extend behind the navigation layer:

```swift
// Before:
Image(imageName)
    .resizable()
    .aspectRatio(contentMode: .fill)

// After:
Image(imageName)
    .resizable()
    .aspectRatio(contentMode: .fill)
    .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
    .backgroundExtensionEffect()
```

Combine with `.ignoresSafeArea(edges: .top)` on the scroll view for full-bleed effect.

### 3b. Convert custom floating buttons to glass

```swift
// Before:
Button("Action") { }
    .background(.ultraThinMaterial)
    .clipShape(Capsule())

// After:
Button("Action") { }
    .buttonStyle(.glass)
// or for interactive:
    .glassEffect(.regular.interactive())
```

### 3c. Wrap grouped glass elements in GlassEffectContainer

Find places where multiple glass elements appear near each other:

```swift
// Before:
VStack {
    Button("A") { }.glassEffect()
    Button("B") { }.glassEffect()
}

// After:
GlassEffectContainer(spacing: 16) {
    VStack {
        Button("A") { }.glassEffect()
        Button("B") { }.glassEffect()
    }
}
```

### 3d. Update toolbar layout with spacers

```swift
// Before:
.toolbar {
    ToolbarItem(placement: .topBarTrailing) {
        HStack {
            Button("Share") { }
            Button("Favorite") { }
            Button("Info") { }
        }
    }
}

// After:
.toolbar {
    ToolbarSpacer(.flexible)
    ToolbarItem { ShareLink(item: data) }
    ToolbarSpacer(.fixed)
    ToolbarItemGroup {
        Button("Favorite", systemImage: "heart") { }
        Button("More", systemImage: "ellipsis") { }
    }
    ToolbarItem {
        Button("Info", systemImage: "info") { }
    }
}
.toolbar(removing: .title)
```

### 3e. Add morphing animations with glassEffectID

For elements that appear/disappear, add coordinated morphing:

```swift
@Namespace private var namespace

GlassEffectContainer(spacing: 16) {
    if showItems {
        ForEach(items) { item in
            ItemView(item: item)
                .glassEffect(.regular, in: .rect(cornerRadius: 16))
                .glassEffectID(item.id, in: namespace)
        }
    }
    toggleButton
        .buttonStyle(.glass)
        .glassEffectID("toggle", in: namespace)
}
```

### 3f. Enable tab bar minimization

```swift
TabView { /* tabs */ }
    .tabBarMinimizeBehavior(.onScrollDown)
```

### 3g. Add sheet morphing transitions

```swift
@Namespace private var transition

// Toolbar button that opens sheet
Button("Info", systemImage: "info") { showInfo = true }
    .matchedTransitionSource(id: "info", in: transition)

// Sheet with morphing
.sheet(isPresented: $showInfo) {
    InfoView()
        .navigationTransition(.zoom(sourceID: "info", in: transition))
        .presentationDetents([.medium, .large])
}
```

---

## Phase 4: Platform-Specific Refinements

### macOS

```swift
#if os(macOS)
// Glass buttons need clear tint for proper rendering
.tint(.clear)

// Use WindowBackgroundShapeStyle for editing backgrounds
static let editingBackgroundStyle = WindowBackgroundShapeStyle.windowBackground

// Use nsColor for background colors
.background(Color(nsColor: .windowBackgroundColor))

// Toolbar delete button placement
let deleteButtonPlacement: ToolbarItemPlacement = .secondaryAction
#endif
```

### iOS (iPhone)

```swift
#if os(iOS)
// Device-specific layouts
if UIDevice.current.userInterfaceIdiom == .phone {
    // Phone-specific toolbar visibility
    .toolbarVisibility(.visible, for: .automatic)
}

// Use uiColor for background colors
.background(Color(uiColor: .systemBackground))

// Use Material for editing backgrounds
static let editingBackgroundStyle = Material.ultraThickMaterial

// Inline title display mode
.navigationBarTitleDisplayMode(.inline)

// Toolbar placement
let deleteButtonPlacement: ToolbarItemPlacement = .topBarLeading
let editButtonPlacement: ToolbarItemPlacement = .topBarTrailing
#endif
```

### iPadOS

```swift
#if os(iOS)
if UIDevice.current.userInterfaceIdiom == .pad {
    // Larger grid item sizes
    return 220.0  // vs 160.0 on iPhone
}
#endif
```

---

## Phase 5: Accessibility and Testing

### Automatic Accessibility

Liquid Glass auto-adapts for these settings (no code needed):
- Reduced Transparency
- Increased Contrast
- Reduced Motion
- Tinted Mode (iOS 26.1+)

### Manual Testing Checklist

1. **Reduced Transparency ON**: Verify glass elements are legible with increased frosting
2. **Increased Contrast ON**: Check borders and colors are visible
3. **Reduced Motion ON**: Confirm animations are subtle
4. **Dark Mode**: Test all glass elements in both light and dark color schemes
5. **Dynamic Type**: Verify glass containers resize properly with large text
6. **VoiceOver**: Ensure all glass controls have proper accessibility labels

### Manual Accessibility Override

```swift
@Environment(\.accessibilityReduceTransparency) var reduceTransparency
@Environment(\.accessibilityReduceMotion) var reduceMotion

content
    .glassEffect(reduceTransparency ? .identity : .regular)
```

---

## Backward Compatibility

### Using @available for Gradual Adoption

```swift
if #available(iOS 26, macOS 26, *) {
    view.glassEffect(.regular, in: .rect(cornerRadius: 16))
} else {
    view.background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
}
```

### Conditional Toolbar Styles

```swift
struct ToolbarLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26, *) {
            Label(configuration)
        } else {
            Label(configuration).labelStyle(.titleOnly)
        }
    }
}
```

### SF Symbol Variants

Replace circle-variant SF Symbols with plain variants on iOS 26+:

```swift
// Use symbolVariant instead of string manipulation
Image(systemName: "heart")
    .symbolVariant(isFavorite ? .fill : .none)
```

### Supporting Multiple OS Versions

```swift
struct GlassButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        if #available(iOS 26, macOS 26, *) {
            Button(title, action: action)
                .buttonStyle(.glass)
        } else {
            Button(title, action: action)
                .buttonStyle(.bordered)
        }
    }
}
```
