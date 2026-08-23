#!/usr/bin/env python3
"""Generator for the Decarboxylator's container GUI texture.

A 256x256 sheet is far too big to hand-author as an .mctex character matrix (that would be 256
rows of 256 characters), so this script plays the same role for it: the art stays re-editable
and diffable instead of being a binary nobody can change. Re-run it after editing:

    python3 textures-src/decarboxylator_gui.py

Output: src/main/resources/assets/hempdustry/textures/gui/container/decarboxylator.png

Every piece of chrome comes from `gui_common`, which carries vanilla's own panel, slot, flame and
arrow art measured out of the client jar — see `check_panel.py`. Nothing here invents a colour.

Layout must stay in step with DecarboxylatorScreenHandler (slot positions) and
DecarboxylatorScreen (sprite regions), which hold the same numbers as constants.

         [T1] ===>
  (flame)[T2] ===>  [ OUT ]      one arrow per tray, each filling on its own timer
  [FUEL] [T3] ===>               result slot centred on the tray column, furnace-sized

Reads left to right, because that is the direction every vanilla machine reads in. The trays used
to be a row across the top firing arrows downward into a slot beneath -- a layout no vanilla
container uses, and it showed. Fuel keeps its own column with the flame above it, as a furnace has
it; the trays are the furnace's input slot stacked three deep.

Slots are empty. **No hint icons** — vanilla puts a background sprite in a slot only where the slot
takes one specific *shape* of thing that nothing else fits (an armour piece, a smithing template),
and never in a furnace's fuel slot, which is exactly the case here. What may go in a tray is a
recipe lookup, so any icon would be a half-truth as soon as a datapack adds a strain.
"""

import gui_common as gui

# Slot origins — must match DecarboxylatorScreenHandler.
TRAYS = [(62, 17), (62, 35), (62, 53)]
OUTPUT = (121, 35)
FUEL = (26, 54)

# Where the screen draws the live overlays. Each arrow is level with its own tray, and the gaps
# either side of it are vanilla's: 7px from the input slot's bevel, 8px to the result well.
FLAME_XY = (26, 36)
ARROW_X = 85

# Sprite regions read by DecarboxylatorScreen, in the margin right of the 176-wide panel.
FLAME_AT = (176, 0)
ARROW_AT = (176, 14)

sheet = gui.Sheet()
sheet.panel()

# ---- machine slots ----
sheet.slot(*FUEL)
for tray in TRAYS:
    sheet.slot(*tray)
# Furnace-sized result well: vanilla marks "this is what comes out" by size, not by position.
sheet.big_slot(*OUTPUT)

sheet.player_inventory()

# ---- unlit indicators, baked into the panel for the live overlays to reveal against ----
# Furnace-style: the dim flame and the dim arrow belong to the background; the screen draws the lit
# flame and the filled arrow over the top, and those sprites carry their own panel-grey backdrop so
# a partial draw covers what it replaces.
sheet.blit(gui.FLAME_OFF, gui.FLAME_OFF_COLORS, *FLAME_XY)
for _, tray_y in TRAYS:
    sheet.blit(gui.ARROW_OFF, gui.ARROW_OFF_COLORS, ARROW_X, tray_y)

# ---- lit sprites in the margin ----
# Flame: revealed from the bottom up, the way a furnace's burns down.
sheet.blit(gui.FLAME_ON, gui.FLAME_ON_COLORS, *FLAME_AT)
# Arrow: revealed left-to-right as a tray cooks.
sheet.blit(gui.ARROW_ON, gui.ARROW_ON_COLORS, *ARROW_AT)

if __name__ == "__main__":
    sheet.write(gui.resources("textures", "gui", "container", "decarboxylator.png"))
