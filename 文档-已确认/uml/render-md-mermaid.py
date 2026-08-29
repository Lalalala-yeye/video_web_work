# -*- coding: utf-8 -*-
"""Extract mermaid from SRS/DDS markdown, render PNG via mmdc, replace code blocks with images."""
from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(r"c:\Users\zhaozhewen\video_web")
DOC_DIR = ROOT / "文档-已确认"
IMG_DIR = DOC_DIR / "img"
MMD_DIR = DOC_DIR / "uml" / "mermaid"
PUPPETEER = MMD_DIR / "puppeteer.json"
MERMAID_CFG = MMD_DIR / "mermaid.json"

FILES = [
    DOC_DIR / "软件需求规格说明书.md",
    DOC_DIR / "软件详细设计说明书.md",
    DOC_DIR / "微服务划分图.md",
]

BLOCK_RE = re.compile(r"```mermaid\n(.*?)```", re.DOTALL)
CAPTION_RE = re.compile(r"^\*图\s+([^*]+)\*")


def safe_name(caption: str) -> str:
    name = caption.strip()
    name = name.replace(" ", "")
    name = re.sub(r'[<>:"/\\|?*]', "", name)
    return name


def find_chrome() -> str | None:
    candidates = [
        os.environ.get("CHROME_BIN"),
        r"C:\Program Files\Google\Chrome\Application\chrome.exe",
        r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
        os.path.expandvars(r"%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe"),
    ]
    for path in candidates:
        if path and Path(path).is_file():
            return path
    return None


def extract_jobs(md_path: Path) -> list[dict]:
    text = md_path.read_text(encoding="utf-8")
    jobs = []
    for i, match in enumerate(BLOCK_RE.finditer(text), start=1):
        after = text[match.end() : match.end() + 200]
        cap_m = CAPTION_RE.search(after)
        caption = cap_m.group(1).strip() if cap_m else f"{md_path.stem}-{i}"
        stem = safe_name(caption)
        jobs.append(
            {
                "file": md_path,
                "index": i,
                "src": match.group(0),
                "code": match.group(1).strip() + "\n",
                "caption": caption,
                "stem": stem,
                "png_rel": f"img/{stem}.png",
            }
        )
    return jobs


def replace_blocks(md_path: Path, jobs: list[dict]) -> None:
    text = md_path.read_text(encoding="utf-8")
    for job in jobs:
        image = f"![{job['caption']}]({job['png_rel']})"
        text = text.replace(job["src"], image, 1)
    md_path.write_text(text, encoding="utf-8")


def main() -> int:
    chrome = find_chrome()
    if not chrome:
        print("未找到 Chrome，mermaid-cli 需要 Chrome/Edge。", file=sys.stderr)
        return 1

    MMD_DIR.mkdir(parents=True, exist_ok=True)
    IMG_DIR.mkdir(parents=True, exist_ok=True)
    PUPPETEER.write_text(
        json.dumps({"executablePath": chrome, "args": ["--no-sandbox", "--disable-gpu"]}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    MERMAID_CFG.write_text(
        json.dumps(
            {
                "theme": "base",
                "themeVariables": {
                    "fontFamily": "Microsoft YaHei, SimHei, sans-serif",
                    "primaryColor": "#b8d4f0",
                    "primaryTextColor": "#000000",
                    "primaryBorderColor": "#333333",
                    "lineColor": "#333333",
                    "actorBkg": "#b8d4f0",
                    "actorBorder": "#333333",
                    "actorTextColor": "#000000",
                    "signalColor": "#333333",
                    "signalTextColor": "#000000",
                    "labelBoxBkgColor": "#fff5eb",
                    "labelBoxBorderColor": "#e8a050",
                    "noteBkgColor": "#fffef0",
                    "noteBorderColor": "#333333",
                    "activationBkgColor": "#8ec5eb",
                    "sequenceNumberColor": "#ffffff",
                },
                "sequence": {"showSequenceNumbers": True, "useMaxWidth": False},
                "flowchart": {
                    "curve": "linear",
                    "defaultRenderer": "elk",
                    "htmlLabels": True,
                    "useMaxWidth": False,
                    "padding": 12,
                },
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )

    all_jobs = []
    for md in FILES:
        jobs = extract_jobs(md)
        print(f"{md.name}: {len(jobs)} mermaid")
        all_jobs.extend(jobs)

    for job in all_jobs:
        mmd_path = MMD_DIR / f"{job['stem']}.mmd"
        png_path = IMG_DIR / f"{job['stem']}.png"
        mmd_path.write_text(job["code"], encoding="utf-8")
        cmd = [
            "npx.cmd" if os.name == "nt" else "npx",
            "--yes",
            "@mermaid-js/mermaid-cli@11",
            "-i",
            str(mmd_path),
            "-o",
            str(png_path),
            "-b",
            "white",
            "-s",
            "2",
            "-c",
            str(MERMAID_CFG),
            "-p",
            str(PUPPETEER),
        ]
        print("render", job["stem"])
        subprocess.check_call(cmd, cwd=str(ROOT))
        if not png_path.is_file():
            raise SystemExit(f"未生成 {png_path}")

    by_file: dict[Path, list[dict]] = {}
    for job in all_jobs:
        by_file.setdefault(job["file"], []).append(job)
    for md, jobs in by_file.items():
        replace_blocks(md, jobs)
        print("updated", md.name)

    print("done", len(all_jobs), "png")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
