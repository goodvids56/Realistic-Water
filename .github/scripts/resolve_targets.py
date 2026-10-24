#!/usr/bin/env python3
"""Turn .github/build-targets.json into the build matrix for the nightly workflow.

Each target is {"minecraft": "upstream" | "<version>", "loaders": [...]}.

- fabric builds upstream's own project. "upstream" uses upstream's gradle.properties as-is; an
  explicit version overrides minecraft_version / fabric_api_version with the newest Fabric API for
  it. The source has to actually compile for that version, otherwise the target fails and is listed
  as failed in the release notes.
- quilt reuses the fabric jar (Quilt Loader runs Fabric mods), so it rides along with fabric.
- neoforge and forge build upstream's source through the port in port/, which only supports the
  Minecraft version upstream targets, against the newest NeoForge / Forge for it.

Every build becomes one matrix entry: {"id", "minecraft", "build", "loaders", "loader_version",
"fabric_api", "override", "primary"}.

Usage: resolve_targets.py <targets.json> <upstream minecraft_version> <upstream fabric_api_version>
Prints the matrix as compact JSON.
"""

import json
import re
import sys
import urllib.request

FABRIC_API_METADATA = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml"
NEOFORGE_METADATA = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
FORGE_METADATA = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"
SUPPORTED_LOADERS = {"fabric", "quilt", "neoforge", "forge"}
PORTED_LOADERS = {"neoforge", "forge"}

_metadata = {}


def versions(url):
    if url not in _metadata:
        # Forge's maven rejects urllib's default user agent.
        request = urllib.request.Request(url, headers={"User-Agent": "realistic-water-nightly-build"})
        with urllib.request.urlopen(request) as resp:
            _metadata[url] = re.findall(r"<version>([^<]+)</version>", resp.read().decode())
    return _metadata[url]


def version_key(version):
    return [int(part) if part.isdigit() else part for part in re.split(r"[.\-+]", version)]


def newest(candidates, what):
    if not candidates:
        sys.exit(f"No {what} found")
    # Prefer stable releases; fall back to betas when that's all there is.
    stable = [v for v in candidates if not re.search(r"alpha|beta|rc|pre", v, re.I)]
    return max(stable or candidates, key=version_key)


def latest_fabric_api(minecraft):
    # maven-metadata.xml lists versions in publish order, so the last match is the newest.
    matches = [v for v in versions(FABRIC_API_METADATA) if v.endswith("+" + minecraft)]
    if not matches:
        sys.exit(f"No Fabric API release found for Minecraft {minecraft}")
    return matches[-1]


def latest_neoforge(minecraft):
    # NeoForge versions are the Minecraft version padded to three parts, then a build: 26.3 -> 26.3.0.95.
    parts = minecraft.split(".")
    prefix = ".".join((parts + ["0"])[:3]) + "."
    return newest([v for v in versions(NEOFORGE_METADATA) if v.startswith(prefix)], f"NeoForge for Minecraft {minecraft}")


def latest_forge(minecraft):
    return newest([v for v in versions(FORGE_METADATA) if v.startswith(minecraft + "-")], f"Forge for Minecraft {minecraft}")


def main():
    targets_file, upstream_mc, upstream_fabric_api = sys.argv[1:4]
    with open(targets_file) as f:
        targets = json.load(f)["targets"]

    matrix = []
    seen = set()
    for target in targets:
        loaders = target["loaders"]
        unsupported = sorted(set(loaders) - SUPPORTED_LOADERS)
        if unsupported:
            sys.exit(f"Unknown loader(s) {', '.join(unsupported)}; supported: {', '.join(sorted(SUPPORTED_LOADERS))}")

        minecraft = upstream_mc if target["minecraft"] == "upstream" else target["minecraft"]
        if minecraft != upstream_mc and PORTED_LOADERS & set(loaders):
            sys.exit(f"NeoForge/Forge builds only support upstream's Minecraft version ({upstream_mc}), not {minecraft}")

        fabric_loaders = [loader for loader in loaders if loader in ("fabric", "quilt")]
        if fabric_loaders and "fabric" not in fabric_loaders:
            sys.exit("quilt reuses the fabric jar, so a target with quilt needs fabric too")

        builds = []
        if fabric_loaders:
            override = minecraft != upstream_mc
            fabric_api = latest_fabric_api(minecraft) if override else upstream_fabric_api
            builds.append(("fabric", " ".join(fabric_loaders), fabric_api, override))
        if "neoforge" in loaders:
            builds.append(("neoforge", "neoforge", latest_neoforge(minecraft), False))
        if "forge" in loaders:
            builds.append(("forge", "forge", latest_forge(minecraft), False))

        for build, build_loaders, version, override in builds:
            entry_id = f"{build}-mc{minecraft}"
            if entry_id in seen:
                continue
            seen.add(entry_id)
            matrix.append(
                {
                    "id": entry_id,
                    "minecraft": minecraft,
                    "build": build,
                    "loaders": build_loaders,
                    "loader_version": version if build != "fabric" else "",
                    "fabric_api": version if build == "fabric" else upstream_fabric_api,
                    "override": override,
                    # Upstream's own Fabric build has to succeed for a release; the rest are best effort.
                    "primary": build == "fabric" and minecraft == upstream_mc,
                }
            )

    print(json.dumps(matrix, separators=(",", ":")))


if __name__ == "__main__":
    main()
