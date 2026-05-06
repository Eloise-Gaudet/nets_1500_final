'use strict';

// ── API helpers ──────────────────────────────────────────────────────────────

async function api(path) {
  const res = await fetch('/api' + path);
  if (!res.ok) throw new Error('HTTP ' + res.status);
  return res.json();
}

// ── Utility ───────────────────────────────────────────────────────────────────

function fmtScore(s) {
  if (s === undefined || s === null) return '—';
  if (Number.isInteger(s)) return s.toLocaleString();
  if (s < 0.0001) return s.toExponential(3);
  return s.toPrecision(5);
}

function roleTag(p) {
  const tags = [];
  if (p.isActor)    tags.push('<span class="badge badge-actor">Actor</span>');
  if (p.isDirector) tags.push('<span class="badge badge-director">Director</span>');
  return tags.join(' ');
}

function spinner() {
  return '<span class="spinner"></span>';
}

// ── Tab switching ─────────────────────────────────────────────────────────────

function initTabs() {
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const name = btn.dataset.tab;
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab').forEach(t => t.classList.add('hidden'));
      btn.classList.add('active');
      document.getElementById('tab-' + name).classList.remove('hidden');
    });
  });
}

// ── Autocomplete helper ───────────────────────────────────────────────────────
// Returns a controller object: { setChosen(p), getChosen() }

function makeAutocomplete(inputEl, dropdownEl, onSelect) {
  let debounce;
  let chosen = null;

  inputEl.addEventListener('input', () => {
    clearTimeout(debounce);
    const q = inputEl.value.trim();
    if (q.length < 2) { dropdownEl.innerHTML = ''; dropdownEl.classList.add('hidden'); return; }
    debounce = setTimeout(async () => {
      try {
        const results = await api('/search?name=' + encodeURIComponent(q));
        if (!results.length) { dropdownEl.innerHTML = '<div class="ac-item" style="color:var(--muted)">No results</div>'; dropdownEl.classList.remove('hidden'); return; }
        dropdownEl.innerHTML = results.map(p =>
          `<div class="ac-item" data-id="${p.id}">
            <span>${escHtml(p.name)}</span>
            <span class="ac-tags">${p.isActor ? '<span class="tag">Actor</span>' : ''}${p.isDirector ? '<span class="tag">Director</span>' : ''}</span>
          </div>`
        ).join('');
        dropdownEl.classList.remove('hidden');
        dropdownEl.querySelectorAll('.ac-item').forEach((el, i) => {
          el.addEventListener('click', () => {
            inputEl.value = results[i].name;
            dropdownEl.innerHTML = '';
            dropdownEl.classList.add('hidden');
            chosen = results[i];
            onSelect(results[i]);
          });
        });
      } catch (e) { /* ignore */ }
    }, 280);
  });

  document.addEventListener('click', e => {
    if (!inputEl.contains(e.target) && !dropdownEl.contains(e.target)) {
      dropdownEl.classList.add('hidden');
    }
  });

  return {
    getChosen: () => chosen,
    clear: () => { chosen = null; inputEl.value = ''; dropdownEl.innerHTML = ''; }
  };
}

