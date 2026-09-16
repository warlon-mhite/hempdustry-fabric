#!/usr/bin/env python3
"""Writes textures-src/beldia.mctex: Beldía's crop, all eight ages, and its buds and seeds.

    python3 textures-src/beldia.py textures-src/beldia.mctex
    python3 .claude/skills/minecraft-texture-painter/scripts/mctex.py render \
      textures-src/beldia.mctex -o src/main/resources/assets/hempdustry/textures

The crop is composed from parts rather than typed as grids, so the stalk and every leaf stay put
from seedling to harvest; edit the parts or an age here and re-run, never the .mctex by hand.
"""
import sys
W,H=16,32
PAL = """. = none
k = 34491D
d = 4C6526
m = 668530
l = 86A23F
p = ADBE58
y = C8B356
u = 9E8A3E
s = 5E5230
t = 7D6D3C
c = A4B862
g = 7E9A45
f = E4E5C4
a = D8974A
r = A4612E"""
S = {
 'fan': ["...T...",
         ".T.L.T.",
         "..LML..",
         "TLLMLLT",
         "...B..."],
 'fanw':["..T.T..",       # wide, drooping outer leaflets
         "T..L..T",
         ".LLML L".replace(' ','.'),
         "T.LML.T",
         "...B..."],
 'half':["..T..",
         "T.L.T",
         ".LML.",
         "..B.."],
 'tiny':[".T.",
         "TMT",
         ".B."],
}
TONES = {'sun':('p','l','l','m'), 'mid':('l','m','m','d'), 'shade':('m','d','d','k'),
         'dark':('d','k','k','k'), 'yellow':('y','y','u','d')}
def blank(): return [['.']*W for _ in range(H)]
def put(cv,x,y,ch):
    if 0<=x<W and 0<=y<H and ch!='.': cv[y][x]=ch
def stamp(cv,name,x0,y0,tone,flip=False):
    tip,body,mid,base=TONES[tone]
    for j,row in enumerate(S[name]):
        if flip: row=row[::-1]
        for i,ch in enumerate(row):
            put(cv,x0+i,y0+j,{'T':tip,'L':body,'M':mid,'B':base}.get(ch,'.'))
def draw(cv,x0,y0,rows):
    for j,row in enumerate(rows):
        for i,ch in enumerate(row): put(cv,x0+i,y0+j,ch)
def emit(name,cv,slices,out):
    out.append('@sheet %s %dx%d'%(name,W,H)); out.extend(''.join(r) for r in cv); out.append('')
    for tex,y in slices: out.append('@slice block/%s 0 %d 16 16 type=plant'%(tex,y))
    out.append('')


def stalk(cv, top, base_wide=False):
    for y in range(top,32): put(cv,7,y,'s')
    if base_wide:
        for y in range(28,32): put(cv,8,y,'t')

def age0():
    cv=blank(); stalk(cv,29)
    draw(cv,5,27,["p...p",
                  ".lml.",
                  "..s.."])
    stamp(cv,'tiny',6,25,'sun')
    return cv

def age1():
    cv=blank(); stalk(cv,25)
    stamp(cv,'half',2,27,'mid'); stamp(cv,'half',8,27,'mid')
    stamp(cv,'half',5,22,'sun')
    return cv

def age2():
    cv=blank(); stalk(cv,21)
    stamp(cv,'fan',0,26,'shade'); stamp(cv,'fan',8,26,'mid')
    stamp(cv,'half',2,22,'mid'); stamp(cv,'half',9,22,'sun')
    stamp(cv,'half',5,18,'sun')
    return cv

def age3():
    cv=blank()
    for (n,x,y,t) in [('fan',0,24,'dark'),('fan',9,24,'dark')]: stamp(cv,n,x,y,t)
    stalk(cv,17,True)
    for (n,x,y,t) in [('fan',1,26,'shade'),('fan',8,26,'mid'),('fan',2,20,'mid'),('fan',8,20,'sun'),
                      ('half',5,15,'sun')]:
        stamp(cv,n,x,y,t)
    return cv

def lower_body(cv, ripe):
    for (n,x,y,t) in [('fan',0,24,'dark'),('fan',9,24,'dark'),('fan',4,20,'shade'),
                      ('fan',-1,17,'shade'),('fan',10,17,'shade'),('fan',4,13,'shade')]:
        stamp(cv,n,x,y,t)
    stalk(cv,12,True)
    for (n,x,y,t) in [('fan',1,26,'yellow' if ripe else 'mid'),('fan',8,26,'mid'),('fan',2,21,'mid'),
                      ('fan',8,21,'sun'),('half',0,15,'sun'),('half',11,15,'mid'),('fan',4,16,'sun')]:
        stamp(cv,n,x,y,t)

def age4():
    cv=blank(); lower_body(cv,False)
    stamp(cv,'tiny',6,11,'sun')
    return cv

