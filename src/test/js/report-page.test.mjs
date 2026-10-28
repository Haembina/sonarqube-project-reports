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
  const button = { disabled: false };
  const anchor = { click: mock.fn() };
  return {
    innerHTML: '',
    listeners,
    status,
    button,
    anchor,
    addEventListener: (type, listener) => { listeners[type] = listener; },
    removeEventListener: (type, listener) => {
      if (listeners[type] === listener) {
        delete listeners[type];
      }
    },
    querySelector: (selector) => (selector.endsWith('__status') ? status : button),
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

function submit(el, scope, format = 'json', chosen = ALL_SECTIONS) {
  const sections = ALL_SECTIONS.map((value) => ({ value, checked: chosen.includes(value) }));
  const elements = { scope: { value: scope }, format: { value: format }, sections };
  return el.listeners.submit({ preventDefault: mock.fn(), target: { elements } });
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
    assert.equal(el.innerHTML.match(/type="radio"/gu).length, 7);
    assert.equal(el.innerHTML.match(/type="checkbox" name="sections" value="[a-z]+" checked/gu).length, 7);
    assert.match(el.innerHTML, /value="json" checked/u);
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

    await submit(el, 'pullRequest:7', 'sarif');

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

    await submit(el, 'main', 'html', ['coverage', 'duplications']);

    assert.equal(globalThis.fetch.mock.calls.at(-1).arguments[0],
      '/api/project_reports/download?project=acme&format=html&sections=coverage%2Cduplications');
    assert.equal(el.anchor.download, 'report');
  });

  test('asks for a section before downloading when none is chosen', async () => {
    const { el } = await open(undefined, { '/api/project_reports/download': answer('bytes') });
    const calls = globalThis.fetch.mock.callCount();

    await submit(el, 'main', 'pdf', []);

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
    assert.equal(el.innerHTML, '');
  });
});
