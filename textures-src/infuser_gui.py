#!/usr/bin/env python3
"""Generator for the Infuser's container GUI texture.

Same role a .mctex plays for the 16x16 art: keeps a 256x256 sheet editable and diffable instead of
being a binary nobody can change. Re-run after editing:

    python3 textures-src/infuser_gui.py

Output: src/main/resources/assets/hempdustry/textures/gui/container/infuser.png

Chrome comes from `gui_common` — vanilla's panel, slot and flame art, measured out of the client
jar and checked by `check_panel.py`.

Must stay in step with InfuserScreenHandler (slots) and InfuserScreen (sprite regions).

               [hemp]
    (milk)                ==|==!==>  [ OUT ]  | dark NOTCH = collectable from here
               [hemp]       (flame)            ! bright MARK = next grade (moves with the ratio)

Milk has no slot. It is poured into the tub in the world, like water into a cauldron, so the left
column only reports it: an empty pot outline that fills white once there is milk in the tub. It
replaced a milk slot and a bucket-return slot, two slots of GUI that between them said one bit.

The flame is centred under the bar rather than in that column. It is not a fuel gauge -- it reports
whether the block BELOW the Infuser is hot -- and under the bar it governs, "no heat, no progress"
reads without a tooltip. Milk and heat are the batch's two preconditions, and both are pictures
rather than slots because nothing goes in either.

The notch is the whole point of the bar. A plain fill would say "cooking"; the notch says
"collectable from here, but not finished" -- which is the actual decision the player is making.

Slots are empty. **No hint icons** -- see the note in decarboxylator_gui.py. Both hemp slots take
either type, so any icon drawn in them would have been a half-truth from the start.
"""

import gui_common as gui

# Slots — must match InfuserScreenHandler.
HEMP = (62, 17)
WASHED = (62, 53)
OUTPUT = (138, 35)

# Live overlays — must match InfuserScreen.
BAR_XY, BAR_WH = (84, 39), (44, 5)
# Centred on the bar (84 + 44/2 = 106, less half the flame's 14) and on the row of the bottom two
# slots (53..68, less half of 14 about its centre).
FLAME_XY = (99, 54)
# Centred in the left column the milk slot used to occupy (x 26..41) and on the middle row, between
# the two hemp slots (17..32 and 53..68).
MILK_XY = (27, 37)

# Sprite regions in the sheet margin.
BAR_AT = (176, 0)
NOTCH_AT = (176, 5)
MARK_AT = (178, 5)
FLAME_AT = (180, 5)
MILK_AT = (194, 5)

# The milk indicator, 14x12. Unlit, it is an empty pot in the unlit-indicator grey -- an outline, not
# the flame's filled silhouette, because "empty vessel" is the thing it has to say. Lit, the same pot
# holds milk: a white surface a pixel below the brim, where the block model puts it.
MILK_OFF = [
    "##..........##",
    ".#..........#.",
    ".#..........#.",
    ".#..........#.",
    ".#..........#.",
    ".#..........#.",
    ".#..........#.",
    ".#..........#.",
    ".#..........#.",
    "..##########..",
    "..#........#..",
    ".##........##.",
]
MILK_ON = [
    "##..........##",
    ".#..........#.",
    ".#wwwwwwwwww#.",
    ".#mmmmmmmmms#.",
    ".#mmmmmmmmms#.",
    ".#mmmmmmmmms#.",
    ".#mmmmmmmmms#.",
    ".#mmmmmmmmms#.",
    ".#ssssssssss#.",
    "..##########..",
    "..#........#..",
    ".##........##.",
]
MILK_OFF_COLORS = {"#": gui.SLOT_BG, ".": gui.BG}
MILK_ON_COLORS = {
    "#": gui.SLOT_BG,
    ".": gui.BG,
    "w": (255, 255, 255, 255),  # the surface
    "m": (236, 232, 222, 255),  # ECE8DE
    "s": (205, 199, 186, 255),  # CDC7BA, its own shadow
}

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

for slot in (HEMP, WASHED):
    sheet.slot(*slot)
# Furnace-sized result well: vanilla marks what comes out by size.
sheet.big_slot(*OUTPUT)

sheet.player_inventory()

# ---- empty bar track, sunk into the panel exactly as a slot is ----
sheet.well(*BAR_XY, *BAR_WH)

# The notch is NOT baked into the panel. The bar is scaled to each batch's own job rather than to
# the clock, so the minimum-time mark moves with the washed ratio -- a third of the way along for an
# all-washed batch, half for a half-washed one. Both marks are drawn at runtime by InfuserScreen.

# ---- unlit heat and milk indicators, so the lit sprites have something to replace ----
sheet.blit(gui.FLAME_OFF, gui.FLAME_OFF_COLORS, *FLAME_XY)
sheet.blit(MILK_OFF, MILK_OFF_COLORS, *MILK_XY)

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
sheet.blit(MILK_ON, MILK_ON_COLORS, *MILK_AT)

if __name__ == "__main__":
    sheet.write(gui.resources("textures", "gui", "container", "infuser.png"))
