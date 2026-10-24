#!/usr/bin/env python3
"""Turn .github/build-targets.json into the build matrix for the nightly workflow.

Each target is {"minecraft": "upstream" | "<version>", "loaders": [...]}.
"upstream" builds with upstream's own gradle.properties. An explicit version
builds the same source with -Pminecraft_version / -Pfabric_api_version
overrides, using the newest Fabric API published for that version. The source
has to actually compile for that version, otherwise the target fails and is
listed as failed in the release notes.

Usage: resolve_targets.py <targets.json> <upstream minecraft_version> <upstream fabric_api_version>
Prints the matrix as compact JSON.
"""

import json
import re
import sys
import urllib.request

FABRIC_API_METADATA = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml"
# Loaders the build job knows how to produce from the Fabric source.
SUPPORTED_LOADERS = {"fabric", "quilt"}


def latest_fabric_api(minecraft, versions):
    matches = [v for v in versions if v.endswith("+" + minecraft)]
    if not matches:
        sys.exit(f"No Fabric API release found for Minecraft {minecraft}")
    # maven-metadata.xml lists versions in publish order, so the last match is the newest.
    return matches[-1]


def main():
    targets_file, upstream_mc, upstream_fabric_api = sys.argv[1:4]
    with open(targets_file) as f:
        targets = json.load(f)["targets"]

    fabric_api_versions = None
    matrix = []
    seen = set()
    for target in targets:
        loaders = target["loaders"]
        unsupported = sorted(set(loaders) - SUPPORTED_LOADERS)
        if unsupported:
            sys.exit(
                f"Loader(s) {', '.join(unsupported)} have no build recipe yet; "
                f"supported: {', '.join(sorted(SUPPORTED_LOADERS))}"
            )

        if target["minecraft"] == "upstream":
            minecraft, fabric_api, override = upstream_mc, upstream_fabric_api, False
        else:
            if fabric_api_versions is None:
                with urllib.request.urlopen(FABRIC_API_METADATA) as resp:
                    fabric_api_versions = re.findall(r"<version>([^<]+)</version>", resp.read().decode())
            minecraft = target["minecraft"]
            fabric_api = latest_fabric_api(minecraft, fabric_api_versions)
            override = minecraft != upstream_mc

        if minecraft in seen:
            continue
        seen.add(minecraft)
        matrix.append(
            {
                "minecraft": minecraft,
                "fabric_api": fabric_api,
                "override": override,
                "primary": minecraft == upstream_mc,
                "loaders": " ".join(loaders),
            }
        )

    print(json.dumps(matrix, separators=(",", ":")))


if __name__ == "__main__":
    main()
