import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const sharp = require('sharp');

const here = path.dirname(fileURLToPath(import.meta.url));
const modelPath = path.join(here, 'uc01-uc15-component-sequence-model.json');
const imgDir = path.resolve(here, '..', 'img');
const texPath = path.join(here, 'uc01-uc15-component-sequence-diagrams.tex');

const model = JSON.parse(await fs.readFile(modelPath, 'utf8'));

const WIDTH = 2200;
const HEIGHT = 1360;
const BLUE = '#2563eb';
const DARK = '#172033';
const PALE_BLUE = '#f5f8ff';
const NOTE = '#fffbea';
const FONT = 'Microsoft YaHei, SimHei, Noto Sans CJK SC, sans-serif';

function xml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;');
}

function latex(value) {
  return String(value)
    .replaceAll('\\', '\\textbackslash{}')
    .replaceAll('&', '\\&')
    .replaceAll('%', '\\%')
    .replaceAll('$', '\\$')
    .replaceAll('#', '\\#')
    .replaceAll('_', '\\_')
    .replaceAll('{', '\\{')
    .replaceAll('}', '\\}')
    .replaceAll('^', '\\textasciicircum{}')
    .replaceAll('~', '\\textasciitilde{}');
}

function unitWidth(char) {
  return /[\x00-\xff]/.test(char) ? 0.58 : 1;
}

function wrapByUnits(text, maxUnits) {
  const explicit = String(text).split('\n');
  const result = [];
  for (const source of explicit) {
    let line = '';
    let units = 0;
    for (const ch of source) {
      const next = unitWidth(ch);
      if (line && units + next > maxUnits) {
        result.push(line);
        line = ch;
        units = next;
      } else {
        line += ch;
        units += next;
      }
    }
    if (line || source === '') result.push(line);
  }
  return result;
}

function participantXs(count) {
  const start = count === 5 ? 360 : 420;
  const end = 2020;
  if (count === 1) return [1100];
  return Array.from({ length: count }, (_, index) => start + (end - start) * index / (count - 1));
}

function textSvg(lines, x, y, options = {}) {
  const {
    size = 24,
    weight = 400,
    anchor = 'middle',
    fill = DARK,
    lineHeight = size * 1.25,
    outlined = false,
  } = options;
  const startY = y - ((lines.length - 1) * lineHeight) / 2;
  const stroke = outlined ? ' stroke="#fff" stroke-width="7" paint-order="stroke"' : '';
  return `<text x="${x}" y="${startY}" text-anchor="${anchor}" font-family="${FONT}" font-size="${size}" font-weight="${weight}" fill="${fill}"${stroke}>${lines.map((line, index) => `<tspan x="${x}" dy="${index === 0 ? 0 : lineHeight}">${xml(line)}</tspan>`).join('')}</text>`;
}

function renderActor(x, actor) {
  return [
    `<circle cx="${x}" cy="103" r="22" fill="white" stroke="${DARK}" stroke-width="3"/>`,
    `<line x1="${x}" y1="125" x2="${x}" y2="168" stroke="${DARK}" stroke-width="3"/>`,
    `<line x1="${x - 31}" y1="141" x2="${x + 31}" y2="141" stroke="${DARK}" stroke-width="3"/>`,
    `<line x1="${x}" y1="168" x2="${x - 27}" y2="202" stroke="${DARK}" stroke-width="3"/>`,
    `<line x1="${x}" y1="168" x2="${x + 27}" y2="202" stroke="${DARK}" stroke-width="3"/>`,
    textSvg(wrapByUnits(actor, 9), x, 228, { size: 25, weight: 600 }),
  ].join('\n');
}

