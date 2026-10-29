const PLUGIN = 'haembinaprojectreports';
const BLOCK = 'haembina-reports';
const FORMATS = ['json', 'markdown', 'html', 'pdf', 'epub', 'csv', 'sarif'];
const SECTIONS = ['gate', 'measures', 'issues', 'hotspots', 'files', 'coverage', 'duplications'];
const FILE_NAME = /filename="([^"]+)"/u;
const ICONS = {
  json: ['M7 4a2 2 0 0 0 -2 2v3a2 3 0 0 1 -2 3a2 3 0 0 1 2 3v3a2 2 0 0 0 2 2',
    'M17 4a2 2 0 0 1 2 2v3a2 3 0 0 0 2 3a2 3 0 0 0 -2 3v3a2 2 0 0 1 -2 2'],
  markdown: ['M3 7a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-10', 'M7 15v-6l2 2l2 -2v6',
    'M14 13l2 2l2 -2m-2 2v-6'],
  html: ['M14 3v4a1 1 0 0 0 1 1h4', 'M5 12v-7a2 2 0 0 1 2 -2h7l5 5v4', 'M2 21v-6', 'M5 15v6', 'M2 18h3', 'M20 15v6h2',
    'M13 21v-6l2 3l2 -3v6', 'M7.5 15h3', 'M9 15v6'],
  pdf: ['M14 3v4a1 1 0 0 0 1 1h4', 'M5 12v-7a2 2 0 0 1 2 -2h7l5 5v4', 'M5 18h1.5a1.5 1.5 0 0 0 0 -3h-1.5v6', 'M17 18h2',
    'M20 15h-3v6', 'M11 15v6h1a2 2 0 0 0 2 -2v-2a2 2 0 0 0 -2 -2h-1'],
  epub: ['M3 19a9 9 0 0 1 9 0a9 9 0 0 1 9 0', 'M3 6a9 9 0 0 1 9 0a9 9 0 0 1 9 0', 'M3 6l0 13', 'M12 6l0 13',
    'M21 6l0 13'],
  csv: ['M14 3v4a1 1 0 0 0 1 1h4', 'M5 12v-7a2 2 0 0 1 2 -2h7l5 5v4', 'M7 16.5a1.5 1.5 0 0 0 -3 0v3a1.5 1.5 0 0 0 3 0',
    'M10 20.25c0 .414 .336 .75 .75 .75h1.25a1 1 0 0 0 1 -1v-1a1 1 0 0 0 -1 -1h-1a1 1 0 0 1 -1 -1v-1a1 1 0 0 1 1 -1h1.25a.75 .75 0 0 1 .75 .75',
    'M16 15l2 6l2 -6'],
  sarif: ['M12 21a12 12 0 0 1 -8.5 -15a12 12 0 0 0 8.5 -3a12 12 0 0 0 8.5 3c.539 1.832 .627 3.747 .283 5.588',
    'M15 18a3 3 0 1 0 6 0a3 3 0 1 0 -6 0', 'M20.2 20.2l1.8 1.8'],
  zip: ['M6 20.735a2 2 0 0 1 -1 -1.735v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2h-1',
    'M11 17a2 2 0 0 1 2 2v2a1 1 0 0 1 -1 1h-2a1 1 0 0 1 -1 -1v-2a2 2 0 0 1 2 -2z', 'M11 5l-1 0', 'M13 7l-1 0', 'M11 9l-1 0',
    'M13 11l-1 0', 'M11 13l-1 0', 'M13 15l-1 0'],
  download: ['M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2 -2v-2', 'M7 11l5 5l5 -5', 'M12 4l0 12'],
  chevron: ['M6 9l6 6l6 -6'],
  gate: ['M11.46 20.846a12 12 0 0 1 -7.96 -14.846a12 12 0 0 0 8.5 -3a12 12 0 0 0 8.5 3a12 12 0 0 1 -.09 7.06',
    'M15 19l2 2l4 -4'],
  measures: ['M3 13a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v6a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1l0 -6',
    'M15 9a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v10a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1l0 -10',
    'M9 5a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v14a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1l0 -14', 'M4 20h14'],
  issues: ['M3 12a9 9 0 1 0 18 0a9 9 0 0 0 -18 0', 'M12 8v4', 'M12 16h.01'],
  hotspots: ['M12 10.941c2.333 -3.308 .167 -7.823 -1 -8.941c0 3.395 -2.235 5.299 -3.667 6.706c-1.43 1.408 -2.333 3.294 -2.333 5.588c0 3.704 3.134 6.706 7 6.706c3.866 0 7 -3.002 7 -6.706c0 -1.712 -1.232 -4.403 -2.333 -5.588c-2.084 3.353 -3.257 3.353 -4.667 2.235'],
  files: ['M15 3v4a1 1 0 0 0 1 1h4', 'M18 17h-7a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2h4l5 5v7a2 2 0 0 1 -2 2',
    'M16 17v2a2 2 0 0 1 -2 2h-7a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2h2'],
  coverage: ['M4 12a8 8 0 0 1 16 0l-16 0', 'M12 12v6a2 2 0 0 0 4 0'],
  duplications: ['M7 9.667a2.667 2.667 0 0 1 2.667 -2.667h8.666a2.667 2.667 0 0 1 2.667 2.667v8.666a2.667 2.667 0 0 1 -2.667 2.667h-8.666a2.667 2.667 0 0 1 -2.667 -2.667l0 -8.666',
    'M4.012 16.737a2.005 2.005 0 0 1 -1.012 -1.737v-10c0 -1.1 .9 -2 2 -2h10c.75 0 1.158 .385 1.5 1'],
};

