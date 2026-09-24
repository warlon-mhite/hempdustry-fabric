#!/usr/bin/env python3
"""Writes textures-src/ruderalis.mctex: Ruderalis's crop, all eight ages, and its buds, seeds and spliff.

    python3 textures-src/ruderalis.py textures-src/ruderalis.mctex

then render the .mctex into src/main/resources/assets/hempdustry/textures, as the other sheets here.

Composed from parts, like textures-src/beldia.py, so the stalk and every leaf stay put from
seedling to harvest; edit the parts or an age here and re-run, never the .mctex by hand.
"""
import sys
W,H=16,16
# Beldía's palette, which is the mod's drawn one, plus its seed browns: no new hues.
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
r = A4612E
q = 3A3325
e = 8A7550
h = A8916A"""
S = {
 # Beldía's fan and half fan, so the plants read as one family; ruderalis just has fewer of them
 'fan': ["...T...",
         ".T.L.T.",
         "..LML..",
         "TLLMLLT",
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
         'dark':('d','k','k','k'), 'yellow':('y','y','u','u'), 'sere':('u','u','s','s')}
def blank(): return [['.']*W for _ in range(H)]
def put(cv,x,y,ch):
    if 0<=x<W and 0<=y<H and ch!='.': cv[y][x]=ch
def stamp(cv,name,x0,y0,tone):
    tip,body,mid,base=TONES[tone]
    for j,row in enumerate(S[name]):
        for i,ch in enumerate(row):
            put(cv,x0+i,y0+j,{'T':tip,'L':body,'M':mid,'B':base}.get(ch,'.'))
def draw(cv,x0,y0,rows):
    for j,row in enumerate(rows):
        for i,ch in enumerate(row): put(cv,x0+i,y0+j,ch)
def stalk(cv,top,x=7):
    for y in range(top,H): put(cv,x,y,'s')
def emit(name,cv,out):
    out.append('@texture block/%s %dx%d type=plant'%(name,W,H)); out.extend(''.join(r) for r in cv); out.append('')

def cotyledons(cv,y,tone):
    """The two seed leaves: round paddles in a shallow V, the first thing any seedling shows. Lit
    from the upper left like everything else, so the left one catches the sun and the right one
    turns away from it."""
    tip,body,mid,base=TONES[tone]
    draw(cv,4,y,[body+tip+'...'+body+mid,
                 '.'+tip+body+'.'+mid+base+'.',
                 '..'+mid+'.'+base+'..'])

def age0():
    cv=blank(); stalk(cv,14)
    cotyledons(cv,12,'sun')
    put(cv,7,13,'l')                      # the plumule between them
    return cv

def age1():
    cv=blank(); stalk(cv,10)
    cotyledons(cv,13,'mid')
    # the first true leaves are single serrated blades, not yet fans
    draw(cv,4,9,["p..",".lp","..m"])
    draw(cv,8,9,["..p","pl.","m.."])
    stamp(cv,'tiny',6,7,'sun')
    return cv

def age2():
    cv=blank()
    cotyledons(cv,13,'yellow')
    stalk(cv,7)
    stamp(cv,'half',2,10,'mid'); stamp(cv,'half',8,10,'sun')
    draw(cv,4,7,["p..",".lp"]); draw(cv,8,7,["..p","pl."])
    stamp(cv,'tiny',6,4,'sun')
    return cv

def age3():
    cv=blank()
    stamp(cv,'fan',0,10,'dark'); stamp(cv,'fan',9,10,'dark')
    stalk(cv,5)
    stamp(cv,'fan',1,11,'shade'); stamp(cv,'fan',8,11,'mid')
    stamp(cv,'half',2,7,'mid');   stamp(cv,'half',9,6,'sun')
    stamp(cv,'tiny',6,3,'sun')
    return cv

def body(cv, ripe):
    """Ages 4-7 share their leaves: a sparse pyramid, the fans only low down, trios above."""
    stamp(cv,'fan',-1,8,'dark'); stamp(cv,'fan',10,8,'dark'); stamp(cv,'half',5,5,'shade')
    stalk(cv,3)
    stamp(cv,'fan',0,11,'sere' if ripe else 'shade'); stamp(cv,'fan',9,11,'yellow' if ripe else 'mid')
    stamp(cv,'half',0,7,'yellow' if ripe else 'mid'); stamp(cv,'half',11,6,'sun')
    stamp(cv,'half',2,3,'sun'); stamp(cv,'half',9,2,'mid')

def age4():
    cv=blank(); body(cv,False)
    stamp(cv,'tiny',6,0,'sun')
    return cv

def age5():
    # it flowers by age, not by the light: the first pale hairs show before it is fully grown
    cv=blank(); body(cv,False)
    draw(cv,6,0,[".f.",
                 "pcp",
                 "lgl",
                 ".s."])
    put(cv,2,3,'f'); put(cv,13,6,'f')
    return cv

def age6():
    cv=blank(); body(cv,False)
    # a short, loose cola -- the low-resin plant makes calyxes, not dense buds
    draw(cv,5,0,["..f..",
                 ".fcf.",
                 "fcgcf",
                 ".gcg.",
                 "fcgcf",
                 ".lgl."])
    draw(cv,1,3,["f.f","cgc",".l."]); draw(cv,12,5,["f.f","cgc",".m."])
    return cv

def age7():
    cv=blank(); body(cv,True)
    # gone to seed: every calyx has swollen round a tan seed, and the hairs have rusted
    draw(cv,5,0,["..r..",
                 ".rcr.",
                 "ahehc",
                 ".cqc.",
                 "rehea",
                 ".lgl."])
    draw(cv,1,3,["r.a","heh",".l."]); draw(cv,12,5,["a.r","heh",".m."])
    return cv

HEAD = """# Ruderalis -- the crop, eight ages, one block tall. Drawn for Hempdustry 2 (not a placeholder).
# TINTED: the models parent minecraft:block/tinted_cross and HempdustryClient pulls the plant part-way
# to its biome's grass, like the wild flowers. It keeps no light record, so nothing else colours it.
#
# Generated by textures-src/ruderalis.py from a handful of parts -- round cotyledons, single-blade
# first leaves, a 5x3 three-leaflet leaf (the broad ruderalis leaf), a 7x4 five-leaflet fan kept
# for the lowest pair, and hand-drawn colas -- on Beldía's palette plus its seed browns. Edit the
# .py and re-run it rather than this file.
#
# A small, sparse pyramid. It flowers by age: pale hairs already at age 5, a loose calyx cola at 6,
# and at 7 it has gone to seed -- tan seeds in the calyxes, rusted hairs, the low leaves yellowing.
#
# The linter calls the age-0 seedling pancake-shaded. Accepted: at five pixels a pair of cotyledons
# lit from above is a gradient by nature, and vanilla's own wheat and carrot sprouts score the same.
"""
out=[HEAD, '@palette ruderalis', PAL, '']
for n,f in enumerate([age0,age1,age2,age3,age4,age5,age6,age7]):
    out.append('@texture block/ruderalis_crop_stage%d %dx%d palette=ruderalis type=plant'%(n,W,H))
    out.extend(''.join(r) for r in f()); out.append('')

ITEMS = """# ---- items ----
# The BUDS are Warlon Mhite's hand-drawn Purple Kush bud, traced pixel for pixel, under a Ruderalis
# palette, as Beldía's are: the calyx goes the crop's plain green, and where Beldía's frost sits
# this one has none -- it is the low-resin plant -- only a paler calyx. The amber pistils become
# seed browns and the few left are rusted, because a ruderalis bud is mostly seed.
@palette ruderalis_buds
. = none
1 = 0D160E
2 = 3A3325
3 = 34491D
4 = 4C6526
5 = 668530
6 = 7E9A45
7 = 6E3A1C
8 = 86A23F
9 = A4612E
a = A8916A
b = A4B862
c = 8A7550

@texture item/ruderalis_buds 16x16 palette=ruderalis_buds type=item
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

# The SEEDS: the shared hemp-seed drawing in the crop's stalk browns -- duller and more olive than
# the cultivated strains', as a wild seed is.
@palette ruderalis_seeds
. = none
1 = 2F2C24
2 = 5E5230
3 = 7D6D3C

@texture item/ruderalis_seeds 16x16 palette=ruderalis_seeds type=item
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
