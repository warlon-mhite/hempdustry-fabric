#!/usr/bin/env python3
"""Generator for the Infuser's container GUI texture.

Same role a .mctex plays for the 16x16 art: keeps a 256x256 sheet editable and diffable instead of
being a binary nobody can change. Re-run after editing:

    python3 textures-src/infuser_gui.py

Output: src/main/resources/assets/hempdustry/textures/gui/container/infuser.png

Chrome comes from `gui_common` — vanilla's panel, slot and flame art, measured out of the client
jar and checked by `check_panel.py`.

Must stay in step with InfuserScreenHandler (slots) and InfuserScreen (sprite regions).

    [milk]     [hemp]
                       ==|==!==>  [ OUT ]  | dark NOTCH = collectable from here
    [bucket]   [hemp]    (flame)            ! bright MARK = next grade (moves with the ratio)

The left column is in on top, out underneath: milk is emptied into the tub on contact and its
bucket returned below, which is what lets several batches be queued up in advance.

The flame is centred under the bar rather than in that column. It is not a fuel gauge and neither
bucket feeds it -- it reports whether the block BELOW the Infuser is hot -- so parking it between
the two bucket slots implied a relationship that does not exist. Under the bar it governs, "no
heat, no progress" reads without a tooltip.

The notch is the whole point of the bar. A plain fill would say "cooking"; the notch says
"collectable from here, but not finished" -- which is the actual decision the player is making.

Slots are empty. **No hint icons** -- see the note in decarboxylator_gui.py. Both hemp slots take
either type, so any icon drawn in them would have been a half-truth from the start.
"""

import gui_common as gui

# Slots — must match InfuserScreenHandler.
MILK = (26, 17)
BUCKET = (26, 53)
HEMP = (62, 17)
WASHED = (62, 53)
OUTPUT = (138, 35)

# Live overlays — must match InfuserScreen.
BAR_XY, BAR_WH = (84, 39), (44, 5)
# Centred on the bar (84 + 44/2 = 106, less half the flame's 14) and on the row of the bottom two
# slots (53..68, less half of 14 about its centre).
FLAME_XY = (99, 54)

# Sprite regions in the sheet margin.
BAR_AT = (176, 0)
NOTCH_AT = (176, 5)
MARK_AT = (178, 5)
FLAME_AT = (180, 5)

# The simmer bar is gold because vanilla's one horizontal container bar — the brewing stand's fuel
# gauge — is gold, and this ramp is that sprite's own five tones. It also has to stay a colour: the
# two marks that ride on it are vanilla's slot-dark and white, and a white fill would swallow one of
# them. See InfuserScreen for what each mark answers.
BAR_RAMP = [
    (179, 107, 25, 255),   # B36B19
    (255, 243, 45, 255),   # FFF32D
    (255, 193, 0, 255),    # FFC100
    (185, 147, 28, 255),   # B9931C
    (191, 90, 0, 255),     # BF5A00
]

sheet = gui.Sheet()
sheet.panel()

for slot in (MILK, BUCKET, HEMP, WASHED):
    sheet.slot(*slot)
# Furnace-sized result well: vanilla marks what comes out by size.
sheet.big_slot(*OUTPUT)

sheet.player_inventory()

# ---- empty bar track, sunk into the panel exactly as a slot is ----
sheet.well(*BAR_XY, *BAR_WH)

# The notch is NOT baked into the panel. The bar is scaled to each batch's own job rather than to
# the clock, so the minimum-time mark moves with the washed ratio -- a third of the way along for an
# all-washed batch, half for a half-washed one. Both marks are drawn at runtime by InfuserScreen.

# ---- unlit heat indicator, so the lit sprite has something to replace ----
sheet.blit(gui.FLAME_OFF, gui.FLAME_OFF_COLORS, *FLAME_XY)

# ---- sprites in the margin ----
bar_w, bar_h = BAR_WH
for row, color in enumerate(BAR_RAMP[:bar_h]):
    sheet.rect(BAR_AT[0], BAR_AT[1] + row, bar_w, 1, color)

# The dark notch, drawn over the fill so it stays readable once the bar passes it. Vanilla's slot
# shadow, which is legible against both the empty track and the gold.
sheet.rect(*NOTCH_AT, 2, 7, gui.SLOT_DARK)

# The moving "next grade lands here" mark. White, so it separates cleanly from the dark notch AND
# stays readable over the gold fill -- the two marks answer different questions and must never be
# mistaken for each other: dark = "can I take it", bright = "should I wait".
sheet.rect(*MARK_AT, 2, 7, gui.BEVEL_LIGHT)

sheet.blit(gui.FLAME_ON, gui.FLAME_ON_COLORS, *FLAME_AT)

if __name__ == "__main__":
    sheet.write(gui.resources("textures", "gui", "container", "infuser.png"))
