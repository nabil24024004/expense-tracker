# Liquid Glass API Reference

Complete API reference for all SwiftUI Liquid Glass modifiers and types. Always verify against the latest Apple documentation as APIs may change between betas.

## Table of Contents

1. [glassEffect Modifier](#glasseffect-modifier)
2. [Glass Struct](#glass-struct)
3. [GlassEffectContainer](#glasseffectcontainer)
4. [glassEffectID](#glasseffectid)
5. [glassEffectUnion](#glasseffectunion)
6. [backgroundExtensionEffect](#backgroundextensioneffect)
7. [Button Styles](#button-styles)
8. [Toolbar Glass APIs](#toolbar-glass-apis)
9. [TabView Glass APIs](#tabview-glass-apis)
10. [Sheet Glass APIs](#sheet-glass-apis)
11. [Accessibility APIs](#accessibility-apis)
12. [UIKit Bridge](#uikit-bridge)

---

## glassEffect Modifier

Applies the Liquid Glass material to a view.

```swift
func glassEffect<S: Shape>(
    _ glass: Glass = .regular,
    in shape: S = DefaultGlassEffectShape,  // capsule by default
    isEnabled: Bool = true
) -> some View
```

**Parameters:**
- `glass`: The glass material style (`.regular`, `.clear`, `.identity`)
- `shape`: The shape of the glass effect background
- `isEnabled`: Whether the glass effect is active

**Supported shapes:**
- `.capsule` (default)
- `.circle`
- `RoundedRectangle(cornerRadius: 16)`
- `.rect(cornerRadius: .containerConcentric)` - auto-aligns with container corners
- `.ellipse`
- Any custom `Shape` conformance

**Examples:**

```swift
// Default capsule shape
Text("Glass").padding().glassEffect()

// Rounded rectangle
view.glassEffect(.regular, in: .rect(cornerRadius: 24))

// Circle
view.glassEffect(.regular, in: .circle)

// Concentric corners (matches container)
view.glassEffect(.regular, in: .rect(cornerRadius: .containerConcentric))

// Conditionally enabled
view.glassEffect(.regular, isEnabled: showGlass)
```

**Availability:** iOS 26.0+, macOS 26.0+, iPadOS 26.0+, watchOS 26.0+, tvOS 26.0+, visionOS 26.0+

---

## Glass Struct

The `Glass` struct defines the material appearance.

### Static Properties

| Property | Description |
|----------|-------------|
| `.regular` | Standard glass: medium transparency, full adaptation. For toolbars, buttons, nav bars |
| `.clear` | High transparency, limited adaptation. For media-rich backgrounds with bold/bright overlay content |
| `.identity` | No glass effect. For conditionally disabling glass |

### Instance Methods

```swift
// Add a tint color
func tint(_ color: Color) -> Glass

// Enable interactive behavior (scaling, bouncing, shimmering on touch)
func interactive() -> Glass
```

**Method chaining:** Order-independent.

```swift
.glassEffect(.regular.tint(.blue).interactive())
// same as
.glassEffect(.regular.interactive().tint(.blue))
```

**Tint examples:**

```swift
.glassEffect(.regular.tint(.blue))
.glassEffect(.regular.tint(.purple.opacity(0.6)))
.glassEffect(.regular.tint(Color("accentColor")))
```

**Interactive behavior (iOS only):**
- Scales on press
- Bouncing animation on release
- Shimmering effect
- Touch-point illumination radiating to nearby glass elements
- Tap and drag responsiveness

---

## GlassEffectContainer

Groups multiple glass elements into a unified composition. Critical for visual consistency because glass cannot sample other glass.

```swift
struct GlassEffectContainer<Content: View>: View {
    init(spacing: CGFloat? = nil, @ViewBuilder content: () -> Content)
    init(@ViewBuilder content: () -> Content)
}
```

**Parameters:**
- `spacing`: Controls morphing threshold. Elements within this distance visually blend during transitions.

**Why required:** Each glass element creates a `CABackdropLayer` with 3 offscreen textures. Containers share sampling regions, reducing texture count and improving rendering performance.

```swift
GlassEffectContainer(spacing: 30.0) {
    // All glass elements share the same sampling region
    Button("A") { }.glassEffect()
    Button("B") { }.glassEffect()
    Button("C") { }.glassEffect()
}
```

---

## glassEffectID

Links glass elements for morphing transitions within a `GlassEffectContainer`.

```swift
func glassEffectID<ID: Hashable>(
    _ id: ID,
    in namespace: Namespace.ID
) -> some View
```

**Requirements for morphing:**
1. Elements must be in the same `GlassEffectContainer`
2. Each view needs `glassEffectID` with a shared `@Namespace`
3. Conditional show/hide triggers morphing animation
4. Wrap state change in `withAnimation`

```swift
@Namespace private var namespace
@State private var showExtra = false

GlassEffectContainer(spacing: 16) {
    Button("Toggle") {
        withAnimation(.bouncy) { showExtra.toggle() }
    }
    .glassEffect()
    .glassEffectID("toggle", in: namespace)

    if showExtra {
        Button("Extra") { }
            .glassEffect()
            .glassEffectID("extra", in: namespace)
    }
}
```

---

## glassEffectUnion

Combines multiple glass effects into a single unified glass shape.

```swift
func glassEffectUnion<ID: Hashable>(
    _ id: ID,
    in namespace: Namespace.ID
) -> some View
```

Use when multiple separate views should appear as one continuous glass surface.

---

## backgroundExtensionEffect

Extends and blurs visual content beyond a view's bounds, creating continuous backgrounds behind navigation elements.

```swift
func backgroundExtensionEffect() -> some View
```

**Use cases:**
- Hero images that extend behind toolbars/navigation bars
- Detail view images extending behind sidebar/inspector
- Featured content spanning multiple UI sections

**Works with:**
- `NavigationSplitView` detail columns
- Sidebars and inspectors
- Toolbars and navigation bars
- Custom overlay controls

```swift
Image(imageName)
    .resizable()
    .aspectRatio(contentMode: .fill)
    .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
    .backgroundExtensionEffect()
```

**Best practices:**
- Test legibility across different images, themes, and contexts
- Combine with gradients for text readability over extended backgrounds
- Use with `.safeAreaInset()` for overlay content positioning
- Pairs well with `.clipShape()` for rounded corners

---

## Button Styles

### .glass

Standard glass button for secondary/common actions:

```swift
Button("Action") { }
    .buttonStyle(.glass)
```

### .glassProminent

Prominent glass button with full tint surface for primary actions:

```swift
Button("Save") { }
    .buttonStyle(.glassProminent)
```

**Note:** `.confirmationAction` toolbar placement auto-applies `.glassProminent`.

**macOS note:** Apply `.tint(.clear)` on glass buttons for proper rendering:

```swift
Button("Action") { }
    .buttonStyle(.glass)
    #if os(macOS)
    .tint(.clear)
    #endif
```

---

## Toolbar Glass APIs

Toolbar items automatically receive glass styling when compiled with Xcode 26.

### ToolbarSpacer

```swift
ToolbarSpacer(.flexible)  // Expands to fill available space
ToolbarSpacer(.fixed)     // System-default fixed spacing
```

### ToolbarItemGroup

Groups related actions with consistent glass styling:

```swift
ToolbarItemGroup {
    Button("Draw", systemImage: "pencil") { }
    Button("Erase", systemImage: "eraser") { }
}
```

### Placement-Driven Styling

| Placement | Auto-applied style |
|-----------|-------------------|
| `.confirmationAction` | `.glassProminent` |
| `.cancellationAction` | Standard glass |
| `.primaryAction` | Grouping-capable |
| `.topBarLeading` / `.topBarTrailing` | Navigation areas |
| `.secondaryAction` | Overflow menu (macOS) |

### Removing Title

```swift
.toolbar(removing: .title)
```

---

## TabView Glass APIs

### Automatic Glass

Tab bars auto-adopt glass when compiled with Xcode 26 SDK. No code changes needed.

### Tab Bar Minimize

```swift
TabView { /* tabs */ }
    .tabBarMinimizeBehavior(.onScrollDown)
```

### Bottom Accessory

```swift
.tabViewBottomAccessory {
    Button("Quick Action") { }
        .glassEffect(.regular.interactive())
}
```

### Search Role Tab

```swift
Tab("Search", systemImage: "magnifyingglass", role: .search) {
    SearchView()
}
```

---

## Sheet Glass APIs

Partial-height sheets auto-receive Liquid Glass background.

**Requirements:**
- Specify at least one partial-height detent: `.presentationDetents([.medium, .large])`
- Do NOT use `.presentationBackground()` - system handles glass automatically
- View must be inside `NavigationStack` or `NavigationSplitView` for transitions

### Morphing Sheet Transitions

```swift
@Namespace private var transition

// Source button
Button("Info", systemImage: "info") { showSheet = true }
    .matchedTransitionSource(id: "info", in: transition)

// Sheet content
.sheet(isPresented: $showSheet) {
    InfoView()
        .navigationTransition(.zoom(sourceID: "info", in: transition))
        .presentationDetents([.medium, .large])
}
```

---

## Accessibility APIs

Liquid Glass automatically adapts for accessibility settings. No code changes required for:

- **Reduced Transparency:** Increases frosting for clarity
- **Increased Contrast:** Applies stark colors and borders
- **Reduced Motion:** Tones down animations/elastic effects
- **Tinted Mode (iOS 26.1+):** User-controlled opacity increase

### Manual Override (when needed)

```swift
@Environment(\.accessibilityReduceTransparency) var reduceTransparency

var body: some View {
    content
        .glassEffect(reduceTransparency ? .identity : .regular)
}
```

---

## UIKit Bridge

For cases where SwiftUI glass has rendering issues (e.g., rotation animations):

```swift
// UIKit glass effect
let glassEffect = UIGlassEffect()
let effectView = UIVisualEffectView(effect: glassEffect)

// In UIViewRepresentable
struct GlassView: UIViewRepresentable {
    func makeUIView(context: Context) -> UIVisualEffectView {
        let effect = UIGlassEffect()
        return UIVisualEffectView(effect: effect)
    }
    func updateUIView(_ uiView: UIVisualEffectView, context: Context) { }
}
```

Use this bridge when:
- `rotationEffect` causes glass shape morphing artifacts
- You need precise control over glass rendering in complex layouts
- Integrating glass into existing UIKit view hierarchies