def age5():
    cv=blank(); lower_body(cv,False)
    for y in range(8,12): put(cv,7,y,'s')
    stamp(cv,'half',5,9,'sun'); stamp(cv,'half',1,12,'mid'); stamp(cv,'half',10,12,'sun')
    stamp(cv,'tiny',6,6,'sun')
    return cv

def age6():
    cv=blank(); lower_body(cv,False)
    # young colas: short, frosting, with fresh white pistils
    draw(cv,5,6,["..f..",
                 ".fcf.",
                 "pcgcp",
                 ".cfc.",
                 "fcgc.",
                 ".lcgl",
                 "..s.."])
    draw(cv,0,12,["fc..",
                  ".gcf",
                  ".cgl",
                  "..s."])
    draw(cv,12,12,[".cf.",
                   "fcg.",
                   "lgc.",
                   ".s.."])
    return cv

def age7():
    cv=blank(); lower_body(cv,True)
    draw(cv,5,3,["..a..",
                 ".rca.",
                 ".cfc.",
                 "pcgcr",
                 ".cfc.",
                 "acgcp",
                 ".fcca",
                 "pcgc.",
                 ".lcgl",
                 "..s.."])
    draw(cv,0,10,[".a..",
                  "rcf.",
                  ".cga",
                  "pfc.",
                  ".cgl",
                  "..s."])
    draw(cv,12,10,["..a.",
                   ".fcr",
                   "acg.",
                   ".cfp",
                   "lgc.",
                   ".s.."])
    return cv

HEAD = """# Beldía -- the crop, eight ages. Drawn for Hempdustry 2 (not a placeholder), UNTINTED: the models
# parent hempdustry:block/tinted_crop only so the light record's lamp and stress cue can multiply it;
# the biome is skipped for this block in HempdustryClient, so these colours are what a player sees.
#
# Generated by textures-src/beldia.py from a handful of parts -- a 7x5 fan leaf (one central
# leaflet, two diagonals, two horizontals), a 5x4 half fan, a 3x3 sprout, and hand-drawn colas -- so
# the stalk and every leaf sit in the same place from seedling to harvest. Edit the .py and re-run
# it rather than this file.
#
# Sun-bleached lime greens for a desert-grown landrace, frost-white calyx flecks, and pistils that
# are white while it flowers (age 6) and amber once it is ripe (age 7), when the oldest fan leaf
# yellows. Each age is one 16x32 sheet sliced into LOWER and UPPER, so the halves line up by
# construction; ages 0-3 have no upper half, and 8 is the upper half of age 4.
"""
out=[HEAD, '@palette beldia', PAL, '']
emit('beldia_age0', age0(), [('beldia_crop_stage0',16)], out)
emit('beldia_age1', age1(), [('beldia_crop_stage1',16)], out)
emit('beldia_age2', age2(), [('beldia_crop_stage2',16)], out)
emit('beldia_age3', age3(), [('beldia_crop_stage3',16)], out)
emit('beldia_age4', age4(), [('beldia_crop_stage8',0),('beldia_crop_stage4',16)], out)
emit('beldia_age5', age5(), [('beldia_crop_stage9',0),('beldia_crop_stage5',16)], out)
emit('beldia_age6', age6(), [('beldia_crop_stage10',0),('beldia_crop_stage6',16)], out)
emit('beldia_age7', age7(), [('beldia_crop_stage11',0),('beldia_crop_stage7',16)], out)

ITEMS = """# ---- items ----
# The BUDS are Warlon Mhite's hand-drawn Purple Kush bud, traced pixel for pixel, under a Beldía palette:
# the purple calyx goes pale lime, its lightest tone goes frost, and the pistils go amber. Same
# drawing, so the strains read as one family in a hotbar and apart by colour.
@palette beldia_buds
. = none
1 = 0D160E
2 = 6E3A1C
3 = 3E4A24
4 = 5A6E2C
5 = 6F8736
6 = 7A9440
7 = A4612E
8 = 93AD4C
9 = B8733A
a = C98A44
b = D6DCA8
c = DDA85A

@texture item/beldia_buds 16x16 palette=beldia_buds type=item
................
................
.......1111.....
.....11ca5a11...
....1c85626ca1..
....1a6a48c2a1..
...12a82aca5681.
...1a486386a841.
....114a4682ac1.
...18715a565821.
..19b8b1ac5841..
..17971684aa81..
...1bb16ac211...
....11.1111.....
................
................

# The SEEDS: the shared hemp-seed drawing, a shade sandier so a hotbar tells them apart.
@palette beldia_seeds
. = none
1 = 3A3325
2 = 8A7550
3 = A8916A

@texture item/beldia_seeds 16x16 palette=beldia_seeds type=item
................
................
.........12.....
........123.....
....21..13......
...232..........
...11......21...
.......31..321..
......211...31..
......31........
..32......23....
..132....321....
...31....11.....
................
................
................
"""
out.append(ITEMS)
open(sys.argv[1],'w').write('\n'.join(out)+'\n')