function escHtml(s) {
  return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

// ── Chosen badge ──────────────────────────────────────────────────────────────

function showChosen(badgeEl, person, onClear) {
  badgeEl.innerHTML =
    `<span>◈ ${escHtml(person.name)}</span>
     <button title="Clear">✕</button>`;
  badgeEl.classList.remove('hidden');
  badgeEl.querySelector('button').addEventListener('click', onClear);
}

function clearChosen(badgeEl) {
  badgeEl.innerHTML = '';
  badgeEl.classList.add('hidden');
}

// ── Overview / stats ──────────────────────────────────────────────────────────

async function loadStats() {
  try {
    const stats = await api('/stats');
    document.getElementById('stat-people').textContent = stats.personCount.toLocaleString();
    document.getElementById('header-stats').textContent =
      stats.personCount.toLocaleString() + ' people in graph';
  } catch (e) {
    document.getElementById('header-stats').textContent = 'Graph not loaded';
  }
}

// ── Search tab ────────────────────────────────────────────────────────────────

let searchSelectedId = null;

function initSearch() {
  const input   = document.getElementById('search-input');
  const btn     = document.getElementById('search-btn');
  const results = document.getElementById('search-results');

  async function doSearch() {
    const q = input.value.trim();
    if (!q) return;
    results.innerHTML = spinner() + ' Searching…';
    try {
      const data = await api('/search?name=' + encodeURIComponent(q));
      if (!data.length) { results.innerHTML = '<p class="no-result">No results found.</p>'; return; }
      results.innerHTML = data.map(p =>
        `<div class="result-item" data-id="${p.id}">
          <div>
            <div class="r-name">${escHtml(p.name)}</div>
            <div class="r-id">${p.id}</div>
          </div>
          <div>${roleTag(p)}</div>
        </div>`
      ).join('');
      results.querySelectorAll('.result-item').forEach(el => {
        el.addEventListener('click', () => loadPersonCard(el.dataset.id));
      });
    } catch (e) {
      results.innerHTML = '<p class="no-result">Error during search.</p>';
    }
  }

  btn.addEventListener('click', doSearch);
  input.addEventListener('keydown', e => { if (e.key === 'Enter') doSearch(); });
}

async function loadPersonCard(id) {
  const card      = document.getElementById('person-card');
  const nameEl    = document.getElementById('pc-name');
  const metaEl    = document.getElementById('pc-meta');
  const degreeEl  = document.getElementById('pc-degree');
  const neighbEl  = document.getElementById('pc-neighbors');
  const graphBtn  = document.getElementById('pc-graph-btn');

  card.classList.remove('hidden');
  nameEl.textContent    = 'Loading…';
  metaEl.innerHTML      = '';
  degreeEl.textContent  = '…';
  neighbEl.innerHTML    = spinner();

  try {
    const p = await api('/persons/' + encodeURIComponent(id));
    nameEl.textContent   = p.name;
    metaEl.innerHTML     = roleTag(p) + ` <span style="color:var(--muted);font-size:.8rem">${p.id}</span>`;
    degreeEl.textContent = p.degree.toLocaleString();

    if (!p.neighbors.length) {
      neighbEl.innerHTML = '<p class="no-result">No collaborators found.</p>';
    } else {
      neighbEl.innerHTML =
        `<div class="neighbor-grid">` +
        p.neighbors.map(n => {
          const titleList = n.sharedTitleNames && n.sharedTitleNames.length
            ? `<div class="n-titles">${n.sharedTitleNames.map(escHtml).join(', ')}</div>`
            : '';
          return `<div class="neighbor-item" data-id="${n.id}">
            <div class="n-name">${escHtml(n.name)}</div>
            <div class="n-count">${n.sharedTitles} shared title${n.sharedTitles !== 1 ? 's' : ''}</div>
            ${titleList}
          </div>`;
        }).join('') +
        `</div>`;
      neighbEl.querySelectorAll('.neighbor-item').forEach(el => {
        el.addEventListener('click', () => loadPersonCard(el.dataset.id));
      });
    }

    searchSelectedId = id;
    graphBtn.onclick = () => {
      loadGraphForPerson({ id: p.id, name: p.name });
      document.querySelector('[data-tab="graph"]').click();
    };

    card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  } catch (e) {
    nameEl.textContent = 'Failed to load person.';
  }
}

// ── Centrality tab ────────────────────────────────────────────────────────────

function initCentrality() {
  const typeEl      = document.getElementById('c-type');
  const filterEl    = document.getElementById('c-filter');
  const topNEl      = document.getElementById('c-topN');
  const titleTypeEl = document.getElementById('c-title-type');
  const genreEl     = document.getElementById('c-genre');
  const startYearEl = document.getElementById('c-start-year');
  const endYearEl   = document.getElementById('c-end-year');
  const btn         = document.getElementById('c-btn');
  const warning     = document.getElementById('c-warning');
  const results     = document.getElementById('c-results');

  typeEl.addEventListener('change', () => {
    const slow = typeEl.value !== 'degree';
    warning.classList.toggle('hidden', !slow);
  });

  btn.addEventListener('click', async () => {
    const type      = typeEl.value;
    const filter    = filterEl.value;
    const topN      = parseInt(topNEl.value, 10) || 10;
    const actors    = filter === 'actors'    ? '&actorsOnly=true'    : '';
    const directors = filter === 'directors' ? '&directorsOnly=true' : '';

    const titleType = titleTypeEl.value;
    const genre     = genreEl.value;
    const startYear = startYearEl.value.trim();
    const endYear   = endYearEl.value.trim();

    let filterParams = '';
    if (titleType !== '')        filterParams += `&type=${titleType}`;
    if (genre)                   filterParams += `&genres=${encodeURIComponent(genre)}`;
    if (startYear && endYear)    filterParams += `&startYear=${startYear}&endYear=${endYear}`;

    results.innerHTML = spinner() + ' Computing…';
    btn.disabled = true;

    try {
      const data = await api(`/centrality/${type}?topN=${topN}${actors}${directors}${filterParams}`);
      if (!data.length) { results.innerHTML = '<p class="no-result">No results.</p>'; return; }

      const maxScore = data[0].score;
      results.innerHTML =
        `<table class="rank-table">
          <thead><tr>
            <th>#</th>
            <th>Name</th>
            <th>Score</th>
            <th class="rank-bar-cell"></th>
          </tr></thead>
          <tbody>` +
        data.map((r, i) => {
          const pct = maxScore > 0 ? (r.score / maxScore * 100).toFixed(1) : 0;
          return `<tr>
            <td class="rank-num">${i + 1}</td>
            <td class="rank-name">
              <a href="#" data-id="${r.id}" class="person-link">${escHtml(r.name)}</a>
              <div style="font-size:.75rem;color:var(--muted)">${r.id}</div>
            </td>
            <td class="rank-score">${fmtScore(r.score)}</td>
            <td class="rank-bar-cell">
              <div class="rank-bar-track">
                <div class="rank-bar-fill" style="width:${pct}%"></div>
              </div>
            </td>
          </tr>`;
        }).join('') +
        `</tbody></table>`;

      results.querySelectorAll('.person-link').forEach(el => {
        el.addEventListener('click', e => {
          e.preventDefault();
          document.querySelector('[data-tab="search"]').click();
          loadPersonCard(el.dataset.id);
        });
      });
    } catch (e) {
      results.innerHTML = '<p class="no-result">Error computing centrality.</p>';
    } finally {
      btn.disabled = false;
    }
  });
}

// ── Path finder ───────────────────────────────────────────────────────────────

function initPathFinder() {
  const btn     = document.getElementById('path-btn');
  const results = document.getElementById('path-results');

  const ppFrom = initPersonPicker('pp-from');
  const ppTo   = initPersonPicker('pp-to');

  btn.addEventListener('click', async () => {
    const from = ppFrom.getChosen();
    const to   = ppTo.getChosen();
    if (!from) { alert('Please select Person A.'); return; }
    if (!to)   { alert('Please select Person B.'); return; }
    if (from.id === to.id) { results.innerHTML = '<p class="no-result">Same person — path length is 0.</p>'; return; }

    const titleType = document.getElementById('path-title-type').value;
    const genre     = document.getElementById('path-genre').value;
    const startYear = document.getElementById('path-start-year').value.trim();
    const endYear   = document.getElementById('path-end-year').value.trim();

    let filterParams = '';
    if (titleType)            filterParams += `&type=${titleType}`;
    if (genre)                filterParams += `&genres=${encodeURIComponent(genre)}`;
    if (startYear && endYear) filterParams += `&startYear=${startYear}&endYear=${endYear}`;

    results.innerHTML = spinner() + ' Searching for path (BFS)…';
    btn.disabled = true;

    try {
      const data = await api('/path?from=' + encodeURIComponent(from.id) + '&to=' + encodeURIComponent(to.id) + filterParams);
      if (!data.found) {
        results.innerHTML = '<p class="no-result">No path found between these two people (they may be in disconnected components).</p>';
        return;
      }
      results.innerHTML =
        `<div class="path-summary">
          ${data.hops} degree${data.hops !== 1 ? 's' : ''} of separation
        </div>
        <div class="path-chain">` +
        data.path.map((step, i) => {
          const isFirst = i === 0, isLast = i === data.path.length - 1;
          const edge = (i < data.path.length - 1)
            ? (() => {
                const nextTitles = data.path[i + 1].sharedTitleNames || [];
                if (!nextTitles.length) return `<div class="path-edge"><div class="path-edge-line" style="height:30px"></div></div>`;
                return `<div class="path-edge">
                  <div class="path-edge-line"></div>
                  <div class="path-edge-label">
                    ${nextTitles.length} shared title${nextTitles.length !== 1 ? 's' : ''}:
                    <span class="path-title-list">${nextTitles.map(escHtml).join(', ')}</span>
                  </div>
                  <div class="path-edge-line"></div>
                </div>`;
              })()
            : '';
          return `<div class="path-step">
            <div class="path-node ${isFirst ? 'first' : ''} ${isLast ? 'last' : ''}">
              <div class="pn-name">${escHtml(step.name)}</div>
              <div class="pn-id">${step.id}</div>
            </div>
          </div>${edge}`;
        }).join('') +
        `</div>`;
    } catch (e) {
      results.innerHTML = '<p class="no-result">Error finding path.</p>';
    } finally {
      btn.disabled = false;
    }
  });
}

function initPersonPicker(containerId) {
  const container  = document.getElementById(containerId);
  const inputEl    = container.querySelector('input');
  const dropdownEl = container.querySelector('.ac-dropdown');
  const badgeEl    = container.querySelector('.chosen-badge');

  const ac = makeAutocomplete(inputEl, dropdownEl, person => {
    showChosen(badgeEl, person, () => {
      clearChosen(badgeEl);
      ac.clear();
    });
  });
  return ac;
}

// ── Triadic closure ───────────────────────────────────────────────────────────

function initTriadic() {
  const inputEl    = document.getElementById('tri-input');
  const acEl       = document.getElementById('tri-ac');
  const chosenEl   = document.getElementById('tri-chosen');
  const strongEl   = document.getElementById('tri-strong');
  const limitEl    = document.getElementById('tri-limit');
  const btn        = document.getElementById('tri-btn');
  const resultsEl  = document.getElementById('tri-results');

  let chosenPerson = null;

  makeAutocomplete(inputEl, acEl, person => {
    chosenPerson = person;
    showChosen(chosenEl, person, () => {
      chosenPerson = null;
      clearChosen(chosenEl);
      inputEl.value = '';
    });
  });

  btn.addEventListener('click', async () => {
    if (!chosenPerson) { alert('Please select a person first.'); return; }
    const strong = strongEl.checked;
    const limit  = parseInt(limitEl.value, 10) || 20;

    resultsEl.innerHTML = spinner() + ' Finding open triads…';
    btn.disabled = true;

    try {
      const data = await api(
        `/triadic/${encodeURIComponent(chosenPerson.id)}?limit=${limit}&strongOnly=${strong}`
      );
      if (!data.length) {
        resultsEl.innerHTML = '<p class="no-result">No open triads found' + (strong ? ' (try unchecking "Strong triads only")' : '') + '.</p>';
        return;
      }
      resultsEl.innerHTML =
        `<p style="margin-bottom:12px;font-size:.85rem;color:var(--muted)">
          Found ${data.length} open triad${data.length !== 1 ? 's' : ''} where
          <strong style="color:#fff">${escHtml(chosenPerson.name)}</strong>
          is the mutual collaborator.
        </p>
        <div class="triad-list">` +
        data.map(t =>
          `<div class="triad-card ${t.isStrong ? 'strong' : ''}">
            <div class="triad-title">
              ${escHtml(t.bName)} &amp; ${escHtml(t.cName)}
              ${t.isStrong ? '<span class="triad-strong-badge">Strong</span>' : ''}
            </div>
            <div class="triad-desc">
              <strong style="color:var(--accent)">${escHtml(t.pivotName)}</strong>
              collaborated with <em>${escHtml(t.bName)}</em> on
              <strong style="color:var(--text)">${t.weightAB}</strong> title${t.weightAB !== 1 ? 's' : ''}
              and with <em>${escHtml(t.cName)}</em> on
              <strong style="color:var(--text)">${t.weightAC}</strong> title${t.weightAC !== 1 ? 's' : ''},
              but <em>${escHtml(t.bName)}</em> and <em>${escHtml(t.cName)}</em> have not worked together.
            </div>
          </div>`
        ).join('') +
        `</div>`;
    } catch (e) {
      resultsEl.innerHTML = '<p class="no-result">Error loading triads.</p>';
    } finally {
      btn.disabled = false;
    }
  });
}

// ── Graph Explorer ────────────────────────────────────────────────────────────

let visNetwork  = null;
let visNodes    = null; // vis.DataSet
let visEdges    = null; // vis.DataSet
let graphPersonData  = {}; // id -> PersonDetailDto cache
let expandedNodes    = new Set(); // ids whose neighbors are already in the graph
let graphCenterId    = null;
let graphFilterParams = ''; // filter query string applied to the current graph

function initGraphExplorer() {
  const inputEl   = document.getElementById('gex-input');
  const acEl      = document.getElementById('gex-ac');
  const chosenEl  = document.getElementById('gex-chosen');
  const btn       = document.getElementById('gex-btn');

  let chosenPerson = null;

  makeAutocomplete(inputEl, acEl, person => {
    chosenPerson = person;
    showChosen(chosenEl, person, () => {
      chosenPerson = null;
      clearChosen(chosenEl);
      inputEl.value = '';
    });
  });

  btn.addEventListener('click', () => {
    if (!chosenPerson) { alert('Please select a person first.'); return; }

    const titleType = document.getElementById('gex-title-type').value;
    const genre     = document.getElementById('gex-genre').value;
    const startYear = document.getElementById('gex-start-year').value.trim();
    const endYear   = document.getElementById('gex-end-year').value.trim();

    graphFilterParams = '';
    if (titleType)            graphFilterParams += `&type=${titleType}`;
    if (genre)                graphFilterParams += `&genres=${encodeURIComponent(genre)}`;
    if (startYear && endYear) graphFilterParams += `&startYear=${startYear}&endYear=${endYear}`;

    loadGraphForPerson(chosenPerson);
  });
}

function getGraphLimit() {
  const v = parseInt(document.getElementById('gex-limit').value, 10);
  return (!v || v < 1) ? 30 : Math.min(v, 200);
}

async function loadGraphForPerson(person) {
  const emptyEl  = document.getElementById('graph-empty');
  const netEl    = document.getElementById('graph-net');
  const legendEl = document.getElementById('graph-legend');
  const infoEl   = document.getElementById('graph-info');

  emptyEl.textContent = spinner() + ' Loading…';
  emptyEl.style.display = 'flex';
  netEl.style.display   = 'none';

  const gexInput = document.getElementById('gex-input');
  gexInput.value = person.name;
  showChosen(document.getElementById('gex-chosen'), person, () => {
    clearChosen(document.getElementById('gex-chosen'));
    gexInput.value = '';
  });

  try {
    const limit  = getGraphLimit();
    const detail = await api('/persons/' + encodeURIComponent(person.id) + '?limit=' + limit + graphFilterParams);
    graphPersonData[detail.id] = detail;
    detail.neighbors.forEach(n => { graphPersonData[n.id] = graphPersonData[n.id] || n; });

    expandedNodes = new Set();
    graphCenterId = detail.id;
    buildVisNetwork(detail, netEl);
    emptyEl.style.display = 'none';
    netEl.style.display   = 'block';
    legendEl.classList.remove('hidden');
    infoEl.classList.add('hidden');
  } catch (e) {
    emptyEl.textContent = 'Failed to load graph data.';
  }
}

async function expandNode(id) {
  if (expandedNodes.has(id)) return;
  expandedNodes.add(id);

  const limit = getGraphLimit();
  let detail;
  try {
    detail = await api('/persons/' + encodeURIComponent(id) + '?limit=' + limit + graphFilterParams);
    graphPersonData[id] = detail;
  } catch (e) { expandedNodes.delete(id); return; }

  const newNodes = [];
  const newEdges = [];

  detail.neighbors.forEach(n => {
    graphPersonData[n.id] = graphPersonData[n.id] || { id: n.id, name: n.name };
    if (!visNodes.get(n.id)) {
      const cached = graphPersonData[n.id];
      newNodes.push({
        id:    n.id,
        label: n.name,
        size:  10 + Math.min(n.sharedTitles * 2, 18),
        color: nodeColor(cached, false),
        font:  { color: '#c8d4e0', size: 11 },
        shape: 'dot'
      });
    }
    const edgeId = [id, n.id].sort().join('--');
    if (!visEdges.get(edgeId)) {
      const titles = n.sharedTitleNames && n.sharedTitleNames.length ? n.sharedTitleNames.join('\n') : n.sharedTitles + ' shared title' + (n.sharedTitles !== 1 ? 's' : '');
      newEdges.push({
        id:    edgeId,
        from:  id,
        to:    n.id,
        width: Math.max(1, Math.min(n.sharedTitles, 8)),
        title: titles,
        color: { color: '#3a4a5a', highlight: '#f0b429', opacity: 0.8 }
      });
    }
  });

  visNodes.add(newNodes);
  visEdges.add(newEdges);

  // Refresh info panel to remove the spinner/loading state
  showGraphNodeInfo(id, graphCenterId);
}

function nodeColor(p, isCenter) {
  if (isCenter) return { background: '#f0b429', border: '#9a7010', highlight: { background: '#ffd060', border: '#f0b429' } };
  if (p && p.isActor && p.isDirector) return { background: '#6a3daa', border: '#a87cf8', highlight: { background: '#a87cf8', border: '#6a3daa' } };
  if (p && p.isDirector) return { background: '#1a5e36', border: '#3ecf74', highlight: { background: '#3ecf74', border: '#1a5e36' } };
  return { background: '#1a3a6e', border: '#4f9cf9', highlight: { background: '#4f9cf9', border: '#1a3a6e' } };
}

function buildVisNetwork(detail, container) {
  const nodes = [];
  const edges = [];

  nodes.push({
    id:    detail.id,
    label: detail.name,
    size:  28,
    color: nodeColor(detail, true),
    font:  { color: '#fff', size: 14, bold: true },
    shape: 'dot'
  });

  detail.neighbors.forEach(n => {
    const cached = graphPersonData[n.id] || {};
    nodes.push({
      id:    n.id,
      label: n.name,
      size:  10 + Math.min(n.sharedTitles * 2, 18),
      color: nodeColor(cached, false),
      font:  { color: '#c8d4e0', size: 11 },
      shape: 'dot'
    });
    const edgeId = [detail.id, n.id].sort().join('--');
    const titles = n.sharedTitleNames && n.sharedTitleNames.length ? n.sharedTitleNames.join('\n') : n.sharedTitles + ' shared title' + (n.sharedTitles !== 1 ? 's' : '');
    edges.push({
      id:    edgeId,
      from:  detail.id,
      to:    n.id,
      width: Math.max(1, Math.min(n.sharedTitles, 8)),
      title: titles,
      color: { color: '#3a4a5a', highlight: '#f0b429', opacity: 0.8 }
    });
  });

  expandedNodes.add(detail.id);

  visNodes = new vis.DataSet(nodes);
  visEdges = new vis.DataSet(edges);

  const options = {
    physics: {
      stabilization: { iterations: 150 },
      barnesHut: {
        gravitationalConstant: -4000,
        springConstant: 0.002,
        springLength: 160,
        damping: 0.5
      }
    },
    interaction: {
      hover: true,
      dragNodes: true,
      dragView: true,
      zoomView: true,
      tooltipDelay: 200
    },
    nodes: { borderWidth: 2, shadow: true },
    edges: { smooth: { type: 'continuous' }, shadow: false }
  };

  if (visNetwork) visNetwork.destroy();
  visNetwork = new vis.Network(container, { nodes: visNodes, edges: visEdges }, options);

  visNetwork.on('click', params => {
    if (!params.nodes.length) return;
    showGraphNodeInfo(params.nodes[0], graphCenterId);
  });

  visNetwork.on('doubleClick', params => {
    if (!params.nodes.length) return;
    const id = params.nodes[0];
    if (id === graphCenterId) return;
    loadGraphForPerson(graphPersonData[id] || { id, name: id });
  });
}

async function showGraphNodeInfo(id, centerId) {
  const infoEl = document.getElementById('graph-info');

  let p = graphPersonData[id];
  if (!p || !p.degree) {
    try {
      p = await api('/persons/' + encodeURIComponent(id) + '?limit=5');
      graphPersonData[id] = p;
    } catch (e) { /* ignore */ }
  }

  if (!p) { infoEl.classList.add('hidden'); return; }

  const isCenter   = id === centerId;
  const isExpanded = expandedNodes.has(id);

  infoEl.innerHTML =
    `<h4>${escHtml(p.name || id)} ${isCenter ? '(selected)' : ''}</h4>
     <p>${roleTag(p)}</p>
     ${p.degree !== undefined ? `<p style="margin-top:6px">Degree: <strong style="color:var(--accent)">${p.degree.toLocaleString()}</strong> collaborators</p>` : ''}
     ${!isCenter && !isExpanded
       ? `<button id="expand-node-btn" class="btn-secondary" style="margin-top:10px;font-size:.8rem;padding:4px 10px">
            + Expand neighbors
          </button>`
       : ''}
     ${!isCenter && isExpanded
       ? `<p style="margin-top:8px;font-size:.78rem;color:var(--muted)">Neighbors loaded · double-click to re-center</p>`
       : ''}`;

  infoEl.classList.remove('hidden');

  const expandBtn = document.getElementById('expand-node-btn');
  if (expandBtn) {
    expandBtn.addEventListener('click', async () => {
      expandBtn.disabled = true;
      expandBtn.textContent = spinner() + ' Loading…';
      await expandNode(id);
    });
  }
}

// ── Boot ──────────────────────────────────────────────────────────────────────

// Allow loadGraphForPerson to be called from the search tab "View in Graph Explorer" button
window.loadGraphForPerson = loadGraphForPerson;

document.addEventListener('DOMContentLoaded', () => {
  initTabs();
  loadStats();
  initSearch();
  initCentrality();
  initPathFinder();
  initTriadic();
  initGraphExplorer();
});
