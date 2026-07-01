# Real-World Liquid Glass Patterns: Apple Landmarks Sample App

Code patterns extracted from Apple's official Landmarks sample app demonstrating production-quality Liquid Glass implementation. Source: [Landmarks: Building an app with Liquid Glass](https://developer.apple.com/documentation/SwiftUI/Landmarks-Building-an-app-with-Liquid-Glass)

## Table of Contents

1. [App Architecture](#app-architecture)
2. [Glass Badge Overlay with Morphing](#glass-badge-overlay-with-morphing)
3. [Background Extension for Hero Images](#background-extension-for-hero-images)
4. [Flexible Header with Parallax Scrolling](#flexible-header-with-parallax-scrolling)
5. [Glass Toolbar Layout](#glass-toolbar-layout)
6. [NavigationSplitView with Glass](#navigationsplitview-with-glass)
7. [Platform-Specific Constants](#platform-specific-constants)
8. [Collection Detail with Editing Mode](#collection-detail-with-editing-mode)
9. [Inspector Panel Pattern](#inspector-panel-pattern)

---

## App Architecture

The app uses a single `@Observable` data model injected via `.environment()`:

```swift
@main
struct LandmarksApp: App {
    @State private var modelData = ModelData()

    var body: some Scene {
        WindowGroup {
            LandmarksSplitView()
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

Key: Window size is tracked via `onGeometryChange` for responsive header calculations.

---

## Glass Badge Overlay with Morphing

A floating badge overlay using `GlassEffectContainer` with coordinated morphing animations. This is the most comprehensive glass pattern in the app.

```swift
struct BadgesView: View {
    @Environment(ModelData.self) private var modelData
    @State private var isExpanded: Bool = false
    @Namespace private var namespace

    var body: some View {
        // GlassEffectContainer groups all glass elements for shared sampling
        GlassEffectContainer(spacing: 16.0) {
            VStack(alignment: .center, spacing: 20.0) {
                if isExpanded {
                    VStack(spacing: 14.0) {
                        ForEach(modelData.earnedBadges) {
                            BadgeLabel(badge: $0)
                                // Glass with rounded rectangle shape
                                .glassEffect(.regular, in: .rect(cornerRadius: 24.0))
                                // ID for morphing animations
                                .glassEffectID($0.id, in: namespace)
                        }
                    }
                }

                Button {
                    withAnimation {
                        isExpanded.toggle()
                    }
                } label: {
                    ToggleBadgesLabel(isExpanded: isExpanded)
                        .frame(width: 24.0, height: 32.0)
                }
                // Glass button style
                .buttonStyle(.glass)
                #if os(macOS)
                .tint(.clear)  // Required on macOS
                #endif
                // ID for morphing with badge elements
                .glassEffectID("togglebutton", in: namespace)
            }
            .frame(width: 74.0)
        }
    }
}
```

**Overlay placement** via custom ViewModifier:

```swift
private struct ShowsBadgesViewModifier: ViewModifier {
    func body(content: Content) -> some View {
        ZStack {
            content
            HStack {
                Spacer()
                VStack {
                    Spacer()
                    BadgesView()
                        .padding()
                }
            }
        }
    }
}

extension View {
    func showsBadges() -> some View {
        modifier(ShowsBadgesViewModifier())
    }
}
```

Usage: `.showsBadges()` on any view that should display the floating badge overlay.

---

## Background Extension for Hero Images

Two patterns for extending images behind the navigation layer.

### Featured Item (Landing Page)

```swift
struct LandmarkFeaturedItemView: View {
    @Environment(ModelData.self) var modelData
    let landmark: Landmark

    var body: some View {
        NavigationLink(value: landmark) {
            Image(decorative: landmark.backgroundImageName)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
                .clipped()
                .backgroundExtensionEffect()  // Extends behind navigation
                .overlay(alignment: .bottom) {
                    VStack {
                        Text("Featured Landmark")
                            .font(.subheadline).fontWeight(.bold)
                            .foregroundColor(.white).opacity(0.8)
                        Text(landmark.name)
                            .font(.largeTitle).fontWeight(.bold)
                            .foregroundColor(.white)
                        Button("Learn More") {
                            modelData.path.append(landmark)
                        }
                        .buttonStyle(.borderedProminent)
                        .padding(.bottom, 6.0)
                    }
                    .padding(.bottom, 40.0)
                }
        }
        .buttonStyle(.plain)
    }
}
```

### Detail View (Full Bleed)

```swift
struct LandmarkDetailView: View {
    let landmark: Landmark

    var body: some View {
        ScrollView(.vertical) {
            VStack(alignment: .leading, spacing: 14.0) {
                Image(landmark.backgroundImageName)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
                    .backgroundExtensionEffect()  // Key modifier
                    .flexibleHeaderContent()       // Parallax stretching

                VStack(alignment: .leading) {
                    Text(landmark.name).font(.title).fontWeight(.bold)
                    Text(landmark.description).textSelection(.enabled)
                }
                .padding(.leading, 26.0)
                .padding(.trailing, 52.0)
            }
        }
        .flexibleHeaderScrollView()   // Scroll tracking for parallax
        .ignoresSafeArea(edges: .top) // Full bleed
        .toolbar(removing: .title)    // Clean glass toolbar
    }
}
```

---

## Flexible Header with Parallax Scrolling

Custom view modifiers that create a stretching parallax effect when scrolling past the top bounds.

```swift
@Observable private class FlexibleHeaderGeometry {
    var offset: CGFloat = 0
}

private struct FlexibleHeaderContentModifier: ViewModifier {
    @Environment(ModelData.self) private var modelData
    @Environment(FlexibleHeaderGeometry.self) private var geometry

    func body(content: Content) -> some View {
        let height = (modelData.windowSize.height / 2) - geometry.offset
        content
            .frame(height: height)
            .padding(.bottom, geometry.offset)
            .offset(y: geometry.offset)
    }
}

private struct FlexibleHeaderScrollViewModifier: ViewModifier {
    @State private var geometry = FlexibleHeaderGeometry()

    func body(content: Content) -> some View {
        content
            .onScrollGeometryChange(for: CGFloat.self) { geometry in
                min(geometry.contentOffset.y + geometry.contentInsets.top, 0)
            } action: { _, offset in
                geometry.offset = offset
            }
            .environment(geometry)
    }
}

// Extensions for clean API
extension ScrollView {
    @MainActor func flexibleHeaderScrollView() -> some View {
        modifier(FlexibleHeaderScrollViewModifier())
    }
}

extension View {
    func flexibleHeaderContent() -> some View {
        modifier(FlexibleHeaderContentModifier())
    }
}
```

Usage pattern:
1. Apply `.flexibleHeaderContent()` to the hero image
2. Apply `.flexibleHeaderScrollView()` to the parent `ScrollView`
3. Track window size via `onGeometryChange` at the app level

---

## Glass Toolbar Layout

Comprehensive toolbar using spacers, groups, and glass-styled items:

```swift
.toolbar {
    ToolbarSpacer(.flexible)                    // Push items to trailing

    ToolbarItem {
        ShareLink(item: landmark, preview: landmark.sharePreview)
    }

    ToolbarSpacer(.fixed)                       // Fixed gap

    ToolbarItemGroup {                           // Grouped actions
        LandmarkFavoriteButton(landmark: landmark)
        LandmarkCollectionsMenu(landmark: landmark)
    }

    ToolbarSpacer(.fixed)

    ToolbarItem {
        Button("Info", systemImage: "info") {
            modelData.selectedLandmark = landmark
            modelData.isLandmarkInspectorPresented.toggle()
        }
    }
}
.toolbar(removing: .title)                      // Clean glass appearance
```

---

## NavigationSplitView with Glass

The root navigation with sidebar, detail, search, and inspector:

```swift
struct LandmarksSplitView: View {
    @Environment(ModelData.self) var modelData
    @State private var preferredColumn: NavigationSplitViewColumn = .detail

    var body: some View {
        @Bindable var modelData = modelData

        NavigationSplitView(preferredCompactColumn: $preferredColumn) {
            List {
                Section {
                    ForEach(NavigationOptions.mainPages) { page in
                        NavigationLink(value: page) {
                            Label(page.name, systemImage: page.symbolName)
                        }
                    }
                }
            }
            .navigationDestination(for: NavigationOptions.self) { page in
                NavigationStack(path: $modelData.path) {
                    page.viewForPage()
                }
                .navigationDestination(for: Landmark.self) { landmark in
                    LandmarkDetailView(landmark: landmark)
                }
                .showsBadges()  // Glass badge overlay
            }
            .frame(minWidth: 150)
        } detail: {
            NavigationStack(path: $modelData.path) {
                NavigationOptions.landmarks.viewForPage()
            }
            .navigationDestination(for: Landmark.self) { landmark in
                LandmarkDetailView(landmark: landmark)
            }
            .showsBadges()  // Glass badge overlay
        }
        .searchable(text: $modelData.searchString, prompt: "Search")
        .inspector(isPresented: $modelData.isLandmarkInspectorPresented) {
            if let landmark = modelData.selectedLandmark {
                LandmarkDetailInspectorView(
                    landmark: landmark,
                    inspectorIsPresented: $modelData.isLandmarkInspectorPresented
                )
            }
        }
    }
}
```

---

## Platform-Specific Constants

Centralized constants with platform-aware sizing:

```swift
struct Constants {
    // Glass-related constants
    static let badgeGlassSpacing: CGFloat = 16.0   // GlassEffectContainer spacing
    static let badgeCornerRadius: CGFloat = 24.0    // Glass shape corner radius
    static let badgeSize: CGFloat = 52.0
    static let badgeFrameWidth: CGFloat = 74.0

    // Platform-specific grid sizes
    @MainActor static var collectionGridItemMinSize: CGFloat {
        #if os(iOS)
        if UIDevice.current.userInterfaceIdiom == .pad {
            return 220.0
        } else {
            return 160.0
        }
        #else
        return 220.0
        #endif
    }

    // Platform-specific background styles
    #if os(macOS)
    static let editingBackgroundStyle = WindowBackgroundShapeStyle.windowBackground
    #else
    static let editingBackgroundStyle = Material.ultraThickMaterial
    #endif
}
```

---

## Collection Detail with Editing Mode

Animated editing transitions with symbol effects:

```swift
struct CollectionDetailView: View {
    @Environment(\.colorScheme) var colorScheme
    @State var isEditing: Bool = false

    var body: some View {
        ScrollView(.vertical) {
            if isEditing {
                CollectionDetailEditingView(collection: collection, ...)
            } else {
                CollectionDetailDisplayView(collection: collection)
            }
        }
        #if os(iOS)
        .background(Color(uiColor: isEditing && colorScheme == .light
            ? .systemGray5 : .systemBackground))
        #endif
        #if os(macOS)
        .background(Color(nsColor: isEditing && colorScheme == .light
            ? .secondarySystemFill : .windowBackgroundColor))
        #endif
        .toolbar(removing: .title)
        .toolbar {
            ToolbarItem(placement: editButtonPlacement) {
                Button {
                    withAnimation { isEditing.toggle() }
                } label: {
                    if isEditing {
                        Image(systemName: "checkmark")
                            .transition(.editButtonTransition())
                    } else {
                        Text("Edit")
                    }
                }
            }
        }
    }
}

extension AnyTransition {
    @MainActor static func editButtonTransition() -> AnyTransition {
        .asymmetric(
            insertion: .init(.symbolEffect(.drawOn)),
            removal: .opacity
        )
    }
}
```

---

## Inspector Panel Pattern

Used for supplementary content alongside detail views:

```swift
// On the NavigationSplitView or parent:
.inspector(isPresented: $modelData.isLandmarkInspectorPresented) {
    if let landmark = modelData.selectedLandmark {
        LandmarkDetailInspectorView(
            landmark: landmark,
            inspectorIsPresented: $modelData.isLandmarkInspectorPresented
        )
    } else {
        EmptyView()
    }
}

// The inspector view itself:
struct LandmarkDetailInspectorView: View {
    let landmark: Landmark
    @Binding var inspectorIsPresented: Bool

    var body: some View {
        ScrollView {
            // Map, activities, elevation info
            LandmarkDetailMapView(landmark: landmark)
            // ... more detail content
        }
        #if os(iOS)
        .toolbarVisibility(
            UIDevice.current.userInterfaceIdiom == .phone ? .visible : .hidden,
            for: .automatic
        )
        .toolbar {
            if UIDevice.current.userInterfaceIdiom == .phone {
                Button {
                    inspectorIsPresented.toggle()
                } label: {
                    Label("Dismiss", systemImage: "xmark")
                }
            }
        }
        #endif
    }
}
```
