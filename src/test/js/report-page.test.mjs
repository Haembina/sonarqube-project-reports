import assert from 'node:assert/strict';
import { beforeEach, describe, mock, test } from 'node:test';

const registered = {};
globalThis.registerExtension = (key, start, css) => Object.assign(registered, { key, start, css });
await import('../../main/resources/static/report_page.js');

const intl = { formatMessage: ({ id }, values) => `${id.replace('haembinaprojectreports.', '')}${JSON.stringify(values)}` };

const BRANCHES = { branches: [{ name: 'main', isMain: true }, { name: 'feature/<x>', isMain: false }] };
const PULL_REQUESTS = { pullRequests: [{ key: '7', title: 'Add reports', branch: 'feature/x' }, { key: '8', branch: 'fix' }] };

function element() {
  const listeners = {};
  const classes = new Set();
  const status = { textContent: '', classes, classList: { add: (name) => classes.add(name), remove: (name) => classes.delete(name) } };
  const button = { disabled: false, innerHTML: '' };
  const each = { disabled: false, hidden: true };
  const anchor = { saved: [] };
  anchor.click = mock.fn(() => anchor.saved.push(anchor.download));
  const copyClasses = new Set();
  const copyStatus = {
    textContent: '', classes: copyClasses,
    classList: { add: (name) => copyClasses.add(name), remove: (name) => copyClasses.delete(name) },
  };
  const box = { hidden: true, value: '', focus: mock.fn(), select: mock.fn() };
  const named = {
    '[value="together"]': button, '[value="each"]': each,
    '.haembina-reports__copy-status': copyStatus, '.haembina-reports__copy-box': box,
  };
  return {
    innerHTML: '',
    listeners,
    status,
    button,
    each,
    anchor,
    copyStatus,
    box,
    addEventListener: (type, listener) => { listeners[type] = listener; },
    removeEventListener: (type, listener) => {
      if (listeners[type] === listener) {
        delete listeners[type];
      }
    },
    querySelector: (selector) => (selector.endsWith('__status') ? status : named[selector]),
    querySelectorAll: () => [button, each],
    ownerDocument: { createElement: () => anchor, baseURI: 'https://sonar.test/project/extension/reports' },
  };
}

function answer(body, { ok = true, headers = {}, json = true, late = false } = {}) {
  return {
    ok,
    statusText: 'Bad Request',
    headers: { get: (name) => headers[name] ?? null },
    json: async () => {
      if (!json) {
        throw new SyntaxError('not JSON');
      }
      return body;
    },
    text: async () => body,
    blob: () => new Promise((resolve) => { setTimeout(() => resolve(body), late ? 20 : 0); }),
  };
}

function serve(routes) {
  globalThis.fetch = mock.fn(async (url) => {
    const path = new URL(url, 'https://sonar.test').pathname;
    const route = routes[path];
    return typeof route === 'function' ? route(url) : route;
  });
}

async function open(branchLike, routes = {}) {
  serve({
    '/api/project_branches/list': answer(BRANCHES),
    '/api/project_pull_requests/list': answer(PULL_REQUESTS),
    ...routes,
  });
  const el = element();
  const stop = registered.start({ el, baseUrl: '', component: { key: 'acme' }, branchLike, intl });
  const loading = el.innerHTML;
  await stop.loaded;
  return { el, stop, loading };
}

const ALL_SECTIONS = ['gate', 'measures', 'issues', 'hotspots', 'files', 'coverage', 'duplications'];
const ALL_FORMATS = ['json', 'markdown', 'html', 'pdf', 'epub', 'csv', 'sarif'];

function boxes(all, chosen) {
  return all.map((value) => ({ value, checked: chosen.includes(value) }));
}

function submit(el, scope, formats = ['json'], chosen = ALL_SECTIONS, as = undefined) {
  const elements = { scope: { value: scope }, formats: boxes(ALL_FORMATS, formats), sections: boxes(ALL_SECTIONS, chosen) };
  const submitter = as === undefined ? null : { value: as };
  return el.listeners.submit({ preventDefault: mock.fn(), target: { elements }, submitter });
}

