import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync} from 'node:fs';
import os from 'node:os';
import {join} from 'node:path';
import test from 'node:test';

import {
  compareDocumentationEditions,
  inspectDocumentationEditions,
  markdownContract,
  verifyReviewedDocumentation
} from './documentation-editions.mjs';

function pair(t) {
  const parent = mkdtempSync(join(os.tmpdir(), 'atf-documentation-editions-'));
  t.after(() => rmSync(parent, {recursive: true, force: true}));
  const english = join(parent, 'main');
  const romanian = join(parent, 'main_RO');
  mkdirSync(english);
  mkdirSync(romanian);
  for (const [root, language, orientation, heading] of [
    [english, 'en', 'At a glance', 'Report ownership'],
    [romanian, 'ro', 'Pe scurt', 'Proprietatea raportului']
  ]) {
    mkdirSync(join(root, 'config'), {recursive: true});
    mkdirSync(join(root, 'docs'), {recursive: true});
    writeFileSync(join(root, 'config/atf-governance-policy.json'),
      JSON.stringify({schemaVersion: 1, documentationLanguage: language}, null, 2));
    writeFileSync(join(root, 'pom.xml'), '<project>same-build</project>\n');
    writeFileSync(join(root, 'README.md'), `# ATF\n\n## ${orientation}\n\n[Guide\n](docs/guide.md#${heading.toLowerCase().replaceAll(' ', '-')})\n`);
    writeFileSync(join(root, 'docs/guide.md'), [
      `# ${heading}`,
      '',
      `- ${heading}`,
      ...(language === 'en' ? ['  this list item continues on the next source line'] : []),
      '',
      '| Owner | Result |',
      '|---|---|',
      `| ${heading} | ${heading} |`,
      '',
      '```java',
      'String report = "stable";',
      '```',
      '',
      '```yaml',
      `reason: ${language === 'en' ? 'The owner keeps the contract stable after review.' : 'Ownerul păstrează contractul stabil după revizie.'}`,
      '```',
      '',
      '```mermaid',
      'flowchart LR',
      language === 'en' ? 'A[Input] --> B[Result]' : 'A[Intrare] --> B[Rezultat]',
      language === 'en' ? 'A -. protects .-> B' : 'A -. protejează .-> B',
      '```',
      ''
    ].join('\n'));
  }
  return {english, romanian};
}

test('accepts parallel editions with translated labels and matching Markdown structure', t => {
  const roots = pair(t);
  const result = compareDocumentationEditions(roots.english, roots.romanian);
  assert.deepEqual(result.problems, []);
  assert.equal(result.documents, 2);
});

test('ignores trailing blank lines while preserving internal Markdown structure', t => {
  const roots = pair(t);
  const enPath = join(roots.english, 'docs/guide.md');
  const roPath = join(roots.romanian, 'docs/guide.md');
  writeFileSync(enPath, readFileSync(enPath, 'utf8').replace(/\n+$/, '\n'));
  writeFileSync(roPath, readFileSync(roPath, 'utf8').replace(/\n+$/, '\n\n\n'));
  assert.deepEqual(compareDocumentationEditions(roots.english, roots.romanian).problems, []);

  writeFileSync(roPath, readFileSync(roPath, 'utf8').replace('| Owner | Result |', '\n| Owner | Result |'));
  assert.ok(compareDocumentationEditions(roots.english, roots.romanian).problems
    .includes('Markdown structure differs: docs/guide.md'));
});

test('allows localized prose examples in text fences while preserving their line structure', t => {
  const {english, romanian} = pair(t);
  const englishDoc = readFileSync(join(english, 'docs/guide.md'), 'utf8')
    .replace('String report = "stable";', 'this is an English example\nwith two lines');
  const romanianDoc = readFileSync(join(romanian, 'docs/guide.md'), 'utf8')
    .replace('String report = "stable";', 'acesta este un exemplu românesc\ncu două linii');
  writeFileSync(join(english, 'docs/guide.md'), englishDoc.replace('```java', '```text'));
  writeFileSync(join(romanian, 'docs/guide.md'), romanianDoc.replace('```java', '```text'));
  assert.deepEqual(compareDocumentationEditions(english, romanian).problems, []);

  writeFileSync(join(romanian, 'docs/guide.md'), romanianDoc
    .replace('```java', '```text').replace('```yaml', 'extra localized line\n```yaml'));
  assert.ok(compareDocumentationEditions(english, romanian).problems
    .includes('Markdown structure differs: docs/guide.md'));
});

