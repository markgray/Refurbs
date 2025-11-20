AdaptiveIconSample
==================

An adaptive icon, or AdaptiveIconDrawable, can display differently depending on individual device
capabilities and user theming. Adaptive icons are primarily used by the launcher on the home screen,
but they can also be used in shortcuts, the Settings app, sharing dialogs, and the overview screen.
Adaptive icons are used across all Android form factors.

In contrast to bitmap images, adaptive icons can adapt to different use cases:
    - Different shapes: an adaptive icon can display a variety of shapes across different device
    models. For example, it can display a circular shape on one OEM device, and display a squircle
    (a shape between a square and a circle) on another device. Each device OEM must provide a mask,
    which the system uses to render all adaptive icons with the same shape.

    - Visual effects: an adaptive icon supports a variety of engaging visual effects, which display
    when users place or move the icon around the home screen.

    - User theming: starting with Android 13 (API level 33), users can theme their adaptive icons.
    If a user enables themed app icons in their system settings, and the launcher supports this
    feature, the system uses the coloring of the user's chosen wallpaper and theme to determine the
    tint color of the app icons for apps that have a monochrome layer in their adaptive icon.
    Starting with Android 16 QPR 2, Android automatically themes app icons for apps that don't
    provide their own.

See res/mipmap-anydpi/ic_launcher1.xml and res/mipmap-anydpi/ic_launcher2.xml for the adaptive-icon's
