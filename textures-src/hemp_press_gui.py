#!/usr/bin/env python3
"""Generator for the Hemp Press's container GUI texture.

Same role a .mctex plays for the 16x16 art: keeps a 256x256 sheet editable and diffable instead of
being a binary nobody can change. Re-run after editing:

    python3 textures-src/hemp_press_gui.py

Output: src/main/resources/assets/hempdustry/textures/gui/container/hemp_press.png

Chrome comes from `gui_common` -- vanilla's panel, slot, arrow and flame art, measured out of the
client jar and checked by `check_panel.py`.

Must stay in step with HempPressScreenHandler (slots) and HempPressScreen (sprite regions).

    [ IN ]  ===>  [ OUT ]
             (flame)

A furnace's layout with the fuel column removed, because there is no fuel: the heat comes from the
block underneath. The flame therefore sits UNDER THE ARROW rather than under a slot -- it is not a
gauge for anything in this screen, it is a report about a neighbour, and putting it in a slot column
would imply a relationship that does not exist. Same placement and the same argument as the
Infuser's.

Slots are empty. **No hint icons** -- see the note in decarboxylator_gui.py.
"""

import gui_common as gui

# Slots — must match HempPressScreenHandler.
INPUT = (56, 35)
OUTPUT = (116, 35)

# Live overlays — must match HempPressScreen.
ARROW_XY = (79, 35)
# Centred under the arrow (79 + 24/2 = 91, less half the flame's 14) and clear of y=72, where the
# "Inventory" label sits on a 166-tall panel.
FLAME_XY = (84, 55)

# Sprite regions in the sheet margin.
ARROW_AT = (176, 0)
FLAME_AT = (176, 16)

sheet = gui.Sheet()
sheet.panel()

sheet.slot(*INPUT)
# Furnace-sized result well: vanilla marks what comes out by size.
sheet.big_slot(*OUTPUT)

sheet.player_inventory()

# Furnace-style: the dim arrow and the dim flame belong to the background; the screen draws the
# filled arrow and the lit flame over the top, and those sprites carry their own panel-grey backdrop
# so they cover the unlit ones cleanly.
sheet.blit(gui.ARROW_OFF, gui.ARROW_OFF_COLORS, *ARROW_XY)
sheet.blit(gui.FLAME_OFF, gui.FLAME_OFF_COLORS, *FLAME_XY)

# ---- sprites in the margin ----
sheet.blit(gui.ARROW_ON, gui.ARROW_ON_COLORS, *ARROW_AT)
sheet.blit(gui.FLAME_ON, gui.FLAME_ON_COLORS, *FLAME_AT)

if __name__ == "__main__":
    sheet.write(gui.resources("textures", "gui", "container", "hemp_press.png"))
