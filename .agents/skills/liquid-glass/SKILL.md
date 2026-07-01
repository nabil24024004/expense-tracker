---
name: liquid-glass
description: Build and migrate iOS, macOS, iPadOS, watchOS, tvOS, and visionOS apps with Apple's Liquid Glass design system (iOS 26+, macOS 26 Tahoe+). Use when creating new SwiftUI apps targeting Apple's 2025+ platforms, migrating existing apps to Liquid Glass, applying .glassEffect(), .backgroundExtensionEffect(), .buttonStyle(.glass), GlassEffectContainer, glass toolbars, glass tab bars, glass sheets, or any Liquid Glass UI work. Also use when the user asks about modern Apple design, navigation patterns with glass, or SwiftUI best practices for the latest OS versions.
---

# Liquid Glass Design System for Apple Platforms

Build and migrate SwiftUI apps using Apple's Liquid Glass design language introduced at WWDC 2025. This skill covers iOS 26, iPadOS 26, macOS 26 (Tahoe), watchOS 26, tvOS 26, and visionOS 26.

## Important: Use Latest Documentation

Always fetch the latest Apple developer documentation when implementing Liquid Glass features. The APIs may evolve between OS betas. Key documentation URLs to reference:

- `https://developer.apple.com/documentation/SwiftUI/Applying-Liquid-Glass-to-custom-views`
- `https://developer.apple.com/documentation/swiftui/view/glasseffect(_:in:)`
- `https://developer.apple.com/documentation/SwiftUI/Landmarks-Building-an-app-with-Liquid-Glass`
- `https://developer.apple.com/videos/play/wwdc2025/323/` (WWDC25 Session: Build a SwiftUI app with the new design)

## Core Concept

Liquid Glass is a translucent, dynamic material exclusively for the **navigation layer** (toolbars, tab bars, buttons, controls) that floats above app content. It bends and refracts light in real-time, responds to device motion with specular highlights, and adapts continuously to background content.

**Never apply glass to content itself** (lists, tables, media, text blocks). Glass is for controls and navigation only.

## Quick Start: Key APIs

### 1. Glass Effect on Custom Views

```swift
// Basic - capsule shape (default)
Text("Label")
    .padding()
    .glassEffect()

// With shape and style
Image(systemName: "heart.fill")
    .padding()
    .glassEffect(.regular, in: .rect(cornerRadius: 16))

// Tinted glass
Text("Tinted")
    .padding()
    .glassEffect(.regular.tint(.blue))

// Interactive glass (scales, bounces, shimmers on touch - iOS only)
Button("Tap Me") { }
    .glassEffect(.regular.interactive())
```

### 2. Glass Styles

| Style | Use Case | Transparency |
|-------|----------|-------------|
| `.regular` | Standard UI: toolbars, buttons, nav bars | Medium |
| `.clear` | Media-rich backgrounds where content is bold/bright | High |
| `.identity` | Conditionally disable glass (accessibility) | None |

### 3. GlassEffectContainer (Critical)

Glass cannot sample other glass. Nearby glass elements MUST share a container for visual consistency and morphing.

```swift
GlassEffectContainer(spacing: 30.0) {
    Button("Action 1") { }
        .glassEffect()
        .glassEffectID("btn1", in: namespace)

    Button("Action 2") { }
        .glassEffect()
        .glassEffectID("btn2", in: namespace)
}
```

### 4. Morphing Transitions

Use `@Namespace` + `glassEffectID` inside a `GlassEffectContainer` for smooth glass morphing:

```swift
@Namespace private var namespace
@State private var isExpanded = false

GlassEffectContainer(spacing: 16) {
    if isExpanded {
        ForEach(items) { item in
            ItemView(item: item)
                .glassEffect(.regular, in: .rect(cornerRadius: 24))
                .glassEffectID(item.id, in: namespace)
        }
    }
    Button {
        withAnimation { isExpanded.toggle() }
    } label: { Label("Toggle", systemImage: "chevron.down") }
    .buttonStyle(.glass)
    .glassEffectID("toggle", in: namespace)
}
```

### 5. Background Extension Effect

Extends and blurs visual content behind navigation elements (toolbars, sidebars, inspectors):

```swift
Image(landmark.backgroundImageName)
    .resizable()
    .aspectRatio(contentMode: .fill)
    .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
    .backgroundExtensionEffect()
```