test('allows translated multiline YAML reasons while preserving block style and indentation', t => {
  const {english, romanian} = pair(t);
  const englishDoc = readFileSync(join(english, 'docs/guide.md'), 'utf8').replace(
    'reason: The owner keeps the contract stable after review.',
    'reason: >-\n  The owner keeps the contract stable after review.\n  The recorded proof remains independently verifiable.'
  );
  const romanianDoc = readFileSync(join(romanian, 'docs/guide.md'), 'utf8').replace(
    'reason: Ownerul păstrează contractul stabil după revizie.',
    'reason: >-\n  Ownerul păstrează contractul stabil după revizie.\n  Dovada înregistrată rămâne verificabilă independent.'
  );
  writeFileSync(join(english, 'docs/guide.md'), englishDoc);
  writeFileSync(join(romanian, 'docs/guide.md'), romanianDoc);
  assert.deepEqual(compareDocumentationEditions(english, romanian).problems, []);

  writeFileSync(join(romanian, 'docs/guide.md'), romanianDoc.replace('reason: >-', 'reason: |'));
  assert.ok(compareDocumentationEditions(english, romanian).problems
    .includes('Markdown structure differs: docs/guide.md'));
});

test('allows branch-specific test-control commits without hiding proof differences', t => {
  const {english, romanian} = pair(t);
  const control = base => [
    '',
    '```yaml',
    'testChanges:',
    '  - id: DOC-CONTROL-01',
    '    status: VERIFIED',
    `    base: ${base}`,
    '    expires: 2026-12-20',
    '    reason: branch-specific verification control',
    `    proof: mvn -Dtest=DocumentationDirectRelationshipGovernanceTest -Datf.governance.testChanges.baseRef=${base} test`,
    '    paths:',
    '      - atf-tests/src/test/java/atf/api/tests/governance/WholeAtfGovernanceSuiteTest.java',
    '```',
    ''
  ].join('\n');
  const englishHash = '4ec3e14b357e0968776399477592fd13c3713631';
  const romanianHash = '754eb315045e565c150ca819498cf63154bee9b4';
  const enPath = join(english, 'docs/guide.md');
  const roPath = join(romanian, 'docs/guide.md');
  writeFileSync(enPath, readFileSync(enPath, 'utf8') + control(englishHash));
  writeFileSync(roPath, readFileSync(roPath, 'utf8') + control(romanianHash));

  assert.deepEqual(compareDocumentationEditions(english, romanian).problems, []);
  const enContract = markdownContract('docs/guide.md', readFileSync(enPath, 'utf8'));
  const roContract = markdownContract('docs/guide.md', readFileSync(roPath, 'utf8'));
  assert.deepEqual(enContract.structure, roContract.structure);

  writeFileSync(roPath, readFileSync(roPath, 'utf8')
    .replace('DocumentationDirectRelationshipGovernanceTest', 'DocumentationTargetGovernanceTest'));
  assert.ok(compareDocumentationEditions(english, romanian).problems
    .includes('Markdown structure differs: docs/guide.md'));
});

test('rejects missing document counterparts and changed link destinations', t => {
  const roots = pair(t);
  writeFileSync(join(roots.romanian, 'README.md'), readFileSync(join(roots.romanian, 'README.md'), 'utf8')
    .replace('docs/guide.md#', 'docs/missing.md#'));
  const result = compareDocumentationEditions(roots.english, roots.romanian);
  assert.ok(result.problems.some(problem => problem.startsWith('Link destinations differ: README.md')));
  rmSync(join(roots.romanian, 'docs/guide.md'));
  assert.ok(compareDocumentationEditions(roots.english, roots.romanian).problems
    .includes('Missing Markdown counterpart: docs/guide.md'));
});

test('rejects structural drift and non-Markdown implementation drift', t => {
  const roots = pair(t);
  writeFileSync(join(roots.romanian, 'docs/guide.md'), readFileSync(join(roots.romanian, 'docs/guide.md'), 'utf8')
    .replace('| Owner | Result |', '| Owner | Result | Extra |'));
  writeFileSync(join(roots.romanian, 'pom.xml'), '<project>different-build</project>\n');
  const result = compareDocumentationEditions(roots.english, roots.romanian);
  assert.ok(result.problems.includes('Markdown structure differs: docs/guide.md'));
  assert.ok(result.problems.includes('Non-Markdown content differs: pom.xml'));
});

test('normalizes checkout line endings for JavaScript tools but detects code drift', t => {
  const {english, romanian} = pair(t);
  const englishTool = join(english, 'atf-tests/tools/documentation-editions.mjs');
  const romanianTool = join(romanian, 'atf-tests/tools/documentation-editions.mjs');
  mkdirSync(join(english, 'atf-tests/tools'), {recursive: true});
  mkdirSync(join(romanian, 'atf-tests/tools'), {recursive: true});
  writeFileSync(englishTool, 'const edition = "shared";\n');
  writeFileSync(romanianTool, 'const edition = "shared";\r\n');
  assert.deepEqual(compareDocumentationEditions(english, romanian).problems, []);

  writeFileSync(romanianTool, 'const edition = "drift";\r\n');
  assert.ok(compareDocumentationEditions(english, romanian).problems
    .includes('Non-Markdown content differs: atf-tests/tools/documentation-editions.mjs'));
});

