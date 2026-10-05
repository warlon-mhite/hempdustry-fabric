#!/usr/bin/env python3
"""Fails when a build drops anything an addon compiled against an older release could call.

    python3 scripts/api_additive.py <released.jar> <new.jar>

Starts from every class in the released jar's api package, follows every type of this mod that
their public and protected signatures name, and walks those in turn: SmokeContents from the events,
Strain from SmokeContents, and so on. Every constructor, method and field those classes had, by name
and JVM descriptor, must still be in the new jar, because that is what a compiled addon links
against. Additions are fine; anything missing is printed and the exit code is 1.

Give it the remapped jars (build/libs, or a release download) on both sides, so that the Minecraft
types in the descriptors are the same intermediary names. Needs javap on the PATH.
"""
import re
import subprocess
import sys
import zipfile

PACKAGE = 'com/warlonmhite/hempdustry/'
MOD_TYPE = re.compile(r'L(' + PACKAGE + r'[\w/$]+);|(com\.warlonmhite\.hempdustry\.[\w.$]+)')
HEADER = re.compile(r'^\S.*\b(?:class|interface|enum|record) (com\.warlonmhite\.hempdustry\.[\w.$]+)')


def javap(jar, classes):
    """{class name: (header and declarations, {(member name, descriptor)})} for the classes this jar
    has. The declarations are kept for their generics: RegistryEntry<Strain> names Strain only there,
    never in a descriptor."""
    present = set(zipfile.ZipFile(jar).namelist())
    wanted = [c for c in classes if c.replace('.', '/') + '.class' in present]
    out = {}
    if not wanted:
        return out
    text = subprocess.run(['javap', '-protected', '-s', '-cp', jar, *wanted],
                          capture_output=True, text=True, check=True).stdout
    current, member = None, None
    for line in text.splitlines():
        header = HEADER.match(line)
        if header:
            current = header.group(1)
            out[current] = ([line], set())
        elif line.startswith('    descriptor: ') and current and member:
            out[current][1].add((member, line.split(': ', 1)[1]))
            member = None
        elif line.startswith('  ') and not line.startswith('    ') and current:
            decl = line.strip().rstrip(';')
            out[current][0].append(decl)
            if decl.startswith('static {}'):
                member = None
            elif '(' in decl:
                name = decl.split('(')[0].split()[-1]
                member = '<init>' if name == current else name
            else:
                member = decl.split()[-1]
    return out


def referenced(declarations, members):
    """The mod's own types named in a class's header, declarations and descriptors."""
    names = set()
    for text in declarations + [d for _, d in members]:
        for slashed, dotted in MOD_TYPE.findall(text):
            names.add(slashed.replace('/', '.') if slashed else dotted)
    return names


def main(old_jar, new_jar):
    roots = [n[:-len('.class')].replace('/', '.') for n in zipfile.ZipFile(old_jar).namelist()
             if n.startswith(PACKAGE + 'api/') and n.endswith('.class')]
    if not roots:
        sys.exit(f'{old_jar} has no {PACKAGE}api/ classes — is it a Hempdustry jar?')
    seen, frontier, old = set(), set(roots), {}
    while frontier:
        found = javap(old_jar, sorted(frontier))
        old.update(found)
        seen |= frontier
        frontier = set()
        for declarations, members in found.values():
            frontier |= referenced(declarations, members) - seen

    new = javap(new_jar, sorted(old))
    missing = []
    for name in sorted(old):
        if name not in new:
            missing.append(f'{name}: the whole class is gone')
            continue
        for member, descriptor in sorted(old[name][1] - new[name][1]):
            missing.append(f'{name}#{member} {descriptor}')
    print(f'{len(old)} classes reachable from the api package of {old_jar}')
    for line in missing:
        print('  MISSING ' + line)
    print(f'{len(missing)} missing')
    return 1 if missing else 0


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    sys.exit(main(sys.argv[1], sys.argv[2]))