function renderComponent(x, label) {
  const boxWidth = 350;
  const boxHeight = 108;
  const left = x - boxWidth / 2;
  const top = 73;
  const lines = String(label).split('\n');
  return [
    `<rect x="${left}" y="${top}" width="${boxWidth}" height="${boxHeight}" rx="2" fill="${PALE_BLUE}" stroke="${BLUE}" stroke-width="3"/>`,
    textSvg(lines, x - 13, top + boxHeight / 2 + 2, { size: lines.length > 1 ? 20 : 25, weight: 600 }),
    `<rect x="${left + boxWidth - 35}" y="${top + 19}" width="19" height="23" fill="white" stroke="${BLUE}" stroke-width="2.5"/>`,
    `<rect x="${left + boxWidth - 43}" y="${top + 24}" width="10" height="8" fill="white" stroke="${BLUE}" stroke-width="2.5"/>`,
    `<rect x="${left + boxWidth - 43}" y="${top + 34}" width="10" height="8" fill="white" stroke="${BLUE}" stroke-width="2.5"/>`,
  ].join('\n');
}

function activationRanges(diagram) {
  const ranges = {};
  diagram.messages.forEach((message, index) => {
    for (const key of [message[0], message[1]]) {
      if (!ranges[key]) ranges[key] = [index, index];
      ranges[key][0] = Math.min(ranges[key][0], index);
      ranges[key][1] = Math.max(ranges[key][1], index);
    }
  });
  return ranges;
}

function renderDiagramSvg(diagram) {
  const componentXs = participantXs(diagram.components.length);
  const positions = { actor: 112 };
  componentXs.forEach((x, index) => { positions[`c${index + 1}`] = x; });
  const startY = 292;
  const endY = 1018;
  const step = diagram.messages.length === 1 ? 0 : (endY - startY) / (diagram.messages.length - 1);
  const messageFont = diagram.messages.length > 13 ? 17 : diagram.messages.length > 11 ? 18 : 20;
  const messageLineHeight = messageFont + 2;
  const messageYs = diagram.messages.map((_, index) => startY + index * step);
  const ranges = activationRanges(diagram);
  const parts = [];

  parts.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${WIDTH}" height="${HEIGHT}" viewBox="0 0 ${WIDTH} ${HEIGHT}">`);
  parts.push('<defs><marker id="arrow" markerWidth="12" markerHeight="9" refX="11" refY="4.5" orient="auto"><path d="M0,0 L12,4.5 L0,9 Z" fill="#172033"/></marker></defs>');
  parts.push('<rect width="100%" height="100%" fill="white"/>');
  parts.push(textSvg([`${diagram.id} ${diagram.name}—组件级顺序图`], WIDTH / 2, 38, { size: 31, weight: 700 }));
  parts.push(textSvg([`追溯测试：${diagram.tests.join('、')}`], WIDTH - 52, 39, { size: 18, anchor: 'end', fill: '#526078' }));
  parts.push(renderActor(positions.actor, diagram.actor));
  diagram.components.forEach((label, index) => parts.push(renderComponent(componentXs[index], label)));

  parts.push(`<line x1="${positions.actor}" y1="242" x2="${positions.actor}" y2="1082" stroke="${DARK}" stroke-width="2" stroke-dasharray="9 9"/>`);
  componentXs.forEach((x) => parts.push(`<line x1="${x}" y1="181" x2="${x}" y2="1082" stroke="${DARK}" stroke-width="2" stroke-dasharray="9 9"/>`));

  for (const [key, [first, last]] of Object.entries(ranges)) {
    const x = positions[key];
    const top = Math.max(key === 'actor' ? 246 : 190, messageYs[first] - 17);
    const bottom = Math.min(1072, messageYs[last] + 23);
    parts.push(`<rect x="${x - 7}" y="${top}" width="14" height="${Math.max(34, bottom - top)}" fill="white" stroke="${DARK}" stroke-width="2"/>`);
  }

  diagram.messages.forEach(([from, to, kind, label], index) => {
    const y = messageYs[index];
    const x1 = positions[from];
    const x2 = positions[to];
    if (kind === 'self') {
      const loopWidth = 92;
      parts.push(`<path d="M ${x1 + 7} ${y} H ${x1 + loopWidth} V ${y + 34} H ${x1 + 8}" fill="none" stroke="${DARK}" stroke-width="2.3" marker-end="url(#arrow)"/>`);
      parts.push(textSvg(wrapByUnits(label, 24), x1 + loopWidth / 2 + 20, y - 13, { size: messageFont, lineHeight: messageLineHeight, outlined: true }));
      return;
    }
    const direction = Math.sign(x2 - x1) || 1;
    const fromX = x1 + direction * 8;
    const toX = x2 - direction * 10;
    const dashed = kind === 'return' ? ' stroke-dasharray="10 8"' : '';
    parts.push(`<line x1="${fromX}" y1="${y}" x2="${toX}" y2="${y}" stroke="${DARK}" stroke-width="2.3"${dashed} marker-end="url(#arrow)"/>`);
    const distance = Math.abs(x2 - x1);
    const maxUnits = Math.max(14, Math.min(38, distance / (messageFont * 0.95)));
    const lines = wrapByUnits(label, maxUnits);
    parts.push(textSvg(lines, (x1 + x2) / 2, y - (lines.length > 1 ? messageLineHeight + 3 : 12), { size: messageFont, lineHeight: messageLineHeight, outlined: true }));
  });

  const noteX = 315;
  const noteY = 1120;
  const noteW = 1815;
  const noteH = 170;
  parts.push(`<rect x="${noteX}" y="${noteY}" width="${noteW}" height="${noteH}" rx="8" fill="${NOTE}" stroke="#6b7280" stroke-width="2"/>`);
  parts.push(textSvg(['分支结果：'], noteX + 25, noteY + 34, { size: 22, weight: 700, anchor: 'start' }));
  diagram.branches.forEach((branch, index) => {
    parts.push(textSvg(wrapByUnits(branch, 78), noteX + 25, noteY + 75 + index * 52, { size: 21, anchor: 'start', lineHeight: 24 }));
  });
  parts.push('</svg>');
  return parts.join('\n');
}