function copyFrom(el, as, { scope = 'main', formats = ['json'], chosen = ['gate'], platform = 'auto' } = {}) {
  const elements = {
    scope: { value: scope }, formats: boxes(ALL_FORMATS, formats), sections: boxes(ALL_SECTIONS, chosen),
    platform: { value: platform },
  };
  return el.listeners.submit({ preventDefault: mock.fn(), target: { elements }, submitter: { value: as } });
}

function clipboard({ write = true, item = true, refuse = false, none = false } = {}) {
  const copied = [];
  const writer = mock.fn(async ([entry]) => {
    const blob = await entry.data['text/plain'];
    if (refuse) {
      throw new DOMException('Write permission denied.', 'NotAllowedError');
    }
    copied.push(await blob.text());
  });
  const navigator = write ? { clipboard: { write: writer } } : { clipboard: {} };
  Object.defineProperty(globalThis, 'navigator', { value: none ? undefined : navigator, configurable: true });
  globalThis.ClipboardItem = item ? class { constructor(data) { this.data = data; } } : undefined;
  return copied;
}

function tick(el, formats, name = 'formats') {
  el.listeners.change({ target: { name, form: { elements: { formats: boxes(ALL_FORMATS, formats) } } } });
}

describe('the Reports page', () => {
  beforeEach(() => {
    globalThis.URL.createObjectURL = mock.fn(() => 'blob:report');
    globalThis.URL.revokeObjectURL = mock.fn();
  });

  test('registers as the project page with its stylesheet', () => {
    assert.equal(registered.key, 'haembinaprojectreports/report_page');
    assert.equal(registered.css, true);
  });

  test('shows a loading line, then every branch, pull request and format', async () => {
    const { el, loading } = await open({ name: 'main', isMain: true });

    assert.match(loading, /page\.loading/u);
    const html = el.innerHTML.replaceAll('&#34;', '"');
    assert.match(html, /<option value="main" selected>page\.main_branch\{"0":"main"\}<\/option>/u);
    assert.match(html, /<option value="branch:feature\/&#60;x&#62;">feature\/&#60;x&#62;<\/option>/u);
    assert.match(html, /<option value="pullRequest:7">page\.pull_request\{"0":"7","1":"Add reports"\}/u);
    assert.match(html, /page\.pull_request\{"0":"8","1":"fix"\}/u);
    assert.equal(el.innerHTML.match(/class="haembina-reports__tile"><input type="checkbox" name="formats"/gu).length, 7);
    assert.ok(el.innerHTML.indexOf('name="sections"') < el.innerHTML.indexOf('name="formats"'));
    assert.match(el.innerHTML, /<legend[^>]*>page\.format\{\}<\/legend><p class="haembina-reports__hint">page\.format_hint\{\}<\/p>/u);
    assert.equal(el.innerHTML.match(/type="checkbox" name="sections" value="[a-z]+" checked/gu).length, 7);
    assert.match(el.innerHTML, /name="formats" value="json" checked/u);
    assert.match(el.innerHTML, /value="together"[^>]*>.*page\.download\{\}<\/button>/u);
    assert.match(el.innerHTML, /value="each"[^>]*hidden>.*page\.download_each\{\}<\/button>/u);
  });

  test('offers a zip and separate files once two or more formats are ticked', async () => {
    const { el } = await open(undefined);

    tick(el, ['json', 'pdf']);
    assert.match(el.button.innerHTML, /icon--button.*page\.download_zip\{\}/u);
    assert.equal(el.each.hidden, false);

    tick(el, ['pdf']);
    assert.match(el.button.innerHTML, /page\.download\{\}/u);
    assert.equal(el.each.hidden, true);

    tick(el, ['json', 'pdf'], 'sections');
    assert.match(el.button.innerHTML, /page\.download\{\}/u);
  });

  test('asks for every ticked format in one zip', async () => {
    const { el } = await open(undefined, {
      '/api/project_reports/download': answer('bytes', {
        headers: { 'Content-Disposition': 'attachment; filename="acme-main-reports.zip"' },
      }),
    });

    await submit(el, 'main', ['json', 'pdf'], ['gate'], 'together');

    assert.equal(globalThis.fetch.mock.calls.at(-1).arguments[0],
      '/api/project_reports/download?project=acme&format=json%2Cpdf&sections=gate');
    assert.equal(el.anchor.download, 'acme-main-reports.zip');
    assert.equal(el.anchor.click.mock.callCount(), 1);
  });

  test('downloads each ticked format as its own file, in the order ticked, counting them', async () => {
    const statuses = [];
    const { el } = await open(undefined, {
      '/api/project_reports/download': (url) => {
        statuses.push(el.status.textContent);
        const format = new URL(url, 'https://sonar.test').searchParams.get('format');
        return answer('bytes', {
          headers: { 'Content-Disposition': `attachment; filename="acme-main-report.${format}"` }, late: format === 'json',
        });
      },
    });

    await submit(el, 'main', ['json', 'csv', 'sarif'], ['gate'], 'each');

    const asked = globalThis.fetch.mock.calls.slice(-3).map((call) => call.arguments[0]);
    assert.deepEqual(asked, ['json', 'csv', 'sarif'].map((format) =>
      `/api/project_reports/download?project=acme&format=${format}&sections=gate`));
    assert.deepEqual(statuses, Array(3).fill(`page.downloading_each${JSON.stringify({ 0: 3 })}`));
    assert.deepEqual(el.anchor.saved, ['json', 'csv', 'sarif'].map((format) => `acme-main-report.${format}`));
    assert.equal(el.status.textContent, '');
    assert.equal(el.each.disabled, false);
  });

  test('saves no separate file when one of them fails', async () => {
    const { el } = await open(undefined, {
      '/api/project_reports/download': (url) => (url.includes('format=pdf')
        ? answer({ errors: [{ msg: 'gone' }] }, { ok: false }) : answer('bytes')),
    });
    const calls = globalThis.fetch.mock.callCount();

    await submit(el, 'main', ['json', 'pdf'], ['gate'], 'each');

    assert.equal(globalThis.fetch.mock.callCount(), calls + 2);
    assert.equal(el.status.textContent, `page.failed${JSON.stringify({ 0: 'gone' })}`);
    assert.equal(el.anchor.click.mock.callCount(), 0);
  });

  test('asks for a format before downloading when none is ticked', async () => {
    const { el } = await open(undefined, { '/api/project_reports/download': answer('bytes') });
    const calls = globalThis.fetch.mock.callCount();

    await submit(el, 'main', []);

    assert.equal(globalThis.fetch.mock.callCount(), calls);
    assert.equal(el.status.textContent, 'page.no_formats{}');
    assert.ok(el.status.classes.has('haembina-reports__status--error'));
  });

  test('selects the branch or pull request open in SonarQube', async () => {
    const onBranch = await open({ name: 'feature/<x>', isMain: false });
    const onPullRequest = await open({ key: '7', base: 'main', branch: 'feature/x' });
    const unknown = await open(undefined);

    assert.match(onBranch.el.innerHTML, /value="branch:feature\/&#60;x&#62;" selected/u);
    assert.match(onPullRequest.el.innerHTML, /value="pullRequest:7" selected/u);
    assert.match(unknown.el.innerHTML, /value="main" selected/u);
  });

  test('leaves out pull requests where the server lists none', async () => {
    const { el } = await open(undefined, {
      '/api/project_branches/list': answer(null, { ok: false }),
      '/api/project_pull_requests/list': answer(null, { ok: false }),
    });

    assert.doesNotMatch(el.innerHTML, /optgroup label="page\.pull_requests/u);
    assert.doesNotMatch(el.innerHTML, /<option value="(main|branch:|pullRequest:)/u);
  });

  test('downloads the chosen report under the name the server gives it', async () => {
    const { el } = await open(undefined, {
      '/api/project_reports/download': answer('bytes', {
        headers: { 'Content-Disposition': 'attachment; filename="acme-pr-7-report.sarif"' },
      }),
    });

    await submit(el, 'pullRequest:7', ['sarif']);

    const url = globalThis.fetch.mock.calls.at(-1).arguments[0];
    assert.equal(url, '/api/project_reports/download?project=acme&format=sarif&sections=gate%2Cmeasures%2Cissues%2Chotspots%2Cfiles%2Ccoverage%2Cduplications&pullRequest=7');
    assert.equal(el.anchor.download, 'acme-pr-7-report.sarif');
    assert.equal(el.anchor.click.mock.callCount(), 1);
    assert.equal(globalThis.URL.revokeObjectURL.mock.calls[0].arguments[0], 'blob:report');
    assert.equal(el.status.textContent, '');
    assert.equal(el.button.disabled, false);
  });

  test('asks for the main branch without a branch and names a file without a header', async () => {
    const { el } = await open(undefined, { '/api/project_reports/download': answer('bytes') });

    await submit(el, 'main', ['html'], ['coverage', 'duplications']);

    assert.equal(globalThis.fetch.mock.calls.at(-1).arguments[0],
      '/api/project_reports/download?project=acme&format=html&sections=coverage%2Cduplications');
    assert.equal(el.anchor.download, 'report');
  });

  test('asks for a section before downloading when none is chosen', async () => {
    const { el } = await open(undefined, { '/api/project_reports/download': answer('bytes') });
    const calls = globalThis.fetch.mock.callCount();

    await submit(el, 'main', ['pdf'], []);

    assert.equal(globalThis.fetch.mock.callCount(), calls);
    assert.equal(el.status.textContent, 'page.no_sections{}');
    assert.ok(el.status.classes.has('haembina-reports__status--error'));
  });

  test('says why a report could not be prepared', async () => {
    const reasons = [
      [answer({ errors: [{ msg: 'needs the plugin' }] }, { ok: false }), 'needs the plugin'],
      [answer({}, { ok: false }), 'Bad Request'],
      [answer(null, { ok: false, json: false }), 'Bad Request'],
    ];
    for (const [response, reason] of reasons) {
      const { el } = await open(undefined, { '/api/project_reports/download': response });

      await submit(el, 'branch:dev');

      assert.equal(el.status.textContent, `page.failed${JSON.stringify({ 0: reason })}`);
      assert.ok(el.status.classes.has('haembina-reports__status--error'));
      assert.equal(el.button.disabled, false);
    }
  });

  test('stops listening and empties the page when SonarQube leaves it', async () => {
    const { el, stop } = await open(undefined);

    stop();

    assert.equal(el.listeners.submit, undefined);
    assert.equal(el.listeners.change, undefined);
    assert.equal(el.innerHTML, '');
  });
});

describe('copying from the Reports page', () => {
  const COMMENT = '### acme · Quality gate: ✓ Passed\n';

  test('offers a platform and a button for the comment, the status line and the link', async () => {
    const { el } = await open(undefined);

    assert.match(el.innerHTML, /<legend[^>]*>page\.copy\{\}<\/legend><p class="haembina-reports__hint">page\.copy_hint\{\}/u);
    assert.match(el.innerHTML, /<select name="platform" class="haembina-reports__select" aria-label="page\.platform\{\}">/u);
    assert.deepEqual([...el.innerHTML.matchAll(/<option value="([a-z]+)">page\.platform\./gu)].map((found) => found[1]),
      ['auto', 'github', 'gitlab', 'bitbucket', 'gitea', 'azure']);
    assert.deepEqual([...el.innerHTML.matchAll(/value="(copy-[a-z]+)"/gu)].map((found) => found[1]),
      ['copy-comment', 'copy-line', 'copy-link']);
    assert.match(el.innerHTML, /<textarea class="haembina-reports__copy-box" readonly hidden aria-label="page\.copy_text\{\}">/u);
  });

  test('copies a comment shaped for the chosen platform while it is still being fetched', async () => {
    const copied = clipboard();
    const { el } = await open(undefined, { '/api/project_reports/download': answer(COMMENT) });

    await copyFrom(el, 'copy-comment', { scope: 'pullRequest:7', chosen: ['gate', 'issues'], platform: 'gitlab' });

    assert.equal(globalThis.fetch.mock.calls.at(-1).arguments[0], '/api/project_reports/download?project=acme'
      + '&format=markdown&sections=gate%2Cissues&layout=comment&flavor=gitlab&pullRequest=7');
    assert.deepEqual(copied, [COMMENT]);
    assert.equal(el.copyStatus.textContent, 'page.copied_comment{}');
    assert.equal(el.box.hidden, true);
  });

  test('copies one status line for the platform the project names', async () => {
    const copied = clipboard();
    const { el } = await open(undefined, { '/api/project_reports/download': answer('**acme** · Main branch\n') });

    await copyFrom(el, 'copy-line');

    assert.match(globalThis.fetch.mock.calls.at(-1).arguments[0], /&layout=line&flavor=auto$/u);
    assert.deepEqual(copied, ['**acme** · Main branch\n']);
    assert.equal(el.copyStatus.textContent, 'page.copied_line{}');
  });

  test('copies the whole address of the report as chosen without fetching it', async () => {
    const copied = clipboard();
    const { el } = await open(undefined);
    const calls = globalThis.fetch.mock.callCount();

    await copyFrom(el, 'copy-link', { scope: 'branch:dev', formats: ['pdf', 'sarif'] });

    assert.equal(globalThis.fetch.mock.callCount(), calls);
    assert.deepEqual(copied,
      ['https://sonar.test/api/project_reports/download?project=acme&format=pdf%2Csarif&sections=gate&branch=dev']);
    assert.equal(el.copyStatus.textContent, 'page.copied_link{}');
  });

  test('selects the text in a box where the clipboard is closed or missing', async () => {
    for (const closed of [{ refuse: true }, { write: false }, { item: false }, { none: true }]) {
      clipboard(closed);
      const { el } = await open(undefined, { '/api/project_reports/download': answer(COMMENT) });

      await copyFrom(el, 'copy-comment');

      assert.equal(el.box.hidden, false);
      assert.equal(el.box.value, COMMENT);
      assert.equal(el.box.focus.mock.callCount(), 1);
      assert.equal(el.box.select.mock.callCount(), 1);
      assert.equal(el.copyStatus.textContent, 'page.copy_manual{}');
    }
  });

  test('says why the text could not be prepared and copies nothing', async () => {
    for (const closed of [{}, { write: false }]) {
      const copied = clipboard(closed);
      const { el } = await open(undefined, {
        '/api/project_reports/download': answer({ errors: [{ msg: 'gone' }] }, { ok: false }),
      });

      await copyFrom(el, 'copy-comment');

      assert.deepEqual(copied, []);
      assert.equal(el.copyStatus.textContent, `page.copy_failed${JSON.stringify({ 0: 'gone' })}`);
      assert.ok(el.copyStatus.classes.has('haembina-reports__status--error'));
      assert.equal(el.box.hidden, true);
    }
  });

  test('asks for a section, and for the link a format, before copying', async () => {
    const copied = clipboard();
    const { el } = await open(undefined);
    const calls = globalThis.fetch.mock.callCount();

    await copyFrom(el, 'copy-line', { chosen: [] });
    assert.equal(el.copyStatus.textContent, 'page.no_sections{}');

    await copyFrom(el, 'copy-link', { formats: [] });
    assert.equal(el.copyStatus.textContent, 'page.no_formats{}');
    assert.ok(el.copyStatus.classes.has('haembina-reports__status--error'));

    await copyFrom(el, 'copy-link');
    assert.equal(el.copyStatus.classes.has('haembina-reports__status--error'), false);
    assert.equal(globalThis.fetch.mock.callCount(), calls);
    assert.equal(copied.length, 1);
  });
});