### 6. Glass Buttons

```swift
// Standard glass button
Button("Action") { }
    .buttonStyle(.glass)

// Prominent glass button (for primary actions)
Button("Save") { }
    .buttonStyle(.glassProminent)
```

### 7. Toolbars with Glass

Toolbar items automatically receive glass styling. Use `ToolbarSpacer` and `ToolbarItemGroup` for layout:

```swift
.toolbar {
    ToolbarSpacer(.flexible)
    ToolbarItem { ShareLink(item: data, preview: preview) }
    ToolbarSpacer(.fixed)
    ToolbarItemGroup {
        Button("Favorite", systemImage: "heart") { }
        Button("Add", systemImage: "plus") { }
    }
    ToolbarItem {
        Button("Info", systemImage: "info") { }
    }
}
.toolbar(removing: .title) // Remove title for clean glass toolbar
```

### 8. Tab Bars

Tab bars automatically adopt glass when compiled with Xcode 26:

```swift
TabView {
    Tab("Home", systemImage: "house") { HomeView() }
    Tab("Search", systemImage: "magnifyingglass") { SearchView() }
}
.tabBarMinimizeBehavior(.onScrollDown) // Collapse tab bar on scroll
```

### 9. Sheets with Glass

Partial-height sheets automatically get Liquid Glass backgrounds:

```swift
.sheet(isPresented: $showSheet) {
    SheetContent()
        .presentationDetents([.medium, .large])
    // Do NOT add .presentationBackground() - system handles it
}
```

## Migration Workflow (Existing Apps)

For detailed migration steps, see [references/migration-guide.md](references/migration-guide.md).

**Summary:**
1. Compile with Xcode 26 SDK - system components auto-adopt glass
2. Remove custom toolbar backgrounds (`.toolbarBackground`)
3. Replace custom materials on navigation elements with glass modifiers
4. Wrap grouped glass elements in `GlassEffectContainer`
5. Add `.backgroundExtensionEffect()` to hero images
6. Update SF Symbol variants (circle variants -> none variants on iOS 26+)
7. Test accessibility (Reduced Transparency, Increased Contrast, Reduced Motion)

## Platform Considerations

For platform-specific details, see [references/platform-specifics.md](references/platform-specifics.md).

**Key differences:**
- **macOS**: Use `.tint(.clear)` on glass buttons for proper rendering; use `WindowBackgroundShapeStyle.windowBackground` instead of `Material` for editing backgrounds
- **iOS (iPhone)**: `.interactive()` works; use `UIDevice.current.userInterfaceIdiom` for layout
- **iPadOS**: Larger grid sizes; sidebar adaptable tab views
- **Conditional compilation**: Use `#if os(macOS)` / `#if os(iOS)` for platform-specific code

## Common Pitfalls

For detailed pitfalls and solutions, see [references/pitfalls-and-solutions.md](references/pitfalls-and-solutions.md).

**Critical issues:**
- Glass elements outside `GlassEffectContainer` produce inconsistent visuals
- `rotationEffect` on glass views causes shape morphing - bridge to UIKit with `UIGlassEffect`
- Menu labels with glass cause animation artifacts - use custom `ButtonStyle`
- Hit-testing only registers on content, not glass area - use `contentShape()` to fix
- Multiple glass effects = multiple `CABackdropLayer` instances (3 offscreen textures each) - use containers to group

## Real-World Example Patterns

For complete code patterns from Apple's Landmarks sample app, see [examples/landmarks-patterns.md](examples/landmarks-patterns.md).

## Architecture Best Practices

1. **NavigationSplitView** as app root with glass sidebar
2. **NavigationStack** for deep navigation within detail columns
3. **@Observable** data model with `@Environment` injection
4. **FlexibleHeader** pattern: stretching hero images with scroll-linked parallax
5. **.inspector()** for supplementary detail panels
6. **.searchable()** for global search (auto-styled with glass)
7. Use `ToolbarSpacer(.flexible)` and `ToolbarSpacer(.fixed)` for toolbar layout
8. Prefer symbol-based buttons with text labels in toolbars
9. Use `.symbolVariant()` modifier for SF Symbol state changes
10. Use `.symbolEffect(.drawOn)` for animated icon transitions