function texPoint(x) {
  return (x / 100).toFixed(2);
}

function renderDiagramTex(diagram) {
  const componentXs = participantXs(diagram.components.length);
  const positions = { actor: 1.12 };
  componentXs.forEach((x, index) => { positions[`c${index + 1}`] = x / 100; });
  const startY = 2.92;
  const endY = 10.18;
  const step = diagram.messages.length === 1 ? 0 : (endY - startY) / (diagram.messages.length - 1);
  const ys = diagram.messages.map((_, index) => startY + index * step);
  const lines = [];
  lines.push('\\begin{tikzpicture}[x=1cm,y=-1cm,>=Stealth,font=\\small]');
  lines.push(`\\node[font=\\bfseries\\Large] at (11,0.38) {${latex(`${diagram.id} ${diagram.name}—组件级顺序图`)}};`);
  lines.push(`\\node[anchor=east,text=gray] at (21.45,0.38) {追溯测试：${latex(diagram.tests.join('、'))}};`);
  lines.push('\\draw[line width=0.8pt] (1.12,0.82) circle (0.22);');
  lines.push('\\draw[line width=0.8pt] (1.12,1.04)--(1.12,1.68) (0.81,1.31)--(1.43,1.31) (1.12,1.68)--(0.85,2.02) (1.12,1.68)--(1.39,2.02);');
  lines.push(`\\node[font=\\bfseries] at (1.12,2.28) {${latex(diagram.actor)}};`);
  lines.push('\\draw[lifeline] (1.12,2.42)--(1.12,10.82);');
  diagram.components.forEach((label, index) => {
    const x = componentXs[index] / 100;
    const labelTex = String(label).split('\n').map(latex).join('\\\\');
    lines.push(`\\node[component] (head${index + 1}) at (${x.toFixed(2)},1.27) {\\small ${labelTex}};`);
    lines.push(`\\draw[lifeline] (${x.toFixed(2)},1.81)--(${x.toFixed(2)},10.82);`);
    lines.push(`\\draw[blue,line width=0.55pt] (${(x + 1.28).toFixed(2)},0.95) rectangle (${(x + 1.47).toFixed(2)},1.18);`);
    lines.push(`\\draw[blue,line width=0.55pt] (${(x + 1.20).toFixed(2)},1.00) rectangle (${(x + 1.30).toFixed(2)},1.08) (${(x + 1.20).toFixed(2)},1.10) rectangle (${(x + 1.30).toFixed(2)},1.18);`);
  });
  const ranges = activationRanges(diagram);
  for (const [key, [first, last]] of Object.entries(ranges)) {
    const x = positions[key];
    const top = Math.max(key === 'actor' ? 2.46 : 1.90, ys[first] - 0.17);
    const bottom = Math.min(10.72, ys[last] + 0.23);
    lines.push(`\\draw[activation] (${(x - 0.07).toFixed(2)},${top.toFixed(2)}) rectangle (${(x + 0.07).toFixed(2)},${bottom.toFixed(2)});`);
  }
  diagram.messages.forEach(([from, to, kind, label], index) => {
    const y = ys[index];
    const x1 = positions[from];
    const x2 = positions[to];
    const text = wrapByUnits(label, Math.max(14, Math.min(34, Math.abs(x2 - x1) * 100 / 19))).map(latex).join('\\\\');
    if (kind === 'self') {
      lines.push(`\\draw[call] (${x1.toFixed(2)},${y.toFixed(2)})--++(0.92,0)--++(0,0.34)--++(-0.84,0) node[midway,above=2pt,align=center] {${text}};`);
    } else {
      const style = kind === 'return' ? 'return' : 'call';
      lines.push(`\\draw[${style}] (${x1.toFixed(2)},${y.toFixed(2)})--node[midway,above=2pt,fill=white,inner sep=1pt,align=center] {${text}} (${x2.toFixed(2)},${y.toFixed(2)});`);
    }
  });
  lines.push('\\node[note,anchor=north west] at (3.15,11.20) {\\textbf{分支结果：}\\\\[2pt]' + diagram.branches.map((branch) => latex(branch)).join('\\\\[2pt]') + '};');
  lines.push('\\end{tikzpicture}');
  return lines.join('\n');
}