function escape(value) {
  return String(value).replace(/[&<>"']/gu, (character) => `&#${character.codePointAt(0)};`);
}

function textFor(intl) {
  return (key, ...values) => {
    const named = Object.fromEntries(values.map((value, index) => [String(index), value]));
    return intl.formatMessage({ id: `${PLUGIN}.${key}` }, named);
  };
}

async function getJson(baseUrl, path, params) {
  const response = await fetch(`${baseUrl}/${path}?${new URLSearchParams(params)}`, { credentials: 'same-origin' });
  return response.ok ? response.json() : null;
}

function scopesFrom(branches, pullRequests, text) {
  const branchScopes = (branches?.branches ?? []).map((branch) => ({
    value: branch.isMain ? 'main' : `branch:${branch.name}`,
    label: branch.isMain ? text('page.main_branch', branch.name) : branch.name,
  }));
  const pullRequestScopes = (pullRequests?.pullRequests ?? []).map((pullRequest) => ({
    value: `pullRequest:${pullRequest.key}`,
    label: text('page.pull_request', pullRequest.key, pullRequest.title || pullRequest.branch),
  }));
  return { branchScopes, pullRequestScopes };
}

function currentScope(branchLike) {
  if (branchLike?.key !== undefined && branchLike?.base !== undefined) {
    return `pullRequest:${branchLike.key}`;
  }
  return branchLike && !branchLike.isMain && branchLike.name ? `branch:${branchLike.name}` : 'main';
}

function icon(name, modifier) {
  const paths = ICONS[name].map((path) => `<path d="${path}"/>`).join('');
  return `<svg class="${BLOCK}__icon ${BLOCK}__icon--${modifier}" viewBox="0 0 24 24" aria-hidden="true" `
    + `focusable="false" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" `
    + `stroke-linejoin="round">${paths}</svg>`;
}

function options(scopes, selected) {
  return scopes.map(({ value, label }) =>
    `<option value="${escape(value)}"${value === selected ? ' selected' : ''}>${escape(label)}</option>`).join('');
}

function form(state, text) {
  const { branchScopes, pullRequestScopes } = state.scopes;
  const pullRequests = pullRequestScopes.length === 0 ? ''
    : `<optgroup label="${escape(text('page.pull_requests'))}">${options(pullRequestScopes, state.selected)}</optgroup>`;
  const formats = FORMATS.map((format) => {
    const checked = format === FORMATS[0] ? ' checked' : '';
    const name = escape(text(`format.${format}`));
    const note = escape(text(`format.${format}.description`));
    return `<label class="${BLOCK}__tile"><input type="checkbox" name="formats" value="${format}"${checked}>`
      + `${icon(format, 'tile')}<span class="${BLOCK}__choice-name">${name}</span>`
      + `<span class="${BLOCK}__choice-note">${note}</span></label>`;
  }).join('');
  const sections = SECTIONS.map((section) => {
    const name = escape(text(`section.${section}`));
    const note = escape(text(`section.${section}.description`));
    return `<label class="${BLOCK}__choice"><input type="checkbox" name="sections" value="${section}" checked>`
      + `${icon(section, 'choice')}`
      + `<span class="${BLOCK}__choice-name">${name}</span><span class="${BLOCK}__choice-note">${note}</span></label>`;
  }).join('');
  return `<form class="${BLOCK}__card">`
    + `<div><label class="${BLOCK}__label" for="${BLOCK}-scope">${escape(text('page.scope'))}</label>`
    + `<div class="${BLOCK}__picker"><select id="${BLOCK}-scope" name="scope" class="${BLOCK}__select">`
    + `<optgroup label="${escape(text('page.branches'))}">${options(branchScopes, state.selected)}</optgroup>`
    + `${pullRequests}</select>${icon('chevron', 'chevron')}</div></div>`
    + `<fieldset class="${BLOCK}__fieldset"><legend class="${BLOCK}__label">${escape(text('page.sections'))}</legend>`
    + `<div class="${BLOCK}__choices">${sections}</div></fieldset>`
    + `<fieldset class="${BLOCK}__fieldset"><legend class="${BLOCK}__label">${escape(text('page.format'))}</legend>`
    + `<p class="${BLOCK}__hint">${escape(text('page.format_hint'))}</p>`
    + `<div class="${BLOCK}__tiles">${formats}</div></fieldset>`
    + `<div class="${BLOCK}__actions">`
    + `<button type="submit" name="as" value="together" class="${BLOCK}__button">${togetherLabel(1, text)}</button>`
    + `<button type="submit" name="as" value="each" class="${BLOCK}__button ${BLOCK}__button--secondary" hidden>`
    + `${icon('download', 'button')}${escape(text('page.download_each'))}</button>`
    + `<p class="${BLOCK}__status" role="status" aria-live="polite"></p></div></form>`;
}

function togetherLabel(count, text) {
  return count > 1 ? `${icon('zip', 'button')}${escape(text('page.download_zip'))}`
    : `${icon('download', 'button')}${escape(text('page.download'))}`;
}

function checkedValues(boxes) {
  return Array.from(boxes).filter((box) => box.checked).map((box) => box.value);
}

function offerFormats(el, changed, text) {
  if (changed.name !== 'formats') {
    return;
  }
  const count = checkedValues(changed.form.elements.formats).length;
  el.querySelector('[value="together"]').innerHTML = togetherLabel(count, text);
  el.querySelector('[value="each"]').hidden = count < 2;
}

function render(el, state, text) {
  const body = state.scopes ? form(state, text)
    : `<p class="${BLOCK}__status" role="status" aria-live="polite">${escape(text('page.loading'))}</p>`;
  el.innerHTML = `<div class="${BLOCK}"><h1 class="${BLOCK}__title">${escape(text('page.title'))}</h1>${body}</div>`;
}

function reportUrl(baseUrl, project, scope, formats, sections) {
  const params = new URLSearchParams({ project, format: formats.join(','), sections: sections.join(',') });
  const [kind, value] = scope.split(/:(.*)/su);
  if (kind !== 'main') {
    params.set(kind, value);
  }
  return `${baseUrl}/api/project_reports/download?${params}`;
}

async function failureOf(response) {
  try {
    const body = await response.json();
    return body.errors?.[0]?.msg ?? response.statusText;
  } catch {
    return response.statusText;
  }
}

function fail(status, message) {
  status.textContent = message;
  status.classList.add(`${BLOCK}__status--error`);
}

async function save(el, response) {
  const name = FILE_NAME.exec(response.headers.get('Content-Disposition') ?? '')?.[1] ?? 'report';
  const link = el.ownerDocument.createElement('a');
  link.href = URL.createObjectURL(await response.blob());
  link.download = name;
  link.click();
  URL.revokeObjectURL(link.href);
}

async function download(el, start, submitted, as) {
  const { baseUrl, component, text } = start;
  const status = el.querySelector(`.${BLOCK}__status`);
  const buttons = Array.from(el.querySelectorAll(`.${BLOCK}__button`));
  const fields = submitted.elements;
  const formats = checkedValues(fields.formats);
  const sections = checkedValues(fields.sections);
  if (formats.length === 0 || sections.length === 0) {
    fail(status, text(formats.length === 0 ? 'page.no_formats' : 'page.no_sections'));
    return;
  }
  const batches = as === 'each' ? formats.map((format) => [format]) : [formats];
  status.textContent = batches.length > 1 ? text('page.downloading_each', batches.length) : text('page.downloading');
  status.classList.remove(`${BLOCK}__status--error`);
  buttons.forEach((button) => { button.disabled = true; });
  try {
    const responses = await Promise.all(batches.map((batch) =>
      fetch(reportUrl(baseUrl, component.key, fields.scope.value, batch, sections), { credentials: 'same-origin' })));
    const failed = responses.find((response) => !response.ok);
    if (failed) {
      fail(status, text('page.failed', await failureOf(failed)));
      return;
    }
    await Promise.all(responses.map((response) => save(el, response)));
    status.textContent = '';
  } finally {
    buttons.forEach((button) => { button.disabled = false; });
  }
}

function startReportPage({ el, baseUrl, component, branchLike, intl }) {
  const text = textFor(intl);
  const state = { scopes: null, selected: currentScope(branchLike) };
  const onSubmit = (event) => {
    event.preventDefault();
    return download(el, { baseUrl, component, text }, event.target, event.submitter?.value);
  };
  const onChange = (event) => offerFormats(el, event.target, text);
  render(el, state, text);
  el.addEventListener('submit', onSubmit);
  el.addEventListener('change', onChange);
  const loaded = Promise.all([
    getJson(baseUrl, 'api/project_branches/list', { project: component.key }),
    getJson(baseUrl, 'api/project_pull_requests/list', { project: component.key }),
  ]).then(([branches, pullRequests]) => {
    state.scopes = scopesFrom(branches, pullRequests, text);
    render(el, state, text);
  });
  const stop = () => {
    el.removeEventListener('submit', onSubmit);
    el.removeEventListener('change', onChange);
    el.innerHTML = '';
  };
  stop.loaded = loaded;
  return stop;
}

globalThis.registerExtension(`${PLUGIN}/report_page`, startReportPage, true);
