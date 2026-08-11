# Credits

Hempdustry is not all under one licence. The code and the music are separate works by separate
people, distributed together in one jar. This file records who made what and under which terms, and
it ships inside the jar so the attribution travels with the files.

---

## Music

**Nefuß** — two tracks, written for this mod:

| Track | In game | Listen |
|---|---|---|
| **Moonlight** | Music Disc — *Nefuß - Moonlight* | [Bandcamp](https://nefu1.bandcamp.com/track/moonlight) · [SoundCloud](https://soundcloud.com/user-427551104/moonlight) |
| **Robadob** | Music Disc — *Nefuß - Robadob* | [Bandcamp](https://nefu1.bandcamp.com/track/robadob-2) · [SoundCloud](https://soundcloud.com/user-427551104/robadob) |

Licensed under
[**Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International**](https://creativecommons.org/licenses/by-nc-sa/4.0/)
(CC BY-NC-SA 4.0). Files: `assets/hempdustry/sounds/records/moonlight.ogg` and `robadob.ogg`.

The mod plays each track with its artist and title on screen, so ordinary play credits Nefuß by
itself. Anything beyond ordinary play — reuploading the audio, remixing it, or distributing it
outside this mod — is governed by the licence above and not by the mod's own.

### If you are putting Hempdustry in a modpack

Read this rather than assuming, because the answer differs from most mods:

- **Attribution (BY)** — keep this file in the jar, or credit Nefuß in the pack's own credits. Do
  not strip the tracks' names from the language files.
- **NonCommercial (NC)** — the music may not be used commercially. Bundling the mod unchanged in a
  freely distributed pack is fine. **A pack that earns money — ad revenue, a paid download, a
  monetised launcher or a subscription server — is not covered by this licence**, and neither the
  mod's AGPL nor a pack platform's terms grant it. If that is your situation, ask Nefuß first; the
  Bandcamp links above are the way to reach them.
- **ShareAlike (SA)** — if you *adapt* the music (re-encode beyond format conversion, remix, cut),
  the adaptation has to carry the same licence. Redistributing the mod as-is is not an adaptation.
- If any of that is a problem for your pack, the two `.ogg` files can be removed from a copy of the
  jar and everything else keeps working — the discs simply play nothing.

---

## Code

**Warlon Mhite**, under the [GNU Affero General Public License v3.0](LICENSE) (`AGPL-3.0-only`).
This covers everything in `src/main/java` and the mod's data files.

Large Language Models — chiefly Anthropic's Claude, through Claude Code — assisted with parts of the
implementation, documentation, localisation, placeholder art and design review. See the README's AI
disclaimer.

## Art

**Warlon Mhite** — the hand-drawn textures, the music disc art, the paintings, and the mod icon,
under the same AGPL-3.0-only as the rest of the mod.

Some textures are **generated placeholders** rather than final art, and are marked as such in the
project's own notes. Where a placeholder is derived from a vanilla Minecraft texture, only its
palette has been changed; those remain Mojang's work and are used in the same way any resource pack
uses them.

## Third-party

Built against [Fabric Loader and Fabric API](https://fabricmc.net/) (Apache-2.0). Neither is
redistributed inside this jar — both are the player's own installation.
