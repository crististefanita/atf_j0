import {spawnSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {readFileSync, readdirSync} from 'node:fs';
import {join, posix} from 'node:path';
import {fileURLToPath} from 'node:url';
import {isDeepStrictEqual} from 'node:util';

export const documentationEditionsPolicy = Object.freeze({
  englishBranch: 'main',
  romanianBranch: 'main_RO',
  reviewPath: 'config/documentation-editions.json'
});

const COMMIT = /^[a-f0-9]{40}$/;
const utf8 = new TextDecoder('utf-8', {fatal: true});
const sha256 = bytes => createHash('sha256').update(bytes).digest('hex');

const IGNORED = new Set(['.git', '.idea', 'target', 'artifacts', 'node_modules', 'dist']);
const FENCE = /^\s*(~{3,}|`{3,})(.*)$/;
const LINK = /!?\[(?:[^\[\]]|\[[^\[\]]*\])*\]\(((?:[^()]|\([^()]*\))*)\)/g;
const REFERENCE = /^\s*\[[^\]]+\]:\s*(<[^>]+>|\S+)/gm;

function listedFiles(root) {
  const result = [];
  function visit(directory, prefix = '') {
    for (const entry of readdirSync(directory, {withFileTypes: true})) {
      if (IGNORED.has(entry.name)) continue;
      const path = prefix ? `${prefix}/${entry.name}` : entry.name;
      const absolute = join(directory, entry.name);
      if (entry.isDirectory()) visit(absolute, path);
      else if (entry.isFile()) result.push(path);
    }
  }
  visit(root);
  return result.sort();
}

function readPolicy(root) {
  const value = JSON.parse(readFileSync(join(root, 'config/atf-governance-policy.json'), 'utf8'));
  if (value?.schemaVersion !== 1 || !['en', 'ro'].includes(value.documentationLanguage)) {
    throw new Error(`${root}: expected schemaVersion 1 and documentationLanguage en|ro`);
  }
  return value;
}

function stripFragment(destination) {
  const target = destination.trim().replace(/^<|>$/g, '');
  if (/^[a-z][a-z0-9+.-]*:/i.test(target)) return target.split('#', 1)[0];
  const [path] = target.split('#', 1);
  return path ? posix.normalize(path) : '';
}

function diagramStructure(line) {
  if (line.trimStart().startsWith('%%')) return line;
  return line
    .replace(/\|[^|]+\|/g, '||')
    .replace(/(-\.\s*)[^.]+(\s*\.->)/g, '$1<label>$2')
    .replace(/\[[^\[\]]*\]/g, '[]')
    .replace(/\{[^{}]*\}/g, '{}')
    .replace(/\(\([^()]+\)\)/g, '(())')
    .replace(/^(\s*(?:participant|actor)\s+\S+\s+as\s+).+$/i, '$1<label>')
    .replace(/^(\s*(?:loop|opt|alt|else|par|and|critical|break|section|title)\s+).+$/i, '$1<label>')
    .replace(/^(\s*(?:Note|note)\s+[^:]+:\s*).+$/i, '$1<label>')
    .replace(/^(\s*[A-Za-z]\w*\s*[-]+[x>]+[^:]+:\s*).+$/i, '$1<label>')
    .trimEnd();
}

function linkDestination(path, destination) {
  const clean = stripFragment(destination);
  if (!clean || /^[a-z][a-z0-9+.-]*:/i.test(clean)) return clean;
  return posix.normalize(posix.join(posix.dirname(path), clean));
}

function languageAwareDestination(destination, language, path, problems) {
  const match = /^(https:\/\/github\.com\/crististefanita\/(?:atf_j1|whist|chess)\/blob\/)(main|main_RO)(\/.*)$/i.exec(destination);
  if (!match) return destination;
  const expected = language === 'en' ? 'main' : 'main_RO';
  if (match[2] !== expected) {
    problems.push(`${language} edition links to the wrong language branch in ${path}: expected ${expected}`);
  }
  return `${match[1]}{language-branch}${match[3]}`;
}

export function markdownContract(path, source) {
  const structure = [];
  const destinations = [];
  const linkSource = [];
  let fence = null;
  let fenceLanguage = '';
  let yamlReasonIndent = null;
  let inTextBlock = false;
  let listContinuation = false;
  const flushText = () => {
    if (!inTextBlock) return;
    structure.push(['text']);
    inTextBlock = false;
  };

  const lines = source.replaceAll('\r\n', '\n').split('\n');
  while (lines.length > 0 && !lines.at(-1).trim()) lines.pop();
  for (const line of lines) {
    const match = FENCE.exec(line);
    if (match) {
      flushText();
      listContinuation = false;
      if (!fence) {
        fence = match[1];
        fenceLanguage = match[2].trim().split(/\s+/, 1)[0].toLowerCase();
        yamlReasonIndent = null;
        structure.push(['fence-open', fence[0], fence.length, fenceLanguage]);
      } else if (match[1][0] === fence[0] && match[1].length >= fence.length && !match[2].trim()) {
        structure.push(['fence-close', fence[0], match[1].length]);
        fence = null;
        fenceLanguage = '';
        yamlReasonIndent = null;
      } else {
        structure.push(['fence-line', fenceLanguage, line]);
      }
      continue;
    }

    if (fence) {
      let content = line;
      if (fenceLanguage === 'mermaid') content = diagramStructure(line);
      else if (fenceLanguage === 'text') content = line.trim() ? '<localized text>' : '';
      else if (fenceLanguage === 'yaml') {
        const indent = line.match(/^\s*/)[0].length;
        const reason = /^(\s*(?:-\s+)?reason:\s*)(.*)$/.exec(line);
        const blockReason = reason && /^([>|])([+-]?\d*|\d*[+-]?)$/.exec(reason[2]);
        if (yamlReasonIndent !== null && (!line.trim() || indent > yamlReasonIndent)) {
          content = line.trim() ? line.match(/^\s*/)[0] + '<localized explanation>' : '';
        } else {
          yamlReasonIndent = null;
          if (reason) {
            content = reason[1] + '<localized explanation>';
            if (blockReason) {
              content = reason[1] + reason[2];
              yamlReasonIndent = indent;
            }
          } else if (/^\s*(?:-\s+)?base:\s*[a-f0-9]{40}(?:[a-f0-9]{24})?\s*$/i.test(line)) {
            content = line.replace(/[a-f0-9]{40}(?:[a-f0-9]{24})?\s*$/i, '<branch-commit>');
          } else {
            content = line.replace(/(-Datf\.governance\.testChanges\.baseRef=)[a-f0-9]{40}(?:[a-f0-9]{24})?(?=["\s]|$)/gi,
              '$1<branch-commit>');
          }
        }
      }
      structure.push(['fence-content', fenceLanguage, content]);
      continue;
    }

    linkSource.push(line);

    if (!line.trim()) {
      flushText();
      listContinuation = false;
      structure.push(['blank']);
      continue;
    }

    const heading = /^\s*(#{1,6})\s+/.exec(line);
    const list = /^\s*((?:>\s*)*(?:[-+*]|\d+[.)])\s+)/.exec(line);
    const html = /^\s*(<\/?(?:details|summary)\b)/i.exec(line);
    const table = line.trimStart().startsWith('|');
    if (heading) {
      flushText();
      listContinuation = false;
      structure.push(['heading', heading[1].length]);
    } else if (list) {
      flushText();
      listContinuation = true;
      structure.push(['list', list[1].replace(/\d+/, '<n>')]);
    } else if (html) {
      flushText();
      listContinuation = false;
      structure.push(['html', html[1].toLowerCase()]);
    } else if (table) {
      flushText();
      listContinuation = false;
      structure.push(['table-row', (line.match(/\|/g) ?? []).length]);
    } else if (listContinuation && /^\s{2,}\S/.test(line)) {
      // Translations can wrap one list item onto several indented source lines.
    } else {
      listContinuation = false;
      inTextBlock = true;
    }

  }
  flushText();
  const markdownSource = linkSource.join('\n');
  const linkEntries = [];
  for (const match of markdownSource.matchAll(LINK)) {
    linkEntries.push({index: match.index, destination: match[1]});
  }
  for (const match of markdownSource.matchAll(REFERENCE)) {
    linkEntries.push({index: match.index, destination: match[1]});
  }
  linkEntries.sort((left, right) => left.index - right.index);
  for (const entry of linkEntries) destinations.push(linkDestination(path, entry.destination));
  return {structure, destinations};
}

function sameNonMarkdownTree(englishRoot, romanianRoot, problems) {
  const enFiles = listedFiles(englishRoot).filter(path => !path.toLowerCase().endsWith('.md'));
  const roFiles = listedFiles(romanianRoot).filter(path => !path.toLowerCase().endsWith('.md'));
  const enSet = new Set(enFiles), roSet = new Set(roFiles);
  for (const path of [...new Set([...enFiles, ...roFiles])].sort()) {
    if (!enSet.has(path) || !roSet.has(path)) {
      problems.push(`Non-Markdown file has no counterpart: ${path}`);
      continue;
    }
    if (path.replaceAll('\\', '/') === 'config/atf-governance-policy.json') {
      const en = readPolicy(englishRoot), ro = readPolicy(romanianRoot);
      const enCopy = {...en}, roCopy = {...ro};
      delete enCopy.documentationLanguage;
      delete roCopy.documentationLanguage;
      if (!isDeepStrictEqual(enCopy, roCopy)) problems.push('Governance policy differs beyond documentationLanguage.');
      continue;
    }
    const enBytes = readFileSync(join(englishRoot, path));
    const roBytes = readFileSync(join(romanianRoot, path));
    const publishedLibraryPom = /^lib\/[^/]+\.pom$/i.test(path);
    const textFile = /\.(?:java|xml|pom|html|json|ya?ml|properties|txt|ps1|cmd|bat|md|cjs|mjs|js)$/i.test(path)
      || path.startsWith('atf-reporting-tests/src/test/resources/META-INF/services/');
    const equal = publishedLibraryPom ? enBytes.equals(roBytes) : textFile
      ? utf8.decode(enBytes).replaceAll('\r\n', '\n').replaceAll('\r', '\n')
          === utf8.decode(roBytes).replaceAll('\r\n', '\n').replaceAll('\r', '\n')
      : enBytes.equals(roBytes);
    if (!equal) problems.push(`Non-Markdown content differs: ${path}`);
  }
}

export function compareDocumentationEditions(englishRoot, romanianRoot) {
  const problems = [];
  const enPolicy = readPolicy(englishRoot);
  const roPolicy = readPolicy(romanianRoot);
  if (enPolicy.documentationLanguage !== 'en') problems.push('English edition must declare documentationLanguage=en.');
  if (roPolicy.documentationLanguage !== 'ro') problems.push('Romanian edition must declare documentationLanguage=ro.');

  const enFiles = listedFiles(englishRoot).filter(path => path.toLowerCase().endsWith('.md'));
  const roFiles = listedFiles(romanianRoot).filter(path => path.toLowerCase().endsWith('.md'));
  const enSet = new Set(enFiles), roSet = new Set(roFiles);
  for (const path of [...new Set([...enFiles, ...roFiles])].sort()) {
    if (!enSet.has(path) || !roSet.has(path)) {
      problems.push(`Missing Markdown counterpart: ${path}`);
      continue;
    }
    const en = markdownContract(path, utf8.decode(readFileSync(join(englishRoot, path))));
    const ro = markdownContract(path, utf8.decode(readFileSync(join(romanianRoot, path))));
    if (!isDeepStrictEqual(en.structure, ro.structure)) problems.push(`Markdown structure differs: ${path}`);
    const enDestinations = en.destinations.map(destination => languageAwareDestination(destination, 'en', path, problems));
    const roDestinations = ro.destinations.map(destination => languageAwareDestination(destination, 'ro', path, problems));
    if (!isDeepStrictEqual(enDestinations, roDestinations)) {
      problems.push(`Link destinations differ: ${path}\n  en: ${JSON.stringify(enDestinations)}\n  ro: ${JSON.stringify(roDestinations)}`);
    }
  }

  sameNonMarkdownTree(englishRoot, romanianRoot, problems);
  const englishMarkdown = new Map(enFiles.map(path => [path, readFileSync(join(englishRoot, path))]));
  const romanianMarkdown = new Map(roFiles.map(path => [path, readFileSync(join(romanianRoot, path))]));
  return {problems, documents: enFiles.length,
    review: createReview(englishMarkdown, romanianMarkdown)};
}

function git(project, args, input) {
  const run = spawnSync('git', ['-C', project, ...args], {
    input, windowsHide: true, maxBuffer: 128 * 1024 * 1024, timeout: 30_000,
    env: {...process.env, GIT_TERMINAL_PROMPT: '0'}
  });
  if (run.status !== 0) throw new Error('Cannot read documentation Git input: ' + args[0]);
  return run.stdout;
}

function resolveCommit(project, ref) {
  const commit = git(project, ['rev-parse', '--verify', '--end-of-options', ref + '^{commit}']).toString().trim();
  if (!COMMIT.test(commit)) throw new Error('Invalid documentation commit');
  return commit;
}

function filesAt(project, commit, paths) {
  if (!paths.length) return new Map();
  if (paths.some(path => /[\r\n]/.test(path))) throw new Error('Unsupported documentation path');
  const requests = paths.map(path => commit + ':' + path);
  const response = git(project, ['cat-file', '--batch'], requests.join('\n') + '\n');
  const files = new Map();
  let start = 0;
  for (const path of paths) {
    const end = response.indexOf('\n', start);
    const header = /^([a-f0-9]{40}) blob (\d+)$/.exec(response.subarray(start, end).toString());
    if (!header) throw new Error('Missing documentation blob: ' + path);
    const next = end + 1 + Number(header[2]);
    if (response[next] !== 10) throw new Error('Invalid documentation blob: ' + path);
    files.set(path, response.subarray(end + 1, next));
    start = next + 1;
  }
  return files;
}

function readMarkdownEdition(project, commit) {
  const paths = git(project, ['ls-tree', '-r', '--name-only', '-z', commit]).toString().split('\0')
    .filter(path => path.endsWith('.md'));
  if (!paths.length) throw new Error('Empty documentation inventory at ' + commit);
  return filesAt(project, commit, paths);
}

function createReview(englishFiles, romanianFiles) {
  return {
    schemaVersion: 1,
    englishBranch: documentationEditionsPolicy.englishBranch,
    romanianBranch: documentationEditionsPolicy.romanianBranch,
    documents: englishFiles.size,
    englishSha256: inventoryHash(englishFiles),
    romanianSha256: inventoryHash(romanianFiles)
  };
}

function inventoryHash(files) {
  return sha256([...files].sort(([left], [right]) => left < right ? -1 : left > right ? 1 : 0)
    .map(([path, bytes]) => path + '\t' + sha256(bytes) + '\n').join(''));
}

export function inspectDocumentationEditions(project, englishRef, romanianRef) {
  const englishCommit = resolveCommit(project, englishRef);
  const romanianCommit = resolveCommit(project, romanianRef);
  const englishFiles = readMarkdownEdition(project, englishCommit);
  const romanianFiles = readMarkdownEdition(project, romanianCommit);
  const problems = [];
  const enPolicyPath = 'config/atf-governance-policy.json';
  const enPolicy = JSON.parse(utf8.decode(filesAt(project, englishCommit, [enPolicyPath]).get(enPolicyPath)));
  const roPolicy = JSON.parse(utf8.decode(filesAt(project, romanianCommit, [enPolicyPath]).get(enPolicyPath)));
  if (enPolicy.documentationLanguage !== 'en') problems.push('English edition must declare documentationLanguage=en.');
  if (roPolicy.documentationLanguage !== 'ro') problems.push('Romanian edition must declare documentationLanguage=ro.');

  const englishPaths = new Set(englishFiles.keys());
  const romanianPaths = new Set(romanianFiles.keys());
  for (const path of [...new Set([...englishPaths, ...romanianPaths])].sort()) {
    if (!englishPaths.has(path) || !romanianPaths.has(path)) {
      problems.push(`Missing Markdown counterpart: ${path}`);
      continue;
    }
    const en = markdownContract(path, utf8.decode(englishFiles.get(path)));
    const ro = markdownContract(path, utf8.decode(romanianFiles.get(path)));
    if (!isDeepStrictEqual(en.structure, ro.structure)) problems.push(`Markdown structure differs: ${path}`);
    if (!isDeepStrictEqual(en.destinations, ro.destinations)) problems.push(`Link destinations differ: ${path}`);
  }

  const review = {...createReview(englishFiles, romanianFiles), schemaVersion: 2, romanianCommit};
  return {englishCommit, romanianCommit, problems, documents: englishFiles.size, review};
}

export function verifyReviewedDocumentation(project, englishCommit) {
  if (!COMMIT.test(englishCommit ?? '')) throw new Error('A pinned English source commit is required.');
  const remote = git(project, ['ls-remote', '--exit-code', 'origin', 'refs/heads/main_RO']).toString().trim();
  const remoteCommit = remote.split(/\s+/)[0];
  if (!COMMIT.test(remoteCommit)) throw new Error('No published Romanian edition.');
  const reviewPath = documentationEditionsPolicy.reviewPath;
  const bytes = filesAt(project, englishCommit, [reviewPath]).get(reviewPath);
  const recorded = JSON.parse(utf8.decode(bytes));
  const romanianCommit = recorded.romanianCommit;
  if (recorded.schemaVersion !== 2 || !COMMIT.test(romanianCommit ?? '')) {
    throw new Error('A pinned Romanian documentation commit is required.');
  }
  try {
    git(project, ['cat-file', '-e', remoteCommit + '^{commit}']);
  } catch {
    git(project, ['fetch', '--no-tags', '--no-write-fetch-head', '--refmap=', 'origin', 'refs/heads/main_RO']);
  }
  git(project, ['merge-base', '--is-ancestor', romanianCommit, remoteCommit]);
  const checked = inspectDocumentationEditions(project, englishCommit, remoteCommit);
  checked.review.romanianCommit = romanianCommit;
  if (!isDeepStrictEqual(recorded, checked.review)) {
    checked.problems.push('Documentation changed since the recorded bilingual review.');
  }
  return {...checked, reviewSha256: sha256(bytes)};
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const [command, project, first, second] = process.argv.slice(2);
  let result;
  if (command === 'compare' && project && first) {
    result = compareDocumentationEditions(project, first);
  } else if (project && first && ['inspect', 'verify'].includes(command)
      && (command !== 'inspect' || second)) {
    result = command === 'inspect' ? inspectDocumentationEditions(project, first, second)
      : verifyReviewedDocumentation(project, resolveCommit(project, first));
  } else {
    throw new Error('Use compare <main-worktree> <main_RO-worktree>, inspect <repo> <English ref> <Romanian ref>, or verify <repo> <English commit>.');
  }
  process.stdout.write(JSON.stringify(result, null, 2) + '\n');
  if (result.problems.length) process.exitCode = 2;
}