test('compares published library POM bytes because provenance hashes include line endings', t => {
  const {english, romanian} = pair(t);
  mkdirSync(join(english, 'lib'), {recursive: true});
  mkdirSync(join(romanian, 'lib'), {recursive: true});
  writeFileSync(join(english, 'lib/atf-core-1.0.0-c4.pom'), '<project>\n</project>\n');
  writeFileSync(join(romanian, 'lib/atf-core-1.0.0-c4.pom'), '<project>\r\n</project>\r\n');
  const result = compareDocumentationEditions(english, romanian);
  assert.ok(result.problems.includes('Non-Markdown content differs: lib/atf-core-1.0.0-c4.pom'));
});

test('requires English and Romanian branch policy values', t => {
  const roots = pair(t);
  writeFileSync(join(roots.romanian, 'config/atf-governance-policy.json'),
    JSON.stringify({schemaVersion: 1, documentationLanguage: 'en'}));
  const result = compareDocumentationEditions(roots.english, roots.romanian);
  assert.ok(result.problems.includes('Romanian edition must declare documentationLanguage=ro.'));
});

test('pins a reviewed Romanian commit and detects later English documentation drift', t => {
  const root = mkdtempSync(join(os.tmpdir(), 'atf-documentation-review-'));
  t.after(() => rmSync(root, {recursive: true, force: true}));
  const git = (...args) => execFileSync('git', ['-C', root, ...args], {
    encoding: 'utf8', windowsHide: true, stdio: ['ignore', 'pipe', 'pipe']
  }).trim();
  const write = (path, content) => {
    const absolute = join(root, path);
    mkdirSync(join(absolute, '..'), {recursive: true});
    writeFileSync(absolute, content);
  };
  const commit = message => {
    git('add', '--all');
    git('-c', 'user.name=Documentation Test', '-c', 'user.email=docs@example.invalid',
      '-c', 'commit.gpgSign=false', 'commit', '-m', message);
    return git('rev-parse', 'HEAD');
  };

  git('init', '--initial-branch=main_RO');
  write('config/atf-governance-policy.json', '{"schemaVersion":1,"documentationLanguage":"ro"}\n');
  write('README.md', '# ATF\n\n## Pe scurt\n\n[Ghid](docs/guide.md)\n');
  write('docs/guide.md', '# Ghid\n\n- Contract verificabil\n');
  const romanianCommit = commit('Create Romanian edition');
  git('remote', 'add', 'origin', root);
  git('switch', '-c', 'main');
  write('config/atf-governance-policy.json', '{"schemaVersion":1,"documentationLanguage":"en"}\n');
  write('README.md', '# ATF\n\n## At a glance\n\n[Guide](docs/guide.md)\n');
  write('docs/guide.md', '# Guide\n\n- Verifiable contract\n');
  const englishCommit = commit('Create English edition');
  const reviewed = inspectDocumentationEditions(root, englishCommit, romanianCommit);
  assert.deepEqual(reviewed.problems, []);
  assert.equal(reviewed.review.documents, 2);

  write('config/documentation-editions.json', JSON.stringify(reviewed.review, null, 2) + '\n');
  const pinnedEnglishCommit = commit('Record the paired documentation review');
  git('switch', 'main_RO');
  write('config/documentation-editions.json', JSON.stringify(reviewed.review, null, 2) + '\n');
  const publishedRomanianCommit = commit('Record the paired documentation review');
  assert.equal(git('merge-base', '--is-ancestor', romanianCommit, publishedRomanianCommit), '');

  git('switch', 'main');
  write('docs/guide.md', '# Guide\n\n- Updated contract\n');
  const changedEnglishCommit = commit('Update English documentation without a new review');
  assert.ok(verifyReviewedDocumentation(root, changedEnglishCommit).problems
    .includes('Documentation changed since the recorded bilingual review.'));
  assert.deepEqual(verifyReviewedDocumentation(root, pinnedEnglishCommit).problems, [],
    'a later English edit cannot rewrite the reviewed commit pair');

  git('switch', 'main_RO');
  write('docs/guide.md', '# Ghid\n\n- Contract românesc revizuit\n');
  commit('Update Romanian documentation without a new review');
  git('switch', 'main');
  assert.ok(verifyReviewedDocumentation(root, pinnedEnglishCommit).problems
    .includes('Documentation changed since the recorded bilingual review.'),
    'the published Romanian branch must still match the pinned documentation review');
});
