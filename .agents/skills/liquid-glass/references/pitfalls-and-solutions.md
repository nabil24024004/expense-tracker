# Liquid Glass: Common Pitfalls and Solutions

Known issues, workarounds, and best practices when working with Liquid Glass.

## Table of Contents

1. [Visual Issues](#visual-issues)
2. [Animation Issues](#animation-issues)
3. [Performance Issues](#performance-issues)
4. [Interaction Issues](#interaction-issues)
5. [Layout Issues](#layout-issues)
6. [Migration Issues](#migration-issues)

---

## Visual Issues

### Glass elements look inconsistent near each other

**Problem:** Nearby glass elements have different visual appearances, breaking visual harmony.

**Cause:** Glass cannot sample other glass. Without a shared container, each glass element creates its own sampling region.

**Solution:** Wrap all nearby glass elements in a `GlassEffectContainer`:

```swift
// BAD - inconsistent glass
VStack {
    view1.glassEffect()
    view2.glassEffect()
}

// GOOD - shared sampling region
GlassEffectContainer(spacing: 16) {
    VStack {
        view1.glassEffect()
        view2.glassEffect()
    }
}
```

### Glass button rendering on macOS

**Problem:** Glass buttons appear incorrectly tinted on macOS.

**Solution:** Apply `.tint(.clear)` on macOS:

```swift
Button("Action") { }
    .buttonStyle(.glass)
    #if os(macOS)
    .tint(.clear)
    #endif
```

### Sheet background conflicts

**Problem:** Custom `.presentationBackground()` conflicts with automatic glass styling.

**Solution:** Remove `.presentationBackground()` on iOS 26. The system applies Liquid Glass automatically for partial-height sheets:

```swift
// BAD
.sheet(isPresented: $show) {
    Content()
        .presentationBackground(.ultraThinMaterial) // Remove this
        .presentationDetents([.medium, .large])
}

// GOOD
.sheet(isPresented: $show) {
    Content()
        .presentationDetents([.medium, .large])
    // System handles glass background
}
```

### Text illegibility over glass backgrounds

**Problem:** Text is hard to read when overlaid on `backgroundExtensionEffect` images.

**Solution:** Layer gradients over extended backgrounds:

```swift
Image(imageName)
    .backgroundExtensionEffect()
    .overlay(alignment: .bottom) {
        VStack {
            Text("Title").font(.largeTitle).bold().foregroundColor(.white)
        }
        .padding()
    }
    .safeAreaInset(edge: .bottom) {
        textContent
            .background(LinearGradient(
                colors: [.clear, .black.opacity(0.5)],
                startPoint: .top, endPoint: .bottom
            ))
    }
```

---

## Animation Issues

### rotationEffect causes glass shape morphing

**Problem:** When applying `rotationEffect(_:anchor:)` to views with glass effects, the glass shape morphs unpredictably during animation instead of rotating smoothly.

**Solution:** Bridge to UIKit using `UIViewRepresentable` with `UIGlassEffect`:

```swift
struct RotatableGlassView: UIViewRepresentable {
    let rotation: Angle

    func makeUIView(context: Context) -> UIVisualEffectView {
        let effect = UIGlassEffect()
        let view = UIVisualEffectView(effect: effect)
        return view
    }

    func updateUIView(_ uiView: UIVisualEffectView, context: Context) {
        uiView.transform = CGAffineTransform(rotationAngle: rotation.radians)
    }
}
```

### Menu morphing animation glitches

**Problem:** Applying glass effects directly to Menu labels causes animation artifacts - morphing starts/ends as rectangle, then snaps to circle.

**Solution (varies by OS version):**

iOS 26.0-26.0.1:
```swift
Menu { /* items */ } label: {
    Label("More", systemImage: "ellipsis")
}
.glassEffect(.regular.interactive())
```

iOS 26.1+:
```swift
struct GlassMenuStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding()
            .glassEffect(.regular)
    }
}

Menu { /* items */ } label: {
    Label("More", systemImage: "ellipsis")
}
.buttonStyle(GlassMenuStyle())
```

### GlassEffectContainer breaks Menu morphing

**Problem:** On iOS 26.1, wrapping Menus in `GlassEffectContainer` causes morphing to break (works on 26.0).

**Solution:** Test across OS versions. Consider placing Menu glass effects outside the container or file feedback with Apple.

### Morphing doesn't animate

**Problem:** Elements with `glassEffectID` don't morph when shown/hidden.

**Checklist:**
1. Elements are in the same `GlassEffectContainer`?
2. Each element has `glassEffectID` with the same `@Namespace`?
3. State change is wrapped in `withAnimation`?
4. Views are conditionally rendered (not just hidden/opacity)?

```swift
// BAD - opacity doesn't trigger morph
view.opacity(isVisible ? 1 : 0)
    .glassEffectID("item", in: namespace)

// GOOD - conditional rendering triggers morph
if isVisible {
    view.glassEffect()
        .glassEffectID("item", in: namespace)
}
```

---

## Performance Issues

### Multiple glass effects cause frame drops

**Problem:** Many individual glass effects create excessive `CABackdropLayer` instances, each requiring 3 offscreen textures.

**Solution:** Group glass elements in `GlassEffectContainer`:

```swift
// BAD - 5 separate backdrop layers (15 textures)
ForEach(items) { item in
    ItemView(item: item).glassEffect()
}

// GOOD - shared backdrop layer
GlassEffectContainer(spacing: 16) {
    ForEach(items) { item in
        ItemView(item: item).glassEffect()
    }
}
```

### ScrollView with many glass elements

**Problem:** Long lists with glass effects cause memory pressure and jank.

**Solution:** Use `LazyVStack`/`LazyHStack` and minimize glass elements in scrollable content. Apply glass to floating overlays, not list items:

```swift
// BAD - glass on every list item
ScrollView {
    LazyVStack {
        ForEach(items) { item in
            ItemRow(item: item).glassEffect()  // Too many!
        }
    }
}

// GOOD - glass on floating controls only
ScrollView {
    LazyVStack {
        ForEach(items) { item in
            ItemRow(item: item)  // No glass on content
        }
    }
}
.overlay(alignment: .bottomTrailing) {
    FloatingButton().glassEffect(.regular.interactive())
}
```

---

## Interaction Issues

### Hit-testing failures on glass buttons

**Problem:** Glass effect buttons only register taps on the content (symbol/text), not the entire glass area.

**Solution:** Use `contentShape()` to define the hit region:

```swift
Button("Action") { }
    .padding()
    .glassEffect(.regular, in: .rect(cornerRadius: 16))
    .contentShape(.rect(cornerRadius: 16))
```

### Interactive glass not responding

**Problem:** `.interactive()` glass doesn't scale/bounce on interaction.

**Checklist:**
- `.interactive()` is iOS only (not macOS)
- The view must be interactive (Button, gesture attached)
- Verify the glass modifier is applied correctly: `.glassEffect(.regular.interactive())`

---

## Layout Issues

### backgroundExtensionEffect clipping

**Problem:** Content with `backgroundExtensionEffect` gets clipped by parent views.

**Solution:** Ensure proper frame and ignore safe area:

```swift
Image(imageName)
    .resizable()
    .aspectRatio(contentMode: .fill)
    .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
    .clipped()
    .backgroundExtensionEffect()

// On the parent ScrollView:
.ignoresSafeArea(edges: .top)
```

### Glass container sizing

**Problem:** `GlassEffectContainer` doesn't size correctly.

**Solution:** Apply frame constraints to the inner content, not the container:

```swift
GlassEffectContainer(spacing: 16) {
    VStack {
        ForEach(items) { item in
            ItemView(item: item)
                .glassEffect()
                .glassEffectID(item.id, in: namespace)
        }
    }
    .frame(width: 74)  // Frame on inner content
}
```

---

## Migration Issues

### Custom toolbar backgrounds override glass

**Problem:** Existing `.toolbarBackground()` modifiers prevent glass from appearing.

**Solution:** Remove or conditionally disable custom toolbar backgrounds:

```swift
.toolbar { /* items */ }
// Remove: .toolbarBackground(.visible, for: .navigationBar)
// Remove: .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
```

### Old Material usage on navigation elements

**Problem:** `.background(.ultraThinMaterial)` on navigation containers conflicts with glass.

**Solution:** Remove material backgrounds from navigation elements. Glass handles this automatically. Keep materials only on content backgrounds where appropriate:

```swift
// OK - material on content editing background
#if os(macOS)
static let editingBackgroundStyle = WindowBackgroundShapeStyle.windowBackground
#else
static let editingBackgroundStyle = Material.ultraThickMaterial
#endif
```

### Navigation transitions not working

**Problem:** Sheet morphing transitions or navigation transitions break.

**Solution:** Ensure the view is inside `NavigationStack` or `NavigationSplitView`:

```swift
// BAD - no navigation context
.sheet(isPresented: $show) {
    ContentView()
        .navigationTransition(.zoom(sourceID: "id", in: namespace))
}

// GOOD - wrapped in NavigationStack
.sheet(isPresented: $show) {
    NavigationStack {
        ContentView()
    }
    .navigationTransition(.zoom(sourceID: "id", in: namespace))
}
```

### Symbol effect crashes on older OS

**Problem:** `.symbolEffect(.drawOn)` or other iOS 26 symbol effects crash on older OS versions.

**Solution:** Use `@available` checks:

```swift
if #available(iOS 26, *) {
    image.transition(.init(.symbolEffect(.drawOn)))
} else {
    image.transition(.opacity)
}
```
