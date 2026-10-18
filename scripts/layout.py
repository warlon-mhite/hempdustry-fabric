"""Where a checkout keeps the mod's code and data, for the scripts that read them.

Since 2.1 the mod is a common project plus one project per loader (common/, fabric/, neoforge/);
before that it was one project. A checkout of either shape reads the same, so a script pointed at an older
branch's checkout still works.
"""
import pathlib

LOADERS = ("fabric", "neoforge")


class Layout:
    def __init__(self, root):
        self.root = pathlib.Path(root)
        self.split = (self.root / "common").is_dir()
        if self.split:
            common = self.root / "common"
            #: every source tree that ships: common's, then each loader's
            self.java = [common / "src/main/java"] + [
                self.root / loader / "src/main/java" for loader in LOADERS
                if (self.root / loader / "src/main/java").is_dir()]
            self.resources = common / "src/main/resources"
            self.generated = common / "src/generated/resources"
            #: the Gradle path of the project that runs the game tests, datagen and Fabric's jar
            self.fabric = ":fabric:"
            self.fabric_libs = self.root / "fabric/build/libs"
            #: NeoForge's project, the few files its own data run writes and its jar, on a branch that has it
            has_neoforge = (self.root / "neoforge").is_dir()
            self.neoforge = ":neoforge:" if has_neoforge else None
            self.neoforge_generated = self.root / "neoforge/src/generated/resources" if has_neoforge else None
            self.neoforge_libs = self.root / "neoforge/build/libs" if has_neoforge else None
        else:
            self.java = [self.root / "src/main/java"]
            self.resources = self.root / "src/main/resources"
            self.generated = self.root / "src/main/generated"
            self.fabric = ""
            self.fabric_libs = self.root / "build/libs"
            self.neoforge = None
            self.neoforge_generated = None
            self.neoforge_libs = None
        self.client = self.java[0] / "com/warlonmhite/hempdustry/HempdustryClient.java"
        self.assets = [self.resources / "assets/hempdustry", self.generated / "assets/hempdustry"]

    def java_files(self):
        for tree in self.java:
            yield from sorted(tree.rglob("*.java"))


def of(root=None):
    """The layout of the checkout at root, or of the one this script lives in."""
    return Layout(root or pathlib.Path(__file__).resolve().parent.parent)
