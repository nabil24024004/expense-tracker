# Platform-Specific Liquid Glass Implementation

Detailed platform considerations for implementing Liquid Glass across Apple platforms.

## Table of Contents

1. [iOS 26 (iPhone)](#ios-26-iphone)
2. [iPadOS 26](#ipados-26)
3. [macOS 26 (Tahoe)](#macos-26-tahoe)
4. [watchOS 26](#watchos-26)
5. [tvOS 26](#tvos-26)
6. [visionOS 26](#visionos-26)
7. [Cross-Platform Patterns](#cross-platform-patterns)

---

## iOS 26 (iPhone)

### Glass Features Available
- Full `.glassEffect()` with all styles (`.regular`, `.clear`, `.identity`)
- `.interactive()` glass - scaling, bouncing, shimmering on touch
- `GlassEffectContainer` with morphing transitions
- `.backgroundExtensionEffect()` for hero images
- `.buttonStyle(.glass)` and `.buttonStyle(.glassProminent)`
- Automatic glass tab bars with `.tabBarMinimizeBehavior(.onScrollDown)`
- Glass sheets with `.presentationDetents([.medium, .large])`
- Glass search bars via `.searchable()`

### iPhone-Specific Patterns

```swift
#if os(iOS)
// Phone-specific toolbar visibility
if UIDevice.current.userInterfaceIdiom == .phone {
    .toolbarVisibility(.visible, for: .automatic)
    .toolbar {
        Button { inspectorIsPresented.toggle() } label: {
            Label("Toggle Inspector", systemImage: "info.circle")
        }
    }
}

// Inline navigation title
.navigationBarTitleDisplayMode(.inline)

// Smaller grid item sizes for compact screens
static var gridItemMinSize: CGFloat {
    if UIDevice.current.userInterfaceIdiom == .phone {
        return 160.0
    }
    return 240.0
}
#endif
```

### Navigation on iPhone
- `NavigationSplitView` collapses to single column
- Use `preferredCompactColumn` to control default view
- Search bar dynamically moves based on scroll position

---

## iPadOS 26

### iPad-Specific Features
- Floating Liquid Glass sidebar
- Side-by-side split view with glass navigation
- Inspector panels with glass styling
- Larger touch targets for glass buttons

### iPad Layout Patterns

```swift
#if os(iOS)
if UIDevice.current.userInterfaceIdiom == .pad {
    // Larger grid sizes
    static let gridItemMinSize: CGFloat = 220.0
    static let gridItemEditingMinSize: CGFloat = 180.0

    // Hide phone-specific toolbar items
    .toolbarVisibility(.hidden, for: .automatic)
}
#endif
```

### Split View with Glass

```swift
NavigationSplitView(preferredCompactColumn: $preferredColumn) {
    // Sidebar - auto-glass on iPad
    List {
        ForEach(NavigationOptions.mainPages) { page in
            NavigationLink(value: page) {
                Label(page.name, systemImage: page.symbolName)
            }
        }
    }
    .frame(minWidth: 150)
} detail: {
    NavigationStack(path: $modelData.path) {
        MainContentView()
    }
}
.searchable(text: $modelData.searchString, prompt: "Search")
.inspector(isPresented: $modelData.isInspectorPresented) {
    InspectorView()
}
```

---

## macOS 26 (Tahoe)

### macOS-Specific Considerations

**Glass button tinting:** Always apply `.tint(.clear)` to glass buttons on macOS:

```swift
Button("Action") { }
    .buttonStyle(.glass)
    #if os(macOS)
    .tint(.clear)
    #endif
```

**Background styles:** Use `WindowBackgroundShapeStyle` instead of `Material`:

```swift
#if os(macOS)
static let editingBackgroundStyle = WindowBackgroundShapeStyle.windowBackground
.background(Color(nsColor: .windowBackgroundColor))
.background(Color(nsColor: isEditing ? .secondarySystemFill : .windowBackgroundColor))
#else
static let editingBackgroundStyle = Material.ultraThickMaterial
.background(Color(uiColor: .systemBackground))
#endif
```

**Toolbar placement:** Use `.secondaryAction` for overflow items:

```swift
#if os(macOS)
let deleteButtonPlacement: ToolbarItemPlacement = .secondaryAction
let editButtonPlacement: ToolbarItemPlacement = .automatic
#elseif os(iOS)
let deleteButtonPlacement: ToolbarItemPlacement = .topBarLeading
let editButtonPlacement: ToolbarItemPlacement = .topBarTrailing
#endif
```

**Window configuration:**

```swift
WindowGroup {
    ContentView()
        .frame(minWidth: 375.0, minHeight: 375.0)
}
```

### macOS Navigation
- `NavigationSplitView` shows persistent sidebar
- Window toolbar integrates glass automatically
- Inspector panels slide from trailing edge
- Search bar appears in toolbar area

---

## watchOS 26

### Glass on Apple Watch
- Simplified glass effects for smaller display
- System handles glass on navigation elements automatically
- Limited custom glass usage recommended (screen real estate)
- TabView with glass tab bar adapts to watch form factor

### Considerations
- Avoid complex `GlassEffectContainer` layouts on watch
- Use `.regular` style only (`.clear` less effective on small screens)
- Test with all watch face complications active
- `.interactive()` works with Digital Crown interactions

---

## tvOS 26

### Glass on Apple TV
- Focus-driven glass interactions
- Glass elements highlight on focus
- Remote-friendly touch targets
- Background extension effects work with media-rich content

### Focus-Aware Glass

```swift
Button("Play") { }
    .buttonStyle(.glass)
    .focusable()
    // Glass automatically responds to focus state on tvOS
```

---

## visionOS 26

### Glass in Spatial Computing
- Glass integrates with spatial UI paradigms
- Window ornaments use glass automatically
- Volumetric content benefits from glass overlays
- System handles glass depth and parallax

---

## Cross-Platform Patterns

### Conditional Compilation Pattern

```swift
#if os(macOS)
// macOS-specific implementation
#elseif os(iOS)
if UIDevice.current.userInterfaceIdiom == .pad {
    // iPad-specific
} else {
    // iPhone-specific
}
#elseif os(watchOS)
// Watch-specific
#elseif os(tvOS)
// TV-specific
#elseif os(visionOS)
// Vision-specific
#endif
```

### Shared Architecture Pattern

Use a single codebase with platform adaptations:

```swift
@main
struct MyApp: App {
    @State private var modelData = ModelData()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(modelData)
                .frame(minWidth: 375.0, minHeight: 375.0)
                .onGeometryChange(for: CGSize.self) { geometry in
                    geometry.size
                } action: {
                    modelData.windowSize = $0
                }
        }
    }
}
```

### Responsive Layout Constants

```swift
struct LayoutConstants {
    @MainActor static var gridItemMinSize: CGFloat {
        #if os(iOS)
        if UIDevice.current.userInterfaceIdiom == .pad {
            return 220.0  // iPad
        } else {
            return 160.0  // iPhone
        }
        #elseif os(macOS)
        return 240.0
        #elseif os(watchOS)
        return 80.0
        #else
        return 200.0
        #endif
    }
}
```

### NavigationSplitView Across Platforms

```swift
NavigationSplitView(preferredCompactColumn: $preferredColumn) {
    // Sidebar: glass on all platforms
    SidebarContent()
} detail: {
    // Detail: NavigationStack for deep navigation
    NavigationStack(path: $path) {
        DetailContent()
    }
    .navigationDestination(for: Item.self) { item in
        ItemDetailView(item: item)
    }
}
// Global search - positioned by system per platform
.searchable(text: $searchString, prompt: "Search")
// Inspector - system handles presentation per platform
.inspector(isPresented: $showInspector) {
    InspectorContent()
}
```
