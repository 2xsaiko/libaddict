# libaddict

_(**ad**vanced **dict**ionary)_

This library provides advanced localization support for Minecraft. The format
is straightforward, more featureful and easier to write than Minecraft's
builtin localization format:

```
// Comments are supported with two slashes

// Include another language file:
// Resolves subdirectories:
include subdir/blah.str
// or absolute paths:
include /lang/something.str
include othermod:lang/something.str

// Defining strings (what you'll be doing for the most part):
block.examplemod.example: Example

// Multiline strings:
gui.examplemod.example:
  This is a multiline string,
  smallest indent found across all lines will be removed,

  and paragraph breaks also work.
```
