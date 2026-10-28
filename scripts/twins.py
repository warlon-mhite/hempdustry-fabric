#!/usr/bin/env python3
"""List the work one Minecraft line has and another has not taken yet.

The mod is built for more than one Minecraft at a time, one branch each (2.1 for 1.21.11, 26.2-2.1
for 26.2, 1.21.1-2.1 for 1.21.1), and a change made on one line is carried to the others as a twin:
the same change, rewritten where the game's code differs, under the same subject. A cherry-pick keeps
the subject on its own, and that is all this script pairs on. Everything since the two branches split
that has no twin on the other side is listed, oldest first.

A change that belongs to one line only -- the port itself, a fix for something only one Minecraft
does -- says so with a trailer in its message, and is listed apart instead of as missing:

    Line-only: 26.2

Usage:
    python3 scripts/twins.py OTHER            # this checkout's branch against OTHER
    python3 scripts/twins.py OTHER --strict   # exit non-zero while anything is missing on either side

OTHER is any ref: a branch (2.1), a remote one (origin/2.1), a tag.
"""
import argparse
import subprocess
import sys

SEPARATOR = "\x1e"


def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout


def commits(base, tip):
    """(sha, subject, line-only) for every commit in base..tip, oldest first, merges left out."""
    log = git("log", "--reverse", "--no-merges", f"--format=%h%x1f%s%x1f%(trailers:key=Line-only,valueonly)"
              + SEPARATOR, f"{base}..{tip}")
    out = []
    for record in log.split(SEPARATOR):
        record = record.strip("\n")
        if not record:
            continue
        sha, subject, line_only = (record.split("\x1f") + ["", ""])[:3]
        out.append((sha, subject, line_only.strip()))
    return out


def report(title, rows):
    print(f"{title}: {len(rows)}")
    for sha, subject, note in rows:
        print(f"  {sha}  {subject}" + (f"  [{note}]" if note else ""))


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("other", help="the other line's branch, or any ref")
    parser.add_argument("--strict", action="store_true", help="fail while a change is missing on either side")
    args = parser.parse_args()

    here = git("rev-parse", "--abbrev-ref", "HEAD").strip()
    base = git("merge-base", "HEAD", args.other).strip()
    ours, theirs = commits(base, "HEAD"), commits(base, args.other)
    our_subjects = {subject for _, subject, _ in ours}
    their_subjects = {subject for _, subject, _ in theirs}

    paired = [c for c in ours if c[1] in their_subjects]
    only_ours = [c for c in ours if c[1] not in their_subjects]
    only_theirs = [c for c in theirs if c[1] not in our_subjects]
    missing_there = [c for c in only_ours if not c[2]]
    missing_here = [c for c in only_theirs if not c[2]]
    line_only = [c for c in only_ours + only_theirs if c[2]]

    print(f"{here} against {args.other}, since they split at {git('log', '-1', '--format=%h %s', base).strip()}")
    print(f"paired: {len(paired)}")
    report(f"on {here}, not yet on {args.other}", missing_there)
    report(f"on {args.other}, not yet on {here}", missing_here)
    report("line-only (a Line-only trailer)", [(sha, subject, note) for sha, subject, note in line_only])
    return 1 if args.strict and (missing_there or missing_here) else 0


if __name__ == "__main__":
    sys.exit(main())
