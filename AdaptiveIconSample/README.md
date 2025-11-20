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
and see AndroidManifest.xml for where they are used.

BTW: If you click on the three icons in the launcher you will observe some puzzling behavior that
results from having 3 launchable activities in the same application:
    - If you put one of the activities in the background and click a different icon, the first
    acctivity will return to the foreground instead of launching the second activity.

    - Even when one of the other activities is not in the background, the launcher will often launch
    a different activity from the one that the Icon is supposed to launch.

    - Since the activity element for Activity3 lacks a android:label attribute it uses the android:label
    attribute of the application ("AdaptiveIconSample") but its app bar is sometimes "AdaptiveIconSample"
    and sometimes "Icon 1" for no reason I can fathom.

Bottom line is if you put multiple launchable activities in the same application expect some oddities.
