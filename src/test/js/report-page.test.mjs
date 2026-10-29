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
  const anchor = { click: mock.fn() };
  const named = { '[value="together"]': button, '[value="each"]': each };
  return {
    innerHTML: '',
    listeners,
    status,
    button,
    each,
    anchor,
    addEventListener: (type, listener) => { listeners[type] = listener; },
    removeEventListener: (type, listener) => {
      if (listeners[type] === listener) {
        delete listeners[type];
      }
    },
    querySelector: (selector) => (selector.endsWith('__status') ? status : named[selector]),
    querySelectorAll: () => [button, each],
    ownerDocument: { createElement: () => anchor },
  };
}

function answer(body, { ok = true, headers = {}, json = true } = {}) {
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
    blob: async () => body,
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
    assert.equal(el.innerHTML.match(/type="checkbox" name="formats"/gu).length, 7);
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

  test('downloads each ticked format as its own file, counting them', async () => {
    const statuses = [];
    const { el } = await open(undefined, {
      '/api/project_reports/download': (url) => {
        statuses.push(el.status.textContent);
        const format = new URL(url, 'https://sonar.test').searchParams.get('format');
        return answer('bytes', { headers: { 'Content-Disposition': `attachment; filename="acme-main-report.${format}"` } });
      },
    });

    await submit(el, 'main', ['json', 'csv', 'sarif'], ['gate'], 'each');

    const asked = globalThis.fetch.mock.calls.slice(-3).map((call) => call.arguments[0]);
    assert.deepEqual(asked, ['json', 'csv', 'sarif'].map((format) =>
      `/api/project_reports/download?project=acme&format=${format}&sections=gate`));
    assert.deepEqual(statuses, [1, 2, 3].map((index) => `page.downloading_each${JSON.stringify({ 0: index, 1: 3 })}`));
    assert.equal(el.anchor.click.mock.callCount(), 3);
    assert.equal(el.anchor.download, 'acme-main-report.sarif');
    assert.equal(el.status.textContent, '');
    assert.equal(el.each.disabled, false);
  });

  test('stops at the first separate file that fails', async () => {
    const { el } = await open(undefined, {
      '/api/project_reports/download': answer({ errors: [{ msg: 'gone' }] }, { ok: false }),
    });
    const calls = globalThis.fetch.mock.callCount();

    await submit(el, 'main', ['json', 'pdf'], ['gate'], 'each');

    assert.equal(globalThis.fetch.mock.callCount(), calls + 1);
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
    assert.doesNotMatch(el.innerHTML, /<option/u);
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