function renderTex(diagrams) {
  return `\\documentclass[a4paper,landscape]{ctexart}
\\usepackage[margin=5mm]{geometry}
\\usepackage{tikz}
\\usetikzlibrary{arrows.meta,positioning}
\\pagestyle{empty}
\\definecolor{umlblue}{HTML}{2563EB}
\\definecolor{umldark}{HTML}{172033}
\\definecolor{umlpale}{HTML}{F5F8FF}
\\definecolor{umlnote}{HTML}{FFFBEA}
\\tikzset{
  component/.style={draw=umlblue,fill=umlpale,line width=0.8pt,minimum width=3.50cm,minimum height=1.08cm,align=center,font=\\bfseries},
  lifeline/.style={draw=umldark,dashed,line width=0.55pt},
  activation/.style={draw=umldark,fill=white,line width=0.55pt},
  call/.style={draw=umldark,-{Stealth[length=2.5mm]},line width=0.65pt},
  return/.style={draw=umldark,dashed,-{Stealth[length=2.5mm]},line width=0.65pt},
  note/.style={draw=gray,fill=umlnote,rounded corners=2pt,text width=17.7cm,inner sep=7pt,align=left}
}
\\begin{document}
${diagrams.map((diagram, index) => `${renderDiagramTex(diagram)}${index < diagrams.length - 1 ? '\n\\newpage' : ''}`).join('\n\n')}
\\end{document}
`;
}

await fs.mkdir(imgDir, { recursive: true });
for (const diagram of model.diagrams) {
  const svg = renderDiagramSvg(diagram);
  const svgPath = path.join(imgDir, `${diagram.filename}.svg`);
  const pngPath = path.join(imgDir, `${diagram.filename}.png`);
  await fs.writeFile(svgPath, svg, 'utf8');
  await sharp(Buffer.from(svg)).png({ compressionLevel: 9 }).toFile(pngPath);
}
await fs.writeFile(texPath, renderTex(model.diagrams), 'utf8');

console.log(`Generated ${model.diagrams.length} SVG + ${model.diagrams.length} PNG diagrams.`);
console.log(`TikZ source: ${texPath}`);
